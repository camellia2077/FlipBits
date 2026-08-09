# LOC Scanner Command Reference

本文记录 `tools/scripts/loc/run.py` 的完整调用方式、参数、scope、Windows 快捷入口和配置来源。首次使用先看 [README](../README.md)；使用报告规划重构前先看 [AGENTS.md](../AGENTS.md)。

## 命令格式

在仓库根目录执行：

```powershell
python tools/scripts/loc/run.py (--lang <cpp|kt|py|rs|js|md> | --scope <scope>) [paths ...] [--over N | --under [N] | --dir-over-files [N] | --responsibility-risk [N]] [--dir-max-depth N] [--log-file <path>] [--baseline <path>]
```

`--lang` 和 `--scope` 必须二选一。扫描模式 `--over`、`--under`、`--dir-over-files` 和 `--responsibility-risk` 互斥；未显式选择时使用工具默认模式。

## 选择扫描范围

- `--lang <cpp|kt|py|rs|js|md>`：按语言扫描。
- `--scope <name>`：按仓库架构 scope 聚合一个或多个语言 part。
- `paths`：可选的一个或多个扫描目录；省略时读取配置中的默认路径。

当前 scope：

| Scope | 路径 | 语言 |
|---|---|---|
| `audio_api` | `libs/audio_api` | C++ |
| `audio_core` | `libs/audio_core` | C++ |
| `audio_io` | `libs/audio_io` | C++ |
| `audio_runtime` | `libs/audio_runtime` | C++ |
| `audio_android` | `apps/audio_android` | C++、Kotlin |
| `audio_cli` | `apps/audio_cli` | Rust |
| `audio_web` | `apps/audio_web` | C++、JavaScript |

Scope 的精确路径与语言集合以 `scan_lines.toml` 的 `[scopes.<name>]` 为准。JavaScript 当前支持行数和目录文件数扫描，不支持 responsibility analyzer。

## 扫描模式

- `--over N`：报告超过阈值的文件。
- `--under [N]`：报告低于阈值的文件；省略 `N` 时使用语言默认值。
- `--dir-over-files [N]`：报告代码文件数超过阈值的目录；省略 `N` 时使用语言默认值。
- `--dir-max-depth N`：限制目录扫描相对输入根的深度，只与 `--dir-over-files` 一起使用；`0` 表示只检查根目录。
- `--responsibility-risk [N]`：运行对应语言的职责风险 analyzer；省略 `N` 时使用语言默认风险阈值。
- `-t N` / `--threshold N`：兼容旧入口，等价于 `--over N`。

`--responsibility-risk` 当前支持 C++、Kotlin、Python 和 Rust。各语言信号、评分和误报边界见 [responsibility-risk-rules.md](responsibility-risk-rules.md)，不要从命令阈值推导重构结论。

## 路径与比较参数

- `--config <path>`：指定 TOML 配置，默认使用 `tools/scripts/loc/scan_lines.toml`。
- `--log-file <path>`：覆盖总报告路径。相对路径固定相对 `tools/scripts/loc/`，不是相对当前终端目录。
- `--baseline <path>`：读取以前的扫描 JSON 并在当前报告中生成 diff；相对路径相对仓库根目录。

输出布局和 baseline 字段见 [report-and-baseline.md](report-and-baseline.md)。

## 配置

配置文件是 `tools/scripts/loc/scan_lines.toml`，包含：

- 每种语言的扩展名、排除目录、默认路径和扫描阈值；
- `[scopes.*]` 下的 scope 路径及语言集合；
- responsibility analyzer 使用的阈值和策略开关。

文档不复制当前阈值清单，避免配置修改后产生第二份过期事实来源。需要确认实际值时直接读取 TOML。

## Windows BAT 快捷入口

语言级入口位于 `tools/scripts/loc/scripts/lang/`：

- `scan_cpp_over.bat`
- `scan_cpp_dir_over_files.bat`
- `scan_cpp_responsibility_risk.bat`
- `scan_kt_over.bat`
- `scan_kt_dir_over_files.bat`
- `scan_kt_responsibility_risk.bat`
- `scan_py_over.bat`
- `scan_py_dir_over_files.bat`
- `scan_py_responsibility_risk.bat`

Scope 入口位于 `tools/scripts/loc/scripts/`：

- `scan_scope_audio_api.bat`
- `scan_scope_audio_core.bat`
- `scan_scope_audio_io.bat`
- `scan_scope_audio_runtime.bat`
- `scan_scope_audio_android.bat`
- `scan_scope_audio_cli.bat`
- `scan_scope_audio_web.bat`

Scope BAT 只固定 `--scope`，其余参数透传。

## 示例

```powershell
# 按 scope 生成职责风险报告
python tools/scripts/loc/run.py --scope audio_core --responsibility-risk

# 检查 Android 两层目录内的文件数量
python tools/scripts/loc/run.py --scope audio_android --dir-over-files --dir-max-depth 2

# 按语言扫描小文件
python tools/scripts/loc/run.py --lang py --under

# 与上一次结果比较
python tools/scripts/loc/run.py --lang py --responsibility-risk --baseline tools/scripts/loc/logs/scan_py.json

# 使用 BAT，并继续传递扫描参数
tools/scripts/loc/scripts/scan_scope_audio_core.bat --responsibility-risk
```
