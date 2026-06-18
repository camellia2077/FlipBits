<p align="center">
  <img src="ui/app/icon-foreground.svg" alt="FlipBits icon" width="128" />
</p>

<p align="center">
  <em>Icon designed for <a href="https://github.com/camellia2077/FlipBits">camellia2077/FlipBits</a></em>
</p>

<h1 align="center">FlipBits</h1>

<p align="center">
  中文 | <a href="README_en.md">English</a>
</p>

<p align="center">
  <strong>把文本编码变成声音，也把声音背后的 bit、byte、token 与节奏变成可观看的表演。</strong><br />
  <em>FlipBits 是一个融合编码转换、音频可视化与编码可视化的项目：它支持 Morse、BFSK / FSK、双音映射和多频点映射；`flash` 模式还会用 bit 持续时间、停顿和 Hz 变化模拟不同“说话语气”，让文本传输既像协议，也像人类在以不同情绪说话。</em>
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-red.svg)](LICENSE)
[![Platform Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg)]()
[![CI Android Assemble](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-assemble.yml/badge.svg)](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-assemble.yml)
[![CI Android Quality](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-quality.yml/badge.svg)](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-quality.yml)
[![CI Host Verify](https://github.com/camellia2077/FlipBits/actions/workflows/ci-host-verify.yml/badge.svg)](https://github.com/camellia2077/FlipBits/actions/workflows/ci-host-verify.yml)

<table>
  <tr>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/3c1a3eaf-17e5-4b19-b4d3-27c642aee92d" alt="主界面"><br>
      <sub>主界面</sub>
    </td>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/66efd1fb-61a6-485a-bf69-611646cef57b" alt="歌词式token同步显示"><br>
      <sub>歌词式token同步显示</sub>
    </td>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/2b5e3d1c-c61c-4967-926c-ff93d63c1160" alt="自适应布局"><br>
      <sub>自适应布局</sub>
    </td>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/bb82b369-ef65-4c55-83fc-7ef515a0007f" alt="音频可视化"><br>
      <sub>音频可视化</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/0ec94cc2-8a15-4624-8e23-76900e7495e6" alt="自定义双色主题"><br>
      <sub>自定义双色主题</sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/abae9141-ef48-4ccf-97c0-2c3eb3e2f07c" alt="material主题"><br>
      <sub>material主题</sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/911bd395-81ac-4a2b-ade8-6806e8cec6c2" alt="morse code"><br>
      <sub>morse code</sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/d55089ff-f4cf-4dfb-85e1-488ce2b62658" alt="16 fsk"><br>
      <sub>16 fsk</sub>
    </td>
  </tr>
</table>


## 快速概览

- Android 原生应用，把计算机文本编码转成可听音频，并把编码层与信号层同步可视化。
- 支持 Morse code (`mini`)、逐 bit BFSK / FSK (`flash`)、DTMF-like 双音映射 (`pro`) 和 `16-FSK` (`ultra`)。
- Android 里的 `voice` 变声器支持 `single track` 和 `dual track` 两种工作方式：`single track` 直接处理输入音频，`dual track` 则把输入轨和副轨分开处理，让副轨复用 `flash` 的 voicing style，形成更明显的机械感和层次感。
- 可视化提供 Visual 与 Lyrics 两种模式，用于同时观察音频信号层、文本编码层，以及 token / byte / bit 的播放跟随关系。
- 可以帮助理解 CJK、Latin、Cyrillic 等文本在 UTF-8 下每个字符占用多少 bytes，以及它们如何被进一步展开为 hex / binary / bits。
- `mini` 模式既能生成和解析 Morse code，也适合作为文本与 Morse 相互转换、节奏学习和人工识读的辅助工具。
- 支持多语言界面：英语、德语、西班牙语、法语、意大利语、日语、韩语、波兰语、巴西葡萄牙语、俄语、乌克兰语、简体中文、繁体中文，以及用于营造庄重、宗教感、科技感与太空歌剧氛围的 dog Latin。
- 本地化工作仍在持续优化中。如果您发现翻译错误或有更好的建议，欢迎指正。

## 下载 / 安装

Android APK 将通过 GitHub Releases 发布。

当前参考构建下，安装包约 `6.71 MB`(按 Android 系统千进制换算)，这些数字会随版本、ABI 与构建配置变化。

## 在线体验

如果你想在不安装 APK 的情况下直接试听、生成、解析和下载音频，可以使用 GitHub Pages 在线 Demo：

- [FlipBits Pages 在线体验](https://camellia2077.github.io/FlipBits/)

Pages 支持输入文本后直接生成不同模式的音频，并可在线播放、解析项目内生成的结果以及下载音频文件。当前项目仍以逐 bit BFSK / FSK (`flash`) 的风格化表达为主要重点。

Android 用户可读更新摘要见：

- [`docs/history/presentation/android/CHANGELOG.zh-CN.md`](docs/history/presentation/android/CHANGELOG.zh-CN.md)

## 模式总览

| Mode | 技术类别 | 适合用途 |
| --- | --- | --- |
| `mini` | Morse code | 短促、清晰、节奏可读的点划信号 |
| `flash` | 逐 bit BFSK / FSK | 更强情绪化听感、Visual/Lyrics 对照学习 |
| `pro` | DTMF-like 双音映射 | 更紧凑的双音结构 |
| `ultra` | `16-FSK` 频点映射 | 更短音频、更快生成与解析 |

这些名字不是“强弱等级”，而是项目内部的产品化命名。它们分别强调不同的听感、表达气质和传输结构，而不是同一协议从基础版到高级版的线性升级关系。

## 项目定位

FlipBits 是一个把文本编码、可听音频信号和可视化界面绑在一起的项目。它不只把文本转换成声音，也试图把文本在机器内部的编码结构直接展示出来，包括 token、character、byte、hex、binary 与 bit 如何进入播放时间轴。

项目可以把文本内容映射为波形，也可以从项目内生成的波形中还原文本；项目本身不提供任何形式的密码学加密，也不把“隐藏通信”作为设计目标。

- **编码理解**：项目可以帮助理解 CJK、Latin、Cyrillic 等不同文字在 UTF-8 下每个字符占几个 bytes，以及它们如何继续被展开成 hex、binary 与 bit。对想直观看懂“文本进入机器后到底长什么样”的用户来说，这比只看文档或十六进制表更直接。
- **Morse 学习**：`mini` 模式不只是把文本转成 Morse 声音，也适合作为文本与 Morse 相互转换、点划节奏学习和手动识读的辅助界面。
- **表达重点**：逐 bit BFSK / FSK (`flash`) 会刻意牺牲编码效率，用更长的 bit、停顿和频率变化换取更强的情绪化听感与拟人“说话语气”。它更像在用信号结构模拟情绪化说话，而不是只追求最快传输。
- **效率补充**：如果需要更短、更快、更正式的文本传输，Morse code (`mini`)、DTMF-like 双音映射 (`pro`) 和 `16-FSK` (`ultra`) 提供了更紧凑的编码路径。`16-FSK` (`ultra`) 不只是生成的音频更短，通常生成消耗和解析耗时也明显低于逐 bit BFSK / FSK (`flash`)；但“更快”不是项目唯一目标。
- **可视化价值**：Android app 提供两种互补的跟随视图。Visual 偏向信号层，展示文本编码后如何变成 FSK low/high bit、频率片段和播放时间轴；Lyrics 偏向文本编码层，用 token 展示文本如何被编码为 UTF-8 bytes、hex/bin 和 bit，并随音频播放高亮。

## Android App 特性

Android app 当前保持轻量原生取向：冷启动速度快，包体控制较小，适合直接生成、转换、分享与导出音频。

它不只是一个把文本转换成 WAV 的生成工具，还是项目中承载可视化、主题表达与学习体验的主要界面：

- **双视图跟随**：Visual 用于看信号、频率、节奏和时间轴；Lyrics 用于看 token、UTF-8 bytes、hex / binary / bits 如何跟着播放推进。
- **主题表达**：除了 `Material` 外，还提供更偏观赏性和气氛化的三色主题（由主色、次色与描边色构成），让不同模式和风格拥有更强的视觉气质。
- **学习与展示并重**：它既能帮助理解编码结构，也强调“看起来和听起来都成立”的展示效果，因此 UI、主题、动画和可视化不是附属功能，而是产品的一部分。
- **Voice mode**：Android voice 现在包含类似变声器的音频处理能力，分为 `single track` 和 `dual track`。`single track` 直接作用于输入音频，适合单一路径的风格化处理；`dual track` 会把输入主轨和副轨拆开，副轨复用 `flash` voicing style，再与主轨混合，适合更有机械层次的听感。

## 设计边界

本项目当前重点是“文本 -> 风格化音频 -> 项目内解码”的受控闭环，尤其强调 Android app 内的音频生成、转换、分享与导出体验。

它不以“外放后被另一设备直接实时解析”为主要交互目标，也不以真实环境下的抗噪、抗回声、远场接收或复杂同步鲁棒性为设计优先级。对本项目来说，氛围感、可辨识的风格表达和可控的模式体验，优先于现实声学环境中的通信稳健性。

## 深入阅读

README 只保留项目首页需要的信息。更细的模式设计、参数、实现边界和架构说明见：

- [`docs/design/modes/README.md`](docs/design/modes/README.md)
- [`docs/design/modes/flash/README.md`](docs/design/modes/flash/README.md)
- [`docs/design/modes/voice/README.md`](docs/design/modes/voice/README.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)
## 使用边界与责任说明

- FlipBits 使用公开、通用的声学编码与音频处理方法，不提供加密、隐写或绕过安全审查的能力。
- 项目重点是音频信号处理、编码可视化、风格化声音表达和受控的项目内生成/解析闭环，不承诺真实外放、远场录音、噪声、回声或设备差异下的稳定通信表现。
- FlipBits 是独立开源项目，不隶属于任何影视、游戏或商业品牌；视觉和文案气质只参考复古未来主义、工业美学、仪式化表达等通用创作方向。
- 软件和图标资源按仓库许可证与“现状”提供，使用者应自行承担部署、发布、二创、商业使用和合规责任。

更详细的分发声明、商标边界与图标使用说明见 [NOTICE](NOTICE) 与 [docs/legal/TRADEMARKS.md](docs/legal/TRADEMARKS.md)。
## 快速开始

- Android 工程入口：[`apps/audio_android`](apps/audio_android)
- 在线 Demo：[`FlipBits Pages`](https://camellia2077.github.io/FlipBits/)
- APK：通过 GitHub Releases 发布

如果你是开发者或 AI / agent，请从 [`.agent/AGENTS.md`](.agent/AGENTS.md) 开始。Android、CLI、libs 的具体构建、测试和修改约定已下沉到各子系统 `AGENTS.md`，README 不再展开维护这些内部流程。
