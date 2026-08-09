# audio_web Scope Refactor Guide

Scope: `apps/audio_web`

Common requirements: [共同重构原则](../refactoring_principles.md).

## Boundary

- `audio_web` owns the static page, browser application flow, DOM rendering, sample/i18n presentation, browser audio/file adapters, worker messaging, and the WebAssembly ABI adapter.
- Stable owners are:
  - `app.js`: composition root;
  - `AppController`: workflow and browser resource lifecycle;
  - `UiController` and sample view/controller/service: DOM and sample presentation;
  - `EncoderClient`: worker request/promise ownership;
  - `encode-worker.js`: Wasm initialization and encode operation pump lifecycle;
  - Wasm wrapper/native bridge: memory and `bag_api` ABI conversion.
- Web consumes the `libs` encode operation snapshot/work-plan as the only source of progress, phase, terminal state, and failure semantics. The bridge must not define a second lifecycle or progress model.
- Browser decoding/resampling, PCM16 conversion, Blob creation, recording streams, and Object URLs are Web platform responsibilities. Codec, Voice FX, follow, and transport algorithms remain in `libs`.
- Offline file Voice FX stays aligned with `bag_apply_voice_fx`; streaming APIs are only for an explicit live/block workflow.

Dependency direction:

```text
DOM/app -> controllers/views -> EncoderClient -> worker
worker -> Wasm wrapper -> native bridge -> bag_api.h
```

DOM and browser types do not move below the presentation/adapter layer; core rules do not move upward into JavaScript.

## Refactor direction

- Extract by workflow, view owner, browser resource lifecycle, worker protocol, or ABI contract family—not by method count or file length.
- Keep `begin -> pump -> terminal check -> take result -> abort` together in the worker operation owner. Pump budget and message throttling are worker scheduling policy; snapshot/work-plan contents remain `libs` facts.
- Keep Object URL replacement/revocation, recording stream start/stop, selected input, and current result cleanup with the application workflow that owns those resources.
- Keep `EncoderClient` request ids, pending promises, callbacks, and completion cleanup together.
- Keep Wasm memory allocation/copy/free and native operation/result cleanup visibly paired across the wrapper and bridge.
- Performance diagnostics may measure or approximately attribute local cost, but approximations cannot drive user progress, phase labels, terminal decisions, or business behavior. In particular, worker-side Flash subphase estimation is diagnostic-only unless promoted to a `libs` contract.
- Preserve browser-visible progress/failure behavior, offline Voice FX path, file sample-rate conversion, downloadable WAV behavior, accessibility, and i18n keys during moves.

Do not:

- derive a smoother local progress state machine or reconstruct work-plan phases from mode constants;
- implement tokenization, UTF-8 character boundaries, mode validation, Voice FX, or codec rules in the UI/worker;
- create a JavaScript file for each DOM section, private formatter, scalar Wasm getter, or message type without an independent owner;
- split worker lifecycle cleanup away from the operation it cleans up;
- treat `AppController` or `UiController` size alone as proof that their cohesive workflow/view ownership is wrong.

Before accepting a scanner candidate, trace the browser event through controller, client, worker, Wasm wrapper, bridge, public C API, result cleanup, and browser resource cleanup.

## Validation

```powershell
python tools/run.py web test
```

There is no JavaScript responsibility analyzer yet; use scope line/directory results as navigation hints and confirm boundaries from the source and Web tests.

Architecture sources:

- `../../../apps/audio_web/AGENTS.md`
- `../../architecture/web-app-architecture.md`
- `../../architecture/encode-operation-contract.md`
- `../../architecture/text-follow-contract.md`
- `../../design/transports.md`
