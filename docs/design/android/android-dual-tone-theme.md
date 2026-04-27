# Android Dual-tone Theme

Related:

- [Android Encode Glyph Colors](./android-encode-glyph-colors.md)

## Goal

`dual-tone` 不把两种颜色平均铺到所有控件上，而是从 `BrandThemeCatalog.kt` 的视觉角色出发：

- `backgroundColor`
  - 负责页面背景、surface 基底、dock 基底和大面积氛围
- `accentColor`
  - 负责局部强调、选中状态、主交互、图标和 disclosure chrome
- `outlineColor`
  - 负责机械细节、小面积支撑色、非激活轨道或图形描边
  - 不默认接管主交互边框；只有文档或共享 token 明确声明时才进入结构性状态

## Rules

- `Material` 主题继续走单色 `ColorScheme` 语义。
- `dual-tone` 主题必须视为独立主题系统。
- `dual-tone` 组件优先从 `backgroundColor` / `accentColor` / `outlineColor` 进入共享 token/helper，再映射到具体控件。
- 不要把 `primaryContainer` / `surfaceVariant` / `outlineVariant` 直接当成 dual-tone 的最终视觉语义；这些 Material 槽位只负责承载组件结构、状态和可读性基础。
- 大面积背景和卡片层次优先跟 `backgroundColor`
- 小面积强调优先跟 `accentColor`
- `outlineColor` 优先服务于非激活轨道、机械细节和小面积支撑描边
- 正文文本和分隔线优先从 `ColorScheme` 的可读性色推导
- 不让 `accentColor` 直接铺满整块卡片或正文区域
- `dual-tone` 通过显式 token 把 `accentColor` 和 `outlineColor` 接到状态性 chrome 与细节层

## Theme Types

### Light base + solid accent

Examples:

- `Mars Relic`
- `Scarlet Guard`

Rules:

- `disclosure arrow`
  - use `accentColor` directly or a slightly deeper variant
- `selected label`
  - use `accentColor`
- `selected border`
  - use `accentColor`
- `action icon`
  - use a lighter version of `accentColor`
  - typically `accentColor` mixed with `onSurface`

### Dark base + solid accent

Examples:

- `Black Crimson Rite`

Rules:

- `disclosure arrow`
  - use a darker, heavier `accentColor`
- `selected label`
  - use a darker, heavier `accentColor`
- `selected border`
  - use a darker, heavier `accentColor`
- `action icon`
  - do not use the heaviest `accentColor` directly
  - typically `accentColor` mixed with `onSurface`

### Dark base + energy accent

Examples:

- `Dynasty Revival`
- `Sepulcher Cyan`
- `Ancient Alloy`

Rules:

- `disclosure arrow`
  - use a restrained bright `accentColor`
  - typically `accentColor` mixed with `onSurface`
- `selected label`
  - use a stronger `accentColor` than the arrow
- `selected border`
  - use a stronger `accentColor` than the arrow
- `action icon`
  - use the most restrained `accentColor` variant
  - typically more mixed with `onSurface` than the arrow

## Theme Systems

- `Material`
  - continue using a single-color `ColorScheme`
  - component visuals may directly follow Material slots such as `primary`, `primaryContainer`, `surfaceVariant`
- `dual-tone`
  - start from `backgroundColor` / `accentColor` / `outlineColor`
  - route stateful chrome through shared helpers such as:
    - `AppThemeAccentTokens.kt`
    - `AppThemeVisualTokens.kt`
    - `AudioAndroidThemeMappings.kt`
  - keep Material components for structure and readable defaults, but do not let Material-derived container slots define dual-tone semantics by accident

## Current Mapping

- `Material`
  - all current UI accent tokens use `primary`
- `Dual-tone`
  - `AppThemeAccentTokens`
    - routes stateful accent lanes such as disclosure, selected label, selected border and utility icons
  - `AppThemeVisualTokens`
    - routes dock container, segmented-button idle container, input container, action container, support surfaces, visualization base background and inactive support tones
  - bottom navigation and mini-player dock are handled as explicit `container + foreground` pairs instead of falling back to generic Material container slots

## Strong Selected Treatments

- `Settings` dual-tone theme rows
  - some themes need a stronger selected state to stay distinct from the surrounding card
  - selected rows can use all three together:
    - higher-contrast `backgroundColor`-side container
    - stronger `accentColor` border
    - clearer selected badge
- `Audio` flash voicing rows
  - follow the same stronger selected-state pattern as Settings rows
  - selected state should read as a structured choice, not only a text color change
- `Audio` primary action buttons
  - buttons normally stay on the standard container treatment
  - a small number of stronger actions can add a clearer `accentColor` border when they need more separation from nearby surfaces

## Bottom Navigation

- `Material`
  - selected state follows the normal Material mapping
  - unselected state uses `primary` with reduced alpha
- `Dual-tone`
  - dock container is mapped explicitly from `backgroundColor` with a restrained `accentColor` mix
  - dark-base themes keep unselected icon/text on the brighter paired color directly
  - light-base themes slightly correct the brighter paired color toward `onPrimaryContainer`
    - this preserves the `backgroundColor` / `accentColor` relationship while keeping the foreground distinct from the dock container
  - selected icon/text/indicator are mapped explicitly from the original dual-tone pair
  - this keeps navigation in the same dual-tone language instead of falling back to single-color Material alpha variants

## Current Tokens

- `disclosureAccentTint`
  - 展开箭头、关于页箭头这类 disclosure chrome
  - 优先承担“状态性强”的展开指示
- `actionAccentTint`
  - 输入卡片里的 `TXT` / 骰子等小型 action icon
  - 比 `disclosureAccentTint` 更克制
- `selectionLabelAccentTint`
  - 已选标签文字
  - 比普通正文更强，但不直接接管正文系统
- `selectionBorderAccentTint`
  - 选中边框、选中描边
  - 用于选中态的结构性强调
- `AppThemeVisualTokens`
  - `dockContainerColor`
    - mini-player / bottom navigation 的共享 dock 容器色
  - `segmentedInactiveContainerColor`
    - dual-tone segmented button 未选中态容器
  - `inputContainerColor`
    - dual-tone 输入框容器
  - `supportSurfaceColor` / `supportStrongSurfaceColor`
    - dual-tone 说明卡片、辅助 pill、可视化说明面板的容器层
  - `visualizationBaseBackgroundColor`
    - waveform / flash / symbol 等可视化底板
  - `visualizationInactiveToneColor`
    - 非激活轨道、机械支撑色；当前优先从 `outlineColor` 落位

## Color Role Names

`BrandThemeCatalog.kt` uses visual-role names rather than Material slot names:

- `backgroundColor`
  - dominant page / surface color
  - used for large-area atmosphere and dual-tone preview's left side
- `accentColor`
  - primary interaction and selection color
  - used for buttons, selected states, disclosure accents, and dual-tone preview's right side
- `outlineColor`
  - small-area graphic / mechanical highlight color
  - currently used by the Audio encode gear glyph outline, visualizer inactive support tones, and other small support-detail tokens
  - defaults to `accentColor` when a theme does not need a separate outline accent

Do not infer color responsibility from `isDarkTheme`. `isDarkTheme` only selects readable Material color-scheme behavior; it must not swap the semantic meaning of `backgroundColor` and `accentColor`.

## Audio Encode Glyph

- The Audio encode gear glyph is part of the dual-tone language.
- Fill colors come from `AudioEncodeGlyphColors`:
  - `primarySplit` = `accentColor`
  - `secondarySplit` = `backgroundColor`
  - `outline` = `outlineColor`
- Do not hard-code theme-specific gear colors inside glyph drawing components.
- If a new theme needs better gear contrast, set `outlineColor` in `BrandThemeCatalog.kt` first.
- If a new dual-tone visualizer, guide rail, or non-active support mark needs a mechanical support tone, prefer routing it through a shared token backed by `outlineColor` before hard-coding a one-off local color.

## Adding Or Changing A Dual-tone Theme

When adding a new dual-tone theme or changing theme colors:

1. Read this document first.
2. Update `BrandThemeCatalog.kt` with:
   - `backgroundColor`
   - `accentColor`
   - optional `outlineColor`
   - `sampleFlavor`
3. If adding a new group / lineup, update all visible string resources for the group and theme names.
4. If adding a new text flavor, update:
   - `AndroidSampleInputTextProvider.kt`
   - matching `audio_samples_*` resources for every supported language
   - tests or fake providers that exhaustively switch on `SampleFlavor`
5. If adding a new Settings group, wire it into `ConfigThemeAppearanceSection.kt` so it participates in collapsible dual-tone grouping.
6. Keep concrete UI components free of theme-id color branches; route color behavior through shared helpers / tokens.
7. If the change introduces a new dual-tone container or support surface rule, update `AppThemeVisualTokens.kt` and this document together.
8. If the change introduces a new stateful accent rule, update `AppThemeAccentTokens.kt` and this document together.
9. Run the focused Android verification used by nearby theme/sample changes.

