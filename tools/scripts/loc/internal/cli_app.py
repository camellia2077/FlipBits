import argparse
import shutil
from dataclasses import replace
from pathlib import Path

from .config import LANG_CHOICES, load_language_config, load_scan_lines_config
from .report_builder import ReportBuilder
from .report_diff import build_scan_diff
from .report_formatter import ReportFormatter, ScopeReportFormatter
from .report_models import ScanReport, ScopeReport
from .reporter import LocConsoleReporter, ScopeConsoleReporter
from .report_writers import JsonReportWriter, MarkdownReportWriter
from .scope_builder import ScopeReportBuilder
from .service import UNDER_SENTINEL, LocScanService, ScanArgumentResolver


SUPPORTED_RESPONSIBILITY_LANGS = frozenset({"cpp", "kt", "py", "rs"})


class LocCliApplication:
    DIR_OVER_SENTINEL = -1

    def run(self) -> int:
        args = self.parse_args()
        repo_root = self._repo_root()
        tool_root = self._tool_root()
        if args.scope:
            log_path = self._resolve_scope_log_path(args.log_file, args.scope, tool_root=tool_root)
        else:
            log_path = self._resolve_log_path(args.log_file, args.lang, tool_root=tool_root)
        log_path.parent.mkdir(parents=True, exist_ok=True)
        if args.scope:
            self._clear_scope_detail_logs(scope=args.scope, logs_root=log_path.parent)
        else:
            self._clear_language_detail_logs(lang=args.lang, logs_root=log_path.parent)

        exit_code, report, artifacts, formatter = self._run_scan(args, repo_root)
        if args.baseline:
            try:
                baseline_path = self._resolve_baseline_path(args.baseline, repo_root=repo_root)
                report = replace(
                    report,
                    baseline=str(baseline_path),
                    diff=build_scan_diff(baseline_path=baseline_path, report=report),
                )
            except (OSError, ValueError) as error:
                return self._print_baseline_error(report, formatter, error)

        if args.scope:
            detail_md_by_path = self._write_scope_reports(
                log_path=log_path,
                report=report,
                artifacts=artifacts,
                formatter=formatter,
            )
            self._print_scope_report(report, formatter, detail_md_by_path)
        else:
            detail_md_by_path = self._write_reports(
                log_path=log_path,
                report=report,
                artifacts=artifacts,
                formatter=formatter,
            )
            self._print_report(report, formatter, detail_md_by_path)

        print(f"[LOG] 扫描日志: {log_path}")
        return exit_code

    def _run_scan(self, args: argparse.Namespace, repo_root: Path):
        if args.scope:
            return self._run_scope_scan(args, repo_root)
        return self._run_language_scan(args, repo_root)

    def _run_language_scan(self, args: argparse.Namespace, repo_root: Path):
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

        builder = ReportBuilder(lang=args.lang, scan_service=LocScanService(config))
        resolver = ScanArgumentResolver()
        paths = resolver.resolve_paths(args.paths, config.default_paths, repo_root=repo_root)

        if args.responsibility_risk is not False:
            threshold = config.default_responsibility_risk_threshold if args.responsibility_risk is True else int(args.responsibility_risk)
            if threshold <= 0:
                return self._error_report(args.lang, generated_at, "--responsibility-risk 阈值必须是正整数。", formatter)
            if args.lang not in SUPPORTED_RESPONSIBILITY_LANGS:
                return self._error_report(args.lang, generated_at, f"--responsibility-risk 当前仅支持 --lang kt / --lang py / --lang cpp / --lang rs，收到 {args.lang}。", formatter)
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

    def _run_scope_scan(self, args: argparse.Namespace, repo_root: Path):
        generated_at = self._now()
        if args.dir_max_depth is not None and args.dir_over_files is None:
            return self._error_scope_report(args.scope, generated_at, "--dir-max-depth 只能与 --dir-over-files 一起使用。", args)
        if args.responsibility_risk and (
            args.over is not None
            or args.under is not None
            or args.threshold is not None
            or args.dir_over_files is not None
        ):
            return self._error_scope_report(args.scope, generated_at, "--responsibility-risk 不能与 over/under/threshold/dir-over-files 混用。", args)

        try:
            config = load_scan_lines_config(Path(args.config).resolve())
            scope_config = config.scopes.get(args.scope)
            if scope_config is None:
                raise ValueError(f"配置缺失 scope 节点: [scopes.{args.scope}]")
            language_configs = tuple(config.languages[lang] for lang in scope_config.languages)
        except (FileNotFoundError, KeyError, ValueError, OSError) as error:
            return self._error_scope_report(args.scope, generated_at, f"配置加载失败: {error}", args)

        formatter = ScopeReportFormatter(
            display_name=scope_config.display_name,
            over_inclusive_by_lang={item.lang: item.over_inclusive for item in language_configs},
            display_name_by_lang={item.lang: item.display_name for item in language_configs},
        )
        builder = ScopeReportBuilder(config=scope_config, language_configs=language_configs)
        resolver = ScanArgumentResolver()
        paths = resolver.resolve_paths(args.paths, scope_config.default_paths, repo_root=repo_root)

        if args.responsibility_risk is not False:
            unsupported = [
                item.lang for item in language_configs if item.lang not in SUPPORTED_RESPONSIBILITY_LANGS
            ]
            if unsupported:
                return self._error_scope_report(
                    args.scope,
                    generated_at,
                    "--responsibility-risk 当前不支持 scope 中的语言: " + ", ".join(unsupported),
                    args,
                )
            thresholds = {
                item.lang: item.default_responsibility_risk_threshold
                if args.responsibility_risk is True
                else int(args.responsibility_risk)
                for item in language_configs
            }
            if any(value <= 0 for value in thresholds.values()):
                return self._error_scope_report(args.scope, generated_at, "--responsibility-risk 阈值必须是正整数。", args)
            build_result = builder.build_responsibility_scan(
                generated_at=generated_at,
                paths=paths,
                thresholds=thresholds,
            )
            return 0, build_result.report, build_result.artifacts, formatter

        if args.dir_over_files is not None:
            thresholds = {
                item.lang: item.default_dir_over_files
                if args.dir_over_files == self.DIR_OVER_SENTINEL
                else int(args.dir_over_files)
                for item in language_configs
            }
            if any(value <= 0 for value in thresholds.values()):
                return self._error_scope_report(args.scope, generated_at, "--dir-over-files 阈值必须是正整数。", args)
            if args.dir_max_depth is not None and args.dir_max_depth < 0:
                return self._error_scope_report(args.scope, generated_at, "--dir-max-depth 必须是 >= 0 的整数。", args)
            build_result = builder.build_dir_scan(
                generated_at=generated_at,
                paths=paths,
                thresholds=thresholds,
                max_depth=args.dir_max_depth,
            )
            return 0, build_result.report, build_result.artifacts, formatter

        mode = "under" if args.under is not None else "over"
        if args.under is not None and args.under != UNDER_SENTINEL:
            thresholds = {item.lang: args.under for item in language_configs}
        elif args.over is not None or args.threshold is not None:
            explicit = args.over if args.over is not None else args.threshold
            thresholds = {item.lang: explicit for item in language_configs}
        else:
            thresholds = {
                item.lang: item.default_under_threshold if mode == "under" else item.default_over_threshold
                for item in language_configs
            }
        if any(value <= 0 for value in thresholds.values()):
            return self._error_scope_report(args.scope, generated_at, "阈值必须是正整数。", args)
        build_result = builder.build_line_scan(
            generated_at=generated_at,
            paths=paths,
            mode=mode,
            thresholds=thresholds,
        )
        return 0, build_result.report, build_result.artifacts, formatter

    def _write_reports(self, *, log_path: Path, report: ScanReport, artifacts, formatter: ReportFormatter) -> dict[str, str]:
        markdown_writer = MarkdownReportWriter(formatter)
        JsonReportWriter.write_scan_report(log_path, report)
        markdown_writer.write_scan_report(log_path.with_suffix(".md"), report)
        logs_root = log_path.parent
        self._clear_artifact_detail_logs(logs_root=logs_root, artifacts=artifacts)
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

    def _write_scope_reports(self, *, log_path: Path, report: ScopeReport, artifacts, formatter: ScopeReportFormatter) -> dict[str, str]:
        base_formatter = formatter.formatter_for(report.parts[0].part) if report.parts else ReportFormatter(display_name=report.display_name, over_inclusive=False, lang="cpp")
        markdown_writer = MarkdownReportWriter(base_formatter)
        JsonReportWriter.write_scope_report(log_path, report)
        markdown_writer.write_scope_report(log_path.with_suffix(".md"), report, formatter)
        logs_root = log_path.parent
        self._clear_artifact_detail_logs(logs_root=logs_root, artifacts=artifacts)
        detail_md_by_path: dict[str, str] = {}
        for artifact in artifacts:
            json_path = logs_root / artifact.relative_output_path
            md_path = json_path.with_suffix(".md")
            json_path.parent.mkdir(parents=True, exist_ok=True)
            JsonReportWriter.write_detail_report(json_path, artifact.report)
            MarkdownReportWriter(formatter.formatter_for(artifact.report.lang)).write_detail_report(md_path, artifact.report)
            source_path = getattr(artifact.report.result, "path", None)
            if source_path:
                detail_md_by_path[str(source_path)] = str(md_path.resolve())
        return detail_md_by_path

    def _print_report(self, report: ScanReport, formatter: ReportFormatter, detail_md_by_path: dict[str, str]) -> None:
        LocConsoleReporter(formatter, detail_md_by_path=detail_md_by_path).print_scan_report(report)

    def _print_scope_report(self, report: ScopeReport, formatter: ScopeReportFormatter, detail_md_by_path: dict[str, str]) -> None:
        ScopeConsoleReporter(formatter, detail_md_by_path=detail_md_by_path).print_scan_report(report)

    @staticmethod
    def _clear_language_detail_logs(*, lang: str, logs_root: Path) -> None:
        detail_dir = logs_root / ("kotlin" if lang == "kt" else lang)
        if detail_dir.exists():
            shutil.rmtree(detail_dir)

    @staticmethod
    def _clear_scope_detail_logs(*, scope: str, logs_root: Path) -> None:
        del scope
        for part_dir in (logs_root / "cpp", logs_root / "kt", logs_root / "kotlin", logs_root / "py", logs_root / "rs", logs_root / "js"):
            if part_dir.exists():
                shutil.rmtree(part_dir)

    @staticmethod
    def _clear_artifact_detail_logs(*, logs_root: Path, artifacts) -> None:
        root_names = {
            artifact.relative_output_path.parts[0]
            for artifact in artifacts
            if artifact.relative_output_path.parts
        }
        for root_name in root_names:
            detail_dir = logs_root / root_name
            if detail_dir.exists():
                shutil.rmtree(detail_dir)

    @staticmethod
    def parse_args() -> argparse.Namespace:
        parser = argparse.ArgumentParser(description="统一代码行数扫描工具（C++/Kotlin/Python/Rust/JavaScript）。")
        selector = parser.add_mutually_exclusive_group(required=True)
        selector.add_argument("--lang", choices=LANG_CHOICES, help="按语言扫描: cpp | kt | py | rs | js | md。")
        selector.add_argument("--scope", help="按架构 scope 扫描，例如 audio_api、audio_core、audio_android。")
        parser.add_argument("paths", nargs="*", help="待扫描目录（可传多个，支持相对/绝对路径）。未传时使用配置默认路径。")
        parser.add_argument(
            "--config",
            default=str(Path(__file__).resolve().parents[1] / "scan_lines.toml"),
            help="TOML 配置文件路径。",
        )
        parser.add_argument(
            "--log-file",
            default=None,
            help="扫描日志输出路径（支持相对/绝对）。未传时按语言或 scope 写入默认日志。",
        )
        parser.add_argument(
            "--baseline",
            default=None,
            help="基线扫描 JSON 路径；提供后会在当前报告中输出 added/removed/changed/unchanged diff。",
        )

        group = parser.add_mutually_exclusive_group()
        group.add_argument("--over", type=int, metavar="N", help="扫描大文件（over 模式）。")
        group.add_argument("--under", type=int, nargs="?", const=UNDER_SENTINEL, metavar="N", help="扫描小文件（under 模式）。不传 N 时使用配置默认阈值。")
        group.add_argument("--dir-over-files", type=int, nargs="?", const=LocCliApplication.DIR_OVER_SENTINEL, metavar="N", help="扫描目录中代码文件数超过 N 的目录。不传 N 时使用配置默认值。")
        parser.add_argument("-t", "--threshold", type=int, help="兼容旧参数，等价于 --over N。")
        parser.add_argument("--dir-max-depth", type=int, default=None, help="目录扫描最大深度（相对输入根目录；0 仅根目录）。仅与 --dir-over-files 配合使用。")
        parser.add_argument("--responsibility-risk", type=int, nargs="?", const=True, default=False, metavar="N", help="扫描职责混杂风险；按语言使用现有 analyzer。当前支持 cpp / kt / py。")
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
    def _resolve_scope_log_path(log_file: str | None, scope: str, tool_root: Path) -> Path:
        if log_file:
            path = Path(log_file)
            return ((tool_root / path).resolve() if not path.is_absolute() else path.resolve())
        return (tool_root / "logs" / "scopes" / scope / "scan.json").resolve()

    @staticmethod
    def _resolve_baseline_path(baseline: str, *, repo_root: Path) -> Path:
        path = Path(baseline)
        return (repo_root / path).resolve() if not path.is_absolute() else path.resolve()

    @staticmethod
    def _print_baseline_error(report, formatter, error: Exception) -> int:
        print(f"[ERROR] 基线读取失败: {error}")
        return 2

    @staticmethod
    def _now() -> str:
        from datetime import datetime

        return datetime.now().astimezone().isoformat(timespec="seconds")

    @staticmethod
    def _error_report(lang: str, generated_at: str, message: str, formatter: ReportFormatter | None):
        report = ScanReport(generated_at=generated_at, status="error", lang=lang, error=message)
        return 2, report, (), formatter or ReportFormatter(display_name=lang, over_inclusive=False, lang=lang)

    @staticmethod
    def _error_scope_report(scope: str, generated_at: str, message: str, args: argparse.Namespace):
        display_name = scope
        try:
            config = load_scan_lines_config(Path(args.config).resolve())
            if scope in config.scopes:
                display_name = config.scopes[scope].display_name
        except (FileNotFoundError, OSError, ValueError):
            pass
        report = ScopeReport(
            generated_at=generated_at,
            status="error",
            scope=scope,
            display_name=display_name,
            error=message,
        )
        formatter = ScopeReportFormatter(display_name=display_name, over_inclusive_by_lang={})
        return 2, report, (), formatter
