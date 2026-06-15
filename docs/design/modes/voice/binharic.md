# Binharic

更新时间：2026-06-15

## 分类

- UI 分类：`Dual track`
- UI preset：`Binharic`
- Native preset：`kBinharic` / `BAG_VOICE_FX_BINHARIC`

`Binharic` 是当前最接近机械神甫 vox-synthesizer 的 dual track preset。它由机械化主轨和阈值触发的 flash-style 第二轨混合而成。

## 轨道结构

- 主轨：输入人声经过机械化处理。
- 第二轨：由输入人声包络阈值触发的 flash-style 副轨。
- 混音：主轨权重高于第二轨，最终通过 saturation/limiter 收口。
- final mix 带有 `Binharic` 专用输出补偿，避免 dual track 的保守 saturation 在手机外放上显得整体偏小。
- diagnostics 打开时返回 `main_voice` 和 `subvoice`。
- `Litany` style 使用专用 final mix：主轨轻微软压并加入很短的 chant tail，第二轨略突出，用来形成念诵感；它不改变 diagnostics 中的原始 `main_voice` / `subvoice`。

## 主轨特征

`Binharic` 主轨比普通 `Metal` 更偏封闭金属箱体和机械共鸣：

- 更强 pitch flatten 和轻度下移，降低自然说话感。
- 较强的 throat formant、cabinet resonance 和 resonant shaping，作为主轨的干净机械感来源。
- 借用 `Code` 的僵硬 pitch 方向：较强 pitch flatten 和适度 pitch-down，但不引入事件型 signal overlay。
- 借用 `Metal` 的机械基频方向：提高 pitch-bias layer 与 harmonics，让主轨下方有稳定的机械发声层，但不提高 static。
- 更窄的 vox grille 频带。
- 中等强度的机械 AM / chopper，只作为振膜调制，不作为主要身份来源。
- 极低随机 static 层，避免 dual track 的主轨听起来像传统重噪声变声器。
- 只在 voiced gate 打开时加入略可感知的 deterministic flutter，使用一个慢抖和一个细颤调制主轨 gain、AM depth 与 pitch-bias phase，模拟机械声门 / 振膜在负载下的不稳定。
- `kBinharic` 专用短多反射 chamber layer，制造封闭金属腔体的早期反射。

`Binharic` 的主轨目标是“人声被 vox 装置机械化”，而不是单轨重度变声。强个性主要交给第二轨承担，主轨需要保留足够可懂度。

## 第二轨特征

第二轨使用 `RenderGatedFlashSubvoiceBranch`：

- 先提取输入人声包络。
- 包络超过阈值后打开 gate。
- gate 有 attack/release，不是瞬时硬切。
- 第二轨延迟约十几毫秒，形成跟随主轨的副声道。
- `Binharic` 使用专用副轨 tuning：更低的 gate floor、更快 attack、更短 release，让第二轨按人声 envelope 做更明显的动态开合。
- `Binharic` 专用副轨加入两个窄带 formant resonator，使 flash-style 副轨更像一个机械副发声器官，而不是只在背景铺一层信号。
- `subvoice_style` 支持 `Standard / Litany / Hostility / Collapse / Zeal / Void`。
- gated branch 对 `Litany` 有额外 style gain，用来补偿阈值触发后相对 `Raw Constant` 更容易被主轨遮住的问题。
- 第二轨不编码用户文本，不参与 `flash` transport decode 语义；它只复用 flash signal/voicing 的音色材料。

## 与其他 Dual Track Preset 的区别

- 对比 `Voice Trigger`
  - `Binharic` 会处理主轨人声。
  - `Voice Trigger` 保留原始主轨，只让第二轨随人声开合。
  - `Voice Trigger` 继续使用 standard gated subvoice tuning，不使用 `Binharic` 的窄带 resonator 和更强 envelope 动态。
  - `Voice Trigger` 对 `Litany` 使用更高的最终副轨 mix，补偿原始主轨较容易遮住 gated `Litany` 的问题。
- 对比 `Raw Constant`
  - `Binharic` 的第二轨由人声阈值触发。
  - `Raw Constant` 的第二轨全程连续播放。
  - `Raw Constant` 不进入 gated subvoice branch，因此不使用 `Binharic` 的副轨整形。

## 调音边界

- 优先处理主轨机械化、短腔体、第二轨与主轨分层。
- `Litany` 可以有更平的主轨、更长一点的短尾和更突出的第二轨，但不要拉成大混响，也不要变成独立 preset。
- 可以调第二轨 gate、style、延迟和混音，但不要让第二轨抢掉主轨可懂度。
- 不要把 `Binharic` 调成 `Signal Cant` 式 burst/chirp 事件插入；它的第二轨是持续跟随语音的副声道，不是 protocol event overlay。
