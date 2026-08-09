# audio_api Scope Refactor Guide

Scope: `libs/audio_api`

## Boundary

- Keep `bag_api` public signatures, enum values, ownership rules, and result-buffer contracts stable.
- Keep API entrypoints focused on validation, orchestration, and conversion between public API data and library operations.
- Keep codec rules and transport-specific algorithms in `audio_core`; keep WAV byte/metadata work in `audio_io`.

## Refactor direction

- Split entrypoint files by operation family or ownership boundary, not by individual helper.
- Keep encode/decode operation lifecycle code together with its matching result and cleanup behavior.
- Treat tests as contract documentation; organize them by encode, decode, lifecycle, and public ABI coverage.

## Validation

```powershell
python tools/run.py build --build-dir build/dev
python tools/run.py test-lib audio_api --build-dir build/dev
```

Language guidance: `../languages/cpp_refactor.md`.
