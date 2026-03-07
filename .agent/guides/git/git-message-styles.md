---
description: Agent 专用 Git 提交消息模板
---

# Git Commit Message Template

本文件只定义 agent 生成或重写 Git commit message 时必须遵守的最小规则与模板。

## Allowed Types

- `feat`
- `feat!`
- `fix`
- `refactor`
- `docs`
- `perf`
- `chore`

## Hard Rules

- 代码改动禁止使用 `docs`
- 纯文档改动才允许使用 `docs`
- `subject` 必须简短直接，不写表情，不写句号
- `Release-Version: <version>` 必须为最后一行
- 存在 breaking changes 时，使用 `feat!` 或添加 `[Breaking Changes]`
- 空 section 不要保留
- `squash` 或 `reword` 后，不要保留 `Squashed commits:` 或原 commit 列表
- 涉及中文 commit message 的生成、落盘、amend、reword 或 `--file` 提交时，优先使用 `pwsh` / `pwsh.exe`
- 需要将提交信息写入文件时，使用 `pwsh` 的 UTF-8 输出（如 `Set-Content -Encoding utf8`），避免中文乱码

## Template

```text
<type>: <subject>

[Summary]
<1-3 行摘要>

[Breaking Changes]
- <breaking change>

[Added]
- <added item>

[Changed & Refactored]
- <changed item>

[Fixed]
- <fixed item>

[Verification]
- <verification step>

Release-Version: vX.Y.Z
```

## Section Rules

- `[Summary]` 必填
- `[Verification]` 必填
- `[Breaking Changes]` 仅在存在 breaking changes 时出现
- `[Added]`、`[Changed & Refactored]`、`[Fixed]` 按实际改动保留
- 列表项统一使用 `- `

## Generic Example

```text
refactor: simplify module structure

[Summary]
整理模块边界并统一默认入口，减少重复实现。

[Changed & Refactored]
- 调整目录结构
- 合并重复逻辑
- 同步更新相关脚本

[Verification]
- 运行项目构建
- 运行相关测试

Release-Version: vX.Y.Z
```
