# apps/audio_web Agent Rules

## Role

- `apps/audio_web` 是 `WaveBits` 的 Web presentation / GitHub Pages demo 入口。
- 这里的文档只做导航，不重复写实现细节、视觉规则、构建细节或发布记录。
- 真实说明统一收口到仓库根目录 `docs/`。

## Hard Rules

- `apps/audio_web` 是 presentation 层，不定义第二套 encode lifecycle、progress state 或 mode-specific 进度语义。
- Web 侧音频生成进度必须直接消费 `libs` 当前对外提供的统一 `encode operation / snapshot / work-plan / pump` 契约。
- 不要兼容旧的进度显示用法。旧的 callback、旧的 job 命名、旧的 percent 计算方式、旧的 polling DTO、旧的 phase 映射都应直接删除，不保留兼容分支。
- 如果 `libs` 的 encode contract 发生不兼容变更，Web presentation 应直接迁移到新 contract，而不是在 `site/js/` 或 wasm bridge 里叠加适配层。
- Web UI 展示的进度、phase、可完成态和失败态，必须以 `libs` snapshot 为唯一事实来源，不允许本地伪造“看起来更平滑”的旧式进度状态机。

## First Read

- 想先看 presentation 版本更新，读：
  - `docs/presentation/web/2026-05-22.md`
- 想看仓库总文档导航，读：
  - `docs/README.md`
- 想看 transport / mode 设计语义，读：
  - `docs/design/transports.md`
  - `docs/design/modes/README.md`
  - `docs/design/modes/mini.md`
  - `docs/design/modes/pro.md`
  - `docs/design/modes/ultra.md`
  - `docs/design/modes/flash/README.md`
- 想看 Web 本地构建和预览命令，读：
  - `docs/notes/web/cmd.md`
- 想看 libs 的 toolchain capability / `import std` 兼容策略，读：
  - `docs/notes/toolchain-capabilities.md`
  - `docs/notes/toolchain-capability-regression-checklist.md`

## Code Map

- 改静态页面结构或样式，先看：
  - `site/index.html`
  - `site/styles.css`
- 改前端交互、状态流、i18n 或 sample UI，先看：
  - `site/js/app.js`
  - `site/js/app-controller.js`
  - `site/js/ui-controller.js`
  - `site/js/sample-controller.js`
  - `site/js/sample-view.js`
  - `site/js/i18n.js`
  - `site/js/i18n/`
- 改 WebAssembly bridge 或 Web presentation/native 边界，先看：
  - `src/flipbits_web_bridge.cpp`
  - `CMakeLists.txt`
- 改生成进度、encode lifecycle、player 出现时机或 wasm encode contract，先看：
  - `site/js/app-controller.js`
  - `site/js/ui-controller.js`
  - `src/flipbits_web_bridge.cpp`
  - `C:/code/WaveBits/libs/AGENTS.md`
- 改 sample 文本导出、Pages 站点准备、本地构建或本地预览入口，先看：
  - `tools/export_sample_texts.py`
  - `tools/build_wasm.py`
  - `tools/prepare_pages_site.py`
  - `tools/serve_site.py`

## Cross-Project Pointers

- Web sample 文本来源于 Android XML；如果要改 sample key、`short/long` 语义或 sample text workflow，统一去看：
  - `.agent/workflows/translations/android-sample-text.md`
  - `apps/audio_android/AGENTS.md`
- GitHub Pages workflow 入口在：
  - `.github/workflows/deploy-pages-demo.yml`

## Validation

- 改 Web Python 工具后，最小检查优先跑：
  - `python -m py_compile apps/audio_web/tools/build_wasm.py apps/audio_web/tools/export_sample_texts.py apps/audio_web/tools/prepare_pages_site.py apps/audio_web/tools/serve_site.py`
- 改前端 JS 后，最小检查优先跑：
  - `node --check apps/audio_web/site/js/app.js`
- 需要本地构建 / 预览时，命令统一看：
  - `docs/notes/web/cmd.md`
