---
description: 代码变更与文档更新的 Git 提交消息规范
---

# Git Commit Message Standards (Git 提交规范)

本文档定义了修改项目内程序代码（`src/`）及文档（`docs/`、`README.md`）时的标准 Git 提交信息格式。

## 1. 核心原则 (Core Principles)

- **原子性提交**: 每次提交应聚焦于单一逻辑变更或文档主题更新。
- **格式一致性**: 所有提交必须遵循本文档定义的结构化格式。
- **Release-Version**: 确保每次提交都关联当前或目标发布的版本号。
- **语义化类型**: 根据变更本质选择正确的 `<type>` 前缀。

## 2. 提交类型 (Commit Types)

- **`feat`**: 新增功能。
- **`feat!`**: 包含重大变更（Breaking Changes）的新功能或架构重构。
- **`fix`**: 修复 Bug。
- **`refactor`**: 代码重构（既不修复 Bug 也不新增功能）。
- **`docs`**: 纯文档更新（不涉及程序代码）。
- **`perf`**: 性能优化。
- **`chore`**: 构建过程或辅助工具的变动。

## 3. Git Commit Message 结构 (Structure)

提交信息必须包含结构化的 Header、Summary、分类变更块以及验证说明。

### 模板结构

```text
<type>: <subject>

[Summary]
<详细说明变更的内容、动机或背景背景。>

[Breaking Changes]
- <列出不兼容的重大变更内容（如果有）。>

[Added]
- <列出新增的功能点、文件或文档章节。>

[Changed & Refactored]
- <列出代码架构调整、逻辑重组或格式细节优化。>

[Fixed]
- <列出修复的 Bug 详情或错别字信息。>

[Verification]
- <验证方式说明：如单元测试、构建校验、Markdown 预览等。>

Release-Version: <版本号>
```

### 字段说明

- **`<type>`**: 语义化前缀。涉及代码变更时严禁使用 `docs`。
- **`<subject>`**: 简短总结，明确体现变更核心。
- **`[Summary]`**: (必填) 完整的描述。
- **`[Breaking Changes]`**: 若涉及架构解耦、接口变动等不兼容变更，必须在此列出。
- **分类块**: 根据实际内容选择 `[Added]`, `[Changed & Refactored]`, `[Fixed]`。
- **`[Verification]`**: (必填) 说明如何验证了代码或文档的正确性。
- **`Release-Version`**: (必填) 当前发布版本号，例如 `v0.3.0`。

## 4. 示例 (Example)

### 示例 1: 架构重构 (Feature/Refactor with Breaking Changes)
```text
feat!: 实现核心逻辑去耦与跨平台 ABI 适配

[Summary]
纵向打通从 C++ 核心逻辑剥离到多端复用的链路。实现核心层与 CLI 表现层彻底解耦，通过标准 C ABI 建立平台中性边界。

[Breaking Changes]
- 架构调整: 完成 `workout_core` 架构去耦，由单体工程转为“核心 + 适配器”模式。
- 通信协议: 全面迁移至 `workout_core_abi` 强类型接口，废弃直接对象耦合。

[Added]
- 核心 target: 新增 `workout_core` 独立逻辑构建目标。
- 结果模型: 引入 `UseCaseResult<T>` 统一跨边界错误传导。

[Changed & Refactored]
- 核心重构：将文件 IO、JSON 序列化等平台相关逻辑移出核心库。
- 文档记录：同步重构 `v0.1.md` - `v0.3.md` 以对齐最新架构定义。

[Verification]
- 验证核心计算逻辑 100% 覆盖，所有单元测试通过。
- 通过 Windows 侧 CMake 构建校验并完成 CLI 冒烟测试。

Release-Version: v0.3.0
```

### 示例 2: 纯文档更新 (Pure Documentation)
```text
docs: 优化版本历史记录样式规范

[Summary]
按照最新的 formatting 指南统一 `docs/` 目录下所有 Markdown 文件的标题层级与代码块样式。

[Changed & Refactored]
- 统一 `## [vX.Y.Z] - YYYY-MM-DD` 标题格式。
- 对齐列表缩进，确保 Markdown 预览渲染无误。

[Verification]
- 在 IDE 预览模式下检查所有 .md 文件。

Release-Version: v0.3.0
```