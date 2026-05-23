如果你的 `PATH` 已经包含：

- `C:\Computer\emsdk`
- `C:\Computer\emsdk\upstream\emscripten`

Web 的构建命令就是：

```powershell
python apps/audio_web/tools/build_wasm.py
```

显式指定构建目录和配置：

```powershell
python apps/audio_web/tools/build_wasm.py --build-dir build/web --configuration Release
```

构建完成后，本地预览命令是：

```powershell
python apps/audio_web/tools/serve_site.py
```

然后浏览器打开：

http://127.0.0.1:4173

依赖：

```powershell
python --version
cmake --version
ninja --version
emcmake --version
em++ --version
```

只要 `emcmake` 和 `em++` 正常，`build_wasm.py` 就可以直接跑。