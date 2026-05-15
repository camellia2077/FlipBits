<p align="center">
  <img src="ui/app/icon-foreground.svg" alt="FlipBits icon" width="128" />
</p>

<p align="center">
  <em>Icon designed by camellia2077 (FlipBits Project)</em>
</p>

<h1 align="center">FlipBits</h1>


<p align="center">
  <a href="README.md">中文</a> | English
</p>

<p align="center">
  <strong>A stylized encoding/decoding tool between text and audio signals, generating audible "tone-based" encoded audio using FSK rhythms, Hz variations, and different pause intervals</strong><br />
  <em>A tool covering rhythmic BFSK signals, Dual-tone mapping, and efficient 16-FSK transmission</em>
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

## Quick Overview
- Native Android application for the visualization and sonification of computer text encoding.
- Supports Morse code (`mini`), bit-by-bit BFSK / FSK (`flash`), DTMF-like dual-tone mapping (`pro`), and `16-FSK` (`ultra`).
- Visualization provides two modes: Visual and Lyrics, used to observe the audio signal layer and the text encoding layer respectively.
- Supports multilingual interfaces: English, German, Spanish, French, Italian, Japanese, Korean, Polish, Brazilian Portuguese, Russian, Ukrainian, Simplified Chinese, Traditional Chinese, as well as dog Latin to create solemn, religious, sci-fi, and space opera atmospheres.
- Localization work is continuously being optimized. If you find translation errors or have better suggestions, corrections are warmly welcomed.

## Download / Install
The Android APK will be published via GitHub Releases.

Under the current reference build, the installation package is approximately `5.93 MB`. These numbers will vary depending on the version, ABI, and build configurations.

## Modes Overview
| Mode | Technology Category | Suitable Uses |
| --- | --- | --- |
| `mini` | Morse code | Short, clear, rhythmically readable dot-dash signals |
| `flash` | Bit-by-bit BFSK / FSK | Stronger emotional listening experience, Visual/Lyrics comparison learning |
| `pro` | DTMF-like dual-tone mapping | More compact dual-tone structure |
| `ultra` | `16-FSK` frequency mapping | Shorter audio, faster generation and parsing |

These names are not "power levels", but rather productized naming within the project. They emphasize different listening experiences, expressive temperaments, and transmission structures, rather than representing a linear upgrade from a basic to an advanced version of the same protocol.

## Project Positioning
FlipBits is an encoding/decoding tool between text and audible audio signals. It doesn't just convert text into sound; it also attempts to give the generated audio a listening experience similar to human speech under different emotions and tones by adjusting the duration, pause intervals, frequency combinations, and playback rhythm of bit-by-bit FSK.

The project can map text content into waveforms and can also restore text from the waveforms generated within the project. The project itself does not provide any form of cryptographic encryption.

* **Expressive Focus**: Bit-by-bit BFSK / FSK (`flash`) deliberately sacrifices encoding efficiency, trading longer bits, pauses, and frequency changes for a stronger emotional listening experience and a sense of ritual.
* **Efficiency Supplement**: If shorter, faster, and more formal text transmission is needed, Morse code (`mini`), DTMF-like dual-tone mapping (`pro`), and `16-FSK` (`ultra`) provide more compact encoding paths. `16-FSK` (`ultra`) not only generates shorter audio, but its generation and parsing time is also usually significantly lower than bit-by-bit BFSK / FSK (`flash`); however, being "faster" is not the sole goal of the project.
* **Visualization Value**: The Android app provides two complementary tracking views. "Visual" leans toward the signal layer, showing how text encoding turns into FSK low/high bits, frequency segments, and the playback timeline; "Lyrics" leans toward the text encoding layer, using tokens to show how text is encoded into UTF-8 bytes, hex/bin, and bits, highlighting them as the audio plays.

## Android App Features
The Android app currently maintains a lightweight, native approach: fast cold start speed and small package size, making it suitable for directly generating, converting, sharing, and exporting audio.

## Design Boundaries
The current focus of this project is a controlled closed loop of "text -> stylized audio -> in-project decoding", with a particular emphasis on the audio generation, conversion, sharing, and export experience within the Android app.

It does not take "direct real-time parsing by another device after external playback" as its main interactive goal, nor does it prioritize anti-noise, anti-echo, far-field reception, or complex synchronization robustness in real-world environments as design priorities. For this project, atmosphere, recognizable style expression, and a controlled mode experience take precedence over communication robustness in real-world acoustic environments.

## Mode Instructions

### Bit-by-bit BFSK / FSK (`flash`)

Bit-by-bit BFSK / FSK (`flash`) is the most highly stylized mode. Its listening experience comes from the acoustic expression of binary encoding: it uses high/low Hz as bit states, with each bit only switching between low and high frequency states, corresponding to 0 / 1 in binary. Then, by adjusting bit duration, frequency configuration, and pause intervals, it simulates listening experiences closer to 6 common human speaking emotions.

In the Litany style, low speed is an intentional feature. The same text might generate nearly a minute of audio under `flash`, while it takes only a few seconds under `16-FSK` (`ultra`). The longer bit lengths and intervals allow users to easily manually transcribe binary from the audio. The **220 / 440 Hz** (standard A3/A4 pitch) configuration makes it easy for humans to sing along with the digital signals.

### Interface Preview and Style Definition

<div align="center">
  <table style="width: 100%; table-layout: fixed;">
    <tr>
      <td width="25%"><img src="https://github.com/user-attachments/assets/cbf5af39-7069-42b3-aeca-07d219542f83" width="100%"></td>
      <td width="25%"><img src="https://github.com/user-attachments/assets/2bd1b34b-962e-473e-b105-1964305a58d8" width="100%"></td>
      <td width="25%"><img src="https://github.com/user-attachments/assets/29db01d6-8164-47f0-af1a-2edb661060ed" width="100%"></td>
      <td width="25%"><img src="https://github.com/user-attachments/assets/02dcddce-ed31-4c9a-8156-fd629bc21c1b" width="100%"></td>
    </tr>
    <tr align="center">
      <td><small>Visualization 1</small></td>
      <td><small>Visualization 2</small></td>
      <td><small>Visualization 3</small></td>
      <td><small>Visualization 4</small></td>
    </tr>
  </table>
</div>

Currently, six styles are provided. Through combinations of bit duration, frequency organization, and pause intervals, they shape different emotional "speaking tones":

| Style | Low / High Hz | Listening Goal |
| :--- | :--- | :--- |
| [Litany](docs/design/modes/flash/litany.md) | `220 / 440` | Deep, solemn, chanting |
| [Collapse](docs/design/modes/flash/collapse.md) | `226-320 / 452-640` | Whispering, panicked, stuttering |
| [Standard](docs/design/modes/flash/standard.md) | `300 / 600` | Daily, precise, steady |
| [Hostility](docs/design/modes/flash/hostility.md) | `438-536 / 876-1072` | Sharp, rapid, aggressive |
| [Zeal](docs/design/modes/flash/zeal.md) | `560-900 / 1120-1800` | Bright, variable speed, dense |
| [Void](docs/design/modes/flash/void.md) | `240 / 480` | Deep, trailing, sparse |



### Demo Audio Download

#### 1. Text Content: `rs`

* **[ 8.8s ]** **Litany**: [flash[litany]-rs.wav](https://github.com/user-attachments/files/27787615/flash.litany.-rs.wav)

#### 2. Text Content: `github`

The following shows the auditory performance of the same input under different voicing styles:

* **[ 7.0s ]** **Void**: [flash[void]-github.wav](https://github.com/user-attachments/files/27787632/flash.void.-github.wav)
* **[ 3.6s ]** **Collapse**: [flash[collapse]github.wav](https://github.com/user-attachments/files/27787621/flash.collapse.github.wav)
* **[ 2.5s ]** **Standard**: [flash[standard]-github.wav](https://github.com/user-attachments/files/27787627/flash.standard.-github.wav)
* **[ 2.4s ]** **Hostility**: [flash[hostility]-github.wav](https://github.com/user-attachments/files/27787626/flash.hostility.-github.wav)
* **[ 1.8s ]** **Zeal**: [flash[zeal]-github.wav](https://github.com/user-attachments/files/27787637/flash.zeal.-github.wav)

### Design Details

For more details on the emotional positioning, naming semantics, and preset designs of the `flash` voicing style, see:
- [`docs/design/modes/flash/README.md`](docs/design/modes/flash/README.md)
- [`docs/design/modes/flash/voicing-emotions.md`](docs/design/modes/flash/voicing-emotions.md)
- [`docs/design/modes/flash/`](docs/design/modes/flash/)


### Morse code (`mini`)

Morse code (`mini`) standardizes input according to Morse rules, emphasizing clear visual effects and dot-dash rhythms. The core of its design lies in the **"visibility of rhythm"**: through real-time UI feedback, abstract codes are transformed into intuitive visual progress.

#### Speed Presets and Visual Follow
Three Speed Presets are currently provided, aiming to balance "feasibility of manual recognition" and "transmission efficiency":

<div align="center">
  <table style="width: 100%; table-layout: fixed;">
    <tr>
      <td width="25%"><img src="https://github.com/user-attachments/assets/d79e0302-b13f-4ec7-9136-66dbdc7bb00c" style="width: 100%;"></td>
      <td width="25%"><img src="https://github.com/user-attachments/assets/f50e4b9c-e6b3-4193-b957-bd9bf4a12d64" style="width: 100%;"></td>
      <td width="25%"><img src="https://github.com/user-attachments/assets/726dd727-bc20-4f1f-9564-f49095f9eddd" style="width: 100%;"></td>
      <td width="25%"><img src="https://github.com/user-attachments/assets/716cdb9b-0f37-4692-9942-1b1a4d48cc32" style="width: 100%;"></td>
    </tr>
    <tr align="center">
      <td><small>Page</small></td>
      <td><small>Visualization 1</small></td>
      <td><small>Visualization 2</small></td>
      <td><small>Word Selection</small></td>
    </tr>
  </table>
</div>

| Speed | Positioning | Design Goal |
| :--- | :--- | :--- |
| **Slow** | Extremely slow | Teaching-level speed, best for observing dot / dash and word-by-word comparison (Lyrics Follow) |
| **Standard** | Standard rhythm | Simulates the classic rhythmic feel and recognizability of traditional Morse code |
| **Fast** | Compact | Compresses dot-dash intervals, providing a more efficient, shorter audio output |

#### Demo Audio Download
**Text Content**: `github`

*   **[ 4.0s ]** **Slow**: [mini_slow_github.wav](https://github.com/user-attachments/files/27787705/mini_slow_github.wav)
*   **[ 2.7s ]** **Standard**: [mini_standard_github.wav](https://github.com/user-attachments/files/27787714/mini_standard_github.wav)
*   **[ 1.3s ]** **Fast**: [mini_fast_github.wav](https://github.com/user-attachments/files/27787703/mini_fast_github.wav)

### Design Details
For `mini`'s input specifications, Visual real-time follow logic, and specific preset parameter descriptions, see:
- [`docs/design/modes/mini.md`](docs/design/modes/mini.md)


### DTMF-like Dual-tone Mapping (`pro`)

Auditorily, this is a **pure telephone dial tone**. It is a standard ASCII-only mode: by splitting bytes into high and low bits and mapping them to dual-tone signals (DTMF), it achieves a precise transmission of `1 byte = 2 symbols`. It discards redundant embellishments, pursuing the ultimate clarity and pure acoustic link feedback.

#### Interface Preview

<div align="center">
  <table style="width: 100%; table-layout: fixed;">
    <tr>
      <td><img src="https://github.com/user-attachments/assets/d991b7a2-4e88-4e4c-88e7-e897d6b0bd78" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/0f88277d-42f2-4618-a745-8648d795f15d" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/ac6b33f1-8ece-4644-ba9a-9483690e751e" width="100%"></td>
    </tr>
    <tr align="center">
      <td><small>Visualization 1</small></td>
      <td><small>Visualization 2</small></td>
      <td><small>Word Selection</small></td>
    </tr>
  </table>
</div>

### Demo Audio Download
* **[ 3.7s ]** **Text Content**: `RED STEEL RECEIVES NEW SERIAL NUMBERS`
* **Download Link**: [RED STEEL RECE_pro_20260515_105734.wav](https://github.com/user-attachments/files/27786956/RED.STEEL.RECE_pro_20260515_105734.wav)


### Design Details
For more descriptions of `pro`'s mode positioning and implementation, see:
- [`docs/design/modes/pro.md`](docs/design/modes/pro.md)
- [`docs/design/transports.md`](docs/design/transports.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)


### `16-FSK` Frequency Point Mapping (`ultra`)

`16-FSK` (`ultra`) is a high-density mode aimed at UTF-8 text. It uses a more lightweight generation and parsing path, splitting input text bytes into two Nibbles and mapping them to 16 fixed frequency points.

**1 byte = 2 symbols**. Compared to the dual-tone multi-frequency of the `pro` mode, `ultra` only sends a single frequency point in each Symbol, pursuing purer information density and processing speed. It is the preferred solution for formal, long-text transmission.

#### Interface Preview
<div align="center">
  <table style="width: 100%; table-layout: fixed;">
    <tr>
      <td><img src="https://github.com/user-attachments/assets/96b3862f-771e-4c98-984e-30ea5411fd0b" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/6f5c8cad-babe-4157-ba59-e4f7838d1fb8" width="100%"></td>
      <td><img src="https://github.com/user-attachments/assets/8dc9bf72-d0d9-4c53-85eb-435e60062cc1" width="100%"></td>
    </tr>
    <tr align="center">
      <td><small>Visualization 1</small></td>
      <td><small>Visualization 2</small></td>
      <td><small>Word Selection</small></td>
    </tr>
  </table>
</div>


#### Performance Benchmark Comparison (7000 Characters/Bytes Text)
| Dimension | `flash` (Bit-by-bit BFSK) | `ultra` (16-FSK) | Difference |
| :--- | :--- | :--- | :--- |
| **Generation Time** | ~ 116.0 seconds | **~ 2.0 seconds** | 58x speedup |
| **Audio Length** | ~ 43.0 minutes | **~ 11.7 minutes** | 3.7x compression |
| **Core Positioning** | Ritualistic, audibility, emotional | **High throughput, fast processing, industrial feel** | - |

---

### Demo Audio: Comparison of Efficiency and Texture
**Text Content**: `完好的源端矩阵`

| Mode | Style | Time Cost | Download |
| :--- | :--- | :--- | :--- |
| **`ultra`** | **Default** | **5.4s** | [Download audio](https://github.com/user-attachments/files/27787427/ultra-.wav) |
| `flash` | Zeal (Extreme speed) | 13.8s | [Download audio](https://github.com/user-attachments/files/27787423/flash.zeal.-.wav) |
| `flash` | Standard | 20.5s | [Download audio](https://github.com/user-attachments/files/27787417/flash.standard.-.wav) |
| `flash` | Litany (Solemn) | 173.4s | [Download audio](https://github.com/user-attachments/files/27787454/flash.litany.-.wav) |

> **Note**: The `litany` style audio is up to 2.9 minutes long. This is deliberate redundancy designed for "stylization", aiming to achieve frequency intervals that humans can clearly identify, record, or even read aloud.

---

### Design Details
For the mode positioning, transport layer protocol, and system architecture design of `ultra`, see:
- [`docs/design/modes/ultra.md`](docs/design/modes/ultra.md)
- [`docs/design/transports.md`](docs/design/transports.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)

---

#### Acknowledgements
The implementation approach of the `ultra` mode referenced the public engineering design and acoustic transmission practices of [ggerganov/ggwave](https://github.com/ggerganov/ggwave). We hereby express our gratitude.
*This project is an independent implementation; except for the explicitly marked third-party components in the repository, we claim no affiliation, endorsement, or official relationship with that project.*

---

## Usage & Liability

### Icon Usage Instructions (Public Resource)
FlipBits icon resources (including source files and component SVG files) are open to the community as public resources.

Under the premise of complying with this repository's license and applicable laws, you can use these icons for badges, fan creations, video displays, and commercial releases (including sales), without needing to apply for separate authorization from the project.

If used for public dissemination, it is recommended to attribute:
`Icon designed by FlipBits Project`

Limitations and Boundaries (Legally Conservative Version):
- This authorization only covers the icon resources themselves and does not grant trademark rights, patent rights, moral rights, or any rights not explicitly granted.
- Icons are provided "as is", without any express or implied warranties; users shall bear their own compliance and risk responsibilities.
- You may not privatize, claim exclusivity, or sublicense these icon resources as proprietary/exclusive resources.
- You may not imply in any way that you have official representation, endorsement, or a sole authorization relationship with the FlipBits project or authors.

### 1. Principles & Limitations
* **Open and transparent protocols**: The transmission methods used by this tool belong to **open, general acoustic encoding methods**, including high/low frequency switching, dual-tone mapping, and multi-frequency point mapping. Its essence is the orderly encoding of text bytes or symbols, and it does not provide encryption or steganography capabilities to bypass security reviews.
* **Clear usage positioning**: This project is for **audio signal processing (DSP) research, acoustic communication principle verification, and related codec performance experiments**, and is not designed for covert communication scenarios.
* **Entertainment expression priority**: Some modes will deliberately retain lengthy, low-speed, and heavily stylized audio appearances to serve the overall atmosphere expression; efficiency is not the primary goal for all modes.
* **No promise of real-world robustness**: The current focus is the main link closed-loop of "generating audio -> parsing generated audio", without promising stable reception performance under real-world playback, recording, noise, echo, clipping, device frequency response deviation, or long-distance propagation conditions.
* **User responsibility**: The developer provides the source code and implementation methods. When running, modifying, or deploying this project, users should independently ensure that their usage complies with local laws, regulations, platform rules, and network security requirements.
* **Distributed As-Is**: This software is provided as-is. To the extent permitted by applicable law, the author assumes no liability for damages resulting from its suitability, stability, or use.

### 2. Style & IP
* **Independent open-source project**: This project is an independently developed unofficial open-source project. It is not affiliated with any film, television, game, or commercial brand, nor does it represent the stance of any third party.
* **Disclaimer of Affiliation**: This project is an independent original work and has no affiliation, authorization, sponsorship, endorsement, or other connection with Games Workshop and Warhammer 40,000.
* **Style source explanation**: The project references general creative directions such as retro-futurism, industrial aesthetics, and religious ritualistic expressions in its visual and copywriting temperament, but it does not use any proprietary settings from protected worldviews as the foundation of the project.
* **Content processing principles**: If materials, namings, or expressions that may cause confusion, infringement, or inappropriate associations appear in the repository, you are welcome to raise them via [GitHub Issues](../../issues), and we will promptly evaluate and correct them.

---

## Quick Start

> If you find inaccurate, or potentially misleading content in the repository, you are welcome to provide feedback via [GitHub Issues](../../issues).

If you are an AI / agent, it is recommended to first read [`.agent/AGENTS.md`](.agent/AGENTS.md) and the `AGENTS.md` under the corresponding subsystems to quickly understand the repository structure, tool entry points, and modification conventions.

### Android
- The official Android project entry is at `C:\code\FlipBits\apps\audio_android`.
- Execute uniformly from the repository root directory:
  - `python tools/run.py android assemble-debug`
  - `python tools/run.py android assemble-release`
  - `python tools/run.py android native-debug`
- `apps/audio_android` is the Android Gradle root, and `apps/audio_android/app` is the actual application module.
- It is recommended to open `apps/audio_android` directly in Android Studio.

### Local Orchestration Tools
- It is recommended to uniformly use `python tools/run.py <command>`.
- Common commands:
  - `python tools/run.py build --build-dir build/dev`
  - `python tools/run.py clean`
  - `python tools/run.py verify --build-dir build/dev --skip-android`
  - `python tools/run.py android native-debug`
  - `python tools/run.py android assemble-debug`
  - `python tools/run.py artifact export-apk`
- Conventions:
  - `python tools/run.py --help` only views the main command overview; for detailed parameters use `python tools/run.py <command> --help`.
  - The host root directory currently directly fixes one official main line: `clang++ + Ninja + build/dev`.
  - `python tools/run.py verify --build-dir build/dev --skip-android` only verifies the host default modules main path.
  - The Android native side is assembled independently via `apps/audio_android/native_package -> bag_android_native`; the remaining `C++17` exceptions are restricted to the package-private wrapper and `android_bag/**` private declaration layer.
  - `build/` continues to be reserved for native build outputs and test artifacts of CMake / Gradle.
  - The root directory `dist/` only stores the final deliverables exported by Python; currently, the Android APK is exported to `dist/android/` by default.

### Development Navigation
When reading or modifying by modules, you can prioritize entering from the following entries:

- agent / AI main entry: [`.agent/AGENTS.md`](.agent/AGENTS.md)
- Core libraries and shared business logic: [`libs/AGENTS.md`](libs/AGENTS.md)
- CLI presentation layer: [`apps/audio_cli/AGENTS.md`](apps/audio_cli/AGENTS.md)
- Android application: [`apps/audio_android/AGENTS.md`](apps/audio_android/AGENTS.md)

For more repository structure and tool instructions, see:
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)
- [`tools/README.md`](tools/README.md)
