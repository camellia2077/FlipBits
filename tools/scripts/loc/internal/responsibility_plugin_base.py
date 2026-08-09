from __future__ import annotations

from abc import ABC, abstractmethod
from pathlib import Path

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
)


class ResponsibilityLanguagePlugin(ABC):
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
