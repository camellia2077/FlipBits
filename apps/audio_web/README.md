# audio_web

`audio_web` 是面向 GitHub Pages 的最小网页演示入口。

当前目标不是在这里重写编码逻辑，而是提供一个静态页面壳层，后续把
`libs/audio_api` 的 C API 通过 WebAssembly 接进来，让页面继续复用项目主逻辑。

## 为什么放在 `apps/`

- 这个仓库已经把平台表现层放在 `apps/` 下：
  - `apps/audio_cli`
  - `apps/audio_android`
- 网页 demo 同样属于 presentation 层，不应该塞进 `libs/`。

建议结构：

- `apps/audio_web/site/`
  - GitHub Pages 直接部署的静态资源。
- `apps/audio_web/site/wasm/`
  - 后续放 Emscripten 产物，例如 `flipbits_web.js` 和 `flipbits_web.wasm`。
- `apps/audio_web/tools/`
  - 后续如需本地打包脚本，再放这里。

## GitHub Pages 方案边界

GitHub Pages 只能托管静态文件，不能在服务器上直接运行当前 C++/Rust CLI。

如果要做到：

- 用户输入文本
- 点击按钮
- 浏览器里直接生成音频

那就需要把核心编码能力编译成 WebAssembly，再由网页调用。

对这个仓库，最适合复用的边界是：

- `libs/audio_api/include/bag_api.h`

最小可行路径是：

1. 页面调用 WebAssembly wrapper。
2. wrapper 内部调用 `bag_encode_text(...)` 拿到 `int16_t PCM`。
3. 页面用 JavaScript 把 PCM 包成 WAV `Blob`。
4. 页面把 WAV 塞进 `<audio>` 播放器，并提供下载。

这样不必一开始把 `audio_io` 一起搬进浏览器。

## 当前提交包含什么

- `site/index.html`
- `site/styles.css`
- `site/main.js`
- `site/wasm/flipbits_web.js`
- `src/flipbits_web_bridge.cpp`
- `CMakeLists.txt`
- `tools/build_wasm.py`
- `tools/prepare_pages_site.py`
- `tools/serve_site.py`
- Pages workflow：`.github/workflows/deploy-pages-demo.yml`
- i18n 约定：`apps/audio_web/docs/i18n-style-guide.md`

现在的链路是：

1. 页面收集 `text / mode / flash style`。
2. `site/wasm/flipbits_web.js` 调用 Emscripten runtime。
3. `src/flipbits_web_bridge.cpp` 调 `bag_encode_text(...)`。
4. 页面把返回的 PCM16 封成 WAV 并播放/下载。

## 本地构建

先安装并激活 Emscripten SDK，然后在仓库根目录运行：

```powershell
python ./apps/audio_web/tools/build_wasm.py
```

成功后会生成：

- `apps/audio_web/site/wasm/flipbits_web_runtime.js`
- `apps/audio_web/site/wasm/flipbits_web_runtime.wasm`

## 本地预览

```powershell
python ./apps/audio_web/tools/serve_site.py
```

默认地址：

- `http://127.0.0.1:4173`

## GitHub Pages 部署入口

如果要在 CI / GitHub Pages 场景里一次性准备站点资源，使用：

```powershell
python ./apps/audio_web/tools/prepare_pages_site.py
```

这个入口会依次完成：

1. 导出 Android sample 文本
2. 构建 WebAssembly 产物
3. 校验 `site/` 下的 Pages 发布文件是否齐全
