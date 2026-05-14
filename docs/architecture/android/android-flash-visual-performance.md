# Android Flash Visual Performance

## Purpose

This document records the recent Android Flash Visual long-audio performance investigation, the optimizations that were tried, and the measured results from real-device sweeps.

Use this before changing Flash Visual performance behavior, especially for `Lanes`, long generated audio, seek/scrub follow, or windowing. The goal is to preserve the successful directions, avoid repeating the failed ones, and keep future performance work tied to measurable device data instead of subjective impressions.

This document complements:

- `docs/architecture/android/android-flash-visual.md`
  - Rendering architecture, data flow, and primary diagnostics.
- `docs/architecture/android/android-flash-automation.md`
  - Debug scenario contract, adb extras, and sweep automation entry points.
- `docs/architecture/android/android-flash-visual-scrub-regression-baseline.md`
  - Accepted post-fix real-device baseline for `Lanes` / `Pitch` scrub shape and seek-follow behavior.

## Scope

This investigation focused on:

- Flash `Visual` page playback with long generated audio.
- Real-device seek/scrub follow behavior.
- Cross-mode comparison for `Lanes`, `Pitch`, and `Pulse`.
- `Lanes`-specific performance optimization after correctness was stabilized.

It did not attempt:

- native/libs algorithm changes
- screenshot-based visual assertions
- release-build performance profiling

## Why This Exists

The original symptom was not only "performance feels heavy". It was a mix of:

- visual follow lag after seek
- uncertainty about whether token preview and Flash Visual were moving together
- concern that long-audio `Lanes` might be too expensive on mid-range hardware

The important lesson is that these symptoms cannot be diagnosed reliably from subjective playback feedback alone. Real-device adb logs were required to separate:

- correctness bugs
- smoothing / anchor bugs
- windowing cost
- actual Canvas draw cost

## Primary Files

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/AudioFlashSignalVisualizer.kt`
  - Main Flash Visual component and perf trace integration.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/FlashSignalVisualizationState.kt`
  - Smoothed visual sample state and seek-follow behavior.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/state/FlashVisualWindowState.kt`
  - Current Flash Visual window state and drawable budget.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/FlashVisualWindowActions.kt`
  - Window request/build logic.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioPlaybackScrubActions.kt`
  - Shared scrub lifecycle and window refresh timing.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/PlaybackTokenContextTape.kt`
  - Token preview motion behavior during playback and scrub.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/PlayerDetailSheet.kt`
  - Wires `isScrubbing` into the Visual page preview path.
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidApp.kt`
  - Debug scenario execution, including automated continuous scrub pacing.
- `tools/repo_tooling/android_debug/flash_visual_sweep.py`
  - Batch sweep entry point for `Lanes`, `Pitch`, and `Pulse`.

## Observability Strategy

This work used two debug streams:

- `FlashVisualPerf`
  - Rendering, windowing, raw/smoothed sample positions, primitive estimates, and visual error.
- `FlashLyricsPerf`
  - Token preview motion during scrub, including `scrubbing`, `direct`, and `deltaTx`.

The two key automation shapes were:

- seek-settle validation
  - proves whether visual/readout state lands near the committed seek target
- continuous scrub validation
  - proves whether token preview is following during the drag itself rather than only after commit

For the scenario surface and adb examples, use:

- `docs/architecture/android/android-flash-automation.md`

## Artifact Index

The main sweep and investigation artifacts from this round live under:

- `temp/android-debug/flash_seek_capture/`
  - seek and continuous scrub capture logs
- `temp/android-debug/flash_visual_sweep_20260514/`
  - first cross-mode baseline for `Lanes`, `Pitch`, `Pulse`
- `temp/android-debug/flash_visual_sweep_20260514_after_lanes_opt/`
  - baseline after `drawableSegments` budget optimization
- `temp/android-debug/flash_visual_sweep_20260514_after_window_opt/`
  - failed "larger window / lazier refresh" experiment
- `temp/android-debug/flash_visual_sweep_20260514_scrub_window_only/`
  - stable state keeping scrub-window optimization only
- `temp/android-debug/flash_visual_sweep_20260514_lanes_draw_cull/`
  - failed `draw-cull` experiment for `Lanes`

These temp artifacts are not the architecture contract, but they are the factual record behind the conclusions in this document.

## Correctness Fixes Before Performance Work

Performance conclusions only became trustworthy after the following correctness fixes were in place:

- Flash Visual seek-follow fix
  - `FlashSignalVisualizationState.kt` now snaps non-playing/scrubbing visual state to the current `rawSample` instead of returning a stale smoothed sample.
- Visual-page token preview direct-follow during scrub
  - `PlaybackTokenContextTape.kt` now uses direct follow for `VisualPreview + isScrubbing`.
- `isScrubbing` wiring fix
  - `PlayerDetailSheet.kt` now correctly passes scrub state into the playback display block.
- continuous scrub automation
  - `AudioAndroidApp.kt` now gives the UI real frame time between scrub start, intermediate changes, and scrub finish.

Do not evaluate Flash Visual performance on long audio until these fixes are present. Otherwise the logs can be dominated by correctness lag instead of rendering cost.

## Cross-mode Baseline

The first useful comparison sweep showed:

- `Lanes` was the heaviest mode.
- `Pitch` was materially lighter than `Lanes`.
- `Pulse` was the lightest mode by a wide margin.
- seek-immediate alignment was correct for all three modes after the seek-follow fix.
- token preview direct-follow during continuous scrub was stable across modes once the wiring was fixed.

Representative baseline conclusions:

- `Lanes`
  - highest primitive count
  - highest visible density
  - highest visual error peaks
- `Pitch`
  - medium cost
  - lower primitive count and lower error peaks than `Lanes`
- `Pulse`
  - very low primitive count
  - not the current optimization priority

This means future performance work should start with `Lanes`, then `Pitch`, and only touch `Pulse` if a new regression appears.

## Optimizations That Worked

### 1. Prefer Budgeted Drawable Segments

Change:

- `FlashSignalVisualizationState.kt`
  - fixed-timeline rendering now prefers `drawableSegments` instead of feeding full exact `segments` into Canvas by default
- `FlashSignalVisualizerModel.kt`
  - fallback fixed timeline also downsamples to a drawable budget

Why it worked:

- It reduced the amount of segment data pushed into the draw path without changing timing ownership.
- It preserved the fixed-timeline contract while cutting the upper bound of renderable primitives.

Measured outcome:

- `Lanes maxPrimitives` dropped from `216` to `108`.
- draw cost improved modestly.
- the seek-follow correctness fix remained intact.

Status:

- Keep this optimization.

### 2. Do Not Rebuild Windows On Every Scrub Step

Change:

- `AudioPlaybackScrubActions.kt`
  - `onScrubChanged(...)` no longer calls follow/Flash window refresh for every drag step
  - `onScrubFinished(...)` performs the window refresh once after the committed position is known

Why it worked:

- Continuous scrub can emit many intermediate samples quickly.
- Rebuilding the current Flash window on every step creates avoidable churn that does not improve the visible result during the drag.

Measured outcome:

- lower peak draw cost during scrub-heavy runs
- slightly lower visible primitive and settled delta values
- no regression in direct-follow token preview

Limit:

- This did not materially reduce the biggest `visualErrorMs` spikes by itself.

Status:

- Keep this optimization.

## Optimizations That Did Not Work

### 1. Larger Windows / Lazier Refresh

Change attempted:

- larger `VisualWindowPaddingSamples`
- larger `VisualWindowAnchorStepSamples`
- larger `RefreshMarginSamples`

Hypothesis:

- Fewer refreshes and a larger prefetched window might reduce rebuild churn.

Measured outcome:

- `Lanes maxPrimitives` increased
- `maxDrawMaxMs` increased
- `maxAbsVisualErrorMs` increased significantly

Interpretation:

- Increasing the window size moved more data into the active draw path and did not solve the true error source.
- The optimization traded request frequency for heavier windows and worse peak behavior.

Status:

- Do not continue this direction unless a future metric set proves a different bottleneck.

### 2. Lanes Draw Cull

Change attempted:

- skip extremely narrow off-playhead `Lanes` segments after viewport clipping
- adjust visible-segment metrics to match that culling rule

Hypothesis:

- Small edge segments might be inflating drawable/primitive cost without adding meaningful readability.

Measured outcome:

- no meaningful reduction in the effective primitive ceiling
- draw peaks became worse again
- `maxAbsVisualErrorMs` increased

Interpretation:

- The experiment did not remove the expensive part of the workload.
- The dominant issue was not tiny edge rectangles.
- The extra culling logic added complexity without improving the real bottleneck.

Status:

- Reverted.
- Do not continue this direction unless a future trace proves that micro-segment raster cost is the real limiter.

## Current Recommended State

The current stable recommendation is:

- keep the seek-follow correctness fix
- keep direct-follow token preview during `VisualPreview + isScrubbing`
- keep `drawableSegments` budgeted rendering for fixed timeline
- keep scrub-window refresh deferred until commit
- keep exact `segments` for `Lanes` and `Pitch` during scrub-time fallback so drag shape stays correct
- do not enlarge the visual window constants just to reduce request frequency
- do not reintroduce the `Lanes draw-cull` experiment

For the accepted real-device regression baseline after the `Lanes` / `Pitch` scrub-shape fixes, use:

- `docs/architecture/android/android-flash-visual-scrub-regression-baseline.md`

## How To Run The Same Investigation Again

1. Build and install a debug build on a connected device.
2. Use the Flash debug scenario with long generated audio.
3. Run the cross-mode sweep through the tooling wrapper.
4. Compare the new summary against the latest accepted baseline before deciding whether a change is an optimization or only a metric shuffle.

Representative workflow:

```powershell
python tools/run.py android assemble-debug
python tools/run.py android install-debug-fresh
python tools/run.py android-debug capture-flash-visual-sweep
```

For seek/scrub alignment investigations, use the raw scenario described in `android-flash-automation.md` when you need explicit `wb.seek.*` control.

## What To Check Before Starting New Performance Work

If a future contributor wants to optimize Flash Visual again, check these in order:

1. Is the issue really performance, or is it seek/smoothing correctness?
2. Which mode is affected: `Lanes`, `Pitch`, or `Pulse`?
3. Do `FlashVisualPerf` logs show draw cost, window churn, or visual error as the primary problem?
4. Is the proposed change reducing actual visible/draw cost, or only changing the counting method?
5. Can the change be validated with the same sweep and compared against an existing baseline folder?

If the answer to these questions is unclear, do not widen the change. Add a small debug metric first.

## Recommended Future Directions

If performance work resumes later, the best next directions are:

- re-check `Lanes` first, not `Pulse`
- prioritize changes that can lower real visible/draw cost without enlarging the active window
- inspect anchor/predict timing behavior when `visualErrorMs` is high even though draw cost is acceptable
- avoid speculative native/libs changes until Android logs prove the bottleneck is upstream

## Anti-patterns

Avoid these patterns unless metrics explicitly justify them:

- optimizing from subjective "feels heavy" reports alone
- widening the active window to avoid refreshes
- special-casing draw cull rules before confirming that draw cost is the real bottleneck
- changing native generation or follow-data structure before Android-side metrics identify an upstream data problem

The rule for Flash Visual performance work is simple:

measure first, choose the layer from logs, make the narrowest change that should move the metric, and rerun the same device capture.
