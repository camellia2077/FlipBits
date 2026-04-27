import argparse
import json
from datetime import datetime
from pathlib import Path

from .config import LANG_CHOICES, LanguageConfig, load_language_config
from .reporter import LocConsoleReporter
from .service import UNDER_SENTINEL, LocScanService, ScanArgumentResolver


class LocCliApplication:
    DIR_OVER_SENTINEL = -1

    def run(self) -> int:
        args = self.parse_args()
        repo_root = self._repo_root()
        tool_root = self._tool_root()
        log_path = self._resolve_log_path(args.log_file, args.lang, tool_root=tool_root)
        log_path.parent.mkdir(parents=True, exist_ok=True)

        exit_code, payload = self._run_scan(args, repo_root)
        self._write_json_log(log_path, payload)

        print(f"[LOG] 扫描日志: {log_path}")
        return exit_code

    def _run_scan(self, args: argparse.Namespace, repo_root: Path) -> tuple[int, dict]:
        config_path = Path(args.config).resolve()
        payload = self._build_base_payload(args=args)

        if args.dir_max_depth is not None and args.dir_over_files is None:
            return self._error(
                "--dir-max-depth 只能与 --dir-over-files 一起使用。",
                payload,
            )
        if args.responsibility_risk and (
            args.over is not None
            or args.under is not None
            or args.threshold is not None
            or args.dir_over_files is not None
        ):
            return self._error(
                "--responsibility-risk 不能与 over/under/threshold/dir-over-files 混用。",
                payload,
            )

        try:
            config = load_language_config(config_path=config_path, lang=args.lang)
        except (FileNotFoundError, ValueError, OSError) as error:
            return self._error(f"配置加载失败: {error}", payload)

        resolver = ScanArgumentResolver()
        scan_service = LocScanService(config)
        reporter = LocConsoleReporter(config)
        paths = resolver.resolve_paths(args.paths, config.default_paths, repo_root=repo_root)

        if args.responsibility_risk is not False:
            threshold = (
                config.default_responsibility_risk_threshold
                if args.responsibility_risk is True
                else int(args.responsibility_risk)
            )
            if threshold <= 0:
                return self._error("--responsibility-risk 阈值必须是正整数。", payload)
            return self._run_responsibility_scan(
                payload=payload,
                paths=paths,
                threshold=threshold,
                config=config,
                scan_service=scan_service,
                reporter=reporter,
            )

        if args.dir_over_files is not None:
            return self._run_dir_file_scan(args, payload, paths, config, scan_service, reporter)

        mode, threshold = resolver.resolve_mode_and_threshold(args, config)
        if threshold <= 0:
            return self._error("阈值必须是正整数。", payload)
        return self._run_line_scan(
            payload=payload,
            paths=paths,
            mode=mode,
            threshold=threshold,
            scan_service=scan_service,
            reporter=reporter,
        )

    def _run_line_scan(
        self,
        *,
        payload: dict,
        paths: list[Path],
        mode: str,
        threshold: int,
        scan_service: LocScanService,
        reporter: LocConsoleReporter,
    ) -> tuple[int, dict]:
        reporter.print_line_scan_header(mode, threshold)
        path_results: list[dict] = []
        total_matched_files = 0

        for path in paths:
            if not path.exists():
                reporter.print_missing_path(path)
                path_results.append({"path": str(path), "matched_files": []})
                continue
            matched = scan_service.analyze_path(path, mode, threshold)
            reporter.print_line_path_result(path, mode, threshold, matched)
            matched_files = [{"path": file_path, "lines": lines} for file_path, lines in matched]
            total_matched_files += len(matched_files)
            path_results.append({"path": str(path), "matched_files": matched_files})

        payload["status"] = "ok"
        payload["scan"] = {"mode": mode, "threshold": threshold}
        payload["results"] = path_results
        payload["summary"] = {"matched_files": total_matched_files}
        return 0, payload

    def _run_dir_file_scan(
        self,
        args: argparse.Namespace,
        payload: dict,
        paths: list[Path],
        config: LanguageConfig,
        scan_service: LocScanService,
        reporter: LocConsoleReporter,
    ) -> tuple[int, dict]:
        if args.dir_over_files == self.DIR_OVER_SENTINEL:
            threshold = config.default_dir_over_files
        else:
            threshold = int(args.dir_over_files)

        if threshold <= 0:
            return self._error("--dir-over-files 阈值必须是正整数。", payload)
        if args.dir_max_depth is not None and args.dir_max_depth < 0:
            return self._error("--dir-max-depth 必须是 >= 0 的整数。", payload)

        reporter.print_dir_scan_header(threshold, args.dir_max_depth)
        path_results: list[dict] = []
        total_matched_dirs = 0

        for path in paths:
            if not path.exists():
                reporter.print_missing_path(path)
                path_results.append({"path": str(path), "matched_dirs": []})
                continue
            matched = scan_service.analyze_directory_file_counts(
                path,
                threshold=threshold,
                max_depth=args.dir_max_depth,
            )
            reporter.print_dir_path_result(path, threshold, matched)
            matched_dirs = [
                {"path": dir_path, "files": file_count} for dir_path, file_count in matched
            ]
            total_matched_dirs += len(matched_dirs)
            path_results.append({"path": str(path), "matched_dirs": matched_dirs})

        payload["status"] = "ok"
        payload["scan"] = {"mode": "dir_over_files", "threshold": threshold}
        if args.dir_max_depth is not None:
            payload["scan"]["max_depth"] = args.dir_max_depth
        payload["results"] = path_results
        payload["summary"] = {"matched_dirs": total_matched_dirs}
        return 0, payload

    def _run_responsibility_scan(
        self,
        *,
        payload: dict,
        paths: list[Path],
        threshold: int,
        config: LanguageConfig,
        scan_service: LocScanService,
        reporter: LocConsoleReporter,
    ) -> tuple[int, dict]:
        reporter.print_responsibility_scan_header(threshold)
        path_results: list[dict] = []
        total_matched_files = 0

        if config.lang not in {"kt", "py", "cpp"}:
            return self._error(
                f"--responsibility-risk 当前仅支持 --lang kt / --lang py / --lang cpp，收到 {config.lang}。",
                payload,
            )

        for path in paths:
            if not path.exists():
                reporter.print_missing_path(path)
                path_results.append({"path": str(path), "matched_files": []})
                continue
            matched = scan_service.analyze_responsibility_risk(path, threshold)
            matched_files = [item.to_dict() for item in matched]
            reporter.print_responsibility_path_result(path, matched_files)
            total_matched_files += len(matched_files)
            path_results.append({"path": str(path), "matched_files": matched_files})

        payload["status"] = "ok"
        payload["scan"] = {"mode": "responsibility_risk", "threshold": threshold}
        payload["results"] = path_results
        payload["summary"] = {"matched_files": total_matched_files}
        return 0, payload

    @staticmethod
    def parse_args() -> argparse.Namespace:
        parser = argparse.ArgumentParser(
            description="统一代码行数扫描工具（C++/Kotlin/Python/Rust）。"
        )
        parser.add_argument(
            "--lang",
            choices=LANG_CHOICES,
            required=True,
            help="语言类型: cpp | kt | py | rs。",
        )
        parser.add_argument(
            "paths",
            nargs="*",
            help="待扫描目录（可传多个，支持相对/绝对路径）。未传时使用 TOML 默认路径。",
        )
        parser.add_argument(
            "--config",
            default=str(Path(__file__).resolve().parents[1] / "scan_lines.toml"),
            help="TOML 配置文件路径。",
        )
        parser.add_argument(
            "--log-file",
            default=None,
            help=(
                "扫描日志输出路径（支持相对/绝对）。"
                "未传时默认写入 scripts/devtools/loc/logs/scan_<lang>.json。"
            ),
        )

        group = parser.add_mutually_exclusive_group()
        group.add_argument("--over", type=int, metavar="N", help="扫描大文件（over 模式）。")
        group.add_argument(
            "--under",
            type=int,
            nargs="?",
            const=UNDER_SENTINEL,
            metavar="N",
            help="扫描小文件（under 模式）。不传 N 时使用 TOML 的 default_under_threshold。",
        )
        group.add_argument(
            "--dir-over-files",
            type=int,
            nargs="?",
            const=LocCliApplication.DIR_OVER_SENTINEL,
            metavar="N",
            help=(
                "扫描目录中代码文件数超过 N 的目录。"
                "不传 N 时使用 TOML 中该语言的 default_dir_over_files。"
            ),
        )
        parser.add_argument("-t", "--threshold", type=int, help="兼容旧参数，等价于 --over N。")
        parser.add_argument(
            "--dir-max-depth",
            type=int,
            default=None,
            help="目录扫描最大深度（相对输入根目录；0 仅根目录）。仅与 --dir-over-files 配合使用。",
        )
        parser.add_argument(
            "--responsibility-risk",
            type=int,
            nargs="?",
            const=True,
            default=False,
            metavar="N",
            help=(
                "扫描语言文件的职责混杂风险（启发式预警，当前支持 --lang kt / --lang py）。"
                "当前也支持 --lang cpp。"
                "不传 N 时使用 TOML 中的 default_responsibility_risk_threshold。"
            ),
        )
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
            if not path.is_absolute():
                path = (tool_root / path).resolve()
            else:
                path = path.resolve()
            return path
        return (tool_root / "logs" / f"scan_{lang}.json").resolve()

    @staticmethod
    def _build_base_payload(*, args: argparse.Namespace) -> dict:
        return {
            "generated_at": datetime.now().astimezone().isoformat(timespec="seconds"),
            "status": "unknown",
            "lang": args.lang,
        }

    @staticmethod
    def _write_json_log(path: Path, payload: dict) -> None:
        path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    @staticmethod
    def _error(message: str, payload: dict) -> tuple[int, dict]:
        print(f"[ERROR] {message}")
        payload["status"] = "error"
        payload["error"] = message
        return 2, payload
