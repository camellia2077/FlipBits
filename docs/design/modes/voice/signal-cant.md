# Code / Signal Cant

更新时间：2026-06-15

## 分类

- UI 分类：`Single track`
- UI preset：`Code`
- Native preset：`kSignalCant` / `BAG_VOICE_FX_SIGNAL_CANT`

`Code` 是一条协议插入型机械人声。它处理输入人声本身，并在同一条最终输出中按语音 onset 插入短促的 machine-language overlay。它不是 `Dual track` preset，不返回 `subvoice`。

## 定义

`Code` 的目标是“一个机械化发声主体正在说话，同时有短促协议脉冲附着在重音和起音处”。它不靠连续噪声区分，也不追求厚重箱体感；区别点是干净、僵硬、事件化。

核心特征：

- 主轨比 `Metal` 更干、更窄、更僵硬。
- pitch flatten 和适度 pitch-down 更明显，降低自然语调。
- 连续 static 很低，避免把协议事件糊成背景噪声。
- overlay 是短促的 burst / dual-tone / chirp cluster，由语音 onset 和 envelope 触发。
- overlay 与主轨共存于最终单轨混音，混音时主轨会轻微 duck。

## 与其他 Single Track Preset 的边界

- 对比 `Metal`
  - `Metal` 是连续、厚实、礼仪化的机械主轨。
  - `Code` 是更干、更冷、更事件化的协议插入声。
  - `Code` 的身份来自 onset-triggered overlay，而不是更重的 distortion 或 static。
- 对比 `Robot`
  - `Code` 仍保留输入人声的发声主体和语言节奏。
  - `Robot` 更平、更合成化，不强调 protocol cluster。

## 当前实现抓手

主轨使用 `MainTuningForPreset(kSignalCant)`：

- 更窄的主轨带宽，减少箱体厚度。
- 较强 pitch flattening 和 pitch-down，让语调更规整。
- 较低 AM / chopper / static，让主轨保持干净。
- 收紧的 throat formant 和较轻 cabinet resonance，使主轨像协议终端输出，而不是厚重金属箱体。

事件层使用 `RenderSignalCantOverlay`：

- 先通过 `ExtractVoiceEnvelope` 提取输入包络。
- 当 envelope delta 和 level 超过阈值时触发事件。
- 事件有最小间隔，避免过密。
- 每个事件是较短的 signal window / cluster。
- cluster 内包含 flash-inspired burst、DTMF-like dual-tone burst、chirp/high burst。
- 混音时主轨按 overlay presence 做轻微 duck。
- diagnostics 打开时返回 `signal_overlay`，不返回 `subvoice`。

## 调音优先级

1. 先调 onset 触发和 cluster 密度。
2. 再调 overlay mix、main duck 和 window length，让事件清楚但不盖住语义。
3. 最后调 burst/chirp 频带，让事件和机械主声道分层。
