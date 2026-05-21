from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from .report_models import DetailReport, DirectoryFileMatch, LineFileMatch, PathScanResult, ScanReport
from .responsibility_analyzers import ResponsibilityRiskResult
from .responsibility_metrics import ResponsibilityAnchor, ResponsibilityFunctionHotspot


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

    def _extraction_candidates(
        self,
        item: ResponsibilityRiskResult,
        hotspots: tuple[FormattedHotspot, ...],
    ) -> tuple[FormattedExtractionCandidate, ...]:
        if self.lang == "kt" and Path(item.path).name.endswith("Dialogs.kt") and item.lines <= 900:
            return ()
        candidates: list[FormattedExtractionCandidate] = []
        for hotspot in hotspots[:3]:
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
        return tuple(candidates)

    def _stop_signal(
        self,
        item: ResponsibilityRiskResult,
        hotspots: tuple[FormattedHotspot, ...],
    ) -> str | None:
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
            return ("run the focused native build/test for the touched module",)
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
        return tuple(notes)

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
