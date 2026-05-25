from __future__ import annotations

import os
from pathlib import Path
import shutil

from ..errors import ToolError


def candidate_tool_paths(name: str, *, env: dict[str, str] | None = None) -> list[Path]:
    suffixes = ["", ".exe", ".bat", ".cmd"]
    candidates: list[Path] = []
    environ = env or os.environ

    emsdk_root = environ.get("EMSDK")
    if emsdk_root:
        emsdk_path = Path(emsdk_root)
        search_roots = [
            emsdk_path,
            emsdk_path / "upstream" / "emscripten",
        ]
        for root in search_roots:
            for suffix in suffixes:
                candidates.append(root / f"{name}{suffix}")

    return candidates


def resolve_tool(name: str) -> str | None:
    resolved = shutil.which(name)
    if resolved:
        return resolved

    for candidate in candidate_tool_paths(name):
        if candidate.exists():
            return str(candidate)

    return None


def require_tool(name: str) -> str:
    resolved = resolve_tool(name)
    if resolved:
        return resolved
    raise ToolError(
        f"Missing required tool: {name}. Install Emscripten first and ensure {name} is on PATH or EMSDK is set."
    )


def expected_emscripten_root() -> Path | None:
    emsdk_root = os.environ.get("EMSDK")
    if not emsdk_root:
        return None
    return Path(emsdk_root) / "upstream" / "emscripten"


def cache_contains_stale_emscripten_path(build_dir: Path, expected_root: Path | None) -> bool:
    if not build_dir.exists():
        return False

    files_to_check = [build_dir / "CMakeCache.txt"]
    cmake_files_dir = build_dir / "CMakeFiles"
    if cmake_files_dir.exists():
        files_to_check.extend(cmake_files_dir.rglob("CMakeSystem.cmake"))

    expected = str(expected_root).replace("\\", "/").lower() if expected_root else None
    for file_path in files_to_check:
        if not file_path.exists():
            continue
        try:
            content = file_path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue

        normalized = content.replace("\\", "/").lower()
        if "emscripten" not in normalized:
            continue
        if expected and expected in normalized:
            continue
        return True

    return False


def reset_stale_build_dir(build_dir: Path, expected_root: Path | None) -> None:
    if not cache_contains_stale_emscripten_path(build_dir, expected_root):
        return

    expected_display = str(expected_root) if expected_root else "<unknown>"
    print(
        "[audio_web] Detected stale Emscripten paths in the build directory. "
        f"Resetting {build_dir} for toolchain {expected_display}.",
        flush=True,
    )
    shutil.rmtree(build_dir, ignore_errors=False)
