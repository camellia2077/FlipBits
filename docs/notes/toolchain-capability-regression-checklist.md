# Toolchain Capability Regression Checklist

更新时间：2026-05-23

## 适用范围

这份清单用于以下类型的改动之后做固定回归验证：

- `import std;` 改为 capability-aware 可选路径
- `bag/common/build_features.h` / `build_features_generated.h` 相关改动
- `cmake/flipbits_toolchain_capabilities.cmake` 相关改动
- `audio_core / audio_api / audio_io` 的标准库入口改动
- `FLIPBITS_HAS_STD_MODULE_PROVIDER`

目标不是只验证单个 target 能否“勉强编过”，而是确认：

- `libs` 主链路还能正常构建和测试
- `apps/audio_cli` 还能正常链接共享边界并通过测试
- `apps/audio_android` 还能正常构建，并且 release-ish lane 不回归

## 执行顺序

统一在仓库根目录执行，统一使用 `pwsh` 终端。

推荐顺序：

1. Host build
2. Host ctest
3. Rust CLI
4. Android debug unit tests
5. Android debug assemble
6. Android staging assemble

## 固定命令

### 1. Host build

```powershell
python tools/run.py build --build-dir build/dev
```

验证点：

- 根仓 CMake configure 正常
- `audio_core / audio_api / audio_io / audio_runtime` 可正常编译
- 当前 host lane 下 `std` module provider 与 fallback 接线没有被打坏

### 2. Host ctest

```powershell
ctest --test-dir C:\code\WaveBits\build\dev --output-on-failure
```

验证点：

- `api_tests`
- `unit_tests`
- `runtime_tests`
- `artifact_tests`
- `cli_smoke_tests`
- modules smoke

说明：

- 这一步是当前最直接、最稳定的 `libs` 回归口径。
- 如果只看“库编译成功”而不跑这一步，无法确认 fallback 改动是否破坏了运行行为或跨层接线。

### 3. Rust CLI

```powershell
python tools/run.py cli test --build-dir build/dev
```

验证点：

- Rust CLI FFI 到 `bag_api` / `audio_io` 仍然可用
- Cargo test / CLI integration tests 正常
- `clippy` 无新增问题

### 4. Android debug unit tests

```powershell
python tools/run.py android test-debug
```

验证点：

- Android presentation 层与共享边界仍可联通
- Kotlin / JNI / XML / sample 文本链路没有被 capability 改动间接打坏

### 5. Android debug assemble

```powershell
python tools/run.py android assemble-debug
```

验证点：

- Android debug native build 正常
- debug lane 下 `audio_core / audio_api / audio_io` 的 capability 接线没有回归

### 6. Android staging assemble

```powershell
python tools/run.py android assemble-staging
```

验证点：

- release-ish lane 下 native build 正常
- shrink / JNI keep backstop 仍然成立
- 不会出现“debug 能过、staging/release 风格 lane 失败”的 capability 回归

## 可选补充命令

### WebAssembly lane

如果改动同时影响 WebAssembly / Pages 演示，再补：

```powershell
python tools/run.py web build-wasm
```

验证点：

- `FLIPBITS_HAS_STD_MODULE_PROVIDER=OFF` lane 仍然能走 include-based fallback
- Emscripten lane 不会因为 host-only `import std;` 假设而回归

## 当前已知说明

### 关于 `python tools/run.py test-lib ...`

库级单测入口可以作为补充，但不应替代本清单。

当前建议：

- 优先跑完整 host build
- 再跑根级 `ctest --test-dir build/dev`

原因：

- 这条路径能同时覆盖 `libs` 主链路和跨层 smoke
- 比只跑单个库子目录更接近 capability contract 的真实回归范围

## 通过标准

以下全部满足，才视为这类改动通过：

- `build` 成功
- 根级 `ctest` 全绿
- `cli test` 成功
- `android test-debug` 成功
- `android assemble-debug` 成功
- `android assemble-staging` 成功

只要其中一项失败，就不要把“toolchain capability / std module fallback 改动已验证”写成已完成。
