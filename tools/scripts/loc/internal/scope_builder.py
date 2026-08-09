from __future__ import annotations

from dataclasses import dataclass, replace
from pathlib import Path

from .config import LanguageConfig, ScopeConfig
from .report_builder import ReportBuilder
from .report_models import OutputArtifact, ScopePartReport, ScopeReport
from .service import LocScanService


@dataclass(frozen=True)
class ScopeBuildResult:
    report: ScopeReport
    artifacts: tuple[OutputArtifact, ...]


class ScopeReportBuilder:
    def __init__(self, *, config: ScopeConfig, language_configs: tuple[LanguageConfig, ...]):
        self.config = config
        self.language_configs = language_configs

    def build_line_scan(
        self,
        *,
        generated_at: str,
        paths: list[Path],
        mode: str,
        thresholds: dict[str, int],
    ) -> ScopeBuildResult:
        return self._build(
            generated_at=generated_at,
            build_for_language=lambda language_config: ReportBuilder(
                lang=language_config.lang,
                scan_service=LocScanService(language_config),
            ).build_line_scan(
                generated_at=generated_at,
                paths=paths,
                mode=mode,
                threshold=thresholds[language_config.lang],
            ),
        )

    def build_dir_scan(
        self,
        *,
        generated_at: str,
        paths: list[Path],
        thresholds: dict[str, int],
        max_depth: int | None,
    ) -> ScopeBuildResult:
        return self._build(
            generated_at=generated_at,
            build_for_language=lambda language_config: ReportBuilder(
                lang=language_config.lang,
                scan_service=LocScanService(language_config),
            ).build_dir_scan(
                generated_at=generated_at,
                paths=paths,
                threshold=thresholds[language_config.lang],
                max_depth=max_depth,
            ),
        )

    def build_responsibility_scan(
        self,
        *,
        generated_at: str,
        paths: list[Path],
        thresholds: dict[str, int],
    ) -> ScopeBuildResult:
        return self._build(
            generated_at=generated_at,
            build_for_language=lambda language_config: ReportBuilder(
                lang=language_config.lang,
                scan_service=LocScanService(language_config),
            ).build_responsibility_scan(
                generated_at=generated_at,
                paths=paths,
                threshold=thresholds[language_config.lang],
            ),
        )

    def build_error_report(self, *, generated_at: str, message: str) -> ScopeReport:
        return ScopeReport(
            generated_at=generated_at,
            status="error",
            scope=self.config.scope,
            display_name=self.config.display_name,
            error=message,
        )

    def _build(self, *, generated_at: str, build_for_language) -> ScopeBuildResult:
        parts: list[ScopePartReport] = []
        artifacts: list[OutputArtifact] = []
        matched_files = 0
        matched_dirs = 0

        for language_config in self.language_configs:
            build_result = build_for_language(language_config)
            parts.append(
                ScopePartReport(
                    part=language_config.lang,
                    display_name=language_config.display_name,
                    report=build_result.report,
                )
            )
            artifacts.extend(
                replace(
                    artifact,
                    relative_output_path=Path(
                        "kotlin" if language_config.lang == "kt" else language_config.lang
                    )
                    / artifact.relative_output_path,
                )
                for artifact in build_result.artifacts
            )
            matched_files += build_result.report.summary.get("matched_files", 0)
            matched_dirs += build_result.report.summary.get("matched_dirs", 0)

        summary = {"parts": len(parts)}
        if matched_files:
            summary["matched_files"] = matched_files
        if matched_dirs:
            summary["matched_dirs"] = matched_dirs
        report = ScopeReport(
            generated_at=generated_at,
            status="ok",
            scope=self.config.scope,
            display_name=self.config.display_name,
            parts=tuple(parts),
            summary=summary,
        )
        return ScopeBuildResult(report=report, artifacts=tuple(artifacts))
