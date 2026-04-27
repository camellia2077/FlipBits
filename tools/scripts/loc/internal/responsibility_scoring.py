from __future__ import annotations

from abc import ABC, abstractmethod
from pathlib import Path

from .config import LanguageConfig
from .responsibility_categories import ResponsibilityRiskKind, RISK_SUMMARY_TEXT
from .responsibility_metrics import ResponsibilityAssessment, ResponsibilityMetrics


class BaseResponsibilityScorer(ABC):
    def __init__(self, config: LanguageConfig):
        self.config = config

    @abstractmethod
    def assess(self, *, file_path: Path, metrics: ResponsibilityMetrics) -> ResponsibilityAssessment:
        raise NotImplementedError

    def _base_score(self, metrics: ResponsibilityMetrics) -> int:
        score = 0
        if metrics.line_count >= self.config.responsibility_line_threshold:
            score += 2
        if metrics.state_signal_hits >= self.config.responsibility_state_signal_threshold:
            score += 1
        if (
            metrics.top_level_symbol_count
            >= self.config.responsibility_top_level_composable_threshold
        ):
            score += 1
        if len(metrics.role_kinds) >= self.config.responsibility_role_kind_threshold:
            score += 1
        if metrics.mode_branch_hits >= self.config.responsibility_mode_branch_threshold:
            score += 1
        return score

    @staticmethod
    def _priority_for_score(score: int) -> str:
        if score >= 8:
            return "P0"
        if score >= 6:
            return "P1"
        if score >= 4:
            return "P2"
        return "P3"

    @staticmethod
    def _join_risk_summaries(risks: list[ResponsibilityRiskKind]) -> str:
        if not risks:
            return "文件体量和职责信号较高，建议结合上下文复核拆分边界"
        return "；".join(RISK_SUMMARY_TEXT[risk] for risk in risks)


class KotlinResponsibilityScorer(BaseResponsibilityScorer):
    def assess(self, *, file_path: Path, metrics: ResponsibilityMetrics) -> ResponsibilityAssessment:
        del file_path
        score = self._base_score(metrics)
        dominant_risks: list[ResponsibilityRiskKind] = []
        if metrics.state_signal_hits >= self.config.responsibility_state_signal_threshold:
            dominant_risks.append(ResponsibilityRiskKind.STATEFUL_SIDE_EFFECTS)
        if metrics.mode_branch_hits >= self.config.responsibility_mode_branch_threshold:
            dominant_risks.append(ResponsibilityRiskKind.MODE_BRANCHING)
        summary = self._join_risk_summaries(dominant_risks)
        suggestion = None
        if dominant_risks:
            suggestion = "Keep UI orchestration shallow and move stateful branching into smaller state holders."
        return ResponsibilityAssessment(
            score=score,
            priority=self._priority_for_score(score),
            summary=summary,
            dominant_risks=dominant_risks or None,
            suggestion=suggestion,
        )


class PythonResponsibilityScorer(BaseResponsibilityScorer):
    def assess(self, *, file_path: Path, metrics: ResponsibilityMetrics) -> ResponsibilityAssessment:
        del file_path
        score = self._base_score(metrics)

        if metrics.io_kind_count >= 3:
            score += 1
        if metrics.rule_helper_count >= 6:
            score += 1
        if metrics.responsibility_verb_kind_count >= 3:
            score += 1
        if metrics.command_layer_leak_hits >= 3:
            score += 2

        dominant_risks: list[ResponsibilityRiskKind] = []
        if metrics.command_layer_leak_hits >= 3:
            dominant_risks.append(ResponsibilityRiskKind.COMMAND_LAYER_LEAK)
        if metrics.io_kind_count >= 3:
            dominant_risks.append(ResponsibilityRiskKind.IO_SURFACE_BREADTH)
        if metrics.rule_helper_count >= 6:
            dominant_risks.append(ResponsibilityRiskKind.RULE_HELPER_DENSITY)
        if metrics.responsibility_verb_kind_count >= 3:
            dominant_risks.append(ResponsibilityRiskKind.MIXED_RESPONSIBILITY_VERBS)
        if metrics.state_signal_hits >= self.config.responsibility_state_signal_threshold:
            dominant_risks.append(ResponsibilityRiskKind.STATEFUL_SIDE_EFFECTS)
        if metrics.mode_branch_hits >= self.config.responsibility_mode_branch_threshold:
            dominant_risks.append(ResponsibilityRiskKind.MODE_BRANCHING)

        summary = self._join_risk_summaries(dominant_risks)
        suggestion = self._build_python_suggestion(dominant_risks)
        return ResponsibilityAssessment(
            score=score,
            priority=self._priority_for_score(score),
            summary=summary,
            dominant_risks=dominant_risks or None,
            suggestion=suggestion,
        )

    @staticmethod
    def _build_python_suggestion(
        dominant_risks: list[ResponsibilityRiskKind],
    ) -> str | None:
        if not dominant_risks:
            return None
        if (
            ResponsibilityRiskKind.COMMAND_LAYER_LEAK in dominant_risks
            and ResponsibilityRiskKind.RULE_HELPER_DENSITY in dominant_risks
        ):
            return "Move validation and normalization helpers out of commands into a reusable core module."
        if (
            ResponsibilityRiskKind.RULE_HELPER_DENSITY in dominant_risks
            and ResponsibilityRiskKind.IO_SURFACE_BREADTH in dominant_risks
        ):
            return "Separate pure rule helpers from IO/reporting so command code mainly orchestrates."
        if ResponsibilityRiskKind.MIXED_RESPONSIBILITY_VERBS in dominant_risks:
            return "Split the module by read/validate/mutate/present responsibilities before adding more features."
        if ResponsibilityRiskKind.IO_SURFACE_BREADTH in dominant_risks:
            return "Keep IO at the edge and return structured results from the core logic."
        return "Trim the module into smaller units around side effects, mode branching, and reusable rules."


class CppResponsibilityScorer(BaseResponsibilityScorer):
    def assess(self, *, file_path: Path, metrics: ResponsibilityMetrics) -> ResponsibilityAssessment:
        del file_path
        score = self._base_score(metrics)

        if metrics.io_kind_count >= 3:
            score += 1
        if metrics.rule_helper_count >= 8:
            score += 1
        if metrics.responsibility_verb_kind_count >= 3:
            score += 1
        if metrics.interop_surface_hits >= 2:
            score += 2
        if metrics.resource_lifecycle_hits >= 6:
            score += 1

        dominant_risks: list[ResponsibilityRiskKind] = []
        if metrics.interop_surface_hits >= 2:
            dominant_risks.append(ResponsibilityRiskKind.INTEROP_SURFACE_BREADTH)
        if metrics.resource_lifecycle_hits >= 6:
            dominant_risks.append(ResponsibilityRiskKind.RESOURCE_LIFECYCLE_DENSITY)
        if metrics.rule_helper_count >= 8:
            dominant_risks.append(ResponsibilityRiskKind.RULE_HELPER_DENSITY)
        if metrics.responsibility_verb_kind_count >= 3:
            dominant_risks.append(ResponsibilityRiskKind.MIXED_RESPONSIBILITY_VERBS)
        if metrics.state_signal_hits >= self.config.responsibility_state_signal_threshold:
            dominant_risks.append(ResponsibilityRiskKind.STATEFUL_SHARED_RESOURCES)
        if metrics.mode_branch_hits >= self.config.responsibility_mode_branch_threshold:
            dominant_risks.append(ResponsibilityRiskKind.MODE_BRANCHING)
        if metrics.io_kind_count >= 3:
            dominant_risks.append(ResponsibilityRiskKind.IO_SURFACE_BREADTH)

        summary = self._join_risk_summaries(dominant_risks)
        suggestion = self._build_cpp_suggestion(dominant_risks)
        return ResponsibilityAssessment(
            score=score,
            priority=self._priority_for_score(score),
            summary=summary,
            dominant_risks=dominant_risks or None,
            suggestion=suggestion,
        )

    @staticmethod
    def _build_cpp_suggestion(
        dominant_risks: list[ResponsibilityRiskKind],
    ) -> str | None:
        if not dominant_risks:
            return None
        # Keep language-neutral risk kinds in the shared enum, while still letting
        # C++ tailor the remediation toward bridge/marshalling and ownership code.
        if (
            ResponsibilityRiskKind.INTEROP_SURFACE_BREADTH in dominant_risks
            and ResponsibilityRiskKind.RULE_HELPER_DENSITY in dominant_risks
        ):
            return "Split bridge and marshalling entrypoints from conversion and rule helpers."
        if ResponsibilityRiskKind.RESOURCE_LIFECYCLE_DENSITY in dominant_risks:
            return "Isolate ownership, release, cancel, and cleanup paths from codec or API logic."
        if ResponsibilityRiskKind.MIXED_RESPONSIBILITY_VERBS in dominant_risks:
            return "Separate validation/conversion, encode/decode, bridge, and lifecycle responsibilities."
        if ResponsibilityRiskKind.MODE_BRANCHING in dominant_risks:
            return "Narrow mode/style branching behind smaller strategy helpers or per-mode functions."
        return "Break the file around interop, lifecycle, and helper-heavy responsibilities before it grows further."
