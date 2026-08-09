# audio_runtime Scope Refactor Guide

Scope: `libs/audio_runtime`

## Boundary

- Keep playback/runtime ownership and scheduling concerns separate from codec rules and public API entrypoints.
- Treat runtime handles, cancellation, and shutdown as lifecycle contracts.
- Do not introduce presentation-specific UI state into the runtime library.

## Refactor direction

- Separate runtime orchestration from platform/audio-device adapters when both grow in one file.
- Keep ownership and cleanup paths together with the resource they manage.
- Preserve start, stop, cancel, and failure behavior before changing internal structure.

## Validation

```powershell
python tools/run.py build --build-dir build/dev
python tools/run.py verify review-fixes --build-dir build/dev
```

Language guidance: `../languages/cpp_refactor.md`.
