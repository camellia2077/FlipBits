import argparse
import shutil
from pathlib import Path

from .config import LANG_CHOICES, load_language_config
from .report_builder import ReportBuilder
from .report_formatter import ReportFormatter
from .report_models import ScanReport
from .reporter import LocConsoleReporter
from .report_writers import JsonReportWriter, MarkdownReportWriter
from .service import UNDER_SENTINEL, LocScanService, ScanArgumentResolver


class LocCliApplication:
    DIR_OVER_SENTINEL = -1

    def run(self) -> int:
        args = self.parse_args()
        repo_root = self._repo_root()
        tool_root = self._tool_root()
        log_path = self._resolve_log_path(args.log_file, args.lang, tool_root=tool_root)
        log_path.parent.mkdir(parents=True, exist_ok=True)
        self._clear_language_detail_logs(lang=args.lang, logs_root=log_path.parent)

        exit_code, report, artifacts, formatter = self._run_scan(args, repo_root)
        detail_md_by_path = self._write_reports(log_path=log_path, report=report, artifacts=artifacts, formatter=formatter)
        self._copy_language_prompt(lang=args.lang, tool_root=tool_root, logs_root=log_path.parent)
        self._print_report(report, formatter, detail_md_by_path)

        print(f"[LOG] 扫描日志: {log_path}")
        return exit_code

    def _run_scan(self, args: argparse.Namespace, repo_root: Path):
        generated_at = self._now()
        if args.dir_max_depth is not None and args.dir_over_files is None:
            return self._error_report(args.lang, generated_at, "--dir-max-depth 只能与 --dir-over-files 一起使用。", None)
        if args.responsibility_risk and (
            args.over is not None
            or args.under is not None
            or args.threshold is not None
            or args.dir_over_files is not None
        ):
            return self._error_report(args.lang, generated_at, "--responsibility-risk 不能与 over/under/threshold/dir-over-files 混用。", None)

        try:
            config = load_language_config(config_path=Path(args.config).resolve(), lang=args.lang)
        except (FileNotFoundError, ValueError, OSError) as error:
            return self._error_report(args.lang, generated_at, f"配置加载失败: {error}", None)
        formatter = ReportFormatter(
            display_name=config.display_name,
            over_inclusive=config.over_inclusive,
            lang=args.lang,
        )

        scan_service = LocScanService(config)
        builder = ReportBuilder(lang=args.lang, scan_service=scan_service)
        resolver = ScanArgumentResolver()
        paths = resolver.resolve_paths(args.paths, config.default_paths, repo_root=repo_root)

        if args.responsibility_risk is not False:
            threshold = config.default_responsibility_risk_threshold if args.responsibility_risk is True else int(args.responsibility_risk)
            if threshold <= 0:
                return self._error_report(args.lang, generated_at, "--responsibility-risk 阈值必须是正整数。", formatter)
            if args.lang not in {"kt", "py", "cpp"}:
                return self._error_report(args.lang, generated_at, f"--responsibility-risk 当前仅支持 --lang kt / --lang py / --lang cpp，收到 {args.lang}。", formatter)
            build_result = builder.build_responsibility_scan(generated_at=generated_at, paths=paths, threshold=threshold)
            return 0, build_result.report, build_result.artifacts, formatter

        if args.dir_over_files is not None:
            threshold = config.default_dir_over_files if args.dir_over_files == self.DIR_OVER_SENTINEL else int(args.dir_over_files)
            if threshold <= 0:
                return self._error_report(args.lang, generated_at, "--dir-over-files 阈值必须是正整数。", formatter)
            if args.dir_max_depth is not None and args.dir_max_depth < 0:
                return self._error_report(args.lang, generated_at, "--dir-max-depth 必须是 >= 0 的整数。", formatter)
            build_result = builder.build_dir_scan(
                generated_at=generated_at,
                paths=paths,
                threshold=threshold,
                max_depth=args.dir_max_depth,
            )
            return 0, build_result.report, build_result.artifacts, formatter

        mode, threshold = resolver.resolve_mode_and_threshold(args, config)
        if threshold <= 0:
            return self._error_report(args.lang, generated_at, "阈值必须是正整数。", formatter)
        build_result = builder.build_line_scan(
            generated_at=generated_at,
            paths=paths,
            mode=mode,
            threshold=threshold,
        )
        return 0, build_result.report, build_result.artifacts, formatter

    def _write_reports(self, *, log_path: Path, report: ScanReport, artifacts, formatter: ReportFormatter) -> dict[str, str]:
        markdown_writer = MarkdownReportWriter(formatter)
        JsonReportWriter.write_scan_report(log_path, report)
        markdown_writer.write_scan_report(log_path.with_suffix(".md"), report)
        logs_root = log_path.parent
        detail_md_by_path: dict[str, str] = {}
        for artifact in artifacts:
            json_path = logs_root / artifact.relative_output_path
            md_path = json_path.with_suffix(".md")
            json_path.parent.mkdir(parents=True, exist_ok=True)
            JsonReportWriter.write_detail_report(json_path, artifact.report)
            markdown_writer.write_detail_report(md_path, artifact.report)
            source_path = getattr(artifact.report.result, "path", None)
            if source_path:
                detail_md_by_path[str(source_path)] = str(md_path.resolve())
        return detail_md_by_path

    def _print_report(self, report: ScanReport, formatter: ReportFormatter, detail_md_by_path: dict[str, str]) -> None:
        LocConsoleReporter(formatter, detail_md_by_path=detail_md_by_path).print_scan_report(report)

    @staticmethod
    def _copy_language_prompt(*, lang: str, tool_root: Path, logs_root: Path) -> None:
        prompt_name_by_lang = {
            "kt": "kotlin_refactor_prompt.md",
            "cpp": "cpp_refactor_prompt.md",
            "py": "python_refactor_prompt.md",
        }
        prompt_name = prompt_name_by_lang.get(lang)
        if prompt_name is None:
            return
        source_path = tool_root / "prompts" / prompt_name
        if not source_path.exists():
            return
        target_dir = logs_root / ("kotlin" if lang == "kt" else lang)
        target_dir.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source_path, target_dir / "refactor_prompt.md")

    @staticmethod
    def _clear_language_detail_logs(*, lang: str, logs_root: Path) -> None:
        detail_dir = logs_root / ("kotlin" if lang == "kt" else lang)
        if detail_dir.exists():
            shutil.rmtree(detail_dir)

    @staticmethod
    def parse_args() -> argparse.Namespace:
        parser = argparse.ArgumentParser(description="统一代码行数扫描工具（C++/Kotlin/Python/Rust）。")
        parser.add_argument("--lang", choices=LANG_CHOICES, required=True, help="语言类型: cpp | kt | py | rs。")
        parser.add_argument("paths", nargs="*", help="待扫描目录（可传多个，支持相对/绝对路径）。未传时使用 TOML 默认路径。")
        parser.add_argument(
            "--config",
            default=str(Path(__file__).resolve().parents[1] / "scan_lines.toml"),
            help="TOML 配置文件路径。",
        )
        parser.add_argument(
            "--log-file",
            default=None,
            help="扫描日志输出路径（支持相对/绝对）。未传时默认写入 scripts/devtools/loc/logs/scan_<lang>.json。",
        )

        group = parser.add_mutually_exclusive_group()
        group.add_argument("--over", type=int, metavar="N", help="扫描大文件（over 模式）。")
        group.add_argument("--under", type=int, nargs="?", const=UNDER_SENTINEL, metavar="N", help="扫描小文件（under 模式）。不传 N 时使用 TOML 的 default_under_threshold。")
        group.add_argument("--dir-over-files", type=int, nargs="?", const=LocCliApplication.DIR_OVER_SENTINEL, metavar="N", help="扫描目录中代码文件数超过 N 的目录。不传 N 时使用 TOML 中该语言的 default_dir_over_files。")
        parser.add_argument("-t", "--threshold", type=int, help="兼容旧参数，等价于 --over N。")
        parser.add_argument("--dir-max-depth", type=int, default=None, help="目录扫描最大深度（相对输入根目录；0 仅根目录）。仅与 --dir-over-files 配合使用。")
        parser.add_argument("--responsibility-risk", type=int, nargs="?", const=True, default=False, metavar="N", help="扫描语言文件的职责混杂风险（启发式预警，当前支持 --lang kt / --lang py / --lang cpp）。不传 N 时使用 TOML 中的 default_responsibility_risk_threshold。")
        return parser.parse_args()

    @staticmethod
    def _repo_root() -> Path:
        return Path(__file__).resolve().parents[4]

    @staticmethod
    def _tool_root() -> Path:
        return Path(__file__).resolve().parents[1]

    @staticmethod
    def _resolve_log_path(log_file: str | None, lang: str, tool_root: Path) -> Path:
        if log_file:
            path = Path(log_file)
            return ((tool_root / path).resolve() if not path.is_absolute() else path.resolve())
        return (tool_root / "logs" / f"scan_{lang}.json").resolve()

    @staticmethod
    def _now() -> str:
        from datetime import datetime

        return datetime.now().astimezone().isoformat(timespec="seconds")

    def _error_report(self, lang: str, generated_at: str, message: str, formatter: ReportFormatter | None):
        report = ScanReport(generated_at=generated_at, status="error", lang=lang, error=message)
        return 2, report, (), formatter or ReportFormatter(display_name=lang, over_inclusive=False, lang=lang)
