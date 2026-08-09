from __future__ import annotations

import re
from pathlib import Path

from .responsibility_metrics import ResponsibilityDetails, ResponsibilityMetrics
from .responsibility_plugin_base import ResponsibilityLanguagePlugin
from .responsibility_scoring import BaseResponsibilityScorer, RustResponsibilityScorer


class RustResponsibilityPlugin(ResponsibilityLanguagePlugin):
    ROLE_NAME_PATTERNS = (
        "Adapter",
        "Guard",
        "Config",
        "Command",
        "Output",
        "Metadata",
        "Operation",
        "Decoder",
        "Encoder",
        "Runtime",
    )
    TOP_LEVEL_SYMBOL_RE = re.compile(
        r"^(?:pub(?:\([^)]*\))?\s+)?(?:async\s+)?"
        r"(?:fn|struct|enum|trait|impl|type|const|static|mod)\s+([A-Za-z_][A-Za-z0-9_]*)"
    )
    TOP_LEVEL_FUNCTION_RE = re.compile(
        r"^(?:pub(?:\([^)]*\))?\s+)?(?:async\s+)?fn\s+([A-Za-z_][A-Za-z0-9_]*)"
    )
    STATE_SIGNAL_PATTERNS = (
        re.compile(r"\b(?:Arc|Mutex|RwLock|RefCell|Cell)<"),
        re.compile(r"\bstatic\s+mut\b"),
    )
    MODE_BRANCH_PATTERNS = (
        re.compile(r"\bmatch\s+(?:self\.)?(?:mode|state|phase|kind|style)\b"),
        re.compile(r"\bif\b[^\n]*\b(?:mode|state|phase|kind|style)\b"),
    )
    IO_KIND_PATTERNS = {
        "filesystem": (re.compile(r"\bstd::fs\b|\bFile::|\bPathBuf\b"),),
        "console": (re.compile(r"\b(?:print|println|eprint|eprintln)!"),),
        "process": (re.compile(r"\bstd::process::Command\b"),),
        "network": (re.compile(r"\b(?:TcpStream|UdpSocket|reqwest|hyper)::?"),),
        "ffi": (re.compile(r"extern\s+\"C\"|\*const\s+|\*mut\s+"),),
    }
    RULE_HELPER_RE = re.compile(
        r"^(?:pub(?:\([^)]*\))?\s+)?(?:async\s+)?fn\s+"
        r"(?:validate|check|normalize|resolve|parse|encode|decode|build|map|poll|take|free|drop)[A-Za-z0-9_]*"
    )
    VERB_GROUPS = {
        "convert": ("to_", "from_", "map_", "convert_"),
        "validate": ("validate", "check", "normalize", "parse"),
        "codec": ("encode", "decode", "build", "render"),
        "lifecycle": ("create", "poll", "take", "cancel", "free", "drop"),
        "io": ("read", "write", "print", "load", "save"),
    }
    RESOURCE_PATTERNS = (
        re.compile(r"\bimpl\s+Drop\b"),
        re.compile(r"\b(?:drop|free|take|cancel|release)\s*\("),
        re.compile(r"\b(?:Box|Arc|Rc|Mutex|RwLock)<"),
    )

    def build_scorer(self) -> BaseResponsibilityScorer:
        return RustResponsibilityScorer(self.config)

    def collect_metrics(self, *, file_path: Path, text: str) -> ResponsibilityMetrics:
        del file_path
        lines = text.splitlines()
        names = [
            match.group(1)
            for line in lines
            if not line.startswith((" ", "\t"))
            and (match := self.TOP_LEVEL_SYMBOL_RE.match(line.strip()))
        ]
        function_names = [
            match.group(1).lower()
            for line in lines
            if not line.startswith((" ", "\t"))
            and (match := self.TOP_LEVEL_FUNCTION_RE.match(line.strip()))
        ]
        verb_kinds = sum(
            any(name.startswith(prefix) for name in function_names for prefix in prefixes)
            for prefixes in self.VERB_GROUPS.values()
        )
        return ResponsibilityMetrics(
            line_count=len(lines),
            state_signal_hits=self._count_pattern_hits(text, self.STATE_SIGNAL_PATTERNS),
            top_level_symbol_count=len(names),
            role_kinds=self._collect_role_kinds(names, self.ROLE_NAME_PATTERNS),
            mode_branch_hits=sum(
                any(pattern.search(line) for pattern in self.MODE_BRANCH_PATTERNS)
                for line in lines
            ),
            io_kind_count=self._count_pattern_kinds(text, self.IO_KIND_PATTERNS),
            rule_helper_count=sum(bool(self.RULE_HELPER_RE.match(line.strip())) for line in lines),
            responsibility_verb_kind_count=verb_kinds,
            interop_surface_hits=sum(
                bool(pattern.search(text))
                for pattern in (
                    re.compile(r"extern\s+\"C\""),
                    re.compile(r"\bunsafe\b"),
                    re.compile(r"\*(?:const|mut)\s+"),
                )
            ),
            resource_lifecycle_hits=self._count_pattern_hits(text, self.RESOURCE_PATTERNS),
            dependency_fanout=self.dependency_fanout(text=text),
        )

    def collect_details(self, **kwargs) -> ResponsibilityDetails:
        del kwargs
        return ResponsibilityDetails(function_hotspots=[], anchors=[], move_sets=[])
