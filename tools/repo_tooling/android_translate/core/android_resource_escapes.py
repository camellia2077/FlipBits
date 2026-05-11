from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path

from core.android_string_text import normalize_android_string_resource_text
from core.translation_paths import DEFAULT_RES_DIRECTORY

STRING_ELEMENT_RE = re.compile(r"(<string\b[^>]*>)(.*?)(</string>)", re.DOTALL)


@dataclass(frozen=True)
class FixAndroidResourceEscapesResult:
    exit_code: int
    files_checked: int
    files_updated: int
    strings_updated: int
    updated_files: tuple[str, ...]


def _default_target_files(res_dir: Path) -> list[Path]:
    return sorted(path for path in res_dir.glob("values*/*.xml") if path.is_file())


def _normalize_android_string_elements(xml_text: str) -> tuple[str, int]:
    replacements = 0

    def replace_match(match: re.Match[str]) -> str:
        nonlocal replacements
        raw_inner = match.group(2)
        normalized_inner = normalize_android_string_resource_text(raw_inner)
        if normalized_inner != raw_inner:
            replacements += 1
            return f"{match.group(1)}{normalized_inner}{match.group(3)}"
        return match.group(0)

    updated_text = STRING_ELEMENT_RE.sub(replace_match, xml_text)
    return updated_text, replacements


def run_fix_android_resource_escapes(
    *,
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    files: list[str] | None = None,
    quiet: bool = False,
    emit_text: bool = True,
) -> FixAndroidResourceEscapesResult:
    res_dir_path = Path(res_dir).resolve()
    target_files = (
        [Path(path).resolve() for path in files]
        if files
        else _default_target_files(res_dir_path)
    )

    files_checked = 0
    files_updated = 0
    strings_updated = 0
    updated_files: list[str] = []

    for xml_path in target_files:
        if not xml_path.exists():
            raise FileNotFoundError(f"resource file not found: {xml_path}")
        xml_text = xml_path.read_text(encoding="utf-8")
        updated_text, replacement_count = _normalize_android_string_elements(xml_text)
        files_checked += 1
        if replacement_count <= 0:
            continue
        xml_path.write_text(updated_text, encoding="utf-8")
        files_updated += 1
        strings_updated += replacement_count
        updated_files.append(str(xml_path))

    if emit_text and not quiet:
        print(
            "Done. "
            f"Files checked: {files_checked}; "
            f"files updated: {files_updated}; "
            f"strings updated: {strings_updated}"
        )
        for path in updated_files:
            print(f"UPDATED: {path}")

    return FixAndroidResourceEscapesResult(
        exit_code=0,
        files_checked=files_checked,
        files_updated=files_updated,
        strings_updated=strings_updated,
        updated_files=tuple(updated_files),
    )
