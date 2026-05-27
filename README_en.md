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
  <strong>Turn text encoding into sound, and turn the bits, bytes, tokens, and rhythm behind that sound into something you can watch.</strong><br />
  <em>FlipBits combines encoding conversion, audio visualization, and encoding visualization in one project. It supports Morse, bit-by-bit BFSK / FSK, dual-tone mapping, and multi-frequency mapping; its `flash` mode further stylizes transmission through bit duration, pauses, and Hz variation to imitate different speaking tones, making the result feel part protocol, part human speech under shifting emotions.</em>
</p>

[![License](https://img.shields.io/badge/License-Apache%202.0-red.svg)](LICENSE)
[![Platform Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg)]()
[![CI Android Assemble](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-assemble.yml/badge.svg)](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-assemble.yml)
[![CI Android Quality](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-quality.yml/badge.svg)](https://github.com/camellia2077/FlipBits/actions/workflows/ci-android-quality.yml)
[![CI Host Verify](https://github.com/camellia2077/FlipBits/actions/workflows/ci-host-verify.yml/badge.svg)](https://github.com/camellia2077/FlipBits/actions/workflows/ci-host-verify.yml)


<table>
  <tr>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/3c1a3eaf-17e5-4b19-b4d3-27c642aee92d" alt="Main UI"><br>
      <sub>Main UI</sub>
    </td>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/66efd1fb-61a6-485a-bf69-611646cef57b" alt="Lyrics-style Token Sync"><br>
      <sub>Lyrics-style Token Sync</sub>
    </td>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/2b5e3d1c-c61c-4967-926c-ff93d63c1160" alt="Adaptive Layout"><br>
      <sub>Adaptive Layout</sub>
    </td>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/bb82b369-ef65-4c55-83fc-7ef515a0007f" alt="Audio Visualizer"><br>
      <sub>Audio Visualizer</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/0ec94cc2-8a15-4624-8e23-76900e7495e6" alt="Custom Dual-Tone Themes"><br>
      <sub>Custom Themes</sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/abae9141-ef48-4ccf-97c0-2c3eb3e2f07c" alt="Material Design Themes"><br>
      <sub>Material Design Themes</sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/911bd395-81ac-4a2b-ade8-6806e8cec6c2" alt="Morse Code"><br>
      <sub>Morse Code</sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/d55089ff-f4cf-4dfb-85e1-488ce2b62658" alt="16-FSK"><br>
      <sub>16-FSK</sub>
    </td>
  </tr>
</table>

## Quick Overview
- A native Android app that turns computer text encoding into audible audio while visualizing both the encoding layer and the signal layer in sync.
- Supports Morse code (`mini`), bit-by-bit BFSK / FSK (`flash`), DTMF-like dual-tone mapping (`pro`), and `16-FSK` (`ultra`).
- Visualization provides two modes, Visual and Lyrics, for observing the audio signal layer, the text encoding layer, and the playback follow relationship across tokens, bytes, and bits.
- Helps reveal how CJK, Latin, Cyrillic, and other writing systems occupy different numbers of bytes under UTF-8, and how those bytes expand into hex, binary, and bits.
- `mini` mode is useful not only for Morse generation and decoding, but also for learning Morse timing and practicing text-to-Morse / Morse-to-text conversion.
- Supports multilingual interfaces: English, German, Spanish, French, Italian, Japanese, Korean, Polish, Brazilian Portuguese, Russian, Ukrainian, Simplified Chinese, Traditional Chinese, as well as dog Latin to create solemn, religious, sci-fi, and space opera atmospheres.
- Localization work is continuously being optimized. If you find translation errors or have better suggestions, corrections are warmly welcomed.

## Download / Install
The Android APK will be published via GitHub Releases.

Under the current reference build, the installation package is approximately `6.32 MB`(decimal-based, as reported by Android). These numbers will vary depending on the version, ABI, and build configurations.

## Live Demo

If you want to try FlipBits without installing the APK, you can use the GitHub Pages demo to generate, listen to, parse, and download audio directly in the browser:

- [FlipBits Pages Live Demo](https://camellia2077.github.io/FlipBits/)

The Pages demo lets you enter text, generate audio in different modes, play it online, parse in-project generated results, and download the resulting audio files. The project still places its main expressive focus on bit-by-bit BFSK / FSK (`flash`).

For user-facing Android release notes, see:
- [`docs/history/presentation/android/CHANGELOG.md`](docs/history/presentation/android/CHANGELOG.md)

## Modes Overview
| Mode | Technology Category | Suitable Uses |
| --- | --- | --- |
| `mini` | Morse code | Short, clear, rhythmically readable dot-dash signals |
| `flash` | Bit-by-bit BFSK / FSK | Stronger emotional listening experience, Visual/Lyrics comparison learning |
| `pro` | DTMF-like dual-tone mapping | More compact dual-tone structure |
| `ultra` | `16-FSK` frequency mapping | Shorter audio, faster generation and parsing |

These names are not "power levels", but rather productized naming within the project. They emphasize different listening experiences, expressive temperaments, and transmission structures, rather than representing a linear upgrade from a basic to an advanced version of the same protocol.

## Project Positioning
FlipBits is a project that ties text encoding, audible audio signals, and visualization into one experience. It does not just convert text into sound; it also tries to expose what text looks like inside the machine, including how tokens, characters, bytes, hex, binary, and bits enter a playback timeline.

The project can map text into waveforms and restore text from waveforms generated within the project. It does not provide cryptographic encryption, and covert communication is not its design goal.

* **Encoding literacy**: The project can help explain how CJK, Latin, Cyrillic, and other writing systems occupy different byte counts under UTF-8, and how those bytes expand into hex, binary, and bits. For anyone trying to understand what text looks like after it enters the machine, this is often easier to grasp than reading encoding tables alone.
* **Morse learning**: `mini` mode is not only a Morse transport; it also works as a text/Morse conversion aid, a timing trainer, and a visual companion for manual recognition.
* **Expressive focus**: Bit-by-bit BFSK / FSK (`flash`) deliberately sacrifices encoding efficiency, trading longer bits, pauses, and frequency changes for a stronger emotional listening experience and a more human-like speaking tone. It behaves less like a purely efficient protocol and more like signal structure performing emotion.
* **Efficiency supplement**: If shorter, faster, and more formal transmission is needed, Morse code (`mini`), DTMF-like dual-tone mapping (`pro`), and `16-FSK` (`ultra`) provide more compact encoding paths. `16-FSK` (`ultra`) not only generates shorter audio, but also usually reduces generation and parsing cost compared with bit-by-bit BFSK / FSK (`flash`); however, being faster is not the project's only goal.
* **Visualization value**: The Android app provides two complementary tracking views. Visual leans toward the signal layer, showing how text encoding turns into FSK low/high bits, frequency segments, and the playback timeline; Lyrics leans toward the text encoding layer, using tokens to show how text is encoded into UTF-8 bytes, hex/bin, and bits, highlighting them as the audio plays.

## Android App Features
The Android app currently maintains a lightweight, native approach: fast cold start speed and small package size, making it suitable for directly generating, converting, sharing, and exporting audio.

It is not just a shell for generating WAV files. It also carries much of the project's visual character:

* **Dual follow views**: Visual is for watching signal, frequency, rhythm, and timeline motion; Lyrics is for watching how tokens, UTF-8 bytes, hex / binary / bits progress during playback.
* **Theme expression**: Alongside `Material`, the app includes more atmospheric dual-tone themes designed for spectacle and mood, so different modes and styles can carry a stronger visual identity.
* **Learning and presentation together**: The app is meant to help people understand encoding structure while also looking and sounding compelling, so UI, themes, animation, and visualization are part of the product rather than decoration.

## Design Boundaries
The current focus of this project is a controlled closed loop of "text -> stylized audio -> in-project decoding", with a particular emphasis on the audio generation, conversion, sharing, and export experience within the Android app.

It does not take "direct real-time parsing by another device after external playback" as its main interactive goal, nor does it prioritize anti-noise, anti-echo, far-field reception, or complex synchronization robustness in real-world environments as design priorities. For this project, atmosphere, recognizable style expression, and a controlled mode experience take precedence over communication robustness in real-world acoustic environments.

## Mode Instructions

### Bit-by-bit BFSK / FSK (`flash`)

Bit-by-bit BFSK / FSK (`flash`) is the most highly stylized mode. Its listening experience comes from the acoustic expression of binary encoding: it uses high/low Hz as bit states, with each bit only switching between low and high frequency states, corresponding to 0 / 1 in binary. Then, by adjusting bit duration, frequency configuration, and pause intervals, it simulates listening experiences closer to 6 common human speaking emotions.

In the Litany style, low speed is an intentional feature. The same text might generate nearly a minute of audio under `flash`, while it takes only a few seconds under `16-FSK` (`ultra`). The longer bit lengths and intervals allow users to easily manually transcribe binary from the audio. The **220 / 440 Hz** (standard A3/A4 pitch) configuration makes it easy for humans to sing along with the digital signals.

### Interface Preview and Style Definition


<table>
  <tr>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/9871071f-b035-4547-965d-4d81ce701d6d" alt="Tokens Mode"><br>
      <sub>Tokens Mode</sub>
    </td>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/29c93da9-fe02-465e-8fe8-6dc0acc783c9" alt="Visual Mode"><br>
      <sub>Visual Mode</sub>
    </td>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/49d78c70-b438-430a-adcc-30e8b3437df0" alt="Mix Mode"><br>
      <sub>Mix Mode</sub>
    </td>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/13276e10-4351-4e88-9273-2358f1c15fd2" alt="Select Single Token"><br>
      <sub>Select Single Token</sub>
    </td>
  </tr>
</table>


Currently, six styles are provided. Through combinations of bit duration, frequency organization, and pause intervals, they shape different emotional "speaking tones":

| Style | Low / High Hz | Listening Goal |
| :--- | :--- | :--- |
| [Litany](docs/design/modes/flash/litany.md) | `220 / 440` | Deep, solemn, chanting |
| [Collapse](docs/design/modes/flash/collapse.md) | `226-320 / 452-640` | Whispering, panicked, stuttering |
| [Standard](docs/design/modes/flash/standard.md) | `300 / 600` | Daily, precise, steady |
| [Hostility](docs/design/modes/flash/hostility.md) | `438-536 / 876-1072` | Sharp, rapid, aggressive |
| [Zeal](docs/design/modes/flash/zeal.md) | `560-900 / 1120-1800` | Bright, variable speed, dense |
| [Void](docs/design/modes/flash/void.md) | `240 / 480` | Deep, trailing, sparse |



### Design Details

For more details on the emotional positioning, naming semantics, and preset designs of the `flash` voicing style, see:
- [`docs/design/modes/flash/README.md`](docs/design/modes/flash/README.md)
- [`docs/design/modes/flash/voicing-emotions.md`](docs/design/modes/flash/voicing-emotions.md)
- [`docs/design/modes/flash/`](docs/design/modes/flash/)


### Morse code (`mini`)

Morse code (`mini`) standardizes input according to Morse rules, emphasizing clear visual effects and dot-dash rhythms. The core of its design lies in the **"visibility of rhythm"**: through real-time UI feedback, abstract codes are transformed into intuitive visual progress.

#### Speed Presets and Visual Follow
Three Speed Presets are currently provided, aiming to balance "feasibility of manual recognition" and "transmission efficiency":


<table>
  <tr>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/8980ad13-7fdb-4eed-8e20-284f40208f7b" alt="Main UI"><br>
      <sub>Main UI</sub>
    </td>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/f4183307-511b-49a4-9e24-473f3f6171aa" alt="Visual Mode"><br>
      <sub>Visual Mode</sub>
    </td>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/f2f82628-9511-4f1c-91f2-009c1ad5556c" alt="Tokens Mode"><br>
      <sub>Tokens Mode</sub>
    </td>
    <td width="25%" align="center">
      <img src="https://github.com/user-attachments/assets/4d653c99-12f8-4959-a8a2-f18b1bff673a" alt="Token Selection"><br>
      <sub>Token Selection</sub>
    </td>
  </tr>
</table>

| Speed | Positioning | Design Goal |
| :--- | :--- | :--- |
| **Slow** | Extremely slow | Teaching-level speed, best for observing dot / dash and word-by-word comparison (Lyrics Follow) |
| **Standard** | Standard rhythm | Simulates the classic rhythmic feel and recognizability of traditional Morse code |
| **Fast** | Compact | Compresses dot-dash intervals, providing a more efficient, shorter audio output |

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
- The official Android project entry is at `apps/audio_android`.
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
