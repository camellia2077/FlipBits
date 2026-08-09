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
    "dominant_risks",
    "state_signal_hits",
    "top_level_composables",
    "mode_branch_hits",
    "rule_helper_count",
    "responsibility_verb_kind_count",
    "interop_surface_hits",
    "resource_lifecycle_hits",
    "dependency_fanout",
    "implementation_sources",
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
            entries.append(_diff_entry_metadata(after) | {"status": "added", "after": _public(after)})
            continue
        if after is None:
            entries.append(_diff_entry_metadata(before) | {"status": "removed", "before": _public(before)})
            continue
        changes = _changes(before, after)
        status = "changed" if changes else "unchanged"
        if before.get("_matched") and not after.get("_matched"):
            status = "below_threshold"
        entries.append(
            _diff_entry_metadata(after)
            | {
                "status": status,
                "changes": changes,
            }
        )

    summary = {
        "added": sum(item["status"] == "added" for item in entries),
        "removed": sum(item["status"] == "removed" for item in entries),
        "changed": sum(item["status"] == "changed" for item in entries),
        "unchanged": sum(item["status"] == "unchanged" for item in entries),
    }
    below_threshold = sum(item["status"] == "below_threshold" for item in entries)
    if below_threshold:
        summary["below_threshold"] = below_threshold
    payload = {
        "baseline": str(baseline_path),
        "generated_at": report.generated_at,
        "summary": summary,
        "entries": entries,
    }
    before_inventory = _inventory_entries(baseline_entries)
    after_inventory = _inventory_entries(current_entries)
    if before_inventory or after_inventory:
        payload["fragmentation_delta"] = _fragmentation_delta(before_inventory, after_inventory)
        payload["dependency_fanout_delta"] = _dependency_fanout_delta(
            before_inventory,
            after_inventory,
        )
        payload["duplicate_owner"] = {
            "before": _duplicate_owner_groups(before_inventory),
            "after": _duplicate_owner_groups(after_inventory),
        }
    return payload


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
        for item in result.get("scanned_files", []):
            _add_entry(flattened, item, "file", prefix=prefix, matched=False, inventory=True)
        for item in result.get("matched_files", []):
            _add_entry(flattened, item, "file", prefix=prefix, matched=True, inventory=bool(result.get("scanned_files")))
        for item in result.get("matched_dirs", []):
            _add_entry(flattened, item, "directory", prefix=prefix)


def _add_entry(
    target: dict[str, dict[str, Any]],
    item: dict[str, Any],
    kind: str,
    *,
    prefix: str | None = None,
    matched: bool | None = None,
    inventory: bool = False,
) -> None:
    path = str(item.get("path", ""))
    key_prefix = f"{prefix}:" if prefix else ""
    target[f"{key_prefix}{kind}:{path}"] = {
        "path": path,
        "kind": kind,
        **({"part": prefix} if prefix else {}),
        **item,
        **({"_matched": matched} if matched is not None else {}),
        **({"_inventory": True} if inventory else {}),
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


def _public(item: dict[str, Any]) -> dict[str, Any]:
    return {key: value for key, value in item.items() if not key.startswith("_")}


def _inventory_entries(entries: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    return [item for item in entries.values() if item.get("_inventory")]


def _fragmentation_delta(
    before: list[dict[str, Any]],
    after: list[dict[str, Any]],
) -> dict[str, int]:
    def counts(items: list[dict[str, Any]]) -> dict[str, int]:
        fragment_paths = {
            str(source)
            for item in items
            for source in item.get("implementation_sources", [])
            if str(source).lower().endswith(".inc")
        }
        fragment_paths.update(
            str(item["path"])
            for item in items
            if item.get("is_fragment")
        )
        return {
            "file_count": len(items),
            "small_file_count": sum(bool(item.get("is_small_file")) for item in items),
            "fragment_count": len(fragment_paths),
        }

    before_counts = counts(before)
    after_counts = counts(after)
    return {
        key: after_counts[key] - before_counts[key]
        for key in before_counts
    }


def _dependency_fanout_delta(
    before: list[dict[str, Any]],
    after: list[dict[str, Any]],
) -> dict[str, int]:
    def total(items: list[dict[str, Any]]) -> int:
        return sum(int(item.get("dependency_fanout", 0)) for item in items)

    def maximum(items: list[dict[str, Any]]) -> int:
        return max((int(item.get("dependency_fanout", 0)) for item in items), default=0)

    return {
        "total": total(after) - total(before),
        "max": maximum(after) - maximum(before),
    }


def _duplicate_owner_groups(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    grouped: dict[str, list[dict[str, Any]]] = {}
    for item in items:
        fingerprint = item.get("content_fingerprint")
        if not fingerprint or int(item.get("lines", 0)) < 20:
            continue
        grouped.setdefault(str(fingerprint), []).append(item)

    duplicates: list[dict[str, Any]] = []
    for fingerprint, group in grouped.items():
        source_sets = {
            tuple(sorted(str(path) for path in item.get("implementation_sources", [])))
            for item in group
        }
        if len(group) < 2 or len(source_sets) < 2:
            continue
        duplicates.append(
            {
                "fingerprint": fingerprint,
                "paths": sorted(str(item["path"]) for item in group),
            }
        )
    return duplicates
