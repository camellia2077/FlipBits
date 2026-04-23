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

    def print_responsibility_scan_header(self, threshold: int) -> None:
        print(f"{'=' * 100}")
        print(f"{self.config.display_name} 职责混杂风险扫描报告 (风险分 >= {threshold})")
        print("说明: 这是文件级启发式风险预警，不代表最终设计结论。")
        print(f"{'=' * 100}\n")

    def print_responsibility_path_result(
        self,
        path: Path,
        matched: list[dict],
    ) -> None:
        project_name = path.name if path.name else str(path)
        print(f"[SCAN] 正在扫描项目: [{project_name}]")
        print(f"  路径: {path}")
        if matched:
            print(f"  找到 {len(matched)} 个高风险文件：")
            grouped_items = self._group_responsibility_items_by_priority(matched)
            for priority in ("P0", "P1", "P2", "P3"):
                items = grouped_items.get(priority)
                if not items:
                    continue
                print(f"  [{priority}]")
                for item in items:
                    role_display = ",".join(item["role_kinds"]) if item["role_kinds"] else "-"
                    top_level_label = self._top_level_label()
                    print(
                        f'    score {item["score"]:<2} | {item["lines"]:<5} lines | '
                        f'{top_level_label} {item["top_level_composables"]:<2} | '
                        f'state {item["state_signal_hits"]:<2} | '
                        f'roles {role_display:<24} | '
                        f'mode branches {item["mode_branch_hits"]:<2} | '
                        f'File "{item["path"]}"'
                    )
                    print(f'         -> {item["summary"]}')
        else:
            print("  [OK] 扫描完毕，未发现超过风险阈值的文件。")
        print(f"\n{'-' * 100}\n")

    def _top_level_label(self) -> str:
        if self.config.lang == "py":
            return "symbols"
        return "composables"

    @staticmethod
    def _group_responsibility_items_by_priority(matched: list[dict]) -> dict[str, list[dict]]:
        grouped: dict[str, list[dict]] = {}
        for item in matched:
            grouped.setdefault(item["priority"], []).append(item)
        return grouped

    @staticmethod
    def print_missing_path(path: Path) -> None:
        print(f"[SCAN] 正在扫描项目: [{path.name}]")
        print(f"  路径: {path}")
        print("  [ERROR] 路径不存在，跳过扫描。")
        print(f"\n{'-' * 100}\n")
