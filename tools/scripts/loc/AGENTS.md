# LOC-assisted Refactoring

本目录的扫描器只负责发现值得阅读的候选并提供定位证据，不负责判断代码是否确实需要重构，也不生成可直接执行的重构方案。

## Required reading

使用扫描结果评估或规划重构前，必须按顺序阅读：

1. [共同重构原则](../../../docs/refactoring/refactoring_principles.md)
2. 与扫描 scope 对应的架构与重构说明：
   - `audio_api` → [audio_api](../../../docs/refactoring/scopes/audio_api.md)
   - `audio_core` → [audio_core](../../../docs/refactoring/scopes/audio_core.md)
   - `audio_io` → [audio_io](../../../docs/refactoring/scopes/audio_io.md)
   - `audio_runtime` → [audio_runtime](../../../docs/refactoring/scopes/audio_runtime.md)
   - `audio_android` → [audio_android](../../../docs/refactoring/scopes/audio_android.md)
   - `audio_cli` → [audio_cli](../../../docs/refactoring/scopes/audio_cli.md)
   - `audio_web` → [audio_web](../../../docs/refactoring/scopes/audio_web.md)
3. 与候选文件语言对应的 `docs/refactoring/languages/*_refactor.md`
4. 候选源码、直接调用方、依赖、相关测试和平台构建边界

扫描报告中的 score、priority、hotspot、cluster、move set、suggestion、next action 和 stop signal 都只是排序、定位或边界假设。未完成源码阅读和依赖确认前，不得据此决定拆分。

## Tool documentation

快速入口与文档导航见 [README.md](README.md)，完整命令和配置见 [command-reference.md](docs/command-reference.md)，输出结构与 baseline/diff 见 [report-and-baseline.md](docs/report-and-baseline.md)。启发式计算规则见 [responsibility-risk-rules.md](docs/responsibility-risk-rules.md)，报告证据的读取方式见 [responsibility-report-guide.md](docs/responsibility-report-guide.md)。
