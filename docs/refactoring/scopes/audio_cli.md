# audio_cli Scope Refactor Guide

Scope: `apps/audio_cli`

Common requirements: [共同重构原则](../refactoring_principles.md).

## Boundary

- `audio_cli` owns command-line arguments, command/use-case orchestration, filesystem interaction, terminal output/progress/errors, and safe Rust adapters over the `bag_api` and `audio_io_api` C ABIs.
- `main.rs` is the composition root: parse, dispatch, and print/exit. `commands/` owns encode/decode use cases. `presentation/` owns CLI syntax, filesystem I/O, stdout/stderr, progress, errors, and version display.
- `bag_api/` and `audio_io_api/` own raw declarations, ABI types, status conversion, ownership guards, and the safe surface exposed to commands.
- Shared transport validation, codec behavior, WAV serialization/parsing, and metadata binary format stay in `libs`.
- CLI metadata policy may map CLI inputs and use-case results to the shared metadata DTO; that application policy is distinct from duplicating metadata format rules.

Dependency direction:

```text
main -> commands -> presentation ports + safe FFI adapters
safe FFI adapters -> bag_api/audio_io_api C ABI
```

Commands and presentation must not declare raw C ABI or manipulate native pointers directly.

## Refactor direction

- Separate command orchestration, FFI conversion, filesystem I/O, and terminal rendering when each has an independent reason to change and a clear interface.
- Keep `unsafe`, raw pointers, native allocation, and cleanup guards inside the matching FFI adapter. A guard and the operation/result it protects should remain easy to follow as one lifecycle.
- Keep encode/decode command flow cohesive: request construction, progress adapter, result handling, metadata policy, file write, and user-visible result should have one clear use-case owner even if delegated to stable collaborators.
- Preserve stdout for consumable results and stderr for progress/diagnostics/errors, along with exit codes, error text contracts, help output, and file behavior.
- Keep `main.rs` and module facades thin, but do not split raw bindings, guards, status mapping, and result conversion into additional files unless they form a stable ABI family.

Do not:

- recreate transport validation, mode algorithms, WAV parsing, or metadata binary layout in Rust;
- expose raw ABI types upward for the sake of reducing adapter size;
- create one module per enum mapper, status helper, or one-call wrapper;
- merge terminal presentation into FFI adapters or native cleanup into command code;
- treat `unsafe` count alone as evidence for extraction without tracing pointer ownership and cleanup.

Before acting on a scanner result, trace the command from CLI argument through its safe adapter to the C header and back through guard/result cleanup, then check unit and process-level integration tests.

## Validation

```powershell
python tools/run.py cli test
```

Rust responsibility reports are navigation hints, not extraction instructions. Confirm FFI ownership and command/presentation boundaries from the source, then follow `../languages/rust_refactor.md` and validate with CLI tests.

Architecture sources:

- `../../../apps/audio_cli/AGENTS.md`
- `../../architecture/repo-map.md`
- `../../architecture/compatibility-layer-inventory.md`
- `../../architecture/encode-operation-contract.md`
- `../../design/transports.md`
