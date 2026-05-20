# Android Changelog

[中文版本 / Chinese Version](C:/code/WaveBits/docs/presentation/android/CHANGELOG.zh-CN.md)

User-facing Android release notes. For detailed engineering history, see `docs/presentation/android/v*/x.y.z.md`.

## v0.4.17

### Added
* Added `untranslated-equals-english` for Latin-script locales such as `de`, `es`, `fr`, `it`, `pl`, and `pt-rBR`, so the translation workflow can find strings that are still fully identical to English and emit JSON-first tasks for `keep_en`, `needs_translation`, and `needs_context`.
* Added `mixed-language-context-audit` for locales such as `zh`, `zh-rTW`, `ja`, `ko`, `ru`, and `uk`, so leftover English can now be grouped by context and missing `CONTEXT` annotations can be reported separately.

### Improved
* Unified Android translation policy into a single shared source for locked English terms, native language names, locale-specific same-form words, and shared prompt terminology, and wired `lint`, `mixed-language`, `audit`, and prompt generation to the same policy.
* Simplified translation entry points so project routing now lives in `apps/audio_android/AGENTS.md`, execution steps live under `.agent/workflows/translations`, and tool definitions live under `tools/repo_tooling/android_translate/docs`.
* Standardized translation task payloads around `JSON first, Markdown optional`, including shared `execution_contract`, `decision_hint`, `ui_surface`, and `length_pressure` fields across the main translation task outputs.

### Fixed
* Fixed multiple batches of untranslated English, stale dual-tone descriptions, and locked-term mismatches across `ja`, `ko`, `zh`, `zh-rTW`, `ru`, `uk`, `de`, `es`, `fr`, `it`, `la`, `pl`, and `pt-rBR`, bringing the main locale audits back to a clean state.
* Fixed missing `CONTEXT` coverage in the English base resources and removed outdated translation workflow/documentation wording that still pointed to older paths and entry points.

## v0.4.16

### Added
* Added full editing for custom Material color presets, including renaming and deleting saved presets.
* Added drag-to-reorder support for custom dual-tone colors.
* Added a `Horizontal` layout option for Mini Morse Visual.
* Added four new dual-tone themes: `Brass Forge`, `Fires of Fate`, `Ecstatic Rapture`, and `Plague Mire`.

### Improved
* Persisted more theme state in `Settings`, including `Material` / `Dual-tone` mode selection, the last-used light and dark Material colors, and section expand/collapse state for built-in and custom palettes.
* Improved custom Material editing so foreground contrast, random color generation, and gear preview colors all stay aligned with the active palette and light/dark mode.
* Added smoother transitions when switching between Material colors, dual-tone themes, and `Material <-> Dual-tone` theme modes.
* Refined Material and dual-tone theme layout in `Settings`, including two-column built-in palettes, clearer family headers, and better compact card labeling.
* Unified `Visual > Hz` outline styling with `Lanes / Pitch / Pulse`, fixed the current-frequency HUD width to the widest expected Hz value, and kept the `Hz` unit pinned on the right to reduce flicker during readout changes.
* Changed `Visual > Hz` `low/high` labels to use contrast against the mini player background instead of directly following theme accent slots.

### Fixed
* Fixed custom Material imports so duplicate presets now offer `Overwrite` or `Add copy` instead of being silently skipped.
* Fixed cases where Material custom colors could lose context after switching between light and dark theme modes.
* Fixed a brief black flash during language switching.
* Fixed a cold-start theme flash where the app could briefly show the default dual-tone theme before restoring the last-used theme.
* Fixed `Visual > Hz` jumpiness during rapid play/pause debugging by tightening the playback end-action and visual trace reset path.

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
