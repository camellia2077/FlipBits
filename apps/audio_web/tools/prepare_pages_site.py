from __future__ import annotations

import subprocess
import sys
from pathlib import Path


def run(command: list[str], cwd: Path) -> None:
    print(f"[audio_web] Running: {' '.join(command)}", flush=True)
    subprocess.run(command, cwd=cwd, check=True)


def main() -> None:
    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parents[2]
    site_dir = repo_root / "apps" / "audio_web" / "site"

    run([sys.executable, str(script_dir / "export_sample_texts.py")], cwd=repo_root)
    run(
        [
            sys.executable,
            str(script_dir / "build_wasm.py"),
            "--build-dir",
            "build/web",
            "--configuration",
            "Release",
        ],
        cwd=repo_root,
    )

    if not (site_dir / "index.html").exists():
        raise SystemExit(f"Missing Pages entry file: {site_dir / 'index.html'}")
    if not (site_dir / "wasm" / "flipbits_web_runtime.js").exists():
        raise SystemExit("Missing generated runtime JS for Pages deployment.")
    if not (site_dir / "wasm" / "flipbits_web_runtime.wasm").exists():
        raise SystemExit("Missing generated runtime WASM for Pages deployment.")

    print(f"[audio_web] Pages site is ready at {site_dir}", flush=True)


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as error:
        raise SystemExit(error.returncode) from error
