# CI Workflow

本文件记录当前仓库 GitHub Actions / CI 的职责地图与修改口径，方便后续维护 `.github/workflows/`、查看失败原因、或判断某次改动应该触发哪条 CI。

## Current Workflows

- `ci-host-verify`
  - Host 默认主线 CI
  - 运行 `python tools/run.py verify --build-dir build/dev --skip-android`
  - 主要覆盖 `libs`、`Test`、`apps/audio_cli`、`cmake`、`tools`

- `ci-android-assemble`
  - Android Debug 组装 CI
  - 运行 `python tools/run.py android assemble-debug`
  - 主要覆盖 `apps/audio_android`，以及会影响 Android native/package 的 `libs`、`cmake`、`tools`

- `ci-android-quality`
  - Android Kotlin 质量 CI
  - 运行 `python tools/run.py android quality`
  - 主要覆盖 Android Kotlin/Gradle/tooling 质量 gate

## Trigger Intent

- Host 相关改动：
  - 优先触发 `ci-host-verify`
  - 典型路径：
    - `libs/**`
    - `Test/**`
    - `apps/audio_cli/**`
    - `cmake/**`
    - `tools/**`

- Android 相关改动：
  - 优先触发 `ci-android-assemble`
  - Kotlin / detekt / ktlint 相关同时触发 `ci-android-quality`
  - 典型路径：
    - `apps/audio_android/**`

- 共享基础设施改动：
  - `libs/**`、`cmake/**`、`tools/**` 可能同时影响 host 与 Android
  - 这类改动允许同时触发多条 workflow

- 无关文档改动：
  - 不应默认触发上述三条 CI
  - 优先通过 `paths` 过滤减少噪音

## Modification Rules

- 优先扩现有职责线，不轻易新增第四条、第五条 workflow
- Host verify 与 Android assemble 保持分开，不要重新混成一个大 workflow
- Android assemble 与 Android quality 保持分开，避免构建失败和 Kotlin 质量失败混在一起
- 修改 workflow 时，优先先看：
  - `.github/workflows/ci-host-verify.yml`
  - `.github/workflows/ci-android-assemble.yml`
  - `.github/workflows/ci-android-quality.yml`
- 如果只是调整触发范围，优先改 `paths`，不要顺手重写 job 结构
- 如果只是替换命令，优先继续通过 `python tools/run.py ...` 间接调用，不要直接把仓库内部细节散落到 workflow 里

## Reading Failures

- `ci-host-verify` 失败：
  - 先看 host toolchain / `verify` / `ctest` / Rust CLI / static policy

- `ci-android-assemble` 失败：
  - 先看 Android SDK/NDK/CMake 安装
  - 再看 `apps/audio_android` Gradle/JNI/native package

- `ci-android-quality` 失败：
  - 先看 `ktlint` / `detekt`
  - 再看 Kotlin 源码或 Android tooling 配置

## History / Commit Note

- 如果改动的是仓库内部 CI / tooling 结构，而不是产品能力：
  - 优先写入 `docs/tools/history/YYYY-MM-DD.md`
  - 不为 CI 单独发明产品版本号

- 如果改动会改变 agent / 开发者对 CI 的使用方式：
  - 在 commit message 中明确写出：
    - 哪条 workflow 被新增/拆分/收紧
    - 是触发范围变化，还是 job 职责变化
