# Android Flash Visual Scrub Regression Baseline

## Purpose

This document records the accepted real-device baseline after fixing the Flash `Visual` page scrub-shape regressions for `Lanes` and `Pitch`.

Use this when a future change touches:

- scrub behavior
- fallback timeline selection
- `drawableSegments` vs exact `segments`
- `Lanes` / `Pitch` shape preservation during drag

This is intentionally short. It is not the general performance strategy document. For the broader investigation history, optimization attempts, and rejected directions, use:

- `docs/architecture/android/android-flash-visual-performance.md`

## What Was Fixed

Two scrub regressions were fixed:

- `Lanes` disappeared during drag
  - Cause: scrub disabled window refresh, but `Lanes` still preferred the old windowed timeline; once the playhead moved outside that window, there were no visible segments to draw.
- `Lanes` and `Pitch` collapsed into overly long continuous high/low segments during drag
  - Cause: scrub fallback used budget-merged `drawableSegments`, which compressed multiple bit segments into a single long segment.

The accepted behavior is now:

- when scrubbing and the visual must fall back from windowed timeline
  - `Lanes` and `Pitch` use exact `segments`
- when not scrubbing
  - visuals continue to prefer budgeted `drawableSegments`

This preserves drag-time bit shape without rolling back the normal playback performance optimizations.

## Relevant Files

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/FlashSignalVisualizationState.kt`
  - controls fallback/windowed timeline selection and exact-vs-budgeted segment selection
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioPlaybackScrubActions.kt`
  - keeps window refresh deferred until scrub commit
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/PlaybackTokenContextTape.kt`
  - keeps token preview in direct-follow mode while scrubbing
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidApp.kt`
  - drives continuous scrub automation with real frame spacing

## Accepted Device Baseline

Capture folder:

- `temp/android-debug/20260514-084302-flash-visual-sweep-standard/`

Primary summary:

- `temp/android-debug/20260514-084302-flash-visual-sweep-standard/summary.md`

Per-mode summaries:

- `temp/android-debug/20260514-084302-flash-visual-sweep-standard/lanes/summary.md`
- `temp/android-debug/20260514-084302-flash-visual-sweep-standard/pitch/summary.md`
- `temp/android-debug/20260514-084302-flash-visual-sweep-standard/pulse/summary.md`

## Baseline Conclusions

The accepted post-fix baseline is:

- `Lanes`, `Pitch`, and `Pulse` all keep seek correctness
  - `seekImmediate display/raw/smooth/readout = 0`
- token preview direct-follow remains stable during continuous scrub
  - `scrubPreview = 124`
  - `directPreview = 124`
  - `scrub max |deltaTx| = 0.00`
- `Lanes` and `Pitch` no longer lose their intended bit shape during drag
- no obvious user-facing scrub regression was observed from the exact-segment fallback path

Representative sweep table:

| mode | seekImmediate ok | scrubPreview | directPreview | scrub max \|deltaTx\| | avgDrawAvgMs | maxDrawMaxMs | maxAbsVisualErrorMs |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `lanes` | yes | 124 | 124 | 0.00 | 0.17 | 2.75 | 204.16 |
| `pitch` | yes | 124 | 124 | 0.00 | 0.17 | 1.25 | 52.31 |
| `pulse` | yes | 124 | 124 | 0.00 | 0.07 | 2.15 | 38.36 |

## Important Interpretation

One metric changed sharply for `Lanes`:

- `maxPrimitives = 7312`

Do not interpret this alone as a regression. In this accepted baseline, that number reflects scrub-time exact-segment fallback and trace counting during drag, not an observed visible failure.

The more important acceptance criteria for this baseline are:

- drag-time shape correctness
- seek correctness
- token preview direct-follow correctness
- no obvious drag-time visual disappearance

If a future change reduces `maxPrimitives` but breaks drag-time symbol shape again, that is not an improvement.

## When To Re-open Performance Work

Do not reopen `Pitch` performance work just because it is heavier than `Pulse`.

Only reopen a `Pitch` or `Lanes` optimization pass when at least one of these becomes true:

- real-device drag or playback visibly stutters
- new sweeps show sustained draw-cost growth, not just scrub-time primitive-count inflation
- low-end or mid-range devices show a reproducible regression
- a future change breaks this baseline and needs a narrower rework

## Validation Workflow

Use the standard sweep:

```powershell
python tools/run.py android assemble-debug
python tools/run.py android install-debug-fresh
python tools/run.py android-debug capture-flash-visual-sweep
```

Use this baseline as the first comparison point before accepting any future scrub-related visual change.
