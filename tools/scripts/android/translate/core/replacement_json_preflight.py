from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class ReplacementJsonPreflightResult:
    ok: bool
    raw_text: str
    repaired_text: str | None = None
    changed: bool = False
    error: str | None = None
    fix_summary: str | None = None


def load_replacement_json_with_preflight(
    json_path: Path,
    *,
    auto_fix: bool,
) -> ReplacementJsonPreflightResult:
    raw_text = json_path.read_text(encoding="utf-8")
    parse_error = _validate_json(raw_text)
    if parse_error is None:
        return ReplacementJsonPreflightResult(ok=True, raw_text=raw_text)

    repaired = _repair_high_confidence_json_issues(raw_text)
    if repaired is None:
        return ReplacementJsonPreflightResult(
            ok=False,
            raw_text=raw_text,
            error=_build_preflight_error(parse_error, fix_summary=None, auto_fix=auto_fix),
        )

    repaired_error = _validate_json(repaired)
    if repaired_error is not None:
        return ReplacementJsonPreflightResult(
            ok=False,
            raw_text=raw_text,
            repaired_text=repaired,
            changed=True,
            error=_build_preflight_error(parse_error, fix_summary="A high-confidence repair was attempted but the JSON is still invalid.", auto_fix=auto_fix),
        )

    fix_summary = "Escaped unbalanced double quotes inside JSON string values."
    if auto_fix:
        json_path.write_text(repaired, encoding="utf-8")
        return ReplacementJsonPreflightResult(
            ok=True,
            raw_text=raw_text,
            repaired_text=repaired,
            changed=True,
            fix_summary=fix_summary,
        )

    return ReplacementJsonPreflightResult(
        ok=False,
        raw_text=raw_text,
        repaired_text=repaired,
        changed=True,
        error=_build_preflight_error(parse_error, fix_summary=fix_summary, auto_fix=auto_fix),
        fix_summary=fix_summary,
    )


def _validate_json(raw_text: str) -> json.JSONDecodeError | None:
    try:
        json.loads(raw_text)
    except json.JSONDecodeError as exc:
        return exc
    return None


def _build_preflight_error(
    parse_error: json.JSONDecodeError,
    *,
    fix_summary: str | None,
    auto_fix: bool,
) -> str:
    lines = [
        "Replacement JSON preflight failed.",
        f"JSON decode error: {parse_error.msg} at line {parse_error.lineno}, column {parse_error.colno}.",
    ]
    if fix_summary is not None:
        if auto_fix:
            lines.append(f"Automatic repair was enabled, but it could not produce a valid JSON file. Attempted fix: {fix_summary}")
        else:
            lines.append(f"A high-confidence auto-fix is available: {fix_summary}")
            lines.append("Re-run with --auto-fix-json to apply the repair before replace.")
    else:
        lines.append("No high-confidence automatic repair is available for this JSON syntax error.")
    return "\n".join(lines)


def _repair_high_confidence_json_issues(raw_text: str) -> str | None:
    repaired_chars: list[str] = []
    in_string = False
    escaping = False
    changed = False
    length = len(raw_text)
    index = 0

    while index < length:
        char = raw_text[index]

        if not in_string:
            repaired_chars.append(char)
            if char == '"':
                in_string = True
            index += 1
            continue

        if escaping:
            repaired_chars.append(char)
            escaping = False
            index += 1
            continue

        if char == "\\":
            repaired_chars.append(char)
            escaping = True
            index += 1
            continue

        if char == '"':
            next_non_ws = _next_non_whitespace(raw_text, index + 1)
            if next_non_ws in {",", "}", "]", ":"}:
                repaired_chars.append(char)
                in_string = False
            else:
                repaired_chars.append('\\"')
                changed = True
            index += 1
            continue

        repaired_chars.append(char)
        index += 1

    if not changed:
        return None
    return "".join(repaired_chars)


def _next_non_whitespace(raw_text: str, start_index: int) -> str | None:
    for index in range(start_index, len(raw_text)):
        if not raw_text[index].isspace():
            return raw_text[index]
    return None
