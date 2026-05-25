from __future__ import annotations

import argparse

from ...commands import cmd_web
from ..common import RAW_FORMATTER


def register_web_group(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    web_parser = subparsers.add_parser(
        "web",
        help="Run audio_web build, preview, sample export, and tests.",
        description=(
            "Run Web presentation tooling from the repository root.\n\n"
            "Actions:\n"
            "- build-wasm: configure and build the WebAssembly bundle with Emscripten.\n"
            "- export-sample-texts: export Android sample XML into site/data/sample-texts.json.\n"
            "- prepare-pages-site: export samples, build WASM, and validate Pages artifacts.\n"
            "- serve-site: serve apps/audio_web/site locally with no-cache headers.\n"
            "- test: run JS syntax checks and Web tool unit tests."
        ),
        formatter_class=RAW_FORMATTER,
    )
    web_parser.add_argument(
        "action",
        choices=[
            "build-wasm",
            "export-sample-texts",
            "prepare-pages-site",
            "serve-site",
            "test",
        ],
        help="Web tooling action.",
    )
    web_parser.add_argument(
        "--build-dir",
        default="build/web",
        help="Build directory relative to the repository root. Default: build/web",
    )
    web_parser.add_argument(
        "--configuration",
        default="Release",
        help="CMake build configuration for build-wasm and prepare-pages-site. Default: Release",
    )
    web_parser.add_argument(
        "--port",
        type=int,
        default=4173,
        help="Port for serve-site. Default: 4173",
    )
    web_parser.set_defaults(func=cmd_web)
