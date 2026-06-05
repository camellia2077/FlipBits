from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import json
import re

from core.translation_paths import (
    APP_TEXT_GROUPS,
    DEFAULT_RES_DIRECTORY,
    DEFAULT_TRANSLATION_UNUSED_KEYS_DIRECTORY,
    FACTIONS,
    REPO_ROOT,
    TEXT_TYPES,
)
from core.translation_reporting import OutputDirectoryManager
from core.translation_resources import AndroidStringResourceRepository


DEFAULT_OUTPUT_DIRECTORY = DEFAULT_TRANSLATION_UNUSED_KEYS_DIRECTORY
SCANNED_FILE_SUFFIXES = {".gradle", ".java", ".kt", ".kts", ".xml"}
EXCLUDED_DIRECTORY_NAMES = {
    ".git",
    ".gradle",
    ".idea",
    ".pytest_cache",
    ".venv",
    "__pycache__",
    "build",
    "node_modules",
    "temp",
}
STRING_REFERENCE_PATTERNS = (
    re.compile(r"\bR\.string\.([A-Za-z0-9_]+)\b"),
    re.compile(r"@string/([A-Za-z0-9_]+)\b"),
    re.compile(r'getIdentifier\(\s*"([A-Za-z0-9_]+)"\s*,\s*"string"\b'),
)


@dataclass(frozen=True)
class UnusedTranslationKeyEntry:
    xml: str
    name: str
    en: str
    context: str | None
    text_type: str
    group: str


@dataclass(frozen=True)
class UnusedTranslationKeysResult:
    exit_code: int
    output_dir: Path
    suspicious_unused_key_count: int
    scanned_file_count: int
    scanned_string_reference_count: int
    report_file_count: int
    summary_json_path: str


class UnusedTranslationKeyChecker:
    def __init__(
        self,
        *,
        res_dir: Path | str = DEFAULT_RES_DIRECTORY,
        output_dir: Path | str = DEFAULT_OUTPUT_DIRECTORY,
        repo_root: Path | str = REPO_ROOT,
    ) -> None:
        self.repository = AndroidStringResourceRepository(res_dir)
        self.output_manager = OutputDirectoryManager(output_dir)
        self.repo_root = Path(repo_root)

    def run(
        self,
        *,
        text_type: str = "",
        group: str = "",
        key_pattern: str = "",
        quiet: bool = False,
        emit_text: bool = True,
    ) -> UnusedTranslationKeysResult:
        self.repository.ensure_base_directory()
        self.output_manager.reset()

        key_regex = re.compile(key_pattern) if key_pattern else None
        entries = self._load_candidate_entries(
            text_type=text_type,
            group=group,
            key_regex=key_regex,
        )
        used_keys, scanned_file_count, scanned_reference_count = self._scan_repo_for_string_usages()
        unused_entries = [entry for entry in entries if entry.name not in used_keys]

        markdown_path = self.output_manager.output_dir / "unused_translation_keys.md"
        summary_json_path = self.output_manager.output_dir / "unused_translation_keys.json"
        self._write_markdown_report(
            markdown_path,
            entries=unused_entries,
            scanned_file_count=scanned_file_count,
            scanned_reference_count=scanned_reference_count,
            text_type=text_type,
            group=group,
            key_pattern=key_pattern,
        )
        self._write_summary_json(
            summary_json_path,
            entries=unused_entries,
            scanned_file_count=scanned_file_count,
            scanned_reference_count=scanned_reference_count,
            text_type=text_type,
            group=group,
            key_pattern=key_pattern,
        )

        if emit_text and not quiet:
            print(f"Done. Suspicious unused keys: {len(unused_entries)}")
            print(f"Scanned files: {scanned_file_count}")
            print(f"Scanned string references: {scanned_reference_count}")
            print(f"Reports generated under: {self.output_manager.output_dir} (2 files)")

        return UnusedTranslationKeysResult(
            exit_code=0 if not unused_entries else 2,
            output_dir=self.output_manager.output_dir,
            suspicious_unused_key_count=len(unused_entries),
            scanned_file_count=scanned_file_count,
            scanned_string_reference_count=scanned_reference_count,
            report_file_count=2,
            summary_json_path=str(summary_json_path),
        )

    def _load_candidate_entries(
        self,
        *,
        text_type: str,
        group: str,
        key_regex: re.Pattern[str] | None,
    ) -> list[UnusedTranslationKeyEntry]:
        entries: list[UnusedTranslationKeyEntry] = []
        base_files = self.repository.load_base_resource_files()
        normalized_text_type = text_type.strip()
        normalized_group = group.strip()

        for resource_file in sorted(base_files.values(), key=lambda item: item.filename):
            if normalized_text_type and resource_file.text_type != normalized_text_type:
                continue
            if normalized_group and resource_file.faction != normalized_group:
                continue
            for key, english_text in resource_file.strings.items():
                if key_regex is not None and not key_regex.search(key):
                    continue
                entries.append(
                    UnusedTranslationKeyEntry(
                        xml=f"values/{resource_file.filename}",
                        name=key,
                        en=english_text,
                        context=resource_file.contexts.get(key),
                        text_type=resource_file.text_type,
                        group=resource_file.faction,
                    )
                )
        return entries

    def _scan_repo_for_string_usages(self) -> tuple[set[str], int, int]:
        used_keys: set[str] = set()
        scanned_file_count = 0
        scanned_reference_count = 0

        for path in self.repo_root.rglob("*"):
            if not path.is_file():
                continue
            if self._should_skip_path(path):
                continue
            if path.suffix not in SCANNED_FILE_SUFFIXES:
                continue
            try:
                text = path.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                continue
            except OSError:
                continue

            scanned_file_count += 1
            for pattern in STRING_REFERENCE_PATTERNS:
                matches = pattern.findall(text)
                if not matches:
                    continue
                used_keys.update(matches)
                scanned_reference_count += len(matches)
        return used_keys, scanned_file_count, scanned_reference_count

    def _should_skip_path(self, path: Path) -> bool:
        relative_path = path.relative_to(self.repo_root)
        if any(part in EXCLUDED_DIRECTORY_NAMES for part in relative_path.parts):
            return True
        if self._is_values_definition_xml(relative_path):
            return True
        return False

    @staticmethod
    def _is_values_definition_xml(relative_path: Path) -> bool:
        if relative_path.suffix != ".xml":
            return False
        parts = relative_path.parts
        if "res" not in parts:
            return False
        try:
            res_index = parts.index("res")
        except ValueError:
            return False
        if res_index + 1 >= len(parts):
            return False
        return parts[res_index + 1].startswith("values")

    def _write_markdown_report(
        self,
        output_path: Path,
        *,
        entries: list[UnusedTranslationKeyEntry],
        scanned_file_count: int,
        scanned_reference_count: int,
        text_type: str,
        group: str,
        key_pattern: str,
    ) -> None:
        scope_text_type = text_type or "all"
        scope_group = group or "all"
        scope_key_pattern = key_pattern or "*"
        lines = [
            "# Suspicious Unused Android String Keys",
            "",
            "This report is heuristic-only. It matches direct `R.string.key`, `@string/key`, and literal `getIdentifier(\"key\", \"string\", ...)` references.",
            "Dynamic resource-name construction is not resolved here, so review findings before deleting anything.",
            "",
            f"TEXT_TYPE: {scope_text_type}",
            f"GROUP: {scope_group}",
            f"KEY_PATTERN: {scope_key_pattern}",
            f"SCANNED_FILES: {scanned_file_count}",
            f"SCANNED_STRING_REFERENCES: {scanned_reference_count}",
            f"SUSPICIOUS_UNUSED_KEY_COUNT: {len(entries)}",
        ]
        if not entries:
            lines.extend(
                [
                    "",
                    "OK: No suspicious unused keys found in the selected scope.",
                ]
            )
        else:
            current_xml = ""
            for entry in entries:
                if entry.xml != current_xml:
                    current_xml = entry.xml
                    lines.extend(["", f"FILE: {current_xml}"])
                lines.extend(
                    [
                        "",
                        f"KEY: {entry.name}",
                        "ISSUE: no direct repo usage found",
                        f"TEXT_TYPE: {entry.text_type}",
                        f"GROUP: {entry.group}",
                    ]
                )
                if entry.context:
                    lines.append(f"CONTEXT: {entry.context}")
                lines.append(f"EN: {entry.en}")
        output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    def _write_summary_json(
        self,
        output_path: Path,
        *,
        entries: list[UnusedTranslationKeyEntry],
        scanned_file_count: int,
        scanned_reference_count: int,
        text_type: str,
        group: str,
        key_pattern: str,
    ) -> None:
        payload = {
            "command": "unused-keys",
            "scope": {
                "text_type": text_type or "",
                "group": group or "",
                "key_pattern": key_pattern or "",
            },
            "summary": {
                "suspicious_unused_key_count": len(entries),
                "scanned_file_count": scanned_file_count,
                "scanned_string_reference_count": scanned_reference_count,
            },
            "items": [
                {
                    "xml": entry.xml,
                    "name": entry.name,
                    "en": entry.en,
                    "context": entry.context,
                    "text_type": entry.text_type,
                    "group": entry.group,
                    "issue": "no direct repo usage found",
                }
                for entry in entries
            ],
            "notes": [
                "This report only matches direct R.string / @string / literal getIdentifier references.",
                "Review dynamic resource lookups manually before deleting keys.",
            ],
        }
        output_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def validate_unused_key_scope(*, text_type: str, group: str) -> None:
    normalized_text_type = text_type.strip()
    normalized_group = group.strip()
    if not normalized_text_type and not normalized_group:
        return
    if normalized_text_type and normalized_text_type not in TEXT_TYPES:
        raise ValueError(f"Unsupported text type: {normalized_text_type}")
    allowed_groups = {
        "app_text": set(APP_TEXT_GROUPS) | {"other"},
        "sample_text": set(FACTIONS) | {"other"},
    }
    if normalized_group:
        if not normalized_text_type:
            raise ValueError("`--group` requires `--text-type` so the scope is unambiguous.")
        if normalized_group not in allowed_groups[normalized_text_type]:
            raise ValueError(
                f"Unsupported group `{normalized_group}` for text type `{normalized_text_type}`."
            )


def run_unused_translation_key_check(
    *,
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    output_dir: Path | str = DEFAULT_OUTPUT_DIRECTORY,
    repo_root: Path | str = REPO_ROOT,
    text_type: str = "",
    group: str = "",
    key_pattern: str = "",
    quiet: bool = False,
    emit_text: bool = True,
) -> UnusedTranslationKeysResult:
    validate_unused_key_scope(text_type=text_type, group=group)
    checker = UnusedTranslationKeyChecker(
        res_dir=res_dir,
        output_dir=output_dir,
        repo_root=repo_root,
    )
    return checker.run(
        text_type=text_type,
        group=group,
        key_pattern=key_pattern,
        quiet=quiet,
        emit_text=emit_text,
    )
