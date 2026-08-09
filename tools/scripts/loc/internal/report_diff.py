from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from .report_models import ScanReport, ScopeReport


_COMPARE_FIELDS = (
    "lines",
    "files",
    "priority",
    "score",
    "confidence",
    "decision",
    "summary",
)


def build_scan_diff(*, baseline_path: Path, report: ScanReport | ScopeReport) -> dict[str, Any]:
    baseline_payload = json.loads(baseline_path.read_text(encoding="utf-8"))
    baseline_entries = _flatten_payload(baseline_payload)
    current_entries = _flatten_report(report)

    entries: list[dict[str, Any]] = []
    for key in sorted(set(baseline_entries) | set(current_entries)):
        before = baseline_entries.get(key)
        after = current_entries.get(key)
        if before is None:
            entries.append(_diff_entry_metadata(after) | {"status": "added", "after": after})
            continue
        if after is None:
            entries.append(_diff_entry_metadata(before) | {"status": "removed", "before": before})
            continue
        changes = _changes(before, after)
        entries.append(
            _diff_entry_metadata(after)
            | {
                "status": "changed" if changes else "unchanged",
                "changes": changes,
            }
        )

    summary = {
        "added": sum(item["status"] == "added" for item in entries),
        "removed": sum(item["status"] == "removed" for item in entries),
        "changed": sum(item["status"] == "changed" for item in entries),
        "unchanged": sum(item["status"] == "unchanged" for item in entries),
    }
    return {
        "baseline": str(baseline_path),
        "generated_at": report.generated_at,
        "summary": summary,
        "entries": entries,
    }


def _flatten_report(report: ScanReport | ScopeReport) -> dict[str, dict[str, Any]]:
    return _flatten_payload(report.to_dict())


def _flatten_payload(payload: dict[str, Any]) -> dict[str, dict[str, Any]]:
    flattened: dict[str, dict[str, Any]] = {}
    for part in payload.get("parts", []):
        part_name = str(part.get("part", part.get("lang", "")))
        _flatten_results(
            flattened,
            part.get("results", []),
            prefix=part_name,
        )
    _flatten_results(flattened, payload.get("results", []))
    return flattened


def _flatten_results(
    flattened: dict[str, dict[str, Any]],
    results: list[dict[str, Any]],
    *,
    prefix: str | None = None,
) -> None:
    for result in results:
        for item in result.get("matched_files", []):
            _add_entry(flattened, item, "file", prefix=prefix)
        for item in result.get("matched_dirs", []):
            _add_entry(flattened, item, "directory", prefix=prefix)


def _add_entry(
    target: dict[str, dict[str, Any]],
    item: dict[str, Any],
    kind: str,
    *,
    prefix: str | None = None,
) -> None:
    path = str(item.get("path", ""))
    key_prefix = f"{prefix}:" if prefix else ""
    target[f"{key_prefix}{kind}:{path}"] = {
        "path": path,
        "kind": kind,
        **({"part": prefix} if prefix else {}),
        **item,
    }


def _diff_entry_metadata(item: dict[str, Any]) -> dict[str, Any]:
    metadata = {"path": item["path"], "kind": item["kind"]}
    if "part" in item:
        metadata["part"] = item["part"]
    return metadata


def _changes(before: dict[str, Any], after: dict[str, Any]) -> dict[str, dict[str, Any]]:
    changes: dict[str, dict[str, Any]] = {}
    for field in _COMPARE_FIELDS:
        if before.get(field) != after.get(field):
            changes[field] = {"before": before.get(field), "after": after.get(field)}
    return changes
