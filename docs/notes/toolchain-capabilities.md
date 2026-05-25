# Toolchain Capabilities

更新时间：2026-05-22

## 目标

这份文档记录仓库级的 toolchain capability contract，避免把某一条构建 lane
当前“刚好能编过”的假设误写成所有 lane 的默认前提。

## 当前统一能力开关

- `FLIPBITS_HAS_STD_MODULE_PROVIDER`
  - 含义：当前 toolchain 是否提供可正式启用的 `std` 模块 provider。
  - `ON` 时：target 可以启用 `CXX_MODULE_STD`，源码允许走 `import std;` 主口径。
  - `OFF` 时：不能把 `import std;` 当成无条件前提；需要走 include-based fallback。
  - 当前覆盖面：`libs/audio_core`、`libs/audio_api`、`libs/audio_io` 的标准库入口统一走这一个 capability。

## 单一入口

- CMake capability 检测：
  - `cmake/flipbits_toolchain_capabilities.cmake`
- 生成给源码消费的共享头：
  - `bag/common/build_features.h`
  - `bag/common/build_features_generated.h`

## 当前 lane 口径

- Root host `clang++ + Ninja`
  - 继续视为 module-first 主线。
  - 这条 lane 当前仍要求 `FLIPBITS_HAS_STD_MODULE_PROVIDER=ON`，因此 host 构建会继续走 `import std;` 主口径。
- WebAssembly / Emscripten
  - 当前支持 named modules graph。
  - 当前不把 `std` module provider 当成正式前提。
  - encode generation 走 operation pump。

## 修改规则

1. 新增跨 toolchain 条件时，优先先问：它是仓库级 capability，还是单个 target 的一次性例外。
2. 只要一个条件会影响多个 target、多个源码文件或 ABI 行为，就应该先进入这份 capability contract。
3. 不要继续新增散落的 target-specific `if(EMSCRIPTEN)` / `if(Android)` 去表达同一类能力差异，除非它真的是平台专属而不是通用 capability。
4. `libs/audio_core`、`libs/audio_api`、`libs/audio_io` 里凡是“标准库入口”相关的新源码，都应优先使用 `bag/common/build_features.h` 判断 capability，而不是直接写死 `import std;` 或平台宏。

## 回归验证

这类 capability 改动完成后，统一按固定回归清单执行：

- `docs/notes/toolchain-capability-regression-checklist.md`
