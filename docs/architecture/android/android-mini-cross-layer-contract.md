# Android Mini Cross-Layer Contract

更新时间：2026-05-16

## 目的
- 把 `mini` encode 链路中最容易漂移的跨层语义写清楚。
- 让 Android UI、JNI、`audio_api` 和 `audio_core` 在 whitespace case 上使用同一套模型。
- 给 adb automation 和回归测试提供稳定判定表。

## Encode Chain
1. Android input / debug scenario 接收原始字符串
2. Kotlin `mini` normalization 生成 `encodeText`
3. `audio_api` validation 校验 `encodeText`
4. `audio_core` 生成 payload
5. `audio_core` 生成 PCM
6. Android session state 用生成的 PCM / metadata 更新 mini player 和 follow data

## Layer Responsibilities

### Android UI / Debug Scenario
- 必须允许把 whitespace-only 输入原样送入 encode 链路。
- 不允许因为 `isBlank()` 或“像没输入”就直接吞掉。
- adb scenario、capture wrapper 和 UI action 应该区分：
  - extra absent
  - extra present but empty string
  - extra present and whitespace-only

### Kotlin Encode Request
- `mini` mode 必须使用规范化后的 `encodeText` 进入 native。
- 原始输入只用于编辑辅助、日志和 normalization 预览。
- 不应让 UI normalization 和 core validation 使用两套不同文本。

### `audio_api`
- `bag_validate_encode_request()` 的权威语义：
  - `""` -> `BAG_VALIDATION_EMPTY_TEXT`
  - whitespace-only `mini` -> `BAG_VALIDATION_OK`
- encode operation 和 sync encode 对同一输入必须给出一致结果。

### `audio_core`
- `mini` normalization 统一处理 `space / \t / \r / \n`。
- whitespace-only 输入必须产生 separator-only payload。
- separator-only payload 必须产生 silence PCM。
- decode / follow 的 canonical text 应为规范化后的 `" "`。

### Android Session / Mini Player
- 只要 sample count 大于 `0`，即使 PCM 全部为静音样本，也应视为“生成成功”。
- mini player 闪现后消失通常意味着生成失败后状态被清空，不意味着成功但 UI 没显示。

## Stable Regression Cases
- `""`
  - 预期：validation failure
- `"   "`
  - 预期：success, `payloadBytes=1`, sample count `> 0`
- `"\n"`
  - 预期：success, `payloadBytes=1`, sample count `> 0`
- `" \t\r\n "`
  - 预期：success, `payloadBytes=1`, sample count `> 0`
- `"   123\n"`
  - 预期：success, normalized encode text `"123"`

## adb / Automation Signals
- 成功链路应看到：
  - `execute:validated ... issue=0`
  - `execute:singleSuccess ... samples=...`
  - `applySuccess ... pcmSamples=...`
- 失败链路如果看到：
  - `execute:singleFailed error=4`
  - `applyFailure`
  说明某一层仍把 whitespace case 误判成 empty payload / empty PCM / internal error。

## 文档关系
- `mini` 业务语义见 [`../../design/modes/mini-whitespace-contract.md`](../../design/modes/mini-whitespace-contract.md)
- `mini` mode 总设计见 [`../../design/modes/mini.md`](../../design/modes/mini.md)
- Android automation 入口见 [`android-mini-automation.md`](android-mini-automation.md)
