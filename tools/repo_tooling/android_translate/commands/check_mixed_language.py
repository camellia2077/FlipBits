from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from core.mixed_language_detection import (
    check_ascii_range,
    check_cjk_language_for_latin_chunks,
    check_non_cjk_language_for_cjk_chunks,
    describe_detection_strategy,
    is_cjk_language,
    is_pro_ascii_context_key,
    should_skip_mixed_language_detection,
)
from core.translation_paths import (
    DEFAULT_RES_DIRECTORY,
    display_language_tag,
    is_pro_sample_key,
)
from core.translation_reporting import (
    MinimalMarkdownReportWriter,
    OutputDirectoryManager,
    ReportFileBlock,
    ReportKeyBlock,
)
from core.translation_resources import AndroidStringResourceRepository, ResourceFile

DEFAULT_OUTPUT_DIRECTORY = Path(__file__).resolve().parents[5] / "temp" / "mixed_language_reports"


@dataclass(frozen=True)
class MixedLanguageCheckResult:
    exit_code: int
    output_dir: Path
    suspicious_issue_count: int
    report_file_count: int


class MixedLanguageReportGenerator:
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
    ) -> MixedLanguageCheckResult:
        self.repository.ensure_base_directory()
        base_files = self.repository.load_base_resource_files()
        self.output_manager.reset()

        total_issues = 0
        written_reports = 0

        for lang_code, folder_path in self.repository.iter_localized_directories():
            category_results = self._build_language_results(lang_code, folder_path, base_files)
            for text_type, result in category_results.items():
                if result["issue_count"] <= 0:
                    continue

                output_file = self.output_manager.output_dir / lang_code / text_type / "mixed_language_report.md"
                self.writer.write(
                    output_file,
                    title=f"Mixed Language Report (Split Strings) [{text_type.upper()}][{display_language_tag(lang_code)}]",
                    section=f"{display_language_tag(lang_code)} | {result['strategy_name']}",
                    metadata_lines=(f"TOTAL_ISSUES: {result['issue_count']}",),
                    file_blocks=tuple(result["file_blocks"]),
                )
                total_issues += result["issue_count"]
                written_reports += 1

        if total_issues == 0:
            output_file = self.output_manager.output_dir / "mixed_language_report.md"
            output_file.write_text(
                "# Mixed Language Report\n\nOK: No missing translation or mixed-language issues found by current rules.\n",
                encoding="utf-8",
            )
            written_reports = 1

        if emit_text and not quiet:
            print(f"Done. Suspicious issues: {total_issues}")
            print(f"Reports generated under: {self.output_manager.output_dir} ({written_reports} files)")

        return MixedLanguageCheckResult(
            exit_code=0,
            output_dir=self.output_manager.output_dir,
            suspicious_issue_count=total_issues,
            report_file_count=written_reports,
        )

    def _build_language_results(
        self,
        lang_code: str,
        folder_path: Path,
        base_files: dict[str, ResourceFile],
    ) -> dict[str, dict[str, object]]:
        is_cjk_lang, lang_strategy = describe_detection_strategy(lang_code)
        category_results: dict[str, dict[str, object]] = {
            "app_text": {"strategy_name": lang_strategy, "issue_count": 0, "file_blocks": []},
            "sample_text": {"strategy_name": lang_strategy, "issue_count": 0, "file_blocks": []},
        }

        xml_names = self.repository.localized_xml_names(folder_path, base_files)
        for filename in xml_names:
            resource_file = base_files[filename]
            trans_dict = self.repository.load_localized_strings(folder_path, filename)
            key_blocks = self._build_issue_key_blocks(
                resource_file,
                lang_code=lang_code,
                trans_dict=trans_dict,
                is_cjk_language=is_cjk_lang,
            )
            if not key_blocks:
                continue

            category_results[resource_file.text_type]["issue_count"] += len(key_blocks)
            category_results[resource_file.text_type]["file_blocks"].append(
                ReportFileBlock(filename=filename, key_blocks=tuple(key_blocks))
            )

        return category_results

    def _build_issue_key_blocks(
        self,
        resource_file: ResourceFile,
        *,
        lang_code: str,
        trans_dict: dict[str, str],
        is_cjk_language: bool,
    ) -> list[ReportKeyBlock]:
        key_blocks: list[ReportKeyBlock] = []
        skip_mixed_language = should_skip_mixed_language_detection(lang_code)
        for key, en_text in resource_file.strings.items():
            if key not in trans_dict:
                continue

            trans_text = trans_dict[key]

            # ------ 核心检测逻辑 ------
            if is_pro_sample_key(key):
                # Pro 的示例文本是协议/编码用 ASCII 输入，不是需要翻译的目标语言文案。
                # 所以在这里提前短路，跳过更昂贵的混杂语言检查，只验证 ASCII 范围。
                suspicious = check_ascii_range(trans_text)
                issue_label = "非 ASCII 字符"
            elif is_pro_ascii_context_key(key):
                # Pro UI 描述的是 ASCII/byte/token 编码细节；这些英文协议词跨语言保留是预期行为。
                # 直接跳过混杂语言检查，避免把合法的 ASCII 术语误报为漏翻。
                continue
            elif skip_mixed_language:
                # values-la 当前是拉丁风格英文，不适合作为普通本地化翻译去检查英文残留。
                # 这类目录仍纳入遍历和 Pro ASCII 校验，但跳过常规 mixed-language 误报。
                continue
            elif is_cjk_language:
                # 中日韩资源只检查明显的拉丁脚本残留，不再尝试做更宽的多语种猜测。
                suspicious = check_cjk_language_for_latin_chunks(trans_text)
                issue_label = "可疑漏翻/混杂"
            else:
                # 非中日韩资源只检查是否混入了明显的 CJK 片段，避免欧美语种之间互查造成误报。
                suspicious = check_non_cjk_language_for_cjk_chunks(trans_text)
                issue_label = "可疑漏翻/混杂"

            if not suspicious:
                continue

            key_blocks.append(
                ReportKeyBlock(
                    key=key,
                    fields=self._build_issue_fields(
                        resource_file,
                        key,
                        issue_label,
                        suspicious,
                        trans_text,
                        en_text,
                    ),
                )
            )

        return key_blocks

    def _build_issue_fields(
        self,
        resource_file: ResourceFile,
        key: str,
        issue_label: str,
        suspicious: list[str],
        translated_text: str,
        english_text: str,
    ) -> tuple[tuple[str, str], ...]:
        fields: list[tuple[str, str]] = []
        sample_length = resource_file.sample_lengths.get(key)
        if sample_length is not None:
            fields.append(("SAMPLE_LENGTH", sample_length))
        fields.extend(
            (
                ("ISSUE", f"{issue_label}: {', '.join(suspicious)}"),
                ("TR", translated_text),
                ("EN", english_text),
            )
        )
        return tuple(fields)


def run_mixed_language_check(
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    output_dir: Path | str = DEFAULT_OUTPUT_DIRECTORY,
    quiet: bool = False,
    emit_text: bool = True,
) -> MixedLanguageCheckResult:
    generator = MixedLanguageReportGenerator(res_dir=res_dir, output_dir=output_dir)
    return generator.run(quiet=quiet, emit_text=emit_text)
