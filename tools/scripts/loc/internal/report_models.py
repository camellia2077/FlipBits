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
    matched_dirs: tuple[DirectoryFileMatch, ...] = ()

    def to_dict(self) -> dict:
        payload = {"path": self.path}
        if self.matched_dirs:
            payload["matched_dirs"] = [item.to_dict() for item in self.matched_dirs]
        else:
            payload["matched_files"] = [self._file_item_to_dict(item) for item in self.matched_files]
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
