---
description: Agent 专用 GitHub Release 文案写法
---

# GitHub Release Note Styles

本文件只定义 agent 在编写 GitHub Release 正文时应遵守的最小规则与推荐结构。

它的目标不是替代：

- `docs/presentation/**` 或 `docs/libs/**` 中的 history
- Git commit message
- PR 描述

它只负责一件事：

- 让 GitHub Release 页面上的版本说明更稳定、更面向用户、更适合中英双语发布

## Intent

- 面向最终用户说明“这个版本带来了什么体验变化”
- 让用户快速知道：
  - 这次更新主要在改什么
  - 新增了什么能力
  - 哪些体验被优化了
  - 修了哪些值得感知的问题
- 避免把 Release 正文写成：
  - 文件清单
  - commit message 搬运
  - history 原文直贴
  - 面向开发者的实现细节说明

## Source Priority

编写 GitHub Release 时，语义来源优先级如下：

1. 对应版本的 history 文档
   - 例如 `docs/presentation/android/v0.4/0.4.16.md`
   - 例如 `docs/libs/v0.6/0.6.1.md`
2. 已确认的版本范围内 commit message
3. 产品实际改动和用户可感知行为
4. `git diff` / `git status`

规则：

- 有明确 history 时，优先吃 history，不重新从文件列表发明语义
- history 是“事实底稿”，Release 是“用户向重写”
- 不要直接把 history 各条目原样复制进 Release

## Audience

GitHub Release 默认面向：

- 普通用户
- 测试用户
- 关注新版本变化的使用者

不是默认面向：

- 仓库维护者自己
- 只关心实现细节的开发者
- 想靠 Release 读完所有底层改动的人

因此 Release 的默认语气应该：

- 直接
- 易懂
- 面向体验
- 少术语

## Hard Rules

- 默认输出中英双语
- 先中文，再英文
- 使用 Markdown
- 标题层级保持稳定，避免过深嵌套
- 不要把 Release 写成 commit message 的 section 结构
- 不要出现 `[Summary]`、`[Added]`、`[Fixed]`、`Release-Version` 这类 commit 专用段落
- 不要出现文件路径、类名、函数名、模块名堆砌，除非它们本身就是用户可见能力的一部分
- 不要罗列“重构了哪些文件”
- 不要写无意义套话，例如：
  - “修复了一些已知问题”
  - “优化了部分细节体验”
  - “进行了若干改进”
- 不要承诺未验证的效果
- 不要虚构兼容性、性能或稳定性结论

## Recommended Structure

推荐使用以下三段结构：

```md
## 中文

### 更新说明
<1 段总述>

### 新增与优化
- <用户可感知新增或改进>
- <用户可感知新增或改进>

### 修复
- <用户可感知修复>
- <用户可感知修复>

---

## English

### Release Notes
<1 段总述>

### New and Improved
- <user-facing addition or improvement>
- <user-facing addition or improvement>

### Fixes
- <user-facing fix>
- <user-facing fix>
```

说明：

- `更新说明 / Release Notes`：一句到两句，概括这次更新重心
- `新增与优化 / New and Improved`：合并“新增功能”和“体验优化”
- `修复 / Fixes`：只写用户能感知、值得知道的问题修复

如果本次版本没有明确修复项，可删除 `修复 / Fixes` 整段。

## Writing Rules

### 总述怎么写

总述先回答：

- 这次更新主要围绕什么
- 用户最应该知道的变化方向是什么

推荐写法：

- “这次更新主要围绕主题系统、自定义配色和播放显示体验展开……”
- “This update focuses on theme customization, playback clarity, and smoother settings behavior…”

避免写法：

- “本次版本包含以下更新”
- “This release includes bug fixes and improvements”

因为这些句子没有提供真实信息。

### 列表项怎么写

每个列表项优先写：

1. 用户现在能做什么
2. 用户现在会看到什么变化
3. 之前哪里不顺，现在怎么变得更顺

推荐：

- “mini Morse visual 新增 `Horizontal` 横向显示模式，方便按不同阅读习惯查看播放内容。”
- “Material custom colors now support preset renaming and deletion, making palette management more direct.”

不推荐：

- “新增 `MiniMorseVisualizationModeSwitcher`”
- “重构 `ConfigThemeAppearanceSection`”

因为这些是实现细节，不是用户收益。

### 什么该合并

以下内容通常应该合并表达，而不是拆很多小点：

- 同一条设置链路上的多个持久化改动
- 同一类主题视觉调整
- 同一组导入体验改进
- 同一类布局优化

目标：

- 让用户读到的是“能力变化”
- 不是“工程拆分后的子任务清单”

### 什么该省略

默认省略：

- 内部重构
- 测试补充
- 工具链调整
- 自动化脚本更新
- 翻译工作流更新

除非它们直接改变了用户可见能力，例如：

- 导入流程更明确
- 多语言文案更完整
- 设置页布局在长语言下明显更稳定

## Tone Guidance

中文建议：

- 简洁自然
- 不要营销腔
- 不要过度夸张
- 少用“重磅”“全面升级”“大幅提升”之类的词

英文建议：

- Plain and direct
- Prefer product-facing wording over engineering wording
- Avoid hype and vague claims

## Scope Guidance

### 适合写进 Release 的内容

- 用户现在能直接使用的新功能
- 用户能明显感知的界面或交互变化
- 用户之前容易遇到、现在已修复的问题
- 会影响版本理解的重要命名或口径变化

### 不适合默认写进 Release 的内容

- 纯重构
- 纯测试
- 纯脚本
- 纯 CI
- 纯文档同步
- 对用户无感的内部边界调整

如果一个版本同时包含产品改动和大量 repo-internal tooling 改动：

- Release 只保留和用户体验相关的部分
- tooling/CI/internal workflow 不要强行写进 Release

## Relationship To History

history 和 Release 的关系：

- history：更完整、偏发布记录
- Release：更短、更用户向、更适合 GitHub 页面阅读

推荐做法：

1. 先写好 history
2. 从 history 提炼用户可感知内容
3. 压缩成 Release 正文

一个常见误区是：

- history 写 12 条，Release 也写 12 条

这通常太长。Release 应该主动合并同类项。

## Example

```md
## 中文

### 更新说明
这次更新主要围绕主题自定义、设置持久化和播放可视化体验展开，重点提升了主题编辑的完整性、切换过程的连贯性，以及日常使用中的稳定性。

### 新增与优化
- `Material` 自定义配色现在支持重命名、删除和更完整的编辑流程，自定义颜色管理更直接。
- dual-tone 自定义颜色支持长按排序，mini Morse visual 新增横向显示模式。
- 主题设置现在会记住更多状态，包括主题外观选择、最近使用的颜色以及 palette 折叠状态。
- 主题切换与随机配色的视觉表现进一步优化，预览、对比度和过渡动画更连贯。

### 修复
- 修复了随机 Material 配色后前景色未正确更新的问题。
- 修复了 light / dark 模式切换后颜色上下文丢失的问题。
- 修复了语言切换时短暂黑屏，以及冷启动时先闪默认主题的问题。

---

## English

### Release Notes
This update focuses on theme customization, settings persistence, and Morse visualization, with improvements to editing flow, smoother theme transitions, and overall day-to-day stability.

### New and Improved
- `Material` custom colors now support renaming, deletion, and a more complete editing flow.
- Dual-tone custom colors can now be reordered with long press, and mini Morse visual now includes a horizontal mode.
- Theme settings now remember more state, including the selected appearance, recently used colors, and palette collapse states.
- Theme transitions and random color previews have been refined for clearer contrast and smoother visual feedback.

### Fixes
- Fixed stale foreground colors after randomizing Material custom colors.
- Fixed color context getting lost when switching between light and dark mode.
- Fixed the brief black screen during language switching and the default-theme flash during cold start.
```

## Review Checklist

发布前至少过一遍：

- 总述是否说明了“这次更新主要在改什么”
- 列表项是否面向用户，而不是面向实现
- 是否把同类噪音条目合并了
- 是否删除了无价值套话
- 中文和英文是否表达一致，但不是机械直译
- 是否避免了未验证结论
- 是否没有把 commit message section 直接搬进来

