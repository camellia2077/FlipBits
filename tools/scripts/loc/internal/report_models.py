from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

from .responsibility_analyzers import ResponsibilityRiskResult


@dataclass(frozen=True)
class ScanSpec:
    mode: str
    threshold: int
    max_depth: int | None = None

    def to_dict(self) -> dict:
        payload = {
            "mode": self.mode,
            "threshold": self.threshold,
        }
        if self.max_depth is not None:
            payload["max_depth"] = self.max_depth
        return payload


@dataclass(frozen=True)
class LineFileMatch:
    path: str
    lines: int
    priority: str | None = None
    summary: str | None = None

    def to_dict(self) -> dict:
        payload = {
            "path": self.path,
            "lines": self.lines,
        }
        if self.priority is not None:
            payload["priority"] = self.priority
        if self.summary is not None:
            payload["summary"] = self.summary
        return payload


@dataclass(frozen=True)
class DirectoryFileMatch:
    path: str
    files: int
    priority: str | None = None
    summary: str | None = None

    def to_dict(self) -> dict:
        payload = {
            "path": self.path,
            "files": self.files,
        }
        if self.priority is not None:
            payload["priority"] = self.priority
        if self.summary is not None:
            payload["summary"] = self.summary
        return payload


@dataclass(frozen=True)
class PathScanResult:
    path: str
    matched_files: tuple[LineFileMatch | ResponsibilityRiskResult, ...] = ()
    scanned_files: tuple[ResponsibilityRiskResult, ...] = ()
    canonical_mirrors: tuple[str, ...] = ()
    matched_dirs: tuple[DirectoryFileMatch, ...] = ()

    def to_dict(self) -> dict:
        payload = {"path": self.path}
        if self.matched_dirs:
            payload["matched_dirs"] = [item.to_dict() for item in self.matched_dirs]
        else:
            payload["matched_files"] = [self._file_item_to_dict(item) for item in self.matched_files]
            if self.scanned_files:
                payload["scanned_files"] = [item.to_dict() for item in self.scanned_files]
            if self.canonical_mirrors:
                payload["canonical_mirrors"] = list(self.canonical_mirrors)
        return payload

    @staticmethod
    def _file_item_to_dict(item: LineFileMatch | ResponsibilityRiskResult) -> dict:
        if isinstance(item, ResponsibilityRiskResult):
            return item.to_dict()
        return item.to_dict()


@dataclass(frozen=True)
class ScanReport:
    generated_at: str
    status: str
    lang: str
    scan: ScanSpec | None = None
    results: tuple[PathScanResult, ...] = ()
    summary: dict[str, int] = field(default_factory=dict)
    error: str | None = None
    baseline: str | None = None
    diff: dict | None = None

    def to_dict(self) -> dict:
        payload = {
            "generated_at": self.generated_at,
            "status": self.status,
            "lang": self.lang,
        }
        if self.scan is not None:
            payload["scan"] = self.scan.to_dict()
        if self.results:
            payload["results"] = [item.to_dict() for item in self.results]
        if self.summary:
            payload["summary"] = self.summary
        if self.error is not None:
            payload["error"] = self.error
        if self.baseline is not None:
            payload["baseline"] = self.baseline
        if self.diff is not None:
            payload["diff"] = self.diff
        return payload


@dataclass(frozen=True)
class ScopePartReport:
    part: str
    display_name: str
    report: ScanReport

    def to_dict(self) -> dict:
        payload = self.report.to_dict()
        payload["part"] = self.part
        payload["display_name"] = self.display_name
        return payload


@dataclass(frozen=True)
class ScopeReport:
    generated_at: str
    status: str
    scope: str
    display_name: str
    parts: tuple[ScopePartReport, ...] = ()
    summary: dict[str, int] = field(default_factory=dict)
    error: str | None = None
    baseline: str | None = None
    diff: dict | None = None

    def to_dict(self) -> dict:
        payload = {
            "generated_at": self.generated_at,
            "status": self.status,
            "scope": self.scope,
            "display_name": self.display_name,
        }
        if self.parts:
            payload["parts"] = [item.to_dict() for item in self.parts]
        if self.summary:
            payload["summary"] = self.summary
        if self.error is not None:
            payload["error"] = self.error
        if self.baseline is not None:
            payload["baseline"] = self.baseline
        if self.diff is not None:
            payload["diff"] = self.diff
        return payload


@dataclass(frozen=True)
class DetailReport:
    generated_at: str
    status: str
    lang: str
    scan: ScanSpec
    result: LineFileMatch | DirectoryFileMatch | ResponsibilityRiskResult

    def to_dict(self) -> dict:
        if isinstance(self.result, ResponsibilityRiskResult):
            result_payload = self.result.to_dict()
        else:
            result_payload = self.result.to_dict()
        return {
            "generated_at": self.generated_at,
            "status": self.status,
            "lang": self.lang,
            "scan": self.scan.to_dict(),
            "result": result_payload,
        }


@dataclass(frozen=True)
class OutputArtifact:
    relative_output_path: Path
    report: DetailReport
