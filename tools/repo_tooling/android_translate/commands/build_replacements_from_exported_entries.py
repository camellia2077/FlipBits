from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

from commands.build_replacements_from_suggestions import _build_minimal_find_replace


@dataclass(frozen=True)
class BuildExportReplacementsResult:
    exit_code: int
    input_path: str
    output_path: str | None
    dir_name: str | None
    built_items: int
    skipped_items: int
    errors: tuple[str, ...]


def build_replacements_from_exported_entries(
    *,
    input_path: str | Path,
    output_path: str | Path,
) -> BuildExportReplacementsResult:
    input_path = Path(input_path)
    output_path = Path(output_path)

    if not input_path.exists():
        return BuildExportReplacementsResult(
            exit_code=1,
            input_path=str(input_path),
            output_path=None,
            dir_name=None,
            built_items=0,
            skipped_items=0,
            errors=(f"Entry export JSON not found: {input_path}",),
        )

    try:
        payload = json.loads(input_path.read_text(encoding="utf-8"))
    except Exception as exc:
        return BuildExportReplacementsResult(
            exit_code=2,
            input_path=str(input_path),
            output_path=None,
            dir_name=None,
            built_items=0,
            skipped_items=0,
            errors=(str(exc),),
        )
    if not isinstance(payload, dict):
        return BuildExportReplacementsResult(
            exit_code=2,
            input_path=str(input_path),
            output_path=None,
            dir_name=None,
            built_items=0,
            skipped_items=0,
            errors=("Entry export JSON must be an object.",),
        )

    dir_name = str(payload.get("dir", "")).strip()
    raw_items = payload.get("items")
    if not dir_name:
        return BuildExportReplacementsResult(
            exit_code=2,
            input_path=str(input_path),
            output_path=None,
            dir_name=None,
            built_items=0,
            skipped_items=0,
            errors=("Entry export JSON must include a non-empty dir field.",),
        )
    if not isinstance(raw_items, list):
        return BuildExportReplacementsResult(
            exit_code=2,
            input_path=str(input_path),
            output_path=None,
            dir_name=dir_name,
            built_items=0,
            skipped_items=0,
            errors=("Entry export JSON items must be a list.",),
        )

    errors: list[str] = []
    replacement_items: list[dict[str, str]] = []
    skipped_items = 0
    for index, item in enumerate(raw_items, start=1):
        if not isinstance(item, dict):
            errors.append(f"Entry #{index} is not an object.")
            continue
        name = str(item.get("name", "")).strip()
        current_text = item.get("current_text")
        proposed_text = item.get("proposed_text")
        if not name:
            errors.append(f"Entry #{index} has an empty name field.")
            continue
        if not isinstance(current_text, str):
            errors.append(f"Entry #{index} is missing current_text.")
            continue
        if not isinstance(proposed_text, str):
            errors.append(f"Entry #{index} is missing proposed_text.")
            continue
        minimal_change = _build_minimal_find_replace(current_text, proposed_text)
        if minimal_change is None:
            skipped_items += 1
            continue
        find_text, replace_text = minimal_change
        replacement_items.append(
            {
                "name": name,
                "find": find_text,
                "replace": replace_text,
            }
        )

    if errors:
        return BuildExportReplacementsResult(
            exit_code=2,
            input_path=str(input_path),
            output_path=None,
            dir_name=dir_name,
            built_items=0,
            skipped_items=skipped_items,
            errors=tuple(errors),
        )

    output_payload = {
        "dir": dir_name,
        "items": replacement_items,
    }
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(output_payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return BuildExportReplacementsResult(
        exit_code=0,
        input_path=str(input_path),
        output_path=str(output_path),
        dir_name=dir_name,
        built_items=len(replacement_items),
        skipped_items=skipped_items,
        errors=(),
    )
