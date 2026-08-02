---
description: Agent 专用发布历史工作流
---

# Release History Workflow

用于根据当前工作区改动编写 release history。工具负责收集和预填，agent 负责核对事实、判断价值并定稿。

## Agent Read Protocol

按以下顺序读取，避免一开始加载完整 diff：

1. 运行：
   `python tools/run.py history prep --format markdown --view relevant`
2. 目标 history 文件已知时，增加 `--scope <repo-path> --target <history-file.md>`；只知道模块范围时至少传 `--scope`。
3. 先读取 prep 输出中的 `Relevant Summary` 和 `Draft Entry`，了解范围与候选结构。
4. 再读取 `Writing triage`，决定哪些改动应写入、合并或跳过。
5. 仅在需要核实时读取对应 topic 的 `fact` 和代表文件；最终以实际改动为准。
6. 对目标 scope 检查实际改动：
   - `git diff --stat -- <scope>`：确认改动规模。
   - `git diff -- <scope>`：核对具体行为变化。
   - `git ls-files --others --exclude-standard -- <scope>`：发现未跟踪文件，并按需读取其内容。

如果需要按模块分块交给 agent，可使用 `--out-dir temp/history-prep --split-by bucket`；这属于可选优化，不是默认步骤。

## Decision Rules

- 只写用户可感知或工程上重要的变化。
- 同类改动合并表达，不抄文件清单。
- `history-worthy` 优先改写成 history 条目。
- `supporting implementation` 通常并入上层能力或工程边界。
- `probably skip` 默认不写，除非它实际代表交付内容。
- 涉及迁移、版本、配置、构建或入口变化时，写清旧口径与新口径。
- prep 提示与实际改动冲突时，以已落盘内容和 `git diff` 为准。
- 不要把不同 scope（例如 libs、Android、tools）的改动串写到同一个 history，除非它们确实属于同一交付变化。
- Android history 优先按“用户能力 + 前端边界 + 工程入口”归纳。

## Output Contract

最终 history 必须满足：

- 最新版本在最前。
- 标题格式为 `## [vX.Y.Z] - YYYY-MM-DD`。
- 日期格式为 `YYYY-MM-DD`。
- 分类仅使用：
  - `### 改动意图`（可选元数据段，用于说明本版本要解决的问题和阶段目标）
  - `### 关联版本`（可选元数据段，用于标明同一交付在其他模块的对应版本）
  - `### 新增功能 (Added)`
  - `### 技术改进/重构 (Changed/Refactor)`
  - `### 修复 (Fixed)`
  - `### 安全性 (Security)`
  - `### 弃用/删除 (Deprecated/Removed)`
- 列表统一使用 `* `，空分类删除。
- 条目以动词开头，简短直接。
- 文件名、命令、路径和配置键使用反引号。
- 删除无关分类、噪音和 `TODO(agent)`。
- 跨模块交付可在版本标题后增加 `### 关联版本`，使用 `* 模块：版本` 列表记录对应的 Core、Android 或 Web 版本。
- 需要解释版本阶段目标时可在版本标题后增加 `### 改动意图`，使用 `* ` 列表说明改动目的和当前边界。

## Tool Responsibilities

`history prep` 负责：

- 收集工作区状态并按 scope 归类。
- 扫描现有版本口径。
- 提供 candidate topics、写作分流、少量事实和 draft scaffold。

`history prep` 不负责最终价值判断或最终文案；topic、triage 和 fact 都只是辅助信息。

`history validate <history-file.md>` 负责校验标题、分类、列表、顺序和 `TODO(agent)`；不负责判断改动是否值得写或文案是否准确。

## Completion Flow

```text
history prep -> 读取相关 diff -> agent 重写 -> history validate -> 落盘
```

推荐示例：

```powershell
python tools/run.py history prep --format markdown --view relevant --scope apps/audio_android --target docs/history/presentation/android/v0.3/0.3.0.md
python tools/run.py history validate docs/history/presentation/android/v0.3/0.3.0.md
```

若目标 history 尚未确定，不要猜版本号；先使用 `--scope` 生成上下文，再根据仓库现有 history 口径确定目标文件。
