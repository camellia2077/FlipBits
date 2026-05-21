from __future__ import annotations

import re
from datetime import date
from pathlib import Path

from ..constants import ROOT_DIR
from ..process import run_capture
from .collect import (
    apply_scopes,
    collect_changed_paths,
    collect_version_hints,
    group_paths,
    normalize_scope,
    semantic_version_key,
)
from .model import BucketSummary, CandidateTopic, ChangedPath, HistoryPrepResult, RelevantSummary, VersionHint


def _has_any_path(paths: set[str], fragments: list[str]) -> bool:
    return any(any(fragment in path for fragment in fragments) for path in paths)


def _changed_diff_text(items: list[ChangedPath]) -> str:
    tracked_paths = [item.path for item in items if item.status != "??"]
    texts: list[str] = []
    if tracked_paths:
        result = run_capture(["git", "diff", "--unified=0", "--", *tracked_paths], cwd=ROOT_DIR, echo=False)
        if result.returncode == 0:
            texts.append(result.stdout)
    for item in items:
        if item.status != "??":
            continue
        path = ROOT_DIR / item.path
        if path.is_file() and path.stat().st_size <= 200_000:
            try:
                texts.append(path.read_text(encoding="utf-8", errors="replace"))
            except OSError:
                continue
    return "\n".join(texts)


def _has_any_diff(diff_text: str, tokens: list[str]) -> bool:
    return any(token in diff_text for token in tokens)


def _topic(
    *,
    bucket: str,
    title: str,
    reason: str,
    recommendation: str = "history-worthy",
    key_facts: list[str] | None = None,
    representative_files: list[str] | None = None,
) -> CandidateTopic:
    return CandidateTopic(
        title=title,
        reason=reason,
        bucket=bucket,
        recommendation=recommendation,
        key_facts=key_facts or [],
        representative_files=representative_files or [],
    )


def candidate_topics_for_bucket(bucket: str, items: list[ChangedPath]) -> list[CandidateTopic]:
    paths = {item.path for item in items}
    diff_text = _changed_diff_text(items)

    if bucket == "android-app":
        topics: list[CandidateTopic] = []
        if _has_any_path(paths, ["MorseCode.kt", "MorseSpeedOptionTest.kt"]) or _has_any_diff(
            diff_text, ["Wpm10", "Wpm15", "Wpm20", "translateMorseNotation"]
        ):
            facts: list[str] = []
            if _has_any_diff(diff_text, ["Wpm10", "Wpm15", "Wpm20"]):
                facts.append("Mini Morse speed presets changed from named styles to explicit `10 WPM` / `15 WPM` / `20 WPM` values.")
            if "translateMorseNotation" in diff_text:
                facts.append("Mini input can translate Morse notation pasted into the text field.")
            topics.append(
                _topic(
                    bucket=bucket,
                    title="Mini Morse speed and notation tools",
                    reason="Detected Morse speed/model changes or Mini-only Morse notation translation UI.",
                    key_facts=facts,
                    representative_files=[path for path in paths if "Morse" in path][:4],
                )
            )

        if _has_any_path(paths, ["UltraFrame", "UltraSymbolStepVisualizer.kt", "SymbolEnvelopeVisualizationAnalysis.kt"]) or _has_any_diff(
            diff_text, ["UltraFrameSection", "ultraFrameTimeline", "carrierFrequencyHz"]
        ):
            facts = []
            if "ultraFrameTimeline" in diff_text:
                facts.append("Android domain/JNI now carries Ultra full-frame symbol timeline data.")
            if "UltraFrameSection" in diff_text:
                facts.append("Ultra visual can distinguish preamble, sync, header, payload, and CRC sections.")
            if "debugUltraVisualLayout" in diff_text:
                facts.append("Debug builds expose `UltraVisualLayout` logs for visual layout inspection.")
            topics.append(
                _topic(
                    bucket=bucket,
                    title="Ultra full-frame visual timeline",
                    reason="Detected Ultra frame timeline, JNI/domain DTO, or visualizer changes.",
                    key_facts=facts,
                    representative_files=[path for path in paths if "Ultra" in path or "SymbolEnvelope" in path or "PayloadFollow" in path][:5],
                )
            )

        if _has_any_path(paths, ["AppSettingsRepository.kt", "TransportModeOption.kt"]) and _has_any_diff(
            diff_text, ["SelectedTransportModeId", "setSelectedTransportModeId", "fromWireName"]
        ):
            topics.append(
                _topic(
                    bucket=bucket,
                    title="Transport mode persistence",
                    reason="Detected DataStore and transport-mode selection persistence changes.",
                    key_facts=["Transport mode selection is stored and restored instead of always starting from the default mode."],
                    representative_files=[path for path in paths if "AppSettingsRepository" in path or "TransportModeOption" in path],
                )
            )

        if _has_any_path(paths, ["AudioSectionContainer.kt", "AppThemeVisualTokens.kt"]) or _has_any_diff(
            diff_text, ["AudioSectionContainer", "minimumSeparatedBlend", "groupContainerColor"]
        ):
            topics.append(
                _topic(
                    bucket=bucket,
                    title="Audio page grouped section containers",
                    reason="Detected Audio page grouping surfaces or theme token contrast changes.",
                    key_facts=[
                        "Input/result sections use a shared lightweight container.",
                        "Dual-tone group container color gets a minimum visual-difference fallback against the background.",
                    ],
                    representative_files=[path for path in paths if "AudioSectionContainer" in path or "AppThemeVisualTokens" in path],
                )
            )

        if _has_any_path(paths, ["app/src/main/cpp/", "native_package/private_include/"]) and _has_any_diff(
            diff_text, ["ultra_frame_timeline", "bag_ultra_frame_symbol_entry"]
        ):
            topics.append(
                _topic(
                    bucket=bucket,
                    title="Android native DTO support for Ultra frame data",
                    reason="Detected JNI/native-package bridge updates that support higher-level Ultra visual changes.",
                    recommendation="supporting implementation",
                    key_facts=["Native bridge copies Ultra frame timeline buffers into Android domain objects."],
                    representative_files=[path for path in paths if "cpp/" in path or "native_package/" in path][:5],
                )
            )

        if any(path.startswith("apps/audio_android/.kotlin/") for path in paths):
            topics.append(
                _topic(
                    bucket=bucket,
                    title="Android local Kotlin cache",
                    reason="Detected local Kotlin cache output; this is normally not release-history content.",
                    recommendation="probably skip",
                    representative_files=[path for path in paths if path.startswith("apps/audio_android/.kotlin/")][:3],
                )
            )

        if topics:
            return topics
        if any(path.endswith("app/src/main/cpp/audio_io_jni.cpp") for path in paths):
            return [_topic(
                bucket=bucket,
                title="android formal audio I/O boundary",
                reason="Detected Android JNI audio I/O changes that likely switch or reshape the app-facing WAV/metadata boundary.",
            )]
        if any(path.endswith("data/NativePlaybackRuntimeGateway.kt") for path in paths) or any(
            "NativePlaybackRuntimeBridge" in path for path in paths
        ):
            return [_topic(
                bucket=bucket,
                title="android playback runtime integration",
                reason="Detected Android playback runtime bridge/gateway changes for progress, scrub, or playback-state transitions.",
            )]
        if any(path.endswith("app/build.gradle.kts") for path in paths) or any(path.endswith("gradle.properties") for path in paths):
            return [_topic(
                bucket=bucket,
                title="android gradle root and release tooling",
                reason="Detected Android Gradle root, wrapper, or version-source changes under apps/audio_android.",
            )]
        if any(path.endswith("domain/DecodedAudioData.kt") for path in paths):
            return [_topic(
                bucket=bucket,
                title="android wav and metadata status split",
                reason="Detected Android decoded-audio domain changes that likely separate WAV status from metadata status.",
            )]
        return [_topic(
            bucket=bucket,
            title="android presentation workflow updates",
            reason="Detected Android app, domain, or native boundary changes under apps/audio_android.",
        )]

    if bucket == "cli-app":
        if any(path.endswith("src/commands.rs") for path in paths) or any(path.endswith("src/main.rs") for path in paths):
            return [_topic(
                bucket=bucket,
                title="rust cli command surface updates",
                reason="Detected Rust CLI command parsing or top-level command-surface changes.",
            )]
        if any(path.endswith("src/bag_api.rs") for path in paths):
            return [_topic(
                bucket=bucket,
                title="rust cli bag_api integration",
                reason="Detected Rust CLI bag_api FFI or encode/decode pipeline changes.",
            )]
        if any(path.endswith("src/audio_io_api.rs") for path in paths):
            return [_topic(
                bucket=bucket,
                title="rust cli wav and metadata integration",
                reason="Detected Rust CLI audio_io_api usage changes around WAV or metadata handling.",
            )]
        if any(path.endswith("Cargo.toml") for path in paths) or any(path.endswith("Cargo.lock") for path in paths):
            return [_topic(
                bucket=bucket,
                title="rust cli cargo and build workflow",
                reason="Detected Cargo manifest or Rust build workflow changes for the CLI presentation layer.",
            )]
        return [_topic(
            bucket=bucket,
            title="rust cli presentation updates",
            reason="Detected Rust CLI source or build changes under apps/audio_cli.",
        )]

    if bucket == "libs/audio_io":
        if any(path.endswith("include/audio_io_api.h") for path in paths):
            return [_topic(
                bucket=bucket,
                title="audio_io formal C ABI",
                reason="Detected `audio_io_api.h` changes under the audio I/O library boundary.",
            )]
        return [_topic(
            bucket=bucket,
            title="audio_io library contract updates",
            reason="Detected library-side audio I/O changes and/or tests.",
        )]

    if bucket == "libs/audio_api":
        topics = []
        if _has_any_diff(diff_text, ["bag_ultra_frame_symbol_entry", "ultra_frame_timeline", "BAG_ULTRA_FRAME_SECTION"]):
            topics.append(
                _topic(
                    bucket=bucket,
                    title="bag_api Ultra frame follow contract",
                    reason="Detected public C API fields for Ultra frame timeline data.",
                    key_facts=[
                        "`bag_payload_follow_data` exposes an Ultra frame timeline buffer/count/status set.",
                        "Encode result layout reports `ultra_frame_timeline_count` for two-pass allocation.",
                    ],
                    representative_files=[path for path in paths if "bag_api" in path][:4],
                )
            )
        if topics:
            return topics
        if any("/tests/" in path for path in paths):
            return [_topic(
                bucket=bucket,
                title="bag_api contract and test coverage",
                reason="Detected `bag_api` boundary changes together with library-side tests.",
            )]
        return [_topic(
            bucket=bucket,
            title="bag_api boundary updates",
            reason="Detected public API and implementation changes in `libs/audio_api`.",
        )]

    if bucket == "libs/audio_runtime":
        return [_topic(
            bucket=bucket,
            title="audio_runtime state and test coverage",
            reason="Detected runtime library and runtime test changes.",
        )]

    if bucket == "libs/audio_core":
        topics = []
        if _has_any_diff(diff_text, ["EncodePayloadToFrame", "DecodeFrameToPayload", "kCleanFrameV1Preamble", "Crc16CcittFalse"]):
            topics.append(
                _topic(
                    bucket=bucket,
                    title="Ultra clean frame codec",
                    reason="Detected Ultra frame encode/decode and validation logic.",
                    key_facts=[
                        "Ultra payloads are wrapped in a frame with preamble, sync, version, flags, payload length, and CRC.",
                        "Decode validates the frame before returning payload bytes.",
                    ],
                    representative_files=[path for path in paths if "ultra" in path][:4],
                )
            )
        if _has_any_diff(diff_text, ["BuildUltraFrameTimeline", "payload_begin_sample", "carrier_freq_hz"]):
            topics.append(
                _topic(
                    bucket=bucket,
                    title="Ultra frame-aware follow timeline",
                    reason="Detected follow timeline updates that align payload and visual data with framed Ultra audio.",
                    key_facts=[
                        "Payload begin/sample counts account for Ultra frame prefix and CRC.",
                        "Binary group entries carry Ultra carrier frequency for each payload nibble.",
                    ],
                    representative_files=[path for path in paths if "follow_payload" in path or "common/types" in path][:4],
                )
            )
        if topics:
            return topics
        if any("voicing_internal_" in path for path in paths):
            return [_topic(
                bucket=bucket,
                title="flash voicing internal split",
                reason="Detected `flash voicing` implementation split into internal support/texture/shell units.",
            )]
        return [_topic(
            bucket=bucket,
            title="audio_core module and implementation updates",
            reason="Detected core library source/module changes.",
        )]

    if bucket == "tests":
        return [_topic(
            bucket=bucket,
            title="cross-lib and product smoke regression coverage",
            reason="Detected root-level integration or smoke test changes.",
            recommendation="supporting implementation",
        )]

    if bucket == "tools":
        return [_topic(
            bucket=bucket,
            title="developer workflow tooling updates",
            reason="Detected `tools/run.py` or helper workflow changes.",
        )]

    return []


def collect_candidate_topics(grouped_paths: dict[str, list[ChangedPath]]) -> list[CandidateTopic]:
    topics: list[CandidateTopic] = []
    for bucket, items in grouped_paths.items():
        topics.extend(candidate_topics_for_bucket(bucket, items))
    return topics


def pick_representative_files(bucket: str, items: list[ChangedPath]) -> list[str]:
    paths = [item.path for item in items]

    preferred_by_bucket: dict[str, list[str]] = {
        "android-app": [
            "apps/audio_android/app/src/main/cpp/audio_io_jni.cpp",
            "apps/audio_android/app/src/main/java/com/bag/audioandroid/domain/DecodedAudioData.kt",
            "apps/audio_android/app/src/main/java/com/bag/audioandroid/data/NativePlaybackRuntimeGateway.kt",
            "apps/audio_android/app/build.gradle.kts",
            "apps/audio_android/gradle.properties",
        ],
        "cli-app": [
            "apps/audio_cli/rust/src/commands.rs",
            "apps/audio_cli/rust/src/bag_api.rs",
            "apps/audio_cli/rust/src/audio_io_api.rs",
            "apps/audio_cli/rust/Cargo.toml",
            "apps/audio_cli/rust/src/lib.rs",
        ],
        "libs/audio_api": [
            "libs/audio_api/include/bag_api.h",
            "libs/audio_api/tests/api_tests.cpp",
        ],
        "libs/audio_core": [
            "libs/audio_core/src/flash/voicing_impl.inc",
        ],
        "libs/audio_io": [
            "libs/audio_io/include/audio_io_api.h",
            "libs/audio_io/include/wav_io.h",
            "libs/audio_io/tests/unit_tests.cpp",
        ],
        "libs/audio_runtime": [
            "libs/audio_runtime/include/audio_runtime.h",
            "libs/audio_runtime/tests/runtime_tests.cpp",
        ],
    }

    selected = [path for path in preferred_by_bucket.get(bucket, []) if path in paths]
    if selected:
        return selected
    return paths[:3]


def make_relevant_summary(grouped_paths: dict[str, list[ChangedPath]]) -> RelevantSummary:
    return RelevantSummary(
        bucket_counts={bucket: len(items) for bucket, items in grouped_paths.items()},
        changed_bucket_order=list(grouped_paths.keys()),
    )


def suggest_release_version(version_hints: list[VersionHint]) -> str:
    explicit_versions = [hint.value for hint in version_hints if hint.value.startswith("v")]
    if not explicit_versions:
        return "vX.Y.Z"
    return max(explicit_versions, key=semantic_version_key)


def version_from_target_history_file(target_history_file: Path | None) -> str | None:
    if target_history_file is None:
        return None
    stem = target_history_file.stem
    if re.fullmatch(r"\d+\.\d+\.\d+", stem):
        return f"v{stem}"
    return None


def build_history_prep_result(scopes: list[str], target_history_file: Path | None = None) -> HistoryPrepResult:
    # Centralize collection + summarization here so markdown/plain/json output
    # remains a pure rendering concern.
    normalized_scopes = [normalize_scope(scope) for scope in scopes]
    changed_paths = apply_scopes(collect_changed_paths(), normalized_scopes)
    grouped_paths = group_paths(changed_paths)
    version_hints = collect_version_hints(changed_paths)
    candidate_topics = collect_candidate_topics(grouped_paths)
    # Suggested date intentionally comes from the current working date because
    # history headings are written as "the day this draft is produced", not
    # inferred from git commit timestamps or existing docs.
    suggested_date = date.today().isoformat()
    # Suggested version prefers the explicit target history filename such as
    # docs/presentation/cli/v0.2/0.2.0.md -> v0.2.0. If no target file is
    # provided, fall back to repo history hints so the agent still gets a
    # version placeholder.
    suggested_version = version_from_target_history_file(target_history_file) or suggest_release_version(version_hints)
    relevant_summary = make_relevant_summary(grouped_paths)
    representative_files = [
        representative
        for bucket_name, items in grouped_paths.items()
        for representative in pick_representative_files(bucket_name, items)
    ]

    buckets: list[BucketSummary] = []
    for bucket_name, items in grouped_paths.items():
        bucket_topics = [topic for topic in candidate_topics if topic.bucket == bucket_name]
        buckets.append(
            BucketSummary(
                name=bucket_name,
                changed_files=items,
                candidate_topics=bucket_topics,
                representative_files=pick_representative_files(bucket_name, items),
                relevant_summary=RelevantSummary(
                    bucket_counts={bucket_name: len(items)},
                    changed_bucket_order=[bucket_name],
                ),
            )
        )

    return HistoryPrepResult(
        scopes=normalized_scopes,
        suggested_date=suggested_date,
        suggested_version=suggested_version,
        component_versions=version_hints,
        candidate_topics=candidate_topics,
        representative_files=representative_files,
        relevant_summary=relevant_summary,
        changed_files=changed_paths,
        buckets=buckets,
    )
