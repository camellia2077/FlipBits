from __future__ import annotations

import argparse

from ...commands import cmd_android_translate
from ..common import RAW_FORMATTER


def register_android_translate_group(
    subparsers: argparse._SubParsersAction[argparse.ArgumentParser],
) -> None:
    android_translate_parser = subparsers.add_parser(
        "android-translate",
        help="Run Android XML translation and prompt tooling.",
        description=(
            "Android XML translation tooling.\n\n"
            "This group owns translation prompt generation, XML inspection, lint/autofix,\n"
            "key alignment, and bulk replacement workflows for Android string resources.\n"
            "Run with no extra arguments to see the runner help, or pass a translate\n"
            "subcommand and its arguments after `android-translate`."
        ),
        formatter_class=RAW_FORMATTER,
    )
    android_translate_parser.add_argument(
        "android_translate_args",
        nargs=argparse.REMAINDER,
        help="Arguments forwarded to the Android translation runner.",
    )
    android_translate_parser.set_defaults(func=cmd_android_translate)
