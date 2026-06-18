# `libs/` 说明

这个目录存放共享库代码：
- `audio_core`
- `audio_api`
- `audio_io`
- `audio_runtime`

查看或修改 `libs/` 下代码时，优先按任务选择相关文档，不要先递归扫描整个目录。

快速定位入口：
- `docs/README.md`
- `docs/architecture/repo-map.md`

如果只是改某个 mode：
- `mini` / `flash` / `pro` / `ultra` 的总览与入口位置，优先看 `docs/design/transports.md`
- 具体 mode 细节优先看 `docs/design/modes/README.md`
- 具体文件地图，优先看 `docs/architecture/repo-map.md`
- `flash` 情绪音色、preset、payload cadence 或 voicing，总览看 `docs/design/modes/flash/voicing-emotions.md`，具体 preset 看 `docs/design/modes/flash/<preset>.md`

如果是改 API / ABI / 平台边界：
- `bag_api` 入口看 `libs/audio_api/src/bag_api.cpp`
- 稳定边界与兼容层说明看 `docs/architecture/compatibility-layer-inventory.md`

如果是改 Voice FX / audio-to-audio / Android-Web 结果一致性：
- 先看 `libs/AGENTS.md` 的 “Voice FX / audio-to-audio 契约”。
- 离线文件处理的 canonical API 是 `bag_apply_voice_fx`：
  - 声明：`libs/audio_api/include/bag_api.h`
  - 实现：`libs/audio_api/src/bag_api_voice_fx_entrypoints_impl.inc`
  - 覆盖：`libs/audio_api/tests/api_voice_fx_tests.cpp`
- streaming/live API 是 `bag_create_voice_fx_processor` / `bag_process_voice_fx_block` / `bag_flush_voice_fx_processor`。它只用于实时或分块场景，不用于验证 Android/Web 文件生成一致性。
- Android 离线入口看 `apps/audio_android/app/src/main/cpp/jni_bridge_voice.cpp` 的 `NativeApplyVoiceFx`。
- Web 离线入口看 `apps/audio_web/src/flipbits_web_bridge.cpp` 的 `flipbits_web_apply_voice_fx_pcm_bytes`。

如果是改生成进度 / Android 或 Web 生成显示：
- 当前权威契约是 encode operation：
  - `bag_create_encode_operation`
  - `bag_get_encode_operation_work_plan`
  - `bag_pump_encode_operation`
  - `bag_poll_encode_operation`
  - `bag_take_encode_operation_result`
  - `bag_destroy_encode_operation`
- `bag_encode_operation_progress` 是生成过程 snapshot，映射 `audio_core` 的 `EncodeProgressSnapshot`。
- `bag_encode_operation_work_plan` 是生成工作计划，映射 `audio_core` 的 `EncodeWorkPlan`。
- Android 和 Web 应直接消费 operation snapshot / work-plan / pump / terminal result，不要各自重新推导 phase、percent 或工作量。
- `bag_take_encode_operation_result` 只用于终态结果。需要 follow/raw/timeline 展示数据时，使用 `bag_build_encode_follow_data` 单独 hydration。
- 不要新增第二套生成 lifecycle、progress 或兼容分支。
- 相关入口：
  - `libs/audio_api/include/bag_api.h`
  - `libs/audio_api/src/bag_api_encode_operation_entrypoints_impl.inc`
  - `libs/audio_api/src/bag_api_encode_result_copy_impl.inc`
  - `libs/audio_api/tests/api_async_tests.cpp`
  - `libs/audio_core/include/bag/interface/common/types.h`
  - `libs/audio_core/src/transport/transport_impl.inc`

如果是改 WAV / I/O：
- 文件地图看 `docs/architecture/repo-map.md`
- roundtrip 与 metadata 测试口径看 `docs/testing.md`

如果是改播放会话 / seek / 样本位置与时间换算：
- 优先看 `libs/audio_runtime/include/audio_runtime.h`
- 实现入口看 `libs/audio_runtime/src/audio_runtime.cpp`

如果是改测试或验证命令：
- 优先看 `docs/testing.md`

生成进度契约改动的最低验证建议：
- `python tools/run.py test-lib audio_api --build-dir build/dev`
- `python tools/run.py android test-debug`
- 如果改到 Android/JNI/Web bridge，再按对应应用说明补充 assemble 或前端检查。
