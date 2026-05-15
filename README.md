<p align="center">
  <img src="ui/app/icon-foreground.svg" alt="FlipBits icon" width="128" />
</p>

<p align="center">
  <em>Icon designed by camellia2077 (FlipBits Project)</em>
</p>

<h1 align="center">FlipBits</h1>


<p align="center">
  中文 | <a href="README_en.md">English</a>
</p>

<p align="center">
  <strong>文本与音频信号之间的风格化编码/解析工具，用 FSK 节奏、Hz 变化与不同停顿间隔，生成可听的“语气化”编码音频</strong><br />
  <em>一款涵盖了韵律感 BFSK 信号、Dual-tone 映射与高效 16-FSK 传输的工具</em>
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-red.svg)](LICENSE)
[![Platform Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg)]()
[![CI Android Assemble](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-assemble.yml/badge.svg)](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-assemble.yml)
[![CI Android Quality](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-quality.yml/badge.svg)](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-quality.yml)
[![CI Host Verify](https://github.com/camellia2077/FlipBits/actions/workflows/ci-host-verify.yml/badge.svg)](https://github.com/camellia2077/FlipBits/actions/workflows/ci-host-verify.yml)


<table>
  <tr>
    <td width="25%"><img src="https://github.com/user-attachments/assets/3790efee-6fcb-4584-a5c7-5b3a3140cad7" alt="1-zh"></td>
    <td width="25%"><img src="https://github.com/user-attachments/assets/1d515f54-abee-4422-8d9b-094165d11b85" alt="2-en"></td>
    <td width="25%"><img src="https://github.com/user-attachments/assets/e8ee3c5d-a438-4fab-a9ec-d15dee0370be" alt="3-de"></td>
    <td width="25%"><img src="https://github.com/user-attachments/assets/6641c520-1c35-45c4-8235-6e5a4b78f4a7" alt="4-jp"></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/2b53391f-a415-4765-92f9-b04276792dc5" alt="5-la"></td>
    <td><img src="https://github.com/user-attachments/assets/04d6ad29-4bed-4554-b4c2-182ad60c094c" alt="6-la"></td>
    <td><img src="https://github.com/user-attachments/assets/970438f7-2ebe-4efd-9aa7-2afb6d75865c" alt="7-la"></td>
    <td><img src="https://github.com/user-attachments/assets/a8786e2b-c65e-445d-9d8c-dd9ec05be31a" alt="8-la"></td>
  </tr>
</table>

## 快速概览
- Android 原生应用，计算机文本编码的可视化，音频化。
- 支持 Morse code (`mini`)、逐 bit BFSK / FSK (`flash`)、DTMF-like 双音映射 (`pro`) 和 `16-FSK` (`ultra`)。
- 可视化提供 Visual 与 Lyrics 两种模式，用于观察音频信号层和文本编码层。
- 支持多语言界面：英语、德语、西班牙语、法语、意大利语、日语、韩语、波兰语、巴西葡萄牙语、俄语、乌克兰语、简体中文、繁体中文，以及用于营造庄重、宗教感、科技感与太空歌剧氛围的 dog Latin。
- 本地化工作仍在持续优化中。如果您发现翻译错误或有更好的建议，欢迎指正。

## 下载 / 安装
Android APK 将通过 GitHub Releases 发布。

当前参考构建下，安装包约 `5.93 MB`，这些数字会随版本、ABI 与构建配置变化。

## 模式总览
| Mode | 技术类别 | 适合用途 |
| --- | --- | --- |
| `mini` | Morse code | 短促、清晰、节奏可读的点划信号 |
| `flash` | 逐 bit BFSK / FSK | 更强情绪化听感、Visual/Lyrics 对照学习 |
| `pro` | DTMF-like 双音映射 | 更紧凑的双音结构 |
| `ultra` | `16-FSK` 频点映射 | 更短音频、更快生成与解析 |

这些名字不是“强弱等级”，而是项目内部的产品化命名。它们分别强调不同的听感、表达气质和传输结构，而不是同一协议从基础版到高级版的线性升级关系。

## 项目定位
FlipBits 是一个文本与可听音频信号之间的编码/解析工具。它不只把文本转换成声音，也尝试通过逐 bit FSK 的持续时间、停顿间隔、频率组合和播放节奏，让生成音频呈现类似人类不同情绪和语气下说话的听感。

项目可以把文本内容映射为波形，也可以从项目内生成的波形中还原文本；项目本身不提供任何形式的密码学加密。

* **表达重点**：逐 bit BFSK / FSK (`flash`) 会刻意牺牲编码效率，用更长的 bit、停顿和频率变化换取更强的情绪化听感与仪式感。
* **效率补充**：如果需要更短、更快、更正式的文本传输，Morse code (`mini`)、DTMF-like 双音映射 (`pro`) 和 `16-FSK` (`ultra`) 提供了更紧凑的编码路径。`16-FSK` (`ultra`) 不只是生成的音频更短，通常生成消耗和解析耗时也明显低于逐 bit BFSK / FSK (`flash`)；但“更快”不是项目唯一目标。
* **可视化价值**：Android app 提供两种互补的跟随视图。Visual 偏向信号层，展示文本编码后如何变成 FSK low/high bit、频率片段和播放时间轴；Lyrics 偏向文本编码层，用 token 展示文本如何被编码为 UTF-8 bytes、hex/bin 和 bit，并随音频播放高亮。

## Android App 特性
Android app 当前保持轻量原生取向：冷启动速度快，包体控制较小，适合直接生成、转换、分享与导出音频。

## 设计边界
本项目当前重点是“文本 -> 风格化音频 -> 项目内解码”的受控闭环，尤其强调 Android app 内的音频生成、转换、分享与导出体验。

它不以“外放后被另一设备直接实时解析”为主要交互目标，也不以真实环境下的抗噪、抗回声、远场接收或复杂同步鲁棒性为设计优先级。对本项目来说，氛围感、可辨识的风格表达和可控的模式体验，优先于现实声学环境中的通信稳健性。

## 模式说明

### 逐 bit BFSK / FSK (`flash`)

逐 bit BFSK / FSK (`flash`) 是最偏风格化的模式，其听感来自二进制编码的声学化表达：它用高频 / 低频两种 Hz 作为 bit 状态，每个 bit 都只在 low / high 两种频率状态之间切换，对应二进制中的 0 / 1。再通过调整 bit 持续时间、频率配置与停顿间隔，模拟出更接近人类常见的6种说话情绪。

在 Litany 风格下，低速率是一种有意为之的特性，相同文本在 `flash` 下可能生成接近一分钟的音频，而在 `16-FSK` (`ultra`) 下只需几秒。较长的bit 长度与间隔，使得用户可以轻松从音频手动转译二进制。**220 / 440 Hz** (标准 A3/A4 音高)的配置，让人类可以轻松跟随数字信号歌唱。

### 界面预览与样式定义

<div align="center">
  <table style="width: 100%; table-layout: fixed;">
    <tr>
      <td><img src="https://github.com/user-attachments/assets/9fd647f2-b0e6-4a49-93d8-3a24bde45523" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/00e27a38-7475-4866-bf6a-97b089366611" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/8cf04a7b-3fa3-4356-b980-f88b9e72a0c3" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/3f5a97c4-e58f-45ac-b1a8-147f7bba4b55" width="100%"></td>
    </tr>
    <tr align="center">
      <td><small>可视化1</small></td>
      <td><small>可视化2</small></td>
      <td><small>可视化3</small></td>
      <td><small>可视化4</small></td>
    </tr>
  </table>
</div>

目前提供六种 style，通过 bit 持续时间、频率组织与停顿间隔的组合，塑造出不同的情绪化“说话语气”：

| Style | Low / High Hz | 听感目标 |
| :--- | :--- | :--- |
| [Litany](docs/design/modes/flash/litany.md) | `220 / 440` | 低沉、肃穆、吟诵 |
| [Collapse](docs/design/modes/flash/collapse.md) | `226-320 / 452-640` | 低声、慌张、结巴 |
| [Standard](docs/design/modes/flash/standard.md) | `300 / 600` | 日常、精确、平稳 |
| [Hostility](docs/design/modes/flash/hostility.md) | `438-536 / 876-1072` | 尖锐、急促、攻击 |
| [Zeal](docs/design/modes/flash/zeal.md) | `560-900 / 1120-1800` | 明亮、变速、密集 |
| [Void](docs/design/modes/flash/void.md) | `240 / 480` | 低沉、拖尾、稀疏 |



### 演示音频下载

#### 1. 文本内容：`rs`

* **[ 8.8s ]** **Litany**: [flash[litany]-rs.wav](https://github.com/user-attachments/files/27787615/flash.litany.-rs.wav)

#### 2. 文本内容：`github`

以下展示了相同输入在不同语音风格下的听感表现：

* **[ 7.0s ]** **Void**: [flash[void]-github.wav](https://github.com/user-attachments/files/27787632/flash.void.-github.wav)
* **[ 3.6s ]** **Collapse**: [flash[collapse]github.wav](https://github.com/user-attachments/files/27787621/flash.collapse.github.wav)
* **[ 2.5s ]** **Standard**: [flash[standard]-github.wav](https://github.com/user-attachments/files/27787627/flash.standard.-github.wav)
* **[ 2.4s ]** **Hostility**: [flash[hostility]-github.wav](https://github.com/user-attachments/files/27787626/flash.hostility.-github.wav)
* **[ 1.8s ]** **Zeal**: [flash[zeal]-github.wav](https://github.com/user-attachments/files/27787637/flash.zeal.-github.wav)

### 设计细节

更多 `flash` voicing style 的情绪定位、命名语义与 preset 设计见：
- [`docs/design/modes/flash/README.md`](docs/design/modes/flash/README.md)
- [`docs/design/modes/flash/voicing-emotions.md`](docs/design/modes/flash/voicing-emotions.md)
- [`docs/design/modes/flash/`](docs/design/modes/flash/)


### Morse code (`mini`)

Morse code (`mini`) 按 Morse 规则规范化输入，强调清晰的可视化效果与点划节奏。其设计的核心在于**“节奏的可见性”**：通过 UI 的实时反馈，将抽象的电码转化为直观的视觉进度。

#### 速率预设与视觉跟随
目前提供三种 Speed Preset，旨在平衡“手动识别的可行性”与“传输效率”：

<div align="center">
  <table style="width: 100%; table-layout: fixed;">
    <tr>
      <td><img src="https://github.com/user-attachments/assets/d7d4c86e-e0cb-4ab4-9063-be437f794986" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/d6c1ec7e-2de6-438d-b3c7-5dee5bb17f1c" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/6517cb87-9024-4bb5-b382-a4d600e70a6d" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/55356088-a0cf-4a4e-9a75-3af2bca7127c" width="100%"></td>
    </tr>
    <tr align="center">
      <td><small>页面</small></td>
      <td><small>可视化1 </small></td>
      <td><small>可视化2</small></td>
      <td><small>单词选择</small></td>
    </tr>
  </table>
</div>

| Speed | 定位 | 设计目标 |
| :--- | :--- | :--- |
| **Slow** | 极慢速 | 教学级速率，最适合观察 dot / dash 与词词对照 (Lyrics Follow) |
| **Standard** | 标准节奏 | 模拟传统 Morse 电码的经典节奏感与识别度 |
| **Fast** | 紧凑型 | 压缩点划间隔，提供更高效、更短促的音频输出 |

#### 演示音频下载
**文本内容**：`github`

*   **[ 4.0s ]** **Slow**: [mini_slow_github.wav](https://github.com/user-attachments/files/27787705/mini_slow_github.wav)
*   **[ 2.7s ]** **Standard**: [mini_standard_github.wav](https://github.com/user-attachments/files/27787714/mini_standard_github.wav)
*   **[ 1.3s ]** **Fast**: [mini_fast_github.wav](https://github.com/user-attachments/files/27787703/mini_fast_github.wav)

### 设计细节
关于 `mini` 的输入规范、Visual 实时跟随逻辑与具体的预设参数说明见：
- [`docs/design/modes/mini.md`](docs/design/modes/mini.md)


### DTMF-like 双音映射 (`pro`)

听感上，这是一种**纯粹的电话拨号音**。它是一种标准的 ASCII-only 模式：通过将字节拆分为高低位并映射为双音信号（DTMF），实现 `1 byte = 2 symbol` 的精确传输。它抛弃了冗余的修饰，追求极致清晰、职责单纯的声学链路反馈。

#### Interface Preview

<div align="center">
  <table style="width: 100%; table-layout: fixed;">
    <tr>
      <td><img src="https://github.com/user-attachments/assets/91b4a9c2-52c0-4ce2-96b1-5f29741e6ac1" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/22f60b1b-2f5a-48cf-a9af-afafaaf4143c" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/53dbd62a-5a8c-49ce-9af4-22531c691735" width="100%"></td>
    </tr>
    <tr align="center">
      <td><small>可视化 1</small></td>
      <td><small>可视化 2</small></td>
      <td><small>单词选择</small></td>
    </tr>
  </table>
</div>


### 演示音频下载
* **[ 3.7s ]** **文本内容**：`RED STEEL RECEIVES NEW SERIAL NUMBERS`
* **下载地址**: [RED STEEL RECE_pro_20260515_105734.wav](https://github.com/user-attachments/files/27786956/RED.STEEL.RECE_pro_20260515_105734.wav)


### 设计细节
更多 `pro` 的模式定位与实现说明见：
- [`docs/design/modes/pro.md`](docs/design/modes/pro.md)
- [`docs/design/transports.md`](docs/design/transports.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)

- 
### `16-FSK` 频点映射 (`ultra`)

`16-FSK` (`ultra`) 是面向 UTF-8 文本的高密度模式。它采用更轻量的生成与解析路径，将输入文本按字节拆分为两个 Nibble，映射至 16 阶固定频点。

**1 byte = 2 symbols**。相比 `pro` 模式的双音多频，`ultra` 在每个 Symbol 中只发送单一频点，追求更纯粹的信息密度与处理速度，是正式、长文本传输的首选方案。

#### 界面预览
<div align="center">
  <table style="width: 100%; table-layout: fixed;">
    <tr>
      <td><img src="https://github.com/user-attachments/assets/eebede6a-d307-41ad-8b17-f533b2bffb65" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/f8a28bbf-998a-4c74-850d-fe24b34af50a" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/cb7b3813-b49b-4458-8047-ee5e3b0aa5e1" width="100%"></td>
    </tr>
    <tr align="center">
      <td><small>可视化1</small></td>
      <td><small>可视化2</small></td>
      <td><small>单词选择</small></td>
    </tr>
  </table>
</div>

#### 性能压测对照 (7000 字符/字节文本)
| 维度 | `flash` (逐 bit BFSK) | `ultra` (16-FSK) | 差异 |
| :--- | :--- | :--- | :--- |
| **生成耗时** | ~ 116.0 秒 | **~ 2.0 秒** | 58x 提速 |
| **音频长度** | ~ 43.0 分钟 | **~ 11.7 分钟** | 3.7x 压缩 |
| **核心定位** | 仪式感、可听性、情绪化 | **高吞吐、快处理、工业感** | - |

---

### 演示音频：效率与质感的对比
**文本内容**：`完好的源端矩阵`

| 模式 | 风格 | 耗时 | 下载 |
| :--- | :--- | :--- | :--- |
| **`ultra`** | **默认** | **5.4s** | [下载音频](https://github.com/user-attachments/files/27787427/ultra-.wav) |
| `flash` | Zeal (极速) | 13.8s | [下载音频](https://github.com/user-attachments/files/27787423/flash.zeal.-.wav) |
| `flash` | Standard (标准) | 20.5s | [下载音频](https://github.com/user-attachments/files/27787417/flash.standard.-.wav) |
| `flash` | Litany (肃穆) | 173.4s | [下载音频](https://github.com/user-attachments/files/27787454/flash.litany.-.wav) |

> **注**：`litany` 风格音频长达 2.9 分钟，这是为了“风格化”有意设计的刻意冗余，旨在达到人类可清晰辨识、记录甚至朗读的频率间隔。

---

### 设计细节
关于 `ultra` 的模式定位、传输层协议及系统架构设计见：
- [`docs/design/modes/ultra.md`](docs/design/modes/ultra.md)
- [`docs/design/transports.md`](docs/design/transports.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)

---

#### 致谢
`ultra` 模式在实现思路上参考了 [ggerganov/ggwave](https://github.com/ggerganov/ggwave) 的公开工程设计与声学传输实践，特此致谢。
*本项目为独立实现；除仓库中已明确标注的第三方组件外，不主张与该项目存在隶属、背书或官方关系。*

---

## 使用边界与责任说明 (Usage & Liability)

### 图标使用说明（公共资源）
FlipBits 图标资源（包括源文件与组件 SVG 文件）作为公共资源向社区开放使用。

在遵守本仓库许可证与适用法律的前提下，你可以将这些图标用于勋章、二创、视频展示与商业发布（包括售卖），无需向项目方额外申请单独授权。

若用于公开传播，建议标注：
`Icon designed by FlipBits Project`

限制与边界（法律保守版）：
- 本授权仅覆盖图标资源本身，不授予商标权、专利权、人格权或任何未明确授予的权利。
- 图标按“现状”提供，不附带任何明示或默示担保；使用者应自行承担合规与风险责任。
- 不得将这些图标资源私有化、独占声明，或再授权为专有/排他性资源。
- 不得以任何方式暗示你对 FlipBits 项目或作者拥有官方代表、背书或唯一授权关系。

### 1. 原理与限制 (Principles & Limitations)
* **协议公开透明**：本工具使用的传输方式属于**公开、通用的声学编码方法**，包括高低频切换、双音映射和多频点映射等形式。其本质是对文本字节或符号进行有序编码，不提供绕过安全审查的加密或隐写（Steganography）能力。
* **用途定位明确**：本项目用于**音频信号处理（DSP）研究、声学通信原理验证，以及相关编解码性能实验**，并非为隐蔽通信场景设计。
* **娱乐表达优先**：部分模式会刻意保留冗长、低速和强风格化的音频外观，以服务整体氛围表达；效率不是所有模式的首要目标。
* **不承诺现实环境鲁棒性**：当前重点是“生成音频 -> 解析生成音频”的主链路闭环，不承诺在真实播放、录音、噪声、回声、削波、设备频响偏差或远距离传播条件下的稳定接收表现。
* **使用责任自负**：开发者提供的是源代码与实现方法。用户在运行、修改或部署本项目时，应自行确保其用途符合所在地法律法规、平台规则与网络安全要求。
* **原样分发 (As-Is)**：本软件按现状提供。在适用法律允许的范围内，作者不对其适用性、稳定性或因使用本软件造成的损失承担赔偿责任。

### 2. 风格与知识产权 (Style & IP)
* **独立开源项目**：本项目为独立开发的非官方开源项目，不隶属于任何影视、游戏或商业品牌，也不代表任何第三方立场。
* **无关联声明**：本项目为独立原创作品，与 Games Workshop 及 Warhammer 40,000 无任何隶属、授权、赞助、认可或其他关联关系。
* **风格来源说明**：项目在视觉与文案气质上参考了复古未来主义、工业美学、宗教式仪式感表达等通用创作方向，但不使用任何受保护世界观中的专有设定作为项目基础。
* **内容处理原则**：若仓库中出现可能引发混淆、侵权或不当联想的素材、命名或表述，欢迎通过 [GitHub Issues](../../issues) 提出，我们会及时评估与修正。

---

## 快速开始

> 若发现仓库中存在不准确、或可能引发误解的内容，欢迎通过 [GitHub Issues](../../issues) 反馈。

若你是 AI / agent，建议先阅读 [`.agent/AGENTS.md`](.agent/AGENTS.md) 以及对应子系统下的 `AGENTS.md`，用来快速理解仓库结构、工具入口与修改约定。

### Android
- Android 官方工程入口在 `C:\code\FlipBits\apps\audio_android`。
- 从仓库根目录统一执行：
  - `python tools/run.py android assemble-debug`
  - `python tools/run.py android assemble-release`
  - `python tools/run.py android native-debug`
- `apps/audio_android` 是 Android Gradle root，`apps/audio_android/app` 是实际应用模块。
- Android Studio 建议直接打开 `apps/audio_android`。

### 本地编排工具
- 推荐统一使用 `python tools/run.py <command>`。
- 常用命令：
  - `python tools/run.py build --build-dir build/dev`
  - `python tools/run.py clean`
  - `python tools/run.py verify --build-dir build/dev --skip-android`
  - `python tools/run.py android native-debug`
  - `python tools/run.py android assemble-debug`
  - `python tools/run.py artifact export-apk`
- 约定：
  - `python tools/run.py --help` 只看主命令概览；详细参数用 `python tools/run.py <command> --help`。
  - host 根目录当前直接固定为一条正式主线：`clang++ + Ninja + build/dev`。
  - `python tools/run.py verify --build-dir build/dev --skip-android` 只验证 host 默认 modules 主路径。
  - Android native 侧通过 `apps/audio_android/native_package -> bag_android_native` 独立装配；剩余 `C++17` 例外被限制在 package-private wrapper 与 `android_bag/**` 私有声明层。
  - `build/` 继续保留给 CMake / Gradle 的原生构建输出与测试产物。
  - 根目录 `dist/` 只存放 Python 导出的最终交付物；当前 Android APK 默认导出到 `dist/android/`。

### 开发导航
按模块阅读或修改时，可优先从以下入口进入：

- agent / AI 总入口：[`.agent/AGENTS.md`](.agent/AGENTS.md)
- 核心库与共享业务逻辑：[`libs/AGENTS.md`](libs/AGENTS.md)
- CLI 表现层：[`apps/audio_cli/AGENTS.md`](apps/audio_cli/AGENTS.md)
- Android 应用：[`apps/audio_android/AGENTS.md`](apps/audio_android/AGENTS.md)

更多仓库结构与工具说明见：
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)
- [`tools/README.md`](tools/README.md)
