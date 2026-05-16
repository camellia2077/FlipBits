# Kotlin Refactor Goal

仓库职责定位：
- `apps/audio_android/app/src/main/java`
- 以 Compose Android UI 为主
- 基本不承载核心业务规则

通常负责什么：
- 页面结构、交互状态、视觉呈现、播放显示、theme/config UI
- Compose state holder、render path、overlay、UI mapping

通常不负责什么：
- 核心编码/解码业务规则
- 底层 metadata contract
- 跨平台 bridge/interop 规则

因此重构时优先关注：
- UI orchestration 是否过深
- state/effect/render decision 是否混在一起
- Composable 主体是否同时承担太多职责
- 显示模式、视觉分支、overlay 逻辑是否难以局部修改

核心目标：
- 提高 UI 可维护性，而不是机械降低行数
- 提高状态、effect、mode/style 决策的可定位性
- 让 Composable 主体更浅，减少同时承担渲染、分支、状态、副作用的情况
- 让 render path、overlay、telemetry、state holder 更容易独立调整

优先方向：
- 把 state/mode/style decision 下沉到小模型或 state holder
- 把绘制分发、overlay 组装、telemetry 记录从主 Composable 拆开
- 把可复用的 UI 规则和 mapping 从页面 orchestration 中抽离
- 保持主流程可读，减少“读一个 Composable 要同时理解五种职责”

不以这些为目标：
- 不以单纯减少文件行数为目标
- 不为了拆分而拆分成过多小组件
- 不为了复用而制造更重的跳转成本
- 不把简单 UI 逻辑过度抽象成难读的框架层

判断重构是否有效：
- 主 Composable 是否更浅
- 状态和副作用是否更容易定位
- mode/style 分支是否更集中
- 改一个显示模式或视觉细节时，影响面是否更局部

重构停止条件：
- 当主 Composable 已经明显从“大杂烩”变成“装配器 + 少量渲染入口”时，应停止继续细拆
- 当 state/effect/render decision 已经能快速定位，后续开发不再需要同时改很多职责时，应停止继续细拆
- 当下一步重构的收益主要是“结构更整齐”而不是“后续需求更容易改”时，应停止
- 当继续拆分会引入更多参数穿透、更多跳转成本、更多跨文件联动时，应停止
- 当继续拆分开始接近视觉实现细节，而不是职责边界问题时，应停止

执行边界建议：
- 一次只处理同一条 UI 链路里的一个层级
- 一个文件连续做 2-4 小步后，应重新评估是否还值得继续
- 如果当前文件的主流程、关键分支、状态入口都已经清晰，就优先转去下一条更痛的链路，而不是继续深挖
