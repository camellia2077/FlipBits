#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from dataclasses import dataclass
from pathlib import Path


KEY_VALUE_RE = re.compile(r"([A-Za-z][A-Za-z0-9/]*)=([^ ]+)")
TIME_RE = re.compile(r"^(\d\d-\d\d \d\d:\d\d:\d\d\.\d{3})")
TAG = "EncodeProgressAutomation"


@dataclass(frozen=True)
class Event:
    time: str
    kind: str
    fields: dict[str, str]
    raw: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Filter and summarize Android encode progress adb logs.")
    parser.add_argument("log", type=Path, help="Raw adb logcat text file.")
    parser.add_argument("--output", type=Path, help="Optional markdown summary output path.")
    parser.add_argument("--max-rows", type=int, default=80, help="Maximum UI/tick rows to print.")
    return parser.parse_args()


def parse_event(line: str) -> Event | None:
    if TAG not in line:
        return None
    time_match = TIME_RE.match(line)
    time = time_match.group(1) if time_match else ""
    tag_tail = line.split(TAG, 1)[-1]
    message = tag_tail.split("):", 1)[-1].strip()
    kind = message.split(" ", 1)[0] if message else ""
    fields = {key: value for key, value in KEY_VALUE_RE.findall(message)}
    return Event(time=time, kind=kind, fields=fields, raw=line.rstrip())


def str_field(event: Event, key: str, default: str = "") -> str:
    return event.fields.get(key, default)


def int_field(event: Event, key: str, default: int = -1) -> int:
    try:
        return int(float(event.fields.get(key, str(default))))
    except ValueError:
        return default


def rows_for_display(events: list[Event]) -> list[Event]:
    rows = [event for event in events if event.kind == "uiTick"]
    return rows if rows else [event for event in events if event.kind == "ui"]


def phase_transitions(rows: list[Event]) -> list[Event]:
    transitions: list[Event] = []
    previous_phase: str | None = None
    previous_busy: str | None = None
    for event in rows:
        phase = str_field(event, "phase", "none")
        busy = str_field(event, "busy")
        if phase != previous_phase or busy != previous_busy:
            transitions.append(event)
            previous_phase = phase
            previous_busy = busy
    return transitions


def count_preparing_finalizing_bounces(transitions: list[Event]) -> int:
    count = 0
    for previous, current in zip(transitions, transitions[1:]):
        pair = {str_field(previous, "phase"), str_field(current, "phase")}
        if pair == {"PreparingInput", "Finalizing"}:
            count += 1
    return count


def build_summary(events: list[Event], max_rows: int) -> str:
    rows = rows_for_display(events)
    transitions = phase_transitions(rows)
    received = [event for event in events if event.kind == "received"]
    input_resolved = [event for event in events if event.kind == "inputResolved"]
    capture_end = [event for event in events if event.kind == "captureEnd"]
    busy_rows = [event for event in rows if str_field(event, "busy") == "true"]
    visible_rows = [event for event in rows if str_field(event, "barVisible") == "true"]
    phases = [str_field(event, "phase", "none") for event in rows]

    lines: list[str] = []
    lines.append("# Encode Progress Log Summary")
    lines.append("")
    lines.append(f"- EncodeProgressAutomation rows: {len(events)}")
    lines.append(f"- Display sample rows: {len(rows)}")
    lines.append(f"- Busy rows: {len(busy_rows)}")
    lines.append(f"- Bar visible rows: {len(visible_rows)}")
    lines.append(f"- Phase transitions: {len(transitions)}")
    lines.append(f"- Preparing/Finalizing bounces: {count_preparing_finalizing_bounces(transitions)}")
    if received:
        latest = received[-1]
        lines.append(
            "- Scenario: "
            f"mode={str_field(latest, 'mode')} "
            f"speed={str_field(latest, 'speed')} "
            f"repeat={str_field(latest, 'repeat')} "
            f"captureMs={str_field(latest, 'captureMs')} "
            f"pollMs={str_field(latest, 'pollMs')}"
        )
    if input_resolved:
        latest = input_resolved[-1]
        lines.append(
            "- Input: "
            f"source={str_field(latest, 'source')} "
            f"sampleId={str_field(latest, 'sampleId', '_')} "
            f"chars={str_field(latest, 'chars')} "
            f"payloadBytes={str_field(latest, 'payloadBytes')}"
        )
    if capture_end:
        latest = capture_end[-1]
        lines.append(
            "- Capture end: "
            f"ticks={str_field(latest, 'ticks')} "
            f"sawBusy={str_field(latest, 'sawBusy')} "
            f"elapsedMs={str_field(latest, 'elapsedMs')}"
        )
    if phases:
        unique_phases = ", ".join(dict.fromkeys(phases))
        lines.append(f"- Observed phases: {unique_phases}")
    lines.append("")
    lines.append("## Phase Transitions")
    lines.append("")
    lines.append("| time | kind | tick | busy | barVisible | labelVisible | phase | percent | progress |")
    lines.append("| --- | --- | ---: | --- | --- | --- | --- | ---: | ---: |")
    for event in transitions[:max_rows]:
        lines.append(progress_row(event))
    lines.append("")
    lines.append("## Display Rows")
    lines.append("")
    lines.append("| time | kind | tick | busy | barVisible | labelVisible | phase | percent | progress |")
    lines.append("| --- | --- | ---: | --- | --- | --- | --- | ---: | ---: |")
    for event in rows[:max_rows]:
        lines.append(progress_row(event))
    lines.append("")
    lines.append("## Filtered Rows")
    lines.append("")
    for event in events:
        lines.append(f"- `{event.raw}`")
    lines.append("")
    return "\n".join(lines)


def progress_row(event: Event) -> str:
    return (
        "| "
        f"{event.time} | "
        f"{event.kind} | "
        f"{int_field(event, 'tick')} | "
        f"{str_field(event, 'busy')} | "
        f"{str_field(event, 'barVisible')} | "
        f"{str_field(event, 'labelVisible')} | "
        f"{str_field(event, 'phase', 'none')} | "
        f"{int_field(event, 'percent')} | "
        f"{str_field(event, 'progress', '-')} |"
    )


def main() -> int:
    args = parse_args()
    text = args.log.read_text(encoding="utf-8", errors="replace")
    events = [event for line in text.splitlines() if (event := parse_event(line)) is not None]
    summary = build_summary(events, max_rows=args.max_rows)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(summary, encoding="utf-8")
        print(f"Wrote {args.output}")
    else:
        print(summary)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
