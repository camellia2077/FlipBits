# Future Multi-tone FSK Design

更新时间：2026-05-21

## 定位
Multi-tone FSK 是未来计划加入的高速传输模式，不是当前 `ultra` 的替代实现。

它的目标是在近距离、低干扰、设备频响相对稳定的环境下提高数据吞吐量。它可以使用更多并行 tone、更多 bits per symbol，或更短 symbol duration 来换速度；这些选择天然会降低抗混响、抗窄带干扰、抗设备频响凹陷和复杂声学拓扑的余量。

## 和 `ultra` 的关系
- `ultra` 当前使用 clean `16-FSK`，定位是低速、清晰、可视化友好、参数保守。
- Multi-tone FSK 的定位是更高吞吐量，适合近距离、低干扰环境。
- 两者不是同一协议的参数档位；应作为不同 mode 或明确不同 profile 管理。
- `ultra` 的 clean frame v1 可以作为 frame / CRC / payload boundary 的参考，但 Multi-tone FSK 不应默认复用 `ultra` 的全部 PHY 参数。

## 设计取舍
Multi-tone FSK 可以从以下方向提高速度：

| 方向 | 速度收益 | 代价 |
| --- | --- | --- |
| 增加并行 tone | 单个 symbol 承载更多信息 | 更容易受到设备频响凹陷、互调和窄带干扰影响 |
| 缩短 symbol duration | symbol rate 更高 | 抗混响和抗 timing error 的余量降低 |
| 增大 tone count 或缩小 tone spacing | 频谱利用率更高 | 频点判别更难，对频偏和频响更敏感 |
| 多 tone 组合映射 | 信息密度更高 | visual、decode、错误定位和调试复杂度上升 |

## 适用场景
- 近距离播放和接收。
- 背景噪声较低。
- 播放设备和接收设备的频响没有明显窄带凹陷。
- 用户更关心较快传输，而不是极端环境下的稳健性。

## 非目标
- 不以长距离、高混响、多路径、强噪声或复杂声学拓扑作为主目标。
- 不承诺兼容外部 MFSK 标准。
- 不在当前阶段替换 `ultra` clean `16-FSK` baseline。
- 不为了速度牺牲 frame boundary、CRC 和可验证 decode 结果。

## 建议协议方向
正式实现前应先定义独立 baseline，而不是直接把 `ultra` 参数改快：

- 明确 mode/profile 名称。
- 明确 frame layout 是否复用 `Ultra clean frame v1` 的字段语义，或定义新 frame version。
- 明确 symbol duration、tone table、tone spacing、并行 tone 数量和组合映射。
- 明确 visual timeline：Android 不应猜测并行 tone 的语义，应由 libs 提供 frame section、symbol group 和 payload byte range。
- 明确 decode failure 行为：CRC 或 frame validation 失败时不输出不可信 payload。

## 推荐推进顺序
1. 保持 `ultra` 作为 clean `16-FSK` 稳定基线。
2. 在文档中先定义 Multi-tone FSK baseline 和可视化契约。
3. 在 libs 中实现独立 codec / PHY / tests。
4. 再接 Android visual 和 token follow，不让 Android 反推协议细节。
