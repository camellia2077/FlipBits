from __future__ import annotations

import os
import socket
import subprocess
from functools import partial
from http.server import ThreadingHTTPServer

from ..errors import ToolError
from ..process import print_command
from .paths import WEB_NODE_CACHE_ROOT, WEB_PERF_ARTIFACT_ROOT, WEB_SITE_DIR, WEB_TEST_ROOT
from .server import NoCacheStaticHandler


def _find_free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return int(sock.getsockname()[1])


def run_data_perf() -> None:
    if not (WEB_TEST_ROOT / "package.json").exists():
        raise ToolError("tools/web/package.json is missing; Playwright tooling is not available.")

    port = _find_free_port()
    handler = partial(NoCacheStaticHandler, directory=str(WEB_SITE_DIR))
    server = ThreadingHTTPServer(("127.0.0.1", port), handler)
    WEB_PERF_ARTIFACT_ROOT.mkdir(parents=True, exist_ok=True)

    env = os.environ.copy()
    env.setdefault("npm_config_cache", str(WEB_NODE_CACHE_ROOT / "npm-cache"))
    command = [
        "node",
        "perf/data-perf.mjs",
        "--url",
        f"http://127.0.0.1:{port}/",
        "--output-dir",
        str(WEB_PERF_ARTIFACT_ROOT),
    ]
    print(f"[audio_web] Serving {WEB_SITE_DIR} at http://127.0.0.1:{port}", flush=True)
    try:
        server_thread = __import__("threading").Thread(target=server.serve_forever, daemon=True)
        server_thread.start()
        if os.name == "nt":
            print_command(command, WEB_TEST_ROOT)
            completed = subprocess.run(["cmd", "/c", *command], cwd=WEB_TEST_ROOT, env=env)
        else:
            print_command(command, WEB_TEST_ROOT)
            completed = subprocess.run(command, cwd=WEB_TEST_ROOT, env=env)
        if completed.returncode != 0:
            raise SystemExit(completed.returncode)
    finally:
        server.shutdown()
        server.server_close()
        server_thread.join(timeout=5)
