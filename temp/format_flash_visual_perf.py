from __future__ import annotations

import argparse
import re
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_INPUT = SCRIPT_DIR / "log.txt"
DEFAULT_OUTPUT = SCRIPT_DIR / "log.md"

LINE_RE = re.compile(r"(?P<time>\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3}).*?FlashVisualPerf:\s+(?P<body>.*)$")
FIELD_RE = re.compile(r"(?P<key>[A-Za-z/]+)=\[(?P<bracket>[^\]]*)\)|(?P<key2>[A-Za-z/]+)=(?P<value>\S+)")

SUMMARY_COLUMNS = [
    "time",
    "mode",
    "playing",
    "draw/s",
    "compose/s",
    "displayedDelta/s",
    "visible",
    "visiblePrimitives",
    "drawAvgMs",
    "drawMaxMs",
    "rawUpdate/s",
    "rawStepMaxMs",
    "smoothStepMaxMs",
    "visualErrorMs",
    "pxStepMax",
    "anchorJumpMaxMs",
    "smoothReset",
    "windowShiftMaxMs",
    "viewportStartStepMaxMs",
    "largePxStep",
    "windowReq/s",
    "windowBuild/s",
    "windowBusySkip/s",
    "window",
    "distStart",
    "distEnd",
]


def parse_fields(body: str) -> dict[str, str]:
    fields: dict[str, str] = {}
    for match in FIELD_RE.finditer(body):
        key = match.group("key") or match.group("key2")
        value = match.group("bracket") if match.group("key") else match.group("value")
        fields[key] = value
    return fields


def parse_log(text: str) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    for line in text.splitlines():
        match = LINE_RE.search(line)
        if not match:
            continue
        row = parse_fields(match.group("body"))
        row["time"] = match.group("time")
        rows.append(row)
    return rows


def markdown_table(rows: list[dict[str, str]], columns: list[str]) -> str:
    header = "| " + " | ".join(columns) + " |"
    divider = "| " + " | ".join("---" for _ in columns) + " |"
    body = [
        "| " + " | ".join(escape_cell(row.get(column, "")) for column in columns) + " |"
        for row in rows
    ]
    return "\n".join([header, divider, *body])


def escape_cell(value: str) -> str:
    return value.replace("|", "\\|")


def numeric(row: dict[str, str], key: str) -> float | None:
    try:
        return float(row[key])
    except (KeyError, ValueError):
        return None


def average(rows: list[dict[str, str]], key: str) -> float | None:
    values = [value for row in rows if (value := numeric(row, key)) is not None]
    if not values:
        return None
    return sum(values) / len(values)


def maximum(rows: list[dict[str, str]], key: str) -> float | None:
    values = [value for row in rows if (value := numeric(row, key)) is not None]
    if not values:
        return None
    return max(values)


def sum_numeric(rows: list[dict[str, str]], key: str) -> float | None:
    values = [value for row in rows if (value := numeric(row, key)) is not None]
    if not values:
        return None
    return sum(values)


def fmt(value: float | None) -> str:
    return "n/a" if value is None else f"{value:.2f}"


def build_markdown(rows: list[dict[str, str]], source: Path) -> str:
    if not rows:
        return f"# Flash Visual Perf Log\n\nNo `FlashVisualPerf` rows found in `{source.name}`.\n"

    all_columns = ["time", *sorted({key for row in rows for key in row if key != "time"})]
    summary = [
        "# Flash Visual Perf Log",
        "",
        f"- Source: `{source.name}`",
        f"- Rows: `{len(rows)}`",
        f"- Avg FPS: `{fmt(average(rows, 'draw/s'))}`",
        f"- Avg draw ms: `{fmt(average(rows, 'drawAvgMs'))}`",
        f"- Max draw ms: `{fmt(maximum(rows, 'drawMaxMs'))}`",
        f"- Avg visible primitives: `{fmt(average(rows, 'visiblePrimitives'))}`",
        f"- Max px step: `{fmt(maximum(rows, 'pxStepMax'))}`",
        f"- Max anchor jump ms: `{fmt(maximum(rows, 'anchorJumpMaxMs'))}`",
        f"- Max viewport start step ms: `{fmt(maximum(rows, 'viewportStartStepMaxMs'))}`",
        f"- Total smooth resets: `{fmt(sum_numeric(rows, 'smoothReset'))}`",
        f"- Total large px steps: `{fmt(sum_numeric(rows, 'largePxStep'))}`",
        f"- Avg window builds/s: `{fmt(average(rows, 'windowBuild/s'))}`",
        "",
        "## Summary",
        "",
        markdown_table(rows, [column for column in SUMMARY_COLUMNS if column in all_columns]),
        "",
        "## All Fields",
        "",
        markdown_table(rows, all_columns),
        "",
    ]
    return "\n".join(summary)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert adb FlashVisualPerf log lines into a readable markdown report.",
    )
    parser.add_argument(
        "--input",
        type=Path,
        default=DEFAULT_INPUT,
        help="Input log file. Defaults to log.txt beside this script.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT,
        help="Output markdown file. Defaults to log.md beside this script.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    input_path = args.input
    output_path = args.output
    if not input_path.exists():
        raise SystemExit(f"Missing input file: {input_path}")

    rows = parse_log(input_path.read_text(encoding="utf-8", errors="replace"))
    output_path.write_text(build_markdown(rows, input_path), encoding="utf-8")
    print(f"Wrote {output_path}")


if __name__ == "__main__":
    main()
