# Python Refactor Goal

仓库职责定位：
- `tools/`
- 以脚手架、扫描、报告、辅助开发脚本为主
- 更偏命令编排和工具链，而不是核心业务域模型

通常负责什么：
- 命令入口、扫描流程、报告输出、路径与配置处理、开发辅助自动化
- data model / formatter / writer / CLI orchestration

通常不负责什么：
- 核心 runtime product logic
- native codec / metadata contract
- Compose UI 状态与渲染编排

因此重构时优先关注：
- command layer 是否过厚
- 规则判断、路径处理、输出格式是否耦合
- 新增一个格式或规则时是否需要同时改很多处
- 脚本是否难测、难调试、难批量演进

核心目标：
- 提高脚本和工具链的可演进性，而不是机械降低行数
- 让 command layer 薄一些，规则判断、路径处理、输出格式化更分离
- 降低“新增一个输出格式或一条规则要同时改很多处”的成本
- 提高测试性、批处理稳定性和调试清晰度

优先方向：
- 把参数解析、路径解析、规则判断、数据模型、writer 分开
- 把 command/CLI 层缩成编排入口
- 把 helper/rule/formatter 从命令层中下沉
- 让数据和输出格式解耦，便于新增 JSON/MD/console 之外的产物

不以这些为目标：
- 不为了“像框架”而增加复杂抽象
- 不把简单脚本硬拆成过多文件
- 不为了复用制造更深的调用链
- 不把 command 层继续做成规则和 IO 的总集散地

判断重构是否有效：
- 新增一种输出或规则时改动是否更局部
- CLI 层是否更薄
- writer / formatter / rule 是否更容易单测
- 出错时是否更容易判断是参数、规则、路径还是输出问题
