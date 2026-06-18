# Web WASM Release 编译参数性能实验

更新时间：2026-06-18

## 目的

这份记录只关注 `apps/audio_web` 的 WebAssembly Release 编译参数是否还能继续优化运行时性能。

当前 Web demo 部署在 GitHub Pages，产物体积不是主目标；优先看 encode 实际耗时。

## 实验范围

- 目标项目：
  - `apps/audio_web`
- 工具链：
  - `Emscripten 5.0.7`
- 基线配置：
  - `-O3`
  - `-sALLOW_MEMORY_GROWTH=1`
- 测试入口：
  - `python tools/run.py web perf-data`
- 测试用例：
  - `short-flash-standard`
  - `long-flash-litany`
  - `long-ultra`

## 实验方法

- 所有对照组都通过 `apps/audio_web/CMakeLists.txt` 的缓存参数切换。
- 每组都重新 configure + build，然后覆盖 `apps/audio_web/site/wasm/` 产物。
- 每组都顺序运行一次 `python tools/run.py web perf-data`。
- 结果以 `perf-data` 输出的 `totalMs` 为主，`workerRoundtripMs` / `pumpMs` 作为辅助参考。

## 结果

| 配置 | short-flash-standard | long-flash-litany | long-ultra | 结论 |
| --- | ---: | ---: | ---: | --- |
| `O3` baseline | `771.7 ms` | `58763.9 ms` | `153.1 ms` | 当前最稳的默认值 |
| `O3 + LTO` | `857.6 ms` | `58241.7 ms` | `146.6 ms` | `long-flash` 仅快约 `0.9%`，整体收益很小 |
| `O3 + SIMD` | `756.9 ms` | `69336.9 ms` | `335.8 ms` | 明显变慢，不建议 |
| `O3 + LTO + SIMD` | `1413.7 ms` | `83684.2 ms` | `340.7 ms` | 明显更慢，不建议 |
| `O3 + LTO + SIMD + fixed 64MB memory` | `1500.6 ms` | `91266.0 ms` | `296.6 ms` | 最慢，不建议 |

## 结论

- 默认 `O3` 仍然是当前最合适的 Release 性能基线。
- `LTO` 在这套 workload 上没有稳定、显著的收益，暂时不值得默认开启。
- `SIMD` 在当前实现上不是优化项，反而会明显拖慢 `flash` 和 `ultra` case。
- 关闭 `ALLOW_MEMORY_GROWTH` 并固定 `INITIAL_MEMORY=64MB` 没有带来收益，当前不建议改。

## 后续建议

- 若继续做编译参数实验，优先把新增方案与这里的 `O3` 基线对比，不要再回到 `Oz/Os` 体积方向。
- 若目标仍然是降低 `long-flash-litany` 耗时，下一阶段更值得看 native 算法热点，而不是继续堆编译选项。
- 如果未来升级 Emscripten / LLVM / Binaryen 版本，建议重新跑这里的同一组 case，因为 `LTO` 和 `SIMD` 的收益可能随工具链变化。

## 相关入口

- 构建命令：
  - `docs/notes/web/cmd.md`
- Web build 脚本：
  - `tools/repo_tooling/web/build.py`
- Web perf 入口：
  - `tools/repo_tooling/web/perf.py`
  - `tools/web/perf/data-perf.mjs`
