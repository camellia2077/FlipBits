# Android Encode Glyph Colors

## Goal

`Audio` 页里的机械圣像不是普通 loading icon。

它需要同时保留两件事：

- `brand identity`
  - 始终能被看作同一个项目 icon
- `theme adaptation`
  - 在 `双色主题` 下跟当前主题产生呼应

因此当前规则不是“所有主题都直接复用原始主题色”，而是按明暗类型做映射。

## Shared Rule

- 黄铜描边先固定不变
- 主题切换时，优先调整主体左右分色
- 深色主题不直接照搬背景色和满强度 accent，避免图标塌成剪影或变成霓虹 HUD

## Light Dual-tone

Examples:

- `Mars Relic`
- `Scarlet Guard`

Rules:

- 左右主体分色直接跟当前主题主配色
- 浅底主题允许 icon 更直接地复用双色关系
- 描边保持固定黄铜

## Dark Base + Solid Accent

Examples:

- `Black Crimson Rite`

Rules:

- 不直接使用接近背景的深底黑
- 左侧改成可见的深红金属底
- 右侧使用原始红 accent
- 目标是让图标读起来像“仪式机器”，而不是一团近黑剪影

Current mapping:

- left split: `#5A181C`
- right split: `#CE1126`
- outline: fixed brass

## Dark Base + Energy Accent

Examples:

- `Dynasty Revival`
- `Sepulcher Cyan`
- `Tomb Sigil`
- `Ancient Alloy`

Rules:

- 不直接使用背景深底黑
- 左侧使用可见的金属暗底
- 右侧使用收敛后的绿 / 青能量 accent
- 目标是让图标读起来像“古代机械圣像”，而不是霓虹 HUD 元素

Current mapping:

- left split: dark metal base derived from theme base + `onSurface`
- right split: restrained energy accent derived from theme accent + `onSurface`
- outline: fixed brass

## Current Scope

- `Material`
  - 继续使用默认 `Mars Relic` glyph colors
- `Light dual-tone`
  - 已接入主题分色
- `Black Crimson Rite`
  - 已接入专用深红映射
- `Ancient dynasty`
  - 已接入统一金属暗底 + 收敛能量色映射

后续如果继续精修，优先按单个深色主题微调左右 split，而不是先动描边。
