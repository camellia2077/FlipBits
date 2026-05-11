from __future__ import annotations

import argparse
from pathlib import Path

from ..constants import ROOT_DIR
from ..process import run


ANDROID_TRANSLATE_RUNNER = ROOT_DIR / "tools" / "repo_tooling" / "android_translate" / "run.py"


def cmd_android_translate(args: argparse.Namespace) -> None:
    forwarded_args = list(args.android_translate_args)
    if not forwarded_args:
        forwarded_args = ["--help"]
    command = ["python", str(ANDROID_TRANSLATE_RUNNER), *forwarded_args]
    run(command, cwd=ROOT_DIR)
