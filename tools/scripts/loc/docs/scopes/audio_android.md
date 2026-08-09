# audio_android Scope Refactor Guide

Scope: `apps/audio_android`

## Boundary

- Kotlin presentation owns screen state and UI composition; JNI/C++ owns the native boundary and marshaling.
- Keep shared audio behavior in `libs`; Android should consume stable API contracts rather than duplicate codec or WAV rules.
- Preserve JNI names, signatures, `@Keep`/ProGuard reachability, and Android resource contracts.

## Refactor direction

- Split Kotlin files by UI responsibility or state ownership, not by arbitrary line ranges.
- Keep JNI bridge code thin and move domain rules into `libs` or focused native modules.
- When moving lifecycle code, verify both Kotlin state transitions and native handle cleanup.

## Validation

```powershell
python tools/run.py android test-debug
python tools/run.py android assemble-debug
```

Language guidance:

- `../languages/kotlin_refactor.md`
- `../languages/cpp_refactor.md`
