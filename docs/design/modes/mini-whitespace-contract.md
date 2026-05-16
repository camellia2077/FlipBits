# `mini` Whitespace Contract

更新时间：2026-05-16

## 目的
- 明确 `mini` mode 对空字符串、空白字符和混合文本的业务语义。
- 统一 Android UI、`audio_core`、`audio_api`、follow data 与 adb automation 的判断口径。
- 避免后续再次把“空白输入”和“无输入”混为一谈。

## 核心原则
- 这个项目是 text-to-audio contract，不是普通文本输入框。
- 在 `mini` mode 里，separator whitespace 本身是可编码内容。
- 因此 `space / \t / \r / \n` 不是“自动忽略的脏输入”，而是应当生成音频的合法输入。

## 三类输入

### 1. Truly Empty Text
- 输入：`""`
- 语义：没有任何可编码内容
- 预期：
  - validation 失败
  - 不生成音频
  - `audio_api` 暴露 `BAG_VALIDATION_EMPTY_TEXT`

### 2. Whitespace-Only Text
- 输入示例：
  - `"   "`
  - `"\n"`
  - `"\t"`
  - `"\r\n"`
  - `" \t\r\n "`
- 语义：只有分隔符，没有可见 glyph
- 预期：
  - validation 成功
  - 生成 separator-only payload
  - 生成 silence PCM
  - follow / decode 以规范化后的 separator text 作为 canonical text

### 3. Mixed Text
- 输入示例：
  - `"   123\n"`
  - `"A B"`
  - `"A\nB"`
  - `" \tHELLO\r\nWORLD "`
- 语义：可见 Morse glyph 与 separator whitespace 混合
- 预期：
  - validation 成功
  - 先做 `mini` normalization，再进入 payload / PCM
  - 分隔符按协议 silence 编码

## Normalization Rules
- 小写字母统一转为大写。
- `space / \t / \r / \n` 统一规范化为单个 ASCII space。
- 连续 whitespace 折叠成一个 separator。
- `mini` 的 canonical separator 文本始终是 `" "`，不保留换行和 tab 的原始字面形式。

## Payload Rules
- 可见 Morse 字符按其 ASCII byte 写入 payload。
- separator whitespace 按 `0x20` 写入 payload。
- 纯 whitespace 输入必须至少产生一个 separator byte。
- mixed text 的 trailing separator 只在 payload 同时包含可见 glyph 时允许裁掉，避免无意义尾随分隔。

## PCM Rules
- separator byte 不应被吞掉。
- separator byte 应渲染为 `kMorseWordGapUnits` 对应的 silence PCM。
- 因此 whitespace-only `mini` 输入的正确结果是“有样本数的静音音频”，不是空 PCM，也不是内部错误。

## Decode / Follow Rules
- decode 后返回规范化文本，不返回原始 `\n` / `\t` 字面量。
- whitespace-only payload decode 后的 canonical text 为 `" "`。
- follow data 仍然应发布有效 payload timeline 和 text follow，对应这一个 separator token。

## Regression Cases
- `""` -> validation: `EMPTY_TEXT`
- `"   "` -> success, silence PCM, canonical text `" "`
- `"\n"` -> success, silence PCM, canonical text `" "`
- `" \t\r\n "` -> success, silence PCM, canonical text `" "`
- `"   123\n"` -> success, canonical text `"123"`

## 相关入口
- 总览见 [`../transports.md`](../transports.md)
- `mini` 设计见 [`mini.md`](mini.md)
- Android 跨层契约见 [`../../architecture/android/android-mini-cross-layer-contract.md`](../../architecture/android/android-mini-cross-layer-contract.md)
