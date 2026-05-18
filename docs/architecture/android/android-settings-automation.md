# Android Settings Automation

## Purpose

This document records the current automation surface that touches Settings-owned state. It exists so later agents do not have to infer which settings can already be driven through adb debug scenarios and which ones still require manual UI changes.

For the full Android automation matrix across JVM tests, instrumentation, and adb debug scenarios, see `docs/architecture/android/android-automation-coverage.md`.

## Current Coverage

Covered today:

- App language override through adb debug scenarios:
  - Flash playback scenario
  - Mini playback scenario
  - Encode-progress scenario
- Direct tab navigation through adb debug scenario:
  - `Audio`
  - `Saved`
  - `Settings`
- Material custom copy/import duplicate-check through adb debug scenario:
  - copy current material custom preset as export text
  - parse it back through material import
  - log whether duplicate confirm should be shown
  - optionally apply `overwrite` or `copy`
- Measurement-baseline reset for lyrics/layout captures:
  - `Demo mode = off`
  - `Visual perf overlay = off`
- Built-in sample resolution after language override:
  - `wb.sample.length`
  - `wb.sample.id`

Not covered yet:

- Palette/theme switching
- Theme style / brand theme switching
- Sample decoration toggle
- Manual expansion state of Settings cards
- Any Settings-only screenshot or pixel assertions

## Scenario Contract

Current settings-aware adb extras:

- `wb.lang`
  - Optional app-language override.
  - Accepted values follow app locale tags such as `zh`, `en`, `ru`.
  - Applied before built-in sample resolution and before player-detail capture.
  - Available in:
    - `com.bag.audioandroid.DEBUG_FLASH_SCENARIO`
    - `com.bag.audioandroid.DEBUG_MINI_SCENARIO`
    - `com.bag.audioandroid.DEBUG_ENCODE_PROGRESS_SCENARIO`
    - `com.bag.audioandroid.DEBUG_APP_TAB_SCENARIO`
- `wb.tab`
  - Optional app tab target for navigation-only automation.
  - Accepted values: `audio`, `saved`, `settings`.
  - Available in:
    - `com.bag.audioandroid.DEBUG_APP_TAB_SCENARIO`
- `wb.import.confirm`
  - Optional confirm action for the Settings material import automation.
  - Accepted values: `none`, `overwrite`, `copy`.
  - Available in:
    - `com.bag.audioandroid.DEBUG_SETTINGS_IMPORT_SCENARIO`
- `wb.import.scope`
  - Optional export/import scope for the Settings material import automation.
  - Accepted values: `current`, `all`.
  - `current` copies the active material custom preset and re-imports that single block.
  - `all` copies all material custom presets and re-imports the whole batch.
  - Available in:
    - `com.bag.audioandroid.DEBUG_SETTINGS_IMPORT_SCENARIO`
    - `com.bag.audioandroid.DEBUG_APP_TAB_SCENARIO`

Wrapper note:

- `capture-flash` now exposes `--lang`
- `capture-mini` now exposes `--lang`
- `capture-encode-progress` now exposes `--lang`
- `capture-tab` exposes `--tab` and optional `--lang`
- `capture-settings-import` exposes optional `--lang`, `--confirm`, and `--scope`

So common language-sensitive captures no longer require a raw `adb shell am start ... --es wb.lang <tag>` path unless you need an extra that still sits below the wrapper surface.

Navigation scenario commands:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_APP_TAB_SCENARIO --es wb.tab settings
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_APP_TAB_SCENARIO --es wb.tab saved --es wb.lang ja
python tools/run.py android-debug capture-tab --tab audio
python tools/run.py android-debug capture-tab --tab settings --lang en
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_SETTINGS_IMPORT_SCENARIO --es wb.import.confirm overwrite
python tools/run.py android-debug capture-settings-import --confirm none --scope current --lang en
python tools/run.py android-debug capture-settings-import --confirm copy --scope all
```

Settings baseline behavior used by lyrics/layout measurement:

- Flash and Mini UI scenarios automatically force:
  - `Demo mode = off`
  - `Visual perf overlay = off`

This reset is intentional. Demo mode only adds touch-feedback animation, but measurement captures still disable it so device-local debug toggles cannot contaminate layout numbers.

## Shared Flow

For settings-aware debug scenarios, the common order is:

- Optionally apply `wb.lang`.
- Optionally switch `wb.tab`.
- Optionally reset measurement-related debug toggles.
- Resolve built-in sample text under the requested language.
- Continue with the transport-specific scenario flow.

This means agents do not need to navigate the Settings tab before running a language-sensitive measurement capture.

## Observability

Current logs:

- `FlashAutomation languageApplied ... language=<tag>`
- `MiniAutomation languageApplied ... language=<tag>`
- `EncodeProgressAutomation languageApplied ... language=<tag>`
- `TabAutomation received ... tab=<audio|saved|settings>`
- `TabAutomation tabSelected ... requested=<...> selected=<...>`
- `SettingsImportAutomation received ... confirm=<none|overwrite|copy>`
- `SettingsImportAutomation copyResolved ... scope=<current|all> presetCount=<n>`
- `SettingsImportAutomation importParsed ... scope=<current|all>`
- `SettingsImportAutomation batchImportPreview ... duplicateCount=<n> newCount=<n>`
- `SettingsImportAutomation duplicatePrompt ... shown=<true|false>`
- `SettingsImportAutomation importApplied ... action=<overwrite|copy> scope=<current|all> blocks=<n>`
- `FlashAutomation measurementBaselineReset ... demoMode=false visualPerfOverlay=false`
- `MiniAutomation measurementBaselineReset ... demoMode=false visualPerfOverlay=false`

These logs confirm the scenario actively changed Settings-owned state instead of inheriting whatever the device was using from an earlier manual session.

## Success Criteria

- The target scenario logs `languageApplied` when `wb.lang` is present.
- The tab-navigation scenario logs `tabSelected` and the selected tab matches the request.
- The Settings import scenario logs `duplicatePrompt` and `shown=true` when the copied preset is re-imported unchanged.
- The Settings import scenario logs `batchImportPreview` when the copied text contains multiple material custom presets.
- When `wb.import.confirm` is `overwrite` or `copy`, the scenario logs `importApplied`.
- Built-in sample selection logs still resolve a valid sample after the language switch.
- Flash/Mini lyrics-size captures log `measurementBaselineReset` before encode/playback begins.
- Layout or progress diagnostics collected after that point can be treated as coming from a known Settings baseline.

## Maintenance Rules

- Keep settings-aware automation behind existing debug scenarios; do not introduce coordinate taps into the Settings tab for state that can be expressed as intent extras.
- If a new measurement-sensitive setting is added, decide whether it should also be forced into the baseline-reset step and update this document in the same change.
- If a setting becomes part of a scenario contract, document both the extra name and the confirmation log tag here.
