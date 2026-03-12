## Terminal

- 在这个仓库执行终端命令时，统一使用 `pwsh` 作为 shell 入口，不使用 `powershell.exe`。

## Build

- 修改代码后，如需开始编译，优先使用 `python tools/run.py build --build-dir build/dev`。
- 修改代码后，如需完整验证，优先使用 `python tools/run.py verify --build-dir build/dev`。
- 修改 Android / Gradle / JNI / CMake 集成后，如需直接验证 Android，优先在仓库根执行 `pwsh -Command ".\\gradlew.bat :app:assembleDebug"`。
- Android 官方 `Gradle` 入口固定在仓库根目录，`apps/audio_android` 不再作为独立 `Gradle` root 使用。
- 如需生成可见测试音频产物，优先使用 `python tools/run.py roundtrip --build-dir build/dev --mode flash --text "Hello"` 或 `python tools/run.py smoke --build-dir build/dev`。

## Temp Files

- 根目录 `temp/` 是临时文件夹，默认不会被 Git 跟踪。
- 如需生成临时分析、草稿、阶段文档或中间产物，统一放到仓库根目录的 `temp/` 下，不要散落到其他目录。
