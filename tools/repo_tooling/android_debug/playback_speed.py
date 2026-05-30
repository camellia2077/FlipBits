from __future__ import annotations

from dataclasses import dataclass
import re


KEY_VALUE_RE = re.compile(r"([A-Za-z][A-Za-z0-9/]*)=([^ ]+)")
TIME_RE = re.compile(r"^(\d\d-\d\d \d\d:\d\d:\d\d\.\d{3})")
LOG_LINE_RE = re.compile(r"^[\d-]+\s+[\d:.]+\s+[DWE]/(?P<tag>[A-Za-z0-9_]+)\(\s*\d+\):\s+(?P<message>.*)$")
TAGS = {
    "PlaybackSpeedDiag",
    "PlaybackCoordDiag",
    "AudioPlayerDiag",
    "AudioTrackTransport",
    "PlaybackAutomation",
}


@dataclass(frozen=True)
class Event:
    time: str
    tag: str
    kind: str
    fields: dict[str, str]
    raw: str


def parse_event(line: str) -> Event | None:
    if not any(tag in line for tag in TAGS):
        return None
    stripped = line.strip()
    match = LOG_LINE_RE.match(stripped)
    if match is None:
        return None
    tag = match.group("tag")
    if tag not in TAGS:
        return None
    message = match.group("message")
    kind = message.split(" ", 1)[0] if message else ""
    time_match = TIME_RE.match(stripped)
    fields = {key: value for key, value in KEY_VALUE_RE.findall(message)}
    return Event(
        time=time_match.group(1) if time_match else "",
        tag=tag,
        kind=kind,
        fields=fields,
        raw=stripped,
    )


def build_summary(events: list[Event], max_rows: int) -> str:
    lines: list[str] = ["# Playback Speed Capture Summary", ""]
    if not events:
        lines.append("No playback speed diagnostic events were captured.")
        lines.append("")
        return "\n".join(lines)

    lines.append(f"- Event count: {len(events)}")
    for tag in sorted({event.tag for event in events}):
        lines.append(f"- {tag}: {sum(1 for event in events if event.tag == tag)}")

    speed_requests = [event for event in events if event.tag == "PlaybackSpeedDiag" and event.kind == "select"]
    coord_results = [event for event in events if event.tag == "PlaybackSpeedDiag" and event.kind == "coordinatorApplied"]
    track_results = [event for event in events if event.tag == "AudioTrackTransport"]
    rejected = [event for event in events if event.kind.endswith("Rejected")]

    if speed_requests:
        latest = speed_requests[-1]
        lines.append(
            "- Latest UI request: "
            f"time={latest.time} source={field(latest, 'source')} "
            f"requested={field(latest, 'requested')} previous={field(latest, 'previous')} "
            f"phase={field(latest, 'phase')}"
        )
    if coord_results:
        latest = coord_results[-1]
        lines.append(
            "- Latest coordinator result: "
            f"time={latest.time} source={field(latest, 'source')} "
            f"requested={field(latest, 'requested')} applied={field(latest, 'applied')}"
        )
    if track_results:
        latest = track_results[-1]
        lines.append(
            "- Latest AudioTrack result: "
            f"time={latest.time} kind={latest.kind} requested={field(latest, 'requested')} "
            f"resolved={field(latest, 'resolved')} actualSpeed={field(latest, 'actualSpeed')} "
            f"actualRate={field(latest, 'actualRate')}"
        )
    if rejected:
        lines.append(f"- Rejected commands: {len(rejected)}")

    lines.append("")
    lines.append("## Rows")
    lines.append("")
    lines.append("| time | tag | kind | source | requested | stored/actual | applied | phase |")
    lines.append("| --- | --- | --- | --- | ---: | ---: | --- | --- |")
    for event in events[:max_rows]:
        lines.append(
            "| "
            f"{event.time} | "
            f"{event.tag} | "
            f"{event.kind} | "
            f"{field(event, 'source')} | "
            f"{field(event, 'requested', field(event, 'speed'))} | "
            f"{field(event, 'stored', field(event, 'actualSpeed', field(event, 'to')))} | "
            f"{field(event, 'applied', field(event, 'fallbackApplied'))} | "
            f"{field(event, 'phase')} |"
        )

    lines.append("")
    lines.append("## Filtered Log Lines")
    lines.append("")
    for event in events:
        lines.append(f"- `{event.raw}`")
    lines.append("")
    return "\n".join(lines)


def field(
    event: Event,
    key: str,
    default: str = "",
) -> str:
    return event.fields.get(key, default)
