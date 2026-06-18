<p align="center">
  <img src="ui/app/icon-foreground.svg" alt="FlipBits icon" width="128" />
</p>

<p align="center">
  <em>Icon designed for <a href="https://github.com/camellia2077/FlipBits">camellia2077/FlipBits</a></em>
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
- Android voice now also works like a voice changer: `single track` processes the input audio directly, while `dual track` splits the input path and the secondary path so the secondary track can reuse `flash` voicing styles and create a more mechanical layered feel.
- Visualization provides two modes, Visual and Lyrics, for observing the audio signal layer, the text encoding layer, and the playback follow relationship across tokens, bytes, and bits.
- Helps reveal how CJK, Latin, Cyrillic, and other writing systems occupy different numbers of bytes under UTF-8, and how those bytes expand into hex, binary, and bits.
- `mini` mode is useful not only for Morse generation and decoding, but also for learning Morse timing and practicing text-to-Morse / Morse-to-text conversion.
- Supports multilingual interfaces: English, German, Spanish, French, Italian, Japanese, Korean, Polish, Brazilian Portuguese, Russian, Ukrainian, Simplified Chinese, Traditional Chinese, as well as dog Latin to create solemn, religious, sci-fi, and space opera atmospheres.
- Localization work is continuously being optimized. If you find translation errors or have better suggestions, corrections are warmly welcomed.

## Download / Install
The Android APK will be published via GitHub Releases.

Under the current reference build, the installation package is approximately `6.71 MB`(decimal-based, as reported by Android). These numbers will vary depending on the version, ABI, and build configurations.

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
* **Theme expression**: Alongside `Material`, the app includes more atmospheric tri-color themes (primary, secondary, and outline colors) designed for spectacle and mood, so different modes and styles can carry a stronger visual identity.
* **Learning and presentation together**: The app is meant to help people understand encoding structure while also looking and sounding compelling, so UI, themes, animation, and visualization are part of the product rather than decoration.
* **Voice mode**: Android voice now includes voice-changer-like audio processing, split into `single track` and `dual track`. `single track` applies directly to the input audio and works well for one-path stylization; `dual track` splits the main input path and the secondary path, reuses `flash` voicing styles for the secondary track, and blends them back together for a more layered mechanical sound.

## Design Boundaries
The current focus of this project is a controlled closed loop of "text -> stylized audio -> in-project decoding", with a particular emphasis on the audio generation, conversion, sharing, and export experience within the Android app.

It does not take "direct real-time parsing by another device after external playback" as its main interactive goal, nor does it prioritize anti-noise, anti-echo, far-field reception, or complex synchronization robustness in real-world environments as design priorities. For this project, atmosphere, recognizable style expression, and a controlled mode experience take precedence over communication robustness in real-world acoustic environments.

## Further Reading

This README only keeps the information needed for a public project homepage. Deeper mode design, parameters, implementation boundaries, and architecture notes live in:

- [`docs/design/modes/README.md`](docs/design/modes/README.md)
- [`docs/design/modes/flash/README.md`](docs/design/modes/flash/README.md)
- [`docs/design/modes/voice/README.md`](docs/design/modes/voice/README.md)
- [`docs/architecture/repo-map.md`](docs/architecture/repo-map.md)
## Usage & Liability

- FlipBits uses open, general acoustic encoding and audio-processing methods. It does not provide encryption, steganography, or tools for bypassing security review.
- The project focuses on audio signal processing, encoding visualization, stylized sound expression, and controlled in-project generation/parsing. It does not promise robust communication under real playback, far-field recording, noise, echo, clipping, or device-response differences.
- FlipBits is an independent open-source project and is not affiliated with any film, television, game, or commercial brand. Its visual and copywriting direction references broad creative ideas such as retro-futurism, industrial aesthetics, and ritualized expression.
- Software and icon resources are provided under the repository license and as-is. Users are responsible for deployment, publishing, derivative works, commercial use, and legal compliance.

For the fuller distribution notice, trademark boundaries, and icon usage notes, see [NOTICE](NOTICE) and [docs/legal/TRADEMARKS.md](docs/legal/TRADEMARKS.md).
## Quick Start

- Android project entry: [`apps/audio_android`](apps/audio_android)
- Live demo: [`FlipBits Pages`](https://camellia2077.github.io/FlipBits/)
- APK: published through GitHub Releases

If you are a developer or AI / agent, start from [`.agent/AGENTS.md`](.agent/AGENTS.md). Android, CLI, and libs build/test/editing conventions now live in their subsystem `AGENTS.md` files instead of being expanded in this public README.
