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


class KotlinResponsibilityPlugin(ResponsibilityLanguagePlugin):
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
        r"^\s*(?:private|internal|public)?\s*fun\s+(?:[A-Za-z_][A-Za-z0-9_.<>?]*\s*\.\s*)?([A-Za-z_][A-Za-z0-9_]*)\b"
    )
    STATE_LINE_PATTERNS = (
        (_re.compile(r"\bremember\w*\b"), "state holder"),
        (_re.compile(r"\bmutableStateOf\b"), "mutable state"),
        (_re.compile(r"\bLaunchedEffect\b"), "effect"),
    )
    MODE_LINE_PATTERN = _re.compile(r"\b(if|when)\b[^{\n]*\b(\w*(Mode|mode)|selected\w*|viewMode|displayMode)\b")
    DRAW_BRANCH_PATTERN = _re.compile(r"\bwhen\s*\(\s*(?:val\s+\w+\s*=\s*)?(drawContent|mode)\s*\)")

    def build_scorer(self) -> BaseResponsibilityScorer:
        return KotlinResponsibilityScorer(self.config)

    def collect_metrics(self, *, file_path: Path, text: str) -> ResponsibilityMetrics:
        del file_path
        lines = text.splitlines()
        composable_count, composable_names = self._count_top_level_composables(lines)
        return ResponsibilityMetrics(
            line_count=len(lines),
            state_signal_hits=self._count_pattern_hits(text, self.STATE_SIGNAL_PATTERNS),
            top_level_symbol_count=composable_count,
            role_kinds=self._collect_role_kinds(composable_names, self.ROLE_NAME_PATTERNS),
            mode_branch_hits=sum(1 for line in lines if self.MODE_BRANCH_RE.search(line)),
        )

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

    def collect_details(
        self,
        *,
        file_path: Path,
        text: str,
        metrics: ResponsibilityMetrics,
        assessment: ResponsibilityAssessment,
    ) -> ResponsibilityDetails:
        del file_path, metrics, assessment
        lines = text.splitlines()
        functions = self._collect_top_level_function_ranges(lines)
        hotspots = self._collect_kotlin_function_hotspots(lines, functions)
        anchors = self._collect_kotlin_anchors(lines, functions)
        return ResponsibilityDetails(function_hotspots=hotspots, anchors=anchors, move_sets=[])

    def _collect_top_level_function_ranges(self, lines: list[str]) -> list[tuple[str, int, int, bool]]:
        depth = 0
        pending_composable = False
        functions: list[tuple[str, int, int, bool]] = []
        current_name: str | None = None
        current_start = 0
        current_is_composable = False
        pending_signature_lines: list[str] = []
        pending_signature_start = 0

        for index, raw_line in enumerate(lines, start=1):
            stripped = raw_line.strip()
            if depth == 0 and stripped.startswith("@Composable"):
                pending_composable = True
            if depth == 0 and current_name is None:
                if pending_signature_lines:
                    pending_signature_lines.append(stripped)
                elif stripped.startswith(("private fun ", "internal fun ", "public fun ", "fun ")):
                    pending_signature_lines = [stripped]
                    pending_signature_start = index
                if pending_signature_lines:
                    signature = " ".join(part for part in pending_signature_lines if part)
                    match = self.TOP_LEVEL_FUNCTION_RE.match(signature)
                    if match and "{" in signature:
                        current_name = match.group(1)
                        current_start = pending_signature_start or index
                        current_is_composable = pending_composable
                        pending_composable = False
                        pending_signature_lines = []
                        pending_signature_start = 0
                    elif stripped.endswith("{") and not match:
                        pending_signature_lines = []
                        pending_signature_start = 0
            depth += raw_line.count("{") - raw_line.count("}")
            if depth == 0 and current_name is not None:
                functions.append((current_name, current_start, index, current_is_composable))
                current_name = None
                current_start = 0
                current_is_composable = False
            if depth < 0:
                depth = 0
        return functions

    def _collect_kotlin_function_hotspots(
        self,
        lines: list[str],
        functions: list[tuple[str, int, int, bool]],
    ) -> list[ResponsibilityFunctionHotspot]:
        hotspots: list[ResponsibilityFunctionHotspot] = []
        for name, start_line, end_line, is_composable in functions:
            function_lines = lines[start_line - 1 : end_line]
            line_count = len(function_lines)
            state_hits = self._count_pattern_hits("\n".join(function_lines), tuple(pattern for pattern, _ in self.STATE_LINE_PATTERNS))
            mode_hits = sum(1 for line in function_lines if self.MODE_LINE_PATTERN.search(line))
            draw_dispatch_hits = sum(1 for line in function_lines if self.DRAW_BRANCH_PATTERN.search(line))
            overlay_hits = sum(1 for line in function_lines if "Overlay(" in line or "Overlay =" in line)
            score = 0
            if line_count >= 80:
                score += 1
            if line_count >= 140:
                score += 1
            if state_hits >= 2:
                score += 1
            if state_hits >= 4:
                score += 1
            if mode_hits >= 1:
                score += 1
            if mode_hits >= 3:
                score += 1
            if draw_dispatch_hits >= 1:
                score += 1
            if draw_dispatch_hits >= 2:
                score += 1
            if overlay_hits >= 2:
                score += 1
            has_material_risk = state_hits >= 2 or mode_hits >= 1 or draw_dispatch_hits >= 1
            if not is_composable and (score < 2 or not has_material_risk):
                continue
            if is_composable and (score < 2 or not has_material_risk):
                continue
            risks: list[str] = []
            evidence: list[str] = [f"lines {line_count}"]
            if state_hits >= 2:
                risks.append("stateful_side_effects")
                evidence.append(f"state hits {state_hits}")
            if mode_hits >= 1 or draw_dispatch_hits >= 1:
                risks.append("mode_branching")
                evidence.append(f"mode branches {mode_hits + draw_dispatch_hits}")
            if overlay_hits >= 2:
                evidence.append(f"overlays {overlay_hits}")
            if line_count >= 80:
                evidence.append("large function")
            summary_parts: list[str] = []
            if state_hits >= 2:
                summary_parts.append("状态/副作用偏多")
            if mode_hits >= 1 or draw_dispatch_hits >= 1:
                summary_parts.append("分支分发较多")
            if overlay_hits >= 2:
                summary_parts.append("overlay 挂载较多")
            if not summary_parts:
                summary_parts.append("局部职责偏重")
            hotspots.append(
                ResponsibilityFunctionHotspot(
                    name=name,
                    kind="composable" if is_composable else "function",
                    start_line=start_line,
                    end_line=end_line,
                    score=score,
                    summary="；".join(summary_parts),
                    risks=risks,
                    evidence=evidence,
                )
            )
        hotspots.sort(key=lambda item: (-item.score, item.start_line))
        return hotspots[:4]

    def _collect_kotlin_anchors(
        self,
        lines: list[str],
        functions: list[tuple[str, int, int, bool]],
    ) -> list[ResponsibilityAnchor]:
        anchors: list[ResponsibilityAnchor] = []
        function_by_range = [(start, end, name) for name, start, end, _ in functions]
        seen_keys: set[tuple[str, str, str]] = set()
        owner_issue_counts: dict[tuple[str, str], int] = {}
        for index, raw_line in enumerate(lines, start=1):
            stripped = raw_line.strip()
            if not stripped or stripped.startswith(("package ", "import ")):
                continue
            owner = self._anchor_owner(function_by_range, index)
            if self.DRAW_BRANCH_PATTERN.search(raw_line):
                self._append_anchor(
                    anchors=anchors,
                    seen_keys=seen_keys,
                    owner_issue_counts=owner_issue_counts,
                    anchor=ResponsibilityAnchor(
                        line=index,
                        label=owner,
                        issue="绘制分发分支集中在这里",
                        evidence=stripped,
                    ),
                    per_owner_issue_limit=2,
                )
            elif self.MODE_LINE_PATTERN.search(raw_line):
                self._append_anchor(
                    anchors=anchors,
                    seen_keys=seen_keys,
                    owner_issue_counts=owner_issue_counts,
                    anchor=ResponsibilityAnchor(
                        line=index,
                        label=owner,
                        issue="mode/style 状态分支出现在这里",
                        evidence=stripped,
                    ),
                    per_owner_issue_limit=2,
                )
            else:
                for pattern, label in self.STATE_LINE_PATTERNS:
                    if pattern.search(raw_line):
                        self._append_anchor(
                            anchors=anchors,
                            seen_keys=seen_keys,
                            owner_issue_counts=owner_issue_counts,
                            anchor=ResponsibilityAnchor(
                                line=index,
                                label=owner,
                                issue=f"{label} 信号出现在这里",
                                evidence=stripped,
                            ),
                            per_owner_issue_limit=2,
                        )
                        break
            if len(anchors) >= 8:
                break
        return anchors


class PythonResponsibilityPlugin(ResponsibilityLanguagePlugin):
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
        _re.compile(r"^\s*(?:import\s+subprocess|from\s+subprocess\s+import|\w+\s*=\s*)?subprocess\."),
        _re.compile(r"^\s*(?:import\s+requests|from\s+requests\s+import|\w+\s*=\s*)?requests\."),
    )
    TOP_LEVEL_SYMBOL_RE = _re.compile(r"^(?:async\s+def|def|class)\s+([A-Za-z_][A-Za-z0-9_]*)\b")
    TOP_LEVEL_FUNCTION_RE = _re.compile(r"^(?:async\s+def|def)\s+([A-Za-z_][A-Za-z0-9_]*)\b")
    MODE_BRANCH_PATTERNS = (
        _re.compile(r"\bif\b[^\n]*\b(mode|kind|type)\s*=="),
        _re.compile(r"\belif\b[^\n]*\b(mode|kind|type)\s*=="),
        _re.compile(r"^\s*match\b"),
        _re.compile(r"^\s*case\b"),
    )
    IO_KIND_PATTERNS = {
        "filesystem": (
            _re.compile(r"\bopen\s*\("),
            _re.compile(r"\bread_text\s*\("),
            _re.compile(r"\bwrite_text\s*\("),
            _re.compile(r"\bread_bytes\s*\("),
            _re.compile(r"\bwrite_bytes\s*\("),
        ),
        "console": (
            _re.compile(r"\bprint\s*\("),
            _re.compile(r"\bsys\.(stdout|stderr)\b"),
        ),
        "process": (
            _re.compile(r"^\s*import\s+subprocess\b"),
            _re.compile(r"^\s*from\s+subprocess\s+import\b"),
            _re.compile(r"\bsubprocess\."),
        ),
        "network": (
            _re.compile(r"^\s*import\s+requests\b"),
            _re.compile(r"^\s*from\s+requests\s+import\b"),
            _re.compile(r"\brequests\."),
            _re.compile(r"\burllib\."),
            _re.compile(r"\bhttpx\."),
        ),
        "env": (_re.compile(r"\bos\.environ\b"),),
        "serialization": (
            _re.compile(r"\bjson\.(load|loads|dump|dumps)\b"),
            _re.compile(r"\bxml\.(etree|minidom)\b"),
        ),
    }
    RULE_HELPER_PATTERNS = (
        _re.compile(r"^(?:async\s+def|def)\s+(validate|check|normalize|resolve|parse|encode|decode|match)[A-Za-z_0-9]*\b"),
        _re.compile(r"^\s*[A-Z][A-Z0-9_]+\s*="),
        _re.compile(r"^\s*[A-Za-z_][A-Za-z0-9_]*_RE\s*="),
    )
    RESPONSIBILITY_VERB_GROUPS = {
        "read_load_parse": ("load", "read", "parse"),
        "validate_check": ("validate", "check"),
        "resolve_normalize": ("resolve", "normalize"),
        "apply_write_update": ("apply", "write", "update"),
        "print_render_format": ("print", "render", "format"),
    }
    IO_LINE_PATTERNS = (
        (_re.compile(r"\bopen\s*\("), "filesystem"),
        (_re.compile(r"\bprint\s*\("), "console"),
        (_re.compile(r"^\s*import\s+subprocess\b|\bsubprocess\."), "process"),
        (_re.compile(r"^\s*import\s+requests\b|\brequests\."), "network"),
        (_re.compile(r"\bos\.environ\b"), "env"),
        (_re.compile(r"\bjson\.(load|loads|dump|dumps)\b"), "serialization"),
    )

    def build_scorer(self) -> BaseResponsibilityScorer:
        return PythonResponsibilityScorer(self.config)

    def collect_metrics(self, *, file_path: Path, text: str) -> ResponsibilityMetrics:
        lines = text.splitlines()
        top_level_symbol_count, top_level_symbol_names = self._count_top_level_symbols(lines)
        return ResponsibilityMetrics(
            line_count=len(lines),
            state_signal_hits=self._count_pattern_hits(text, self.STATE_SIGNAL_PATTERNS),
            top_level_symbol_count=top_level_symbol_count,
            role_kinds=self._collect_role_kinds(top_level_symbol_names, self.ROLE_NAME_PATTERNS),
            mode_branch_hits=sum(
                1 for line in lines if any(pattern.search(line) for pattern in self.MODE_BRANCH_PATTERNS)
            ),
            io_kind_count=self._count_pattern_kinds(text, self.IO_KIND_PATTERNS),
            rule_helper_count=self._count_rule_helpers(lines),
            responsibility_verb_kind_count=self._count_responsibility_verb_kinds(lines),
            command_layer_leak_hits=self._count_command_layer_leaks(file_path, lines, text),
        )

    def _count_top_level_symbols(self, lines: list[str]) -> tuple[int, list[str]]:
        symbol_names: list[str] = []
        for raw_line in lines:
            if raw_line.startswith((" ", "\t")):
                continue
            match = self.TOP_LEVEL_SYMBOL_RE.match(raw_line.strip())
            if match:
                symbol_names.append(match.group(1))
        return len(symbol_names), symbol_names

    def _count_rule_helpers(self, lines: list[str]) -> int:
        count = 0
        for raw_line in lines:
            if raw_line.startswith((" ", "\t")):
                continue
            stripped = raw_line.strip()
            if any(pattern.match(stripped) for pattern in self.RULE_HELPER_PATTERNS):
                count += 1
        return count

    def _count_responsibility_verb_kinds(self, lines: list[str]) -> int:
        top_level_function_names: list[str] = []
        for raw_line in lines:
            if raw_line.startswith((" ", "\t")):
                continue
            match = self.TOP_LEVEL_FUNCTION_RE.match(raw_line.strip())
            if match:
                top_level_function_names.append(match.group(1).lower())
        matched_groups = 0
        for prefixes in self.RESPONSIBILITY_VERB_GROUPS.values():
            if any(name.startswith(prefix) for name in top_level_function_names for prefix in prefixes):
                matched_groups += 1
        return matched_groups

    def _count_command_layer_leaks(self, file_path: Path, lines: list[str], text: str) -> int:
        if "commands" not in {part.lower() for part in file_path.parts}:
            return 0
        leak_hits = self._count_rule_helpers(lines)
        if self._count_pattern_kinds(text, self.IO_KIND_PATTERNS) >= 3:
            leak_hits += 1
        top_level_function_names = [
            match.group(1).lower()
            for raw_line in lines
            if not raw_line.startswith((" ", "\t"))
            for match in [self.TOP_LEVEL_FUNCTION_RE.match(raw_line.strip())]
            if match
        ]
        helper_prefixes = ("validate", "check", "normalize", "resolve", "parse", "encode", "decode", "match")
        leak_hits += sum(
            1 for name in top_level_function_names if any(name.startswith(prefix) for prefix in helper_prefixes)
        )
        return leak_hits

    def collect_details(
        self,
        *,
        file_path: Path,
        text: str,
        metrics: ResponsibilityMetrics,
        assessment: ResponsibilityAssessment,
    ) -> ResponsibilityDetails:
        del metrics, assessment
        lines = text.splitlines()
        functions = self._collect_python_symbol_ranges(lines)
        hotspots = self._collect_python_function_hotspots(file_path=file_path, lines=lines, functions=functions)
        anchors = self._collect_python_anchors(file_path=file_path, lines=lines, functions=functions)
        move_sets = self._collect_python_move_sets(file_path=file_path, functions=functions)
        return ResponsibilityDetails(function_hotspots=hotspots, anchors=anchors, move_sets=move_sets)

    def _collect_python_symbol_ranges(self, lines: list[str]) -> list[tuple[str, int, int, str]]:
        symbols: list[tuple[str, int, int, str]] = []
        stack: list[tuple[int, str, int, str]] = []
        for index, raw_line in enumerate(lines, start=1):
            stripped = raw_line.strip()
            if not stripped or stripped.startswith("#"):
                continue
            indent = len(raw_line) - len(raw_line.lstrip(" "))
            while stack and indent <= stack[-1][0]:
                start_indent, name, start_line, kind = stack.pop()
                symbols.append((name, start_line, index - 1, kind))
            if raw_line.startswith((" ", "\t")):
                continue
            match = self.TOP_LEVEL_SYMBOL_RE.match(stripped)
            if match:
                kind = "class" if stripped.startswith("class ") else "function"
                stack.append((indent, match.group(1), index, kind))
        while stack:
            _, name, start_line, kind = stack.pop()
            symbols.append((name, start_line, len(lines), kind))
        symbols.sort(key=lambda item: item[1])
        return symbols

    def _collect_python_function_hotspots(
        self,
        *,
        file_path: Path,
        lines: list[str],
        functions: list[tuple[str, int, int, str]],
    ) -> list[ResponsibilityFunctionHotspot]:
        hotspots: list[ResponsibilityFunctionHotspot] = []
        in_commands = "commands" in {part.lower() for part in file_path.parts}
        for name, start_line, end_line, kind in functions:
            function_lines = lines[start_line - 1 : end_line]
            body_text = "\n".join(function_lines)
            line_count = len(function_lines)
            state_hits = self._count_pattern_hits(body_text, self.STATE_SIGNAL_PATTERNS)
            mode_hits = sum(1 for line in function_lines if any(pattern.search(line) for pattern in self.MODE_BRANCH_PATTERNS))
            io_kind_hits = self._count_pattern_kinds(body_text, self.IO_KIND_PATTERNS)
            helper_hits = sum(1 for line in function_lines if any(pattern.match(line.strip()) for pattern in self.RULE_HELPER_PATTERNS))
            score = 0
            if line_count >= 60:
                score += 1
            if line_count >= 120:
                score += 1
            if io_kind_hits >= 2:
                score += 1
            if io_kind_hits >= 3:
                score += 1
            if helper_hits >= 1:
                score += 1
            if mode_hits >= 1:
                score += 1
            if state_hits >= 2:
                score += 1
            if in_commands and (io_kind_hits >= 2 or helper_hits >= 1):
                score += 1
            risks: list[str] = []
            evidence: list[str] = [f"lines {line_count}"]
            if io_kind_hits >= 2:
                risks.append("io_surface_breadth")
                evidence.append(f"io kinds {io_kind_hits}")
            if helper_hits >= 1:
                risks.append("rule_helper_density")
                evidence.append(f"helpers {helper_hits}")
            if mode_hits >= 1:
                risks.append("mode_branching")
                evidence.append(f"mode branches {mode_hits}")
            if in_commands and (io_kind_hits >= 2 or helper_hits >= 1):
                risks.append("command_layer_leak")
                evidence.append("command layer mixed concerns")
            if state_hits >= 2:
                evidence.append(f"state hits {state_hits}")
            if line_count >= 60:
                evidence.append("large symbol")
            if score < 2 or not risks:
                continue
            summary_parts: list[str] = []
            if io_kind_hits >= 2:
                summary_parts.append("IO 面偏宽")
            if helper_hits >= 1:
                summary_parts.append("规则/helper 偏密")
            if mode_hits >= 1:
                summary_parts.append("mode/type 分支较多")
            if in_commands and (io_kind_hits >= 2 or helper_hits >= 1):
                summary_parts.append("命令层混入底层职责")
            hotspots.append(
                ResponsibilityFunctionHotspot(
                    name=name,
                    kind=kind,
                    start_line=start_line,
                    end_line=end_line,
                    score=score,
                    summary="；".join(summary_parts),
                    risks=risks,
                    evidence=evidence,
                )
            )
        hotspots.sort(key=lambda item: (-item.score, item.start_line))
        return hotspots[:4]

    def _collect_python_anchors(
        self,
        *,
        file_path: Path,
        lines: list[str],
        functions: list[tuple[str, int, int, str]],
    ) -> list[ResponsibilityAnchor]:
        del file_path
        anchors: list[ResponsibilityAnchor] = []
        function_ranges = [(start, end, name) for name, start, end, _ in functions]
        seen_keys: set[tuple[str, str, str]] = set()
        owner_issue_counts: dict[tuple[str, str], int] = {}
        for index, raw_line in enumerate(lines, start=1):
            stripped = raw_line.strip()
            if not stripped or stripped.startswith(("#", "import ", "from ")):
                continue
            owner = self._anchor_owner(function_ranges, index)
            if any(pattern.search(raw_line) for pattern in self.MODE_BRANCH_PATTERNS):
                self._append_anchor(
                    anchors=anchors,
                    seen_keys=seen_keys,
                    owner_issue_counts=owner_issue_counts,
                    anchor=ResponsibilityAnchor(
                        line=index,
                        label=owner,
                        issue="mode/kind/type 分支出现在这里",
                        evidence=stripped,
                    ),
                    per_owner_issue_limit=2,
                )
                continue
            if any(pattern.match(stripped) for pattern in self.RULE_HELPER_PATTERNS):
                self._append_anchor(
                    anchors=anchors,
                    seen_keys=seen_keys,
                    owner_issue_counts=owner_issue_counts,
                    anchor=ResponsibilityAnchor(
                        line=index,
                        label=owner,
                        issue="规则/helper 逻辑出现在这里",
                        evidence=stripped,
                    ),
                    per_owner_issue_limit=2,
                )
                continue
            for pattern, label in self.IO_LINE_PATTERNS:
                if pattern.search(raw_line):
                    self._append_anchor(
                        anchors=anchors,
                        seen_keys=seen_keys,
                        owner_issue_counts=owner_issue_counts,
                        anchor=ResponsibilityAnchor(
                            line=index,
                            label=owner,
                            issue=f"{label} IO 信号出现在这里",
                            evidence=stripped,
                        ),
                        per_owner_issue_limit=2,
                    )
                    break
            if len(anchors) >= 8:
                break
        return anchors

    def _collect_python_move_sets(
        self,
        *,
        file_path: Path,
        functions: list[tuple[str, int, int, str]],
    ) -> list[ResponsibilityMoveSet]:
        groups: dict[str, tuple[str, str, str, list[ResponsibilityMoveSetHelper]]] = {}
        for name, start_line, end_line, kind in functions:
            move_set = self._python_move_set_for_symbol(file_path, name)
            if move_set is None:
                continue
            key, target_boundary, reason, validation = move_set
            if key not in groups:
                groups[key] = (target_boundary, reason, validation, [])
            groups[key][3].append(
                ResponsibilityMoveSetHelper(
                    name=name,
                    kind=kind,
                    start_line=start_line,
                    end_line=end_line,
                )
            )

        result: list[ResponsibilityMoveSet] = []
        for key, (target_boundary, reason, validation, helpers) in groups.items():
            if len(helpers) < 2:
                continue
            helpers.sort(key=lambda item: item.start_line)
            result.append(
                ResponsibilityMoveSet(
                    name=key,
                    target_boundary=target_boundary,
                    helpers=helpers[:8],
                    reason=reason,
                    validation=validation,
                )
            )
        result.sort(key=lambda item: (-len(item.helpers), item.helpers[0].start_line))
        return result[:4]

    @staticmethod
    def _python_move_set_for_symbol(file_path: Path, name: str) -> tuple[str, str, str, str] | None:
        path = str(file_path).replace("\\", "/").lower()
        lower = name.lower()
        if "/repo_tooling/commands/" in path:
            domain = file_path.stem
            if lower.startswith(("candidate_", "resolve_", "require_", "expected_", "cache_", "reset_")):
                return (
                    f"{domain}_toolchain_resolution",
                    f"repo_tooling/{domain}/toolchain.py or repo_tooling/{domain}/paths.py",
                    "tool discovery, env lookup, stale-cache checks, and path policy should move out of commands as one package",
                    f"python -m unittest tools.tests.test_{domain}_tools",
                )
            if lower.startswith(("build_", "read_", "values_", "export_")):
                return (
                    f"{domain}_pure_rules",
                    f"repo_tooling/{domain}/sample_texts.py or repo_tooling/{domain}/rules.py",
                    "pure parsing and payload-building helpers should move together so command modules stay as adapters",
                    f"python -m unittest tools.tests.test_{domain}_tools",
                )
            if lower.startswith(("run_", "serve_", "prepare_")):
                return (
                    f"{domain}_execution_boundary",
                    f"repo_tooling/{domain}/build.py, repo_tooling/{domain}/server.py, or repo_tooling/{domain}/tests.py",
                    "subprocess/server/test orchestration is an IO boundary and should be isolated from CLI dispatch",
                    f"python tools/run.py {domain} test",
                )
        if "/repo_tooling/android_debug/" in path:
            if lower.startswith(("run_adb", "dump_", "start_", "ensure_", "capture_")):
                return (
                    "android_debug_device_io",
                    "repo_tooling/android_debug/device_io.py",
                    "ADB process calls, device checks, logcat, and capture side effects should sit behind a mockable device IO boundary",
                    "python -m unittest tools.tests.test_android_debug",
                )
            if lower.startswith(("parse_", "build_", "format_", "summarize_", "write_")):
                return (
                    "android_debug_report_rules",
                    "repo_tooling/android_debug/reporting.py",
                    "pure parsing, formatting, and report-writing helpers should be testable without a connected device",
                    "python -m unittest tools.tests.test_android_debug",
                )
        if "/scripts/loc/internal/" in path:
            if "formatter" in path and lower.startswith(("_format_", "_render_", "_responsibility_", "_candidate_", "_validation_")):
                return (
                    "loc_formatter_mode_pack",
                    "scripts/loc/internal/report_formatters/",
                    "scan-mode formatting, candidate rendering, and validation text should move by mode, not one helper at a time",
                    "python tools/scripts/loc/run.py --lang py --responsibility-risk",
                )
        return None


class CppResponsibilityPlugin(ResponsibilityLanguagePlugin):
    import re as _re

    ROLE_NAME_PATTERNS = (
        "Bridge",
        "Codec",
        "Api",
        "Parser",
        "Builder",
        "Factory",
        "Adapter",
        "Facade",
        "Runtime",
        "Registry",
        "Manager",
        "Support",
        "Test",
    )
    TOP_LEVEL_FUNCTION_RE = _re.compile(
        r"^\s*(?:(?:inline|static|constexpr|consteval|extern|friend|virtual)\s+)*"
        r"(?:[A-Za-z_][A-Za-z0-9_:<>\s*&~]+?\s+)?([A-Za-z_~][A-Za-z0-9_:~]*)\s*"
        r"\([^;{}]*\)\s*(?:const)?(?:\s*noexcept(?:\s*\([^)]*\))?)?"
        r"(?:\s*(?:override|final))*"
        r"(?:\s*->\s*[^{]+)?(?:\s*:\s*[^{]+)?\s*\{"
    )
    TOP_LEVEL_TYPE_RE = _re.compile(r"^\s*(?:class|struct|enum(?:\s+class)?)\s+([A-Za-z_][A-Za-z0-9_]*)\b")
    CONTROL_FLOW_NAMES = {"if", "for", "while", "switch", "catch"}
    MODE_BRANCH_PATTERNS = (
        _re.compile(r"\bswitch\s*\([^)]*\b(mode|style|profile|state|phase|kind|flavor)\b"),
        _re.compile(r"\bif\s*\([^)]*\b(mode|style|profile|state|phase|kind|flavor)\b"),
        _re.compile(r"\bcase\b"),
    )
    STATE_SIGNAL_PATTERNS = (
        _re.compile(r"\bstd::mutex\b"),
        _re.compile(r"\bstd::atomic\b"),
        _re.compile(r"\bstd::condition_variable\b"),
        _re.compile(r"\bthread_local\b"),
        _re.compile(r"\bstd::thread\b"),
        _re.compile(r"\bstd::future\b"),
        _re.compile(r"\bstd::promise\b"),
    )
    IO_KIND_PATTERNS = {
        "filesystem": (
            _re.compile(r"\bstd::filesystem\b"),
            _re.compile(r"\b(std::)?(i|o|io)fstream\b"),
            _re.compile(r"\bfopen\s*\("),
        ),
        "console": (
            _re.compile(r"\bstd::(cout|cerr|clog)\b"),
            _re.compile(r"\b(f?printf|puts)\s*\("),
        ),
        "process": (_re.compile(r"\b(std::system|popen|CreateProcess[A-Za-z]*|ShellExecute[A-Za-z]*)\b"),),
        "network": (_re.compile(r"\b(curl|asio|WinHttp|httplib|websocket)\b"),),
        "interop": (
            _re.compile(r"\bJNIEnv\b"),
            _re.compile(r'extern\s+"C"'),
            _re.compile(r"\bj(object|class|string|array|methodID|fieldID)\b"),
        ),
    }
    RULE_HELPER_PATTERNS = (
        _re.compile(
            r"^\s*(?:inline|static|constexpr|consteval|extern|friend|virtual)?\s*"
            r"(?:[A-Za-z_][A-Za-z0-9_:<>\s*&~]+?\s+)?"
            r"(Validate|Check|Normalize|Resolve|Parse|Encode|Decode|Build|Map|Fill|Copy|Take|Poll|Cancel|Destroy|Free|Reset|Convert|Read|Write)"
            r"[A-Za-z_0-9]*\s*\("
        ),
        _re.compile(r"^\s*[A-Z][A-Z0-9_]+\s*="),
    )
    RESPONSIBILITY_VERB_GROUPS = {
        "bridge_convert": ("to", "from", "map", "convert", "copy", "fill", "reset"),
        "validate_parse": ("validate", "check", "parse", "read"),
        "encode_decode_build": ("encode", "decode", "build", "serialize", "render", "write"),
        "lifecycle_async": ("start", "poll", "take", "cancel", "destroy", "free", "release", "close"),
        "fetch_apply": ("get", "set", "load", "store", "apply"),
    }
    INTEROP_KIND_PATTERNS = {
        "jni_surface": (
            _re.compile(r"\bJNIEnv\b"),
            _re.compile(r"\bJNIEXPORT\b"),
            _re.compile(r"\bj(object|class|string|array|methodID|fieldID)\b"),
        ),
        "c_api_surface": (
            _re.compile(r'extern\s+"C"'),
            _re.compile(r'#\s*include\s+[<"][A-Za-z0-9_/]*[A-Za-z0-9_]+_api\.h[>"]'),
            _re.compile(r"^\s*bag_[a-z0-9_]+\s*\("),
        ),
        "marshalling_helpers": (
            _re.compile(r"\b(Get|Set|Call|New)[A-Za-z0-9_]*(Method|Field|Object)\b"),
        ),
    }
    RESOURCE_LIFECYCLE_PATTERNS = (
        _re.compile(r"\bnew\b"),
        _re.compile(r"\bdelete\b"),
        _re.compile(r"\b(std::unique_ptr|std::shared_ptr|std::lock_guard|std::unique_lock)\b"),
        _re.compile(r"\b(Release|Destroy|Cancel|Close|Free)[A-Za-z0-9_]*\b"),
        _re.compile(r"\bGetStringUTFChars\b"),
        _re.compile(r"\bReleaseStringUTFChars\b"),
    )
    INTEROP_LINE_PATTERNS = (
        (_re.compile(r"\bJNIEnv\b"), "jni surface"),
        (_re.compile(r'extern\s+"C"'), "c api surface"),
        (_re.compile(r"\b(Get|Set|Call|New)[A-Za-z0-9_]*(Method|Field|Object)\b"), "marshalling helper"),
    )

    def build_scorer(self) -> BaseResponsibilityScorer:
        return CppResponsibilityScorer(self.config)

    def collect_metrics(self, *, file_path: Path, text: str) -> ResponsibilityMetrics:
        lines = text.splitlines()
        top_level_symbol_count, top_level_symbol_names = self._count_cpp_symbols(lines)
        return ResponsibilityMetrics(
            line_count=len(lines),
            state_signal_hits=self._count_pattern_hits(text, self.STATE_SIGNAL_PATTERNS),
            top_level_symbol_count=top_level_symbol_count,
            role_kinds=self._collect_role_kinds(top_level_symbol_names, self.ROLE_NAME_PATTERNS),
            mode_branch_hits=sum(
                1 for line in lines if any(pattern.search(line) for pattern in self.MODE_BRANCH_PATTERNS)
            ),
            io_kind_count=self._count_pattern_kinds(text, self.IO_KIND_PATTERNS),
            rule_helper_count=self._count_rule_helpers(lines),
            responsibility_verb_kind_count=self._count_responsibility_verb_kinds(lines),
            interop_surface_hits=self._count_cpp_interop_kinds(file_path, text),
            resource_lifecycle_hits=self._count_pattern_hits(text, self.RESOURCE_LIFECYCLE_PATTERNS),
        )

    def _count_cpp_symbols(self, lines: list[str]) -> tuple[int, list[str]]:
        type_names = [
            match.group(1)
            for raw_line in lines
            if (match := self.TOP_LEVEL_TYPE_RE.match(raw_line.strip()))
        ]
        function_names = [name for name, _, _, _ in self._collect_cpp_symbol_ranges(lines)]
        return len(type_names) + len(function_names), type_names + function_names

    def _count_rule_helpers(self, lines: list[str]) -> int:
        count = 0
        for raw_line in lines:
            stripped = raw_line.strip()
            if not stripped or stripped.startswith(("#", "//")):
                continue
            if any(pattern.match(raw_line) for pattern in self.RULE_HELPER_PATTERNS):
                count += 1
        return count

    def _count_responsibility_verb_kinds(self, lines: list[str]) -> int:
        top_level_function_names = [name.lower() for name, _, _, _ in self._collect_cpp_symbol_ranges(lines)]
        matched_groups = 0
        for prefixes in self.RESPONSIBILITY_VERB_GROUPS.values():
            if any(name.startswith(prefix) for name in top_level_function_names for prefix in prefixes):
                matched_groups += 1
        return matched_groups

    def _count_cpp_interop_kinds(self, file_path: Path | None, text: str) -> int:
        jni_surface = any(pattern.search(text) for pattern in self.INTEROP_KIND_PATTERNS["jni_surface"])
        c_api_surface = any(pattern.search(text) for pattern in self.INTEROP_KIND_PATTERNS["c_api_surface"])
        marshalling_helpers = (
            (jni_surface or "JNIEnv" in text)
            and any(pattern.search(text) for pattern in self.INTEROP_KIND_PATTERNS["marshalling_helpers"])
        )
        count = sum(1 for present in (jni_surface, c_api_surface, marshalling_helpers) if present)
        if file_path is None:
            return count
        path_text = str(file_path).replace("\\", "/").lower()
        if "jni" in path_text and ("JNIEnv" in text or "jobject" in text):
            count = max(count, 1)
        if "audio_api" in path_text and ("bag_api.h" in text or "bag_" in text):
            count = max(count, 1)
        return count

    @staticmethod
    def _append_signature_part(pending_signature_parts: list[str], stripped_line: str) -> list[str]:
        if pending_signature_parts:
            pending_signature_parts.append(stripped_line)
            return pending_signature_parts
        if "(" not in stripped_line or stripped_line.startswith(("return ", "case ")):
            return pending_signature_parts
        return [stripped_line]

    def _maybe_extract_top_level_function_name(self, pending_signature_parts: list[str]) -> str | None:
        if not pending_signature_parts:
            return None
        signature = " ".join(pending_signature_parts)
        if "{" not in signature:
            return None
        match = self.TOP_LEVEL_FUNCTION_RE.match(signature)
        if not match:
            return None
        name = match.group(1).split("::")[-1]
        if name in self.CONTROL_FLOW_NAMES:
            return None
        return name

    @staticmethod
    def _brace_delta(text: str) -> int:
        return text.count("{") - text.count("}")

    @staticmethod
    def _cpp_symbol_kind(first_line: str, name: str) -> str:
        stripped = first_line.strip()
        if "::" in name:
            return "function"
        if first_line.startswith((" ", "\t")) or stripped.endswith(("override {", "final {")):
            return "method"
        return "function"

    def collect_details(
        self,
        *,
        file_path: Path,
        text: str,
        metrics: ResponsibilityMetrics,
        assessment: ResponsibilityAssessment,
    ) -> ResponsibilityDetails:
        del metrics, assessment
        lines = text.splitlines()
        functions = self._collect_cpp_symbol_ranges(lines)
        hotspots = self._collect_cpp_function_hotspots(lines=lines, functions=functions)
        anchors = self._collect_cpp_anchors(lines=lines, functions=functions)
        move_sets = self._collect_cpp_move_sets(file_path=file_path, functions=functions)
        return ResponsibilityDetails(function_hotspots=hotspots, anchors=anchors, move_sets=move_sets)

    def _collect_cpp_symbol_ranges(self, lines: list[str]) -> list[tuple[str, int, int, str]]:
        brace_depth = 0
        pending_signature_parts: list[str] = []
        pending_start = 0
        functions: list[tuple[str, int, int, str]] = []
        current_name: str | None = None
        current_start = 0
        current_kind = "function"
        current_depth = 0
        for index, raw_line in enumerate(lines, start=1):
            stripped = raw_line.strip()
            if not stripped or stripped.startswith(("#", "//")):
                if current_name is not None:
                    current_depth += self._brace_delta(raw_line)
                brace_depth += self._brace_delta(raw_line)
                continue

            if current_name is None:
                if current_name is None:
                    pending_signature_parts = self._append_signature_part(pending_signature_parts, stripped)
                    if pending_signature_parts and pending_start == 0:
                        pending_start = index
                    function_name = self._maybe_extract_top_level_function_name(pending_signature_parts)
                    if function_name is not None:
                        current_name = function_name
                        current_start = pending_start or index
                        current_kind = self._cpp_symbol_kind(lines[current_start - 1], function_name)
                        current_depth = self._brace_delta(" ".join(pending_signature_parts))
                        pending_signature_parts = []
                        pending_start = 0
                    elif ";" in stripped and "{" not in stripped:
                        pending_signature_parts = []
                        pending_start = 0
            else:
                current_depth += self._brace_delta(raw_line)
            brace_depth += self._brace_delta(raw_line)
            if brace_depth < 0:
                brace_depth = 0
            if current_name is not None and current_depth <= 0:
                functions.append((current_name, current_start, index, current_kind))
                current_name = None
                current_start = 0
                current_kind = "function"
                current_depth = 0
        return functions

    def _collect_cpp_function_hotspots(
        self,
        *,
        lines: list[str],
        functions: list[tuple[str, int, int, str]],
    ) -> list[ResponsibilityFunctionHotspot]:
        hotspots: list[ResponsibilityFunctionHotspot] = []
        for name, start_line, end_line, kind in functions:
            function_lines = lines[start_line - 1 : end_line]
            body_text = "\n".join(function_lines)
            line_count = len(function_lines)
            mode_hits = sum(1 for line in function_lines if any(pattern.search(line) for pattern in self.MODE_BRANCH_PATTERNS))
            interop_hits = self._count_cpp_interop_kinds(None, body_text)
            lifecycle_hits = self._count_pattern_hits(body_text, self.RESOURCE_LIFECYCLE_PATTERNS)
            helper_hits = sum(1 for line in function_lines if any(pattern.match(line) for pattern in self.RULE_HELPER_PATTERNS))
            state_hits = self._count_pattern_hits(body_text, self.STATE_SIGNAL_PATTERNS)
            score = 0
            if line_count >= 50:
                score += 1
            if line_count >= 100:
                score += 1
            if mode_hits >= 2:
                score += 1
            if interop_hits >= 1:
                score += 1
            if interop_hits >= 2:
                score += 1
            if lifecycle_hits >= 2:
                score += 1
            if helper_hits >= 1:
                score += 1
            if state_hits >= 2:
                score += 1
            risks: list[str] = []
            evidence: list[str] = [f"lines {line_count}"]
            if interop_hits >= 1:
                risks.append("interop_surface_breadth")
                evidence.append(f"interop kinds {interop_hits}")
            if lifecycle_hits >= 2:
                risks.append("resource_lifecycle_density")
                evidence.append(f"lifecycle hits {lifecycle_hits}")
            if helper_hits >= 1:
                risks.append("rule_helper_density")
                evidence.append(f"helpers {helper_hits}")
            if mode_hits >= 2:
                risks.append("mode_branching")
                evidence.append(f"mode branches {mode_hits}")
            if state_hits >= 2:
                evidence.append(f"state hits {state_hits}")
            if line_count >= 50:
                evidence.append("large function")
            if score < 2 or not risks:
                continue
            summary_parts: list[str] = []
            if interop_hits >= 1:
                summary_parts.append("桥接/interop 面偏宽")
            if lifecycle_hits >= 2:
                summary_parts.append("生命周期逻辑偏密")
            if helper_hits >= 1:
                summary_parts.append("规则/helper 偏密")
            if mode_hits >= 2:
                summary_parts.append("mode/style/state 分支较多")
            hotspots.append(
                ResponsibilityFunctionHotspot(
                    name=name,
                    kind=kind,
                    start_line=start_line,
                    end_line=end_line,
                    score=score,
                    summary="；".join(summary_parts),
                    risks=risks,
                    evidence=evidence,
                )
            )
        hotspots.sort(key=lambda item: (-item.score, item.start_line))
        return hotspots[:4]

    def _collect_cpp_move_sets(
        self,
        *,
        file_path: Path,
        functions: list[tuple[str, int, int, str]],
    ) -> list[ResponsibilityMoveSet]:
        groups: dict[tuple[str, str, str, str, int], list[ResponsibilityMoveSetHelper]] = {}
        for name, start_line, end_line, kind in functions:
            spec = self._cpp_move_set_spec(path=str(file_path), function_name=name)
            if spec is None:
                continue
            groups.setdefault(spec, []).append(
                ResponsibilityMoveSetHelper(
                    name=name,
                    kind=kind,
                    start_line=start_line,
                    end_line=end_line,
                )
            )

        move_sets: list[tuple[int, int, ResponsibilityMoveSet]] = []
        for (name, target_boundary, reason, validation, rank), helpers in groups.items():
            helpers.sort(key=lambda item: (item.start_line, item.name))
            if len(helpers) < 2:
                continue
            first_line = helpers[0].start_line
            move_sets.append(
                (
                    rank,
                    first_line,
                    ResponsibilityMoveSet(
                        name=name,
                        target_boundary=target_boundary,
                        helpers=helpers,
                        reason=reason,
                        validation=validation,
                    ),
                )
            )
        move_sets.sort(key=lambda item: (item[0], item[1], item[2].name))
        return [item for _, _, item in move_sets[:5]]

    def _cpp_move_set_spec(
        self,
        *,
        path: str,
        function_name: str,
    ) -> tuple[str, str, str, str, int] | None:
        lower_path = path.replace("\\", "/").lower()
        lower_name = function_name.lower()
        validation = self._cpp_move_set_validation(path)

        if "jni_bridge_helpers" in lower_path:
            if self._name_has_any(lower_name, ("list", "array", "items")):
                return (
                    "jni_list_marshalling",
                    "jni_bridge_list_marshalling.cpp/.h",
                    "Move JNI list/array builders together so the bridge keeps one DTO list boundary.",
                    validation,
                    20,
                )
            if self._name_has_any(lower_name, ("follow", "viewdata", "entry", "timeline", "token")):
                return (
                    "jni_follow_marshalling",
                    "jni_bridge_follow_marshalling.cpp/.h",
                    "Move follow/view-data DTO helpers together instead of splitting individual JNI helpers.",
                    validation,
                    10,
                )
            if self._name_has_any(lower_name, ("to", "from", "map", "copy", "fill", "reset", "string")):
                return (
                    "jni_scalar_marshalling",
                    "jni_bridge_marshalling.cpp/.h",
                    "Move scalar conversion helpers together behind the JNI bridge surface.",
                    validation,
                    30,
                )

        if "transport" in lower_path and (
            "transport_impl" in lower_path or "encode_operation" in lower_path
        ):
            if self._name_has_any(lower_name, ("workplan", "work_plan", "work", "budget")):
                return (
                    "encode_operation_work_plan",
                    "module bag.transport.encode_work_plan -> modules/bag/transport/encode_work_plan.cppm + src/transport/encode_work_plan.cpp",
                    "Move work accounting helpers as one work-plan package.",
                    validation,
                    10,
                )
            if self._name_has_any(lower_name, ("prepare", "render", "postprocess", "finalize", "complete")):
                return (
                    "encode_operation_steps",
                    "module bag.transport.encode_operation_steps -> modules/bag/transport/encode_operation_steps.cppm + src/transport/encode_operation_steps.cpp",
                    "Move staged encode helpers together so the operation owner delegates phase steps.",
                    validation,
                    20,
                )
            if self._name_has_any(lower_name, ("pump", "run", "cancel", "take", "poll", "create", "destroy")):
                return (
                    "encode_operation_lifecycle",
                    "module impl bag.transport.facade -> src/transport/transport_encode_operation.cpp",
                    "Move operation state transitions together and keep public transport entrypoints thin.",
                    validation,
                    30,
                )
            if self._name_has_any(lower_name, ("validate", "check", "config")):
                return (
                    "transport_validation",
                    "module impl bag.transport.facade -> src/transport/transport_validation.cpp",
                    "Move validation helpers together before changing transport behavior.",
                    validation,
                    40,
                )

        if "flash" in lower_path and "phy_clean" in lower_path:
            if self._name_has_any(lower_name, ("normalize", "formal", "budget", "config", "layout", "voicing", "signal", "flavor", "trim", "make", "build")):
                return (
                    "flash_phy_rules",
                    "module bag.flash.phy_rules -> modules/bag/flash/phy_rules.cppm + src/flash/phy_rules.cpp",
                    "Move Flash config/layout/budget helpers as one rules package.",
                    validation,
                    10,
                )
            if self._name_has_any(lower_name, ("decoder", "decode", "poll", "push", "reset")):
                return (
                    "flash_phy_decode",
                    "module bag.flash.phy_decode -> modules/bag/flash/phy_decode.cppm + src/flash/phy_decode.cpp",
                    "Move decoder state and decode helpers together instead of splitting Poll/Reset/Decode separately.",
                    validation,
                    20,
                )
            if self._name_has_any(lower_name, ("encode", "payload", "pcm", "render")):
                return (
                    "flash_phy_encode",
                    "module bag.flash.phy_encode -> modules/bag/flash/phy_encode.cppm + src/flash/phy_encode.cpp",
                    "Move encode/render helpers together and keep the public facade shallow.",
                    validation,
                    30,
                )

        if "flash" in lower_path and "signal" in lower_path:
            if self._name_has_any(lower_name, ("layout", "payload", "cadence", "budget")):
                return (
                    "flash_signal_layout",
                    "module bag.flash.signal_layout -> modules/bag/flash/signal_layout.cppm + src/flash/signal_layout.cpp",
                    "Move signal layout and payload cadence helpers as one package.",
                    validation,
                    10,
                )
            if "decode" in lower_name:
                return (
                    "flash_signal_decode",
                    "module bag.flash.signal_decode -> modules/bag/flash/signal_decode.cppm + src/flash/signal_decode.cpp",
                    "Move signal decode helpers together.",
                    validation,
                    20,
                )
            if self._name_has_any(lower_name, ("normalize", "profile", "flavor", "config", "make")):
                return (
                    "flash_signal_rules",
                    "module bag.flash.signal_rules -> modules/bag/flash/signal_rules.cppm + src/flash/signal_rules.cpp",
                    "Move profile/flavor normalization and config helpers together.",
                    validation,
                    30,
                )

        if "mini" in lower_path and "phy_clean" in lower_path:
            if self._name_has_any(lower_name, ("morse", "valid", "config", "make", "count")):
                return (
                    "mini_morse_rules",
                    "module bag.mini.morse_rules -> modules/bag/mini/morse_rules.cppm + src/mini/morse_rules.cpp",
                    "Move Morse config and payload rule helpers together.",
                    validation,
                    10,
                )
            if self._name_has_any(lower_name, ("decoder", "decode", "poll", "push", "reset", "chunk", "flush")):
                return (
                    "mini_phy_decode",
                    "module bag.mini.phy_decode -> modules/bag/mini/phy_decode.cppm + src/mini/phy_decode.cpp",
                    "Move Mini decoder state, RMS, and payload decode helpers together.",
                    validation,
                    20,
                )
            if self._name_has_any(lower_name, ("renderer", "render", "tone", "segment", "append", "progress", "lerp", "unit")):
                return (
                    "mini_tone_renderer",
                    "module bag.mini.tone_renderer -> modules/bag/mini/tone_renderer.cppm + src/mini/tone_renderer.cpp",
                    "Move tone rendering and progress helpers together.",
                    validation,
                    30,
                )

        if ("pro" in lower_path or "ultra" in lower_path) and "phy_clean" in lower_path:
            mode_name = "pro" if "pro" in lower_path else "ultra"
            if self._name_has_any(lower_name, ("valid", "config", "template", "bank", "make")):
                return (
                    f"{mode_name}_phy_rules",
                    f"module bag.{mode_name}.phy_rules -> modules/bag/{mode_name}/phy_rules.cppm + src/{mode_name}/phy_rules.cpp",
                    f"Move {mode_name} config/template rule helpers together.",
                    validation,
                    10,
                )
            if self._name_has_any(lower_name, ("decoder", "decode", "poll", "push", "reset", "goertzel", "strongest")):
                return (
                    f"{mode_name}_phy_decode",
                    f"module bag.{mode_name}.phy_decode -> modules/bag/{mode_name}/phy_decode.cppm + src/{mode_name}/phy_decode.cpp",
                    f"Move {mode_name} decoder state and tone detection helpers together.",
                    validation,
                    20,
                )
            if self._name_has_any(lower_name, ("encode", "render", "symbol", "progress", "lerp", "work")):
                return (
                    f"{mode_name}_phy_encode",
                    f"module bag.{mode_name}.phy_encode -> modules/bag/{mode_name}/phy_encode.cppm + src/{mode_name}/phy_encode.cpp",
                    f"Move {mode_name} symbol rendering and encode progress helpers together.",
                    validation,
                    30,
                )

        if "audio_io/src/wav_io" in lower_path or "wav_io_bytes" in lower_path:
            if self._name_has_any(lower_name, ("metadata", "version", "createdat", "segment")):
                if self._name_has_any(lower_name, ("parse", "read", "valid", "is")):
                    return (
                        "wav_metadata_parse_rules",
                        "module bag.wav.metadata_parse -> modules/bag/wav/metadata_parse.cppm + src/wav/metadata_parse.cpp",
                        "Move WAV metadata parsing and validation helpers together.",
                        validation,
                        10,
                    )
                if self._name_has_any(lower_name, ("build", "write", "serialize")):
                    return (
                        "wav_metadata_build_rules",
                        "module bag.wav.metadata_build -> modules/bag/wav/metadata_build.cppm + src/wav/metadata_build.cpp",
                        "Move WAV metadata serialization helpers together.",
                        validation,
                        20,
                    )
            if self._name_has_any(lower_name, ("read", "parse")):
                return (
                    "wav_byte_parse",
                    "module bag.wav.bytes_parse -> modules/bag/wav/bytes_parse.cppm + src/wav/bytes_parse.cpp",
                    "Move WAV byte readers and parse flow together.",
                    validation,
                    30,
                )
            if self._name_has_any(lower_name, ("write", "serialize", "build")):
                return (
                    "wav_byte_build",
                    "module bag.wav.bytes_build -> modules/bag/wav/bytes_build.cppm + src/wav/bytes_build.cpp",
                    "Move WAV byte writers and serialization flow together.",
                    validation,
                    40,
                )

        if self._name_has_any(lower_name, ("validate", "check", "normalize", "resolve", "parse", "map", "fill", "copy", "build", "make", "isvalid")):
            return (
                "domain_rules",
                "module-first helper boundary near the owning domain",
                "Move reusable rule/build/map helpers as a named package.",
                validation,
                80,
            )
        if self._name_has_any(lower_name, ("decode", "poll")):
            return (
                "decode_flow",
                "module-first decode boundary near the owning domain",
                "Move decode helpers as one package.",
                validation,
                90,
            )
        if self._name_has_any(lower_name, ("encode", "render")):
            return (
                "encode_flow",
                "module-first encode boundary near the owning domain",
                "Move encode/render helpers as one package.",
                validation,
                100,
            )
        return None

    @staticmethod
    def _name_has_any(lower_name: str, needles: tuple[str, ...]) -> bool:
        return any(needle in lower_name for needle in needles)

    @staticmethod
    def _cpp_move_set_validation(path: str) -> str:
        normalized_path = path.replace("\\", "/")
        if "apps/audio_android/app/src/main/cpp" in normalized_path:
            return "python tools/run.py android assemble-debug"
        if "libs/audio_api" in normalized_path:
            return "python tools/run.py test-lib audio_api --build-dir build/dev"
        if "libs/audio_core" in normalized_path:
            return "python tools/run.py test-lib audio_core --build-dir build/dev"
        if "libs/audio_io" in normalized_path:
            return "python tools/run.py test-lib audio_io --build-dir build/dev"
        return "python tools/run.py verify --build-dir build/dev --skip-android"

    def _collect_cpp_anchors(
        self,
        *,
        lines: list[str],
        functions: list[tuple[str, int, int, str]],
    ) -> list[ResponsibilityAnchor]:
        anchors: list[ResponsibilityAnchor] = []
        function_ranges = [(start, end, name) for name, start, end, _ in functions]
        seen_keys: set[tuple[str, str, str]] = set()
        owner_issue_counts: dict[tuple[str, str], int] = {}
        for index, raw_line in enumerate(lines, start=1):
            stripped = raw_line.strip()
            if not stripped or stripped.startswith(("#", "//")):
                continue
            owner = self._anchor_owner(function_ranges, index)
            if any(pattern.search(raw_line) for pattern in self.MODE_BRANCH_PATTERNS):
                self._append_anchor(
                    anchors=anchors,
                    seen_keys=seen_keys,
                    owner_issue_counts=owner_issue_counts,
                    anchor=ResponsibilityAnchor(
                        line=index,
                        label=owner,
                        issue="mode/style/state 分支出现在这里",
                        evidence=stripped,
                    ),
                    per_owner_issue_limit=2,
                )
                continue
            if any(pattern.match(raw_line) for pattern in self.RULE_HELPER_PATTERNS):
                self._append_anchor(
                    anchors=anchors,
                    seen_keys=seen_keys,
                    owner_issue_counts=owner_issue_counts,
                    anchor=ResponsibilityAnchor(
                        line=index,
                        label=owner,
                        issue="规则/helper 逻辑出现在这里",
                        evidence=stripped,
                    ),
                    per_owner_issue_limit=2,
                )
                continue
            for pattern, label in self.INTEROP_LINE_PATTERNS:
                if pattern.search(raw_line):
                    self._append_anchor(
                        anchors=anchors,
                        seen_keys=seen_keys,
                        owner_issue_counts=owner_issue_counts,
                        anchor=ResponsibilityAnchor(
                            line=index,
                            label=owner,
                            issue=f"{label} 信号出现在这里",
                            evidence=stripped,
                        ),
                        per_owner_issue_limit=2,
                    )
                    break
            if len(anchors) >= 8:
                break
        return anchors


def create_responsibility_language_plugin(
    config: LanguageConfig,
) -> ResponsibilityLanguagePlugin | None:
    if config.lang == "kt":
        return KotlinResponsibilityPlugin(config)
    if config.lang == "py":
        return PythonResponsibilityPlugin(config)
    if config.lang == "cpp":
        return CppResponsibilityPlugin(config)
    return None
