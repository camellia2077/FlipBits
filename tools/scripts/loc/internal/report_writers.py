from __future__ import annotations

import json
from pathlib import Path

from .report_formatter import (
    FormattedAnchor,
    FormattedEntry,
    FormattedHotspot,
    FormattedPathSection,
    FormattedScanReport,
    ReportFormatter,
)
from .report_models import DetailReport, ScanReport


class JsonReportWriter:
    @staticmethod
    def write_scan_report(path: Path, report: ScanReport) -> None:
        path.write_text(
            json.dumps(report.to_dict(), indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )

    @staticmethod
    def write_detail_report(path: Path, report: DetailReport) -> None:
        path.write_text(
            json.dumps(report.to_dict(), indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )


class MarkdownReportWriter:
    def __init__(self, formatter: ReportFormatter):
        self.formatter = formatter

    def write_scan_report(self, path: Path, report: ScanReport) -> None:
        formatted = self.formatter.format_scan_report(report)
        path.write_text(self._render_scan_report(formatted), encoding="utf-8")

    def write_detail_report(self, path: Path, report: DetailReport) -> None:
        formatted = self.formatter.format_detail_report(report)
        path.write_text(self._render_detail_report(report, formatted), encoding="utf-8")

    def _render_scan_report(self, report: FormattedScanReport) -> str:
        lines = [f"# {report.heading}", ""]
        lines.extend(self._render_metadata(report.metadata))
        if report.summary:
            lines.extend(["", "## Summary", ""])
            lines.extend(self._render_summary_table(report.summary))
        if report.path_sections:
            for section in report.path_sections:
                lines.extend(["", f"## {section.title}", "", f"### `{section.path}`", ""])
                if section.entries:
                    lines.extend(self._render_entries_table(section.entries))
                    for entry in section.entries:
                        lines.extend(["", self._render_agent_notes(entry)])
                elif section.empty_message:
                    lines.append(section.empty_message)
        if report.error:
            lines.extend(["", "## Error", "", report.error])
        lines.append("")
        return "\n".join(lines)

    def _render_detail_report(self, report: DetailReport, entry: FormattedEntry) -> str:
        lines = [
            "# LOC Detail Report",
            "",
            "## Metadata",
            "",
        ]
        metadata = (
            ("generated_at", report.generated_at),
            ("status", report.status),
            ("lang", report.lang),
            ("mode", report.scan.mode),
            ("threshold", str(report.scan.threshold)),
        )
        lines.extend(self._render_metadata(metadata))
        lines.extend(["", "## Result", ""])
        lines.extend(self._render_entries_table((entry,)))
        lines.extend(["", self._render_agent_notes(entry), ""])
        return "\n".join(lines)

    @staticmethod
    def _render_metadata(metadata: tuple[tuple[str, str], ...]) -> list[str]:
        lines = ["| Key | Value |", "| --- | --- |"]
        for key, value in metadata:
            lines.append(f"| {key} | `{value}` |")
        return lines

    @staticmethod
    def _render_summary_table(summary: tuple[tuple[str, str], ...]) -> list[str]:
        lines = ["| Metric | Value |", "| --- | --- |"]
        for key, value in summary:
            lines.append(f"| {key} | `{value}` |")
        return lines

    def _render_entries_table(self, entries: tuple[FormattedEntry, ...]) -> list[str]:
        all_keys: list[str] = []
        for entry in entries:
            for key, _ in entry.columns:
                if key not in all_keys:
                    all_keys.append(key)
        header = "| " + " | ".join(key.title().replace("_", " ") for key in all_keys) + " |"
        divider = "| " + " | ".join("---" for _ in all_keys) + " |"
        lines = [header, divider]
        for entry in entries:
            value_by_key = {key: value for key, value in entry.columns}
            row = "| " + " | ".join(f"`{value_by_key.get(key, '-')}`" for key in all_keys) + " |"
            lines.append(row)
        return lines

    @staticmethod
    def _render_agent_notes(entry: FormattedEntry) -> str:
        lines = [f"#### Agent View: `{entry.title}`", ""]
        if entry.summary:
            lines.append(f"- summary: {entry.summary}")
        if entry.risks:
            lines.append(f"- dominant_risks: {', '.join(entry.risks)}")
        if entry.suggestion:
            lines.append(f"- suggestion: {entry.suggestion}")
        if entry.next_action:
            lines.append(f"- next_action: {entry.next_action}")
        if entry.evidence:
            lines.append(f"- evidence: {entry.evidence}")
        if entry.function_hotspots:
            lines.extend(["", "##### Function Hotspots", ""])
            lines.extend(MarkdownReportWriter._render_hotspots_table(entry.function_hotspots))
        if entry.anchors:
            lines.extend(["", "##### Anchors", ""])
            lines.extend(MarkdownReportWriter._render_anchors_table(entry.anchors))
        return "\n".join(lines)

    @staticmethod
    def _render_hotspots_table(items: tuple[FormattedHotspot, ...]) -> list[str]:
        lines = [
            "| Name | Kind | Score | Lines | Summary | Risks | Evidence |",
            "| --- | --- | --- | --- | --- | --- | --- |",
        ]
        for item in items:
            lines.append(
                "| "
                + " | ".join(
                    [
                        f"`{item.name}`",
                        f"`{item.kind}`",
                        f"`{item.score}`",
                        f"`{item.lines}`",
                        item.summary,
                        ", ".join(item.risks) if item.risks else "-",
                        " | ".join(item.evidence) if item.evidence else "-",
                    ]
                )
                + " |"
            )
        return lines

    @staticmethod
    def _render_anchors_table(items: tuple[FormattedAnchor, ...]) -> list[str]:
        lines = [
            "| Line | Owner | Issue | Evidence |",
            "| --- | --- | --- | --- |",
        ]
        for item in items:
            lines.append(
                f"| `{item.line}` | `{item.label}` | {item.issue} | `{item.evidence}` |"
            )
        return lines
