from __future__ import annotations

from pathlib import Path

from .responsibility_metrics import (
    ResponsibilityAnchor,
    ResponsibilityAssessment,
    ResponsibilityDetails,
    ResponsibilityFunctionHotspot,
    ResponsibilityMetrics,
    ResponsibilityMoveSet,
    ResponsibilityMoveSetHelper,
)
from .responsibility_plugin_base import ResponsibilityLanguagePlugin
from .responsibility_scoring import (
    BaseResponsibilityScorer,
    CppResponsibilityScorer,
    KotlinResponsibilityScorer,
    PythonResponsibilityScorer,
)
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
