from __future__ import annotations

import argparse
from pathlib import Path

from ..errors import ToolError
from ..paths import resolve_build_dir
from .test import cmd_test


_LIBRARY_BUILD_DIRS: dict[str, Path] = {
    "audio_runtime": Path("libs") / "audio_runtime",
    "audio_api": Path("libs") / "audio_api",
    "audio_io": Path("libs") / "audio_io",
}

_ROOT_TEST_REGEX_BY_LIBRARY: dict[str, str] = {
    "audio_core": "flash_voicing_tests|modules_.*_smoke",
}


def cmd_test_lib(args: argparse.Namespace) -> None:
    root_tests_regex = _ROOT_TEST_REGEX_BY_LIBRARY.get(args.library)
    if root_tests_regex is not None:
        delegated_args = argparse.Namespace(
            build_dir=str(resolve_build_dir(args.build_dir)),
            output_on_failure=args.output_on_failure,
            tests_regex=args.tests_regex or root_tests_regex,
            report_dir=args.report_dir,
            write_report=args.write_report,
        )
        cmd_test(delegated_args)
        return

    library_build_suffix = _LIBRARY_BUILD_DIRS.get(args.library)
    if library_build_suffix is None:
        raise ToolError(f"Unsupported library test target: {args.library}")

    build_dir = resolve_build_dir(args.build_dir) / library_build_suffix
    delegated_args = argparse.Namespace(
        build_dir=str(build_dir),
        output_on_failure=args.output_on_failure,
        tests_regex=args.tests_regex,
        report_dir=args.report_dir,
        write_report=args.write_report,
    )
    cmd_test(delegated_args)
