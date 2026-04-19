---
description: Agent 专用 CI / tooling / libs 提交消息写法
---

# CI And Tooling Commit Message Styles

本文件只定义 agent 在以下几类改动中如何写 Git commit message：

- `.github/workflows/**`
- `tools/**`
- `.agent/**` 中与 repo workflow 直接相关的规则
- `libs/**` 中以基础设施、构建、验证、边界收口为主的工程改动

它不是仓库 CI 地图；CI 当前职责与触发范围统一看：
- `C:\code\WaveBits\docs\tools\ci-workflow.md`

## Intent

- 让 CI / tooling / library-infra 类提交的 `subject` 和 sections 更稳定
- 避免把这类提交写成“文件清单式 changelog”
- 区分：
  - 产品能力提交
  - repo-internal tooling / CI / infra 提交

## Type Guidance

- `feat`
  - 新增一条正式命令、正式 workflow、正式校验入口
  - 例如新增 `message prep`、新增 `ci-android-quality`

- `fix`
  - 修复明确的 CI / tooling / infra 故障
  - 例如修复 workflow 触发范围错误、修复 verify 路径漂移、修复 metadata 读取偏移

- `refactor`
  - 重构现有 tooling / CI / infra 结构，但外部职责不新增
  - 例如 `tools/wavebits_tools -> tools/repo_tooling`
  - 例如拆分大文件、拆分 policy、重整命令分组

- `chore`
  - 低风险维护、清理、辅助性同步
  - 例如 `.gitignore`、辅助文档同步、脚本清理

## Subject Style

- `subject` 要先写“职责变化”，不要先写实现细节
- 推荐形状：
  - `feat: add android quality workflow`
  - `refactor: consolidate repo tooling workflows`
  - `fix: narrow android ci trigger paths`
  - `chore: ignore local android signing files`

- 避免：
  - `refactor: update many files`
  - `fix: tweak github action`
  - `chore: cleanup`

## Section Emphasis By Area

### CI / GitHub Actions

- `[Summary]`
  - 先说职责变化：新增 workflow、拆分 workflow、收紧 trigger、调整 gate

- `[Changed & Refactored]`
  - 重点写：
    - 哪条 workflow 变了
    - 是职责变了，还是触发范围变了
    - 是否把 assemble / quality / host verify 拆开了

- `[Fixed]`
  - 只写真正修复的 CI 问题
  - 例如误触发、漏触发、runner 配置漂移、命令入口错误

### Tools / Agent Workflow

- `[Summary]`
  - 先说工作流变化：命令组、默认入口、history/message/file-name 链路

- `[Added]`
  - 新命令、新草稿输出、新 validate、新 helper

- `[Changed & Refactored]`
  - 包路径迁移
  - 命令分组重排
  - history/message/file-name workflow 拆分

- `[Fixed]`
  - 帮助输出过长
  - 草稿输出路径不稳
  - 工具和文档口径漂移

### Libs / Infra

- `[Summary]`
  - 先写 contract / boundary / verification 的变化

- `[Added]`
  - 新 ABI、新 test target、新 host gate

- `[Changed & Refactored]`
  - 模块/边界/测试注册/CMake 收口

- `[Fixed]`
  - 协议边界、metadata、runtime 状态机、构建路径、测试回归

## Component Versions Guidance

- CI / tools-only 提交：
  - `[Component Versions]` 不必强行写产品版本号
  - 推荐：
    - `repo-tooling: changed`
    - `ci: changed`

- `libs` 提交：
  - 优先参考 `docs/libs/**` 已落盘口径

- Android / CLI presentation 提交：
  - 优先参考 `docs/presentation/android/**` 或 `docs/presentation/cli/**`

## Release-Version Guidance

- `Release-Version` 仍然表示本次对外发布批次
- 即使提交主要是 CI / tooling，只要它属于某个已确定发布批次，仍应跟随该批次统一版本
- 如果只是 repo-internal tools 收口且没有明确产品发布批次，再根据当前上下文决定是否写共享发布版本

## Examples

```text
feat: add android quality workflow

[Summary]
Split Android Kotlin quality checks out of the assemble workflow and make the CI surface easier to read.

[Component Versions]
- ci: changed

[Added]
- Add `ci-android-quality` for `ktlint` + `detekt`.

[Changed & Refactored]
- Keep Android assemble and Android quality in separate workflows.
- Narrow trigger paths so unrelated docs-only changes do not run Android CI.

[Verification]
- Review workflow syntax
- Push a branch and inspect Actions results

Release-Version: vX.Y.Z
```

```text
refactor: consolidate repo tooling workflows

[Summary]
Reshape the repository tooling entrypoints around clearer command groups and history-first drafting workflows.

[Component Versions]
- repo-tooling: changed

[Changed & Refactored]
- Move internal tooling from `tools/wavebits_tools` to `tools/repo_tooling`.
- Split history, message, and file-name workflows into dedicated paths.

[Fixed]
- Reduce noisy root help output.

[Verification]
- `python tools/run.py --help`
- `python -m unittest ...`

Release-Version: vX.Y.Z
```
