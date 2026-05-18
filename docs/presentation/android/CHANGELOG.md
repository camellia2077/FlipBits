# Android Changelog

面向用户的 Android 版本更新摘要。详细工程历史请继续参考 `docs/presentation/android/v*/x.y.z.md`。

## v0.4.16

### Added
* Added full editing for custom Material color presets, including renaming and deleting saved presets.
* Added drag-to-reorder support for custom dual-tone colors.
* Added a `Horizontal` layout option for Mini Morse Visual.
* Added four new dual-tone themes: `Brass Forge`, `Fires of Fate`, `Ecstatic Rapture`, and `Plague Mire`.

### Improved
* Persisted more theme state in `Settings`, including `Material` / `Dual-tone` mode selection, the last-used light and dark Material colors, and section expand/collapse state for built-in and custom palettes.
* Improved custom Material editing so foreground contrast, random color generation, and gear preview colors all stay aligned with the active palette and light/dark mode.
* Added smoother transitions when switching between Material colors, dual-tone themes, and `Material <-> Dual-tone` theme modes.
* Refined Material and dual-tone theme layout in `Settings`, including two-column built-in palettes, clearer family headers, and better compact card labeling.
* Unified `Visual > Hz` outline styling with `Lanes / Pitch / Pulse`, fixed the current-frequency HUD width to the widest expected Hz value, and kept the `Hz` unit pinned on the right to reduce flicker during readout changes.
* Changed `Visual > Hz` `low/high` labels to use contrast against the mini player background instead of directly following theme accent slots.

### Fixed
* Fixed custom Material imports so duplicate presets now offer `Overwrite` or `Add copy` instead of being silently skipped.
* Fixed cases where Material custom colors could lose context after switching between light and dark theme modes.
* Fixed a brief black flash during language switching.
* Fixed a cold-start theme flash where the app could briefly show the default dual-tone theme before restoring the last-used theme.
* Fixed `Visual > Hz` jumpiness during rapid play/pause debugging by tightening the playback end-action and visual trace reset path.

### 新增功能
* 新增 `Material` 自定义配色的完整编辑能力，支持重命名和删除已保存的 preset。
* 新增 dual-tone 自定义颜色的长按拖拽排序能力。
* 新增 Mini Morse Visual 的 `Horizontal` 横向布局选项。
* 新增 4 个 dual-tone 主题：`Brass Forge`、`Fires of Fate`、`Ecstatic Rapture`、`Plague Mire`。

### 体验改进
* 调整 `Settings` 的主题状态持久化，记住 `Material` / `Dual-tone` 模式、Material 明暗两套最近使用的颜色，以及 built-in / custom palette 的折叠展开状态。
* 调整 Material 自定义颜色编辑逻辑，让前景对比色、随机色和齿轮预览都与当前配色及 light/dark 模式保持一致。
* 调整 Material 颜色、dual-tone 主题与 `Material <-> Dual-tone` 模式切换时的过渡动画，让切换更平滑。
* 调整 `Settings` 中 Material 与 dual-tone 的主题布局，包括 built-in palette 双列展示、更清晰的 family header，以及更适合窄卡片的标签布局。
* 调整 `Visual > Hz` 的视觉口径：统一 `Hz` 与 `Lanes / Pitch / Pulse` 的 outline 透明度，右上角当前频率 HUD 按最大 Hz 数字固定宽度，并把 `Hz` 单位固定在右侧，减少读数切换时的闪烁感。
* 调整 `Visual > Hz` 的 `low/high` 标签配色，改为按 mini player 背景取对比色，而不是直接跟随主题强调色槽位。

### 问题修复
* 修复 Material 自定义颜色重复导入时被直接跳过的问题，现在会明确提供 `Overwrite` / `Add copy` 选择。
* 修复切换 light/dark 模式后 Material 自定义颜色容易丢失上下文的问题。
* 修复切换语言时会短暂黑屏的问题。
* 修复应用冷启动时先短暂显示默认 dual-tone 主题、再恢复上次主题的问题。
* 修复 `Visual > Hz` 在高频播放/暂停调试下容易出现画面跳动的问题，收紧了 playback end-action 和 visual trace reset 的状态链路。

## v0.4.14

### Added
* Added custom Material color themes.
* Added a persistent `Sample text` settings section, including `Auto-fill sample text` and sample decoration controls.

### Improved
* Persisted more UI state in `Settings`, including theme section expand/collapse state and dual-tone theme section state.
* Changed Mini speed selection from segmented buttons to a Flash-style dropdown selector named `Speed style`. The selected style is now remembered, with `standard` used only as the first-time default.
* Updated generated audio filenames to `mode[style]_content`.
* Flash and Mini now include `[style]` in filenames:
  Mini: `slow`, `standard`, `fast`
  Flash: `standard`, `hostility`, `litany`, `collapse`, `zeal`, `void`
* Removed generated timestamps from filenames. Creation time is now read from audio metadata when available.
* Reused dual-tone fixed colors for copy icons and related text styling, and extended the same color configuration flow.
* Updated dual-tone visual styling so token dividers and Visual lane lines use `outline` color semantics consistently.
* Refined Mini Morse presentation with fixed dot/dash graphic units and width-aware token packing.
* Improved Morse visual playback smoothness.
* Tapping a token in Flash lyrics now expands the panel and relocates to the current token more reliably.

### Fixed
* Prevented audio generation when the input is truly empty. The app now prompts the user to enter text or use randomized sample text.
* Fixed Mini Morse input cases where whitespace-only or special whitespace characters such as spaces and line breaks could fail to generate audio.
* Fixed cases where Mini generation could briefly flash and disappear when whitespace handling was inconsistent.
* Turning off `Auto-fill sample text` now clears current sample-backed input immediately, while leaving user-edited custom text untouched.

### 新增功能
* 新增 Android `Material` 自定义配色能力。
* 新增持久化的 `Sample text` 设置分组，包含 `Auto-fill sample text` 与 sample decoration 控制项。

### 体验改进
* 调整 `Settings` 的状态持久化范围，补记更多主题相关 UI 状态，包括主题分组与 dual-tone 主题分组的折叠展开状态。
* 调整 Mini 的速度选择交互，从 segmented button 改成与 Flash 风格一致的下拉选择器 `Speed style`，并记住用户上次选择，仅首次进入时默认 `standard`。
* 调整生成音频文件命名规则为 `mode[style]_content`。
* 调整 Flash 与 Mini 的文件命名，让它们都在文件名中带上 `[style]`：
  Mini 使用 `slow`、`standard`、`fast`
  Flash 使用 `standard`、`hostility`、`litany`、`collapse`、`zeal`、`void`
* 调整文件命名策略，去掉生成时间戳，创建时间改为优先从音频 metadata 读取。
* 调整 dual-tone 固定颜色在复制图标与相关文案样式中的复用方式，并扩展同一套颜色配置链路。
* 调整 dual-tone visual 的颜色语义，让 token divider 与 Visual lane 线条统一按 `outline` 语义取色。
* 调整 Mini 的 Morse 展示方式，改用固定 dot/dash 图形单元，并按宽度更稳定地组织 token 排布。
* 改进 Morse visual 的播放平滑度。
* 调整 Flash 歌词页的 token 点击行为，让展开面板并定位到当前 token 更稳定。

### 问题修复
* 修复真正空输入仍可能触发音频生成的问题。现在遇到空输入会明确提示用户输入文本或使用随机 sample text。
* 修复 Mini Morse 对仅空白或包含特殊空白字符（如空格、换行）的输入在某些情况下无法生成音频的问题。
* 修复 Mini 在空白处理不一致时，生成结果可能短暂闪现后立即消失的问题。
* 修复关闭 `Auto-fill sample text` 后当前 sample 驱动输入没有立刻清空的问题，同时保留用户自己编辑的自定义文本不受影响。
