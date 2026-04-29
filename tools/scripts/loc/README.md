# LOC Scanner

统一的代码行数 / 目录文件数扫描入口，位于 `tools/scripts/loc/`。

## 入口

- `python tools/scripts/loc/run.py`
- `tools/scripts/loc/loc.bat`

建议先看：

- [tools/scripts/loc/docs/agent_responsibility_scan.md](/C:/code/WaveBits/tools/scripts/loc/docs/agent_responsibility_scan.md)
  - 解释职责混杂扫描 JSON 的字段含义，以及 agent 应该如何把这些字段转成重构方向

## Windows BAT 快捷入口

- `tools/scripts/loc/loc.bat`
  - 通用透传入口，等价于 `python tools/scripts/loc/run.py %*`
- `tools/scripts/loc/scan_cpp_over.bat`
  - 扫描 C++ 文件行数，默认使用 `scan_lines.toml` 中 `cpp.default_over_threshold`
- `tools/scripts/loc/scan_cpp_dir_over_files.bat`
  - 扫描 C++ 目录内代码文件数，默认使用 `cpp.default_dir_over_files`
- `tools/scripts/loc/scan_cpp_responsibility_risk.bat`
  - 扫描 C++ 文件职责混杂风险，默认使用 `cpp.default_responsibility_risk_threshold`
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
  - 当前支持 Kotlin / Python / C++ 的“职责混杂风险”扫描；不传 `N` 时使用 TOML 中该语言的 `default_responsibility_risk_threshold`
  - Kotlin 第一版是保守启发式：综合文件行数、`remember* / mutableStateOf / LaunchedEffect`、顶层 `@Composable` 数量、`Section/Block/Card/Switcher/Timeline` 命名种类数，以及模式分支数量
  - Python 当前版是保守启发式：综合文件行数、状态/副作用信号、顶层 `def/class` 数量、角色命名种类数、`mode/kind/type` 分支数量，以及下列更贴近“职责混杂”的信号：
    - `io_kind_count`
      - 一个文件同时命中的 IO / 副作用类别数，例如文件读写、控制台输出、子进程、网络、环境、序列化
    - `rule_helper_count`
      - 顶层 `validate/check/normalize/resolve/parse/encode/decode` helper、正则常量、全大写规则常量的密度
    - `responsibility_verb_kind_count`
      - 顶层函数是否同时覆盖“读取/解析、校验、定位、变更、展示”等多类动词簇
    - `command_layer_leak_hits`
      - 文件位于 `commands/` 层时，是否仍然混入过多底层规则 helper 和多种 IO
  - Python 风险结果现在会额外输出：
    - `dominant_risks`
      - 主导风险类型，方便 agent 或人工快速判断是“命令层泄漏”“IO 过宽”还是“规则 helper 过密”
    - `suggestion`
      - 一句方向性建议，例如“把底层规则下沉到 core，命令层只保留编排和结果输出”
    - `next_action`
      - 更具体的第一步动作，例如“先把校验/规范化下沉到 core module，再让 command 调用它”
  - C++ 第一版也是保守启发式：综合文件行数、共享状态/线程原语、顶层符号数量、角色命名种类数、`mode/style/state` 分支数量，以及更贴近桥接/大实现文件的信号：
    - `interop_surface_hits`
      - 一个文件同时命中的桥接/FFI 面数量，例如 JNI 表面、C ABI 表面、封送 helper
    - `resource_lifecycle_hits`
      - 资源 ownership / 释放 / cancel / destroy / lock 等生命周期信号密度
    - `rule_helper_count`
      - 顶层 `Validate/Parse/Encode/Decode/Build/Map/Take/Poll/Cancel/Destroy/Free` helper 与规则常量密度
    - `responsibility_verb_kind_count`
      - 顶层函数是否同时覆盖“桥接转换、校验解析、编码解码、生命周期、读写装配”等多类动词簇
  - C++ 风险结果也会额外输出：
    - `dominant_risks`
      - 主导风险类型，例如 `interop_surface_breadth`、`resource_lifecycle_density`
    - `suggestion`
      - 一句方向性建议，例如“把桥接/封送代码与编码规则 helper 分开，保留一层薄协调面”
    - `next_action`
      - 更具体的第一步动作，例如“先判断该文件是否只是边界 glue；如果是，就保留薄桥接，把转换规则移出”
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
  - `default_responsibility_risk_threshold = 5`
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
python tools/scripts/loc/run.py --lang cpp --responsibility-risk
python tools/scripts/loc/run.py --lang py --under
python tools/scripts/loc/run.py --lang kt --dir-over-files --dir-max-depth 2
python tools/scripts/loc/run.py --lang kt --responsibility-risk
python tools/scripts/loc/run.py --lang kt --responsibility-risk 7
python tools/scripts/loc/run.py --lang py --responsibility-risk
tools\scripts\loc\scan_cpp_over.bat
tools\scripts\loc\scan_cpp_dir_over_files.bat
tools\scripts\loc\scan_cpp_responsibility_risk.bat
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

注意：

- `--log-file` 的相对路径不是相对“你当前在哪个终端目录执行命令”，而是固定相对 `tools/scripts/loc/`
- 例如：
  - `--log-file logs/scan_py.json`
    - 实际落到 [tools/scripts/loc/logs/scan_py.json](/C:/code/WaveBits/tools/scripts/loc/logs/scan_py.json)
  - `--log-file scan_py_custom.json`
    - 实际落到 [tools/scripts/loc/scan_py_custom.json](/C:/code/WaveBits/tools/scripts/loc/scan_py_custom.json)
- 如果你不想依赖这条相对路径规则，最稳的是直接传绝对路径

职责混杂扫描写出的 JSON 日志中，风险条目除了原有的 `score / lines / state_signal_hits / top_level_composables / role_kinds / mode_branch_hits`，还会按语言带出额外字段。

Python 额外字段：

- `io_kind_count`
- `rule_helper_count`
- `responsibility_verb_kind_count`
- `command_layer_leak_hits`
- `dominant_risks`
- `suggestion`
- `next_action`

C++ 额外字段：

- `interop_surface_hits`
- `resource_lifecycle_hits`
- `rule_helper_count`
- `responsibility_verb_kind_count`
- `dominant_risks`
- `suggestion`
- `next_action`

这样终端输出和 `tools/scripts/loc/logs/scan_py.json` 里的结构会保持一致，方便 agent 直接读取日志做后续分析。

另外，职责混杂条目的 JSON 字段顺序现在也按“先结论、后证据”排列：

1. `path`
2. `score`
3. `priority`
4. `summary`
5. `dominant_risks`
6. `suggestion`
7. `next_action`
8. 其余计数型证据信号
