# Mode Design Docs

更新时间：2026-05-21

## 目的
- 把 `mini / flash / pro / ultra` 统一收口到同一层级的 mode-first 文档结构。
- 为人类维护者与 AI / agent 提供稳定的模式入口，避免继续把不同 mode 的细节散落在总览页里。
- 让 `docs/design/transports.md` 退回“总览 / 对比 / 跳转”职责，而不是继续承载全部细节。

## 模式入口
- [`voice/README.md`](voice/README.md)
  - `Voice` 页 `audio -> audio` 效果器 preset 的总说明。
- [`voice/`](voice/)
  - `Single track / Dual track` 分类，以及 `Metal / Code / Robot / Binharic / Voice Trigger / Raw Constant` 的设计说明。
- [`flash/README.md`](flash/README.md)
  - 风格化 BFSK / bit-by-bit signaling 的主说明。
- [`flash/voicing-emotions.md`](flash/voicing-emotions.md)
  - `flash` 六种 emotion preset 的总览与 decode 边界。
- [`flash/`](flash/)
  - `Standard / Hostility / Litany / Collapse / Zeal / Void` 的逐 preset 详细设计。
- [`mini.md`](mini.md)
  - Morse code 模式的输入规范、节奏单位、follow / visual 口径与主链路文件。
- [`mini-whitespace-contract.md`](mini-whitespace-contract.md)
  - `mini` 对 empty text、separator whitespace、canonical normalization 和 silence PCM 的权威语义。
- [`pro.md`](pro.md)
  - ASCII-only + `DTMF-like` 双音映射模式的定位、字节结构与 clean PHY 口径。
- [`ultra.md`](ultra.md)
  - UTF-8 payload + `Ultra clean frame v1` + clean `16-FSK` baseline 的定位、frame layout、nibble 映射与主链路文件。
- [`multi-tone-fsk.md`](multi-tone-fsk.md)
  - 未来 Multi-tone FSK 的定位、与 `ultra` 的取舍关系、适用场景和推进顺序。

## 建议阅读顺序
- 想快速比较四种 mode：
  - 先看 [`../transports.md`](../transports.md)
- 想修改某个 mode 的 core 实现或参数：
  - 先看对应 mode 文档
  - 再看 [`../../architecture/repo-map.md`](../../architecture/repo-map.md)
- 想改 `flash` 的 preset、payload cadence、emotion voicing 或 Android 选择器接线：
  - 先看 [`flash/voicing-emotions.md`](flash/voicing-emotions.md)
  - 再看具体 preset 文档
