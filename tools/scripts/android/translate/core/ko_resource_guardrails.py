from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re


_RAW_ESCAPE_PATTERNS: tuple[re.Pattern[str], ...] = (
    re.compile(r'(?<!\\)\\"'),
    re.compile(r"(?<!\\)\\'"),
    re.compile(r"(?<!\\)\\u(?![0-9a-fA-F]{4})"),
)


@dataclass(frozen=True)
class KoGuardrailIssue:
    file: str
    reason: str
    sample: str


def autofix_ko_xml_text(xml_text: str) -> tuple[str, int]:
    updated = xml_text
    changes = 0

    if '\\"' in updated:
        updated = updated.replace('\\"', "&quot;")
        changes += 1
    if "\\'" in updated:
        updated = updated.replace("\\'", "'")
        changes += 1
    return updated, changes


def validate_ko_values_xml_files(files: list[Path]) -> list[KoGuardrailIssue]:
    issues: list[KoGuardrailIssue] = []
    for path in files:
        text = path.read_text(encoding="utf-8")
        for pattern in _RAW_ESCAPE_PATTERNS:
            match = pattern.search(text)
            if match is None:
                continue
            start = max(0, match.start() - 20)
            end = min(len(text), match.end() + 40)
            snippet = text[start:end].replace("\n", " ").replace("\r", " ")
            issues.append(
                KoGuardrailIssue(
                    file=str(path),
                    reason=f"Detected unsafe escape pattern: {pattern.pattern}",
                    sample=snippet,
                )
            )
            break
    return issues
