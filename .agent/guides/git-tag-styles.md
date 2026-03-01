---
description: Git 标签 (Tag) 命名与管理规范
---

# Git Tag Standards (Git 标签规范)

本文档定义了项目在不同里程碑或发布节点时，使用 Git Tag 进行版本标记的标准。

## 1. 核心命名规则 (Naming Rules)

- **版本格式**: 必须以小写 `v` 开头，遵循 [语义化版本 (SemVer)](https://semver.org/lang/zh-CN/)。
- **标准格式**: `v<Major>.<Minor>.<Patch>` (例如：`v0.3.0`)。
- **预发布标记**: 对于非正式发布版本，允许使用后缀。
    - 示例：`v0.4.0-rc1` (Release Candidate), `v0.4.0-alpha`。
- **一致性**: Tag 名称必须与程序代码（如 `version.hpp`）及文档（`Release-Version`）中的版本号完全一致。

## 2. 标签类型 (Tag Types)

- **附注标签 (Annotated Tags)**: **强制要求**。用于所有的里程碑和正式发布。
    - 命令：`git tag -a v0.3.0 -m "..."`
    - 优点：包含打标签者的信息、日期及完整的描述消息。
- **轻量标签 (Lightweight Tags)**: 仅限用于开发过程中的临时标记，且严禁推送到远程仓库。

## 3. 标签消息规范 (Tag Message Format)

附注标签的消息应简洁地概括该版本的核心价值。

### 格式要求
```text
[Release] <Version> - <简短的里程碑描述>

[Highlights]
- <核心功能/重构 1>
- <核心功能/重构 2>
```

### 示例
```bash
git tag -a v0.3.0 -m "[Release] v0.3.0 - 实现核心逻辑去耦与 ABI 适配

[Highlights]
- 完成 workout_core 与 CLI 的解耦。
- 引入标准 C ABI 契约保障跨平台复用。
- 同步重构历史文档规范。"
```

## 4. 打标签的时机 (When to Tag)

- **版本发布**: 按照计划完成 `vX.Y.Z` 的开发并测试通过后。
- **架构里程碑**: 完成重大重构（如 Phase 4 落地）并确保系统稳定后。
- **文档对齐**: 当 `docs/` 下的版本记录更新并提交后，应立即打上对应的 Tag。

## 5. 操作流程 (Workflow)

1. **确认版本**: 检查 `version.hpp` 是否已更新。
2. **本地打标**: 使用标准消息格式创建附注标签。
3. **推送标签**: `git push origin v0.3.0`。
