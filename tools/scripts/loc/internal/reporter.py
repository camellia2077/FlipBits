from pathlib import Path

from .config import LanguageConfig


class LocConsoleReporter:
    def __init__(self, config: LanguageConfig):
        self.config = config

    def print_line_scan_header(self, mode: str, threshold: int) -> None:
        mode_text = "大文件" if mode == "over" else "小文件"
        over_comparator = ">=" if self.config.over_inclusive else ">"
        comparator = over_comparator if mode == "over" else "<"
        print(f"{'=' * 100}")
        print(
            f"{self.config.display_name} 代码行数扫描报告 "
            f"({mode_text}模式: {comparator} {threshold} 行)"
        )
        print(f"{'=' * 100}\n")

    def print_line_path_result(
        self,
        path: Path,
        mode: str,
        threshold: int,
        matched: list[tuple[str, int]],
    ) -> None:
        project_name = path.name if path.name else str(path)
        print(f"[SCAN] 正在扫描项目: [{project_name}]")
        print(f"  路径: {path}")
        if matched:
            print(f"  找到 {len(matched)} 个匹配文件：")
            for file_path, lines in matched:
                print(f'  {lines:<6} lines | File "{file_path}"')
        else:
            if mode == "over":
                print(f"  [OK] 扫描完毕，未发现超过阈值 {threshold} 的文件。")
            else:
                print(f"  [OK] 扫描完毕，未发现低于阈值 {threshold} 的文件。")
        print(f"\n{'-' * 100}\n")

    def print_dir_scan_header(self, threshold: int, max_depth: int | None) -> None:
        print(f"{'=' * 100}")
        print(f"{self.config.display_name} 目录文件数扫描报告 (目录内代码文件 > {threshold})")
        if max_depth is None:
            print("扫描层级: 不限制")
        else:
            print(f"扫描层级: <= {max_depth}")
        print(f"{'=' * 100}\n")

    def print_dir_path_result(
        self,
        path: Path,
        threshold: int,
        matched: list[tuple[str, int]],
    ) -> None:
        project_name = path.name if path.name else str(path)
        print(f"[SCAN] 正在扫描项目: [{project_name}]")
        print(f"  路径: {path}")
        if matched:
            print(f"  找到 {len(matched)} 个超阈值目录：")
            for dir_path, file_count in matched:
                print(f'  {file_count:<6} files | Dir "{dir_path}"')
        else:
            print(f"  [OK] 扫描完毕，未发现目录内代码文件数超过阈值 {threshold} 的目录。")
        print(f"\n{'-' * 100}\n")

    @staticmethod
    def print_missing_path(path: Path) -> None:
        print(f"[SCAN] 正在扫描项目: [{path.name}]")
        print(f"  路径: {path}")
        print("  [ERROR] 路径不存在，跳过扫描。")
        print(f"\n{'-' * 100}\n")
