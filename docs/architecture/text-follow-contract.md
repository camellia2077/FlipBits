# Text Follow Contract

更新时间：2026-05-15

## 目的
- 说明 `libs` 当前如何把“文本 / payload byte / sample timeline”组织成正式 follow 数据。
- 明确 `token -> character -> byte` 是 `libs` 的 source of truth，Android / CLI 不应再自行猜 UTF-8 字符边界。
- 给后续 lyrics、导出、可视化和多前端接线提供稳定语义。

## 入口
- tokenization 与文本 follow 构建：
  - `libs/audio_core/src/transport/follow_tokenization_impl.inc`
  - `libs/audio_core/src/transport/follow_entrypoints_impl.inc`
- payload timeline 构建：
  - `libs/audio_core/src/transport/follow_payload_timeline_impl.inc`
- lyric line 聚合：
  - `libs/audio_core/src/transport/follow_lyric_lines_impl.inc`
- 对外 ABI：
  - `libs/audio_core/include/bag/interface/common/types.h`
  - `libs/audio_api/include/bag_api.h`

## 总体流程
`BuildTextFollowData(...)` 依赖两个输入：

1. `PayloadFollowData`
2. 原始文本 `text`

它的工作不是重新解码音频，而是把已经存在的 payload/sample timeline 和文本结构对齐，产出正式的 follow contract。

顺序分三层：

1. `token`
2. `character within token`
3. `byte within character`

## 第一层：token
token 的职责是决定：
- Tokens 视图显示哪些块
- active token 如何跟随
- lyric line 如何按 token 聚合

规则在 `TokenizeTextForFollow(...)`：
- Latin/Cyrillic 一类的非 CJK 文本：
  - 按空白切 segment
  - segment 内的非标点字符尽量保持单词级 token
  - 标点拆成独立 token
- CJK-containing segment：
  - 非标点可见字符按单个 code point 拆 token
  - CJK 标点同样拆成独立 token

注意：
- 空白通常不生成独立 token
- 但空白对应的 payload bytes 仍然挂到前一个 token 的 `covered_byte_count`
- 同时把原 separator 文本保存在 `trailing_separator_text` / `joiner_after`

这样做的原因是：
- token card 保持“单词 / 单字符”语义
- payload byte timeline 仍然连续
- lyric line 重建时还能保留原始空格 / 换行

## 第二层：character
character 层是新增的正式 contract。

每个 token 内的字符语义都会发布一条 `TextFollowCharacterEntry`，包括：
- 可见字符
- token 挂带的 trailing space
- token 挂带的 newline
- 其他 separator

字段语义：
- `token_index`
- `character_index_within_token`
- `byte_index_within_token`
- `byte_count`
- `start_sample`
- `sample_count`
- `kind`
- `text_offset`
- `text_size`

其中：
- `kind` 表示 `Visible / Space / Newline / SeparatorOther`
- `text_offset + text_size` 是 ABI 层的文本引用
- Android domain 层应直接把它解析成 `String text`，而不是再从 token 文本切片猜字符内容

例子：

- `hello`
  - 1 个 token
  - 5 个可见 character
  - 如果原文是 `hello `，还会多 1 个 `Space` character entry
- `中`
  - 1 个 token
  - 1 个 character
  - 3 个 byte
- `A中B`
  - tokenization 取决于 segment 规则
  - 但 character 层始终会忠实记录 `A / 中 / B`

Separator 之所以也进入 character 层，是因为它们同样拥有 payload byte 和 sample span。这样 token 文本高亮在播放到空格或换行时，仍然能与 Binary / Hex 保持同一条时间轴，而不需要 Android 自己猜“尾部还有没有不可见字节”。

## 第三层：byte
byte 层对应 `token_raw_display_units`。

每个 payload byte 都会发布一条 `TextFollowRawDisplayUnitEntry`，并带上：
- `token_index`
- `byte_index_within_token`
- `byte_offset`
- `byte_count`
- `character_index_within_token`
- `byte_index_within_character`
- `character_byte_count`
- `is_character_start`
- `is_character_end`
- `start_sample`
- `sample_count`

这层是 Binary / Hex UI 直接消费的核心数据。

例子：`中` 的 UTF-8 bytes

- byte 0:
  - `byte_index_within_character = 0`
  - `character_byte_count = 3`
  - `is_character_start = true`
  - `is_character_end = false`
- byte 1:
  - `byte_index_within_character = 1`
- byte 2:
  - `byte_index_within_character = 2`
  - `is_character_end = true`

所以 UI 可以稳定画出：
- 每个 byte 之间的薄 divider
- character end 后的强 divider

而不需要再从 token 文本反推 UTF-8 宽度。

## sample timeline 如何得出
`BuildTextFollowData(...)` 不重新估算 sample 范围。

它直接复用 `PayloadFollowData.byte_timeline`：
- token 的 sample range：
  - 取 token 首 byte 的 `start_sample`
  - 到 token 末 byte 的 `start_sample + sample_count`
- character 的 sample range：
  - 取该 character 首 byte 到末 byte 的闭包范围
- byte 的 sample range：
  - 直接来自 `byte_timeline`

因此 follow contract 的时间语义是：
- payload timeline 先存在
- text follow 只是把文本结构映射到这条 timeline 上

## lyric lines
lyric line 仍然建立在 token 层之上，不建立在 character 层之上。

规则在 `BuildLyricLineData(...)`：
- 优先换行符
- 再看强标点
- 再看弱标点 + 首选长度
- 最后用 hard cap 收口

line text 的重建依赖 `joiner_after`，所以：
- Latin 空格会保留
- CJK 紧凑排布会保留
- token 内部 character/byte 元数据不会改变 lyric line 文本

## mode 差异
- `flash`
  - 文本按原始字节直通
  - follow byte timeline 来自 `BFSK` payload bit/byte 映射
- `pro`
  - 仅 ASCII
  - character 通常是 1 byte
- `ultra`
  - UTF-8
  - CJK 常见为 3-byte character
- `mini`
  - text follow 仍按 token/character/byte 组织
  - 但 payload/binary follow 的可视语义更偏 Morse tone element

## 对前端的要求
- 不要重新决定 tokenization 规则
- 不要从 token 文本重新猜 UTF-8 character 边界，除非只是 debug fallback
- 不要从 token 文本猜 trailing separator；直接看 `text_characters.kind`
- Binary / Hex divider 直接看 byte entry 的 character metadata
- token 文本高亮、导出、歌词跟随等功能优先复用 `text_characters` 和 `token_raw_display_units`

## ABI 与 Android 解析
- `text_tokens_buffer` 和 `lyric_lines_buffer` 仍然使用 joined text buffer
- `text_characters_buffer` 不直接内嵌文本，而是：
  - `text_character_text_buffer`
  - `entry.text_offset`
  - `entry.text_size`
- 这里的 `text_offset` 是“joined character text buffer 内 offset”，不是 token 内 offset
- Android JNI 层应先用 `offset + size` 取出每个 character 的 UTF-8 slice，再在 Kotlin domain DTO 中转成 `String text`
- UI 解析 separator 时，应优先看 `kind` 决定是否显示为空白占位，而不是再回退到 token 文本启发式

## 当前边界
- 这套 contract 主要描述“文本与 payload/sample timeline 的对齐”
- 它不是录音环境 decode 算法说明
- 它也不负责解释具体 mode 的 PHY 细节；那些内容继续看 `docs/design/transports.md` 和 mode 文档
