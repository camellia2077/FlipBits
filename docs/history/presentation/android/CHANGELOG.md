# Android Changelog

[中文版本 / Chinese Version](CHANGELOG.zh-CN.md)

User-facing Android release notes. For detailed engineering history, see `docs/history/presentation/android/v*/x.y.z.md`.

## v0.5.6

This update focuses on playback state persistence, clearer theme presentation, and more consistent localization, along with stability fixes for speed-adjusted playback and Ultra visualization.

### Added
* Transport mode selection is now persisted, so reopening the app or returning to playback restores the last mode you used instead of resetting to the default every time.
* Theme-related settings have been consolidated under faction theme naming, with a cleaner flow across custom themes, theme mapping, and import/export behavior.
* Localization and audio sample translations on Android have been refined, with more consistent wording across Settings, Saved, and faction theme related text.

### Improved
* The Audio page now uses a more consistent structure for input, result, and grouped sections, with improved fallback contrast for faction theme group backgrounds when nearby colors are too close.

### Fixed
* Fixed PCM handling and edge-transition issues in parts of the speed-adjusted playback path, especially in Flash and Mini CW modes, reducing playback risks when switching speeds or handling longer audio.
* Fixed several edge cases in the Ultra visualization timeline and related playback analysis flow, improving sync stability between full-frame visuals and playback state.

## v0.5.4

### Added
* Added richer custom theme sharing for both `Material` and `dual-tone`, including cleaner plain-text copy/export format and support for importing multiple shared themes while preserving their original order.
* Added the new `The Sacred Machine` dual-tone theme `Xeno Code`.
* Added drag-to-select lyric seeking in mini player detail view, with a target timestamp line that lets you scrub directly to a lyric row.

### Improved
* Improved custom theme management so newly created or imported `Material` and `dual-tone` themes now appear at the top of the list for faster access.
* Improved built-in dual-tone theme copying so exported names now follow the current app language instead of falling back to internal or English-only identifiers.
* Refined dual-tone theme editing and section actions, including clearer outline-color usage in custom editing, a copy action that only appears when a built-in section is expanded, and stronger title readability for built-in theme groups and preview cards.
* Refined expanded player and full lyrics interaction: detail view now follows playback by default, full lyrics keeps its own scrolling controls, and lyric drag selection uses a steadier visual focus line.
* Refined dual-tone helper text color strategy so secondary labels and descriptions now prefer `outline` styling but still fall back to high-contrast text when a custom palette would otherwise make them hard to read.
* Refined custom theme deletion dialogs so `Edit custom dual-tone` and `Edit colors` now reuse the current theme's main background color instead of falling back to a generic dialog surface.
* Refined custom theme deletion dialogs further so they keep using the current preset's primary color even if the edited `primary` field is temporarily invalid.

### Fixed
* Fixed batch custom theme imports reversing the order of shared themes after insertion.
* Fixed `Sample text` length restoration so `short` / `long` now stays consistent when switching dual-tone themes and after a cold start.
* Fixed the built-in dual-tone theme previously labeled `Scarlet Guard`; it is now renamed `Hazard Warning` with updated color description text that matches the actual palette.
* Fixed a release-only startup crash that could close the app immediately after launch on some builds.

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
