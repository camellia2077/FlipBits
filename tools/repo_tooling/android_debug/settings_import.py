from __future__ import annotations

from dataclasses import dataclass
import re


_LINE_RE = re.compile(r"^\S+\s+\S+\s+D/TabAutomation\(\s*\d+\):\s+(?P<message>.*)$")


@dataclass(frozen=True)
class SettingsImportEvent:
    kind: str
    fields: dict[str, str]


def parse_event(line: str) -> SettingsImportEvent | None:
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
    return SettingsImportEvent(kind=kind, fields=fields)


def build_summary(events: list[SettingsImportEvent]) -> str:
    received = next((event for event in reversed(events) if event.kind == "received"), None)
    copied = next((event for event in reversed(events) if event.kind == "copyResolved"), None)
    parsed = next((event for event in reversed(events) if event.kind == "importParsed"), None)
    batch_preview = next((event for event in reversed(events) if event.kind == "batchImportPreview"), None)
    duplicate = next((event for event in reversed(events) if event.kind == "duplicatePrompt"), None)
    applied = next((event for event in reversed(events) if event.kind == "importApplied"), None)

    lines = ["# Settings Import Capture Summary", ""]
    if received is None:
        lines.append("No `TabAutomation` settings-import events were captured.")
        return "\n".join(lines) + "\n"

    lines.append(
        f"- Request: confirm={received.fields.get('confirm', 'unknown')} "
        f"scope={received.fields.get('importScope', 'unknown')} "
        f"requestId={received.fields.get('requestId', 'unknown')}"
    )
    if copied is not None:
        lines.append(
            f"- Copied preset(s): scope={copied.fields.get('scope', 'unknown')} "
            f"count={copied.fields.get('presetCount', 'unknown')} "
            f"name={copied.fields.get('name', 'unknown')} "
            f"primary={copied.fields.get('primary', 'unknown')}"
        )
    if parsed is not None:
        lines.append(
            f"- Parsed import: scope={parsed.fields.get('scope', 'unknown')} "
            f"blocks={parsed.fields.get('blocks', 'unknown')} "
            f"name={parsed.fields.get('name', 'unknown')}"
        )
    if batch_preview is not None:
        lines.append(
            f"- Batch preview: duplicateCount={batch_preview.fields.get('duplicateCount', 'unknown')} "
            f"newCount={batch_preview.fields.get('newCount', 'unknown')}"
        )
    if duplicate is not None:
        lines.append(
            f"- Duplicate prompt: shown={duplicate.fields.get('shown', 'unknown')} "
            f"presetId={duplicate.fields.get('duplicatePresetId', '')}"
        )
    if applied is not None:
        lines.append(
            f"- Applied action: {applied.fields.get('action', 'unknown')} "
            f"scope={applied.fields.get('scope', 'unknown')} "
            f"blocks={applied.fields.get('blocks', 'unknown')} "
            f"presetCount={applied.fields.get('presetCount', 'unknown')}"
        )
    lines.append(f"- Event count: {len(events)}")
    lines.append("")
    return "\n".join(lines)
