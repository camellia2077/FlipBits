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
- 改 Voice FX / audio-to-audio / Android-Web 结果一致性：
  - `libs/audio_api/include/bag_api.h`
  - `libs/audio_api/src/bag_api_voice_fx_entrypoints_impl.inc`
  - `libs/audio_api/tests/api_voice_fx_tests.cpp`
  - Android 离线入口：`apps/audio_android/app/src/main/cpp/jni_bridge_voice.cpp`
  - Web 离线入口：`apps/audio_web/src/flipbits_web_bridge.cpp`
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

## Voice FX / audio-to-audio 契约
- `bag_apply_voice_fx` 是离线文件处理的 canonical API。Android 当前通过 JNI `NativeApplyVoiceFx` 使用这条路径；Web 上传文件也必须通过 wasm bridge 的离线入口调用这条路径。
- `bag_create_voice_fx_processor` / `bag_process_voice_fx_block` / `bag_flush_voice_fx_processor` 是 streaming/live path，只适合实时或分块输入场景；不要用它作为 Android/Web 文件生成一致性的基准。
- streaming/live path 不承诺和 `bag_apply_voice_fx` 逐样本一致，尤其是 dual track preset 与 `subvoice_style` 场景。需要跨端听感一致时，必须对齐到 `bag_apply_voice_fx`。
- Voice FX 目前没有 libs 提供的生成进度契约；Web/Android 不应伪造 Voice FX 百分比进度。只展示等待、解码/处理中、成功或失败状态。
- Web 文件输入要先对齐 Android 的离线处理前提：mono PCM16、48 kHz，然后再调用 `bag_apply_voice_fx`。浏览器解码出的设备采样率不能直接作为跨端一致性的默认输入。
