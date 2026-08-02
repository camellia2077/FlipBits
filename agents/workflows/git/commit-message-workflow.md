---
description: Agent 专用 Git commit 工作流
---

# Git Commit Workflow

目标是安全地完成一次 commit：确认提交范围，生成准确的 commit message，核对 staged 内容后提交。

## Default Workflow

1. 检查工作区：
   - `git status --short --untracked-files=all`
   - `git diff --cached --stat`
   - 必要时读取 `git diff --cached`
2. 明确本次提交范围。已有 staged 文件视为候选范围；如果尚未 staged，只暂存用户明确要求的文件，不要带入无关改动。
3. 生成 message 草稿：
   - 有明确对应的 history 时，运行 `python tools/run.py message prep --history <history-file.md>`。
   - 有多个对应 history 时，显式重复传入 `--history`，不要让工具猜测。
   - 没有对应 history 时，运行 `python tools/run.py message prep`，使用 git fallback。
4. 读取并改写 `temp/message.txt`，按 `agents/guides/git/git-message-styles.md` 校正类型、标题、摘要、版本和验证信息。草稿不可直接当作最终 message。
5. 提交前再次检查：
   - `git diff --cached --stat`
   - `git diff --cached`
   - staged 内容与 message 描述一致，且没有无关文件。
6. 执行 `git commit`。

## History Usage

History 是 message 的可选语义来源，不是普通 commit 的前置步骤。

仅在以下情况使用 history：

- 本次提交明确属于某个 release history。
- 工作区中已有与本次改动对应的 history 文件。
- 用户明确要求基于指定 history 生成 commit message。

如果没有明确 history，直接使用 staged diff 和 git fallback 生成 message。不要为了生成普通 commit message 临时创建 release history。

## Commit Scope Rules

- 只提交本次确定的范围；保留工作区中其他未相关改动。
- `git diff --cached` 是 commit 内容的最终事实来源，不能只依据工作区的完整 `git diff` 写 message。
- 未跟踪文件必须显式纳入提交范围后才能提交。
- 如果 staged 内容为空，先确认需要提交的文件并完成 staging，再生成最终 message。
- 如果 staged 内容与用户目标不一致，先调整范围，不要直接 commit。

## Message Rules

- message 的格式、允许的 type、section、`Release-Version` 和 `[Verification]` 规则，以 `agents/guides/git/git-message-styles.md` 为唯一准则。
- `[Verification]` 只写本次真实执行过的检查。
- 不保留 `TODO(agent)`、文件清单式摘要或无关实现细节。
- `temp/message.txt` 只是草稿，最终提交内容必须经过 agent 校正。

## Release Commit Variant

如果本次是发布批次提交，额外按对应 history 口径处理：

```text
history -> message prep --history -> agent 改写 -> staged diff 校对 -> git commit
```

此时可根据已落盘 history 补充 component versions 和 release version；不明确的版本信息不得猜测。

## Completion Checklist

- 提交范围已确认。
- staged diff 不包含无关改动。
- commit message 与 staged diff 一致。
- `[Verification]` 真实可追溯。
- message 符合 `agents/guides/git/git-message-styles.md`。
- 已执行 `git commit`。
