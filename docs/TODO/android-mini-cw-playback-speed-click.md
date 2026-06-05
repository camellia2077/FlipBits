# Android Mini/CW Playback Speed Switch Click

Date: 2026-06-02

## Current Priority

This issue is currently **not a high-priority blocker**.

User judgment:

- If a user switches to a low playback speed, the main intent is usually to listen carefully at one fixed slow speed.
- Frequent back-and-forth speed switching is not the primary expected workflow.
- It would be better if all speed switches were silent, but this is not urgent compared with the core use case:
  - slow playback can be recorded,
  - fixed-speed Mini/CW playback is usable,
  - Dit/Dah ending clicks are reduced.

Do not spend more time on small speculative patches unless this becomes a priority again or new evidence clearly identifies the remaining source.

## User-Visible Issue

When switching playback speed during Android Mini/CW playback, a click or click-like artifact is still audible at the switching moment.

The issue remains after several rounds of fixes. Based on the latest attempts, this is likely no longer a simple tone-ending envelope problem or a simple AudioTrack release race.

## What Is Already Fixed

### 1. Mini/CW Dit/Dah Ending Clicks

The click at the end of Mini/CW Dit/Dah tones was reduced successfully.

Implemented in:

- `libs/audio_core/src/mini/phy_clean_impl.inc`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/audio/MiniCwSpeedAdjustedPcmRenderer.kt`

Approach:

- Added the same envelope on both the core Mini/CW generator and the Android Mini/CW slow-speed renderer.
- Envelope is a 5 ms raised-cosine attack/release.
- Ramp length is clipped to at most half of the tone length.

Observed result:

- User reported that Mini 1.0x and slow-speed Dit/Dah ending clicks are basically no longer audible.
- Slow playback remains recordable.

### 2. Slow Playback Recording

The slow playback implementation still uses re-synthesized/rendered PCM, so it can be recorded.

This is intentional. Avoid replacing it with an AudioTrack-only playback speed trick if recording slow playback must keep working.

## Speed Switch Click Fixes Already Tried

### 1. Playback Edge Fade

Implemented playback edge fades around playback boundaries:

- Fade-in at the start of newly played PCM.
- Fade-out before stopping the old AudioTrack.

Related files:

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/audio/PlaybackEdgeFade.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/audio/AudioPlayerPlaybackLoop.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/audio/AudioTrackTransport.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/audio/AudioPlayer.kt`

Current shape:

- Playback start fade uses about 12 ms.
- Stop fade uses stepped AudioTrack volume fade before stop.

Result:

- Did not fully remove click during speed switching.

### 2. Wait For Stop Fade Before Release

Logs showed `releaseTrack` could occur between `fadeOutStart` and `fadeOutStop`.

Fix:

- `releaseTrack` now waits for an in-progress stop fade to finish before hard stop/release.
- Max wait is currently 40 ms.

Related file:

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/audio/AudioPlayer.kt`

Useful logs:

- `PlaybackEdgeFade fadeOutStart`
- `PlaybackEdgeFade fadeOutStop`
- `AudioPlayerDiag releaseTrack`
- `PlaybackEdgeFade releaseAwaitFade`
- `PlaybackEdgeFade releaseAwaitFadeDone`

Result:

- Logs later confirmed this race was mostly fixed.
- The speed-switch click still remained.

### 3. Start Streaming Tracks After First Buffer Prime

Logs showed streaming tracks could `play()` before the first rendered/written buffer arrived.

Fix:

- Streaming paths now prime the first buffer before starting AudioTrack playback.
- This avoids empty-buffer startup for:
  - file streaming,
  - rendered PCM streaming,
  - speed-adjusted render streaming.

Related file:

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/audio/AudioPlayerPlaybackLoop.kt`

Useful log:

- `PlaybackEdgeFade startAfterPrime path=... primed=...`

Result:

- Logs confirmed `startAfterPrime` was emitted.
- The speed-switch click still remained.

### 4. Handoff Playback: Prepare New Before Stopping Old

A larger local attempt was made to avoid stop-then-render-then-start gaps.

Fix attempt:

- Speed-change restart no longer immediately stops the old track.
- The new playback is started first.
- When the new playback actually starts, the old playback handle is asynchronously fade-stopped.
- Handoff speed changes force speed-adjusted playback through streaming render to reduce startup latency.
- Rapid consecutive speed changes try to keep the current audible previous handle and stop superseded pending handles.

Related files:

- `apps/audio_android/app/src/main/java/com/bag/audioandroid/audio/AudioPlaybackCoordinator.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/audio/AudioPlayer.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/audio/AudioPlayerPlaybackLoop.kt`
- `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioPlaybackCommandActions.kt`

Useful logs:

- `PlaybackCoordDiag startPlaybackHandoff`
- `PlaybackCoordDiag handoffStopPrevious`
- `PlaybackCoordDiag handoffStopSupersededPending`
- `PlaybackEdgeFade startAfterPrime`
- `PlaybackEdgeFade fadeIn`
- `PlaybackEdgeFade fadeOutStart`
- `PlaybackEdgeFade fadeOutStop`

Result:

- User still reported click during speed switching.
- Therefore, the remaining problem is probably not solved by local fade, release wait, prebuffering, or simple handoff.

## Diagnostic Tools And Commands

Use the playback speed capture tool first if this issue is reopened.

Install latest debug build:

```powershell
python tools/run.py android install-debug-fresh
```

Capture manual speed-switch logs:

```powershell
python tools/run.py android-debug capture-playback-speed --wait-ms 120000 --output-dir temp/android-debug/playback-speed-manual
```

Relevant output:

- `temp/android-debug/playback-speed-manual/raw.log`
- `temp/android-debug/playback-speed-manual/summary.md`

The summary is intentionally filtered. Use `raw.log` for exact ordering if needed.

Relevant log tags:

- `PlaybackSpeedDiag`
- `PlaybackCoordDiag`
- `AudioPlayerDiag`
- `AudioTrackTransport`
- `PlaybackEdgeFade`

## Current Interpretation

The evidence suggests the remaining artifact may not be a simple PCM edge click.

Likely possibilities:

- It may be a semantic playback discontinuity caused by switching between independently rendered timelines.
- It may be a phase/timing discontinuity inside Mini/CW re-synthesis when starting from an arbitrary source sample.
- It may be perceived as a click but actually be a very short timing jump, duplicated segment, or discontinuity in Morse rhythm.
- It may be caused by Android mixer/device behavior when multiple AudioTracks hand off, but the handoff attempt did not eliminate it.

Important: do not assume this is still the old `releaseTrack` race. Newer logs showed that race was addressed.

## Future Repair Directions

If this becomes important again, prefer one of these directions instead of adding more edge fades.

### Option A: Continuous Mini/CW Speed Renderer

Best long-term direction for Mini/CW.

Build a playback renderer that owns the Mini/CW timeline continuously and changes speed parameters without tearing down and recreating playback.

Properties:

- One logical stream.
- No stop/restart on speed switch.
- Speed changes affect future generated samples.
- Renderer preserves enough state to avoid arbitrary phase/timing resets.
- Recording can still use the same rendered PCM stream.

This may require moving more Mini/CW rendering logic into a reusable lower-level renderer, possibly under `libs`, if Android and non-Android paths should share it.

### Option B: Switch Only At Mini/CW Safe Boundaries

Defer speed changes to a safe Morse boundary, for example:

- symbol gap,
- character gap,
- word gap.

Tradeoff:

- Adds latency after the user taps a speed.
- Much simpler than a fully continuous renderer.
- More domain-correct for Morse than switching at arbitrary PCM samples.

This likely needs Mini/CW timeline boundary data from `PlaybackRenderContext.followData` or related frame/follow metadata.

### Option C: Offline Transition PCM Test

Before changing more runtime code, create a deterministic test that synthesizes:

1. old-speed tail around switch point,
2. new-speed head around switch point,
3. the exact intended crossfade/handoff PCM.

Then inspect the waveform and listen to the generated transition WAV.

This helps answer whether the artifact exists in rendered PCM itself or only on-device during AudioTrack handoff.

If the offline transition is clean but device playback clicks, focus on Android playback transport.
If the offline transition clicks, focus on Mini/CW rendering/timeline continuity.

### Option D: Lower Streaming Threshold Only

This is a smaller mitigation, not a full fix.

Recent logs showed some slow-speed switches spent over 100 ms before new playback started when the path pre-rendered a larger speed-adjusted buffer.

Forcing streaming render during speed-change handoff reduced that startup latency, but did not fully solve the issue.

Do not expect this alone to remove all artifacts.

## Verification Commands Used During Attempts

These commands passed during the latest rounds:

```powershell
python tools/run.py android ktlint-check
python tools/run.py android assemble-debug
python tools/run.py android test-debug --tests com.bag.audioandroid.audio.PlaybackEdgeFadeTest
python tools/run.py android test-debug --tests com.bag.audioandroid.audio.SpeedAdjustedPcmRendererTest
python tools/run.py android test-debug --tests com.bag.audioandroid.audio.MiniCwSpeedAdjustedPcmRendererTest
python tools/run.py android test-debug
```

Parallel Gradle test invocations can conflict on test result files. Run `test-debug` commands serially.

## Recommendation For Future Agents

Do not continue iterating blindly on:

- longer fade durations,
- more release waiting,
- more first-buffer priming,
- small AudioTrack lifecycle tweaks.

Those were already tried and did not solve the user-visible speed-switch click.

If work resumes, first decide whether the goal is worth prioritizing. If yes, use an offline transition PCM test or a continuous Mini/CW renderer approach to distinguish renderer/timeline discontinuity from Android transport behavior.
