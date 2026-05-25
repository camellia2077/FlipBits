from __future__ import annotations

import os
from pathlib import Path
import subprocess
import sys

from ..process import print_command, run
from .paths import WEB_NODE_CACHE_ROOT, WEB_SITE_DIR, WEB_TEST_ARTIFACT_ROOT, WEB_TEST_ROOT


def web_js_files() -> list[Path]:
    js_root = WEB_SITE_DIR / "js"
    files = sorted(js_root.glob("*.js"))
    files.extend(sorted((js_root / "i18n").glob("*.js")))
    return files


def run_node_syntax_checks() -> None:
    for js_file in web_js_files():
        run(["node", "--check", str(js_file)])


def run_python_web_tool_tests() -> None:
    run([sys.executable, "-m", "unittest", "discover", "-s", "tools/tests", "-p", "test_web_*.py"])


def run_playwright_tests() -> None:
    if not (WEB_TEST_ROOT / "package.json").exists():
        return

    env = os.environ.copy()
    env.setdefault("npm_config_cache", str(WEB_NODE_CACHE_ROOT / "npm-cache"))
    WEB_TEST_ARTIFACT_ROOT.mkdir(parents=True, exist_ok=True)
    npm_command = ["npm", "test", "--", "--config", "playwright.config.js"]
    if os.name == "nt":
        command = ["cmd", "/c", *npm_command]
        print_command(npm_command, WEB_TEST_ROOT)
        completed = subprocess.run(command, cwd=WEB_TEST_ROOT, env=env)
        if completed.returncode != 0:
            raise SystemExit(completed.returncode)
        return

    run(npm_command, cwd=WEB_TEST_ROOT, env=env)


def run_web_tests() -> None:
    run_node_syntax_checks()
    run_python_web_tool_tests()
    run_playwright_tests()
    print("[audio_web] Web tests completed.", flush=True)
