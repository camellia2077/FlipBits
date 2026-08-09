# LOC Scanner

`tools/scripts/loc/` 提供统一的代码行数、目录文件数和职责风险候选扫描。扫描结果用于安排源码阅读顺序和定位证据，不能独立证明代码需要拆分，也不生成可直接执行的重构方案。

使用扫描结果评估或规划重构前，先阅读 [AGENTS.md](AGENTS.md)。

## 快速开始

在仓库根目录运行：

```powershell
# 扫描一个架构 scope 的职责风险候选
python tools/scripts/loc/run.py --scope audio_core --responsibility-risk

# 扫描 Android 目录中的文件密度
python tools/scripts/loc/run.py --scope audio_android --dir-over-files --dir-max-depth 2

# 扫描一种语言中的小文件
python tools/scripts/loc/run.py --lang py --under

# 与之前的报告比较
python tools/scripts/loc/run.py --lang py --responsibility-risk --baseline tools/scripts/loc/logs/scan_py.json
```

Windows 快捷入口位于 `tools/scripts/loc/scripts/`，例如：

```powershell
tools/scripts/loc/scripts/scan_scope_audio_core.bat --responsibility-risk
```

完整参数、BAT 清单和配置说明见 [command-reference.md](docs/command-reference.md)。

## 支持范围

按语言扫描使用 `--lang`，当前配置包含 `cpp / kt / py / rs / js / md`。

按架构边界扫描使用 `--scope`：

- `audio_api`
- `audio_core`
- `audio_io`
- `audio_runtime`
- `audio_android`
- `audio_cli`
- `audio_web`

`--lang` 和 `--scope` 必须二选一。职责风险 analyzer 当前支持 C++、Kotlin、Python 和 Rust；JavaScript scope 使用行数与目录文件数扫描。

路径、scope 组成、排除目录和阈值的事实来源是 [scan_lines.toml](scan_lines.toml)。

## 输出

默认总报告：

```text
tools/scripts/loc/logs/scan_<lang>.json
tools/scripts/loc/logs/scan_<lang>.md
tools/scripts/loc/logs/scopes/<scope>/scan.json
tools/scripts/loc/logs/scopes/<scope>/scan.md
```

扫描器还会生成按语言、priority 和源码 owner 组织的明细 JSON/Markdown。完整目录布局、字段顺序和 baseline/diff 语义见 [report-and-baseline.md](docs/report-and-baseline.md)。

Android scope 中，`native_package/src` 里直接包含 `libs/**/src/*_impl.inc` 的 package 编译单元会被标为 `canonical_mirrors`，在报告中单独列出但不计为重构候选；共享实现应在对应的 `libs` scope 评估。

## 文档导航

- 命令、参数、BAT、配置与示例：[command-reference.md](docs/command-reference.md)
- 输出、明细报告和 baseline/diff：[report-and-baseline.md](docs/report-and-baseline.md)
- 报告证据的阅读方法：[responsibility-report-guide.md](docs/responsibility-report-guide.md)
- 各语言职责风险启发式：[responsibility-risk-rules.md](docs/responsibility-risk-rules.md)
- 通用重构原则、scope 和语言指南：[docs/refactoring](../../../docs/refactoring/README.md)

LOC 日志只保存扫描结果、明细和 baseline/diff；架构边界与通用重构知识不复制到日志目录。
