# C++ Refactor Goal

仓库职责定位：
- `libs/*`
- `apps/audio_android/app/src/main/cpp`
- 以业务逻辑、编码解码、metadata、bridge/interop 为主

通常负责什么：
- codec / parser / builder / metadata 规则
- native audio pipeline、binary contract、平台桥接、JNI/C ABI 边界
- 生命周期、ownership、resource cleanup

通常不负责什么：
- Compose/UI orchestration
- 页面级状态切换和视觉分发
- 单纯的脚手架命令编排

因此重构时优先关注：
- core rule、bridge、metadata、marshalling 是否边界不清
- ownership / lifecycle 是否和规则逻辑混写
- 改一个 contract 字段是否会牵动太多层
- 出问题时是否难以判断是规则、桥接还是资源管理的问题

核心目标：
- 提高业务逻辑和边界层的可维护性，而不是机械降低行数
- 让 codec/rule、metadata、bridge/ffi、resource lifecycle 的边界更清楚
- 降低改一个协议字段或格式规则时牵动整条链路的风险
- 提高 correctness、可验证性和契约清晰度

优先方向：
- 把 bridge/ffi glue 和 core rule/codec 拆开
- 把 metadata contract、marshalling、platform adapter 分开
- 把生命周期管理和规则逻辑分开
- 让“谁负责解析、谁负责转换、谁负责所有权、谁负责对外接口”更清楚

不以这些为目标：
- 不为了降行数而增加无意义抽象层
- 不把关键二进制规则藏进难以追踪的 helper 链
- 不把边界层和核心规则继续混写
- 不为了复用牺牲 ABI/契约清晰度

判断重构是否有效：
- 改 codec/metadata 时影响面是否更可控
- bridge 和 core 是否更容易独立测试
- 资源生命周期和 ownership 是否更清晰
- 出问题时是否更容易判断是规则问题、桥接问题还是平台问题
