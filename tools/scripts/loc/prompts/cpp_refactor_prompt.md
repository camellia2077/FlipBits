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
- 对 `libs/*` 坚持 module-first：内部 C++ 新职责边界优先使用 named module / `import`，通常落在 `modules/bag/.../*.cppm` + `src/.../*.cpp`
- module-first 不是 module-only：公共 C ABI、JNI/wasm bridge、平台 adapter、generated include、以及 Android/Web toolchain 不兼容 modules 的路径，仍然需要保留 header/include 或 include fallback
- 新增 `libs/*` module 边界时，同步考虑 include fallback / build feature switch，确保 Android/Web 在关闭 module 路径时仍可编译
- 不要为了“全 module”破坏 C 接口、JNI/wasm bridge 或跨平台构建能力；也不要新增 `.inc` 作为新的职责边界
- 对 Android JNI / app native bridge，可按 `.cpp/.h` 拆编译单元，但仍要围绕 DTO marshalling、list/scalar marshalling、lifecycle、entrypoint 分包
- 把 bridge/ffi glue 和 core rule/codec 拆开
- 把 metadata contract、marshalling、platform adapter 分开
- 把生命周期管理和规则逻辑分开
- 读取扫描报告时优先看 `Move Sets`，把同一迁移包里的 helpers 成组移动，不要按单个小函数碎拆
- Move Set 落地后必须复扫：若新文件仍是 P1，再按包内子职责继续拆；若只剩 P2 且风险来自天然边界层 interop，可停止
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
