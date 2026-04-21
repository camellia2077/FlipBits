from __future__ import annotations

import argparse
import os
import shutil
from pathlib import Path

from ..constants import CLI_RUST_DIR, CLI_TARGET_NAME, RUST_CLI_TARGET_TRIPLE
from ..paths import resolve_build_dir
from ..process import run


def cmd_cli(args: argparse.Namespace) -> None:
    build_dir = resolve_build_dir(args.build_dir)
    cargo_env = os.environ.copy()
    base_env = getattr(args, "env", None)
    if base_env:
        cargo_env.update(base_env)
    cargo_env["FLIPBITS_CMAKE_BUILD_DIR"] = str(build_dir)
    cargo_env["CARGO_TARGET_DIR"] = str(_cargo_target_dir(build_dir))

    command = ["cargo", args.action, "--target", RUST_CLI_TARGET_TRIPLE]
    if args.release:
        command.append("--release")

    run(command, cwd=CLI_RUST_DIR, env=cargo_env)

    if args.action == "build":
        cargo_artifact_path = _cargo_artifact_path(build_dir=build_dir, release=args.release)
        final_artifact_path = _final_artifact_path(build_dir)
        final_artifact_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(cargo_artifact_path, final_artifact_path)
        print(f"CLI artifact: {final_artifact_path}", flush=True)


def _cargo_target_dir(build_dir: Path) -> Path:
    return build_dir / "rust-cli" / "target"


def _cargo_artifact_path(*, build_dir: Path, release: bool) -> Path:
    profile = "release" if release else "debug"
    suffix = ".exe" if os.name == "nt" else ""
    return _cargo_target_dir(build_dir) / RUST_CLI_TARGET_TRIPLE / profile / f"{CLI_TARGET_NAME}{suffix}"


def _final_artifact_path(build_dir: Path) -> Path:
    suffix = ".exe" if os.name == "nt" else ""
    return build_dir / "bin" / f"{CLI_TARGET_NAME}{suffix}"
