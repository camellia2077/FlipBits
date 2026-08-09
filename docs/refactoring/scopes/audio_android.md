# audio_android Scope Refactor Guide

Scope: `apps/audio_android`

Common requirements: [共同重构原则](../refactoring_principles.md).

## Boundary

- `audio_android` owns Compose presentation, Android feature orchestration, domain gateway interfaces/models, platform/data adapters, AudioTrack integration, JNI marshaling, and Android resources.
- Shared codec, transport, follow, encode lifecycle, Voice FX, WAV, and playback-state semantics remain in `libs`; Android consumes their stable contracts.
- The intended dependency direction is:

```text
Compose screen -> ViewModel/actions -> domain gateway interface
domain gateway interface <- data/native implementation -> JNI
JNI -> bag_api.h / audio_runtime.h / package-private audio_io wrapper
```

- Screens render immutable state and emit callbacks. They do not call JNI, derive progress/follow rules, or own native handles.
- ViewModels/actions own feature state mutations and application flow. Domain owns stable Kotlin interfaces/models; data/native owns Android and ABI adapters.
- The audio platform layer owns AudioTrack and device actions. `audio_runtime` owns only platform-neutral playback session transitions.
- JNI/C++ owns ABI DTO conversion, Java/native error mapping, and native resource cleanup. It must not include private `audio_core` interfaces or duplicate canonical algorithms.
- Preserve JNI names/signatures, `@Keep`/R8 reachability, resource keys, serialization contracts, navigation contracts, and theme import/export compatibility.

## Refactor direction

- Use feature cohesion before file-type slicing. A screen flow may keep its state, actions, private helpers, and closely coupled rendering together when they change for the same reason.
- Valid Kotlin extractions include an independently reusable component, a stable state reducer/action owner, a gateway interface/implementation, a browser/media repository, or a visual pipeline stage with a measurable contract.
- Do not create files for one private data class, constant group, callback adapter, or forwarding function unless it has an independent consumer or lifecycle.
- Split JNI by complete ABI DTO family or resource lifecycle, not by individual native method. Keep create/use/release and allocate/copy/free behavior visibly connected.
- Android native packaging may wrap canonical shared implementation sources, but must not carry copied codec, WAV, Voice FX, follow, or operation logic.
- Offline file Voice FX and streaming/live Voice FX are different contracts; do not merge them for apparent reuse.
- Faction themes flow through catalog -> primary/secondary/outline mappings -> shared tokens/components. Do not add component-local theme id branches.
- For visual performance work, distinguish data source, visible window, smoothing/state model, analysis, drawing, and diagnostics before deciding a boundary. Measurements justify an extraction; file size does not.
- When moving lifecycle code, verify Kotlin state transitions, coroutines/cancellation, stream/Object ownership, JNI cleanup, and playback device cleanup together.

Do not:

- move shared domain rules upward because they are easier to access from a ViewModel;
- split every state/model/helper into separate microfiles to lower scan scores;
- make JNI a second orchestration layer or let Compose depend on native DTO layout;
- infer tokenization, UTF-8 character boundaries, progress phases, or work percentages in UI code;
- treat a dense feature directory as proof that every file is well-factored or that every large owner must be split.

Before accepting a scanner candidate, read the full feature flow from screen through ViewModel/action/gateway to JNI/libs, including tests and Android resource consumers.

## Validation

```powershell
python tools/run.py android test-debug
python tools/run.py android assemble-debug
```

Language guidance:

- `../languages/kotlin_refactor.md`
- `../languages/cpp_refactor.md`

Architecture sources:

- `../../../apps/audio_android/AGENTS.md`
- `../../architecture/android/android-app-architecture.md`
- `../../architecture/android/android-ui-structure.md`
- `../../architecture/android/android-native-strategy.md`
- `../../architecture/encode-operation-contract.md`
- `../../architecture/text-follow-contract.md`
