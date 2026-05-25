from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from .report_models import DetailReport, DirectoryFileMatch, LineFileMatch, PathScanResult, ScanReport
from .responsibility_analyzers import ResponsibilityRiskResult
from .responsibility_metrics import ResponsibilityAnchor, ResponsibilityFunctionHotspot, ResponsibilityMoveSet


@dataclass(frozen=True)
class FormattedEntry:
    title: str
    summary: str | None = None
    risks: tuple[str, ...] = ()
    suggestion: str | None = None
    next_action: str | None = None
    evidence: str | None = None
    columns: tuple[tuple[str, str], ...] = ()
    function_hotspots: tuple["FormattedHotspot", ...] = ()
    anchors: tuple["FormattedAnchor", ...] = ()
    responsibility_clusters: tuple["FormattedResponsibilityCluster", ...] = ()
    extraction_candidates: tuple["FormattedExtractionCandidate", ...] = ()
    move_sets: tuple["FormattedMoveSet", ...] = ()
    stop_signal: str | None = None
    validation_hints: tuple[str, ...] = ()
    false_positive_notes: tuple[str, ...] = ()


@dataclass(frozen=True)
class FormattedHotspot:
    name: str
    kind: str
    score: int
    lines: str
    summary: str
    risks: tuple[str, ...]
    evidence: tuple[str, ...]


@dataclass(frozen=True)
class FormattedAnchor:
    line: int
    label: str
    issue: str
    evidence: str


@dataclass(frozen=True)
class FormattedResponsibilityCluster:
    name: str
    owners: tuple[str, ...]
    reason: str


@dataclass(frozen=True)
class FormattedExtractionCandidate:
    name: str
    lines: str
    suggested_boundary: str
    reason: str
    risk: str
    validation: str


@dataclass(frozen=True)
class FormattedMoveSet:
    name: str
    target_boundary: str
    helpers: tuple[str, ...]
    reason: str
    validation: str


@dataclass(frozen=True)
class FormattedPathSection:
    path: str
    title: str
    entries: tuple[FormattedEntry, ...]
    empty_message: str | None = None


@dataclass(frozen=True)
class FormattedScanReport:
    heading: str
    metadata: tuple[tuple[str, str], ...]
    path_sections: tuple[FormattedPathSection, ...]
    summary: tuple[tuple[str, str], ...]
    error: str | None = None


class ReportFormatter:
    def __init__(self, *, display_name: str, over_inclusive: bool, lang: str):
        self.display_name = display_name
        self.over_inclusive = over_inclusive
        self.lang = lang

    def format_scan_report(self, report: ScanReport) -> FormattedScanReport:
        heading = self._heading_for_scan(report)
        metadata = self._metadata_for_scan(report)
        path_sections = tuple(self._format_path_section(report, result) for result in report.results)
        summary = tuple((key, str(value)) for key, value in report.summary.items())
        return FormattedScanReport(
            heading=heading,
            metadata=metadata,
            path_sections=path_sections,
            summary=summary,
            error=report.error,
        )

    def format_detail_report(self, report: DetailReport) -> FormattedEntry:
        return self._format_detail_result(report.result)

    def _heading_for_scan(self, report: ScanReport) -> str:
        if report.scan is None:
            return f"{self.display_name} 扫描报告"
        if report.scan.mode == "dir_over_files":
            return f"{self.display_name} 目录文件数扫描报告"
        if report.scan.mode == "responsibility_risk":
            return f"{self.display_name} 职责混杂风险扫描报告"
        mode_text = "大文件" if report.scan.mode == "over" else "小文件"
        comparator = (">=" if self.over_inclusive else ">") if report.scan.mode == "over" else "<"
        return f"{self.display_name} 代码行数扫描报告 ({mode_text}模式: {comparator} {report.scan.threshold} 行)"

    def _metadata_for_scan(self, report: ScanReport) -> tuple[tuple[str, str], ...]:
        items: list[tuple[str, str]] = [
            ("generated_at", report.generated_at),
            ("status", report.status),
            ("lang", report.lang),
        ]
        if report.scan is not None:
            items.append(("mode", report.scan.mode))
            items.append(("threshold", str(report.scan.threshold)))
            if report.scan.max_depth is not None:
                items.append(("max_depth", str(report.scan.max_depth)))
        return tuple(items)

    def _format_path_section(self, report: ScanReport, result: PathScanResult) -> FormattedPathSection:
        if report.scan is None:
            return FormattedPathSection(path=result.path, title="Results", entries=())
        if report.scan.mode == "responsibility_risk":
            entries = tuple(self._format_responsibility_result(item) for item in result.matched_files)
            return FormattedPathSection(
                path=result.path,
                title="High Risk Files",
                entries=entries,
                empty_message="未发现超过风险阈值的文件。",
            )
        if report.scan.mode == "dir_over_files":
            entries = tuple(self._format_directory_result(item) for item in result.matched_dirs)
            return FormattedPathSection(
                path=result.path,
                title="Matched Directories",
                entries=entries,
                empty_message="未发现目录内代码文件数超过阈值的目录。",
            )
        entries = tuple(self._format_line_result(item) for item in result.matched_files)
        empty_message = (
            "未发现超过阈值的文件。"
            if report.scan.mode == "over"
            else "未发现低于阈值的文件。"
        )
        return FormattedPathSection(
            path=result.path,
            title="Matched Files",
            entries=entries,
            empty_message=empty_message,
        )

    def _format_detail_result(
        self,
        result: LineFileMatch | DirectoryFileMatch | ResponsibilityRiskResult,
    ) -> FormattedEntry:
        if isinstance(result, ResponsibilityRiskResult):
            return self._format_responsibility_result(result)
        if isinstance(result, DirectoryFileMatch):
            return self._format_directory_result(result)
        return self._format_line_result(result)

    def _format_line_result(self, item: LineFileMatch | ResponsibilityRiskResult) -> FormattedEntry:
        line_match = item if isinstance(item, LineFileMatch) else LineFileMatch(
            path=item.path,
            lines=item.lines,
            priority=item.priority,
            summary=item.summary,
        )
        columns = [
            ("priority", line_match.priority or "-"),
            ("lines", str(line_match.lines)),
            ("path", line_match.path),
        ]
        return FormattedEntry(
            title=line_match.path,
            summary=line_match.summary,
            columns=tuple(columns),
        )

    def _format_directory_result(self, item: DirectoryFileMatch) -> FormattedEntry:
        return FormattedEntry(
            title=item.path,
            summary=item.summary,
            columns=(
                ("priority", item.priority or "-"),
                ("files", str(item.files)),
                ("path", item.path),
            ),
        )

    def _format_responsibility_result(self, item: ResponsibilityRiskResult) -> FormattedEntry:
        hotspots = tuple(self._format_hotspot(hotspot) for hotspot in (item.function_hotspots or []))
        anchors = tuple(self._format_anchor(anchor) for anchor in (item.anchors or []))
        move_sets = tuple(self._format_move_set(move_set) for move_set in (item.move_sets or []))
        return FormattedEntry(
            title=item.path,
            summary=item.summary,
            risks=tuple(item.dominant_risks or ()),
            suggestion=item.suggestion,
            next_action=item.next_action,
            evidence=self._responsibility_evidence(item),
            function_hotspots=hotspots,
            anchors=anchors,
            responsibility_clusters=self._responsibility_clusters(item, hotspots),
            extraction_candidates=self._extraction_candidates(item, hotspots),
            move_sets=move_sets,
            stop_signal=self._stop_signal(item, hotspots),
            validation_hints=self._validation_hints(item),
            false_positive_notes=self._false_positive_notes(item),
            columns=(
                ("priority", item.priority),
                ("score", str(item.score)),
                ("lines", str(item.lines)),
                ("path", item.path),
            ),
        )

    @staticmethod
    def _format_hotspot(item: ResponsibilityFunctionHotspot) -> FormattedHotspot:
        return FormattedHotspot(
            name=item.name,
            kind=item.kind,
            score=item.score,
            lines=f"{item.start_line}-{item.end_line}",
            summary=item.summary,
            risks=tuple(item.risks),
            evidence=tuple(item.evidence),
        )

    @staticmethod
    def _format_anchor(item: ResponsibilityAnchor) -> FormattedAnchor:
        return FormattedAnchor(
            line=item.line,
            label=item.label,
            issue=item.issue,
            evidence=item.evidence,
        )

    @staticmethod
    def _format_move_set(item: ResponsibilityMoveSet) -> FormattedMoveSet:
        return FormattedMoveSet(
            name=item.name,
            target_boundary=item.target_boundary,
            helpers=tuple(
                f"{helper.name} ({helper.start_line}-{helper.end_line})"
                for helper in item.helpers
            ),
            reason=item.reason,
            validation=item.validation,
        )

    def _responsibility_evidence(self, item: ResponsibilityRiskResult) -> str:
        metrics: list[str] = []
        top_level_label = "symbols" if self.lang in {"py", "cpp"} else "composables"
        metrics.append(f"{top_level_label} {item.top_level_composables}")
        metrics.append(f"state {item.state_signal_hits}")
        if item.mode_branch_hits > 0:
            metrics.append(f"mode branches {item.mode_branch_hits}")
        if item.role_kinds:
            metrics.append(f"roles {','.join(item.role_kinds)}")
        if self.lang == "py":
            if item.io_kind_count > 0:
                metrics.append(f"io {item.io_kind_count}")
            if item.rule_helper_count > 0:
                metrics.append(f"rules {item.rule_helper_count}")
            if item.responsibility_verb_kind_count > 0:
                metrics.append(f"verbs {item.responsibility_verb_kind_count}")
            if item.command_layer_leak_hits > 0:
                metrics.append(f"cmd leak {item.command_layer_leak_hits}")
        if self.lang == "cpp":
            if item.io_kind_count > 0:
                metrics.append(f"io {item.io_kind_count}")
            if item.rule_helper_count > 0:
                metrics.append(f"rules {item.rule_helper_count}")
            if item.responsibility_verb_kind_count > 0:
                metrics.append(f"verbs {item.responsibility_verb_kind_count}")
            if item.interop_surface_hits > 0:
                metrics.append(f"interop {item.interop_surface_hits}")
            if item.resource_lifecycle_hits > 0:
                metrics.append(f"lifecycle {item.resource_lifecycle_hits}")
        return " | ".join(metrics)

    def _responsibility_clusters(
        self,
        item: ResponsibilityRiskResult,
        hotspots: tuple[FormattedHotspot, ...],
    ) -> tuple[FormattedResponsibilityCluster, ...]:
        if self.lang == "cpp":
            return self._cpp_responsibility_clusters(item, hotspots)
        if self.lang == "py":
            return self._python_responsibility_clusters(item, hotspots)
        if self.lang != "kt":
            return ()
        clusters: dict[str, list[str]] = {}
        for hotspot in hotspots:
            cluster = self._kotlin_cluster_for_name(hotspot.name)
            clusters.setdefault(cluster, []).append(hotspot.name)
        if not clusters and item.role_kinds:
            clusters["ui_orchestration"] = list(item.role_kinds)
        result: list[FormattedResponsibilityCluster] = []
        for name, owners in clusters.items():
            result.append(
                FormattedResponsibilityCluster(
                    name=name,
                    owners=tuple(owners[:4]),
                    reason=self._cluster_reason(name),
                )
            )
        return tuple(result[:5])

    def _python_responsibility_clusters(
        self,
        item: ResponsibilityRiskResult,
        hotspots: tuple[FormattedHotspot, ...],
    ) -> tuple[FormattedResponsibilityCluster, ...]:
        clusters: dict[str, list[str]] = {}
        for hotspot in hotspots:
            cluster = self._python_cluster_for_hotspot(item.path, hotspot)
            clusters.setdefault(cluster, []).append(hotspot.name)
        if not clusters:
            normalized = item.path.replace("\\", "/").lower()
            if "/repo_tooling/commands/" in normalized and item.command_layer_leak_hits > 0:
                clusters["command_adapter_leak"] = ["command helpers and IO"]
            elif "/scripts/loc/internal/" in normalized and item.mode_branch_hits > 0:
                clusters["loc_report_mode_formatting"] = ["scan-mode branches"]
            elif item.io_kind_count > 0:
                clusters["tool_io_boundary"] = ["filesystem/process/env helpers"]
        result: list[FormattedResponsibilityCluster] = []
        for name, owners in clusters.items():
            result.append(
                FormattedResponsibilityCluster(
                    name=name,
                    owners=tuple(owners[:4]),
                    reason=self._python_cluster_reason(name),
                )
            )
        return tuple(result[:5])

    def _extraction_candidates(
        self,
        item: ResponsibilityRiskResult,
        hotspots: tuple[FormattedHotspot, ...],
    ) -> tuple[FormattedExtractionCandidate, ...]:
        if self.lang == "kt" and Path(item.path).name.endswith("Dialogs.kt") and item.lines <= 900:
            return ()
        if self.lang == "cpp" and self._is_test_path(item.path) and item.priority in {"P2", "P3"}:
            return ()
        candidate_limit = 3
        if self.lang == "cpp":
            max_hotspot_score = max((hotspot.score for hotspot in hotspots), default=0)
            if item.priority in {"P2", "P3"}:
                candidate_limit = 1 if max_hotspot_score <= 2 else 2
        candidates: list[FormattedExtractionCandidate] = []
        for hotspot in hotspots:
            if hotspot.score < 2:
                continue
            boundary = self._suggested_boundary(item.path, hotspot.name)
            candidates.append(
                FormattedExtractionCandidate(
                    name=hotspot.name,
                    lines=hotspot.lines,
                    suggested_boundary=boundary,
                    reason=self._candidate_reason(hotspot),
                    risk=self._candidate_risk(hotspot),
                    validation=self._candidate_validation(item.path),
                )
            )
            if len(candidates) >= candidate_limit:
                break
        return tuple(candidates)

    def _stop_signal(
        self,
        item: ResponsibilityRiskResult,
        hotspots: tuple[FormattedHotspot, ...],
    ) -> str | None:
        if self.lang == "cpp":
            max_hotspot_score = max((hotspot.score for hotspot in hotspots), default=0)
            if self._is_test_path(item.path) and item.priority in {"P2", "P3"}:
                return "pause: test files often centralize fixture helpers; split only when a test owner becomes hard to read."
            if item.priority == "P0" or max_hotspot_score >= 4:
                return "continue: choose one C++ extraction candidate with a clear owner boundary and keep behavior unchanged."
            if item.lines <= 650 and max_hotspot_score <= 2:
                return "pause: remaining C++ hotspots are modest; continue only for a named behavior change."
            return "review manually: split only where the candidate maps to an existing module or platform boundary."
        if self.lang != "kt":
            return None
        path_name = Path(item.path).name
        max_hotspot_score = max((hotspot.score for hotspot in hotspots), default=0)
        if path_name.endswith("Dialogs.kt") and item.lines <= 900:
            return "pause: dialog/import/export code is a coherent responsibility; avoid splitting only to reduce line count."
        if item.lines <= 750 and max_hotspot_score <= 2:
            return "pause: file is still flagged, but remaining hotspots are modest; continue only for a named behavior change."
        if path_name.endswith("State.kt") and item.lines <= 900 and max_hotspot_score <= 3:
            return "pause: state orchestration has already been narrowed; prefer moving to the next larger file."
        if item.lines >= 1200 or max_hotspot_score >= 3:
            return "continue: choose one extraction candidate, keep behavior unchanged, and validate immediately."
        return "review manually: only continue if a candidate has a clear file boundary and low dependency surface."

    def _validation_hints(self, item: ResponsibilityRiskResult) -> tuple[str, ...]:
        if self.lang == "kt":
            hints = ["android compileDebugKotlin or :app:compileDebugKotlin --rerun-tasks"]
            path_name = Path(item.path).name
            if "ConfigThemeAppearance" in path_name:
                hints.append("run ConfigThemeAppearanceSectionImportErrorTest when import/export helpers move")
                hints.append("run compileDebugUnitTestKotlin if internal test helpers move")
            if "Flash" in path_name or "Playback" in path_name:
                hints.append("for visual/playback changes, prefer debug device check or focused existing UI tests")
            return tuple(hints)
        if self.lang == "py":
            return ("run the focused unit tests for the moved helper module",)
        if self.lang == "cpp":
            path = item.path.replace("\\", "/")
            if "apps/audio_android/app/src/main/cpp" in path:
                return (
                    "python tools/run.py android assemble-debug",
                    "python tools/run.py android test-debug when JNI-facing state or DTO shape changes",
                )
            if "libs/audio_api" in path:
                return ("python tools/run.py test-lib audio_api --build-dir build/dev",)
            if "libs/audio_core" in path:
                return (
                    "python tools/run.py test-lib audio_core --build-dir build/dev",
                    "python tools/run.py test-lib audio_api --build-dir build/dev when C ABI behavior is affected",
                )
            if "libs/audio_io" in path:
                return ("python tools/run.py test-lib audio_io --build-dir build/dev",)
            return ("python tools/run.py verify --build-dir build/dev --skip-android",)
        return ()

    def _false_positive_notes(self, item: ResponsibilityRiskResult) -> tuple[str, ...]:
        notes: list[str] = []
        if self.lang == "kt":
            if item.state_signal_hits > 0:
                notes.append("Compose files naturally contain remember/LaunchedEffect; treat this as risk only when state mixes with unrelated UI or import/export work.")
            if item.mode_branch_hits > 0:
                notes.append("Mode/style branches are expected in router components; extract only repeated policy or large per-mode bodies.")
            if item.lines < 900 and len(item.role_kinds) <= 1:
                notes.append("Line count alone is not a reason to split if the file now has one clear responsibility.")
        if self.lang == "cpp":
            normalized_path = item.path.replace("\\", "/")
            if "libs/audio_core/src/" in normalized_path and not self._is_test_path(item.path):
                notes.append("audio_core host 主线是 module-first；新增职责边界优先使用 module interface + module implementation unit，不要把新边界做成 .inc。")
                notes.append("同一 owner 下的 Normalize/Build/Decode helpers 优先成组迁移到一个 named module，避免按单个小函数碎拆。")
            if self._is_test_path(item.path):
                notes.append("C++ tests often contain compact fixture/build/assert helpers; treat P2 test findings as review prompts, not automatic extraction work.")
            if item.interop_surface_hits > 0:
                notes.append("Interop is expected in JNI/C ABI boundary files; split only when marshalling, lifecycle, and domain rules are growing in the same file.")
            if item.mode_branch_hits > 0:
                notes.append("Mode branches are expected in transport facades; prefer extracting repeated per-mode bodies over hiding a clear dispatcher.")
            if item.lines < 650 and not item.function_hotspots:
                notes.append("Small C++ implementation files can stay intact when no function-level hotspot is found.")
        return tuple(notes)

    @staticmethod
    def _is_test_path(path: str) -> bool:
        normalized = path.replace("\\", "/").lower()
        return "/test/" in normalized or normalized.endswith("/test") or "/tests/" in normalized

    def _cpp_responsibility_clusters(
        self,
        item: ResponsibilityRiskResult,
        hotspots: tuple[FormattedHotspot, ...],
    ) -> tuple[FormattedResponsibilityCluster, ...]:
        clusters: dict[str, list[str]] = {}
        for hotspot in hotspots:
            cluster = self._cpp_cluster_for_hotspot(item.path, hotspot)
            clusters.setdefault(cluster, []).append(hotspot.name)
        if not clusters:
            if item.interop_surface_hits > 0:
                clusters["interop_marshalling"] = ["JNI/C ABI surface"]
            elif item.mode_branch_hits > 0:
                clusters["mode_dispatch"] = ["mode/style/state branches"]
            elif item.rule_helper_count > 0:
                clusters["domain_rules"] = ["rule helpers"]
        result: list[FormattedResponsibilityCluster] = []
        for name, owners in clusters.items():
            result.append(
                FormattedResponsibilityCluster(
                    name=name,
                    owners=tuple(owners[:4]),
                    reason=self._cpp_cluster_reason(name),
                )
            )
        return tuple(result[:5])

    @staticmethod
    def _cpp_cluster_for_hotspot(path: str, hotspot: FormattedHotspot) -> str:
        lower_name = hotspot.name.lower()
        lower_path = path.replace("\\", "/").lower()
        if "jni" in lower_path or "interop_surface_breadth" in hotspot.risks:
            if "list" in lower_name or "entry" in lower_name or "viewdata" in lower_name:
                return "jni_dto_marshalling"
            return "interop_marshalling"
        if "pump" in lower_name or "run" == lower_name or "cancel" in lower_name or "take" in lower_name:
            return "operation_lifecycle"
        if "prepare" in lower_name or "render" in lower_name or "postprocess" in lower_name or "finalize" in lower_name:
            return "operation_stage_steps"
        if "validate" in lower_name or "config" in lower_name or "morse" in lower_name:
            return "domain_rules"
        if "decode" in lower_name:
            return "decode_flow"
        if "encode" in lower_name or "build" in lower_name:
            return "encode_flow"
        if "mode_branching" in hotspot.risks:
            return "mode_dispatch"
        return "cpp_helpers"

    @staticmethod
    def _cpp_cluster_reason(name: str) -> str:
        return {
            "jni_dto_marshalling": "JNI DTO constructors and list builders can usually move behind a focused marshalling boundary",
            "interop_marshalling": "platform/C ABI glue should stay thin and delegate conversion rules",
            "operation_lifecycle": "operation state, cancellation, result taking, and terminal transitions form a coherent lifecycle owner",
            "operation_stage_steps": "prepare/render/postprocess/finalize steps can be extracted behind the operation engine without changing public API",
            "domain_rules": "validation, config normalization, and mode rules are easier to test when separated from lifecycle code",
            "decode_flow": "decoder buffering and decode result assembly are separate from encode/render responsibilities",
            "encode_flow": "encode/build helpers can be isolated from bridge or lifecycle orchestration",
            "mode_dispatch": "mode routing should stay as a thin dispatcher while per-mode bodies live near their domain",
            "cpp_helpers": "helper-heavy regions should be moved only when they have a named owner and focused validation",
        }.get(name, "clustered by C++ hotspot names and risk evidence")

    @staticmethod
    def _kotlin_cluster_for_name(name: str) -> str:
        lower = name.lower()
        if "dialog" in lower or "import" in lower or "export" in lower:
            return "dialog_import_export"
        if "canvas" in lower or "draw" in lower or "overlay" in lower:
            return "canvas_visual_runtime"
        if "state" in lower or lower.startswith("remember"):
            return "state_orchestration"
        if "palette" in lower or "theme" in lower or "brand" in lower:
            return "theme_palette_ui"
        if "annotation" in lower or "token" in lower or "follow" in lower:
            return "follow_annotation_ui"
        return "ui_orchestration"

    @staticmethod
    def _cluster_reason(name: str) -> str:
        return {
            "dialog_import_export": "dialog visibility, parsing, duplicate handling, and clipboard side effects can often live behind one dialog boundary",
            "canvas_visual_runtime": "Canvas rendering and overlay/runtime state should stay separate from screen-level routing",
            "state_orchestration": "remember/effect state can be isolated behind a focused state holder",
            "theme_palette_ui": "theme grouping and palette row rendering are usually separate responsibilities",
            "follow_annotation_ui": "follow/token annotation windowing can often be tested as model helpers",
            "ui_orchestration": "large UI orchestration should be split only along a named user-facing subsection",
        }.get(name, "clustered by hotspot names and risk evidence")

    @staticmethod
    def _python_cluster_for_hotspot(path: str, hotspot: FormattedHotspot) -> str:
        normalized = path.replace("\\", "/").lower()
        lower = hotspot.name.lower()
        if "/repo_tooling/commands/" in normalized:
            if "command_layer_leak" in hotspot.risks or lower.startswith("cmd_"):
                return "command_adapter_leak"
            if lower.startswith(("candidate_", "resolve_", "require_", "expected_", "cache_", "reset_")):
                return "toolchain_resolution"
            if lower.startswith(("run_", "serve_", "prepare_")) or "io_surface_breadth" in hotspot.risks:
                return "execution_boundary"
            return "command_rules"
        if "/repo_tooling/android_debug/" in normalized:
            if lower.startswith(("run_adb", "dump_", "start_", "ensure_", "capture_")):
                return "android_device_io"
            return "android_debug_rules"
        if "/scripts/loc/internal/" in normalized:
            if "formatter" in normalized:
                return "loc_report_mode_formatting"
            return "loc_scan_rules"
        if "io_surface_breadth" in hotspot.risks:
            return "tool_io_boundary"
        if "rule_helper_density" in hotspot.risks:
            return "pure_rule_helpers"
        return "python_helpers"

    @staticmethod
    def _python_cluster_reason(name: str) -> str:
        return {
            "command_adapter_leak": "command modules should remain thin CLI adapters and delegate rules, paths, and IO execution",
            "toolchain_resolution": "tool discovery, env lookup, cache paths, and stale-build checks form one testable toolchain boundary",
            "execution_boundary": "subprocess, local server, and test orchestration should sit behind an explicit IO boundary",
            "command_rules": "pure rules should move out of commands before changing behavior",
            "android_device_io": "ADB/device/logcat side effects should be mockable and separate from parsing/reporting",
            "android_debug_rules": "debug parsing and report rules are easier to test without a connected device",
            "loc_report_mode_formatting": "LOC report formatting should split by scan mode or rendered section, not isolated tiny helpers",
            "loc_scan_rules": "scan rules should stay separate from report writing and CLI dispatch",
            "tool_io_boundary": "filesystem/process/env operations need a named boundary for reliable tests",
            "pure_rule_helpers": "pure helper packs should move together and gain focused unit coverage",
            "python_helpers": "helper-heavy regions should move only when they have a named owner and focused validation",
        }.get(name, "clustered by Python hotspot names and risk evidence")

    @staticmethod
    def _candidate_reason(hotspot: FormattedHotspot) -> str:
        evidence = ", ".join(hotspot.evidence)
        return f"{hotspot.summary}; evidence: {evidence}" if evidence else hotspot.summary

    @staticmethod
    def _candidate_risk(hotspot: FormattedHotspot) -> str:
        if hotspot.kind == "composable" and "stateful_side_effects" in hotspot.risks:
            return "medium: preserve remember keys and state ownership while moving"
        if "mode_branching" in hotspot.risks:
            return "medium: keep branch behavior identical and avoid broad rewrites"
        return "low: prefer pure move/extract with no behavior change"

    @staticmethod
    def _candidate_validation(path: str) -> str:
        normalized_path = path.replace("\\", "/")
        if "tools/repo_tooling/commands/web.py" in normalized_path or "tools/repo_tooling/web/" in normalized_path:
            return "python -m unittest tools.tests.test_web_tools; python tools/run.py web test"
        if "tools/repo_tooling/android_debug/" in normalized_path:
            return "python -m unittest tools.tests.test_android_debug"
        if "tools/scripts/loc/" in normalized_path:
            return "python tools/scripts/loc/run.py --lang py --responsibility-risk"
        if "apps/audio_android/app/src/main/cpp" in normalized_path:
            return "python tools/run.py android assemble-debug"
        if "libs/audio_api" in normalized_path:
            return "python tools/run.py test-lib audio_api --build-dir build/dev"
        if "libs/audio_core" in normalized_path:
            return "python tools/run.py test-lib audio_core --build-dir build/dev"
        if "libs/audio_io" in normalized_path:
            return "python tools/run.py test-lib audio_io --build-dir build/dev"
        path_name = Path(path).name
        if "ConfigThemeAppearance" in path_name:
            return "compileDebugKotlin; if import/export moved, run ConfigThemeAppearanceSectionImportErrorTest"
        if "Flash" in path_name:
            return "compileDebugKotlin; verify Flash visual playback path if canvas/runtime code moved"
        if "Playback" in path_name:
            return "compileDebugKotlin; run compileDebugUnitTestKotlin after test-facing helper moves"
        return "compile the owning module and run focused tests if helpers are test-visible"

    @staticmethod
    def _suggested_boundary(path: str, hotspot_name: str) -> str:
        suffix = Path(path).suffix
        if suffix in {".cpp", ".cc", ".cxx", ".inc", ".h", ".hpp", ".cppm"}:
            return ReportFormatter._suggested_cpp_boundary(path, hotspot_name)
        if suffix == ".py":
            return ReportFormatter._suggested_python_boundary(path, hotspot_name)
        stem = Path(path).stem
        lower = hotspot_name.lower()
        if "dialog" in lower or "import" in lower or "export" in lower:
            return f"{stem}Dialogs.kt or {stem}ImportExport.kt"
        if "canvas" in lower:
            return f"{stem}Canvas.kt or {stem}CanvasState.kt"
        if "visualstate" in lower or "scenestate" in lower or hotspot_name.startswith("remember"):
            cleaned = hotspot_name.removeprefix("remember")
            return f"{cleaned}.kt"
        if "palette" in lower:
            return f"{stem}PaletteGroups.kt"
        if "brand" in lower or "theme" in lower:
            return f"{stem}ThemeGroups.kt"
        if "annotation" in lower or "token" in lower:
            return f"{stem}Model.kt or {stem}Rows.kt"
        return f"{stem}{hotspot_name}.kt"

    @staticmethod
    def _suggested_python_boundary(path: str, hotspot_name: str) -> str:
        normalized = path.replace("\\", "/").lower()
        lower = hotspot_name.lower()
        stem = Path(path).stem
        if "/repo_tooling/commands/" in normalized:
            domain = stem
            if lower.startswith(("candidate_", "resolve_", "require_", "expected_", "cache_", "reset_")):
                return f"repo_tooling/{domain}/toolchain.py or repo_tooling/{domain}/paths.py"
            if lower.startswith(("run_", "serve_", "prepare_")):
                return f"repo_tooling/{domain}/build.py, repo_tooling/{domain}/server.py, or repo_tooling/{domain}/tests.py"
            if lower.startswith(("build_", "read_", "values_", "export_")):
                return f"repo_tooling/{domain}/rules.py or repo_tooling/{domain}/sample_texts.py"
            return f"repo_tooling/{domain}/<focused_boundary>.py"
        if "/repo_tooling/android_debug/" in normalized:
            if lower.startswith(("run_adb", "dump_", "start_", "ensure_", "capture_")):
                return "repo_tooling/android_debug/device_io.py"
            return "repo_tooling/android_debug/reporting.py or repo_tooling/android_debug/<focused_rules>.py"
        if "/scripts/loc/internal/" in normalized:
            if "formatter" in normalized:
                return "scripts/loc/internal/report_formatters/<scan_mode>.py"
            return "scripts/loc/internal/<focused_boundary>.py"
        return f"{stem}_{hotspot_name}.py"

    @staticmethod
    def _suggested_cpp_boundary(path: str, hotspot_name: str) -> str:
        stem = Path(path).stem
        lower_path = path.replace("\\", "/").lower()
        lower_name = hotspot_name.lower()
        if "jni_bridge_helpers" in lower_path:
            if "list" in lower_name:
                return "jni_bridge_list_marshalling.cpp/.h"
            if "follow" in lower_name or "viewdata" in lower_name or "entry" in lower_name:
                return "jni_bridge_follow_marshalling.cpp/.h"
            return "jni_bridge_marshalling.cpp/.h"
        if "transport" in lower_path and ("transport_impl" in lower_path or "encode_operation" in lower_path):
            if "pump" in lower_name or "run" == lower_name or "cancel" in lower_name or "take" in lower_name:
                return "module impl bag.transport.facade -> src/transport/transport_encode_operation.cpp"
            if "prepare" in lower_name or "render" in lower_name or "postprocess" in lower_name or "finalize" in lower_name:
                return "module bag.transport.encode_operation_steps -> modules/bag/transport/encode_operation_steps.cppm + src/transport/encode_operation_steps.cpp"
            if "validate" in lower_name:
                return "module impl bag.transport.facade -> src/transport/transport_validation.cpp"
            if "workplan" in lower_name or "work" in lower_name:
                return "module bag.transport.encode_work_plan -> modules/bag/transport/encode_work_plan.cppm + src/transport/encode_work_plan.cpp"
            return "module impl bag.transport.facade -> src/transport/transport_encode_operation.cpp"
        if "flash" in lower_path and "phy_clean" in lower_path:
            if "normalize" in lower_name or "formal" in lower_name or "budget" in lower_name or "decode" in lower_name:
                return "module bag.flash.phy_rules -> modules/bag/flash/phy_rules.cppm + src/flash/phy_rules.cpp"
            if "decoder" in lower_name or "poll" in lower_name:
                return "module bag.flash.phy_decode -> modules/bag/flash/phy_decode.cppm + src/flash/phy_decode.cpp"
            return "module impl bag.flash.phy_clean -> src/flash/phy_clean.cpp"
        if "flash" in lower_path and "signal" in lower_path:
            if "layout" in lower_name or "payload" in lower_name:
                return "module bag.flash.signal_layout -> modules/bag/flash/signal_layout.cppm + src/flash/signal_layout.cpp"
            if "decode" in lower_name:
                return "module bag.flash.signal_decode -> modules/bag/flash/signal_decode.cppm + src/flash/signal_decode.cpp"
            if "normalize" in lower_name or "profile" in lower_name or "flavor" in lower_name:
                return "module bag.flash.signal_rules -> modules/bag/flash/signal_rules.cppm + src/flash/signal_rules.cpp"
            return "module impl bag.flash.signal -> src/flash/signal.cpp"
        if "pro" in lower_path and "phy_clean" in lower_path:
            if "decoder" in lower_name or "poll" in lower_name or "decode" in lower_name:
                return "module bag.pro.phy_decode -> modules/bag/pro/phy_decode.cppm + src/pro/phy_decode.cpp"
            if "encode" in lower_name or "render" in lower_name or "symbols" in lower_name:
                return "module bag.pro.phy_encode -> modules/bag/pro/phy_encode.cppm + src/pro/phy_encode.cpp"
            return "module bag.pro.phy_rules -> modules/bag/pro/phy_rules.cppm + src/pro/phy_rules.cpp"
        if "ultra" in lower_path and "phy_clean" in lower_path:
            if "decoder" in lower_name or "poll" in lower_name or "decode" in lower_name:
                return "module bag.ultra.phy_decode -> modules/bag/ultra/phy_decode.cppm + src/ultra/phy_decode.cpp"
            if "encode" in lower_name or "render" in lower_name or "symbols" in lower_name:
                return "module bag.ultra.phy_encode -> modules/bag/ultra/phy_encode.cppm + src/ultra/phy_encode.cpp"
            return "module bag.ultra.phy_rules -> modules/bag/ultra/phy_rules.cppm + src/ultra/phy_rules.cpp"
        if "mini" in lower_path and "phy_clean" in lower_path:
            if "decoder" in lower_name or "poll" in lower_name or "decode" in lower_name:
                return "module bag.mini.phy_decode -> modules/bag/mini/phy_decode.cppm + src/mini/phy_decode.cpp"
            if "renderer" in lower_name or "render" in lower_name or "encodepayload" in lower_name:
                return "module bag.mini.tone_renderer -> modules/bag/mini/tone_renderer.cppm + src/mini/tone_renderer.cpp"
            return "module bag.mini.morse_rules -> modules/bag/mini/morse_rules.cppm + src/mini/morse_rules.cpp"
        return f"module impl near {stem} -> src/.../{stem}_{hotspot_name}.cpp"
