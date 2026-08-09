from __future__ import annotations

from abc import ABC, abstractmethod
import hashlib
from pathlib import Path
import re

from .config import LanguageConfig
from .responsibility_metrics import (
    ResponsibilityAnchor,
    ResponsibilityAssessment,
    ResponsibilityDetails,
    ResponsibilityFunctionHotspot,
    ResponsibilityMetrics,
    ResponsibilityMoveSet,
    ResponsibilityMoveSetHelper,
)
from .responsibility_scoring import (
    BaseResponsibilityScorer,
    CppResponsibilityScorer,
    KotlinResponsibilityScorer,
    PythonResponsibilityScorer,
    RustResponsibilityScorer,
)


class ResponsibilityLanguagePlugin(ABC):
    DEPENDENCY_RE = re.compile(
        r'^\s*(?:#\s*include\s*[<"][^>"]+[>"]|(?:pub\s+)?(?:use|mod)\s+[^;]+;|import\s+[^;]+;)',
        re.MULTILINE,
    )
    def __init__(self, config: LanguageConfig):
        self.config = config

    @abstractmethod
    def build_scorer(self) -> BaseResponsibilityScorer:
        raise NotImplementedError

    @abstractmethod
    def collect_metrics(self, *, file_path: Path, text: str) -> ResponsibilityMetrics:
        raise NotImplementedError

    def collect_details(
        self,
        *,
        file_path: Path,
        text: str,
        metrics: ResponsibilityMetrics,
        assessment: ResponsibilityAssessment,
    ) -> ResponsibilityDetails:
        del file_path, text, metrics, assessment
        return ResponsibilityDetails(function_hotspots=[], anchors=[], move_sets=[])

    def metric_text(self, *, file_path: Path, text: str) -> str:
        del file_path
        return text

    def implementation_sources(self, *, file_path: Path, text: str) -> list[str]:
        del text
        return [str(file_path.resolve())]

    def content_fingerprint(self, *, file_path: Path, text: str) -> str:
        normalized = "\n".join(
            line.strip()
            for line in self.metric_text(file_path=file_path, text=text).splitlines()
            if line.strip() and not self.DEPENDENCY_RE.match(line)
        )
        return hashlib.sha256(normalized.encode("utf-8")).hexdigest()

    def dependency_fanout(self, *, text: str) -> int:
        return len({match.group(0).strip() for match in self.DEPENDENCY_RE.finditer(text)})

    @staticmethod
    def _count_pattern_hits(text: str, patterns: tuple[object, ...]) -> int:
        return sum(len(pattern.findall(text)) for pattern in patterns)

    @staticmethod
    def _count_pattern_kinds(text: str, patterns_by_kind: dict[str, tuple[object, ...]]) -> int:
        return sum(
            1
            for patterns in patterns_by_kind.values()
            if any(pattern.search(text) for pattern in patterns)
        )

    @staticmethod
    def _collect_role_kinds(symbol_names: list[str], role_name_patterns: tuple[str, ...]) -> list[str]:
        matched: list[str] = []
        for role_name in role_name_patterns:
            if any(role_name in name for name in symbol_names):
                matched.append(role_name)
        return matched

    @staticmethod
    def _append_anchor(
        *,
        anchors: list[ResponsibilityAnchor],
        seen_keys: set[tuple[str, str, str]],
        owner_issue_counts: dict[tuple[str, str], int],
        anchor: ResponsibilityAnchor,
        per_owner_issue_limit: int,
    ) -> None:
        dedupe_key = (anchor.label, anchor.issue, anchor.evidence)
        if dedupe_key in seen_keys:
            return
        owner_issue_key = (anchor.label, anchor.issue)
        if owner_issue_counts.get(owner_issue_key, 0) >= per_owner_issue_limit:
            return
        anchors.append(anchor)
        seen_keys.add(dedupe_key)
        owner_issue_counts[owner_issue_key] = owner_issue_counts.get(owner_issue_key, 0) + 1

    @staticmethod
    def _anchor_owner(function_ranges: list[tuple[int, int, str]], line: int) -> str:
        for start, end, name in function_ranges:
            if start <= line <= end:
                return name
        return "file"
