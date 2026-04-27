from __future__ import annotations

from abc import ABC, abstractmethod
from pathlib import Path

from .config import LanguageConfig
from .responsibility_metrics import ResponsibilityMetrics
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
        r"^\s*(?:private|internal|public)?\s*fun\s+([A-Za-z_][A-Za-z0-9_]*)\b"
    )

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
        _re.compile(r"\bsubprocess\b"),
        _re.compile(r"\brequests\b"),
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
        "process": (_re.compile(r"\bsubprocess\b"),),
        "network": (
            _re.compile(r"\brequests\b"),
            _re.compile(r"\burllib\b"),
            _re.compile(r"\bhttpx\b"),
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
        r"(?:\s*->\s*[^{]+)?\s*\{"
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
            _re.compile(r"\b[a-z0-9_]+_api\.h\b"),
            # Avoid flagging generic std::string_view usage as FFI surface noise.
            _re.compile(r"\b[a-z0-9_]+_(view|owned_string)\b"),
        ),
        "marshalling_helpers": (
            _re.compile(r"\b(To|From|JStringTo|VectorTo|ShortArrayTo)[A-Za-z0-9_]*\b"),
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

    def build_scorer(self) -> BaseResponsibilityScorer:
        return CppResponsibilityScorer(self.config)

    def collect_metrics(self, *, file_path: Path, text: str) -> ResponsibilityMetrics:
        del file_path
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
            interop_surface_hits=self._count_pattern_kinds(text, self.INTEROP_KIND_PATTERNS),
            resource_lifecycle_hits=self._count_pattern_hits(text, self.RESOURCE_LIFECYCLE_PATTERNS),
        )

    def _count_top_level_symbols(self, lines: list[str]) -> tuple[int, list[str]]:
        # Keep the parser shallow on purpose: we only want a conservative file-level
        # signal, not an AST-perfect count.
        depth = 0
        symbol_names: list[str] = []
        pending_signature_parts: list[str] = []
        for raw_line in lines:
            stripped = raw_line.strip()
            if depth == 0 and stripped and not stripped.startswith(("#", "//")):
                type_match = self.TOP_LEVEL_TYPE_RE.match(raw_line)
                if type_match:
                    symbol_names.append(type_match.group(1))
                else:
                    pending_signature_parts = self._append_signature_part(pending_signature_parts, stripped)
                    function_name = self._maybe_extract_top_level_function_name(pending_signature_parts)
                    if function_name is not None:
                        symbol_names.append(function_name)
                        pending_signature_parts = []
                    elif ";" in stripped and "{" not in stripped:
                        pending_signature_parts = []
            elif depth > 0:
                pending_signature_parts = []
            depth += raw_line.count("{") - raw_line.count("}")
            if depth < 0:
                depth = 0
        return len(symbol_names), symbol_names

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
        top_level_function_names = [name.lower() for name in self._collect_top_level_function_names(lines)]
        matched_groups = 0
        for prefixes in self.RESPONSIBILITY_VERB_GROUPS.values():
            if any(name.startswith(prefix) for name in top_level_function_names for prefix in prefixes):
                matched_groups += 1
        return matched_groups

    def _collect_top_level_function_names(self, lines: list[str]) -> list[str]:
        depth = 0
        pending_signature_parts: list[str] = []
        function_names: list[str] = []
        for raw_line in lines:
            stripped = raw_line.strip()
            if depth == 0 and stripped and not stripped.startswith(("#", "//")):
                pending_signature_parts = self._append_signature_part(pending_signature_parts, stripped)
                function_name = self._maybe_extract_top_level_function_name(pending_signature_parts)
                if function_name is not None:
                    function_names.append(function_name)
                    pending_signature_parts = []
                elif ";" in stripped and "{" not in stripped:
                    pending_signature_parts = []
            elif depth > 0:
                pending_signature_parts = []
            depth += raw_line.count("{") - raw_line.count("}")
            if depth < 0:
                depth = 0
        return function_names

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
