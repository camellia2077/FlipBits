from __future__ import annotations

import argparse
import os
import shutil
import subprocess
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Build the audio_web WebAssembly bundle.")
    parser.add_argument(
        "--build-dir",
        default="build/web",
        help="Build directory relative to the repository root. Default: build/web",
    )
    parser.add_argument(
        "--configuration",
        default="Release",
        help="CMake build configuration. Default: Release",
    )
    return parser.parse_args()


def candidate_tool_paths(name: str) -> list[Path]:
    suffixes = ["", ".exe", ".bat", ".cmd"]
    candidates: list[Path] = []

    emsdk_root = os.environ.get("EMSDK")
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
    raise SystemExit(
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


def run(command: list[str], cwd: Path) -> None:
    executable = resolve_tool(command[0]) or command[0]
    executable_path = Path(executable)
    command = [executable, *command[1:]]
    print(f"[audio_web] Running: {' '.join(command)}", flush=True)
    if executable_path.suffix.lower() in {".bat", ".cmd"}:
        subprocess.run(
            subprocess.list2cmdline(command),
            cwd=cwd,
            check=True,
            shell=True,
        )
        return

    subprocess.run(command, cwd=cwd, check=True)


def main() -> None:
    args = parse_args()
    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parents[2]
    web_root = script_dir.parent
    build_dir = (repo_root / args.build_dir).resolve()

    require_tool("emcmake")
    require_tool("cmake")
    require_tool("ninja")
    emscripten_root = expected_emscripten_root()

    print(f"[audio_web] Repo root: {repo_root}", flush=True)
    print(f"[audio_web] Build dir: {build_dir}", flush=True)
    print(f"[audio_web] Configuration: {args.configuration}", flush=True)
    if emscripten_root:
        print(f"[audio_web] Emscripten root: {emscripten_root}", flush=True)

    reset_stale_build_dir(build_dir, emscripten_root)

    run(
        [
            "emcmake",
            "cmake",
            "-S",
            str(web_root),
            "-B",
            str(build_dir),
            "-G",
            "Ninja",
            f"-DCMAKE_BUILD_TYPE={args.configuration}",
        ],
        cwd=repo_root,
    )
    run(
        [
            "cmake",
            "--build",
            str(build_dir),
            "--config",
            args.configuration,
        ],
        cwd=repo_root,
    )
    print("[audio_web] WebAssembly build completed.", flush=True)


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as error:
        raise SystemExit(error.returncode) from error
