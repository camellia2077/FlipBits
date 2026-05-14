# Flash Voicing: Void

更新时间：2026-05-14

## 情绪目标
`Void` 表达悲伤、空虚、低唤醒和古老技术遗失后的失落。它不是 Collapse 的恐慌结巴，也不是 Litany 的规整吟诵；它像残缺的技术记忆在低频中缓慢下沉，中间留下稀疏、真实的空白。

## Signal / Timing
- 使用 `Void` signal profile。
- profile nominal carrier 为 `205 / 410 Hz`，作为 Void 的低频中心。
- low/high carrier 不再固定为单点，而是使用 deterministic per-bit sag：
  - low carrier 范围为 `180-230 Hz`。
  - high carrier 始终为对应 low carrier 的 `2x`，范围为 `360-460 Hz`。
  - sag 只由 bit position、byte index 和 bit index 推导，不依赖 payload 解码结果；因此解码端可以在不知道明文的前提下复现同一 carrier schedule。
- bit duration 使用缓慢但不完全规整的 deterministic timing：
  - 普通 bit 为 `2.5x frame_samples`。
  - byte tail bit 为 `2.75x frame_samples`，像一句话尾部继续下坠。
  - 少数 loss bit 为 `3x frame_samples`，表达残缺记忆被拖长。
- 插入稀疏真实 payload silence：
  - byte tail 后通常插入 `1 slot`。
  - 每 3 个 byte 的尾部插入 `2 slots`。
  - 每 9 个 byte 的尾部插入 `6 slots`。
  - 少数 deterministic loss pause 插入 `4 slots`。
- decode 使用 variable-window gap-aware layout 判定。

## Voicing 方法
- 更长 attack / release 和更低 harmonic brightness。
  - 每个 tone 更慢进入、更慢泄出，避免 Standard 那种高效机械报告感。
- 极弱 byte / nibble accent。
  - 降低边界重音，让 payload 更像残存信号，而不是命令式通信。
- 更深的低频 drone、慢速 swell 和更轻的 softclip。
  - 只作为空洞、下坠的质感，不用 texture 承载 bit 语义。
- per-bit carrier sag。
  - 让 Hz visual 在低频区间内缓慢下坠和恢复，表达悲伤、空虚和技术遗失后的无力感。
- void descent shell。
  - preamble / epilogue 仍保持 `3x frame_samples`，避免 trim 与 decode 复杂化。
  - shell 使用下行、低频、拖尾的非 payload 声效。

## 听感关键词
悲伤、空虚、拖尾、下沉、残缺、空旷。

## 后续调音方向
- 可继续微调 sag phrase、loss pause 密度、release 和低频层。
- 不建议加入密集 stutter，否则会靠近 Collapse 的恐慌崩溃。
