from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re
import xml.etree.ElementTree as ET

from core.android_string_text import (
    decode_android_string_resource_text,
    encode_android_string_resource_text,
    find_high_risk_android_string_resource_patterns,
)
from core.translation_paths import iter_translation_text_xml_paths


@dataclass(frozen=True)
class AppliedReplacement:
    xml_path: Path
    string_name: str
    original_text: str
    updated_text: str


@dataclass(frozen=True)
class ReplacementAttemptResult:
    status: str
    original_text: str | None
    updated_text: str | None
    error: str | None = None


STRING_TAG_RE = re.compile(
    r"(?P<open><string\b[^>]*\bname=(?P<quote>['\"])(?P<name>[^'\"]+)(?P=quote)[^>]*>)"
    r"(?P<text>.*?)"
    r"(?P<close></string>)",
    re.DOTALL,
)


def validate_string_resource_xml(xml_path: Path, xml_text: str) -> list[str]:
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError as exc:
        return [f"{xml_path} | invalid XML: {exc}"]

    errors: list[str] = []
    if root.tag != "resources":
        errors.append(f"{xml_path} | unexpected root tag: {root.tag}")
        return errors

    if root.text and root.text.strip():
        errors.append(f"{xml_path} | unexpected text directly under <resources>: {root.text.strip()}")

    for child in root:
        if child.tag != "string":
            errors.append(f"{xml_path} | unsupported child tag under <resources>: {child.tag}")
            continue
        if child.tail and child.tail.strip():
            errors.append(
                f'{xml_path} | unexpected trailing text after <string name="{child.get("name", "")}">: {child.tail.strip()}'
            )

    return errors


def validate_android_string_risks(xml_path: Path, xml_text: str) -> list[str]:
    errors: list[str] = []
    for match in STRING_TAG_RE.finditer(xml_text):
        risks = find_high_risk_android_string_resource_patterns(match.group("text"))
        if not risks:
            continue
        risk_summary = "; ".join(dict.fromkeys(risks))
        errors.append(
            f"{xml_path} | string/{match.group('name')} | high-risk Android string escaping: {risk_summary}"
        )
    return errors


def load_localized_directory_index(
    res_dir: Path,
    dir_name: str,
) -> tuple[Path | None, dict[str, list[Path]], dict[Path, str], list[str]]:
    localized_dir = res_dir / dir_name
    if not localized_dir.exists() or not localized_dir.is_dir():
        return None, {}, {}, [f"localized values directory not found under res: {dir_name}"]
    if localized_dir.name != "values" and not localized_dir.name.startswith("values-"):
        return None, {}, {}, [f"replacement dir must target a values or values-* directory: {dir_name}"]

    name_index: dict[str, list[Path]] = {}
    xml_text_cache: dict[Path, str] = {}
    invalid_xml_errors: list[str] = []

    for xml_path in iter_translation_text_xml_paths(localized_dir):
        try:
            xml_text = xml_path.read_text(encoding="utf-8")
        except Exception as exc:
            invalid_xml_errors.append(f"{xml_path} | unreadable XML: {exc}")
            continue

        validation_errors = validate_string_resource_xml(xml_path, xml_text)
        if validation_errors:
            invalid_xml_errors.extend(validation_errors)
            continue
        xml_text_cache[xml_path] = xml_text
        for match in STRING_TAG_RE.finditer(xml_text):
            name_index.setdefault(match.group("name"), []).append(xml_path)

    return localized_dir, name_index, xml_text_cache, invalid_xml_errors


def resolve_string_name_path(
    name_index: dict[str, list[Path]],
    string_name: str,
    *,
    dir_name: str,
) -> tuple[Path | None, list[str]]:
    matches = name_index.get(string_name, [])
    if not matches:
        return None, [f'{dir_name} | string/{string_name} | string name not found in localized directory']
    if len(matches) > 1:
        match_lines = "\n".join(f"  - {path}" for path in matches)
        return None, [f'{dir_name} | string/{string_name} | string name matched multiple xml files\n{match_lines}']
    return matches[0], []


def apply_replacement_in_string(
    xml_path: Path,
    *,
    string_name: str,
    find_text: str,
    replace_text: str,
    xml_text: str,
) -> ReplacementAttemptResult:
    validation_errors = validate_string_resource_xml(xml_path, xml_text)
    if validation_errors:
        raise ValueError("\n".join(validation_errors))

    matched_name_count = 0
    original_text: str | None = None
    updated_text: str | None = None
    result_status = "unchanged"
    result_error: str | None = None

    def replace_match(match: re.Match[str]) -> str:
        nonlocal matched_name_count, original_text, updated_text, result_status, result_error
        if match.group("name") != string_name:
            return match.group(0)

        matched_name_count += 1
        current_text = decode_android_string_resource_text(match.group("text")).strip()
        if matched_name_count > 1:
            return match.group(0)

        occurrence_count = current_text.count(find_text)
        if occurrence_count == 0:
            original_text = current_text
            replace_occurrence_count = current_text.count(replace_text) if replace_text else 0
            if replace_occurrence_count == 1:
                result_status = "already_applied"
                updated_text = current_text
            else:
                result_status = "not_found"
                result_error = f"{xml_path} | string/{string_name} | find text not present in current string"
            return match.group(0)
        if occurrence_count > 1:
            original_text = current_text
            result_status = "ambiguous"
            result_error = f"{xml_path} | string/{string_name} | find text matched multiple times in current string"
            return match.group(0)

        original_text = current_text
        updated_text = current_text.replace(find_text, replace_text, 1)
        if updated_text == current_text:
            result_status = "unchanged"
            return match.group(0)

        result_status = "applied"
        return (
            f"{match.group('open')}"
            f"{encode_android_string_resource_text(updated_text)}"
            f"{match.group('close')}"
        )

    updated_xml_text = STRING_TAG_RE.sub(replace_match, xml_text)
    if matched_name_count == 0:
        raise ValueError(f"{xml_path} | string/{string_name} | string name not found in XML")
    if matched_name_count > 1:
        raise ValueError(f"{xml_path} | string/{string_name} | duplicate string name found in XML")
    if result_error is not None:
        return ReplacementAttemptResult(
            status=result_status,
            original_text=original_text,
            updated_text=updated_text,
            error=result_error,
        )

    if result_status == "applied":
        updated_validation_errors = validate_string_resource_xml(xml_path, updated_xml_text)
        if updated_validation_errors:
            raise ValueError("\n".join(updated_validation_errors))
        high_risk_errors = validate_android_string_risks(xml_path, updated_xml_text)
        if high_risk_errors:
            raise ValueError("\n".join(high_risk_errors))
        xml_path.write_text(updated_xml_text, encoding="utf-8")

    return ReplacementAttemptResult(
        status=result_status,
        original_text=original_text,
        updated_text=updated_text,
        error=None,
    )
