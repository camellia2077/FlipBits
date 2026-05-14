# Flash Voicing: Hostility

更新时间：2026-05-14

## 情绪目标
`Hostility` 表达愤怒、敌意、威胁和攻击性。它不通过变慢或停顿表达情绪，而是通过更硬的边界、更短的 release、更强的金属边缘和软削波，让文本像被强行喷射出来。

## Signal / Timing
- 使用 `Hostility` signal profile。
- 每个 low/high bit 使用 `0.875x frame_samples`。
- profile nominal carrier 为 `559 / 1118 Hz`，作为 Hostility 的高区间中心。
- low/high carrier 不再固定为单点，而是使用 deterministic per-bit jitter：
  - low carrier 使用高区间上冲 jitter，范围为 `500-618 Hz`。
  - high carrier 始终为对应 low carrier 的 `2x`，范围为 `1000-1236 Hz`。
  - jitter 只由 bit position、byte index 和 bit index 推导，不依赖 payload 解码结果；因此解码端可以在不知道明文的前提下复现同一 carrier schedule。
- 不插入额外可跳过 silence。
- 不插入 payload silence，但比 Standard 更短，强调“快、硬、压迫”。

## Voicing 方法
- 更强的 byte / nibble accent。
  - `byte_boundary_click_scale` 明显高于 Standard。
  - `nibble_boundary_accent_factor` 也更强，让 payload 有切割感。
- attack / release 更短。
  - `payload_release_scale` 小于 Standard。
  - 每个 low/high 更像短促命令，而不是吟唱或拖尾。
- 更强金属层。
  - `metallic_layer_gain_scale` 明显提高。
  - 让声效带有机械刃口和侵略性。
- hostility edge layer。
  - 在主载波之外叠加尖锐边缘层。
  - 目标是增加“咬字”和电气怒意。
- 更明显软削波。
  - 提高 softclip drive 和 mix。
  - 让整体听起来被压缩、过载、硬朗，但仍限制在 PCM16 范围。
- 快速 tremolo。
  - 频率约 `9.8 Hz`，深度比 Standard 高。
  - 表达激动和威胁，但不做 Collapse 那种失控颤抖。
- per-bit carrier jitter。
  - 让 Hostility 在 Hz visual 中呈现急促、尖锐的上冲抖动。
  - high 仍严格跟随 low 的 `2x`，避免破坏 BFSK 的 low/high 判定关系。
- preamble / epilogue 使用 hostility challenge shell。
  - preamble 用短促多段 burst 和更硬的高频边缘，像威胁式锁定。
  - epilogue 用快速闭合 burst 和硬门限尾部，像攻击协议的断然确认。
  - 这些声音只存在于 preamble / epilogue；payload 仍保持可解码 BFSK，不用 shell 变化承载 bit。

## 听感关键词
短促、硬、尖、过载、敌意、命令式。

## 后续调音方向
- 可以继续打磨 attack、边界 click 和失真比例。
- 不建议增加随机 silence，否则敌意会变成慌张。
