# LOC Scanner

统一的代码行数 / 目录文件数扫描入口，位于 `tools/scripts/loc/`。

## 入口

- `python tools/scripts/loc/run.py`
- `tools/scripts/loc/loc.bat`

## Windows BAT 快捷入口

- `tools/scripts/loc/loc.bat`
  - 通用透传入口，等价于 `python tools/scripts/loc/run.py %*`
- `tools/scripts/loc/scan_cpp_over.bat`
  - 扫描 C++ 文件行数，默认使用 `scan_lines.toml` 中 `cpp.default_over_threshold`
- `tools/scripts/loc/scan_cpp_dir_over_files.bat`
  - 扫描 C++ 目录内代码文件数，默认使用 `cpp.default_dir_over_files`
- `tools/scripts/loc/scan_kt_over.bat`
  - 扫描 Kotlin 文件行数，默认使用 `kt.default_over_threshold`
- `tools/scripts/loc/scan_kt_dir_over_files.bat`
  - 扫描 Kotlin 目录内代码文件数，默认使用 `kt.default_dir_over_files`
- `tools/scripts/loc/scan_kt_responsibility_risk.bat`
  - 扫描 Kotlin 文件职责混杂风险，默认使用 `kt.default_responsibility_risk_threshold`
- `tools/scripts/loc/scan_py_responsibility_risk.bat`
  - 扫描 Python 文件职责混杂风险，默认使用 `py.default_responsibility_risk_threshold`
- `tools/scripts/loc/scan_py_complexity.bat`
  - Python 职责混杂/复杂度预警快捷入口，等价于 `scan_py_responsibility_risk.bat`
- `tools/scripts/loc/scan_py_over.bat`
  - 扫描 Python 文件行数，默认使用 `py.default_over_threshold`
- `tools/scripts/loc/scan_py_dir_over_files.bat`
  - 扫描 Python 目录内代码文件数，默认使用 `py.default_dir_over_files`

## 基本用法

在仓库根目录执行：

```powershell
python tools/scripts/loc/run.py --lang <cpp|kt|py|rs> [paths ...] [--over N | --under [N] | --dir-over-files [N] | --responsibility-risk [N]] [--dir-max-depth N] [--log-file <path>]
```

参数说明：

- `--lang`
  - 语言类型，当前仓库默认配置了 `cpp` / `kt` / `py`
- `paths`
  - 可选，扫描目录列表；未传时使用 `scan_lines.toml` 中该语言的 `default_paths`
- `--over N`
  - 扫描超过阈值的“大文件”
- `--under [N]`
  - 扫描低于阈值的“小文件”；不传 `N` 时使用 TOML 中该语言的 `default_under_threshold`
- `--dir-over-files [N]`
  - 扫描目录内代码文件数超过阈值的目录；不传 `N` 时使用 TOML 中该语言的 `default_dir_over_files`
- `--dir-max-depth N`
  - 目录扫描最大深度，仅对 `--dir-over-files` 生效
- `--responsibility-risk [N]`
  - 当前支持 Kotlin / Python 的“职责混杂风险”扫描；不传 `N` 时使用 TOML 中该语言的 `default_responsibility_risk_threshold`
  - Kotlin 第一版是保守启发式：综合文件行数、`remember* / mutableStateOf / LaunchedEffect`、顶层 `@Composable` 数量、`Section/Block/Card/Switcher/Timeline` 命名种类数，以及模式分支数量
  - Python 第一版是保守启发式：综合文件行数、状态/副作用信号、顶层 `def/class` 数量、角色命名种类数，以及 `mode/kind/type` 分支数量
- `-t, --threshold N`
  - 兼容旧参数，等价于 `--over N`
- `--log-file`
  - 自定义日志文件路径；相对路径相对 `tools/scripts/loc/`
- `--config`
  - 指定配置文件路径，默认是 `tools/scripts/loc/scan_lines.toml`

## 当前默认配置

配置文件：`tools/scripts/loc/scan_lines.toml`

当前已配置的默认扫描路径：

- `cpp`
  - `apps/audio_android/app/src/main/cpp`
  - `apps/audio_cli/windows/src`
  - `libs/audio_api`
  - `libs/audio_core`
  - `libs/audio_io`
  - `Test`
- `kt`
  - `apps/audio_android/app/src/main/java`
- `py`
  - `tools`

当前默认阈值：

- `cpp`
  - `default_over_threshold = 350`
  - `default_under_threshold = 120`
  - `default_dir_over_files = 16`
- `kt`
  - `default_over_threshold = 180`
  - `default_under_threshold = 100`
  - `default_dir_over_files = 8`
  - `default_responsibility_risk_threshold = 5`
- `py`
  - `default_over_threshold = 200`
  - `default_under_threshold = 120`
  - `default_dir_over_files = 10`
  - `default_responsibility_risk_threshold = 5`

## 示例

```powershell
python tools/scripts/loc/run.py --lang cpp
python tools/scripts/loc/run.py --lang py --under
python tools/scripts/loc/run.py --lang kt --dir-over-files --dir-max-depth 2
python tools/scripts/loc/run.py --lang kt --responsibility-risk
python tools/scripts/loc/run.py --lang kt --responsibility-risk 7
python tools/scripts/loc/run.py --lang py --responsibility-risk
tools\scripts\loc\scan_cpp_over.bat
tools\scripts\loc\scan_cpp_dir_over_files.bat
tools\scripts\loc\scan_kt_over.bat
tools\scripts\loc\scan_kt_responsibility_risk.bat
tools\scripts\loc\scan_py_responsibility_risk.bat
tools\scripts\loc\scan_py_complexity.bat
tools\scripts\loc\scan_py_dir_over_files.bat
tools\scripts\loc\scan_py_over.bat tools
```

## 日志输出

每次执行都会写 JSON 日志，默认输出到：

- `tools/scripts/loc/logs/scan_cpp.json`
- `tools/scripts/loc/logs/scan_kt.json`
- `tools/scripts/loc/logs/scan_py.json`
- `tools/scripts/loc/logs/scan_rs.json`

可通过 `--log-file` 覆盖，例如：

```powershell
python tools/scripts/loc/run.py --lang py --under 120 --log-file logs/loc_scan_py.json
```
