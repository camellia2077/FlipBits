# audio_io Scope Refactor Guide

Scope: `libs/audio_io`

Common requirements: [共同重构原则](../refactoring_principles.md).

## Boundary

- `audio_io` owns WAV byte/path I/O, mono PCM16 format handling, WBAG metadata serialization/parsing, probing, status/resource conversion, and the private libsndfile backend.
- `audio_io_api.h` is the stable C ABI used by Rust CLI and other C consumers. `wav_io.h` is the stable C++ file/byte boundary. `audio_io.wav` and `audio_io.wav_metadata_parse` are host internal module entrypoints.
- The libsndfile backend is private and is the sole approved third-party header owner; it does not need to be split merely to look thin.
- File format and metadata field semantics belong here. The policy that maps a presentation request to metadata values belongs to that presentation/application scope.
- Transport, codec, audio generation, playback, and UI display rules do not belong here.

Dependency direction:

```text
presentation/audio_api consumer -> stable audio_io boundary
stable boundary -> WAV/metadata rules -> private backend
```

The backend must not call upward into presentation or codec orchestration.

## Refactor direction

- Reasonable owners include WAV bytes, metadata parse/serialization rules, C ABI marshaling, and the third-party backend. Extract one only when it has an independent contract and focused tests.
- Keep allocation with its matching cleanup contract: encoded byte buffer/free, decoded WAV/free, and owned metadata/free must remain visibly paired.
- Preserve mono PCM16, sample rate/channel validation, metadata version/status mapping, truncated/unsupported behavior, and probe semantics during moves.
- Keep module and header front-ends aligned with the same canonical bytes implementation; do not create divergent module/header/platform algorithms.
- A backend may coordinate several third-party calls and their error handling as one cohesive owner. Thinness is not a goal by itself.

Do not:

- move CLI option policy, Android resource mapping, or Web Blob behavior into this scope;
- split status mapping, allocation, and free paths into unrelated microfiles;
- expose libsndfile types in stable headers or module interfaces;
- duplicate WAV parsing for an individual platform;
- interpret a cluster of format helpers as multiple owners when they jointly enforce one binary-format invariant.

## Validation

```powershell
python tools/run.py build --build-dir build/dev
python tools/run.py test-lib audio_io --build-dir build/dev
```

Language guidance: `../languages/cpp_refactor.md`.

Architecture sources:

- `../../architecture/module-topology.md`
- `../../architecture/compatibility-layer-inventory.md`
- `../../../libs/AGENTS.md`
