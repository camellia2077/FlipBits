---
description: Agent 专用 GitHub Release 文案写法
---

# GitHub Release Note Styles

本指南只约束 GitHub Release 正文，不替代 `docs/history/**`、commit message 或 PR 描述。

## 目标

- 面向最终用户说明这个版本带来的体验变化。
- 从 history 中提炼用户可感知内容，不直接复制 history 原文。
- 避免把 Release 写成文件清单、commit message 或内部实现说明。

## 信息来源

优先级：

1. 对应版本的 `docs/history/**/x.y.z.md`
2. 已确认版本范围内的 commit message
3. 产品实际行为
4. `git diff` / `git status`

有 history 时，以 history 作为事实底稿；Release 只做用户向改写和压缩。

## 硬性规则

- 默认只写中文。
- 使用 Markdown。
- 不写 `[Added]`、`[Fixed]`、`Release-Version` 等 commit 专用段落。
- 不堆文件路径、类名、函数名或模块名，除非它们本身是用户可见能力。
- 不写空话，例如“修复了一些已知问题”“优化了部分细节体验”。
- 不承诺未验证的性能、稳定性或兼容性结论。
- 删除纯内部重构、测试、工具链、CI、文档同步，除非它们直接改变用户体验。

## 推荐结构

```md
### 更新说明
<1 段总述，说明本次更新重点>

### 新增与优化
- <用户现在能做什么，或体验哪里变得更清楚>
- <同类改动合并后的一条用户向说明>

### 修复
- <用户能感知、值得知道的问题修复>
```

没有明确修复项时，删除 `修复` 整段。

## 写法

- 总述先回答“这次更新主要围绕什么”。
- 列表项优先写用户收益，而不是实现方式。
- 同一条体验链路上的多个小改动要合并。
- 用户无感的内部边界调整不要写。
- Release 应明显短于 history。

## 检查清单

- 是否说明了本次更新重点。
- 是否面向用户，而不是面向实现。
- 是否合并了同类噪音条目。
- 是否删除了空话和未验证结论。
- 是否没有直接搬运 commit message 或 history 条目。
