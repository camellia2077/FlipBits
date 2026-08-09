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
            dependency_fanout=self.dependency_fanout(text=text),
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
