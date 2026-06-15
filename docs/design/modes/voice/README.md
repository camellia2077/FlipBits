# Voice Mode

更新时间：2026-06-15

## 定位

`Voice` 页是 `audio -> audio` 的输入音频处理流程，不是 `mini / flash / pro / ultra` 的文本传输模式。

当前 UI 先按轨道结构分类，再在分类内选择 preset：

- `Single track`
  - 输入是一条人声音频。
  - 输出仍是一条处理后的音频。
  - preset 负责改变输入人声本身的音色。
- `Dual track`
  - 输入是一条人声音频。
  - 处理时生成主轨和第二轨，再混成最终试听/导出的 mono 输出。
  - diagnostics 打开时可返回 `main_voice` 和 `subvoice`，用于调试两条轨道。

Android 的 preset 分类由 `VoiceFxPresetOption.trackMode` 定义；native 行为由 `bag::VoiceFxPreset` / `bag_voice_fx_preset` 定义。

## 当前 Preset

### Single Track

- [`machine-voice.md`](machine-voice.md)
  - UI 名称：`Metal`
  - Native：`kMachineVoice` / `BAG_VOICE_FX_MACHINE_VOICE`
  - 处理输入人声本身，目标是机械喉咙、金属扩音器、vox grille。
- [`signal-cant.md`](signal-cant.md)
  - UI 名称：`Code`
  - Native：`kSignalCant` / `BAG_VOICE_FX_SIGNAL_CANT`
  - 处理输入人声本身，并按语音 onset 插入 machine-language signal overlay。
- [`robot-vox.md`](robot-vox.md)
  - UI 名称：`Robot`
  - Native：`kRobotVox` / `BAG_VOICE_FX_ROBOT_VOX`
  - 处理输入人声本身，目标是更通用的机器人声和短金属回声。

Single track 的三者目标边界：

- `Metal`：连续、厚实、金属喉管和箱体感更强的机械人声。
- `Code`：干、窄、僵硬，并按 onset 插入短促 protocol overlay 的机械人声。
- `Robot`：平、方、单调、合成器式的刻板机器人变声器，不插入协议事件。

### Dual Track

- [`binharic.md`](binharic.md)
  - UI 名称：`Binharic`
  - Native：`kBinharic` / `BAG_VOICE_FX_BINHARIC`
  - 主轨是机械化人声；第二轨是由人声包络阈值触发的 flash-style 副轨。
  - 主轨包含较低 static 层和 deterministic flutter，用来模拟机械声门 / 振膜不稳定。
- `Voice Trigger`
  - Native：`kVoiceTrigger` / `BAG_VOICE_FX_VOICE_TRIGGER`
  - 主轨保留原始输入人声；第二轨是由人声包络阈值触发的 flash-style 副轨。
- `Raw Constant`
  - Native：`kRawConstant` / `BAG_VOICE_FX_RAW_CONSTANT`
  - 主轨保留原始输入人声；第二轨是全程连续播放的 flash-style 副轨。

## 共享实现边界

- 输入/输出 PCM 仍是 mono PCM16。
- `final_mix` 是产品使用的最终音频。
- `main_voice` / `subvoice` / `signal_overlay` 是 diagnostics 轨道；关闭 diagnostics 时通常不会返回。
- dual track 的第二轨不走 text transport payload 编码入口；它复用 flash-style signal/voicing 的音色材料。
- `Standard / Litany / Hostility / Collapse / Zeal / Void` 是第二轨 style，影响 flash-style 副轨的音色和强度。

## 设计原则

- 先判断 preset 属于 `Single track` 还是 `Dual track`，再讨论音色。
- `Single track` 的重点是“输入人声被转换成什么声音”。
- `Dual track` 的重点是“主轨是什么、第二轨如何出现、两轨如何混合”。
- 不要把 `Voice Trigger` 描述成变声器；它不处理主轨人声，只控制第二轨随人声开合。
- 不要把 `Raw Constant` 描述成阈值触发；它的第二轨是连续背景轨。
