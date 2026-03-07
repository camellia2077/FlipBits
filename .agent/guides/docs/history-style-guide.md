---
description: Agent 专用发布历史模板
---

# Release History Template

本文件只定义 agent 编写版本历史或发布说明时必须遵守的最小规则。

## Hard Rules

- 最新版本必须写在最前面
- 版本标题格式必须为 `## [vX.Y.Z] - YYYY-MM-DD`
- 日期必须使用 ISO 8601：`YYYY-MM-DD`
- 分类只使用以下几类：
  - `### 新增功能 (Added)`
  - `### 技术改进/重构 (Changed/Refactor)`
  - `### 修复 (Fixed)`
  - `### 安全性 (Security)`
  - `### 弃用/删除 (Deprecated/Removed)`
- 列表统一使用 `* `
- 空分类不要保留
- 条目应以动词开头，简短直接
- 文件名、命令、路径、配置键统一使用反引号

## Template

```md
## [vX.Y.Z] - YYYY-MM-DD

### 新增功能 (Added)
* 新增 `<feature or file>`

### 技术改进/重构 (Changed/Refactor)
* 重构 `<module or workflow>`

### 修复 (Fixed)
* 修复 `<bug or regression>`

### 安全性 (Security)
* 修复 `<security issue>`

### 弃用/删除 (Deprecated/Removed)
* 删除 `<removed item>`
```

## Writing Rules

- 只写用户可感知或工程上重要的变化
- 同类改动尽量合并表达
- 涉及目录迁移时，明确写出路径
- 若涉及版本号、配置格式、构建方式变化，应明确写出旧口径与新口径

## Generic Example

```md
## [vX.Y.Z] - YYYY-MM-DD

### 技术改进/重构 (Changed/Refactor)
* 重构 `src/module/` 目录结构，统一默认装配入口。

### 修复 (Fixed)
* 修复 `YYYY-MM` 日期解析与校验不一致问题。

### 弃用/删除 (Deprecated/Removed)
* 删除旧配置格式与兼容加载链路。
```
