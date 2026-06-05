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
    "PlaybackEdgeFade",
    "PlaybackSpeedMemory",
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
    edge_fades = [event for event in events if event.tag == "PlaybackEdgeFade"]
    memory_events = [event for event in events if event.tag == "PlaybackSpeedMemory"]
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
    if edge_fades:
        latest = edge_fades[-1]
        lines.append(
            "- Latest playback edge fade: "
            f"time={latest.time} kind={latest.kind} path={field(latest, 'path')} "
            f"offset={field(latest, 'offset')} head={field(latest, 'head')} "
            f"covered={field(latest, 'covered')} elapsedMs={field(latest, 'elapsedMs')}"
        )
    if memory_events:
        latest = memory_events[-1]
        lines.append(
            "- Latest playback memory: "
            f"time={latest.time} kind={latest.kind} renderer={field(latest, 'renderer')} "
            f"mode={field(latest, 'mode')} speed={field(latest, 'speed')} "
            f"streaming={field(latest, 'streaming')} fileBacked={field(latest, 'fileBacked')} "
            f"sourceSamples={field(latest, 'sourceSamples')} renderedSamples={field(latest, 'renderedSamples')} "
            f"heapDelta={field(latest, 'heapDelta')} nativeHeapDelta={field(latest, 'nativeHeapDelta')} "
            f"renderTimeMs={field(latest, 'renderTimeMs', field(latest, 'loadTimeMs'))}"
        )
    if rejected:
        lines.append(f"- Rejected commands: {len(rejected)}")

    key_events = [event for event in events if is_key_event(event)]
    row_events = key_events if key_events else events
    omitted_events = len(events) - min(len(row_events), max_rows)

    lines.append("")
    lines.append("## Key Rows")
    lines.append("")
    if key_events:
        lines.append(f"- Showing {min(len(key_events), max_rows)} of {len(key_events)} key events.")
    else:
        lines.append(f"- No key event filter matched; showing first {min(len(events), max_rows)} events.")
    if omitted_events > 0:
        lines.append(f"- Omitted events from this summary: {omitted_events}. See raw.log for the complete logcat output.")
    lines.append("")
    lines.append("| time | tag | kind | source | requested | stored/actual | applied | phase |")
    lines.append("| --- | --- | --- | --- | ---: | ---: | --- | --- |")
    for event in row_events[:max_rows]:
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

    if memory_events:
        lines.append("")
        lines.append("## Memory Rows")
        lines.append("")
        lines.append(f"- Showing {min(len(memory_events), max_rows)} of {len(memory_events)} memory events.")
        lines.append("")
        lines.append(
            "| time | kind | renderer | mode | speed | streaming | fileBacked | sourceSamples | renderedSamples | "
            "heapDelta | nativeHeapDelta | timeMs |"
        )
        lines.append("| --- | --- | --- | --- | ---: | --- | --- | ---: | ---: | ---: | ---: | ---: |")
        for event in memory_events[:max_rows]:
            lines.append(
                "| "
                f"{event.time} | "
                f"{event.kind} | "
                f"{field(event, 'renderer')} | "
                f"{field(event, 'mode')} | "
                f"{field(event, 'speed')} | "
                f"{field(event, 'streaming')} | "
                f"{field(event, 'fileBacked')} | "
                f"{field(event, 'sourceSamples')} | "
                f"{field(event, 'renderedSamples')} | "
                f"{field(event, 'heapDelta')} | "
                f"{field(event, 'nativeHeapDelta')} | "
                f"{field(event, 'renderTimeMs', field(event, 'loadTimeMs'))} |"
            )

    lines.append("")
    return "\n".join(lines)


def field(
    event: Event,
    key: str,
    default: str = "",
) -> str:
    return event.fields.get(key, default)


def is_key_event(event: Event) -> bool:
    if event.tag in {"PlaybackSpeedDiag", "PlaybackCoordDiag", "AudioTrackTransport", "PlaybackEdgeFade", "PlaybackSpeedMemory"}:
        return True
    if event.tag == "AudioPlayerDiag":
        return event.kind in {
            "playPcm",
            "playPcmFile",
            "releaseTrack",
            "renderSpeedAdjustedPcm",
            "streamSpeedAdjustedPcm",
        }
    return event.kind.endswith("Rejected")
