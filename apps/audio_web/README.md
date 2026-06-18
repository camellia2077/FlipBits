# audio_web

`audio_web` 是 `FlipBits` 的 Web presentation / GitHub Pages demo。

这里的 README 只做导航；实现细节、命令口径、设计语义和发布记录统一收口到根目录 `docs/`。

## 先看哪里

- Web 发布记录：
  - `docs/history/presentation/web/2026-05-22.md`
- Web 本地构建 / 预览命令：
  - `docs/notes/web/cmd.md`
- transport / mode 设计：
  - `docs/design/transports.md`
  - `docs/design/modes/README.md`
- toolchain capability / `import std` 兼容策略：
  - `docs/notes/toolchain-capabilities.md`

## 想改什么，就看哪里

- 页面结构 / 样式：
  - `site/index.html`
  - `site/styles.css`
- 前端状态 / i18n / sample UI：
  - `site/js/`
- WebAssembly bridge：
  - `src/flipbits_web_bridge.cpp`
  - `CMakeLists.txt`
- Voice FX / audio-to-audio：
  - 文件输入解码与 48 kHz 对齐：`site/js/audio-utils.js`
  - Voice UI/action：`site/js/app-controller.js`
  - worker 调用：`site/js/encode-worker.js`
  - wasm wrapper：`site/wasm/flipbits_web.js`
  - native bridge：`src/flipbits_web_bridge.cpp`
  - 上传文件必须走 `flipbits_web_apply_voice_fx_pcm_bytes` -> libs `bag_apply_voice_fx`，这是和 Android 对齐的离线 canonical path。
  - streaming/block Voice FX 只用于 live path，不用于文件生成一致性验证。
  - Voice FX 不显示生成百分比进度，因为 libs 当前没有 Voice FX progress contract。
- 构建 / 部署 / sample 导出工具：
  - `tools/`

## 进一步导航

- agent 入口索引：
  - `apps/audio_web/AGENTS.md`
- 仓库总文档索引：
  - `docs/README.md`
