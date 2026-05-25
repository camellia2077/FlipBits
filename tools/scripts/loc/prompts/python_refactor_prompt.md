# Python Refactor Goal

仓库职责定位：
- `tools/`
- 以脚手架、扫描、报告、辅助开发脚本、仓库构建和测试编排为主
- 更偏命令编排和工具链，而不是核心业务域模型

通常负责什么：
- 命令入口、扫描流程、报告输出、路径与配置处理、开发辅助自动化
- 仓库级 build / test / verify / web / android / cli 等工具链编排
- data model / formatter / writer / CLI orchestration
- 对外部工具的薄封装，例如 Gradle、CMake、Ninja、Cargo、npm、Playwright、ADB、Emscripten、Python unittest

通常不负责什么：
- 核心 runtime product logic
- native codec / metadata contract
- Compose UI 状态与渲染编排
- Web/Android/CLI 的业务规则本体；tools 只负责编排、校验和生成辅助产物

因此重构时优先关注：
- command layer 是否过厚
- 规则判断、路径处理、输出格式是否耦合
- 新增一个格式或规则时是否需要同时改很多处
- 脚本是否难测、难调试、难批量演进
- 构建/测试命令是否把工具查找、环境变量、缓存路径、产物路径、进程执行和报告输出混在同一个函数
- ADB、npm、Playwright、Emscripten、Gradle 等平台 IO 是否被包在可替换、可测试的小边界里
- `tools/repo_tooling/commands` 是否继续变成所有底层规则 helper 的集散地
- 目录文件数过多时，是否是 command 模块过多、还是缺少按领域拆分的子包

核心目标：
- 提高脚本和工具链的可演进性，而不是机械降低行数
- 让 command layer 薄一些，规则判断、路径处理、输出格式化更分离
- 降低“新增一个输出格式或一条规则要同时改很多处”的成本
- 提高测试性、批处理稳定性和调试清晰度
- 让构建/测试编排稳定可复现：命令、cwd、env、cache、artifact path 都应该可追踪、可单测或可集成验证
- 让失败归因清晰：参数错误、工具缺失、环境缺失、外部进程失败、产物校验失败应有不同边界和错误信息

优先方向：
- 把参数解析、路径解析、规则判断、数据模型、writer 分开
- 把 command/CLI 层缩成编排入口
- 把 helper/rule/formatter 从命令层中下沉
- 让数据和输出格式解耦，便于新增 JSON/MD/console 之外的产物
- 把外部工具执行抽成 executor/process boundary；command 层只决定“跑什么”，不要直接散落 `subprocess.run`
- 把工具发现、环境解析、缓存/产物路径策略拆到独立模块，避免 command 函数同时读 env、拼路径、打印、执行进程
- 把纯规则 helper 做成无副作用函数，并优先补 focused unit tests
- 对 Web 工具链，优先把 sample text 规则、Emscripten 路径/缓存规则、Playwright 测试编排与 `cmd_web` 分开
- 对 Android debug 工具链，优先把 ADB 命令构造、设备状态解析、logcat dump、报告写入分开
- 对 LOC 报告工具链，优先把 scan mode 分发、cluster/move-set 格式化、risk 文案和 Markdown/console 展示分开

不以这些为目标：
- 不为了“像框架”而增加复杂抽象
- 不把简单脚本硬拆成过多文件
- 不为了复用制造更深的调用链
- 不把 command 层继续做成规则和 IO 的总集散地
- 不为了减少行数拆掉一个稳定的单一职责流程；扫描报告是预警，不是强制拆分命令
- 不把外部工具适配伪装成纯业务规则；进程、文件系统、网络、设备交互要留在明确的 IO 边界
- 不把构建产物、测试产物、缓存目录硬编码到多个地方；路径策略应集中并尊重 `build/` 目录约定

判断重构是否有效：
- 新增一种输出或规则时改动是否更局部
- CLI 层是否更薄
- writer / formatter / rule 是否更容易单测
- 出错时是否更容易判断是参数、规则、路径还是输出问题
- 构建/测试命令是否仍然能通过根入口运行，例如 `python tools/run.py test-lib ...`、`python tools/run.py web test`
- 外部命令调用是否更容易 mock，真实命令是否仍有端到端验证
- 扫描报告中的 `command_layer_leak`、`io_surface_breadth`、`rule_helper_density` 是否下降，而不是只让行数下降

当前扫描结果给出的优先级提示：
- P0 `tools/repo_tooling/commands/web.py`：优先拆 command 层泄漏。把工具查找、Emscripten/cache 规则、sample text 导出、Playwright 测试执行从 command 文件移到 web 专属 core/service 模块，保留 `cmd_web` 作为薄分发入口。
- P1 `tools/repo_tooling/android_debug/capture.py`：优先拆 ADB/process IO 和纯结果模型。命令构造、设备解析、logcat dump、文件/console 输出不要继续堆在同一模块。
- P2 `tools/scripts/loc/internal/report_formatter.py`：优先按 scan mode 或 formatter responsibility 拆，而不是拆单个小 helper。保持 Markdown/console 文案行为一致，并用现有扫描输出做回归样例。
- 目录扫描显示 `tools/repo_tooling/commands` 文件数偏多：新增命令前先判断是否应该放进领域子包，例如 `web/`、`android_debug/`、`build/`、`test/`，commands 目录只保留稳定 CLI adapter。
