from __future__ import annotations

import json
from dataclasses import dataclass
import re


SUSPICIOUS_MANUAL_ESCAPE_RE = re.compile(r"(\\'|\\\"|\\@|\\\?)")
MISSING_TRANSLATION_SENTINEL = "[MISSING TRANSLATION / 此条目未翻译]"


@dataclass(frozen=True)
class ReplacementEntry:
    name: str
    find: str
    replace: str


@dataclass(frozen=True)
class ReplacementBatch:
    dir_name: str
    items: tuple[ReplacementEntry, ...]


def load_replacement_entries(raw_json_text: str) -> ReplacementBatch:
    raw_payload = json.loads(raw_json_text)
    if not isinstance(raw_payload, dict):
        raise ValueError("Replacement JSON must be an object with dir and items.")
    missing_fields = [field for field in ("dir", "items") if field not in raw_payload]
    if missing_fields:
        raise ValueError(f"Replacement JSON is missing fields: {', '.join(missing_fields)}")

    dir_name = str(raw_payload["dir"]).strip()
    if not dir_name:
        raise ValueError("Replacement JSON has an empty dir field.")

    raw_items = raw_payload["items"]
    if not isinstance(raw_items, list):
        raise ValueError("Replacement JSON items must be a list.")

    entries: list[ReplacementEntry] = []
    for index, item in enumerate(raw_items, start=1):
        if not isinstance(item, dict):
            raise ValueError(f"Entry #{index} is not an object.")

        missing_fields = [field for field in ("name", "find", "replace") if field not in item]
        if missing_fields:
            raise ValueError(f"Entry #{index} is missing fields: {', '.join(missing_fields)}")

        name_value = str(item["name"]).strip()
        find_value = str(item["find"])
        replace_value = str(item["replace"])

        if not name_value:
            raise ValueError(f"Entry #{index} has an empty name field.")
        if not find_value:
            raise ValueError(f"Entry #{index} has an empty find field.")
        if find_value == MISSING_TRANSLATION_SENTINEL or "MISSING TRANSLATION" in find_value:
            raise ValueError(
                f"Entry #{index} uses the missing-translation placeholder as find.\n"
                "Replacements must target real localized text, not a report placeholder."
            )

        suspicious_issues: list[str] = []
        for field, value in (("find", find_value), ("replace", replace_value)):
            suspicious_match = SUSPICIOUS_MANUAL_ESCAPE_RE.search(value)
            if suspicious_match is not None:
                suspicious_issues.append(
                    f"{field} contains manual Android resource escape {suspicious_match.group(0)!r}: {value}"
                )
        if suspicious_issues:
            raise ValueError(
                f"Entry #{index} contains manual Android resource escapes.\n"
                + "\n".join(suspicious_issues)
                + "\nUse plain natural-language text in JSON; the replacement tool handles Android resource escaping."
            )

        entries.append(
            ReplacementEntry(
                name=name_value,
                find=find_value,
                replace=replace_value,
            )
        )
    return ReplacementBatch(dir_name=dir_name, items=tuple(entries))
