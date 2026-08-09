# audio_core Scope Refactor Guide

Scope: `libs/audio_core`

## Boundary

- Keep transport and signal algorithms below `audio_api` and independent from Android, Web, and CLI presentation code.
- Treat module interfaces as public internal boundaries: facades re-export stable concepts; implementation units own algorithms.
- Keep Flash, Mini, Pro, Ultra, and Voice responsibilities separate unless a shared domain contract is explicit.

## Refactor direction

- Prefer module-first extraction with a named module interface and matching implementation unit.
- Move one coherent owner or move set at a time: rules, layout, encode, decode, or rendering.
- Do not put algorithm implementations back into `phy_clean.cppm` or create an unnecessary public API expansion.
- Preserve mode behavior and cross-mode test vectors while moving boundaries.

## Validation

```powershell
python tools/run.py build --build-dir build/dev
python tools/run.py test-lib audio_core --build-dir build/dev
```

Language guidance: `../languages/cpp_refactor.md`.
