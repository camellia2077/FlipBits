#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from dataclasses import dataclass
from pathlib import Path


KEY_VALUE_RE = re.compile(r"([A-Za-z][A-Za-z0-9/]*)=([^ ]+)")
TEXT_FIELD_RE = re.compile(r"(text|tokenText|sourceLineText)=([^=]*?)(?= [A-Za-z][A-Za-z0-9/]*=|$)")
TIME_RE = re.compile(r"^(\d\d-\d\d \d\d:\d\d:\d\d\.\d{3})")
TAGS = ("MiniAutomation", "MiniAlignmentPerf", "MiniVisualPerf", "FlashLyricsPerf")


@dataclass(frozen=True)
class Event:
    time: str
    tag: str
    fields: dict[str, str]
    raw: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Filter and summarize Mini visual + lyrics adb logs.")
    parser.add_argument("log", type=Path, help="Raw adb logcat text file.")
    parser.add_argument("--output", type=Path, help="Optional markdown summary output path.")
    parser.add_argument("--max-rows", type=int, default=24, help="Maximum alignment rows to print.")
    return parser.parse_args()


def parse_event(line: str) -> Event | None:
    tag = next((candidate for candidate in TAGS if candidate in line), None)
    if tag is None:
        return None
    time_match = TIME_RE.match(line)
    time = time_match.group(1) if time_match else ""
    message = line.split(tag, 1)[-1]
    fields = {key: value for key, value in KEY_VALUE_RE.findall(message)}
    for key, value in TEXT_FIELD_RE.findall(message):
        fields[key] = value.strip()
    return Event(time=time, tag=tag, fields=fields, raw=line.rstrip())


def int_field(event: Event, key: str, default: int = -1) -> int:
    try:
        return int(float(event.fields.get(key, str(default))))
    except ValueError:
        return default


def str_field(event: Event, key: str, default: str = "") -> str:
    return event.fields.get(key, default)


def float_field(event: Event, key: str, default: float = 0.0) -> float:
    try:
        return float(event.fields.get(key, str(default)))
    except ValueError:
        return default


def latest_with_field(events: list[Event], field: str) -> Event | None:
    return next((event for event in reversed(events) if field in event.fields), None)


def build_summary(events: list[Event], max_rows: int) -> str:
    automation = [event for event in events if event.tag == "MiniAutomation"]
    alignment = [event for event in events if event.tag == "MiniAlignmentPerf"]
    visual_perf = [event for event in events if event.tag == "MiniVisualPerf"]
    lyrics = [event for event in events if event.tag == "FlashLyricsPerf"]
    lines: list[str] = []
    lines.append("# Mini Alignment Log Summary")
    lines.append("")
    lines.append(f"- MiniAutomation rows: {len(automation)}")
    lines.append(f"- MiniAlignmentPerf rows: {len(alignment)}")
    lines.append(f"- MiniVisualPerf rows: {len(visual_perf)}")
    lines.append(f"- FlashLyricsPerf rows: {len(lyrics)}")
    received = latest_with_field(automation, "scenario")
    if received:
        lines.append(
            "- Scenario: "
            f"scenario={str_field(received, 'scenario')} "
            f"speed={str_field(received, 'speed')} "
            f"playMs={str_field(received, 'playMs')} "
            f"input={str_field(received, 'input', '_')} "
            f"text={str_field(received, 'text', '_')} "
            f"sampleLength={str_field(received, 'sampleLength', '_')}"
        )
    input_resolved = latest_with_field(automation, "chars")
    if input_resolved:
        lines.append(
            "- Input: "
            f"source={str_field(input_resolved, 'source')} "
            f"sampleId={str_field(input_resolved, 'sampleId', '_')} "
            f"chars={str_field(input_resolved, 'chars')} "
            f"payloadBytes={str_field(input_resolved, 'payloadBytes')}"
        )
    playing = [event for event in alignment if str_field(event, "playing") == "true"]
    token_rows = [event for event in playing if int_field(event, "token") >= 0]
    visual_active_rows = [event for event in playing if str_field(event, "visualActive") == "true"]
    lines.append(f"- Alignment playing rows: {len(playing)}")
    lines.append(f"- Alignment active visual rows: {len(visual_active_rows)}")
    lines.append(f"- Alignment active token rows: {len(token_rows)}")
    if alignment:
        sample_deltas = [abs(int_field(event, "sampleDelta", 0)) for event in alignment]
        lines.append(f"- Max absolute sampleDelta: {max(sample_deltas)}")
    if visual_perf:
        latest_perf = visual_perf[-1]
        lines.append(
            "- Latest MiniVisualPerf: "
            f"reason={str_field(latest_perf, 'reason', '_')} "
            f"drawAvgMs={str_field(latest_perf, 'drawAvgMs', '_')} "
            f"rawUpdate/s={str_field(latest_perf, 'rawUpdate/s', '_')} "
            f"rawStepMaxMs={str_field(latest_perf, 'rawStepMaxMs', '_')} "
            f"smoothStepMaxMs={str_field(latest_perf, 'smoothStepMaxMs', '_')} "
            f"visualErrorMs={str_field(latest_perf, 'visualErrorMs', '_')} "
            f"windowStepMaxMs={str_field(latest_perf, 'windowStepMaxMs', '_')}"
        )
        max_visual_error_ms = max(abs(float_field(event, "visualErrorMs")) for event in visual_perf)
        lines.append(
            "- Peak MiniVisualPerf: "
            f"drawAvgMs={max(float_field(event, 'drawAvgMs') for event in visual_perf):.2f} "
            f"rawUpdate/s={max(float_field(event, 'rawUpdate/s') for event in visual_perf):.1f} "
            f"rawStepMaxMs={max(float_field(event, 'rawStepMaxMs') for event in visual_perf):.1f} "
            f"smoothStepMaxMs={max(float_field(event, 'smoothStepMaxMs') for event in visual_perf):.1f} "
            f"visualErrorMs(abs)={max_visual_error_ms:.1f} "
            f"windowStepMaxMs={max(float_field(event, 'windowStepMaxMs') for event in visual_perf):.1f}"
        )
    lines.append("")
    if visual_perf:
        lines.append("## Mini Visual Perf")
        lines.append("")
        lines.append("| time | reason | drawAvgMs | rawUpdate/s | rawStepMaxMs | smoothStepMaxMs | visualErrorMs | windowStepMaxMs |")
        lines.append("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |")
        for event in visual_perf[:max_rows]:
            lines.append(
                "| "
                f"{event.time} | "
                f"{str_field(event, 'reason', '_')} | "
                f"{float_field(event, 'drawAvgMs'):.2f} | "
                f"{float_field(event, 'rawUpdate/s'):.1f} | "
                f"{float_field(event, 'rawStepMaxMs'):.1f} | "
                f"{float_field(event, 'smoothStepMaxMs'):.1f} | "
                f"{float_field(event, 'visualErrorMs'):.1f} | "
                f"{float_field(event, 'windowStepMaxMs'):.1f} |"
            )
        lines.append("")
    lines.append("## Alignment Rows")
    lines.append("")
    lines.append(
        "| time | speed | frameSamples | visualSample | lyricsSample | sampleDelta | "
        "visualGroup | visualBitOffset | token | tokenText | tokenProgress | lyricBitOffset | tone |"
    )
    lines.append("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | ---: | ---: | --- |")
    for event in alignment[:max_rows]:
        lines.append(
            "| "
            f"{event.time} | "
            f"{str_field(event, 'speed')} | "
            f"{int_field(event, 'frameSamples')} | "
            f"{int_field(event, 'visualSample')} | "
            f"{int_field(event, 'lyricsSample')} | "
            f"{int_field(event, 'sampleDelta')} | "
            f"{int_field(event, 'visualGroup')} | "
            f"{int_field(event, 'visualBitOffset')} | "
            f"{int_field(event, 'token')} | "
            f"{str_field(event, 'tokenText', '_')} | "
            f"{str_field(event, 'tokenProgress', '-')} | "
            f"{int_field(event, 'lyricBitOffset')} | "
            f"{str_field(event, 'tone')} |"
        )
    lines.append("")
    lines.append("## Filtered Rows")
    lines.append("")
    for event in events:
        lines.append(f"- `{event.raw}`")
    lines.append("")
    return "\n".join(lines)


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
