#!/usr/bin/env python3
from __future__ import annotations

import runpy
import sys
from pathlib import Path


if __name__ == "__main__":
    runner = (
        Path(__file__).resolve().parents[3]
        / "repo_tooling"
        / "android_translate"
        / "run.py"
    )
    sys.path.insert(0, str(runner.parent))
    runpy.run_path(str(runner), run_name="__main__")
