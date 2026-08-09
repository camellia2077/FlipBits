# audio_io Scope Refactor Guide

Scope: `libs/audio_io`

## Boundary

- Keep WAV byte I/O, metadata parsing, and backend orchestration distinguishable.
- Keep file-format rules independent from higher-level API and presentation concerns.
- Preserve mono PCM16, sample-rate, metadata, and error-code contracts.

## Refactor direction

- Extract format rules and metadata interpretation as cohesive units.
- Keep backend files thin: coordinate reads/writes and delegate parsing or encoding decisions.
- Avoid moving audio generation or transport semantics into this scope.

## Validation

```powershell
python tools/run.py build --build-dir build/dev
python tools/run.py test-lib audio_io --build-dir build/dev
```

Language guidance: `../languages/cpp_refactor.md`.
