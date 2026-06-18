如果你的 `PATH` 已经包含：

- `C:\Computer\emsdk`
- `C:\Computer\emsdk\upstream\emscripten`

Web 的构建命令就是：

```powershell
python tools/run.py web build-wasm
```

显式指定构建目录和配置：

```powershell
python tools/run.py web build-wasm --build-dir build/web --configuration Release
```

构建完成后，本地预览命令是：

```powershell
python tools/run.py web serve-site
```

然后浏览器打开：

http://127.0.0.1:4173

依赖：·

```powershell
python --version
cmake --version
ninja --version
emcmake --version
em++ --version
```

只要 `emcmake` 和 `em++` 正常，`python tools/run.py web build-wasm` 就可以直接跑。

WASM Release 编译参数的性能实验记录见：

- `docs/notes/web/wasm-release-optimization-benchmarks.md`
