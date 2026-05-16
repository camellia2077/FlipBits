# Android Changelog

面向用户的 Android 版本更新摘要。详细工程历史请继续参考 `docs/presentation/android/v*/x.y.z.md`。

## v0.4.14

### Added
* Added custom Material color themes.
* Added a persistent `Sample text` settings section, including `Auto-fill sample text` and sample decoration controls.

### Improved
* Persisted more UI state in `Settings`, including theme section expand/collapse state and dual-tone theme section state.
* Changed Mini speed selection from segmented buttons to a Flash-style dropdown selector named `Speed style`. The selected style is now remembered, with `standard` used only as the first-time default.
* Updated generated audio filenames to `mode[style]_content`.
* Flash and Mini now include `[style]` in filenames:
  Mini: `slow`, `standard`, `fast`
  Flash: `standard`, `hostility`, `litany`, `collapse`, `zeal`, `void`
* Removed generated timestamps from filenames. Creation time is now read from audio metadata when available.
* Reused dual-tone fixed colors for copy icons and related text styling, and extended the same color configuration flow.
* Updated dual-tone visual styling so token dividers and Visual lane lines use `outline` color semantics consistently.
* Refined Mini Morse presentation with fixed dot/dash graphic units and width-aware token packing.
* Improved Morse visual playback smoothness.
* Tapping a token in Flash lyrics now expands the panel and relocates to the current token more reliably.

### Fixed
* Prevented audio generation when the input is truly empty. The app now prompts the user to enter text or use randomized sample text.
* Fixed Mini Morse input cases where whitespace-only or special whitespace characters such as spaces and line breaks could fail to generate audio.
* Fixed cases where Mini generation could briefly flash and disappear when whitespace handling was inconsistent.
* Turning off `Auto-fill sample text` now clears current sample-backed input immediately, while leaving user-edited custom text untouched.
