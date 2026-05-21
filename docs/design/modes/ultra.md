# `ultra` Mode Design

更新时间：2026-05-21

## 定位
- `ultra` 是面向 UTF-8 文本字节忠实传输的正式模式。
- 它使用低速、清晰、可视化友好、参数保守的 clean `16-FSK`，作为后续更高阶多频模式的基线。
- 它的设计目标是在传输速度、信息密度、长距离声学传播和抗干扰性之间取保守平衡；相比未来的 Multi-tone FSK，它优先保留更大的抗混响、抗窄带干扰和抗设备频响凹陷余量。
- 从命名上看它不是 `pro` 的线性升级，而是并列的 UTF-8 / `16-FSK` mode。
- `ultra` 不承诺兼容任何外部 `16-FSK` / `MFSK` 协议；它是 WaveBits 内部的 clean baseline。
- 当前项目目标仍是“encode 生成音频 -> decode 同项目生成音频”的闭环，不承诺真实录音、远距离声学信道、混响或高噪声环境下的接收能力。

## 输入与字符集边界
- 输入文本直接按 UTF-8 byte 处理。
- 面向 UTF-8 文本，不再沿用 `pro` 的 ASCII-only 约束。
- 不额外做人类语义转换；核心语义是“把 UTF-8 字节按 nibble 拆开，再映射到固定频点”。

## 信息层
- 原始文本先按 UTF-8 转为 payload bytes。
- payload bytes 会进入 `Ultra clean frame v1`。
- frame bytes 再按 byte 拆成两个 nibble。
- 每个 nibble 映射成一个独立 symbol。
- 因此 `1 byte = 2 symbol`，但音频层承载的是 frame bytes，不只是裸 payload bytes。
- 它和 `flash` 的差别在于：
  - `flash` 逐 bit 发 low/high BFSK
  - `ultra` 逐 nibble 发单频 `16-FSK` symbol，并用 frame header / CRC 固定 payload 边界
- 它和 `pro` 的差别在于：
  - `pro` 每个 symbol 是一对低/高双音
  - `ultra` 每个 symbol 只发一个固定频点

## Clean 16-FSK baseline
`Ultra clean 16-FSK baseline` 是项目内基线，不是外部标准协议。

| 参数 | v1 baseline |
| --- | --- |
| Sample rate | `44100 Hz` |
| Symbol samples | `2205` |
| Symbol duration | `50 ms` |
| Symbol rate | `20 symbols/s` |
| Bits per symbol | `4` |
| Raw symbol bitrate | `80 bit/s` |
| Tone count | `16` |
| Tone start | `1000 Hz` |
| Tone spacing | `140 Hz` |
| Tone end | `3100 Hz` |
| Amplitude | `0.8` |
| Mapping | nibble `0x0`..`0xF` -> tone index `0`..`15` |

频点表：

```text
0: 1000 Hz
1: 1140 Hz
2: 1280 Hz
3: 1420 Hz
4: 1560 Hz
5: 1700 Hz
6: 1840 Hz
7: 1980 Hz
8: 2120 Hz
9: 2260 Hz
A: 2400 Hz
B: 2540 Hz
C: 2680 Hz
D: 2820 Hz
E: 2960 Hz
F: 3100 Hz
```

这个 baseline 的优先级是：
- 可解释性优先于吞吐量
- 可视化清晰度优先于压缩到最短 symbol
- 稳定 symbol 判别优先于短音节速度
- 保守频点间隔优先于极限频谱利用率
- 项目内 frame contract 优先于外部协议互通

当前不做：
- FEC
- interleaving
- 多帧重组
- 频偏估计
- timing recovery
- 录音环境同步搜索

## Frame layout v1
`Ultra clean frame v1` 是当前 `ultra` 正式协议。它用固定 header 和 CRC 把裸 UTF-8 payload 包成可校验 frame；实现不需要兼容旧的裸 payload ultra 音频。

```text
preamble | sync | version | flags | payload_length | payload | crc16
```

字段按 byte 组织，再拆 nibble 进入 `16-FSK` symbol stream。

| Field | Size | Encoding | Purpose |
| --- | ---: | --- | --- |
| `preamble` | 8 bytes | fixed alternating pattern | 给后续真实接收预留能量检测、粗 symbol 对齐和 tone table 观测入口 |
| `sync` | 2 bytes | fixed word | 标记 frame 正式开始，降低把 payload 误判为 header 的概率 |
| `version` | 1 byte | `0x01` | frame layout 版本 |
| `flags` | 1 byte | bit field, v1 must be `0x00` | 预留后续 FEC、interleaving、多帧等能力 |
| `payload_length` | 4 bytes | unsigned big-endian | UTF-8 payload byte count |
| `payload` | variable | raw UTF-8 bytes | 用户输入文本的原始 UTF-8 byte 序列 |
| `crc16` | 2 bytes | CRC-16/CCITT-FALSE over `version | flags | payload_length | payload`, unsigned big-endian | 检测 frame header/payload 是否被破坏 |

v1 固定值：

```text
preamble = A5 5A A5 5A A5 5A A5 5A
sync     = D3 91
version  = 01
flags    = 00
```

CRC-16/CCITT-FALSE 参数：

```text
poly    = 0x1021
init    = 0xFFFF
refin   = false
refout  = false
xorout  = 0x0000
```

CRC 范围不包含 `preamble` 和 `sync`。这样后续真实接收可以先用 `preamble/sync` 找边界，再用 CRC 判断 frame 内容是否可信。

## Decode baseline
- v1 decode 仍假设输入是 WaveBits encode 生成的干净 PCM/WAV。
- decode 可以按已知 `sample_rate_hz`、`symbol_samples`、tone table 和 frame layout 直接切 symbol。
- decode 必须验证：
  - `preamble`
  - `sync`
  - `version`
  - `flags`
  - `payload_length`
  - `crc16`
- `payload_length` 决定 payload bytes 的精确范围；不能再依赖“全部 symbols 都是 payload”的旧裸 payload 假设。
- CRC 失败应返回明确 decode failure，不应输出不可信文本。

## Follow / visual contract
- Text follow 和 Binary / Hex follow 的正文语义只覆盖 `payload`。
- `preamble`、`sync`、`version`、`flags`、`payload_length`、`crc16` 属于 frame metadata，不应作为用户文本 token 参与 lyrics/token 高亮。
- Ultra visual 的音频符号展示应跟随 core 提供的 full-frame symbol / group timeline，包括 preamble、sync、header、payload 和 CRC 对应的真实 `16-FSK` symbols。
- 正文 tokens、Binary、Hex 的 active range 必须继续以 payload byte timeline 为 source of truth。
- frame metadata 可以作为 visual section 展示，但不能作为用户文本 token 参与歌词或 token 页的正文高亮。

## 听感与工程意图
- `ultra` 比 `pro` 更偏高密度频点映射，而不是正式双音。
- 它的目标不是风格化情绪表达，而是把 UTF-8 文本字节组织成带边界、可校验的 `16-FSK` clean frame。
- 它不是为了追求当前项目内最高吞吐量，而是为了提供一条低速、稳定、可解释、便于可视化的 UTF-8 传输基线。
- 对 LLM / agent 来说，最稳定的理解方式是：
  - `ultra = UTF-8 payload -> Ultra clean frame v1 -> nibbles -> clean 16-FSK`

## 主链路文件
- `libs/audio_core/src/ultra/codec.cpp`
- `libs/audio_core/src/ultra/phy_clean.cpp`
- transport 分发入口：
  - `libs/audio_core/src/transport/transport.cpp`
- C API 边界：
  - `libs/audio_api/src/bag_api.cpp`

## 相关入口
- 总览 / 对比见 [`../transports.md`](../transports.md)
- 未来高速方向见 [`multi-tone-fsk.md`](multi-tone-fsk.md)
- 文件地图见 [`../../architecture/repo-map.md`](../../architecture/repo-map.md)
