# Metal / Machine Voice

更新时间：2026-06-15

## 分类

- UI 分类：`Single track`
- UI preset：`Metal`
- Native preset：`kMachineVoice` / `BAG_VOICE_FX_MACHINE_VOICE`

`Metal` 是一条稳定、连续、礼仪化的机械人声。它直接处理输入人声，只输出一条主轨，不生成第二轨，也不插入事件型协议声。

## 定义

`Metal` 的目标是“人声经过金属喉管、扩音器和封闭箱体后被宣读出来”。它需要听起来像同一个发声主体，而不是普通机器人，也不是由脉冲事件拼接出的代码声。

核心特征：

- 连续的金属喉栅和箱体共鸣。
- 稳定、厚实、可懂的主轨。
- 轻到中等的 carrier/static veil，作为设备摩擦和电气底噪。
- 机械振膜式 AM / pitch-bias layer，但不形成离散协议事件。

## 与其他 Single Track Preset 的边界

- 对比 `Code`
  - `Metal` 是连续机械人声。
  - `Code` 是更干、更僵硬的机械人声，加 onset-triggered protocol overlay。
  - `Metal` 不应出现大量 burst / chirp / dual-tone cluster。
- 对比 `Robot`
  - `Metal` 保留更强的金属喉管、扩音器和箱体身份。
  - `Robot` 更像通用合成声或自动机声，宗教化/仪式化设备感更弱。

## 当前实现抓手

主轨使用 `MainTuningForPreset(kMachineVoice)`：

- 双高通/低通收窄频带，形成扩音器式窄带语音。
- compression + soft saturation 压平动态并增加金属硬边。
- pitch tracking + pitch flattening + 轻度 pitch shift down，降低自然人声起伏。
- resonant / throat formant / cabinet resonant shaping 制造金属喉部和箱体共鸣。
- AM / chopper / pitch-bias layer 制造机械振膜感。
- comb / slap delay 提供短金属腔体反射。
- 内部 parallel mechanical layer 低比例混回主轨：单独强化窄带 resonance、carrier/static、pitch-bias 和 comb，使机械厚度增加但主轨可懂度保留。
- static bed 带门控，避免静音区持续悬浮噪声。

parallel mechanical layer 是 `Metal` 的内部处理层，不是产品层面的 `Dual track`。它不返回 `subvoice`，diagnostics 仍只暴露 single track 主轨。

## 调音优先级

1. 先保持可懂度和稳定主轨。
2. 再调 `700 Hz - 1.6 kHz` 的金属喉栅和箱体共鸣。
3. 最后调 static / AM / comb，不要把它推成事件型协议声。
