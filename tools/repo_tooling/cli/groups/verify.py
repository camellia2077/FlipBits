from __future__ import annotations

import argparse

from ...commands import cmd_verify
from ...commands.verify import cmd_verify_review_fixes
from ...commands.verify import format_verify_check_groups
from ...constants import DEFAULT_GENERATOR
from ..common import RAW_FORMATTER, add_common_build_dir_argument


def _add_verify_pipeline_arguments(parser: argparse.ArgumentParser) -> None:
    add_common_build_dir_argument(parser)
    parser.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use for configure. The only supported root-host generator is Ninja.",
    )


def register_verify_command(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    verify_parser = subparsers.add_parser(
        "verify",
        help="Run static policy checks + configure + build + ctest, then apps/audio_android Gradle :app:assembleDebug by default.",
        description=(
            "Run the full verify pipeline.\n\n"
            f"{format_verify_check_groups()}\n\n"
            "Behavior:\n"
            "- Runs static check groups before configure/build/test.\n"
            "- Can optionally run clang-format --check before configure/build/test.\n"
            "- Builds and tests the selected host build directory.\n"
            "- Runs apps/audio_android assembleDebug unless --skip-android is passed.\n\n"
            "Use --list-checks to print the current static check groups without building."
        ),
        formatter_class=RAW_FORMATTER,
    )
    _add_verify_pipeline_arguments(verify_parser)
    verify_parser.add_argument(
        "--skip-android",
        action="store_true",
        help="Skip the apps/audio_android Gradle :app:assembleDebug step.",
    )
    verify_parser.add_argument(
        "--format-check",
        action="store_true",
        help="Run clang-format --check before configure/build/test.",
    )
    verify_parser.add_argument(
        "--format-scope",
        default="libs",
        choices=["libs", "host-native", "android-native", "all-native"],
        help="Source scope to use when --format-check is enabled. Defaults to libs.",
    )
    verify_parser.add_argument(
        "--list-checks",
        action="store_true",
        help="Print the static check groups verify runs before building, then exit.",
    )
    verify_subparsers = verify_parser.add_subparsers(dest="verify_action")

    full_parser = verify_subparsers.add_parser(
        "full",
        help="Run the full verify pipeline. This is also the default when no verify subcommand is given.",
        formatter_class=RAW_FORMATTER,
    )
    _add_verify_pipeline_arguments(full_parser)
    full_parser.add_argument(
        "--skip-android",
        action="store_true",
        help="Skip the apps/audio_android Gradle :app:assembleDebug step.",
    )
    full_parser.add_argument(
        "--format-check",
        action="store_true",
        help="Run clang-format --check before configure/build/test.",
    )
    full_parser.add_argument(
        "--format-scope",
        default="libs",
        choices=["libs", "host-native", "android-native", "all-native"],
        help="Source scope to use when --format-check is enabled. Defaults to libs.",
    )
    full_parser.add_argument(
        "--list-checks",
        action="store_true",
        help="Print the static check groups verify runs before building, then exit.",
    )
    full_parser.set_defaults(func=cmd_verify)

    review_parser = verify_subparsers.add_parser(
        "review-fixes",
        help="Run a focused review-fix gate: static policy checks, translation key alignment, audio_api tests, ktlint, assembleDebug.",
        description=(
            "Run the focused gate normally useful after code-review fixes.\n\n"
            "Steps:\n"
            "- Static policy checks from verify\n"
            "- Android translation key alignment\n"
            "- audio_api library tests\n"
            "- Android ktlint-check\n"
            "- Android assemble-debug"
        ),
        formatter_class=RAW_FORMATTER,
    )
    add_common_build_dir_argument(review_parser)
    review_parser.set_defaults(func=cmd_verify_review_fixes)

    verify_parser.set_defaults(func=cmd_verify)
