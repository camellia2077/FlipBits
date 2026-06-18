from __future__ import annotations

import argparse

from ..constants import ROOT_DIR
from ..errors import ToolError
from ..web.build import build_wasm, prepare_pages_site
from ..web.perf import run_data_perf
from ..web.sample_texts import export_sample_texts
from ..web.server import serve_site
from ..web.tests import run_web_tests


def cmd_web(args: argparse.Namespace) -> None:
    action = args.action
    build_dir = (ROOT_DIR / args.build_dir).resolve()
    if action == "build-wasm":
        build_wasm(build_dir=build_dir, configuration=args.configuration)
        return
    if action == "export-sample-texts":
        export_sample_texts()
        return
    if action == "prepare-pages-site":
        prepare_pages_site(build_dir=build_dir, configuration=args.configuration)
        return
    if action == "serve-site":
        serve_site(port=args.port)
        return
    if action == "perf-data":
        run_data_perf()
        return
    if action == "test":
        run_web_tests()
        return
    raise ToolError(f"Unsupported web action: {action}")
