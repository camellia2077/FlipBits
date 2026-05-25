from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from pathlib import Path

from .config import LanguageConfig
from .responsibility_metrics import (
    ResponsibilityAnchor,
    ResponsibilityFunctionHotspot,
    ResponsibilityMoveSet,
    build_responsibility_result,
)
from .responsibility_plugins import create_responsibility_language_plugin


@dataclass(frozen=True)
class ResponsibilityRiskResult:
    path: str
    lines: int
    score: int
    priority: str
    summary: str
    state_signal_hits: int
    top_level_composables: int
    role_kinds: list[str]
    mode_branch_hits: int
    io_kind_count: int = 0
    rule_helper_count: int = 0
    responsibility_verb_kind_count: int = 0
    command_layer_leak_hits: int = 0
    interop_surface_hits: int = 0
    resource_lifecycle_hits: int = 0
    dominant_risks: list[str] | None = None
    suggestion: str | None = None
    next_action: str | None = None
    function_hotspots: list[ResponsibilityFunctionHotspot] | None = None
    anchors: list[ResponsibilityAnchor] | None = None
    move_sets: list[ResponsibilityMoveSet] | None = None

    def to_dict(self) -> dict:
        return {
            "path": self.path,
            "score": self.score,
            "priority": self.priority,
            "summary": self.summary,
            "dominant_risks": self.dominant_risks,
            "suggestion": self.suggestion,
            "next_action": self.next_action,
            "lines": self.lines,
            "function_hotspots": [
                {
                    "name": item.name,
                    "kind": item.kind,
                    "start_line": item.start_line,
                    "end_line": item.end_line,
                    "score": item.score,
                    "summary": item.summary,
                    "risks": item.risks,
                    "evidence": item.evidence,
                }
                for item in (self.function_hotspots or [])
            ],
            "anchors": [
                {
                    "line": item.line,
                    "label": item.label,
                    "issue": item.issue,
                    "evidence": item.evidence,
                }
                for item in (self.anchors or [])
            ],
            "move_sets": [
                {
                    "name": item.name,
                    "target_boundary": item.target_boundary,
                    "helpers": [
                        {
                            "name": helper.name,
                            "kind": helper.kind,
                            "start_line": helper.start_line,
                            "end_line": helper.end_line,
                        }
                        for helper in item.helpers
                    ],
                    "reason": item.reason,
                    "validation": item.validation,
                }
                for item in (self.move_sets or [])
            ],
            "mode_branch_hits": self.mode_branch_hits,
            "state_signal_hits": self.state_signal_hits,
            "top_level_composables": self.top_level_composables,
            "role_kinds": self.role_kinds,
            "io_kind_count": self.io_kind_count,
            "rule_helper_count": self.rule_helper_count,
            "responsibility_verb_kind_count": self.responsibility_verb_kind_count,
            "command_layer_leak_hits": self.command_layer_leak_hits,
            "interop_surface_hits": self.interop_surface_hits,
            "resource_lifecycle_hits": self.resource_lifecycle_hits,
        }


class ResponsibilityRiskAnalyzer(ABC):
    def __init__(self, config: LanguageConfig):
        self.config = config

    @abstractmethod
    def analyze_file(self, file_path: Path, text: str) -> ResponsibilityRiskResult:
        raise NotImplementedError


class PluginBackedResponsibilityRiskAnalyzer(ResponsibilityRiskAnalyzer):
    def __init__(self, config: LanguageConfig):
        super().__init__(config)
        plugin = create_responsibility_language_plugin(config)
        if plugin is None:
            raise ValueError(f"职责混杂风险扫描当前不支持语言: {config.lang}")
        self.plugin = plugin
        self.scorer = plugin.build_scorer()

    def analyze_file(self, file_path: Path, text: str) -> ResponsibilityRiskResult:
        metrics = self.plugin.collect_metrics(file_path=file_path, text=text)
        assessment = self.scorer.assess(file_path=file_path, metrics=metrics)
        details = self.plugin.collect_details(file_path=file_path, text=text, metrics=metrics, assessment=assessment)
        return ResponsibilityRiskResult(
            **build_responsibility_result(
                file_path=file_path,
                metrics=metrics,
                assessment=assessment,
                details=details,
            )
        )


def create_responsibility_risk_analyzer(
    config: LanguageConfig,
) -> ResponsibilityRiskAnalyzer | None:
    if config.lang not in {"kt", "py", "cpp"}:
        return None
    return PluginBackedResponsibilityRiskAnalyzer(config)
