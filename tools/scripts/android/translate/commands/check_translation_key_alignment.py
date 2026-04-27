from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from core.translation_paths import (
    DEFAULT_RES_DIRECTORY,
    display_language_tag,
    is_pro_sample_key,
    iter_translation_text_xml_paths,
)
from core.translation_reporting import (
    MinimalMarkdownReportWriter,
    OutputDirectoryManager,
    ReportFileBlock,
    ReportKeyBlock,
)
from core.translation_resources import AndroidStringResourceRepository, ResourceFile
from prompts.translation_review_prompts import build_key_alignment_repair_prompt

DEFAULT_OUTPUT_DIRECTORY = Path(__file__).resolve().parents[5] / "temp" / "translation_key_alignment_reports"


@dataclass(frozen=True)
class TranslationKeyAlignmentResult:
    exit_code: int
    output_dir: Path
    alignment_issue_count: int
    report_file_count: int


class TranslationKeyAlignmentChecker:
    def __init__(
        self,
        *,
        res_dir: Path | str = DEFAULT_RES_DIRECTORY,
        output_dir: Path | str = DEFAULT_OUTPUT_DIRECTORY,
    ) -> None:
        self.repository = AndroidStringResourceRepository(res_dir)
        self.output_manager = OutputDirectoryManager(output_dir)
        self.writer = MinimalMarkdownReportWriter()

    def run(
        self,
        *,
        quiet: bool = False,
        emit_text: bool = True,
    ) -> TranslationKeyAlignmentResult:
        self.repository.ensure_base_directory()
        base_files = self.repository.load_base_resource_files()
        self.output_manager.reset()

        total_issues = 0
        written_reports = 0

        for lang_code, folder_path in self.repository.iter_localized_directories():
            file_blocks = self._build_language_issue_blocks(lang_code, folder_path, base_files)
            if not file_blocks:
                continue

            output_file = self.output_manager.output_dir / lang_code / f"{lang_code}_translation_tasks.md"
            self.writer.write(
                output_file,
                title=f"{display_language_tag(lang_code)} Translation Tasks",
                section=f"{display_language_tag(lang_code)} vs EN",
                prompt=build_key_alignment_repair_prompt(display_language_tag(lang_code)),
                metadata_lines=(
                    f"TOTAL_ISSUES: {sum(len(block.key_blocks) for block in file_blocks)}",
                    f"DIR: values-{lang_code}",
                    "NOTE: Pro sample keys that contain '_ascii_' are intentionally excluded. They are fixed ASCII protocol samples and are not translation tasks.",
                ),
                file_blocks=tuple(file_blocks),
            )
            total_issues += sum(len(block.key_blocks) for block in file_blocks)
            written_reports += 1

        if total_issues == 0:
            output_file = self.output_manager.output_dir / "translation_tasks_ok.md"
            output_file.write_text(
                "# Translation Tasks OK\n\nOK: No missing or extra localized translation keys found.\n",
                encoding="utf-8",
            )
            written_reports = 1

        if emit_text and not quiet:
            print(f"Done. Alignment issues: {total_issues}")
            print(f"Reports generated under: {self.output_manager.output_dir} ({written_reports} files)")
        return TranslationKeyAlignmentResult(
            exit_code=0 if total_issues == 0 else 2,
            output_dir=self.output_manager.output_dir,
            alignment_issue_count=total_issues,
            report_file_count=written_reports,
        )

    def _build_language_issue_blocks(
        self,
        lang_code: str,
        folder_path: Path,
        base_files: dict[str, ResourceFile],
    ) -> list[ReportFileBlock]:
        del lang_code
        localized_xml_paths = iter_translation_text_xml_paths(folder_path)
        localized_by_name = {path.name: path for path in localized_xml_paths}

        file_blocks: list[ReportFileBlock] = []

        extra_filenames = sorted(name for name in localized_by_name if name not in base_files)
        for filename in extra_filenames:
            file_blocks.append(
                ReportFileBlock(
                    filename=filename,
                    key_blocks=(
                        ReportKeyBlock(
                            key="__extra_file__",
                            fields=(("ISSUE", "localized file has no English base counterpart"),),
                        ),
                    ),
                )
            )

        for filename, resource_file in sorted(base_files.items()):
            # Pro sample entries are intentionally ASCII-only across every locale in the app.
            # They are protocol-style fixed inputs rather than target-language translation work,
            # so key-alignment should ignore them on both sides:
            # - if a locale omits them, that is not a missing translation
            # - if a locale keeps the same ASCII-only keys, that is not an extra localized key
            # Only non-Pro translation keys participate in the English-subset contract.
            #
            # High Gothic (`values-la`) is also handled here as a localized output
            # rather than an English baseline. Its prose intentionally blends Latin
            # and English for style, but structurally it still has to line up with
            # the real English `values/` resource set like every other locale.
            base_keys = {
                key
                for key in resource_file.strings.keys()
                if not is_pro_sample_key(key)
            }
            if filename not in localized_by_name:
                if not base_keys:
                    continue
                example_keys = sorted(base_keys)[:5]
                file_blocks.append(
                    ReportFileBlock(
                        filename=filename,
                        key_blocks=(
                            ReportKeyBlock(
                                key="__missing_file__",
                                fields=(
                                    ("ISSUE", "localized file is missing for English base counterpart"),
                                    ("MISSING_KEY_COUNT", str(len(base_keys))),
                                    ("EXAMPLE_KEYS", ", ".join(example_keys)),
                                ),
                            ),
                        ),
                    )
                )
                continue

            localized_strings = self.repository.load_localized_strings(folder_path, filename)
            localized_keys = {
                key
                for key in localized_strings.keys()
                if not is_pro_sample_key(key)
            }

            missing_keys = sorted(base_keys - localized_keys)
            extra_keys = sorted(localized_keys - base_keys)
            if not missing_keys and not extra_keys:
                continue

            key_blocks: list[ReportKeyBlock] = []
            for key in missing_keys:
                key_blocks.append(
                    ReportKeyBlock(
                        key=key,
                        fields=tuple(
                            field
                            for field in (
                                ("ISSUE", "missing localized translation for English base key"),
                                (
                                    "CONTEXT",
                                    resource_file.contexts.get(key),
                                ),
                                ("EN", resource_file.strings[key]),
                            )
                            if field[1]
                        ),
                    )
                )
            for key in extra_keys:
                key_blocks.append(
                    ReportKeyBlock(
                        key=key,
                        fields=(
                            ("ISSUE", "localized key exists but English base key is missing"),
                            ("TR", localized_strings[key]),
                        ),
                    )
                )
            file_blocks.append(ReportFileBlock(filename=filename, key_blocks=tuple(key_blocks)))

        return file_blocks


def run_translation_key_alignment_check(
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    output_dir: Path | str = DEFAULT_OUTPUT_DIRECTORY,
    quiet: bool = False,
    emit_text: bool = True,
) -> TranslationKeyAlignmentResult:
    checker = TranslationKeyAlignmentChecker(res_dir=res_dir, output_dir=output_dir)
    return checker.run(quiet=quiet, emit_text=emit_text)
