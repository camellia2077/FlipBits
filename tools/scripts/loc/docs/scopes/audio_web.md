# audio_web Scope Refactor Guide

Scope: `apps/audio_web`

## Boundary

- Web presentation consumes the stable `libs` encode operation and progress contracts.
- The WebAssembly bridge is an adapter; it should not define a second lifecycle or progress model.
- Keep offline Voice FX aligned with `bag_apply_voice_fx`; streaming APIs are for live/block processing.

## Refactor direction

- Separate UI state, worker orchestration, audio utilities, and native/WASM bridge code.
- Keep mode-specific rules and audio parsing in `libs` rather than duplicating them in JavaScript.
- Preserve browser-visible progress, failure, and file-format behavior while moving boundaries.

## Validation

```powershell
python tools/run.py web test
```

There is no JavaScript responsibility analyzer yet; use scope line/directory results as navigation hints and confirm boundaries from the source and Web tests.
