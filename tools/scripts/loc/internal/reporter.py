from pathlib import Path

from .report_formatter import FormattedPathSection, FormattedScanReport, ReportFormatter
from .report_models import ScanReport


class LocConsoleReporter:
    def __init__(self, formatter: ReportFormatter, detail_md_by_path: dict[str, str] | None = None):
        self.formatter = formatter
        self.detail_md_by_path = detail_md_by_path or {}

    def print_scan_report(self, report: ScanReport) -> None:
        formatted = self.formatter.format_scan_report(report)
        print(f"{'=' * 100}")
        print(formatted.heading)
        if report.scan and report.scan.mode == "responsibility_risk":
            print("说明: 这是文件级启发式风险预警，不代表最终设计结论。")
        if report.scan and report.scan.mode == "dir_over_files" and report.scan.max_depth is not None:
            print(f"扫描层级: <= {report.scan.max_depth}")
        elif report.scan and report.scan.mode == "dir_over_files":
            print("扫描层级: 不限制")
        print(f"{'=' * 100}\n")
        for section in formatted.path_sections:
            self._print_path_section(section)
        if formatted.error:
            print(f"[ERROR] {formatted.error}")

    def _print_path_section(self, section: FormattedPathSection) -> None:
        project_name = Path(section.path).name or section.path
        print(f"[SCAN] 正在扫描项目: [{project_name}]")
        print(f"  路径: {section.path}")
        if section.entries:
            print(f"  找到 {len(section.entries)} 个匹配项：")
            grouped = LocConsoleReporter._group_entries_by_priority(section)
            for priority in ("P0", "P1", "P2", "P3", "-"):
                entries = grouped.get(priority)
                if not entries:
                    continue
                print(f"  [{priority}]")
                for entry in entries:
                    line = " | ".join(f"{key} {value}" for key, value in entry.columns if key in {"score", "lines", "files"})
                    path_value = next((value for key, value in entry.columns if key == "path"), entry.title)
                    print(f'    {line} | File "{path_value}"' if line else f'    File "{path_value}"')
                    detail_parts: list[str] = []
                    if entry.summary:
                        detail_parts.append(entry.summary)
                    if entry.risks:
                        detail_parts.append(f"risks: {', '.join(entry.risks)}")
                    if detail_parts:
                        print(f"         -> {' | '.join(detail_parts)}")
                    detail_md_path = self.detail_md_by_path.get(path_value)
                    if detail_md_path:
                        print(f"         md: {detail_md_path}")
                    print()
        elif section.empty_message:
            print(f"  [OK] 扫描完毕，{section.empty_message}")
        print(f"\n{'-' * 100}\n")

    @staticmethod
    def _group_entries_by_priority(section: FormattedPathSection) -> dict[str, list]:
        grouped: dict[str, list] = {}
        for entry in section.entries:
            priority = next((value for key, value in entry.columns if key == "priority"), "-")
            grouped.setdefault(priority, []).append(entry)
        return grouped

    @staticmethod
    def print_missing_path(path: Path) -> None:
        print(f"[SCAN] 正在扫描项目: [{path.name}]")
        print(f"  路径: {path}")
        print("  [ERROR] 路径不存在，跳过扫描。")
        print(f"\n{'-' * 100}\n")
