from abc import ABC, abstractmethod
from dataclasses import dataclass, asdict
from pathlib import Path

from .config import LanguageConfig


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

    def to_dict(self) -> dict:
        return asdict(self)


class ResponsibilityRiskAnalyzer(ABC):
    def __init__(self, config: LanguageConfig):
        self.config = config

    @abstractmethod
    def analyze_file(self, file_path: Path, text: str) -> ResponsibilityRiskResult:
        raise NotImplementedError


def create_responsibility_risk_analyzer(
    config: LanguageConfig,
) -> ResponsibilityRiskAnalyzer | None:
    if config.lang == "kt":
        return KotlinResponsibilityRiskAnalyzer(config)
    if config.lang == "py":
        return PythonResponsibilityRiskAnalyzer(config)
    return None


class KotlinResponsibilityRiskAnalyzer(ResponsibilityRiskAnalyzer):
    import re as _re

    ROLE_NAME_PATTERNS = ("Section", "Block", "Card", "Switcher", "Timeline")
    MODE_BRANCH_RE = _re.compile(
        r"\b(if|when)\b[^{\n]*\b(\w*(Mode|mode)|selected\w*|viewMode|displayMode)\b"
    )
    STATE_SIGNAL_PATTERNS = (
        _re.compile(r"\bremember\w*\b"),
        _re.compile(r"\bmutableStateOf\b"),
        _re.compile(r"\bLaunchedEffect\b"),
    )
    TOP_LEVEL_FUNCTION_RE = _re.compile(
        r"^\s*(?:private|internal|public)?\s*fun\s+([A-Za-z_][A-Za-z0-9_]*)\b"
    )

    def analyze_file(self, file_path: Path, text: str) -> ResponsibilityRiskResult:
        lines = text.splitlines()
        line_count = len(lines)
        state_signal_hits = self._count_state_signals(text)
        composable_count, composable_names = self._count_top_level_composables(lines)
        role_kinds = self._collect_role_kinds(composable_names)
        mode_branch_hits = self._count_mode_branches(lines)
        score = 0

        if line_count > self.config.responsibility_line_threshold:
            score += 2
            if line_count >= self.config.responsibility_line_threshold + 120:
                score += 1
        if state_signal_hits >= self.config.responsibility_state_signal_threshold:
            score += 1
            if state_signal_hits >= self.config.responsibility_state_signal_threshold + 2:
                score += 1
            if state_signal_hits >= self.config.responsibility_state_signal_threshold + 5:
                score += 1
        if composable_count >= self.config.responsibility_top_level_composable_threshold:
            score += 1
            if composable_count >= self.config.responsibility_top_level_composable_threshold + 2:
                score += 1
            if composable_count >= self.config.responsibility_top_level_composable_threshold + 4:
                score += 1
        if len(role_kinds) >= self.config.responsibility_role_kind_threshold:
            score += 1
            if len(role_kinds) >= self.config.responsibility_role_kind_threshold + 1:
                score += 1
            if len(role_kinds) >= self.config.responsibility_role_kind_threshold + 2:
                score += 1
        if mode_branch_hits >= self.config.responsibility_mode_branch_threshold:
            score += 1
            if mode_branch_hits >= self.config.responsibility_mode_branch_threshold + 2:
                score += 1
            if mode_branch_hits >= self.config.responsibility_mode_branch_threshold + 4:
                score += 1

        priority, summary = self._classify_responsibility_risk(score)
        return ResponsibilityRiskResult(
            path=str(file_path),
            lines=line_count,
            score=score,
            priority=priority,
            summary=summary,
            state_signal_hits=state_signal_hits,
            top_level_composables=composable_count,
            role_kinds=role_kinds,
            mode_branch_hits=mode_branch_hits,
        )

    def _classify_responsibility_risk(self, score: int) -> tuple[str, str]:
        if score >= 9:
            return "P0", "高风险：大文件、状态信号和多角色命名同时出现，疑似职责混杂"
        if score >= 7:
            return "P1", "明显风险：建议优先检查状态拥有者和显示协调逻辑是否收口"
        if score >= 5:
            return "P2", "中度风险：可结合后续需求检查是否需要按职责边界拆分"
        return "P3", "低风险：暂作为观察项"

    def _count_state_signals(self, text: str) -> int:
        return sum(len(pattern.findall(text)) for pattern in self.STATE_SIGNAL_PATTERNS)

    def _count_top_level_composables(self, lines: list[str]) -> tuple[int, list[str]]:
        depth = 0
        pending_composable = False
        composable_names: list[str] = []

        for raw_line in lines:
            stripped = raw_line.strip()
            if depth == 0 and stripped.startswith("@Composable"):
                pending_composable = True
            elif depth == 0 and pending_composable:
                match = self.TOP_LEVEL_FUNCTION_RE.match(raw_line)
                if match:
                    composable_names.append(match.group(1))
                    pending_composable = False
                elif stripped and not stripped.startswith("@"):
                    pending_composable = False

            depth += raw_line.count("{") - raw_line.count("}")
            if depth < 0:
                depth = 0

        return len(composable_names), composable_names

    def _collect_role_kinds(self, composable_names: list[str]) -> list[str]:
        matched: list[str] = []
        for role_name in self.ROLE_NAME_PATTERNS:
            if any(role_name in name for name in composable_names):
                matched.append(role_name)
        return matched

    def _count_mode_branches(self, lines: list[str]) -> int:
        return sum(1 for line in lines if self.MODE_BRANCH_RE.search(line))


class PythonResponsibilityRiskAnalyzer(ResponsibilityRiskAnalyzer):
    import re as _re

    ROLE_NAME_PATTERNS = (
        "Manager",
        "Service",
        "Controller",
        "Handler",
        "Client",
        "Builder",
        "Parser",
        "Formatter",
        "Loader",
        "Writer",
    )
    STATE_SIGNAL_PATTERNS = (
        _re.compile(r"\bself\."),
        _re.compile(r"\bglobal\b"),
        _re.compile(r"\bnonlocal\b"),
        _re.compile(r"\bos\.environ\b"),
        _re.compile(r"\bthreading\b"),
        _re.compile(r"\basyncio\b"),
        _re.compile(r"\bsubprocess\b"),
        _re.compile(r"\brequests\b"),
    )
    TOP_LEVEL_SYMBOL_RE = _re.compile(
        r"^(?:async\s+def|def|class)\s+([A-Za-z_][A-Za-z0-9_]*)\b"
    )
    MODE_BRANCH_PATTERNS = (
        _re.compile(r"\bif\b[^\n]*\b(mode|kind|type)\s*=="),
        _re.compile(r"\belif\b[^\n]*\b(mode|kind|type)\s*=="),
        _re.compile(r"^\s*match\b"),
        _re.compile(r"^\s*case\b"),
    )

    def analyze_file(self, file_path: Path, text: str) -> ResponsibilityRiskResult:
        lines = text.splitlines()
        line_count = len(lines)
        state_signal_hits = self._count_state_signals(text)
        top_level_symbol_count, top_level_symbol_names = self._count_top_level_symbols(lines)
        role_kinds = self._collect_role_kinds(top_level_symbol_names)
        mode_branch_hits = self._count_mode_branches(lines)
        score = 0

        if line_count > self.config.responsibility_line_threshold:
            score += 2
            if line_count >= self.config.responsibility_line_threshold + 120:
                score += 1
        if state_signal_hits >= self.config.responsibility_state_signal_threshold:
            score += 1
            if state_signal_hits >= self.config.responsibility_state_signal_threshold + 3:
                score += 1
            if state_signal_hits >= self.config.responsibility_state_signal_threshold + 6:
                score += 1
        if top_level_symbol_count >= self.config.responsibility_top_level_composable_threshold:
            score += 1
            if top_level_symbol_count >= self.config.responsibility_top_level_composable_threshold + 4:
                score += 1
            if top_level_symbol_count >= self.config.responsibility_top_level_composable_threshold + 8:
                score += 1
        if len(role_kinds) >= self.config.responsibility_role_kind_threshold:
            score += 1
            if len(role_kinds) >= self.config.responsibility_role_kind_threshold + 1:
                score += 1
            if len(role_kinds) >= self.config.responsibility_role_kind_threshold + 2:
                score += 1
        if mode_branch_hits >= self.config.responsibility_mode_branch_threshold:
            score += 1
            if mode_branch_hits >= self.config.responsibility_mode_branch_threshold + 2:
                score += 1
            if mode_branch_hits >= self.config.responsibility_mode_branch_threshold + 4:
                score += 1

        priority, summary = self._classify_responsibility_risk(score)
        return ResponsibilityRiskResult(
            path=str(file_path),
            lines=line_count,
            score=score,
            priority=priority,
            summary=summary,
            state_signal_hits=state_signal_hits,
            top_level_composables=top_level_symbol_count,
            role_kinds=role_kinds,
            mode_branch_hits=mode_branch_hits,
        )

    def _classify_responsibility_risk(self, score: int) -> tuple[str, str]:
        if score >= 9:
            return "P0", "高风险：模块同时承载大体量、状态副作用和多角色符号，疑似职责混杂"
        if score >= 7:
            return "P1", "明显风险：建议优先检查协调逻辑、IO 副作用和模式分支是否收口"
        if score >= 5:
            return "P2", "中度风险：可结合后续需求检查是否需要按模块职责拆分"
        return "P3", "低风险：暂作为观察项"

    def _count_state_signals(self, text: str) -> int:
        return sum(len(pattern.findall(text)) for pattern in self.STATE_SIGNAL_PATTERNS)

    def _count_top_level_symbols(self, lines: list[str]) -> tuple[int, list[str]]:
        symbol_names: list[str] = []
        for raw_line in lines:
            if raw_line.startswith((" ", "\t")):
                continue
            match = self.TOP_LEVEL_SYMBOL_RE.match(raw_line.strip())
            if match:
                symbol_names.append(match.group(1))
        return len(symbol_names), symbol_names

    def _collect_role_kinds(self, symbol_names: list[str]) -> list[str]:
        matched: list[str] = []
        for role_name in self.ROLE_NAME_PATTERNS:
            if any(role_name in name for name in symbol_names):
                matched.append(role_name)
        return matched

    def _count_mode_branches(self, lines: list[str]) -> int:
        return sum(
            1
            for line in lines
            if any(pattern.search(line) for pattern in self.MODE_BRANCH_PATTERNS)
        )
