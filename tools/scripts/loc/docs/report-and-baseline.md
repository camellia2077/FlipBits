# LOC Report and Baseline Reference

本文记录 LOC 扫描器的输出位置、统一报告模型、明细字段和 baseline/diff 语义。如何根据这些证据阅读源码见 [responsibility-report-guide.md](responsibility-report-guide.md)；具体评分启发式见 [responsibility-risk-rules.md](responsibility-risk-rules.md)。

## 默认输出

语言扫描写入：

```text
tools/scripts/loc/logs/scan_<lang>.json
tools/scripts/loc/logs/scan_<lang>.md
```

Scope 扫描写入：

```text
tools/scripts/loc/logs/scopes/<scope>/scan.json
tools/scripts/loc/logs/scopes/<scope>/scan.md
```

Scope JSON 根节点包含 `scope`、`display_name` 和 `parts`。每个 part 保留语言报告的 `lang`、`scan`、`results` 和 summary。

## 明细目录

扫描器还会按语言、priority 和具体源码 owner 写出明细 JSON/Markdown：

```text
tools/scripts/loc/logs/<lang>/P*/<source_name>_scan.json
tools/scripts/loc/logs/<lang>/P*/<source_name>_scan.md
```

- Kotlin 目录名固定为 `kotlin`。
- 目录扫描使用 `_dir_scan.json` / `_dir_scan.md`。
- 文件名只保留源码文件名，完整绝对路径写在报告内容中。
- C++ 被 `.cpp`、`.cppm` 或 `.inc` 包含的本地 `.inc` 归属于编译 owner，不重复生成独立职责结果；无 owner 的孤立 `.inc` 仍会报告。

## 自定义日志路径

`--log-file` 可以覆盖总报告路径：

```powershell
python tools/scripts/loc/run.py --lang py --under 120 --log-file logs/loc_scan_py.json
```

相对路径固定相对 `tools/scripts/loc/`：

- `--log-file logs/scan_py.json` 写入 `tools/scripts/loc/logs/scan_py.json`。
- `--log-file scan_py_custom.json` 写入 `tools/scripts/loc/scan_py_custom.json`。
- 明细报告仍统一写入 `tools/scripts/loc/logs/<lang>/P*/`。

需要避免相对路径规则时传绝对路径。

## 统一输出链路

输出经过同一条语义链：

```text
scanner result model -> formatter -> JSON/Markdown writers
                              \-> console summary
```

Console 与 Markdown 共用 formatter。终端只显示适合快速定位的 score、lines、summary、risks 和明细报告路径；完整证据保留在 JSON/Markdown 中。

## 职责风险条目

公共结论字段优先排列：

1. `path`
2. `score`
3. `priority`
4. `summary`
5. `dominant_risks`
6. `suggestion`
7. `next_action`
8. `confidence`
9. `decision`
10. `lines`

其后才是语言 analyzer 的计数型证据。字段只是排序、定位或边界假设；它们不是自动重构计划。

结构化辅助证据包括：

- `function_hotspots`：函数/composable 级热点。
- `anchors`：状态、副作用、分支或绘制分发等行号锚点。
- `responsibility_clusters`：可能相关的职责聚类。
- `move_sets`：C++ helper 的可能 owner/依赖域分组。
- `suggested_extraction_candidates`：候选 owner、边界假设、风险和验证方向。
- `stop_signal`：启发式证据是否值得继续调查。
- `validation_hints`：候选变更的最小验证方向。
- `false_positive_notes`：框架信号或正常 owner 可能造成的误报。
- `implementation_sources`：区分共享 canonical implementation 与复制实现。

这些字段的正确阅读顺序和限制见 [responsibility-report-guide.md](responsibility-report-guide.md)。

## Baseline diff

传入 `--baseline <previous.json>` 后，报告根部包含：

- `baseline`：基线 JSON 的绝对路径。
- `diff.summary`：`added / removed / changed / unchanged` 数量。
- `diff.entries`：每个文件或目录的状态；changed 条目记录 lines、priority、score、confidence、decision 等字段变化。
- `scanned_files`：responsibility scan 的全部扫描文件，用于区分 `below_threshold` 与真正的 `removed`。
- `diff.fragmentation_delta`：文件总数、小文件数和孤立 fragment 的变化。
- `diff.dependency_fanout_delta`：依赖扇出总量与最大值变化。
- `diff.duplicate_owner`：内容指纹相同但 implementation source 不同的 owner。

Scope diff 使用 `part + kind + path` 作为 identity，避免不同语言 part 中的同名文件互相覆盖。

多个 adapter 共享同一个 canonical implementation source 不视为复制实现。baseline diff 用来发现意外碎片化、依赖扩散和复制 owner，不能代替编译、测试或行为验证。
