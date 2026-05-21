# `mini / flash / pro / ultra` 模式总览

更新时间：2026-05-21

## 目的
- 提供四种 mode 的总览、对比和快速跳转。
- 把详细设计下沉到 `docs/design/modes/`，避免总览页继续承担所有 mode 的细节。
- 为 README、AGENTS 和 repo map 提供稳定的 mode-first 入口。

## 总体原则
- 四种模式按 mode-first 架构解耦。
- 每种模式内部至少区分：
  - 信息层：文本 / 字节 / symbol 的变换
  - clean PHY：symbol 与 PCM 的互转
- 当前先保证“生成音频 -> 解析生成音频”闭环。
- 当前不做复杂风格层、真实录音环境 decode、频偏估计、timing recovery 或复杂同步搜索。
- `ultra` 已使用 `Ultra clean frame v1`，包含 preamble / sync / length / CRC；它是项目内 clean `16-FSK` baseline，不代表外部 `16-FSK` / `MFSK` 协议兼容。
- 未来 Multi-tone FSK 应作为独立高速方向处理，定位是近距离、低干扰环境下用并行 tone 或更短 symbol 换吞吐量。

## 命名映射
- `mini` -> Morse code
- `flash` -> bit-by-bit `BFSK`
- `pro` -> `DTMF-like` dual-tone mapping
- `ultra` -> clean `16-FSK`

这些名字是项目内部的产品化命名，不是同一协议的“基础版 / 高级版 / 终极版”线性升级关系。

## 字符集约束一览
| Mode | 字符集 / 输入边界 | 主要信息结构 | 主要音频结构 |
| --- | --- | --- | --- |
| `flash` | 不限字符集；输入按原始字节直通处理；公共入口仍是字符串接口 | `1 byte -> 8 bit` | low/high `BFSK` |
| `mini` | Morse-compatible text；支持 `A-Z / 0-9 / space / 常见 Morse 标点` | text -> dot/dash pattern | Morse tone + protocol silence |
| `pro` | 仅允许 ASCII | `1 byte -> 2 nibble` | `DTMF-like` dual-tone |
| `ultra` | 面向 UTF-8 文本，按 UTF-8 byte 处理 | UTF-8 payload -> frame v1 -> nibble symbols | clean `16-FSK` |

## 模式跳转

### `flash`
- 定位：娱乐化、仪式感优先的原始直通模式。
- 核心：按原始字节逐 bit 发 low/high `BFSK`，再通过 timing、carrier 和 voicing shell 制造情绪化“说话语气”。
- 详细设计：
  - [`docs/design/modes/flash/README.md`](modes/flash/README.md)
  - [`docs/design/modes/flash/voicing-emotions.md`](modes/flash/voicing-emotions.md)
  - [`docs/design/modes/flash/`](modes/flash/)

### `mini`
- 定位：Morse code 文本音频模式，强调点 / 划 / 静音间隔的可听、可视、可跟随表达。
- 核心：文本先规范化为 Morse-compatible text，再按 dot / dash / silence unit 渲染。
- speed preset 固定为 `10 WPM / 15 WPM / 20 WPM`，属于 Morse 协议速度，不是 style。
- 详细设计：
  - [`docs/design/modes/mini.md`](modes/mini.md)
  - [`docs/design/modes/mini-whitespace-contract.md`](modes/mini-whitespace-contract.md)

### `pro`
- 定位：ASCII-only 的正式模式。
- 核心：ASCII byte 拆成高低 nibble，再映射成 `DTMF-like` 双音 symbol。
- 详细设计：
  - [`docs/design/modes/pro.md`](modes/pro.md)

### `ultra`
- 定位：面向 UTF-8 文本字节忠实传输的正式模式；低速、清晰、可视化友好、参数保守。
- 核心：UTF-8 payload 先进入 `Ultra clean frame v1`，再拆成 nibble 并映射到 clean `16-FSK` 固定频点。
- frame v1 设计为 `preamble | sync | version | flags | payload_length | payload | crc16`，正文 follow 只覆盖 payload。
- 详细设计：
  - [`docs/design/modes/ultra.md`](modes/ultra.md)

### Future Multi-tone FSK
- 定位：未来高速模式，面向近距离、低干扰环境。
- 核心：用更多并行 tone、更多 bits per symbol 或更短 symbol duration 换传输速度。
- 代价：降低抗混响、抗窄带干扰、抗设备频响凹陷和复杂声学拓扑的余量。
- 详细设计：
  - [`docs/design/modes/multi-tone-fsk.md`](modes/multi-tone-fsk.md)

## 外部统一入口
- transport 分发：
  - `libs/audio_core/src/transport/transport.cpp`
- C API：
  - `libs/audio_api/src/bag_api.cpp`

## 当前明确不做
- 随机化或不可预测的 style layer，例如可变长度背景层、随机前导/收尾
- FEC / 重传
- 多帧拆分与长文本协议
- 真实环境同步搜索
- `mini` 的录音环境 decode 鲁棒性
- 噪声、衰减、截断下的鲁棒性承诺
