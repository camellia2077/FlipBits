# `mini` Mode Design

更新时间：2026-05-21

## 定位
- `mini` 是独立的 Morse code 文本音频模式。
- 它不是 `pro` 的更强版本，也不是 `flash` 的简化版，而是面向“点 / 划 / 静音间隔”这种经典电报码听感的并列 transport。
- 当前目标是清晰、可解释、可视化友好的 Morse 表达，不承诺录音环境 decode、抗噪同步搜索或纠错。
- `mini` 的 speed preset 是 Morse 速度参数，不是风格化 style；它不引入闪光灯、马达或其它非音频输出媒介。

## 输入规范
- 支持：
  - `A-Z`
  - `0-9`
  - space
  - `\t / \r / \n`，会规范化成 space separator
  - 常见 International Morse punctuation：`.,?'!/()&:;=+-_"$@`
- 小写输入会自动转成大写。
- `space / \t / \r / \n` 会统一规范化成一个 ASCII space。
- 多个连续 whitespace 会折叠为一个 space separator。
- truly empty string 会在校验阶段失败；whitespace-only text 仍是合法可编码输入。
- 非支持字符应在校验阶段失败，公共 API 通过 `BAG_VALIDATION_MINI_MORSE_ONLY` 暴露失败语义。
- Android 输入区会显示 normalization 预览、Morse 点划预览与 unsupported character 提示；这些 UI 提示是编辑辅助，不改变 core 的最终校验权威。
- whitespace 详细契约见 [`mini-whitespace-contract.md`](mini-whitespace-contract.md)。

## 信息层
- 输入文本先规范化为 Morse-compatible text。
- 规范化后的每个字符仍以 byte 形式保存到 payload 中：
  - 字母 / 数字 / 标点保存其 ASCII byte
  - space 保存为 `0x20`
- 纯 whitespace 输入会保留一个 separator byte，因此会生成 silence PCM，而不是被视为 absent input。
- `mini` 不是原始 byte 透明传输；它只接受 Morse 表中存在的字符。
- `mini` 也不是 nibble / 多频映射模式；每个字符最终会通过 International Morse 表映射到 dot / dash pattern。
- decode 后返回的是规范化文本：
  - 例如 `"praise   the omnissiah"` roundtrip 后返回 `"PRAISE THE OMNISSIAH"`。

## 音频层
- 使用 clean Morse tone PHY。
- 基本单位为 `frame_samples`：
  - dot：`1` unit tone
  - dash：`3` unit tone
  - 同一字符内部 dot/dash 之间：`1` unit silence
  - 字符之间：`3` unit silence
  - 单词之间：`7` unit silence
- `mini` 的静音是协议语义的一部分，不是装饰层；visual 和 follow 都应该把 silence 留空，而不是画成 tone。

## Speed Preset
- `mini` 提供三种 Morse WPM speed preset，通过改变 `frame_samples` 控制 dot unit 长度。
- WPM 使用常见 PARIS 口径：`dot duration ms = 1200 / WPM`。
- 这三档是协议速度，不是播放倍速，也不是 UI style。

| Preset | Dot unit | `frame_samples` at `44100 Hz` | 设计目的 |
| --- | ---: | ---: | --- |
| `10 WPM` | `120 ms` | `5292` | 入门、清晰、最容易听懂 |
| `15 WPM` | `80 ms` | `3528` | 常见练习速度，建议作为默认档 |
| `20 WPM` | `60 ms` | `2646` | 较快但仍然保留 Morse 可听性和可视化清晰度 |

- WAV metadata 会保存实际使用的 `frame_samples`，这样保存后的音频重新加载时仍能按当时的 Morse speed 对齐播放、visual 与 decode。
- decode 应按 PCM timing 和 Morse tone/silence 结构解析，不依赖 metadata、follow data 或 UI speed label 直接给出文本。

## Follow / Visual
- `BuildPayloadFollowData` 对 `mini` 发布两层 timeline：
  - `byte_timeline`：按规范化字符跟随，字符跨度包含该字符 tone 与后续协议 silence。
  - `binary_group_timeline`：按 Morse tone element 跟随，只发布 dot / dash，不发布 silence。
- Text lyrics 使用规范化后的文本，因此小写和连续空格输入不会导致 lyrics token 与 payload byte 错位。
- Morse lyrics 显示 dot/dash pattern，例如 `.--. .-. .- .. ... .`，按 dot / dash element 高亮。
- Android 的 Morse Timeline Visual 使用朴素 block：
  - dot / dash 是实心块
  - dash 比 dot 更长
  - silence gap 保持空白
  - 不显示无意义背景竖线

## 主链路文件
- `libs/audio_core/src/mini/codec.cpp`
- `libs/audio_core/src/mini/phy_clean.cpp`
- `libs/audio_core/src/transport/follow.cpp`
- `libs/audio_core/src/transport/transport.cpp`
- Android package lane 对应：
  - `apps/audio_android/native_package/src/audio_core_mini_codec.cpp`
  - `apps/audio_android/native_package/src/audio_core_mini_phy_clean.cpp`

## 相关入口
- 总览 / 对比见 [`../transports.md`](../transports.md)
- 文件地图见 [`../../architecture/repo-map.md`](../../architecture/repo-map.md)
- Android 跨层契约见 [`../../architecture/android/android-mini-cross-layer-contract.md`](../../architecture/android/android-mini-cross-layer-contract.md)
