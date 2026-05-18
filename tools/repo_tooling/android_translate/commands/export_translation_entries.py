from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path

from core.translation_paths import (
    DEFAULT_RES_DIRECTORY,
    DEFAULT_TRANSLATIONS_TEMP_DIRECTORY,
    TEXT_TYPES,
    get_review_groups_for_text_type,
)
from core.translation_resources import AndroidStringResourceRepository

DEFAULT_OUTPUT_DIRECTORY = DEFAULT_TRANSLATIONS_TEMP_DIRECTORY / "entry_exports"


@dataclass(frozen=True)
class ExportTranslationEntriesResult:
    exit_code: int
    output_path: Path | None
    dir_name: str | None
    item_count: int
    errors: tuple[str, ...] = ()


def export_translation_entries(
    *,
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    output_path: Path | str = DEFAULT_OUTPUT_DIRECTORY / "entries.json",
    lang: str,
    text_type: str = "",
    group: str = "",
    key_pattern: str = "",
) -> ExportTranslationEntriesResult:
    repository = AndroidStringResourceRepository(res_dir)
    repository.ensure_base_directory()

    if not lang.strip():
        return ExportTranslationEntriesResult(
            exit_code=2,
            output_path=None,
            dir_name=None,
            item_count=0,
            errors=("export-entries requires --lang.",),
        )
    if text_type and text_type not in TEXT_TYPES:
        return ExportTranslationEntriesResult(
            exit_code=2,
            output_path=None,
            dir_name=None,
            item_count=0,
            errors=(f"Unsupported text type: {text_type}",),
        )

    valid_groups = set(get_review_groups_for_text_type("app_text")) | set(get_review_groups_for_text_type("sample_text"))
    if group and group not in valid_groups:
        return ExportTranslationEntriesResult(
            exit_code=2,
            output_path=None,
            dir_name=None,
            item_count=0,
            errors=(f"Unsupported group: {group}",),
        )

    try:
        key_regex = re.compile(key_pattern) if key_pattern else None
    except re.error as exc:
        return ExportTranslationEntriesResult(
            exit_code=2,
            output_path=None,
            dir_name=None,
            item_count=0,
            errors=(f"Invalid --key-pattern regex: {exc}",),
        )

    localized_dirs = dict(repository.iter_localized_directories())
    localized_dir = localized_dirs.get(lang)
    if localized_dir is None:
        return ExportTranslationEntriesResult(
            exit_code=2,
            output_path=None,
            dir_name=None,
            item_count=0,
            errors=(f"Unsupported lang filter: {lang}",),
        )

    base_files = repository.load_base_resource_files()
    dir_name = f"values-{lang}"
    items: list[dict[str, str | None]] = []
    for xml_name in sorted(name for name in base_files if (localized_dir / name).exists()):
        base_meta = base_files[xml_name]
        if text_type and base_meta.text_type != text_type:
            continue
        if group and base_meta.faction != group:
            continue

        localized_parsed = repository.extract_strings_from_xml(localized_dir / xml_name)
        for key, en_text in base_meta.strings.items():
            if key not in localized_parsed.strings:
                continue
            if key_regex and not key_regex.search(key):
                continue
            items.append(
                {
                    "xml": f"{dir_name}/{xml_name}",
                    "name": key,
                    "context": base_meta.contexts.get(key),
                    "en_text": en_text,
                    "current_text": localized_parsed.strings[key],
                    "proposed_text": localized_parsed.strings[key],
                }
            )

    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "dir": dir_name,
        "lang": lang,
        "text_type": text_type or None,
        "group": group or None,
        "key_pattern": key_pattern or None,
        "items": items,
    }
    output_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return ExportTranslationEntriesResult(
        exit_code=0,
        output_path=output_path,
        dir_name=dir_name,
        item_count=len(items),
    )
