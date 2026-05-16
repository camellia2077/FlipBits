from __future__ import annotations

from dataclasses import dataclass

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
        return FormattedEntry(
            title=item.path,
            summary=item.summary,
            risks=tuple(item.dominant_risks or ()),
            suggestion=item.suggestion,
            next_action=item.next_action,
            evidence=self._responsibility_evidence(item),
            function_hotspots=tuple(self._format_hotspot(hotspot) for hotspot in (item.function_hotspots or [])),
            anchors=tuple(self._format_anchor(anchor) for anchor in (item.anchors or [])),
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
