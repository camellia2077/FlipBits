# audio_api Scope Refactor Guide

Scope: `libs/audio_api`

Common requirements: [共同重构原则](../refactoring_principles.md).

## Boundary

- `audio_api` owns the stable `bag_api.h` C ABI: validation entrypoints, public-to-domain conversion, orchestration, operation handles, result marshaling, status mapping, and public buffer cleanup.
- Public signatures, enum numeric values, struct layout, probe/allocate/free behavior, and result-buffer ownership are contracts shared by Android, Web, and Rust CLI consumers.
- Codec, signal, transport, follow construction, and Voice FX algorithms belong to `audio_core`; WAV bytes and metadata belong to `audio_io`; platform DTOs and UI state remain in presentation scopes.
- `src/bag_api.cpp` is the compilation and ABI owner. Its private `.inc` files are cohesive implementation fragments of that owner, not automatically independent architecture modules.

Dependency direction:

```text
presentation adapter -> bag_api.h -> audio_api -> audio_core
```

`audio_api` must not depend on Android, Web, CLI, or platform UI types.

## Refactor direction

- A valid extraction is normally an API family with an independent contract, such as sync encode, encode operation, decode operation, follow/result copy, or Voice FX.
- Keep `create/begin -> pump/poll -> terminal check -> take result -> abort/destroy` together under one lifecycle owner. The same applies to native allocation and the matching public free operation.
- Keep validation and conversion close to the entrypoints whose ABI they interpret; move reusable domain decisions downward only when they are no longer API-specific.
- Preserve a single canonical cross-platform implementation. Android packaging wrappers may include the canonical implementation but must not carry a copied fallback algorithm.
- Treat focused API tests as contract documentation and group them by public behavior rather than by implementation fragment.

Do not:

- create one file for each scalar getter, private mapper, constant group, or forwarding entrypoint;
- expose a new public API only to make an internal extraction easier;
- hide duplicated implementations in `.inc` files or count each `.inc` as an independent owner;
- change enum values, ownership, cleanup order, probe semantics, or terminal-result behavior during a structural refactor.

Before accepting a scanner suggestion, read the matching public declarations, direct platform consumers, lifecycle tests, and all fragments included by the same compilation owner.

## Validation

```powershell
python tools/run.py build --build-dir build/dev
python tools/run.py test-lib audio_api --build-dir build/dev
```

Language guidance: `../languages/cpp_refactor.md`.

Architecture sources:

- `../../architecture/repo-map.md`
- `../../architecture/module-topology.md`
- `../../architecture/encode-operation-contract.md`
- `../../../libs/AGENTS.md`
