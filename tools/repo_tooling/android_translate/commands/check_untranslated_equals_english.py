from __future__ import annotations

from dataclasses import dataclass
import json
from pathlib import Path
import re

from core.mixed_language_policy import load_mixed_language_policy
from core.translation_agent_tasks import infer_length_pressure, infer_ui_surface
from core.translation_paths import (
    DEFAULT_RES_DIRECTORY,
    DEFAULT_TRANSLATION_UNTRANSLATED_ENGLISH_DIRECTORY,
    display_language_tag,
    is_pro_sample_key,
)
from core.translation_reporting import MinimalMarkdownReportWriter, OutputDirectoryManager, ReportFileBlock, ReportKeyBlock
from core.translation_resources import AndroidStringResourceRepository, ResourceFile


@dataclass(frozen=True)
class UntranslatedEqualsEnglishResult:
    exit_code: int
    output_dir: Path
    total_entries: int
    keep_en_count: int
    needs_translation_count: int
    needs_context_count: int
    missing_context_count: int
    report_file_count: int
    summary_json_path: str | None
    per_language: dict[str, dict[str, int]]


class UntranslatedEqualsEnglishGenerator:
    def __init__(
        self,
        *,
        res_dir: Path | str = DEFAULT_RES_DIRECTORY,
        output_dir: Path | str | None = DEFAULT_TRANSLATION_UNTRANSLATED_ENGLISH_DIRECTORY,
        lang: str | None = None,
    ) -> None:
        self.repository = AndroidStringResourceRepository(res_dir)
        self.output_manager = OutputDirectoryManager(output_dir or DEFAULT_TRANSLATION_UNTRANSLATED_ENGLISH_DIRECTORY)
        self.lang = lang.strip() if lang else None
        self.writer = MinimalMarkdownReportWriter()
        self.full_policy = load_mixed_language_policy()
        self.audit_policy = self.full_policy.audit
        self.shared_locked_terms = frozenset(item.lower() for item in self.full_policy.detection.shared_locked_terms)
        self.format_only_literals = frozenset({"", "hz", "0x"})

    def run(self, *, quiet: bool = False, emit_text: bool = True) -> UntranslatedEqualsEnglishResult:
        self.repository.ensure_base_directory()
        base_files = self.repository.load_base_resource_files()
        self.output_manager.reset()

        localized_dirs = dict(self.repository.iter_localized_directories())
        if self.lang:
            if self.lang not in localized_dirs:
                raise ValueError(f"Unsupported lang filter: {self.lang}")
            localized_dirs = {self.lang: localized_dirs[self.lang]}

        total_entries = 0
        keep_en_count = 0
        needs_translation_count = 0
        needs_context_count = 0
        missing_context_count = 0
        report_file_count = 0
        per_language: dict[str, dict[str, int]] = {}

        for lang_code, folder_path in localized_dirs.items():
            categorized = self._collect_language_entries(lang_code=lang_code, folder_path=folder_path, base_files=base_files)
            keep_en_entries = [entry for entry in categorized if entry["bucket"] == "keep_en"]
            needs_translation_entries = [entry for entry in categorized if entry["bucket"] == "needs_translation"]
            needs_context_entries = [entry for entry in categorized if entry["bucket"] == "needs_context"]
            missing_context_entries = [entry for entry in categorized if not entry.get("context")]

            per_language[lang_code] = {
                "total_entries": len(categorized),
                "keep_en": len(keep_en_entries),
                "needs_translation": len(needs_translation_entries),
                "needs_context": len(needs_context_entries),
                "missing_context": len(missing_context_entries),
            }

            if not categorized:
                continue

            lang_dir = self.output_manager.output_dir / lang_code
            self._write_language_json(
                lang_dir / f"{lang_code}_untranslated_equals_english.json",
                lang_code=lang_code,
                keep_en_entries=keep_en_entries,
                needs_translation_entries=needs_translation_entries,
                needs_context_entries=needs_context_entries,
                missing_context_entries=missing_context_entries,
            )
            report_file_count += self._write_language_reports(
                lang_dir=lang_dir,
                lang_code=lang_code,
                keep_en_entries=keep_en_entries,
                needs_translation_entries=needs_translation_entries,
                needs_context_entries=needs_context_entries,
                missing_context_entries=missing_context_entries,
            )

            total_entries += len(categorized)
            keep_en_count += len(keep_en_entries)
            needs_translation_count += len(needs_translation_entries)
            needs_context_count += len(needs_context_entries)
            missing_context_count += len(missing_context_entries)

        summary_json_path = self._write_summary_json(
            self.output_manager.output_dir / "untranslated_equals_english_summary.json",
            per_language=per_language,
        )

        if emit_text and not quiet:
            print(f"Done. Exact EN matches: {total_entries}")
            print(
                "Buckets: "
                f"keep_en={keep_en_count}, "
                f"needs_translation={needs_translation_count}, "
                f"needs_context={needs_context_count}"
            )
            print(f"Missing CONTEXT: {missing_context_count}")
            print(f"Artifacts generated under: {self.output_manager.output_dir} ({report_file_count} files)")

        return UntranslatedEqualsEnglishResult(
            exit_code=0,
            output_dir=self.output_manager.output_dir,
            total_entries=total_entries,
            keep_en_count=keep_en_count,
            needs_translation_count=needs_translation_count,
            needs_context_count=needs_context_count,
            missing_context_count=missing_context_count,
            report_file_count=report_file_count,
            summary_json_path=summary_json_path,
            per_language=per_language,
        )

    def _collect_language_entries(
        self,
        *,
        lang_code: str,
        folder_path: Path,
        base_files: dict[str, ResourceFile],
    ) -> list[dict[str, object]]:
        entries: list[dict[str, object]] = []
        xml_names = self.repository.localized_xml_names(folder_path, base_files)
        for filename in xml_names:
            resource_file = base_files[filename]
            localized_strings = self.repository.load_localized_strings(folder_path, filename)
            for key, english_text in resource_file.strings.items():
                localized_text = localized_strings.get(key)
                if localized_text is None or localized_text != english_text:
                    continue
                entry = self._categorize_entry(
                    lang_code=lang_code,
                    folder_name=folder_path.name,
                    resource_file=resource_file,
                    key=key,
                    english_text=english_text,
                    localized_text=localized_text,
                )
                entries.append(entry)
        return entries

    def _categorize_entry(
        self,
        *,
        lang_code: str,
        folder_name: str,
        resource_file: ResourceFile,
        key: str,
        english_text: str,
        localized_text: str,
    ) -> dict[str, object]:
        context = resource_file.contexts.get(key)
        context_lower = (context or "").lower()
        english_lower = english_text.strip().lower()
        language_keep_exact = self.audit_policy.per_language_keep_exact.get(lang_code, frozenset())

        if self._is_format_only_value(english_text):
            bucket = "keep_en"
            reason = "value is a placeholder/format skeleton rather than untranslated prose"
        elif is_pro_sample_key(key) or "strict ascii" in context_lower or "plain ascii" in context_lower:
            bucket = "keep_en"
            reason = "ASCII-only content is expected to stay in English"
        elif (
            english_lower in self.shared_locked_terms
            or english_lower in self.audit_policy.keep_en_exact
            or english_lower in language_keep_exact
        ):
            bucket = "keep_en"
            reason = "exact value is allowed to stay unchanged for this locale"
        elif any(hint in context_lower for hint in self.audit_policy.keep_en_context_hints):
            bucket = "keep_en"
            reason = "context says this English form should stay"
        elif not context:
            bucket = "needs_context"
            reason = "missing CONTEXT annotation"
        else:
            bucket = "needs_translation"
            reason = "localized text is still identical to the English source"

        return {
            "dir": folder_name,
            "xml": f"{folder_name}/{resource_file.filename}",
            "name": key,
            "context": context,
            "sample_length": resource_file.sample_lengths.get(key),
            "ui_surface": infer_ui_surface(resource_file=resource_file, key=key, context=context),
            "length_pressure": infer_length_pressure(
                sample_length=resource_file.sample_lengths.get(key),
                key=key,
                context=context,
            ),
            "en": english_text,
            "localized": localized_text,
            "bucket": bucket,
            "audit_reason": reason,
        }

    def _is_format_only_value(self, text: str) -> bool:
        normalized = text.strip().lower()
        normalized = re.sub(r"%\d+\$[sdif]", "", normalized)
        normalized = normalized.replace("-&gt;", "").replace("->", "")
        skeleton = re.sub(r"[^a-z0-9#]+", "", normalized)
        return skeleton in self.format_only_literals

    def _write_language_json(
        self,
        output_path: Path,
        *,
        lang_code: str,
        keep_en_entries: list[dict[str, object]],
        needs_translation_entries: list[dict[str, object]],
        needs_context_entries: list[dict[str, object]],
        missing_context_entries: list[dict[str, object]],
    ) -> str:
        payload = {
            "task_version": 1,
            "task_type": "untranslated_equals_english",
            "language": lang_code,
            "language_tag": display_language_tag(lang_code),
            "execution_contract": {
                "json_first": True,
                "markdown_optional": True,
                "primary_task_fields": (
                    "bucket",
                    "audit_reason",
                    "context",
                    "ui_surface",
                    "length_pressure",
                    "en",
                    "localized",
                ),
            },
            "summary": {
                "keep_en": len(keep_en_entries),
                "needs_translation": len(needs_translation_entries),
                "needs_context": len(needs_context_entries),
                "missing_context": len(missing_context_entries),
            },
            "buckets": {
                "keep_en": keep_en_entries,
                "needs_translation": needs_translation_entries,
                "needs_context": needs_context_entries,
            },
            "missing_context_entries": missing_context_entries,
        }
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return str(output_path)

    def _write_summary_json(self, output_path: Path, *, per_language: dict[str, dict[str, int]]) -> str:
        payload = {
            "task_version": 1,
            "task_type": "untranslated_equals_english_summary",
            "per_language": per_language,
        }
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return str(output_path)

    def _write_language_reports(
        self,
        *,
        lang_dir: Path,
        lang_code: str,
        keep_en_entries: list[dict[str, object]],
        needs_translation_entries: list[dict[str, object]],
        needs_context_entries: list[dict[str, object]],
        missing_context_entries: list[dict[str, object]],
    ) -> int:
        report_specs = (
            ("keep_en.md", "keep_en", keep_en_entries),
            ("needs_translation.md", "needs_translation", needs_translation_entries),
            ("needs_context.md", "needs_context", needs_context_entries),
            ("missing_context.md", "missing_context", missing_context_entries),
        )
        count = 0
        for filename, section, entries in report_specs:
            if not entries:
                continue
            self.writer.write(
                lang_dir / filename,
                title=f"Untranslated English Audit [{display_language_tag(lang_code)}]",
                section=section,
                metadata_lines=(f"ENTRY_COUNT: {len(entries)}",),
                file_blocks=self._build_file_blocks(entries),
            )
            count += 1
        return count

    @staticmethod
    def _build_file_blocks(entries: list[dict[str, object]]) -> tuple[ReportFileBlock, ...]:
        by_xml: dict[str, list[dict[str, object]]] = {}
        for entry in entries:
            xml = str(entry.get("xml") or "")
            by_xml.setdefault(xml, []).append(entry)

        file_blocks: list[ReportFileBlock] = []
        for xml, xml_entries in sorted(by_xml.items()):
            key_blocks: list[ReportKeyBlock] = []
            for entry in xml_entries:
                fields: list[tuple[str, str]] = []
                if entry.get("audit_reason"):
                    fields.append(("REASON", str(entry["audit_reason"])))
                if entry.get("context"):
                    fields.append(("CONTEXT", str(entry["context"])))
                else:
                    fields.append(("CONTEXT", "[MISSING CONTEXT]"))
                if entry.get("ui_surface"):
                    fields.append(("UI_SURFACE", str(entry["ui_surface"])))
                if entry.get("length_pressure"):
                    fields.append(("LENGTH_PRESSURE", str(entry["length_pressure"])))
                fields.append(("TR", str(entry.get("localized") or "")))
                fields.append(("EN", str(entry.get("en") or "")))
                key_blocks.append(
                    ReportKeyBlock(
                        key=str(entry.get("name") or ""),
                        fields=tuple(fields),
                    )
                )
            file_blocks.append(ReportFileBlock(filename=xml, key_blocks=tuple(key_blocks)))
        return tuple(file_blocks)


def run_untranslated_equals_english_check(
    *,
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    output_dir: Path | str | None = DEFAULT_TRANSLATION_UNTRANSLATED_ENGLISH_DIRECTORY,
    lang: str | None = None,
    quiet: bool = False,
    emit_text: bool = True,
) -> UntranslatedEqualsEnglishResult:
    generator = UntranslatedEqualsEnglishGenerator(
        res_dir=res_dir,
        output_dir=output_dir,
        lang=lang,
    )
    return generator.run(quiet=quiet, emit_text=emit_text)
