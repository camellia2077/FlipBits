from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from statistics import mean

from .flash_alignment import Event, int_field, parse_event, str_field


@dataclass(frozen=True)
class ModeCapture:
    mode: str
    raw_log: Path
    summary: Path
    crash_summary: Path


@dataclass(frozen=True)
class SweepMetrics:
    mode: str
    visual_rows: int
    periodic_visual_rows: int
    seek_immediate_rows: int
    seek_settled_rows: int
    preview_rows: int
    scrub_preview_rows: int
    direct_preview_rows: int
    max_primitives: int
    max_visible_primitives: int
    max_draw_max_ms: float
    avg_draw_avg_ms: float
    max_abs_visual_error_ms: float
    seek_immediate_max_display_delta: int
    seek_immediate_max_raw_delta: int
    seek_immediate_max_smooth_delta: int
    seek_immediate_max_readout_delta: int
    seek_settled_max_display_delta: int
    seek_settled_max_raw_delta: int
    seek_settled_max_smooth_delta: int
    seek_settled_max_readout_delta: int
    scrub_preview_max_abs_delta_tx: float


def read_events(log: Path) -> list[Event]:
    text = log.read_text(encoding="utf-8", errors="replace")
    return [event for line in text.splitlines() if (event := parse_event(line)) is not None]


def summarize_mode(
    mode: str,
    events: list[Event],
) -> SweepMetrics:
    visual_events = [event for event in events if event.tag == "FlashVisualPerf"]
    periodic_visual = [event for event in visual_events if "drawAvgMs" in event.fields]
    forced_immediate = [
        event
        for event in visual_events
        if str_field(event, "reason").startswith("seek-immediate")
    ]
    forced_settled = [
        event
        for event in visual_events
        if str_field(event, "reason").startswith("seek-settled")
    ]
    preview_events = [
        event
        for event in events
        if event.tag == "FlashLyricsPerf" and "scrubbing" in event.fields and "deltaTx" in event.fields
    ]
    scrub_preview = [event for event in preview_events if str_field(event, "scrubbing") == "true"]
    direct_preview = [event for event in scrub_preview if str_field(event, "direct") == "true"]

    return SweepMetrics(
        mode=mode,
        visual_rows=len(visual_events),
        periodic_visual_rows=len(periodic_visual),
        seek_immediate_rows=len(forced_immediate),
        seek_settled_rows=len(forced_settled),
        preview_rows=len(preview_events),
        scrub_preview_rows=len(scrub_preview),
        direct_preview_rows=len(direct_preview),
        max_primitives=max((int_field(event, "primitives", 0) for event in visual_events), default=0),
        max_visible_primitives=max((int_field(event, "visiblePrimitives", 0) for event in visual_events), default=0),
        max_draw_max_ms=max((float(str_field(event, "drawMaxMs", "0")) for event in periodic_visual), default=0.0),
        avg_draw_avg_ms=mean([float(str_field(event, "drawAvgMs", "0")) for event in periodic_visual]) if periodic_visual else 0.0,
        max_abs_visual_error_ms=max((abs(float(str_field(event, "visualErrorMs", "0"))) for event in visual_events), default=0.0),
        seek_immediate_max_display_delta=_max_target_delta(forced_immediate, "displayed"),
        seek_immediate_max_raw_delta=_max_target_delta(forced_immediate, "raw"),
        seek_immediate_max_smooth_delta=_max_target_delta(forced_immediate, "smooth"),
        seek_immediate_max_readout_delta=_max_target_delta(forced_immediate, "readoutSample"),
        seek_settled_max_display_delta=_max_target_delta(forced_settled, "displayed"),
        seek_settled_max_raw_delta=_max_target_delta(forced_settled, "raw"),
        seek_settled_max_smooth_delta=_max_target_delta(forced_settled, "smooth"),
        seek_settled_max_readout_delta=_max_target_delta(forced_settled, "readoutSample"),
        scrub_preview_max_abs_delta_tx=max(
            (abs(float(str_field(event, "deltaTx", "0"))) for event in scrub_preview),
            default=0.0,
        ),
    )


def build_sweep_summary(
    captures: list[ModeCapture],
) -> str:
    metrics = [summarize_mode(capture.mode, read_events(capture.raw_log)) for capture in captures]
    lines: list[str] = []
    lines.append("# Flash Visual Sweep Summary")
    lines.append("")
    lines.append("## Overview")
    lines.append("")
    lines.append("| mode | visualRows | periodicRows | seekImmediate | seekSettled | previewRows | scrubPreview | directPreview | maxPrimitives | maxVisiblePrimitives | avgDrawAvgMs | maxDrawMaxMs | maxAbsVisualErrorMs |")
    lines.append("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
    for item in metrics:
        lines.append(
            "| "
            f"{item.mode} | "
            f"{item.visual_rows} | "
            f"{item.periodic_visual_rows} | "
            f"{item.seek_immediate_rows} | "
            f"{item.seek_settled_rows} | "
            f"{item.preview_rows} | "
            f"{item.scrub_preview_rows} | "
            f"{item.direct_preview_rows} | "
            f"{item.max_primitives} | "
            f"{item.max_visible_primitives} | "
            f"{item.avg_draw_avg_ms:.2f} | "
            f"{item.max_draw_max_ms:.2f} | "
            f"{item.max_abs_visual_error_ms:.2f} |"
        )
    lines.append("")
    lines.append("## Seek Delta")
    lines.append("")
    lines.append("| mode | immediate display | immediate raw | immediate smooth | immediate readout | settled display | settled raw | settled smooth | settled readout | scrub max |deltaTx| |")
    lines.append("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
    for item in metrics:
        lines.append(
            "| "
            f"{item.mode} | "
            f"{item.seek_immediate_max_display_delta} | "
            f"{item.seek_immediate_max_raw_delta} | "
            f"{item.seek_immediate_max_smooth_delta} | "
            f"{item.seek_immediate_max_readout_delta} | "
            f"{item.seek_settled_max_display_delta} | "
            f"{item.seek_settled_max_raw_delta} | "
            f"{item.seek_settled_max_smooth_delta} | "
            f"{item.seek_settled_max_readout_delta} | "
            f"{item.scrub_preview_max_abs_delta_tx:.2f} |"
        )
    lines.append("")
    lines.append("## Artifacts")
    lines.append("")
    for capture in captures:
        lines.append(f"- `{capture.mode}` raw: `{capture.raw_log}`")
        lines.append(f"- `{capture.mode}` summary: `{capture.summary}`")
    lines.append("")
    return "\n".join(lines)


def _max_target_delta(
    events: list[Event],
    field: str,
) -> int:
    deltas: list[int] = []
    for event in events:
        target = int_field(event, "target", 0)
        value = int_field(event, field, 0)
        deltas.append(abs(value - target))
    return max(deltas, default=0)
