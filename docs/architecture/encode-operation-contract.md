# Encode Operation Contract

更新时间：2026-05-24

## 目标

这份文档记录 `libs` 当前的生成进度契约。Android 和 Web 的生成进度显示都应消费这里描述的同一套数据，而不是各自推导 phase、percent 或工作量。

## 权威 API

当前权威生成进度 API 是 encode operation：

- `bag_create_encode_operation`
- `bag_get_encode_operation_work_plan`
- `bag_pump_encode_operation`
- `bag_poll_encode_operation`
- `bag_take_encode_operation_result`
- `bag_destroy_encode_operation`

入口文件：

- `libs/audio_api/include/bag_api.h`
- `libs/audio_api/src/bag_api_encode_operation_entrypoints_impl.inc`
- `libs/audio_api/src/bag_api_encode_result_copy_impl.inc`
- `libs/audio_api/tests/api_async_tests.cpp`
- `libs/audio_core/include/bag/interface/common/types.h`
- `libs/audio_core/src/transport/transport_impl.inc`

## Snapshot

`bag_encode_operation_progress` 是生成过程 snapshot，对应 core 的 `EncodeProgressSnapshot`。

它是 Android/Web 进度显示的事实来源，包含：

- lifecycle state: queued / running / succeeded / failed / cancelled
- phase: preparing input / rendering PCM / postprocessing / finalizing
- overall progress 和 phase progress
- completed / total work units
- phase completed / phase total work units
- terminal code
- estimated PCM sample count
- payload byte count
- segment count
- current segment index

消费者规则：

- UI 的 phase 和 percent 应从 snapshot 派生。
- 不要在 Android/Web 侧重新实现 phase 或 percent 算法。
- snapshot 的枚举数值是跨平台绑定依赖的稳定 ABI，不应随意重排。

## Work Plan

`bag_encode_operation_work_plan` 是生成工作计划，对应 core 的 `EncodeWorkPlan`。

它提供：

- preparing input work units
- rendering PCM work units
- postprocessing work units
- finalizing work units
- total work units
- estimated PCM sample count
- payload byte count
- segment count

消费者规则：

- Android/Web 如需展示总量、阶段工作量或预计输出规模，应直接使用 work-plan。
- 对可能在准备阶段细化 layout 的 mode，消费者应把最新 work-plan 与 snapshot 作为当前事实来源。
- 不要在平台层根据文本长度、mode 或采样率重新推导工作量 bucket。

## Pump

`bag_pump_encode_operation` 推进 operation。`bag_encode_operation_pump_budget` 只控制单次 pump 的 native work/time 预算。

消费者规则：

- UI 刷新频率由消费者节流。
- 不要通过故意缩小 native pump 或在每次 pump 后固定 sleep 来制造可见进度。
- Android/Web 可以用自己的调度策略，但数据来源仍必须是 `bag_poll_encode_operation` 的 snapshot。

## Terminal Result

`bag_take_encode_operation_result` 只承担终态结果获取。

消费者规则：

- operation completion 不应承担 follow/raw/timeline hydration。
- 需要 follow/raw/timeline 展示数据时，使用 `bag_build_encode_follow_data`。
- follow hydration 和 operation terminal result 保持分层，避免把大 DTO 图塞回生成完成路径。

## 验证

修改 snapshot/work-plan 字段、枚举值、pump 语义或 terminal result 分层时，最低验证：

```powershell
python tools/run.py test-lib audio_api --build-dir build/dev
python tools/run.py android test-debug
```

如果改到 Android JNI/Web bridge，再按对应 app 文档补充：

- Android: `python tools/run.py android assemble-debug`
- JNI 或 shrink/keep 风险: `python tools/run.py android assemble-staging`
- Web bridge: 对应 `apps/audio_web` 文档中的 wasm/site 检查
