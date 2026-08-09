# audio_runtime Scope Refactor Guide

Scope: `libs/audio_runtime`

Common requirements: [共同重构原则](../refactoring_principles.md).

## Boundary

- `audio_runtime` currently owns a small, platform-independent, value-semantic playback session state machine and sample/time conversion helpers.
- Its contract covers cleared/load, play/pause/resume/progress, scrub start/change/commit/cancel, stop/complete/fail, progress fraction, clamping, and elapsed/total time.
- It does not own native handles, threads, device scheduling, AudioTrack, audio-device adapters, codec behavior, or presentation UI state.
- `audio_runtime.h` is a stable C17-compatible boundary consumed by Android JNI. Enum values, state fields, and transition semantics are public contracts.

Dependency direction:

```text
platform playback coordinator -> audio_runtime state transitions
```

The runtime returns state; platform code performs device actions and renders UI.

## Refactor direction

- Prefer keeping the current state machine together. Its transitions share the same invariants and are easier to review and test as one owner.
- Extract only if a new independently testable policy appears, such as a genuinely reusable time-conversion policy or a platform-neutral strategy with more than one consumer.
- Preserve every transition, scrub resume behavior, clamping rule, terminal phase, and zero/invalid input behavior during structural changes.
- If a platform/device adapter is introduced later, place it outside this pure state owner and keep its resource lifecycle together in the platform scope.

Do not:

- split one file per transition or group functions only by line count;
- introduce handles, cancellation tokens, shutdown machinery, or scheduling abstractions that the current contract does not own;
- move Android playback coordination or UI flags into the library;
- interpret this small high-signal file as a refactor target solely because many transitions reference the same state fields.

## Validation

```powershell
python tools/run.py build --build-dir build/dev
python tools/run.py test-lib audio_runtime --build-dir build/dev
```

Language guidance: `../languages/cpp_refactor.md`.

Architecture sources:

- `../../architecture/repo-map.md`
- `../../architecture/compatibility-layer-inventory.md`
- `../../../libs/AGENTS.md`
