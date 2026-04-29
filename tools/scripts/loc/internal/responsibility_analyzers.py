from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import asdict, dataclass
from pathlib import Path

from .config import LanguageConfig
from .responsibility_metrics import build_responsibility_result
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

    def to_dict(self) -> dict:
        raw = asdict(self)
        ordered_keys = (
            "path",
            "score",
            "priority",
            "summary",
            "dominant_risks",
            "suggestion",
            "next_action",
            "lines",
            "state_signal_hits",
            "top_level_composables",
            "role_kinds",
            "mode_branch_hits",
            "io_kind_count",
            "rule_helper_count",
            "responsibility_verb_kind_count",
            "command_layer_leak_hits",
            "interop_surface_hits",
            "resource_lifecycle_hits",
        )
        return {key: raw[key] for key in ordered_keys}


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
        return ResponsibilityRiskResult(
            **build_responsibility_result(
                file_path=file_path,
                metrics=metrics,
                assessment=assessment,
            )
        )


def create_responsibility_risk_analyzer(
    config: LanguageConfig,
) -> ResponsibilityRiskAnalyzer | None:
    if config.lang not in {"kt", "py", "cpp"}:
        return None
    return PluginBackedResponsibilityRiskAnalyzer(config)
