from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import shutil


class OutputDirectoryManager:
    def __init__(self, output_dir: Path | str) -> None:
        self.output_dir = Path(output_dir)

    def reset(self) -> None:
        if self.output_dir.exists():
            shutil.rmtree(self.output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)

    def ensure(self) -> None:
        self.output_dir.mkdir(parents=True, exist_ok=True)


@dataclass(frozen=True)
class ReportKeyBlock:
    key: str
    fields: tuple[tuple[str, str], ...]


@dataclass(frozen=True)
class ReportFileBlock:
    filename: str
    key_blocks: tuple[ReportKeyBlock, ...]


class MinimalMarkdownReportWriter:
    def write(
        self,
        output_path: Path | str,
        *,
        title: str,
        section: str,
        prompt: str | None = None,
        metadata_lines: tuple[str, ...] = (),
        file_blocks: tuple[ReportFileBlock, ...],
    ) -> None:
        output_path = Path(output_path)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        lines = [f"# {title}", "", f"## {section}"]
        for metadata_line in metadata_lines:
            lines.append(metadata_line)
        if prompt:
            lines.append(f"PROMPT: {prompt}")

        for file_block in file_blocks:
            if not file_block.key_blocks:
                continue
            lines.extend(("", f"FILE: {file_block.filename}"))
            for key_index, key_block in enumerate(file_block.key_blocks):
                lines.append("")
                lines.append(f"KEY: {key_block.key}")
                for label, value in key_block.fields:
                    lines.append(f"{label}: {value}")
                if key_index == len(file_block.key_blocks) - 1:
                    continue

        output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
