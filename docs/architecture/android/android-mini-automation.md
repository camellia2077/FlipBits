# Android Mini Automation

## Purpose

This document describes the debug-only Mini scenario used for real-device adb capture. The current goal is observability only: gather precise Visual/Lyrics timing data for Mini slow, standard, and fast generated playback before changing sync behavior.

For the full Android automation matrix across JVM tests, instrumentation, and adb debug scenarios, see `docs/architecture/android/android-automation-coverage.md`.

## Current Coverage

Covered:

- Fast-regression input text: `mini sync test`
- Scenario kind: `ui`
- Mini Morse speeds: `slow`, `standard`, `fast`
- UI scenario actions driven through the normal ViewModel path:
  - select Mini mode
  - select the requested Morse speed
  - replace input text
  - encode generated Mini audio
  - open the player detail sheet
  - start playback
  - stop playback after the configured short capture window
- Debug log confirmation through `MiniAutomation`
- Unified Mini visual/Lyrics timing diagnostics through `MiniAlignmentPerf`
- Token-tape diagnostics through `FlashLyricsPerf`

Not covered yet:

- Headless Mini diagnostics
- Saved-audio library playback
- Decode validation after playback
- Visual pixel assertions or screenshot comparison
- Release/staging build automation entry points

## ADB Scenario

Debug builds support:

```text
com.bag.audioandroid.DEBUG_MINI_SCENARIO
```

Fresh install on a dedicated test device:

```powershell
python tools/run.py android install-debug-fresh
```

Single-speed UI scenario:

```powershell
adb shell am start -n com.bag.audioandroid/.MainActivity -a com.bag.audioandroid.DEBUG_MINI_SCENARIO --es wb.scenario ui --es wb.mini.speed standard --ez wb.encode true --ez wb.play true --el wb.play.ms 6000
```

Supported extras:

- `wb.scenario`
  - `ui` by default.
- `wb.input`
  - Optional text override.
  - Defaults to `mini sync test`.
- `wb.mini.speed`
  - `slow`, `standard`, `fast`
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
python tools/run.py android-debug device-prep
```

Recommended log capture:

```powershell
python tools/run.py android-debug capture-mini --speed standard --play-ms 6000 --wait-ms 20000
```

`MiniAlignmentPerf` is the preferred Mini sync diagnostic. It records the UI samples used for the Mini Morse timeline visual and the Lyrics token tape in the same row, plus active Morse group and active token state.

Success criteria for data capture:

- `MiniAutomation` confirms the scenario was received.
- `MiniAlignmentPerf` rows show `playing=true`.
- `sampleDelta` is visible for comparing Mini visual input sample and Lyrics input sample.
- `visualGroup` / `visualBitOffset` change as the Morse visual advances.
- `token`, `tokenText`, `tokenProgress`, and `lyricBitOffset` advance as Lyrics follows playback.

All-speed fast sweep:

```powershell
$speeds = @("slow", "standard", "fast")
foreach ($speed in $speeds) {
    python tools/run.py android-debug capture-mini --speed $speed --play-ms 6000 --wait-ms 20000 --output-dir "temp\android-debug\mini_$speed"
}
```

## Maintenance Rules

- Keep this flow debug-only.
- Do not use coordinate taps or accessibility automation for Mini generated playback checks.
- Keep Mini sync fixes separate from this capture prework unless a later task explicitly asks to change behavior.
