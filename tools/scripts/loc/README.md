# LOC Scanner

统一的代码行数 / 目录文件数扫描入口，位于 `tools/scripts/loc/`。

## 入口

- `python tools/scripts/loc/run.py`
- `tools/scripts/loc/scripts/` 下的 Windows BAT 快捷入口

## 按架构 Scope 扫描

除了按语言扫描，还可以按仓库架构边界扫描。scope 会复用每种语言现有的扫描器，并把结果聚合到同一份报告的 `parts` 中：

```powershell
python tools/scripts/loc/run.py --scope audio_api --responsibility-risk
python tools/scripts/loc/run.py --scope audio_core --responsibility-risk
python tools/scripts/loc/run.py --scope audio_android --responsibility-risk
python tools/scripts/loc/run.py --scope audio_web --over 250
```

当前 scope：

- `audio_api` -> `libs/audio_api`（C++）
- `audio_core` -> `libs/audio_core`（C++）
- `audio_io` -> `libs/audio_io`（C++）
- `audio_runtime` -> `libs/audio_runtime`（C++）
- `audio_android` -> `apps/audio_android`（C++ + Kotlin）
- `audio_cli` -> `apps/audio_cli`（Rust）
- `audio_web` -> `apps/audio_web`（C++ + JavaScript）

`--lang` 和 `--scope` 必须二选一。`--responsibility-risk` 仍只对现有的 C++ / Kotlin / Python analyzer 生效；Rust / JavaScript scope 可使用行数和目录文件数扫描。

scope 默认报告写入：

- `tools/scripts/loc/logs/scopes/<scope>/scan.json`
- `tools/scripts/loc/logs/scopes/<scope>/scan.md`

scope JSON 根节点包含 `scope`、`display_name` 和 `parts`。每个 part 保留原语言报告的 `lang`、`scan`、`results` 和 summary，因此语言级 baseline 仍可继续使用，scope baseline 会按 `part + path` 区分同名文件。

Scope 快捷入口位于 `tools/scripts/loc/scripts/`，例如：

```powershell
tools/scripts/loc/scripts/scan_scope_audio_core.bat --responsibility-risk
tools/scripts/loc/scripts/scan_scope_audio_android.bat --over 300
```

建议先看：

- [tools/scripts/loc/docs/agent_responsibility_scan.md](docs/agent_responsibility_scan.md)
  - 解释职责混杂扫描 JSON 的字段含义，以及 agent 应该如何把这些字段转成重构方向

语言级重构指导位于：

- `tools/scripts/loc/docs/languages/kotlin_refactor.md`
- `tools/scripts/loc/docs/languages/cpp_refactor.md`
- `tools/scripts/loc/docs/languages/python_refactor.md`

这些是固定文档，不会在每次扫描后复制到日志目录。Scope 级边界建议位于：

- `tools/scripts/loc/docs/scopes/audio_api.md`
- `tools/scripts/loc/docs/scopes/audio_core.md`
- `tools/scripts/loc/docs/scopes/audio_io.md`
- `tools/scripts/loc/docs/scopes/audio_runtime.md`
- `tools/scripts/loc/docs/scopes/audio_android.md`
- `tools/scripts/loc/docs/scopes/audio_cli.md`
- `tools/scripts/loc/docs/scopes/audio_web.md`

阅读扫描结果时，先看对应的 scope guide，再按文件语言补充阅读语言级重构指导。扫描日志只保存结果、明细和 baseline/diff。

## Windows BAT 快捷入口

- 语言级入口位于 `tools/scripts/loc/scripts/lang/`：
- `tools/scripts/loc/scripts/lang/scan_cpp_over.bat`
  - 扫描 C++ 文件行数，默认使用 `scan_lines.toml` 中 `cpp.default_over_threshold`
- `tools/scripts/loc/scripts/lang/scan_cpp_dir_over_files.bat`
  - 扫描 C++ 目录内代码文件数，默认使用 `cpp.default_dir_over_files`
- `tools/scripts/loc/scripts/lang/scan_cpp_responsibility_risk.bat`
  - 扫描 C++ 文件职责混杂风险，默认使用 `cpp.default_responsibility_risk_threshold`
- `tools/scripts/loc/scripts/lang/scan_kt_over.bat`
  - 扫描 Kotlin 文件行数，默认使用 `kt.default_over_threshold`
- `tools/scripts/loc/scripts/lang/scan_kt_dir_over_files.bat`
  - 扫描 Kotlin 目录内代码文件数，默认使用 `kt.default_dir_over_files`
- `tools/scripts/loc/scripts/lang/scan_kt_responsibility_risk.bat`
  - 扫描 Kotlin 文件职责混杂风险，默认使用 `kt.default_responsibility_risk_threshold`
- `tools/scripts/loc/scripts/lang/scan_py_responsibility_risk.bat`
  - 扫描 Python 文件职责混杂风险，默认使用 `py.default_responsibility_risk_threshold`
- `tools/scripts/loc/scripts/lang/scan_py_over.bat`
  - 扫描 Python 文件行数，默认使用 `py.default_over_threshold`
- `tools/scripts/loc/scripts/lang/scan_py_dir_over_files.bat`
  - 扫描 Python 目录内代码文件数，默认使用 `py.default_dir_over_files`

- scope 级入口位于 `tools/scripts/loc/scripts/`：
  - `scan_scope_audio_api.bat`
  - `scan_scope_audio_core.bat`
  - `scan_scope_audio_io.bat`
  - `scan_scope_audio_runtime.bat`
  - `scan_scope_audio_android.bat`
  - `scan_scope_audio_cli.bat`
  - `scan_scope_audio_web.bat`

scope 入口只固定 `--scope`，其余参数透传。例如：

```powershell
tools/scripts/loc/scripts/scan_scope_audio_core.bat --responsibility-risk
tools/scripts/loc/scripts/scan_scope_audio_android.bat --dir-over-files --dir-max-depth 2
```

## 基本用法

在仓库根目录执行：

```powershell
python tools/scripts/loc/run.py (--lang <cpp|kt|py|rs|js|md> | --scope <scope>) [paths ...] [--over N | --under [N] | --dir-over-files [N] | --responsibility-risk [N]] [--dir-max-depth N] [--log-file <path>] [--baseline <path>]
```

参数说明：

- `--lang`
  - 语言类型，当前仓库默认配置了 `cpp` / `kt` / `py` / `rs` / `js` / `md`
- `--scope`
  - 按架构边界扫描；scope 定义位于 `scan_lines.toml` 的 `[scopes.<name>]`
  - 一个 scope 可以包含多个语言，输出会按语言 part 分段
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
  - 建议阅读顺序统一为：
    - `priority`
    - `summary`
    - `dominant_risks`
    - `suggestion`
    - `next_action`
    - `lines`
    - 其余计数型证据
  - `lines` 保留，因为大文件信号本身仍然有价值；但它只是结论旁边的体量信号，不应替代风险类型判断
  - C++ 版也是保守启发式：综合文件行数、共享状态/线程原语、C++ 符号数量、角色命名种类数、`mode/style/state` 分支数量，以及更贴近桥接/大实现文件的信号：
    - `interop_surface_hits`
      - 一个文件同时命中的桥接/FFI 面数量，例如 JNI 表面、C ABI 表面、JNI/C ABI 上下文中的封送 helper
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
- `--baseline`
  - 指定之前生成的扫描 JSON；相对路径相对仓库根目录
  - 当前扫描会在 JSON / Markdown 中增加 `added / removed / changed / unchanged` diff
  - scope 报告会按 `part + kind + path` 生成 diff key，避免 C++ / Kotlin 等 part 的同名路径互相覆盖
- `--config`
  - 指定配置文件路径，默认是 `tools/scripts/loc/scan_lines.toml`

## 当前默认配置

配置文件：`tools/scripts/loc/scan_lines.toml`

当前已配置的默认扫描路径：

- `cpp`
  - `apps/audio_android/app/src/main/cpp`
  - `libs/audio_api`
  - `libs/audio_core`
  - `libs/audio_io`
  - `Test`
- `kt`
  - `apps/audio_android/app/src/main/java`
- `py`
  - `tools`
- `rs`
  - `apps/audio_cli`
- `js`
  - `apps/audio_web`

当前已配置的 scope 位于 `scan_lines.toml` 的 `[scopes.*]` 节点；scope 的默认路径和语言集合由 scope 配置统一定义。

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
python tools/scripts/loc/run.py --lang py --responsibility-risk --baseline tools/scripts/loc/logs/scan_py.json
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

同时会额外生成同名 Markdown 总报告，例如：

- `tools/scripts/loc/logs/scan_kt.md`

scope 扫描会生成独立目录，例如：

- `tools/scripts/loc/logs/scopes/audio_android/scan.json`
- `tools/scripts/loc/logs/scopes/audio_android/scan.md`

同时会在 `tools/scripts/loc/logs/` 下直接写出“按语言 / 扫描等级 / 具体文件”的明细 JSON：

- 目录层级：
  - 第一层：语言/代码类型目录，例如 `cpp / kotlin / py`
  - 第二层：扫描等级，例如 `P0 / P1 / P2 / P3`
- 例如：
  - `tools/scripts/loc/logs/kotlin/P2/AudioFlashSignalVisualizer_scan.json`
- 目录扫描结果会写成 `_dir_scan.json`
- 每个明细 JSON 旁边也会同步生成 Markdown：
  - `.../AudioFlashSignalVisualizer_scan.md`
- Kotlin 明细目录根名固定使用 `kotlin`
- 具体文件名只保留源码文件名本身；完整绝对路径保留在 JSON / Markdown 内容里

当前输出链路已经收口为：

- 扫描层：统一结果模型
- formatter 层：统一结论/证据分组
- writer 层：JSON / Markdown
- console 层：也消费同一套 formatter

也就是说，console 和 Markdown 现在共享同一套展示语义，不再各自拼一套字段解释。

职责混杂扫描现在按四层信息输出，目标是让 agent 直接定位问题而不是再猜：

1. 文件级结论
   - `priority / score / summary / dominant_risks / suggestion / next_action / lines`
2. 函数级热点
   - `function_hotspots`
   - 指出哪个函数或 composable 更值得先拆，附 `score / lines / summary / risks / evidence`
3. C++ 迁移包
   - `move_sets`
   - 把同一 owner 下应该一起移动的 helper 明确列成一组，附目标 module/file、原因和验证命令
4. 行号级锚点
   - `anchors`
   - 指出哪一行附近出现了关键的 state/effect/mode 分支/绘制分发信号

当前这套输出已覆盖：

- Kotlin 职责风险扫描
- Python 职责风险扫描
- C++ 职责风险扫描

对 agent 来说，建议读取顺序是：

1. 文件级结论
2. 函数级热点
3. C++ `move_sets` 迁移包
4. 行号级锚点
5. 其余计数型 evidence

可通过 `--log-file` 覆盖，例如：

```powershell
python tools/scripts/loc/run.py --lang py --under 120 --log-file logs/loc_scan_py.json
```

注意：

- `--log-file` 的相对路径不是相对“你当前在哪个终端目录执行命令”，而是固定相对 `tools/scripts/loc/`
- 例如：
  - `--log-file logs/scan_py.json`
    - 实际落到 [tools/scripts/loc/logs/scan_py.json](logs/scan_py.json)
  - `--log-file scan_py_custom.json`
    - 实际落到 [tools/scripts/loc/scan_py_custom.json](scan_py_custom.json)
- 如果你不想依赖这条相对路径规则，最稳的是直接传绝对路径
- 如果你传的是 `--log-file logs/custom_scan_kt.json`
  - 总日志会落到 `tools/scripts/loc/logs/custom_scan_kt.json`
  - 明细日志仍会统一落到 `tools/scripts/loc/logs/<lang>/P*/` 下

职责混杂扫描写出的 JSON 日志中，风险条目现在按“先结论、后证据”排序，优先字段是：

- `path`
- `score`
- `priority`
- `summary`
- `dominant_risks`
- `suggestion`
- `next_action`
- `confidence`
- `decision`
- `lines`

然后才是各语言的计数型证据字段，例如 `state_signal_hits / top_level_composables / mode_branch_hits / io_kind_count` 等。

Python 额外字段：

- `io_kind_count`
- `rule_helper_count`
- `responsibility_verb_kind_count`
- `command_layer_leak_hits`
- `dominant_risks`
- `suggestion`
- `next_action`

所有职责风险条目还包含：

- `confidence`
  - 当前证据组数量对应的置信度：`low` / `medium` / `high`
- `decision`
  - 初步动作建议：`inspect` 或 `refactor_candidate`

`confidence` 和 `decision` 只用于排序和下一步决策，不替代 agent 对源码边界的人工确认。

使用 `--baseline` 时，报告根部会包含：

- `baseline`
  - 基线 JSON 的绝对路径
- `diff.summary`
  - added / removed / changed / unchanged 的数量
- `diff.entries`
  - 文件或目录的变化状态；changed 条目包含 lines、priority、score、confidence、decision 等字段变化

C++ 额外字段：

- `interop_surface_hits`
- `resource_lifecycle_hits`
- `rule_helper_count`
- `responsibility_verb_kind_count`
- `dominant_risks`
- `suggestion`
- `next_action`

终端输出现在故意保持简短，只用于快速扫一眼：

- 首行只保留 `score / lines / File`
- 第二行只保留 `summary` 和 `risks`
- 第三行打印对应明细 Markdown 的绝对路径，方便在 IDE 终端里直接点开

更详细的 `suggestion / next_action / function_hotspots / move_sets / anchors / evidence` 都保留在 JSON / Markdown 日志里，不再在终端展开。

Markdown 明细报告还会额外给 agent 可执行的重构辅助信息：

- `stop_signal`
  - 建议继续、暂停或人工复核，避免只为了降低 score 继续拆文件。
  - `validation_hints`
  - 给出最小验证方向，例如 Kotlin 编译、Android JNI build、`audio_api` focused test 或 host verify。
- `False Positive Notes`
  - 说明 Compose state、mode branch 等可能是框架内正常信号，不应机械追数。
- `Responsibility Clusters`
  - 把热点按职责聚类，例如 dialog/import/export、Canvas runtime、state orchestration。
- `Move Sets`
  - C++ 报告把同一迁移包内的 helper 名称和行号列在一起，例如 JNI follow marshalling、Flash signal layout、WAV metadata parse。
  - 这比单个 hotspot 更适合指导 module-first 提取：一次只选一个 move set，整体迁移后立刻验证。
- `Suggested Extraction Candidates`
  - 给出候选 owner、行号、建议边界、风险和验证方式；一次重构通常只选一个候选。

另外，职责混杂条目的 JSON 字段顺序现在也按“先结论、后证据”排列：

1. `path`
2. `score`
3. `priority`
4. `summary`
5. `dominant_risks`
6. `suggestion`
7. `next_action`
8. `lines`
9. 其余计数型证据信号
