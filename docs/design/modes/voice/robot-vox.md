# Robot

更新时间：2026-06-15

## 分类

- UI 分类：`Single track`
- UI preset：`Robot`
- Native preset：`kRobotVox` / `BAG_VOICE_FX_ROBOT_VOX`

`Robot` 是一条刻板印象机器人变声器 preset。它直接处理输入人声，只输出一条主轨，不生成第二轨，也不插入协议事件。

## 定义

`Robot` 的目标是“平、方、单调、合成器式”的直觉机器人声。它不追求金属礼仪感，也不追求机器语言事件；它应该像传统变声器里最容易识别的机器人效果。

核心特征：

- 音高起伏被强烈收平，接近 monotone。
- 固定速率 AM / chopper 更规则，像电子口器在开合。
- 人声 formant 和 cabinet resonance 较弱，避免靠金属腔体建立身份。
- 有明显但可控的 synthetic repeat / slap delay。
- static 很低，不依赖噪声。

## 与其他 Single Track Preset 的边界

- 对比 `Metal`
  - `Metal` 是连续、厚实、金属喉管和箱体感更强的机械人声。
  - `Robot` 更平、更合成、更像通用机器人变声器。
- 对比 `Code`
  - `Code` 是干、窄、僵硬，并在 onset 处插入 protocol overlay。
  - `Robot` 不插入 burst / chirp / dual-tone cluster；区别来自单调 pitch、规则 AM 和 synthetic repeat。

## 当前实现抓手

主轨使用 `MainTuningForPreset(kRobotVox)`：

- 比其他 single track 更强的 pitch flattening 和 pitch-down。
- 更高的 pitch-bias harmonic layer，制造稳定电子基频。
- 较低 formant / cabinet / resonant shaping，避免靠金属腔体变成 `Metal`。
- 更规则、更低速的 AM / chopper，提供刻板机器人开合感。
- 较长 slap delay 和更高 repeat mix，形成 synthetic repeat。
- static mix 保持很低，避免变成噪声型机械音色。

## 调音优先级

1. 先保持语义清晰。
2. 再调 pitch flatten / pitch shift / pitch-bias harmonic layer。
3. 再调 AM / chopper 和 synthetic repeat。
4. 不要加入 protocol overlay；那属于 `Code`。
