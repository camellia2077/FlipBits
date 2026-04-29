# Flash Voicing Emotions Design

更新时间：2026-04-30

## 目标
`flash` 模式的设计目的不是高效率通信，而是把文本和音频互相转换成一种有机械宗教氛围的可解码声效。因此 `flash` 的风格层要同时满足两个目标：
- 仍然能把原始文本 byte 可靠地映射为 `low/high` BFSK payload，并在同一配置下 decode 回原文。
- 让同一段文本在不同 voicing emotion 下呈现不同情绪：日常、敌意、诵唱、崩溃。

当前用户可见的四个情绪是：
- `Steady` / 平稳
- `Hostile` / 愤怒
- `Litany` / 崇敬
- `Collapse` / 恐惧

## 四情绪模型
`flash` 当前是一次破坏性重构后的四情绪模型：用户可见和代码配置都只保留 `Steady / Hostile / Litany / Collapse`。每个 emotion 同时决定 payload timing 与 voicing 声效，不再保留旧 flavor 或旧兼容映射。

- `flash_signal_profile`
  - 负责 payload 中每个 bit 的基础时长和 silence slot 尺寸。
  - 当前只包括 `Steady`、`Hostile`、`Litany`、`Collapse`。
  - `Steady` 使用 `1x frame_samples`，carrier 为 `300 / 600 Hz`。
  - `Hostile` 使用 `1x frame_samples`，carrier 为更有攻击性的 `450 / 900 Hz`。
  - `Collapse` 使用 `1x frame_samples`，carrier 为更低、更胆怯的 `280 / 560 Hz`。
  - `Litany` 使用 `6x frame_samples`，carrier 为低沉的 `220 / 440 Hz`；它的可跳过 silence slot 仍是 `1x frame_samples`。
- `flash_voicing_flavor`
  - 负责情绪音色、preamble / epilogue、payload texture、停顿与边界 accent。
  - 当前只包括 `Steady`、`Hostile`、`Litany`、`Collapse`。

Android 当前仍然只暴露一个用户选择器，但每个 preset 内部同时指定这两轴：
- `Steady`: `signal=Steady`, `voicing=Steady`
- `Hostile`: `signal=Hostile`, `voicing=Hostile`
- `Litany`: `signal=Litany`, `voicing=Litany`
- `Collapse`: `signal=Collapse`, `voicing=Collapse`

这样做的目的，是让 UI 保持简单，同时让代码上可以分别调整“low/high 持续多久”和“这些 low/high 听起来像什么情绪”。

## Decode 边界
`flash` payload 的根本语义仍是原始 byte 透明传输：
- 每个输入 byte 拆成 8 个 bit。
- bit `0` 渲染为 low carrier。
- bit `1` 渲染为 high carrier。
- decode 时按同一 signal profile 的 slot 大小读取 low/high，再拼回 byte。

所有 emotion 的声效设计都必须遵守这个边界：
- 不把语义信息藏进 texture / drone / tremor。
- 不让 preamble / epilogue 参与 payload decode。
- 可变静音只能以整数 slot 插入，decode 通过 gap-aware 逻辑跳过这些 silence。
- `Litany` / `Collapse` 的 silence chunk 必须是真正的零样本静音，不在 silence 里加入残响，否则跳过静音的能量阈值会变得不稳定。

## Steady / 平稳
### 情绪目标
`Steady` 表达日常交流、冷静状态、协议式自然语言。它不是完全无风格的 clean BFSK，而是“机械人以平稳语调输出文本”：音高偏低，节奏稳定，边界清楚，没有恐惧或敌意。

### 当前 signal / timing
- 使用 `Steady` signal profile。
- 每个 low/high bit 使用 `1x frame_samples`。
- low/high carrier 为 `300 / 600 Hz`。
- 不插入额外可跳过 silence。
- payload timeline 连续，适合作为其他 emotion 的基线。

### 当前 voicing 方法
- 加入很弱的 `120 Hz` low voice layer。
  - 目的：让 Steady 不像裸 BFSK 那么干，而更像稳定的发声格栅。
  - 幅度保持低，避免抢占 `300 Hz / 600 Hz` 主判定频点。
- 保留轻量金属层。
  - 金属层很弱，只提供机械质感，不制造强烈情绪。
- 使用轻微 tremolo。
  - 深度很低，频率约 `6.2 Hz`。
  - 只提供微弱设备感，不形成颤抖。
- byte / nibble 边界 accent 保守。
  - byte 边界 click 比 Hostile 弱。
  - 让二进制结构仍清楚，但不攻击。
- preamble / epilogue 使用克制的 steady protocol shell。
  - 开头和结尾仍像通信协议边界，但音高和 shell 混合比旧 protocol boundary 更低、更轻。
  - 目标是“开始传输 / 结束传输”的冷静提示，不做攻击性警告、仪式召唤或崩溃尾音。

### 听感关键词
稳定、低音、平直、精准、可预测、冷静。

## Hostile / 愤怒
### 情绪目标
`Hostile` 表达愤怒、敌意、威胁和攻击性。它不通过变慢或停顿表达情绪，而是通过更硬的边界、更短的 release、更强的金属边缘和软削波，让文本像被强行喷射出来。

### 当前 signal / timing
- 使用 `Hostile` signal profile。
- 每个 low/high bit 使用 `1x frame_samples`。
- low/high carrier 为 `450 / 900 Hz`。
- 不插入额外可跳过 silence。
- 长度与 Steady 接近，强调“快、硬、压迫”。

### 当前 voicing 方法
- 更强的 byte / nibble accent。
  - `byte_boundary_click_scale` 明显高于 Steady。
  - `nibble_boundary_accent_factor` 也更强，让 payload 有切割感。
- release 更短。
  - `payload_release_scale` 小于 Steady。
  - 每个 low/high 更像短促命令，而不是吟唱或拖尾。
- 更强金属层。
  - `metallic_layer_gain_scale` 明显提高。
  - 让声效带有机械刃口和侵略性。
- hostile edge layer。
  - 在主载波之外叠加尖锐边缘层。
  - 目标是增加“咬字”和电气怒意。
- 更明显软削波。
  - 提高 softclip drive 和 mix。
  - 让整体听起来被压缩、过载、硬朗，但仍限制在 PCM16 范围。
- 快速 tremolo。
  - 频率约 `9.8 Hz`，深度比 Steady 高。
  - 表达激动和威胁，但不做 Collapse 那种失控颤抖。
- preamble / epilogue 使用 hostile challenge shell。
  - preamble 用短促多段 burst 和更硬的高频边缘，像威胁式锁定。
  - epilogue 用快速闭合 burst 和硬门限尾部，像攻击协议的断然确认。
  - 这些声音只存在于 preamble / epilogue；payload 仍保持可解码 BFSK，不用 shell 变化承载 bit。

### 听感关键词
短促、硬、尖、过载、敌意、命令式。

## Litany / 崇敬
### 情绪目标
`Litany` 表达崇敬、祷告、诵经和机械宗教仪式。它的重点是慢、稳、句读、低频共鸣和收束感。它应该听起来像一个机械喉管在逐拍诵读，而不是普通通信声。

### 当前 signal / timing
- 使用 `Litany` signal profile。
- 每个 low/high bit 使用 `6x frame_samples`。
- low/high carrier 为 `220 / 440 Hz`。
- 这让单个 tone 本身变长，慢感来自“吟出每个音”，不只是 tone 之间插空白。

### 当前 silence / cadence 方法
Litany 使用可跳过的整数 slot silence 来表达经文节奏：
- 每个 low/high bit 后默认插入 `1 slot` 静音。
  - visual 上每个 high/low 之间都有真正无波形区。
  - 听感上形成规整的一音一顿。
- 空格 / tab 后使用 `3 slots`。
  - 作为词边界或短呼吸。
- `, ; : ，；：` 后使用 `4 slots`。
  - 作为短句句读。
- 换行后使用 `6 slots`。
  - 作为段落呼吸。
- `. ! ? 。！？` 后使用 `8 slots`。
  - 作为句末大停顿。
- 每 12 byte 的 UTF-8 边界使用 `5 slots`。
  - 作为周期性长呼吸。
  - 会避免切在 UTF-8 continuation byte 中间。

这些 silence 是 payload layout 的一部分，但不承载 bit；decode 时会跳过它们。

### 当前 voicing 方法
- 更长 release 和更高 envelope floor。
  - 每个 tone 不像普通脉冲那样硬断。
  - 在可解码范围内保留一点连贯的吟唱尾部。
- 低频 chant drone。
  - 当前 drone 以 `60 / 120 / 180 Hz` 为核心。
  - gain 较 Steady 更高，用来形成神龛/管腔低鸣。
- 慢速 chant swell。
  - 约 `0.65 Hz`，深度约 `10%`。
  - 表达祷文式呼吸，而不是 tremor。
- 机械喉腔 formant。
  - 叠加很弱的 `160 / 420 / 560 Hz` 共振。
  - 再叠一点 `18 Hz` motor buzz。
  - 目标是机械喉管和金属腔体，而不是人声拟真。
- chant resonance layer。
  - 在主 BFSK 之外添加很弱的共鸣，当前 gain 约 `0.010`。
  - 幅度受控，避免干扰 low/high 判定。
- phrase tail dip。
  - nibble / byte 尾部会更明显地下沉。
  - 当前 phrase tail 从较早位置开始收束，并有较深 dip，用来表达句尾低头收声。
- text-aware pause articulation。
  - 空格和标点对应的 bit 末尾会提前下沉。
  - 让“停顿”不是突然断电，而像诵唱前自然收声。
- 弱 boundary click。
  - Litany 的边界不应像 Hostile 那样攻击。
  - click scale 当前约 `0.18`，让结构存在但不刺耳。
- litany invocation shell。
  - preamble 使用约 `1.35s` 的秒级仪式壳，不再按短 frame 倍数压缩。
  - preamble 使用三次低频金属钟击，绝对时间位置固定为 `0.20s / 0.60s / 1.00s`，三次间隔严格一致，像仪式开始前的三次敲钟。
  - 三次开场钟击使用相同时间、相同基频和相同衰减口径；当前基频约 `104 Hz`，叠加短促金属冲击、非谐波 partial 和更长尾音，目标是“咚……咚……咚……”而不是短促桌面敲击。
  - epilogue 使用约 `1.15s` 的秒级闭礼壳和一长一短二次钟击：第一声约 `92 Hz`、更低、更长，表示经文收束；第二声约 `128 Hz`、更短，表示礼仪闭合。
  - epilogue 的 terminal mute 会更晚介入，避免长钟尾音过早消失。
  - shell 内还会叠加受控 drone，但仍只放在 preamble / epilogue，不进入 payload silence chunk。
  - 前后壳比 Steady / Hostile 更长，让 UI 时长和听感都能明显感到仪式化。

### 听感关键词
慢诵、肃穆、低频、句读、机械喉腔、祷文、收束。

## Collapse / 恐惧
### 情绪目标
`Collapse` 表达恐惧、慌张、崩溃和结巴。它不是“随机坏掉”，而是内在恐惧导致外在语音控制失败：多数时候仍能输出，但会突然卡住、重复、颤抖或短暂断开。

### 当前 signal / timing
- 使用 `Collapse` signal profile。
- 每个 low/high bit 基础时长仍为 `1x frame_samples`。
- low/high carrier 为 `280 / 560 Hz`。
- 不像 Litany 那样整体变慢；Collapse 的不稳定来自局部停顿和 tremor。

### 当前 silence / stutter 方法
Collapse 使用 deterministic hash 触发局部结巴 cluster：
- 普通 bit 有较低概率触发 stutter cluster。
- byte tail 有更高概率触发 cluster，因为听起来更像词尾或音节边界卡住。
- cluster 长度为 `2-4 bits`。
- cluster 内每次停顿固定为 `2 slots`。
  - 这是“结巴”的核心：不是每次长短随机，而是同一节拍反复卡住。
- 极少数 panic break 使用 `5 slots`。
  - 表达突然彻底断一下。
- hash 基于 byte index、source byte、bit index 和全局 bit position。
  - 这样同一输入文本每次生成相同 Collapse pattern。
  - 前面插入多少 silence 不会反过来影响后面的 cluster 选择。

### 当前 voicing 方法
- tremor layer。
  - `collapse_tremor_depth` 让 voiced payload 带颤抖。
  - 目标是胆怯和维持不了稳定输出。
- hesitation articulation。
  - bit 尾部会有 near-silent hesitation 处理。
  - 和真实 silence cluster 配合，让音频有“说到一半泄气”的感觉。
- release 更长、envelope floor 较高。
  - 让声音不是硬切，而像失控拖尾。
- 金属层很弱。
  - 避免听起来像 Hostile 的攻击。
- 轻微软削波。
  - 只给崩溃边缘一点不稳定，不做强烈敌意压缩。
- preamble / epilogue 分工。
  - preamble 仍保留较弱 protocol 边界，表达“尝试建立传输”。
  - epilogue 使用 collapse failure shell，改成下坠低频、破碎短句和更早消失的尾部。
  - 这样结尾不再像旧的普通 closure，而像输出失败后逐段塌陷。

### 听感关键词
慌张、颤抖、结巴、局部卡顿、失控、短暂断线。

## 四种 emotion 的对比
| Emotion | Signal profile | 主要节奏 | 主要音色 | silence 策略 | 目标听感 |
| --- | --- | --- | --- | --- | --- |
| Steady | Steady | 连续 `1x` bit，`300 / 600 Hz` | 低音稳态、轻金属 | 无额外 silence | 日常、冷静、精准 |
| Hostile | Hostile | 连续 `1x` bit，`450 / 900 Hz`，短 release | 强 click、金属边缘、软削波 | 无额外 silence | 愤怒、敌意、命令 |
| Litany | Litany | `6x` bit，`220 / 440 Hz`，逐拍停顿 | 低频 drone、swell、机械喉腔、句尾收束 | 每 bit / 空格 / 标点 / 周期呼吸 | 崇敬、祷告、慢诵 |
| Collapse | Collapse | `1x` bit，`280 / 560 Hz`，局部 cluster | tremor、hesitation、failure shell | 固定 `2 slot` 结巴 cluster，少量 `5 slot` panic break | 恐惧、崩溃、结巴 |

## 后续调音方向
- `Steady`
  - 可以继续降低或微调低音层，让它更接近日常机械语音。
  - 不建议加入明显停顿，否则会抢 Litany / Collapse 的表达空间。
- `Hostile`
  - 可以继续打磨 attack、边界 click 和失真比例。
  - 不建议增加随机 silence，否则敌意会变成慌张。
- `Litany`
  - 可继续打磨 preamble / epilogue 的礼拜短句感。
  - 可进一步微调 formant 和 phrase tail，但要保持 silence chunk 真静音。
- `Collapse`
  - 可继续调 stutter cluster 密度和 panic break 概率。
  - 不建议让每个 bit 都停，否则会靠近 Litany 的规整吟诵。
