from __future__ import annotations

import argparse

from ..constants import ROOT_DIR
from ..android_install import install_debug_fresh
from ..android_sdk import install_android_sdk_components
from ..android_signing import (
    ensure_release_signing_config_exists,
    print_release_apk_path_if_present,
    print_staging_apk_path_if_present,
)
from ..constants import ANDROID_GRADLE_ROOT
from ..errors import ToolError
from ..paths import gradle_wrapper
from ..process import run, run_capture, run_capture_merged_streaming
from .android_kotlin_policy import cmd_android_kotlin_policy


ANDROID_ACTIONS = {
    "assemble-debug": {
        "tasks": (":app:assembleDebug",),
        "gradle_args": (),
    },
    "assemble-staging": {
        "tasks": (":app:assembleStaging",),
        "gradle_args": (),
    },
    "assemble-release": {
        "tasks": (":app:assembleRelease",),
        "gradle_args": (),
    },
    "native-debug": {
        "tasks": (":app:externalNativeBuildDebug",),
        "gradle_args": (),
    },
    "test-debug": {
        "tasks": (":app:testDebugUnitTest",),
        "gradle_args": (),
    },
    "modules-smoke": {
        "tasks": (":app:externalNativeBuildDebug",),
        "gradle_args": ("-Pflipbits.android.modulesSmoke=true",),
    },
    "ktlint-check": {
        "tasks": (":app:ktlintCheck",),
        "gradle_args": (),
    },
    "ktlint-format": {
        "tasks": (":app:ktlintFormat",),
        "gradle_args": (),
    },
    "detekt": {
        "tasks": (":app:detekt",),
        "gradle_args": (),
    },
    "quality": {
        "tasks": (
            ":app:ktlintCheck",
            ":app:detekt",
        ),
        "gradle_args": (),
    },
}


def _extra_gradle_args(args: argparse.Namespace) -> list[str]:
    tests: list[str] = list(getattr(args, "tests", []) or [])
    if not tests:
        return []
    if args.action != "test-debug":
        raise ToolError("--tests is only supported with `python tools/run.py android test-debug`.")
    gradle_args: list[str] = []
    for test_filter in tests:
        gradle_args.extend(["--tests", test_filter])
    return gradle_args


def _run_android_resource_escape_autofix() -> None:
    run(
        [
            "python",
            "tools/repo_tooling/android_translate/run.py",
            "fix-resource-escapes",
            "--res-dir",
            "apps/audio_android/app/src/main/res",
            "--quiet",
        ],
        cwd=ROOT_DIR,
    )

def _add_android_string_key(args: argparse.Namespace) -> None:
    missing_args = [
        option
        for option in ("file", "key", "en")
        if not getattr(args, option, None)
    ]
    if missing_args:
        formatted_args = ", ".join(f"--{option}" for option in missing_args)
        raise ToolError(
            "android strings-add requires resource filename, key, and English text.\n"
            f"Missing: {formatted_args}\n"
            "Example:\n"
            "  python tools/run.py android strings-add --file strings_audio.xml "
            "--key sample_key --en \"Sample text\""
        )
    command = [
        "python",
        "tools/repo_tooling/android_translate/run.py",
        "add-key",
        "--file",
        args.file,
        "--key",
        args.key,
        "--en",
        args.en,
    ]
    if args.localized is not None:
        command.extend(["--localized", args.localized])
    if args.context is not None:
        command.extend(["--context", args.context])
    run(command)

    alignment_command = [
        "python",
        "tools/repo_tooling/android_translate/run.py",
        "key-alignment",
        "--json-output",
    ]
    completed = run_capture(alignment_command)
    if completed.returncode == 0:
        print("[android] Translation key alignment is already complete.")
        return
    if completed.returncode == 2:
        print(
            "[android] Translation tasks were generated for missing localized keys.\n"
            "Review: temp/translation_key_alignment_reports/\n"
            "Use the Android translate workflow before relying on a localized build.",
            flush=True,
        )
        return
    print(completed.stdout, end="")
    print(completed.stderr, end="")
    raise SystemExit(completed.returncode)


def cmd_android(args: argparse.Namespace) -> None:
    if getattr(args, "action", None) == "strings-add":
        _add_android_string_key(args)
        return
    if args.action == "install-debug-fresh":
        install_debug_fresh(clean=args.clean)
        return
    if args.action == "install-sdk":
        install_android_sdk_components(accept_licenses=args.accept_licenses)
        return
    if args.action == "kotlin-policy":
        cmd_android_kotlin_policy()
        return

    if args.action == "assemble-release":
        ensure_release_signing_config_exists()

    _run_android_resource_escape_autofix()

    command = gradle_wrapper()
    if args.clean:
        command.append("clean")
    action = ANDROID_ACTIONS[args.action]
    command.extend(action["gradle_args"])
    command.extend(action["tasks"])
    command.extend(_extra_gradle_args(args))
    if args.action == "ktlint-check":
        completed = run_capture_merged_streaming(command, cwd=ANDROID_GRADLE_ROOT)
        if completed.returncode != 0:
            print(
                "\n[android] ktlint-check failed. If the reported issues are auto-correctable, run:\n"
                "  python tools/run.py android ktlint-format\n"
                "Then rerun:\n"
                "  python tools/run.py android ktlint-check",
                flush=True,
            )
            raise SystemExit(completed.returncode)
    else:
        run(command, cwd=ANDROID_GRADLE_ROOT)

    if args.action == "assemble-staging":
        print_staging_apk_path_if_present()
    if args.action == "assemble-release":
        print_release_apk_path_if_present()
