from __future__ import annotations

from dataclasses import dataclass
import re


_SAVED_LINE_RE = re.compile(r"^\S+\s+\S+\s+D/SavedAudioAutomation\(\s*\d+\):\s+(?P<message>.*)$")
_PERF_LINE_RE = re.compile(r"^\S+\s+\S+\s+D/SavedAudioPerf\(\s*\d+\):\s+(?P<message>.*)$")


@dataclass(frozen=True)
class SavedAudioEvent:
    tag: str
    kind: str
    fields: dict[str, str]


def parse_event(line: str) -> SavedAudioEvent | None:
    stripped = line.strip()
    match = _SAVED_LINE_RE.match(stripped)
    tag = "SavedAudioAutomation"
    if match is None:
        match = _PERF_LINE_RE.match(stripped)
        tag = "SavedAudioPerf"
    if match is None:
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
    return SavedAudioEvent(tag=tag, kind=kind, fields=fields)


def build_summary(events: list[SavedAudioEvent]) -> str:
    lines = ["# Saved Audio Capture Summary", ""]
    if not events:
        lines.append("No `SavedAudioAutomation` or `SavedAudioPerf` events were captured.")
        return "\n".join(lines) + "\n"

    selection = next((event for event in reversed(events) if event.kind == "selectionResolved"), None)
    applied = next((event for event in reversed(events) if event.kind == "selectionApplied"), None)
    detail = next((event for event in reversed(events) if event.kind == "openDetail"), None)
    hydrate = next((event for event in reversed(events) if event.kind == "selectionHydrateEnd"), None)
    gateway = next((event for event in reversed(events) if event.kind == "gatewayLoadEnd"), None)

    if selection is not None:
        lines.append(
            f"- Selection target: itemId={selection.fields.get('targetItemId', '')} "
            f"name={selection.fields.get('targetName', '')}"
        )
    if applied is not None:
        lines.append(
            f"- Selection applied: loaded={applied.fields.get('loaded', 'unknown')} "
            f"elapsedMs={applied.fields.get('elapsedMs', 'unknown')}"
        )
    if detail is not None:
        lines.append(
            f"- Detail sheet: visible={detail.fields.get('detailVisible', 'unknown')} "
            f"elapsedMs={detail.fields.get('elapsedMs', 'unknown')}"
        )
    if gateway is not None:
        lines.append(f"- Gateway load: elapsedMs={gateway.fields.get('elapsedMs', 'unknown')}")
    if hydrate is not None:
        lines.append(f"- Hydration: elapsedMs={hydrate.fields.get('elapsedMs', 'unknown')}")
    lines.append(f"- Event count: {len(events)}")
    lines.append("")
    return "\n".join(lines)
