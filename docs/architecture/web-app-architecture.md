# Web Application Architecture

更新时间：2026-08-10

## 目的

本文说明 `apps/audio_web` 的稳定职责边界和依赖方向。具体页面文案、发布变化和构建参数分别由 i18n、history 与 `docs/notes/web/` 维护，不在本文重复。

## 总体依赖方向

```text
index.html / app.js
        |
        v
AppController --------> UiController / SampleController / SampleView
        |
        v
EncoderClient
        |
        v
encode-worker.js
        |
        v
site/wasm/flipbits_web.js
        |
        v
flipbits_web_bridge.cpp
        |
        v
bag_api.h -> libs
```

依赖只沿图中方向下沉。DOM、浏览器状态和展示文案不得进入 worker、native bridge 或 `libs`；mode、codec、follow、encode lifecycle 和进度语义不得在 JavaScript presentation 中重新实现。

## 各层职责

### Composition root

- `site/index.html` 定义静态页面结构和资源入口。
- `site/js/app.js` 只负责取得 DOM、构造 owner、接线并启动应用。
- composition root 不承载 feature 流程、Wasm 调用或领域判断。

### Application 与 UI

- `AppController` 拥有页面级 workflow、事件编排、当前生成结果、录音/文件输入和 Object URL 等浏览器资源生命周期。
- `UiController` 把应用状态渲染到 DOM，维护 locale、status、progress 和 result summary 的展示状态。
- sample controller/service/view 共同拥有 sample 文本加载、随机化和显示；它们不拥有 encode 规则。
- `request-form.js` 负责读取与规范化表单输入。只有真正属于 Web 表单的选择策略可以留在这里；共享 mode validation 必须来自 `libs` 契约。
- `audio-utils.js` 是浏览器音频 adapter，负责文件解码、mono PCM16/目标采样率转换和 WAV Blob。它不实现 Voice FX 或 transport 算法。

UI owner 可以按独立 workflow、独立资源生命周期或可单独测试的 view model 拆分，不按文件行数、DOM 元素数量或单个私有 helper 拆分。

### Worker 与 client

- `EncoderClient` 拥有 worker request id、pending request 和 Promise/callback 映射。
- `encode-worker.js` 拥有 Wasm runtime 初始化、operation pump loop、进度消息节流、terminal result 获取和 best-effort abort。
- worker 可以决定每次 pump 的预算以及多久向主线程发送一次 snapshot，但不能改变 snapshot/work-plan 的状态、phase、完成度和错误语义。
- 性能 diagnostics 可以记录或近似归因耗时；近似值必须明确只用于诊断，不能进入用户进度、成功判定或业务分支。
- 文件型 Voice FX 使用 offline canonical API。streaming processor 只用于未来明确的 live/block workflow，不能替代上传文件处理。

`begin / pump / terminal check / take result / abort` 是同一个 operation owner，不能为了降低复杂度或行数拆成互不负责清理的文件。

### Wasm 与 native bridge

- `site/wasm/flipbits_web.js` 把 Emscripten 导出映射为 worker 可消费的 JavaScript surface，并负责 Wasm memory 的参数/结果转换。
- `src/flipbits_web_bridge.cpp` 是 `bag_api.h` 到 Emscripten export 的 ABI adapter，负责 config 转换、当前 operation/result storage、错误映射和资源释放。
- bridge 不定义第二套 encode lifecycle、mode 规则、progress phase 或 Voice FX 算法。
- ABI 数值、状态和 ownership 必须与 `bag_api.h` 保持一致。新增 getter 或 DTO 时，应优先保持一个 contract family 的转换内聚，不能按每个标量 getter 创建微文件。

## 共享契约

- encode lifecycle、snapshot、work-plan 与 pump：`docs/architecture/encode-operation-contract.md`
- token / character / byte follow：`docs/architecture/text-follow-contract.md`
- mode 与 transport：`docs/design/transports.md`、`docs/design/modes/README.md`
- Voice FX offline/streaming 口径：`libs/AGENTS.md`

这些文档和 `libs` 公共 API 是事实来源。Web 可以决定如何展示共享状态，但不能推导另一套领域状态。

## 资源与错误 ownership

- `AppController` 负责释放被替换的 Object URL、停止录音 stream 并清理已选输入。
- `EncoderClient` 负责 request 完成后移除 pending entry。
- worker 负责失败时尽力 abort 当前 native operation。
- Wasm wrapper/native bridge 负责与各自分配位置匹配的 memory 和 native result cleanup。
- 错误应沿 bridge -> worker -> client -> application -> UI 传播，不在中间层转换为伪成功或本地 progress state。

## 验证

修改 JavaScript、Web 工具或 Web contract adapter后，至少运行：

```powershell
python tools/run.py web test
```

修改 Wasm bridge、公共 ABI 或共享库接线后，还应按需要运行：

```powershell
python tools/run.py web build-wasm
python tools/run.py verify --build-dir build/dev --skip-android
```

本地交互和浏览器资源生命周期需要人工检查时，预览命令见 `docs/notes/web/cmd.md`。
