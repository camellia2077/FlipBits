# audio_web i18n Style Guide

`audio_web` 的本地化目标不是“所有英文都翻译掉”，而是保持：

- 非英语用户能快速读懂页面
- 技术名词、模式名、协议名与仓库主文档保持一致
- GitHub Pages 作为技术型 demo 站点时，不因为过度翻译而降低辨识度

## 1. 哪些内容必须保持英文

以下内容默认不翻译，所有语言保持英文原样：

### 品牌名与模式名

- `FlipBits`
- `mini`
- `flash`
- `pro`
- `ultra`

### 协议 / 编码 / 文件格式 / 技术缩写

- `ASCII`
- `UTF-8`
- `WASM`
- `WebAssembly`
- `PCM`
- `WAV`
- `BFSK`
- `FSK`
- `DTMF`
- `Hz`
- `bit`
- `low`
- `high`

### 技术语境下建议固定英文的术语

- `Mode`
- `Morse`
- `emoji`
- `GitHub`
- `GitHub Pages`
- `Releases`
- `APK`
- `Android`

说明：

- `Mode` 在这个项目里更接近产品内技术标签，而不是一般 UI 词。
- 与这类技术标签直接绑定的分区标题，例如 Mode Overview，也建议固定英文。
- `emoji` 在当前页面里建议直接固定为英文，不再按语言变体写成 `эмодзи`、`絵文字` 等。
- `Morse` 在说明、速度、限制提示里统一保留英文。

## 2. 哪些内容应该本地化

以下内容默认应该翻译成目标语言：

- 普通界面标签
- 按钮文字
- 状态提示
- 错误信息
- 长段说明文字
- 非固定术语的动词与句式

例如：

- `Generate Audio`
- `Download WAV`
- `Result Summary`
- `Generation failed`
- `Enter some text`

## 3. 句式规则

- 允许在本地语言句子中夹带固定英文技术词。
- 不要求逐词对照英文原文。
- 优先保证目标语言自然顺口。
- 技术缩写前后允许使用本地语言语法，例如：
  - `ввід ASCII`
  - `PCM サンプル`
  - `Morse 速度`

## 4. 新增文案时的规则

新增文案时先判断：

1. 是品牌名 / 模式名 / 协议名 / 文件格式名吗？
   - 是：固定英文
2. 是普通 UI 标签或说明句吗？
   - 是：本地化
3. 是本项目内部已经固定为英文的技术标签吗？
   - 是：沿用英文，不重新翻译

## 5. 当前统一口径

当前 `audio_web` 页面里，以下词已经视为固定英文：

- `FlipBits`
- `Mode`
- `mini`
- `flash`
- `pro`
- `ultra`
- `Morse`
- `emoji`
- `ASCII`
- `UTF-8`
- `WASM`
- `WebAssembly`
- `PCM`
- `WAV`
- `BFSK`
- `FSK`
- `DTMF`
- `Hz`
- `bit`
- `low`
- `high`
- `GitHub`
- `GitHub Pages`
- `Releases`
- `APK`
- `Android`

