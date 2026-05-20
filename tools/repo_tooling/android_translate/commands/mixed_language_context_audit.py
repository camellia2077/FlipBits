from __future__ import annotations

from dataclasses import dataclass
import json
from pathlib import Path

from commands.check_mixed_language import MixedLanguageReportGenerator
from core.mixed_language_policy import load_mixed_language_policy
from core.translation_paths import (
    DEFAULT_RES_DIRECTORY,
    DEFAULT_TRANSLATION_MIXED_LANGUAGE_AUDIT_DIRECTORY,
    display_language_tag,
)
from core.translation_reporting import MinimalMarkdownReportWriter, OutputDirectoryManager, ReportFileBlock, ReportKeyBlock


@dataclass(frozen=True)
class MixedLanguageContextAuditResult:
    exit_code: int
    output_dir: Path
    total_entries: int
    keep_en_count: int
    needs_translation_count: int
    needs_context_count: int
    missing_context_count: int
    report_file_count: int
    json_path: str | None


class MixedLanguageContextAuditGenerator:
    def __init__(
        self,
        *,
        res_dir: Path | str = DEFAULT_RES_DIRECTORY,
        output_dir: Path | str | None = DEFAULT_TRANSLATION_MIXED_LANGUAGE_AUDIT_DIRECTORY,
        lang: str,
    ) -> None:
        self.output_manager = OutputDirectoryManager(output_dir or DEFAULT_TRANSLATION_MIXED_LANGUAGE_AUDIT_DIRECTORY)
        self.lang = lang.strip()
        self.source_generator = MixedLanguageReportGenerator(
            res_dir=res_dir,
            output_dir=output_dir or DEFAULT_TRANSLATION_MIXED_LANGUAGE_AUDIT_DIRECTORY,
            lang=self.lang,
        )
        self.writer = MinimalMarkdownReportWriter()
        self.full_policy = load_mixed_language_policy()
        self.policy = self.full_policy.audit
        self.shared_locked_terms = frozenset(item.lower() for item in self.full_policy.detection.shared_locked_terms)

    def run(
        self,
        *,
        quiet: bool = False,
        emit_text: bool = True,
    ) -> MixedLanguageContextAuditResult:
        self.output_manager.reset()
        result = self.source_generator.run(quiet=True, emit_text=False)

        task_entries = self._load_source_task_entries(result.task_json_paths)
        categorized = [self._categorize_entry(entry) for entry in task_entries]

        keep_en_entries = [entry for entry in categorized if entry["bucket"] == "keep_en"]
        needs_translation_entries = [entry for entry in categorized if entry["bucket"] == "needs_translation"]
        needs_context_entries = [entry for entry in categorized if entry["bucket"] == "needs_context"]
        missing_context_entries = [entry for entry in categorized if not entry.get("context")]

        lang_dir = self.output_manager.output_dir / self.lang
        json_path = self._write_json(
            lang_dir / f"{self.lang}_mixed_language_context_audit.json",
            keep_en_entries=keep_en_entries,
            needs_translation_entries=needs_translation_entries,
            needs_context_entries=needs_context_entries,
            missing_context_entries=missing_context_entries,
        )
        report_count = self._write_reports(
            lang_dir=lang_dir,
            keep_en_entries=keep_en_entries,
            needs_translation_entries=needs_translation_entries,
            needs_context_entries=needs_context_entries,
            missing_context_entries=missing_context_entries,
        )

        if emit_text and not quiet:
            print(f"Done. Audited entries: {len(categorized)}")
            print(
                "Buckets: "
                f"keep_en={len(keep_en_entries)}, "
                f"needs_translation={len(needs_translation_entries)}, "
                f"needs_context={len(needs_context_entries)}"
            )
            print(f"Missing CONTEXT: {len(missing_context_entries)}")
            print(f"Reports generated under: {self.output_manager.output_dir} ({report_count} files)")

        return MixedLanguageContextAuditResult(
            exit_code=0,
            output_dir=self.output_manager.output_dir,
            total_entries=len(categorized),
            keep_en_count=len(keep_en_entries),
            needs_translation_count=len(needs_translation_entries),
            needs_context_count=len(needs_context_entries),
            missing_context_count=len(missing_context_entries),
            report_file_count=report_count,
            json_path=json_path,
        )

    @staticmethod
    def _load_source_task_entries(task_json_paths: tuple[str, ...]) -> list[dict[str, object]]:
        entries: list[dict[str, object]] = []
        for path_str in task_json_paths:
            payload = json.loads(Path(path_str).read_text(encoding="utf-8"))
            for entry in payload.get("entries", []):
                if isinstance(entry, dict):
                    entries.append(entry)
        return entries

    def _categorize_entry(self, entry: dict[str, object]) -> dict[str, object]:
        context = str(entry.get("context") or "").strip()
        suspicious_chunks = [
            str(chunk).strip()
            for chunk in entry.get("suspicious_chunks", [])
            if str(chunk).strip()
        ]
        context_lower = context.lower()
        suspicious_lower = {chunk.lower() for chunk in suspicious_chunks}

        if not context:
            bucket = "needs_context"
            reason = "missing CONTEXT annotation"
        elif "strict ascii" in context_lower or "plain ascii" in context_lower:
            bucket = "keep_en"
            reason = "ASCII-only example is expected to stay in target text"
        elif self._is_keep_en(entry=entry, context_lower=context_lower, suspicious_lower=suspicious_lower):
            bucket = "keep_en"
            reason = "suspicious English is expected to stay in target text"
        elif self._looks_like_translation_ui_copy(entry=entry, context_lower=context_lower):
            bucket = "needs_translation"
            reason = "UI/help/status copy should be translated with available context"
        else:
            bucket = "needs_context"
            reason = "current context is not strong enough to decide keep-vs-translate safely"

        out = dict(entry)
        out["bucket"] = bucket
        out["audit_reason"] = reason
        return out

    def _is_keep_en(
        self,
        *,
        entry: dict[str, object],
        context_lower: str,
        suspicious_lower: set[str],
    ) -> bool:
        if any(chunk in self.shared_locked_terms for chunk in suspicious_lower):
            return True
        if any(chunk in self.policy.keep_en_exact for chunk in suspicious_lower):
            return True
        if any(hint in context_lower for hint in self.policy.keep_en_context_hints):
            return True

        localized = str(entry.get("localized") or "")
        suspicious_chunks = [str(chunk) for chunk in entry.get("suspicious_chunks", [])]
        if localized and suspicious_chunks and all(chunk and chunk in localized for chunk in suspicious_chunks):
            if "strict ascii" in context_lower:
                return True
        return False

    @staticmethod
    def _looks_like_translation_ui_copy(*, entry: dict[str, object], context_lower: str) -> bool:
        policy = load_mixed_language_policy().audit
        if "strict ascii" in context_lower or "plain ascii" in context_lower:
            return False
        if any(hint in context_lower for hint in policy.needs_translation_context_hints):
            return True
        localized = str(entry.get("localized") or "")
        english = str(entry.get("en") or "")
        if localized and english and localized == english:
            return True
        return False

    def _write_json(
        self,
        output_path: Path,
        *,
        keep_en_entries: list[dict[str, object]],
        needs_translation_entries: list[dict[str, object]],
        needs_context_entries: list[dict[str, object]],
        missing_context_entries: list[dict[str, object]],
    ) -> str:
        payload = {
            "task_version": 2,
            "task_type": "mixed_language_context_audit",
            "language": self.lang,
            "language_tag": display_language_tag(self.lang),
            "execution_contract": {
                "json_first": True,
                "markdown_optional": True,
                "primary_task_fields": (
                    "bucket",
                    "audit_reason",
                    "suspicious_chunks",
                    "context",
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

    def _write_reports(
        self,
        *,
        lang_dir: Path,
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
                title=f"Mixed Language Context Audit [{display_language_tag(self.lang)}]",
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
                suspicious_chunks = ", ".join(str(chunk) for chunk in entry.get("suspicious_chunks", []))
                fields: list[tuple[str, str]] = []
                if entry.get("audit_reason"):
                    fields.append(("REASON", str(entry["audit_reason"])))
                if entry.get("context"):
                    fields.append(("CONTEXT", str(entry["context"])))
                else:
                    fields.append(("CONTEXT", "[MISSING CONTEXT]"))
                if suspicious_chunks:
                    fields.append(("SUSPICIOUS", suspicious_chunks))
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


def run_mixed_language_context_audit(
    *,
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    output_dir: Path | str | None = DEFAULT_TRANSLATION_MIXED_LANGUAGE_AUDIT_DIRECTORY,
    lang: str,
    quiet: bool = False,
    emit_text: bool = True,
) -> MixedLanguageContextAuditResult:
    if not lang.strip():
        raise ValueError("mixed-language-context-audit requires --lang.")
    generator = MixedLanguageContextAuditGenerator(
        res_dir=res_dir,
        output_dir=output_dir,
        lang=lang,
    )
    return generator.run(quiet=quiet, emit_text=emit_text)
