# audio_cli Scope Refactor Guide

Scope: `apps/audio_cli`

## Boundary

- Keep Rust focused on command-line presentation, argument handling, and FFI adapters.
- Reuse `audio_api` and `audio_io` contracts for encode/decode and WAV behavior.
- Do not duplicate transport validation or metadata rules in the CLI layer.

## Refactor direction

- Separate command orchestration, FFI conversion, filesystem I/O, and terminal presentation when they accumulate together.
- Preserve stdout/stderr and exit-code contracts while moving code.
- Keep shared business rules below the CLI boundary.

## Validation

```powershell
python tools/run.py cli test
```

There is no Rust responsibility analyzer yet; use scope line/directory results as navigation hints and confirm boundaries from the source and CLI tests.
