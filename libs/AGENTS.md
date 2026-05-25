# libs 工作指引

作用域：`libs/` 整个目录树。

## 开始前先看文档
- 先按任务类型选最小文档集合，不要把下面所有文档当成必读清单。
- 想快速定位入口：
  - `docs/README.md`
  - `docs/architecture/repo-map.md`
- 改 `mini / flash / pro / ultra` 编解码或 mode 参数：
  - `docs/design/transports.md`
  - `docs/design/modes/README.md`
  - `docs/architecture/repo-map.md`
- 改 `flash` 情绪音色、preset、payload cadence 或 voicing：
  - `docs/design/modes/flash/voicing-emotions.md`
  - 具体 preset 细节看 `docs/design/modes/flash/<preset>.md`
  - `docs/design/transports.md`
- 改 `bag_api`、稳定 ABI、Android/CLI 边界或兼容层：
  - `docs/architecture/compatibility-layer-inventory.md`
  - `docs/architecture/repo-map.md`
- 改生成进度、Android/Web 生成状态显示、operation pump 或 work-plan：
  - `libs/audio_api/include/bag_api.h`
  - `libs/audio_api/src/bag_api_encode_operation_entrypoints_impl.inc`
  - `libs/audio_api/src/bag_api_encode_result_copy_impl.inc`
  - `libs/audio_api/tests/api_async_tests.cpp`
  - `libs/audio_core/include/bag/interface/common/types.h`
  - `libs/audio_core/src/transport/transport_impl.inc`
- 改 `text follow` / `token-character-byte` 解析或显示契约：
  - `docs/architecture/text-follow-contract.md`
  - `docs/architecture/repo-map.md`
- 改 WAV / I/O：
  - `docs/architecture/repo-map.md`
  - `docs/testing.md`
- 改测试、验证命令或测试语料：
  - `docs/testing.md`

## 扫描策略
- 不要先全量扫描 `libs/audio_core/`。
- 先按 `docs/architecture/repo-map.md` 的“按任务快速跳转”定位入口文件。
- 若任务只涉及 clean 主链路，通常不要先打开：
  - `phy_compat.*`
  - `frame_codec.*`
  - `text_codec.*`
  - `include/bag/phy/*`
  - `include/bag/link/*`

## 模块入口
- `audio_core`
  - 分发入口：`libs/audio_core/src/transport/transport.cpp`
- `audio_api`
  - API 入口：`libs/audio_api/src/bag_api.cpp`
- `audio_io`
  - I/O 入口：`libs/audio_io/src/wav_io.cpp`
- `audio_runtime`
  - 播放运行时入口：`libs/audio_runtime/src/audio_runtime.cpp`

## 生成进度契约
- 现在的权威生成进度 API 是 encode operation：
  - `bag_create_encode_operation`
  - `bag_get_encode_operation_work_plan`
  - `bag_pump_encode_operation`
  - `bag_poll_encode_operation`
  - `bag_take_encode_operation_result`
  - `bag_destroy_encode_operation`
- `bag_encode_operation_progress` 是 Android/Web 生成进度显示的事实来源，字段映射 core 的 `EncodeProgressSnapshot`。
- `bag_encode_operation_work_plan` 是 Android/Web 生成工作量和总量显示的事实来源，字段映射 core 的 `EncodeWorkPlan`。
- `bag_encode_operation_pump_budget` 只控制一次 pump 的 work/time 预算；UI 进度刷新节奏应该由消费者节流，不应通过改小 native pump 来制造可见进度。
- `bag_take_encode_operation_result` 只承担终态结果获取。需要 follow/raw/timeline 展示数据时，使用 `bag_build_encode_follow_data`，不要把 follow hydration 塞回 operation completion。
- 不要新增第二套生成 lifecycle、phase 或 percent 推导。
- 修改 operation snapshot/work-plan 字段、枚举值或语义时，必须同步检查 Android JNI/Web bridge，并至少运行：
  - `python tools/run.py test-lib audio_api --build-dir build/dev`
  - `python tools/run.py android test-debug`
