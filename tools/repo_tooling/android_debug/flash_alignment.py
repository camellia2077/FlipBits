#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from dataclasses import dataclass
from pathlib import Path


KEY_VALUE_RE = re.compile(r"([A-Za-z][A-Za-z0-9/]*)=([^ ]+)")
TEXT_FIELD_RE = re.compile(r"(text|tokenText|sourceLineText)=([^=]*?)(?= [A-Za-z][A-Za-z0-9/]*=|$)")
TIME_RE = re.compile(r"^(\d\d-\d\d \d\d:\d\d:\d\d\.\d{3})")


@dataclass(frozen=True)
class Event:
    time: str
    tag: str
    fields: dict[str, str]
    raw: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Filter and summarize Flash visual + lyrics adb logs.")
    parser.add_argument("log", type=Path, help="Raw adb logcat text file.")
    parser.add_argument("--output", type=Path, help="Optional markdown summary output path.")
    parser.add_argument(
        "--max-rows",
        type=int,
        default=24,
        help="Maximum visual/lyrics alignment rows to print.",
    )
    return parser.parse_args()


def parse_event(line: str) -> Event | None:
    tag = next(
        (
            candidate
            for candidate in ("FlashAutomation", "FlashAlignmentPerf", "FlashVisualPerf", "FlashLyricsPerf")
            if candidate in line
        ),
        None,
    )
    if tag is None:
        return None
    time_match = TIME_RE.match(line)
    time = time_match.group(1) if time_match else ""
    message = line.split(f"{tag}", 1)[-1]
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


def latest_with_field(events: list[Event], field: str) -> Event | None:
    return next((event for event in reversed(events) if field in event.fields), None)


def nearest_visual(visual_events: list[Event], sample: int) -> Event | None:
    if not visual_events:
        return None
    return min(visual_events, key=lambda event: abs(int_field(event, "readoutSample", int_field(event, "displayed")) - sample))


def build_summary(events: list[Event], max_rows: int) -> str:
    automation = [event for event in events if event.tag == "FlashAutomation"]
    alignment = [event for event in events if event.tag == "FlashAlignmentPerf"]
    visual = [event for event in events if event.tag == "FlashVisualPerf"]
    lyrics = [event for event in events if event.tag == "FlashLyricsPerf"]
    lines: list[str] = []
    lines.append("# Flash Alignment Log Summary")
    lines.append("")
    lines.append(f"- FlashAutomation rows: {len(automation)}")
    lines.append(f"- FlashAlignmentPerf rows: {len(alignment)}")
    lines.append(f"- FlashVisualPerf rows: {len(visual)}")
    lines.append(f"- FlashLyricsPerf rows: {len(lyrics)}")
    received = latest_with_field(automation, "scenario")
    if received:
        lines.append(
            "- Scenario: "
            f"scenario={str_field(received, 'scenario')} "
            f"style={str_field(received, 'style')} "
            f"visual={str_field(received, 'visual')} "
            f"playMs={str_field(received, 'playMs')} "
            f"input={str_field(received, 'input')} "
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
    visual_playing = [event for event in visual if str_field(event, "playing") == "true"]
    lyrics_playing = [event for event in lyrics if str_field(event, "playing") == "true"]
    lines.append(f"- Visual playing rows: {len(visual_playing)}")
    lines.append(f"- Lyrics playing rows: {len(lyrics_playing)}")
    if visual_playing:
        fallback_values = sorted({str_field(event, "fallback") for event in visual_playing})
        bit_values = sorted({str_field(event, "bitReadout") for event in visual_playing})
        lines.append(f"- Visual fallback values while playing: {', '.join(fallback_values)}")
        lines.append(f"- Visual bitReadout values while playing: {', '.join(bit_values)}")
    token_rows = [event for event in lyrics_playing if int_field(event, "token") >= 0]
    token_switches = 0
    previous_token = None
    for event in token_rows:
        token = int_field(event, "token")
        if previous_token is not None and token != previous_token:
            token_switches += 1
        previous_token = token
    lines.append(f"- Lyrics active-token rows: {len(token_rows)}")
    lines.append(f"- Lyrics token switches in capture: {token_switches}")
    lines.append("")
    if alignment:
        alignment_playing = [
            event
            for event in alignment
            if str_field(event, "visualPlaying") == "true" or str_field(event, "lyricsPlaying") == "true"
        ]
        lines.append("## Unified Alignment Rows")
        lines.append("")
        lines.append(
            "| time | visualSample | lyricsSample | sampleDelta | token | tokenText | tokenProgress | "
            "readoutBit | visualBit | lyricBitOffset | lyricBit | bitDelta | fallback | bitReadout |"
        )
        lines.append("| --- | ---: | ---: | ---: | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |")
        for event in alignment_playing[:max_rows]:
            lines.append(
                "| "
                f"{event.time} | "
                f"{int_field(event, 'visualSample')} | "
                f"{int_field(event, 'lyricsSample')} | "
                f"{int_field(event, 'sampleDelta')} | "
                f"{int_field(event, 'token')} | "
                f"{str_field(event, 'tokenText', '_')} | "
                f"{str_field(event, 'tokenProgress', '-')} | "
                f"{int_field(event, 'readoutBit')} | "
                f"{int_field(event, 'visualBit')} | "
                f"{int_field(event, 'lyricBitOffset')} | "
                f"{int_field(event, 'lyricBit')} | "
                f"{int_field(event, 'bitDelta')} | "
                f"{str_field(event, 'fallback')} | "
                f"{str_field(event, 'bitReadout')} |"
            )
        lines.append("")
        lines.append("## Nearest-Pair Fallback Rows")
        lines.append("")
    else:
        lines.append("## Alignment Rows")
        lines.append("")
    lines.append("| time | lyricsSample | token | tokenText | tokenProgress | lyricBit | visualSample | readoutBit | visualBit | sampleDelta |")
    lines.append("| --- | ---: | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |")
    for event in lyrics_playing[:max_rows]:
        sample = int_field(event, "sample")
        visual_event = nearest_visual(visual_playing, sample)
        visual_sample = int_field(visual_event, "readoutSample", int_field(visual_event, "displayed")) if visual_event else -1
        lines.append(
            "| "
            f"{event.time} | "
            f"{sample} | "
            f"{int_field(event, 'token')} | "
            f"{str_field(event, 'tokenText', '_')} | "
            f"{str_field(event, 'tokenProgress', '-')} | "
            f"{int_field(event, 'bit')} | "
            f"{visual_sample} | "
            f"{int_field(visual_event, 'readoutBit') if visual_event else -1} | "
            f"{int_field(visual_event, 'visualBit') if visual_event else -1} | "
            f"{visual_sample - sample if visual_event else 0} |"
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
