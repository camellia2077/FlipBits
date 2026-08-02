# `ultra` Mode Design

更新时间：2026-08-01

## 定位

`ultra` 是 MFSK16 clean 路径，当前目标是“项目生成的无损 PCM/WAV -> 项目解析”。core 支持默认 `15.625 Bd` 和新增的 `31.25 Bd` 两档；本轮只接入 core，Android/Web 仍使用原有默认路径。这里的“无 metadata”仅表示解码不依赖项目自定义的 frame header、payload length 或 CRC；解码仍需要调用方提供 PCM 的采样率。

它不再使用项目自定义的 16-FSK frame、payload length 或 CRC，也不兼容旧的 Ultra 音频。协议层只依赖：`mode=ultra`、固定 MFSK16 参数，以及音频本身的符号内容。

## MFSK16 参数

| 参数 | 当前值 |
| --- | ---: |
| Sample rate | 输入 PCM 的采样率 |
| Symbol rate | 默认 `15.625 Bd`；core 可选 `31.25 Bd` |
| Symbol duration | 默认 `64 ms`；`31.25 Bd` 为 `32 ms` |
| Tone count | `16` |
| Tone spacing | 默认 `15.625 Hz`；`31.25 Bd` 档为 `31.25 Hz` |
| Tone range | 默认 `1000.000 .. 1234.375 Hz`；快速档 `1000.000 .. 1468.750 Hz` |
| Bits per symbol | `4` |
| Tone mapping | MFSK16 Gray mapping |
| FEC | convolutional `R=1/2, K=7` |
| Interleaver | 10-stage diagonal `4x4` (`160` bit delay) |
| Character coding | IZ8BLY MFSK Varicode |

Tone index 到 nibble 的 Gray mapping 是：

```text
0  1  3  2  6  7  5  4  C  D  F  E  A  B  9  8
```

每个 symbol 连续发一个 tone，不插入项目自定义的 symbol gap。渲染器使用连续相位；各 tone 的频率差与所选 symbol rate 对齐到对应的 MFSK16 网格。

core 通过 `Mfsk16Speed::k15_625Bd`（默认）和 `Mfsk16Speed::k31_25Bd` 选择速率。`MakeMfsk16Config()`、文本编解码 speed overload 和底层 PCM API 支持该选择；Android 复用 mini 的 speed selector，并通过 `frame_samples` 传递两档速率。Web 本轮仍未接入。

## Clean 解码契约

当前 clean decoder 是整段音频解码器，不是实时信道接收器。调用方必须保证：

- 输入为单声道、16-bit PCM，且采样率通过 `CoreConfig.sample_rate_hz` 正确提供；
- 音频从第一个 preamble symbol 的第一个采样点开始，没有前导或尾随的未对齐样本；
- PCM 长度必须等于最后一个 symbol 边界 `B(N)`，其中 `N` 是 symbol 数，不能包含未对齐的额外样本；
  - 载波从 `1000 Hz` 起始，tone spacing 和 symbol rate 使用所选速率对应的值；
- 录音过程中没有载波频偏、采样时钟漂移、噪声、混响或多径。

`UltraDecoder` 会缓存已推送的 PCM，并在拥有完整 recording（包括完整 symbol 边界和 4-symbol tail）后一次性解码；它不会在 recording 未结束时输出增量字符，也不会搜索任意起始位置。对尚未形成完整 recording 的 `Poll` 返回 `kNotReady`；需要对单段 PCM 做严格异常检查时，使用 `DecodePcm16ToSymbols`/`DecodePcm16ToText` 等 batch API。

符号边界统一定义为：

```text
B(n) = round(n * sample_rate_hz / symbol_rate_baud)
```

第 `n` 个 symbol 覆盖 `[B(n), B(n+1))`。因此在 `44.1 kHz` 下 symbol 宽度会按 `2822`、`2823` 样本交替调度，避免固定四舍五入造成累计漂移。`NominalSymbolSamples()` 返回的整数仅是兼容性/展示用的平均 nominal 值，不能用于推导长音频的总长度或 symbol 起始位置。

当前两个速率都可用整数边界精确实现：`15.625 = 125/8`，`31.25 = 125/4`。例如 `44.1 kHz`、`31.25 Bd` 的边界为 `1411`、`2822`、`4234`……，仍然按累计边界而不是固定宽度切分。

编码、解码和 follow/timeline 必须使用同一个 `B(n)`；解码器拒绝不满足完整边界的 PCM。`sample_rate_hz < 16` 无法保证每个 symbol 至少有一个样本，因此属于无效 Ultra 配置。

## 信息与编码链路

```text
UTF-8 bytes
  -> IZ8BLY MFSK Varicode bitstream
  -> R=1/2, K=7 convolutional FEC
  -> 10-stage diagonal interleaver
  -> 4-bit groups
  -> MFSK16 Gray tone symbols
  -> selected symbol rate PCM
```

Varicode 的字符边界按 `00` gap 和下一个字符首个 `1` 的 `001` 关系解析。有限的 clean recording 在最后一个字符后提供 look-ahead bit，随后进入 idle/tail；这不是 payload metadata，也不是 frame header。

当前项目把输入字符串的 UTF-8 bytes 作为 Varicode 字节输入，以保持 Android 与 core 的字节结果一致；未来真实环境接收仍应以 MFSK16 Varicode 的字符流边界为准，而不是读取 WAV metadata。

## Preamble、tail 与 follow

- MFSK16 规范规定传输开始时发送 8 个最低 tone 的 idle carrier，结束时保留至少 4 个 symbol 的最低 tone idle；因此本实现的 8-symbol preamble 和 4-symbol tail 不是项目自定义的 frame 字段。
- 在本项目的有限 clean recording 中，4 个 tail symbol 作为 FEC 数据区之外的结束标记保留；这是一项实现契约，用于在没有 payload length metadata 的情况下确定 recording 结束，不代表已实现实时接收中的 idle/diddle 处理。
- preamble、data、tail 都进入 `ultra_frame_timeline`，但它现在表示 MFSK16 symbol timeline，不再表示旧 frame 字段。
- 用户 payload 的 byte timeline 只覆盖 data 区间；binary group timeline 每个 MFSK16 tone symbol 一个 group，`bit_count=4`。
- batch decoder 直接按所选 `symbol_rate_baud` 解析；transport/C API/Android 通过 `frame_samples` 识别 Ultra 的两档速率。未显式选择时生成 `15.625 Bd`，选择 `31.25 Bd` 时使用快速档；当前 Web 仍未接入。

## 当前边界

- 已实现：`15.625 Bd` 默认档和 core-only `31.25 Bd` 档、16 tones、对应 tone spacing、Gray mapping、IZ8BLY MFSK Varicode、`R=1/2, K=7` 硬判决 Viterbi、interleaver、clean PCM roundtrip、无项目 frame metadata API 路径。
- 尚未实现：真实录音中的 carrier offset、symbol timing recovery、AFC、软判决、任意起始位置搜索、实时增量输出、采样时钟漂移补偿和抗多径处理。
- 当前测试重点是固定标准向量和项目 clean round-trip；项目内部 round-trip 不等同于与其他 MFSK16 实现互操作验证。
- 不得重新引入旧的 `frame v1`、nibble-per-payload-byte、20Bd 或 10/15/20 WPM 逻辑作为 Ultra 分支。

## 主链路文件

- `libs/audio_core/src/ultra/codec.cpp`
- `libs/audio_core/src/ultra/phy_clean.cpp`
- `apps/audio_android/native_package/src/audio_core_ultra_codec.cpp`
- `apps/audio_android/native_package/src/audio_core_ultra_phy_clean.cpp`
- C API：`libs/audio_api/src/bag_api.cpp`
