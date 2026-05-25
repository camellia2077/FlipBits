from __future__ import annotations

from pathlib import Path
import subprocess

from ..constants import ROOT_DIR
from ..errors import ToolError
from .emscripten import expected_emscripten_root, require_tool, reset_stale_build_dir, resolve_tool
from .paths import WEB_ROOT, WEB_SITE_DIR
from .sample_texts import export_sample_texts


def run_web_build_command(command: list[str], cwd: Path) -> None:
    executable = resolve_tool(command[0]) or command[0]
    executable_path = Path(executable)
    resolved_command = [executable, *command[1:]]
    print(f"[audio_web] Running: {' '.join(resolved_command)}", flush=True)
    if executable_path.suffix.lower() in {".bat", ".cmd"}:
        subprocess.run(
            subprocess.list2cmdline(resolved_command),
            cwd=cwd,
            check=True,
            shell=True,
        )
        return

    subprocess.run(resolved_command, cwd=cwd, check=True)


def build_wasm(*, build_dir: Path, configuration: str) -> None:
    require_tool("emcmake")
    require_tool("cmake")
    require_tool("ninja")
    emscripten_root = expected_emscripten_root()

    print(f"[audio_web] Repo root: {ROOT_DIR}", flush=True)
    print(f"[audio_web] Build dir: {build_dir}", flush=True)
    print(f"[audio_web] Configuration: {configuration}", flush=True)
    if emscripten_root:
        print(f"[audio_web] Emscripten root: {emscripten_root}", flush=True)

    reset_stale_build_dir(build_dir, emscripten_root)

    run_web_build_command(
        [
            "emcmake",
            "cmake",
            "-S",
            str(WEB_ROOT),
            "-B",
            str(build_dir),
            "-G",
            "Ninja",
            f"-DCMAKE_BUILD_TYPE={configuration}",
        ],
        cwd=ROOT_DIR,
    )
    run_web_build_command(
        [
            "cmake",
            "--build",
            str(build_dir),
            "--config",
            configuration,
        ],
        cwd=ROOT_DIR,
    )
    print("[audio_web] WebAssembly build completed.", flush=True)


def prepare_pages_site(*, build_dir: Path, configuration: str) -> None:
    export_sample_texts()
    build_wasm(build_dir=build_dir, configuration=configuration)

    if not (WEB_SITE_DIR / "index.html").exists():
        raise ToolError(f"Missing Pages entry file: {WEB_SITE_DIR / 'index.html'}")
    if not (WEB_SITE_DIR / "wasm" / "flipbits_web_runtime.js").exists():
        raise ToolError("Missing generated runtime JS for Pages deployment.")
    if not (WEB_SITE_DIR / "wasm" / "flipbits_web_runtime.wasm").exists():
        raise ToolError("Missing generated runtime WASM for Pages deployment.")

    print(f"[audio_web] Pages site is ready at {WEB_SITE_DIR}", flush=True)
