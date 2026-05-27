from __future__ import annotations

import re
from pathlib import Path

from ..constants import ROOT_DIR
from ..errors import ToolError
from .model import ParsedHistoryEntry


_RELEASE_HEADING_RE = re.compile(r"^## \[(v\d+\.\d+\.\d+)\] - (\d{4}-\d{2}-\d{2})$")
_TOOLS_HEADING_RE = re.compile(r"^## (\d{4}-\d{2}-\d{2})$")
_RELEASE_SECTION_RE = re.compile(r"^### .+ \((Added|Changed/Refactor|Fixed)\)$")
_TOOLS_SECTION_MAP = {
    "新增命令": "Added",
    "工作流调整": "Changed/Refactor",
    "校验与策略": "Changed/Refactor",
    "重构": "Changed/Refactor",
    "修复": "Fixed",
}


def component_name_for_history(path: str) -> str:
    normalized = path.replace("\\", "/")
    if normalized.startswith("docs/history/presentation/cli/"):
        return "cli-presentation"
    if normalized.startswith("docs/history/presentation/android/"):
        return "android-presentation"
    if normalized.startswith("docs/history/libs/"):
        return "libs"
    if normalized.startswith("docs/tools/history/"):
        return "tools"
    return Path(normalized).parent.name or "changed-component"


def parse_history_file(path: str) -> ParsedHistoryEntry:
    text = (ROOT_DIR / path).read_text(encoding="utf-8")
    release_version = ""
    release_date = ""
    current_section: str | None = None
    section_bullets: dict[str, list[str]] = {"Added": [], "Changed/Refactor": [], "Fixed": []}

    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line:
            continue

        heading_match = _RELEASE_HEADING_RE.match(line)
        if heading_match:
            release_version, release_date = heading_match.groups()
            continue

        tools_heading_match = _TOOLS_HEADING_RE.match(line)
        if tools_heading_match:
            release_version = "TODO(agent): set release version"
            release_date = tools_heading_match.group(1)
            continue

        section_match = _RELEASE_SECTION_RE.match(line)
        if section_match:
            current_section = section_match.group(1)
            continue

        if line.startswith("### "):
            section_title = line[4:].strip()
            current_section = _TOOLS_SECTION_MAP.get(section_title)
            continue

        if current_section and (line.startswith("* ") or line.startswith("- ")):
            section_bullets[current_section].append(line[2:].strip())

    if not release_version or not release_date:
        raise ToolError(f"History file is missing a valid release heading: {path}")

    return ParsedHistoryEntry(
        path=path,
        release_version=release_version,
        release_date=release_date,
        component_name=component_name_for_history(path),
        added=section_bullets["Added"],
        changed=section_bullets["Changed/Refactor"],
        fixed=section_bullets["Fixed"],
    )
