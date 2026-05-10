# Android Flash Automation

## Purpose

This document explains the current Flash device automation surface for real-device UI regression checks and agent/adb-driven debugging. It is intentionally separate from the Flash Visual rendering architecture so agents can quickly find the automation contract and its current coverage.

## Current Coverage

The automation path covers generated Flash playback only. It does not depend on sample catalog entries.

Covered:

- Fast-regression input text: `flash sync test`
- Scenario kinds:
  - `ui`: real foreground UI regression through player detail + Compose visual rendering.
  - `headless`: generated-audio diagnostics without opening player detail or depending on the visual Canvas.
- Flash voicing styles: `steady`, `hostile`, `litany`, `collapse`, `zeal`, `void`
- Flash visual modes accepted by the adb scenario: `lanes`, `energy`, `pitch`
- UI scenario actions driven through the normal ViewModel path:
  - select Flash mode
  - select the requested Flash voicing style
  - replace input text
  - encode generated Flash audio
  - open the player detail sheet
  - start playback
  - stop playback after the configured short capture window
- Debug log confirmation through `FlashAutomation`
- Visual playback diagnostics through `FlashVisualPerf`
- Unified Visual/readout/Lyrics timing diagnostics through `FlashAlignmentPerf`
- Headless diagnostics through `FlashHeadless`
- Lyrics/token alignment diagnostics through `FlashLyricsPerf`

Not covered yet:

- Saved-audio library playback
- Import/export flows
- Decode validation after playback
- Visual pixel assertions or screenshot comparison
- Running every visual mode for every style in the instrumentation test matrix
- Release/staging build automation entry points

Longer text is still supported through `wb.input`, but it is not the default for full style sweeps. For pangram or UTF-8/readout investigations, pass the text explicitly and usually run one style at a time.

## ADB Scenario

Debug builds support a dedicated action:

```text
com.bag.audioandroid.DEBUG_FLASH_SCENARIO
```

Fresh install on a dedicated test device:

```powershell
python tools/run.py android install-debug-fresh --clean
```

This command builds the standard debug artifact, verifies that `apps/audio_android/app/build/outputs/apk/debug/app-debug.apk` is present and not suspiciously small, runs `adb uninstall com.bag.audioandroid`, then installs the debug APK. Debug automation should use the standard `app-debug.apk`; `FlipBits-*` APK copies are kept for staging/release artifacts only.

UI scenario command:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_FLASH_SCENARIO --es wb.scenario ui --es wb.flash.style litany --es wb.visual lanes --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
```

Headless scenario command:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_FLASH_SCENARIO --es wb.scenario headless --es wb.flash.style litany --es wb.visual lanes --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
```

Supported extras:

- `wb.scenario`
  - `ui` by default.
  - `headless` skips player-detail opening and logs generated playback/follow timing through `FlashHeadless`.
- `wb.input`
  - Optional text override.
  - Defaults to `flash sync test`.
- `wb.flash.style`
  - `steady`, `hostile`, `litany`, `collapse`, `zeal`, `void`
- `wb.visual`
  - `lanes`, `energy`, `pitch`
- `wb.encode`
  - `true` by default.
- `wb.play`
  - `true` by default.
- `wb.play.ms`
  - Playback duration before the debug scenario stops playback.
  - Defaults to `6000`.
  - Use `0` to leave playback running.

Recommended device prep:

```powershell
adb shell svc power stayon usb
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
```

USB debugging alone is not enough for visual/perf validation. The device must be unlocked and able to draw the foreground app; otherwise the scenario can receive the intent and encode audio without producing visual draw logs.

Recommended UI log capture in a second shell:

```powershell
adb logcat -c
adb logcat -v time FlashAutomation:D FlashAlignmentPerf:D FlashVisualPerf:D FlashLyricsPerf:D *:S > temp\log.txt
```

`FlashAutomation` confirms the scenario was received. `FlashVisualPerf` provides the playback and visual timing metrics used for diagnosis.
`FlashLyricsPerf` records the real Lyrics token-tape state derived from `textTokenTimeline` and the current displayed sample.
`FlashAlignmentPerf` is the preferred sync diagnostic: it samples Visual, 8-bit readout, and Lyrics token state through one debug-only throttled trace, so agents do not need to pair separate log rows manually.

For quick regression, do not wait for complete playback. Stop after the first few `FlashVisualPerf` rows show `playing=true`, `bitReadout=true`, and `fallback=false`.

Lyrics/token success criteria:

- `FlashLyricsPerf` rows show `playing=true`.
- `sample` advances with playback.
- `token` and `tokenText` change as `sample` crosses `tokenStart` / `tokenEnd`.
- `tokenProgress` stays in `0.00..1.00` while a token is active.
- `byte` / `bit` move forward inside the active token for Flash payloads.
- Prefer `FlashAlignmentPerf visualSample/lyricsSample/sampleDelta/readoutBit/visualBit/lyricBit/bitDelta` when checking visual, 8-bit row, and Lyrics alignment.
- Compare with `FlashVisualPerf readoutSample/readoutBit/visualBit` only when investigating rendering performance or when `FlashAlignmentPerf` is missing.

Filtered visual + Lyrics summary:

```powershell
adb logcat -d -v time FlashAutomation:D FlashAlignmentPerf:D FlashVisualPerf:D FlashLyricsPerf:D *:S > temp\flash_alignment_raw.log
python tools/scripts/android/flash/filter_flash_alignment_log.py temp\flash_alignment_raw.log --output temp\flash_alignment_summary.md
```

Agents should read the generated summary first. It keeps the useful `FlashAutomation`, `FlashAlignmentPerf`, `FlashVisualPerf`, and `FlashLyricsPerf` rows. When `FlashAlignmentPerf` is present, the summary uses those unified samples first; otherwise it falls back to pairing each Lyrics sample with the nearest visual readout sample.

Recommended headless log capture:

```powershell
adb logcat -c
adb logcat -v time FlashAutomation:D FlashHeadless:D *:S > temp\log.txt
```

Headless success criteria:

- `FlashAutomation` shows `scenario=headless`.
- `FlashHeadless start` shows `follow=true` and `binaryGroups > 0`.
- `FlashHeadless tick` shows `playing=true`, advancing `sample`, and non-negative `bit` / `revealed` values after playback enters the bit timeline.
- `FlashHeadless stop` appears after the configured `wb.play.ms`.

Headless does not validate Compose drawing, edge fades, visual bar spacing, clipping, or layout. Use `ui` for those.

All-style fast sweep example:

```powershell
$styles = @("steady", "hostile", "litany", "collapse", "zeal", "void")
foreach ($style in $styles) {
    adb logcat -c
    adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_FLASH_SCENARIO --es wb.scenario ui --es wb.flash.style $style --es wb.visual lanes --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
    Start-Sleep -Seconds 12
    adb logcat -d -v time FlashAutomation:D FlashAlignmentPerf:D FlashVisualPerf:D FlashLyricsPerf:D *:S
}
```

Pangram single-style example:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_FLASH_SCENARIO --es wb.input "The quick brown fox jumps over the lazy dog." --es wb.flash.style litany --es wb.visual lanes --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
```

## Instrumentation Test

The real-device regression entry is:

```text
apps/audio_android/app/src/androidTest/java/com/bag/audioandroid/ui/FlashDebugScenarioInstrumentedTest.kt
```

It launches `MainActivity` with the debug Flash scenario and parameterizes across all Flash voicing styles. The test waits for:

- `player-detail-sheet-content`
- `playback-display-section`
- `flash-visualization-mode-switcher`
- `flash-visualization-mode-tonetracks`

Compile validation:

```powershell
cd apps\audio_android
.\gradlew.bat :app:compileDebugAndroidTestKotlin
```

Run on a connected device:

```powershell
cd apps\audio_android
.\gradlew.bat :app:connectedDebugAndroidTest --tests com.bag.audioandroid.ui.FlashDebugScenarioInstrumentedTest
```

## Stable UI Tags

Current stable tags added for Flash automation:

- `audio-mode-flash`
- `audio-input-text-field`
- `audio-encode-button`
- `flash-voicing-style-selector`
- `flash-voicing-style-<styleId>`
- `audio-playback-toggle`
- `player-detail-sheet-content`
- `playback-display-section`
- `flash-visualization-mode-switcher`
- `flash-visualization-mode-tonetracks`
- `flash-visualization-mode-toneenergy`
- `flash-visualization-mode-pitchladder`

Prefer these tags in instrumentation tests. For agent/adb debugging, prefer the debug scenario action instead of coordinate taps or accessibility-service automation.

## Maintenance Rules

- Keep the fast-regression default text short unless the test target changes intentionally.
- Add new Flash styles to both `FlashVoicingStyleOption` and the automation coverage notes.
- If the visual mode names change, update the accepted `wb.visual` aliases and stable tags together.
- Debug scenario behavior must stay behind `BuildConfig.DEBUG`.
- When a visual bug is timing-related, capture `FlashAutomation`, `FlashAlignmentPerf`, `FlashVisualPerf`, and `FlashLyricsPerf` together before widening the fix.
