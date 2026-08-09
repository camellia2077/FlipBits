# 重构指南索引

本目录保存跨工具复用的重构原则、scope 架构边界和语言级判断方法。这里定义如何判断职责与拆分边界；LOC 扫描器只提供候选和定位证据，其命令、字段与启发式实现仍由 `tools/scripts/loc/` 维护。

## 阅读顺序

使用扫描结果或人工审查规划重构前，按顺序阅读：

1. [共同重构原则](refactoring_principles.md)
2. 与目标目录对应的 scope 指南
3. 与候选文件对应的语言指南
4. 上游 `AGENTS.md`、架构/设计文档、候选源码、直接调用方、依赖、测试和平台构建边界

扫描结果不能替代第 4 步，也不能独立证明代码需要拆分。

## Scope 指南

- 共享库：
  - [audio_api](scopes/audio_api.md)
  - [audio_core](scopes/audio_core.md)
  - [audio_io](scopes/audio_io.md)
  - [audio_runtime](scopes/audio_runtime.md)
- Presentation：
  - [audio_android](scopes/audio_android.md)
  - [audio_cli](scopes/audio_cli.md)
  - [audio_web](scopes/audio_web.md)

Scope 指南描述 owner、依赖方向、允许拆分点、必须保持内聚的生命周期和验证方式。架构事实仍以上游 `docs/architecture/`、`docs/design/`、公共 API、构建定义和源码为准；scope 指南不维护第二份逐文件架构清单。

## 语言指南

- [C++](languages/cpp_refactor.md)
- [Kotlin](languages/kotlin_refactor.md)
- [Python](languages/python_refactor.md)
- [Rust](languages/rust_refactor.md)

语言指南用于识别语言层面的职责、状态和资源边界。它们不能覆盖 scope 的架构 ownership。

## 与 LOC 扫描器的关系

- 扫描命令、配置和输出：`tools/scripts/loc/README.md`
- agent 使用入口：`tools/scripts/loc/AGENTS.md`
- 报告字段阅读方法：`tools/scripts/loc/docs/responsibility-report-guide.md`
- 启发式风险规则：`tools/scripts/loc/docs/responsibility-risk-rules.md`

LOC 日志只保存扫描结果、明细和 baseline/diff，不复制本目录内容。
