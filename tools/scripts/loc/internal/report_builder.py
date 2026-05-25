from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

from .report_models import (
    DetailReport,
    DirectoryFileMatch,
    LineFileMatch,
    OutputArtifact,
    PathScanResult,
    ScanReport,
    ScanSpec,
)
from .responsibility_analyzers import ResponsibilityRiskResult
from .service import LocScanService


@dataclass(frozen=True)
class ScanBuildResult:
    report: ScanReport
    artifacts: tuple[OutputArtifact, ...]


class ReportBuilder:
    def __init__(self, *, lang: str, scan_service: LocScanService):
        self.lang = lang
        self.scan_service = scan_service

    def build_line_scan(
        self,
        *,
        generated_at: str,
        paths: list[Path],
        mode: str,
        threshold: int,
    ) -> ScanBuildResult:
        path_results: list[PathScanResult] = []
        artifacts: list[OutputArtifact] = []
        total_matched_files = 0
        for path in paths:
            if not path.exists():
                path_results.append(PathScanResult(path=str(path)))
                continue
            matched = self.scan_service.analyze_path(path, mode, threshold)
            matched_files = tuple(LineFileMatch(path=file_path, lines=lines) for file_path, lines in matched)
            artifacts.extend(self._build_line_artifacts(path, matched, mode, threshold))
            total_matched_files += len(matched_files)
            path_results.append(PathScanResult(path=str(path), matched_files=matched_files))
        report = ScanReport(
            generated_at=generated_at,
            status="ok",
            lang=self.lang,
            scan=ScanSpec(mode=mode, threshold=threshold),
            results=tuple(path_results),
            summary={"matched_files": total_matched_files},
        )
        return ScanBuildResult(report=report, artifacts=tuple(artifacts))

    def build_dir_scan(
        self,
        *,
        generated_at: str,
        paths: list[Path],
        threshold: int,
        max_depth: int | None,
    ) -> ScanBuildResult:
        path_results: list[PathScanResult] = []
        artifacts: list[OutputArtifact] = []
        total_matched_dirs = 0
        for path in paths:
            if not path.exists():
                path_results.append(PathScanResult(path=str(path)))
                continue
            matched = self.scan_service.analyze_directory_file_counts(path, threshold=threshold, max_depth=max_depth)
            matched_dirs = tuple(DirectoryFileMatch(path=dir_path, files=file_count) for dir_path, file_count in matched)
            artifacts.extend(self._build_dir_artifacts(path, matched, threshold))
            total_matched_dirs += len(matched_dirs)
            path_results.append(PathScanResult(path=str(path), matched_dirs=matched_dirs))
        report = ScanReport(
            generated_at=generated_at,
            status="ok",
            lang=self.lang,
            scan=ScanSpec(mode="dir_over_files", threshold=threshold, max_depth=max_depth),
            results=tuple(path_results),
            summary={"matched_dirs": total_matched_dirs},
        )
        return ScanBuildResult(report=report, artifacts=tuple(artifacts))

    def build_responsibility_scan(
        self,
        *,
        generated_at: str,
        paths: list[Path],
        threshold: int,
    ) -> ScanBuildResult:
        path_results: list[PathScanResult] = []
        artifacts: list[OutputArtifact] = []
        total_matched_files = 0
        for path in paths:
            if not path.exists():
                path_results.append(PathScanResult(path=str(path)))
                continue
            matched = self.scan_service.analyze_responsibility_risk(path, threshold)
            artifacts.extend(self._build_responsibility_artifacts(path, matched, threshold))
            total_matched_files += len(matched)
            path_results.append(PathScanResult(path=str(path), matched_files=tuple(matched)))
        report = ScanReport(
            generated_at=generated_at,
            status="ok",
            lang=self.lang,
            scan=ScanSpec(mode="responsibility_risk", threshold=threshold),
            results=tuple(path_results),
            summary={"matched_files": total_matched_files},
        )
        return ScanBuildResult(report=report, artifacts=tuple(artifacts))

    def build_error_report(self, *, generated_at: str, message: str) -> ScanReport:
        return ScanReport(
            generated_at=generated_at,
            status="error",
            lang=self.lang,
            error=message,
        )

    def _build_line_artifacts(
        self,
        root_path: Path,
        matched: list[tuple[str, int]],
        mode: str,
        threshold: int,
    ) -> list[OutputArtifact]:
        artifacts: list[OutputArtifact] = []
        for file_path, lines in matched:
            priority, summary = (
                self.scan_service.classify_split_priority(lines, threshold)
                if mode == "over"
                else self.scan_service.classify_small_file_priority(lines, threshold)
            )
            detail_report = DetailReport(
                generated_at=self._now(),
                status="ok",
                lang=self.lang,
                scan=ScanSpec(mode=mode, threshold=threshold),
                result=LineFileMatch(path=file_path, lines=lines, priority=priority, summary=summary),
            )
            artifacts.append(
                OutputArtifact(
                    relative_output_path=self._build_relative_output_path(
                        root_path=root_path,
                        target_path=Path(file_path),
                        priority=priority,
                    ),
                    report=detail_report,
                )
            )
        return artifacts

    def _build_dir_artifacts(
        self,
        root_path: Path,
        matched: list[tuple[str, int]],
        threshold: int,
    ) -> list[OutputArtifact]:
        artifacts: list[OutputArtifact] = []
        for dir_path, file_count in matched:
            priority, summary = self.scan_service.classify_dir_priority(file_count, threshold)
            detail_report = DetailReport(
                generated_at=self._now(),
                status="ok",
                lang=self.lang,
                scan=ScanSpec(mode="dir_over_files", threshold=threshold),
                result=DirectoryFileMatch(path=dir_path, files=file_count, priority=priority, summary=summary),
            )
            artifacts.append(
                OutputArtifact(
                    relative_output_path=self._build_relative_output_path(
                        root_path=root_path,
                        target_path=Path(dir_path),
                        priority=priority,
                        is_directory=True,
                    ),
                    report=detail_report,
                )
            )
        return artifacts

    def _build_responsibility_artifacts(
        self,
        root_path: Path,
        matched: list[ResponsibilityRiskResult],
        threshold: int,
    ) -> list[OutputArtifact]:
        artifacts: list[OutputArtifact] = []
        for item in matched:
            detail_report = DetailReport(
                generated_at=self._now(),
                status="ok",
                lang=self.lang,
                scan=ScanSpec(mode="responsibility_risk", threshold=threshold),
                result=item,
            )
            artifacts.append(
                OutputArtifact(
                    relative_output_path=self._build_relative_output_path(
                        root_path=root_path,
                        target_path=Path(item.path),
                        priority=item.priority,
                    ),
                    report=detail_report,
                )
            )
        return artifacts

    def _build_relative_output_path(
        self,
        *,
        root_path: Path,
        target_path: Path,
        priority: str,
        is_directory: bool = False,
    ) -> Path:
        root_folder = self._root_folder_name(root_path)
        file_stem = self._detail_file_stem(root_path=root_path, target_path=target_path)
        file_name = "_dir_scan.json" if is_directory else f"{file_stem}_scan.json"
        return Path(root_folder) / priority / file_name

    def _detail_file_stem(self, *, root_path: Path, target_path: Path) -> str:
        try:
            relative_target = target_path.resolve().relative_to(root_path.resolve())
        except ValueError:
            relative_target = target_path
        parts = list(relative_target.parts)
        if not parts:
            return self._safe_segment(target_path.stem)
        parts[-1] = Path(parts[-1]).stem
        meaningful_parts = [self._safe_segment(part) for part in parts if part not in {"", "."}]
        return "__".join(meaningful_parts) if meaningful_parts else self._safe_segment(target_path.stem)

    def _root_folder_name(self, root_path: Path) -> str:
        if self.lang == "kt":
            return "kotlin"
        return self._safe_segment(root_path.name or root_path.as_posix().replace("/", "_"))

    @staticmethod
    def _safe_segment(value: str) -> str:
        return "".join(char if char.isalnum() or char in {"-", "_", "."} else "_" for char in value)

    @staticmethod
    def _now() -> str:
        return datetime.now().astimezone().isoformat(timespec="seconds")
