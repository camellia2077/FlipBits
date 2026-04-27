from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from .responsibility_categories import ResponsibilityRiskKind


@dataclass(frozen=True)
class ResponsibilityMetrics:
    line_count: int
    state_signal_hits: int
    top_level_symbol_count: int
    role_kinds: list[str]
    mode_branch_hits: int
    io_kind_count: int = 0
    rule_helper_count: int = 0
    responsibility_verb_kind_count: int = 0
    command_layer_leak_hits: int = 0
    interop_surface_hits: int = 0
    resource_lifecycle_hits: int = 0


@dataclass(frozen=True)
class ResponsibilityAssessment:
    score: int
    priority: str
    summary: str
    dominant_risks: list[ResponsibilityRiskKind] | None = None
    suggestion: str | None = None


def build_responsibility_result(
    *,
    file_path: Path,
    metrics: ResponsibilityMetrics,
    assessment: ResponsibilityAssessment,
) -> dict[str, object]:
    return {
        "path": str(file_path),
        "lines": metrics.line_count,
        "score": assessment.score,
        "priority": assessment.priority,
        "summary": assessment.summary,
        "state_signal_hits": metrics.state_signal_hits,
        "top_level_composables": metrics.top_level_symbol_count,
        "role_kinds": metrics.role_kinds,
        "mode_branch_hits": metrics.mode_branch_hits,
        "io_kind_count": metrics.io_kind_count,
        "rule_helper_count": metrics.rule_helper_count,
        "responsibility_verb_kind_count": metrics.responsibility_verb_kind_count,
        "command_layer_leak_hits": metrics.command_layer_leak_hits,
        "interop_surface_hits": metrics.interop_surface_hits,
        "resource_lifecycle_hits": metrics.resource_lifecycle_hits,
        "dominant_risks": [risk.value for risk in assessment.dominant_risks]
        if assessment.dominant_risks
        else None,
        "suggestion": assessment.suggestion,
    }
