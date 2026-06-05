# Tools Docs

`docs/tools/` 只记录仓库内部开发工具与工作流的长期文档，不承载 `libs` / `cli` / `android` 的产品版本历史。

## 目录口径

- `history/`
  - 记录 `tools/`、agent workflow、repo-internal tooling 相关的重要演进
  - 文件名固定使用日期：`YYYY-MM-DD.md`
  - 不使用 `vX.Y.Z` 版本树

## 适合写进 `docs/tools/history/` 的内容

- `python tools/run.py` 的命令入口、分组、默认工作流变化
- `tools/repo_tooling/` 内部架构重构，只要它改变了 agent / 开发者可感知的使用方式
- `history` / `message` / `file-name` 这类辅助工作流的新增、重构或口径变化
- 静态 policy、validate、artifact、Android build orchestration 等工具侧重要收口

## 不适合写进 `docs/tools/history/` 的内容

- 纯产品功能变化，且 tooling 本身没有用户可感知变化
- 只影响单个组件实现、但没有改变工具入口或开发工作流的内部重排
- 临时调试脚本、一次性草稿、未正式采用的实验命令

## 约定

- 同一天内如果有多轮 tooling 改动，优先合并到同一个日期文件，而不是按批次继续细分文件名
- 如需写作规范，统一参考：
  - `<repo-root>/.agent/workflow/docs/tools-history-style-guide.md`

## Flash speed audio analysis

When investigating Flash playback-speed pitch or timbre reports, first run the host-side PCM analysis tool before changing the renderer:

```powershell
python tools/run.py artifact flash-speed-analysis --speed 0.1
```

The default run analyzes all six Flash styles: `standard`, `hostility`, `litany`, `collapse`, `zeal`, and `void`. It generates Flash source WAV files through the Rust CLI/core path, renders speed-adjusted PCM with a host mirror of Android `SpeedAdjustedPcmRenderer`, then writes `summary.md`, `metrics.json`, and `metrics.csv` under `build/test-artifacts/flash-speed-analysis/`.

Use this output to compare source PCM against rendered PCM before treating subjective style reports as confirmed pitch changes. `hostility` and `zeal` are comparison samples only; they are not assumed to be known-good controls. If this PCM analysis looks stable while device playback still sounds wrong, inspect the Android playback/output chain next.

## Android playback speed memory diagnostics

Speed-adjusted Android playback emits debug-only `PlaybackSpeedMemory` logcat rows from the low-speed / high-speed render path. Use this when deciding whether playback memory is worth optimizing; do not start by changing renderer allocation strategy without before/after numbers.

The log rows cover:

- renderer type: `flash`, `mini_cw`, or `generic`
- mode, playback speed, streaming vs pre-render, and file-backed vs in-memory source
- source and rendered sample counts
- estimated source/rendered PCM byte counts
- Java heap before/after/delta
- native heap before/after/delta
- source file load time or render time

Manual capture window:

```powershell
python tools/run.py android-debug capture-playback-speed --wait-ms 60000
```

During the capture window, start playback and select the speed/scenario being measured. The command writes raw logcat and a markdown summary under `temp/android-debug/`.

To summarize an existing raw log:

```powershell
python tools/run.py android-debug playback-speed-summary temp\android-debug\<capture>\raw.log
```

The summary includes a `Memory Rows` table when `PlaybackSpeedMemory` events are present. Representative scenarios to collect before optimizing are Flash `0.1x`, Mini low-speed playback, generic pre-render, and generic streaming. If a capture has no memory rows, playback did not enter the speed-adjusted render path during that log window.
