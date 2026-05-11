from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

from core.translation_paths import DEFAULT_RES_DIRECTORY
from core.translation_resources import AndroidStringResourceRepository


@dataclass(frozen=True)
class BuildReplacementsResult:
    exit_code: int
    input_path: str
    output_path: str | None
    dir_name: str | None
    built_items: int
    skipped_items: int
    errors: tuple[str, ...]


def _load_json_object(path: Path) -> dict[str, object]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError("Suggestion JSON must be an object.")
    return payload


def _build_minimal_find_replace(current_text: str, proposed_text: str) -> tuple[str, str] | None:
    if current_text == proposed_text:
        return None

    prefix = 0
    max_prefix = min(len(current_text), len(proposed_text))
    while prefix < max_prefix and current_text[prefix] == proposed_text[prefix]:
        prefix += 1

    current_suffix = len(current_text)
    proposed_suffix = len(proposed_text)
    while (
        current_suffix > prefix
        and proposed_suffix > prefix
        and current_text[current_suffix - 1] == proposed_text[proposed_suffix - 1]
    ):
        current_suffix -= 1
        proposed_suffix -= 1

    while (
        prefix > 0
        and current_suffix > prefix
        and proposed_suffix > prefix
        and current_text[prefix - 1].isalnum()
        and current_text[prefix].isalnum()
        and proposed_text[prefix - 1].isalnum()
        and proposed_text[prefix].isalnum()
    ):
        prefix -= 1

    while (
        current_suffix < len(current_text)
        and proposed_suffix < len(proposed_text)
        and current_suffix > prefix
        and proposed_suffix > prefix
        and current_text[current_suffix - 1].isalnum()
        and current_text[current_suffix].isalnum()
        and proposed_text[proposed_suffix - 1].isalnum()
        and proposed_text[proposed_suffix].isalnum()
    ):
        current_suffix += 1
        proposed_suffix += 1

    find_text = current_text[prefix:current_suffix]
    replace_text = proposed_text[prefix:proposed_suffix]
    if not find_text:
        find_text = current_text
        replace_text = proposed_text
    return find_text, replace_text


def build_replacements_from_suggestions(
    *,
    input_path: str | Path,
    output_path: str | Path,
    res_dir: str | Path = DEFAULT_RES_DIRECTORY,
) -> BuildReplacementsResult:
    input_path = Path(input_path)
    output_path = Path(output_path)
    res_dir = Path(res_dir)

    if not input_path.exists():
        return BuildReplacementsResult(
            exit_code=1,
            input_path=str(input_path),
            output_path=None,
            dir_name=None,
            built_items=0,
            skipped_items=0,
            errors=(f"Suggestion JSON not found: {input_path}",),
        )

    try:
        payload = _load_json_object(input_path)
    except Exception as exc:
        return BuildReplacementsResult(
            exit_code=2,
            input_path=str(input_path),
            output_path=None,
            dir_name=None,
            built_items=0,
            skipped_items=0,
            errors=(str(exc),),
        )

    dir_name = str(payload.get("dir", "")).strip()
    raw_items = payload.get("items")
    if not dir_name:
        return BuildReplacementsResult(
            exit_code=2,
            input_path=str(input_path),
            output_path=None,
            dir_name=None,
            built_items=0,
            skipped_items=0,
            errors=("Suggestion JSON must include a non-empty dir field.",),
        )
    if not isinstance(raw_items, list):
        return BuildReplacementsResult(
            exit_code=2,
            input_path=str(input_path),
            output_path=None,
            dir_name=dir_name,
            built_items=0,
            skipped_items=0,
            errors=("Suggestion JSON items must be a list.",),
        )

    repository = AndroidStringResourceRepository(res_dir)
    target_dir = res_dir / dir_name
    if not target_dir.exists():
        return BuildReplacementsResult(
            exit_code=2,
            input_path=str(input_path),
            output_path=None,
            dir_name=dir_name,
            built_items=0,
            skipped_items=0,
            errors=(f"Localized directory not found: {target_dir}",),
        )

    errors: list[str] = []
    replacement_items: list[dict[str, str]] = []
    skipped_items = 0

    for index, item in enumerate(raw_items, start=1):
        if not isinstance(item, dict):
            errors.append(f"Entry #{index} is not an object.")
            continue
        name = str(item.get("name", "")).strip()
        xml_value = str(item.get("xml", "")).strip()
        replace_full = item.get("replace_full")
        if not name:
            errors.append(f"Entry #{index} has an empty name field.")
            continue
        if not isinstance(replace_full, str):
            errors.append(f"Entry #{index} is missing replace_full.")
            continue
        xml_filename = Path(xml_value).name if xml_value else ""
        if not xml_filename:
            errors.append(f"Entry #{index} is missing xml.")
            continue

        localized_strings = repository.load_localized_strings(target_dir, xml_filename)
        current_text = localized_strings.get(name)
        if current_text is None:
            errors.append(f"Entry #{index} could not find {name} in {dir_name}/{xml_filename}.")
            continue

        minimal_change = _build_minimal_find_replace(current_text, replace_full)
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
        return BuildReplacementsResult(
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
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(output_payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return BuildReplacementsResult(
        exit_code=0,
        input_path=str(input_path),
        output_path=str(output_path),
        dir_name=dir_name,
        built_items=len(replacement_items),
        skipped_items=skipped_items,
        errors=(),
    )
