# audio_core Scope Refactor Guide

Scope: `libs/audio_core`

Common requirements: [共同重构原则](../refactoring_principles.md).

## Boundary

- `audio_core` owns platform-independent domain behavior: mode codecs, PHY/signal/rendering rules, transport dispatch, encode work planning and steps, follow timelines, pipeline aggregation, and Voice FX.
- Keep Flash, Mini, Pro, Ultra, transport, pipeline, and Voice responsibilities separate unless a shared domain contract and multiple real consumers justify a common owner.
- Mode modules own their own rules, encode, decode, rendering, and signal/voicing behavior. Transport owns cross-mode validation/dispatch, operation planning, and follow integration; facades aggregate capabilities without becoming algorithm containers.
- Named module interfaces are internal architecture boundaries. Implementation units and canonical `*_impl.inc` bodies own algorithms; reserved C++17 interface headers and stable C ABI headers remain separate boundaries.
- Host code is module-first and capability-aware. Android package-private adapters must not force host code back to header-first design.

Dependency direction:

```text
common/leaf rules -> mode implementation -> mode facade
mode facades -> transport -> pipeline/audio_api consumer
```

Mode implementations must not depend upward on presentation scopes or on orchestration owners that consume them.

## Refactor direction

- Extract only a stable domain owner such as rules, layout, encode, decode, renderer, signal analysis, voicing, follow, or work-plan construction.
- A new owner should have a clear input/output contract, a one-way dependency position, and focused tests or vectors. Prefer a module interface plus matching implementation unit when the capability is consumed across implementation units.
- Keep one algorithm phase, its state/cache, and its invariants together even when the implementation is large. Private helpers with no separate modification reason remain with that owner.
- Update module interface, implementation unit, canonical `.inc`, CMake file set/source list, Android package source mapping, and focused tests as one coordinated change when they describe the same capability.
- Keep `phy_clean` and other facades thin; move algorithms to named responsibility modules without turning every helper into a module.
- Preserve mode behavior, sample timelines, error semantics, cross-mode vectors, and Android/host canonical implementation equivalence.

Do not:

- move mode-specific rules into transport merely because transport dispatches the mode;
- move cross-mode lifecycle/work-plan/follow policy into individual modes;
- create a public module for a single private helper or a one-consumer forwarding layer;
- copy canonical bodies into Android wrappers or create host/platform variants without an actual platform policy;
- use line count as evidence that an algorithm pipeline has multiple owners.

The CMake `CXX_MODULES` file set is the exact current module inventory. Architecture docs describe responsibility families and must not be used as a stale file checklist.

## Validation

```powershell
python tools/run.py build --build-dir build/dev
python tools/run.py test-lib audio_core --build-dir build/dev
```

Language guidance: `../languages/cpp_refactor.md`.

Architecture sources:

- `../../architecture/module-topology.md`
- `../../design/transports.md`
- `../../design/modes/README.md`
- `../../architecture/encode-operation-contract.md`
- `../../architecture/text-follow-contract.md`
- `../../../libs/AGENTS.md`
