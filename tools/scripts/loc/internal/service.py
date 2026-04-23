import argparse
import os
from pathlib import Path

from .config import LanguageConfig
from .responsibility_analyzers import (
    ResponsibilityRiskResult,
    create_responsibility_risk_analyzer,
)

UNDER_SENTINEL = -1


class ScanArgumentResolver:
    @staticmethod
    def resolve_mode_and_threshold(
        args: argparse.Namespace,
        config: LanguageConfig,
    ) -> tuple[str, int]:
        if args.under is not None:
            if args.under == UNDER_SENTINEL:
                return "under", config.default_under_threshold
            return "under", args.under
        if args.over is not None:
            return "over", args.over
        if args.threshold is not None:
            return "over", args.threshold
        return "over", config.default_over_threshold

    @staticmethod
    def resolve_paths(
        raw_paths: list[str],
        default_paths: list[str],
        repo_root: Path,
    ) -> list[Path]:
        source = raw_paths if raw_paths else default_paths
        resolved: list[Path] = []
        for item in source:
            path = Path(item)
            if not path.is_absolute():
                path = (repo_root / path).resolve()
            else:
                path = path.resolve()
            resolved.append(path)
        return resolved


class LocScanService:
    def __init__(self, config: LanguageConfig):
        self.config = config
        self._ignore_dirs_lower = {name.lower() for name in config.ignore_dirs}
        self._responsibility_risk_analyzer = create_responsibility_risk_analyzer(config)

    def analyze_path(
        self,
        target_dir: Path,
        mode: str,
        threshold: int,
    ) -> list[tuple[str, int]]:
        result_files: list[tuple[str, int]] = []

        for root, dirs, files in os.walk(target_dir):
            dirs[:] = [name for name in dirs if not self._should_skip_dir(name)]

            for file_name in files:
                ext = Path(file_name).suffix.lower()
                if ext not in self.config.extensions:
                    continue

                file_path = Path(root) / file_name
                try:
                    with file_path.open(encoding="utf-8", errors="ignore") as handle:
                        line_count = sum(1 for _ in handle)
                except Exception as error:
                    print(f"读取文件时发生意外错误 {file_path}: {error}")
                    continue

                if self._matches_threshold(line_count, mode, threshold):
                    result_files.append((str(file_path), line_count))

        result_files.sort(key=lambda item: item[1], reverse=(mode == "over"))
        return result_files

    def classify_split_priority(self, line_count: int, threshold: int) -> tuple[str, str]:
        if line_count >= max(threshold * 2, threshold + 300):
            return "P0", "严重超阈值，建议优先按页面/状态/组件职责拆分"
        if line_count >= max(int(threshold * 1.6), threshold + 180):
            return "P1", "明显超阈值，建议近期按职责边界拆分"
        if line_count >= max(int(threshold * 1.3), threshold + 100):
            return "P2", "中度超阈值，可结合后续需求一起拆分"
        return "P3", "轻度超阈值，先观察再拆分"

    def analyze_directory_file_counts(
        self,
        target_dir: Path,
        threshold: int,
        max_depth: int | None = None,
    ) -> list[tuple[str, int]]:
        result_dirs: list[tuple[str, int]] = []
        root_depth = len(target_dir.parts)

        for root, dirs, files in os.walk(target_dir):
            current_path = Path(root)
            current_depth = len(current_path.parts) - root_depth
            dirs[:] = [name for name in dirs if not self._should_skip_dir(name)]
            if max_depth is not None and current_depth >= max_depth:
                dirs[:] = []

            file_count = 0
            for file_name in files:
                ext = Path(file_name).suffix.lower()
                if ext in self.config.extensions:
                    file_count += 1

            if file_count > threshold:
                result_dirs.append((str(current_path), file_count))

        result_dirs.sort(key=lambda item: item[1], reverse=True)
        return result_dirs

    def analyze_responsibility_risk(
        self,
        target_dir: Path,
        threshold: int,
    ) -> list[ResponsibilityRiskResult]:
        if self._responsibility_risk_analyzer is None:
            raise ValueError(f"职责混杂风险扫描当前不支持语言: {self.config.lang}")

        results: list[ResponsibilityRiskResult] = []
        for root, dirs, files in os.walk(target_dir):
            dirs[:] = [name for name in dirs if not self._should_skip_dir(name)]
            for file_name in files:
                ext = Path(file_name).suffix.lower()
                if ext not in self.config.extensions:
                    continue

                file_path = Path(root) / file_name
                try:
                    text = file_path.read_text(encoding="utf-8", errors="ignore")
                except Exception as error:
                    print(f"读取文件时发生意外错误 {file_path}: {error}")
                    continue

                result = self._responsibility_risk_analyzer.analyze_file(file_path, text)
                if result.score >= threshold:
                    results.append(result)

        results.sort(key=lambda item: (item.score, item.lines), reverse=True)
        return results

    def _should_skip_dir(self, dir_name: str) -> bool:
        dir_lower = dir_name.lower()
        if dir_lower in self._ignore_dirs_lower:
            return True
        return any(dir_lower.startswith(prefix) for prefix in self.config.ignore_prefixes)

    def _matches_threshold(self, line_count: int, mode: str, threshold: int) -> bool:
        if mode == "over":
            if self.config.over_inclusive:
                return line_count >= threshold
            return line_count > threshold
        return line_count < threshold
