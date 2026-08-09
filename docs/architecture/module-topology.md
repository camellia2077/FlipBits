# FlipBits Host Module Topology

更新时间：2026-08-10

## 目标
- 说明 host 默认 modules 路径下的层级拓扑。
- 标明哪些头文件只是 compatibility layer。
- 标明当前哪些目标仍然不能直接依赖内部 modules。

## 默认规则
- host 根目录 CMake 构建直接固定为当前单一受支持主线
- `libs/` 内部新增实现默认应优先 `import` 已有 modules
- 不再新增横向公开 `#include` 依赖来替代已存在的 module 边界
- 新代码入口应走当前 modules 或稳定边界，不新增旧路径依赖

## 当前 `module-first` 程度
- 当前 host 主线已经是“明确的 `module-first`”，不是“header-first + 少量 module 点缀”。
- `audio_core` 主链路已经以 named modules 为正式内部入口；`bag_api.cpp` 这类 host 边界后的实现层也直接消费 modules。
- 但当前不是“所有东西都必须 pure module”：
  - `bag_api.h` 继续作为稳定 C ABI
  - `wav_io.h` 继续作为稳定文件 I/O 边界
  - Android 继续是独立 `C++23` packaging lane
  - `sndfile` backend owner 继续保留在 include-based private backend
- 因此这里的 `module-first` 应理解为：
  - host 内部能力优先落在 modules
  - 长期边界、平台例外与 third-party/backend owner 不强推成 module-only

## 模块分层

以下列表描述稳定的职责层级，不承担逐文件清单职责。当前实际 module interface 与 implementation unit 以
`libs/audio_core/CMakeLists.txt` 和 `libs/audio_io/CMakeLists.txt` 为准；新增或删除 module 时必须先保证
CMake、依赖方向和测试一致，不要求为维持本文的静态枚举而复制一份文件清单。

### Layer 1：基础数据与错误模型
- `bag.common.config`
- `bag.common.error_code`
- `bag.common.types`
- `bag.common.version`

### Layer 2：叶子能力
- `audio_io.wav`
- `audio_io.wav_metadata_parse`
- `bag.flash.codec`
- `bag.flash.signal`
- `bag.flash.voicing`
- `bag.mini.codec` / `bag.mini.morse_rules` / `bag.mini.tone_renderer`
- `bag.pro.codec`
- `bag.pro.tone_renderer`
- `bag.ultra.codec`
- `bag.ultra.tone_renderer`
- `bag.voice.fx`
- `bag.transport.compat.frame_codec`

### Layer 3：中层能力
- `bag.transport.decoder`
- `bag.transport.follow`
- `bag.transport.encode_operation_steps`
- `bag.transport.encode_work_plan`
- `bag.flash.facade`
- `bag.flash.phy_clean`
- `bag.fsk.codec`
- `bag.mini.phy_clean`
- `bag.pro.phy_clean`
- `bag.pro.phy_compat`
- `bag.ultra.phy_clean`

### Layer 4：汇聚层
- `bag.transport.facade`
- `bag.pipeline`

### Layer 5：C ABI 消费边界
- `libs/audio_api/src/bag_api.cpp`
  - host modules 路径下改为 `import`
  - 通过 `bag.flash.facade` 消费 flash 的聚合能力，不直接组合 `bag.flash.codec` / `signal` / `voicing`
- `libs/audio_api/include/bag_api.h`
  - 保持稳定 C ABI，不改为 module-only

## 长期头文件边界
- `libs/audio_api/include/bag_api.h`
- `libs/audio_io/include/audio_io_api.h`
- `libs/audio_io/include/wav_io.h`
- `libs/audio_runtime/include/audio_runtime.h`

这些头文件当前的职责是：
- `bag_api.h` 继续作为稳定 C ABI
- `audio_io_api.h` 继续作为 WAV bytes / metadata 的稳定 C ABI
- `wav_io.h` 继续作为稳定文件 I/O 边界，不并入 compatibility-only 清理面
- `audio_runtime.h` 继续作为 playback session 状态转换的稳定 C ABI
- `audio_io.wav` 是 host 内部优先入口，但不是唯一入口

## Interface Headers
- `libs/audio_core/include/bag/interface/common/*.h`
- `libs/audio_core/include/bag/link/*.h`
- `libs/audio_core/include/bag/phy/**/*.h`

这些头文件当前的职责是：
- `audio_core` 主线 bridge 语义已经结束
- 预留接口头所需的 `C++17` common declarations 通过 `bag/interface/common/*` 提供，并按长期保留的 reserved-interface boundary 管理
- `link/*` 与 `phy/*` 继续只是预留接口层，不属于当前主线 bridge surface
- 主仓 `bag/internal/**` owner 已归零，不再保留现役 declaration layer
- `link/*` 与 `phy/*` 的 `C++17` fallback 已切到 `bag/interface/common/*`
- `bag/interface/common/*` 不进入 host-only `import std;` 扩面，也不建立 `bag.interface.*` module mirror
- 旧路径与 include token 回流都会被静态门禁阻止

它们不是优先新增能力的主入口；新增内部实现应优先落在：
- `libs/audio_core/modules/`
- `libs/audio_io/modules/`
- 或对应 `.cpp` named module implementation unit

更细的分类与消费端边界说明见：
- `docs/architecture/compatibility-layer-inventory.md`
- `docs/architecture/android/android-native-strategy.md`

## `audio_io` 当前拓扑
- module interface：
  - `libs/audio_io/modules/audio_io/wav.cppm`
- module front-end：
  - `libs/audio_io/modules/audio_io/wav_impl.cpp`
- header front-end：
  - `libs/audio_io/src/wav_io.cpp`
- shared bytes implementation：
  - `libs/audio_io/src/wav_io_bytes_impl.inc`
- private backend：
  - `libs/audio_io/src/wav_io_backend.h`
  - `libs/audio_io/src/wav_io_backend.cpp`

当前语义：
 - `wav_io.h` / `audio_io.wav` 对外同时暴露两类能力：
   - `wav bytes <-> mono PCM16`
   - `path <-> WAV 文件`
- `wav.cppm` 与 `wav_impl.cpp` 现在走和 `audio_core` / `bag_api` 相同的 capability-aware 标准库入口：
  - `FLIPBITS_HAS_STD_MODULE_PROVIDER=ON` 时走 `import std;`
  - `FLIPBITS_HAS_STD_MODULE_PROVIDER=OFF` 时走 include-based fallback
- `wav_io.cpp` 继续保留为稳定 header boundary wrapper，不强收进当前 import-std baseline
- `wav_io_backend.cpp` 继续保持 include-based，因为它是 backend owner 与 `sndfile.h` include token 的唯一批准位置
- `sndfile` third-party C 边界不进入 exported module interface，也不进入 `wav_io.h`
- `audio_io` 因此不是“未模块化”，而是“只把适合进入 host 主线的 front-end 模块化”
- `audio_io.wav` 是 host 内部优先入口，但 `wav_io.h` 继续保留为稳定 header boundary
- 当前不把 `audio_io` 强收成 pure module，是为了同时满足：
  - host 内部消费优先走 module
  - Android 可通过 package-private wrapper 复用同一套 WAV bytes 逻辑
  - 文件 I/O 对外边界保持稳定
  - third-party/backend owner 继续被限制在 private include-based surface

## Host `import std;` Capability Baseline
- `audio_core` 与 `audio_io` 当前加入 CMake `CXX_MODULES` file set 的 module interface 通过统一 capability contract 管理标准库入口。精确清单直接读取对应 CMake，本文不维护第二份易漂移枚举。
- promoted interfaces 的标准库入口规则是：
  - `FLIPBITS_HAS_STD_MODULE_PROVIDER=ON` 时走 `import std;`
  - `FLIPBITS_HAS_STD_MODULE_PROVIDER=OFF` 时走 `bag/common/std_compat.h`
- Root host 当前正式要求的是 named modules 主线，不是无条件 `import-std-only`。
- 只要 CMake 没有实际提供 `__CMAKE::CXX23`，host 就应留在 capability-aware fallback 路径。
- 在 promoted set 全部收口后，当前只保留以下受控形态：
  - core module implementation capability-aware
    - `module;` + capability header + (`import std;` | include-based fallback) + `module bag.*;`
    - 用于 `libs/audio_core/src/*.cpp`
  - `audio_io.wav` module interface capability-aware
    - `module;` + capability header + `export module ...;` + (`import std;` | include-based fallback)
    - 当前只用于 `libs/audio_io/modules/audio_io/wav.cppm`
  - boundary-host capability-aware
    - 非-module boundary host 通过 capability contract 选择 `import std;` 或 include-based fallback，同时保持稳定 boundary include / import 关系
    - 当前只用于 `libs/audio_api/src/bag_api.cpp`
  - backend-bridge capability-aware exception
    - 保留 include-based backend header 与 global fragment backend declaration，同时在命名模块实现面通过 capability contract 选择标准库入口
    - 当前只用于 `libs/audio_io/modules/audio_io/wav_impl.cpp`
- 除以上 capability-aware baseline 形态外，不接受新的标准库入口口径。

## 当前不能直接走 modules 的目标
- Android native `externalNativeBuild`
  - 入口：`apps/audio_android/app/src/main/cpp/CMakeLists.txt`
  - 约束：`CMake 4.1.2 + C++23`
  - 当前通过 `bag_api.h`、`audio_runtime.h` 和 package-private `audio_io` wrapper 作为边界消费方接入，并在 `native_package` 下使用 `audio_core` package-owned implementation sources、`bag_api` / `audio_runtime` package-owned boundary implementation、`audio_io` package-private wrapper 与 `android_bag/**` / `android_audio_io/**` 私有声明层
- `bag_api.h`
  - 原因：它是稳定 C ABI，不适合改成 module-only 对外接口
- `wav_io.h`
  - 原因：它是稳定文件 I/O boundary，不适合改成 module-only 对外接口；`audio_io` 的目标是 module-first，而不是消灭这条边界
- Rust CLI FFI 消费端
  - `apps/audio_cli/rust/src/bag_api/**`
  - `apps/audio_cli/rust/src/audio_io_api/**`
  - 这些 adapter 分别消费 `bag_api.h` / `audio_io_api.h` 对应的 C ABI，不直接 import `audio_core` 内部 modules
- Android JNI 消费端
  - `apps/audio_android/app/src/main/cpp/*.cpp`
  - 继续通过 `bag_api.h`、`audio_runtime.h` 和 package-private `audio_io` wrapper 接入，不直接 import `audio_core` 内部 modules
- WebAssembly bridge
  - `apps/audio_web/src/flipbits_web_bridge.cpp`
  - 通过 `bag_api.h` 接入并把 ABI 映射为 Web runtime surface，不在 bridge 中建立第二套 core module API

## 推荐验证命令
- 默认 host modules 路径：
  - `python tools/run.py verify --build-dir build/dev --skip-android`
- Android focused gate：
  - `python tools/run.py android native-debug`
  - `python tools/run.py android assemble-debug`

## 当前兼容政策摘要
- host 默认 modules 路径是正式主路径。
- Android 是受支持例外路径。
- 主仓 `bag/internal/**` owner 已归零。
- 预留接口头当前独立使用 `bag/interface/common/*` declaration layer；这层按长期保留的 internal boundary 管理。
