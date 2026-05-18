from __future__ import annotations

from dataclasses import dataclass
import re


_LINE_RE = re.compile(r"^\S+\s+\S+\s+D/TabAutomation\(\s*\d+\):\s+(?P<message>.*)$")


@dataclass(frozen=True)
class TabEvent:
    kind: str
    fields: dict[str, str]


def parse_event(line: str) -> TabEvent | None:
    match = _LINE_RE.match(line.strip())
    if not match:
        return None
    message = match.group("message")
    parts = message.split()
    if not parts:
        return None
    kind = parts[0]
    fields: dict[str, str] = {}
    for token in parts[1:]:
        if "=" not in token:
            continue
        key, value = token.split("=", 1)
        fields[key] = value
    return TabEvent(kind=kind, fields=fields)


def build_summary(events: list[TabEvent]) -> str:
    received = next((event for event in reversed(events) if event.kind == "received"), None)
    language = next((event for event in reversed(events) if event.kind == "languageApplied"), None)
    selected = next((event for event in reversed(events) if event.kind == "tabSelected"), None)

    lines = ["# Tab Capture Summary", ""]
    if received is None:
        lines.append("No `TabAutomation` events were captured.")
        return "\n".join(lines) + "\n"

    lines.append(
        f"- Request: tab={received.fields.get('tab', 'unknown')} requestId={received.fields.get('requestId', 'unknown')}"
    )
    if language is not None:
        lines.append(f"- Language override: {language.fields.get('language', 'unknown')}")
    if selected is not None:
        lines.append(
            f"- Result: requested={selected.fields.get('requested', 'unknown')} "
            f"selected={selected.fields.get('selected', 'unknown')}"
        )
    lines.append(f"- Event count: {len(events)}")
    lines.append("")
    return "\n".join(lines)
