from __future__ import annotations

import argparse

from ...commands import cmd_android
from ..common import RAW_FORMATTER


def register_android_group(subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    android_parser = subparsers.add_parser(
        "android",
        help="Run the standard Android Gradle tasks from apps/audio_android.",
        description=(
            "Run Android Gradle tasks from apps/audio_android.\n\n"
            "Behavior:\n"
            "- Uses the Gradle wrapper inside apps/audio_android.\n"
            "- `install-sdk` installs the Android SDK components declared in tooling/build.toml.\n"
            "- Resolves the action to the matching :app Gradle task.\n"
            "- Optionally prepends `clean` before the selected task.\n"
            "- `assemble-staging` builds a minified, shrink-enabled, debuggable APK for catching release-only issues earlier.\n"
            "- `ktlint-check` / `ktlint-format` / `detekt` provide Kotlin quality tooling for apps/audio_android.\n"
            "- `kotlin-policy` runs lightweight project-specific Kotlin policy checks.\n"
            "- `quality` is the minimal Android Kotlin quality gate: ktlintCheck + detekt.\n"
            "- `modules-smoke` enables the opt-in Android named-modules smoke target for the Phase 3A direct-owner shift.\n"
            "- It does not claim Android host-style `import std;` readiness."
        ),
        formatter_class=RAW_FORMATTER,
    )
    android_parser.add_argument(
        "action",
        choices=[
            "install-sdk",
            "assemble-debug",
            "assemble-staging",
            "assemble-release",
            "native-debug",
            "modules-smoke",
            "ktlint-check",
            "ktlint-format",
            "detekt",
            "kotlin-policy",
            "quality",
            "strings-add",
        ],
        help="Android build action.",
    )
    android_parser.add_argument(
        "--clean",
        action="store_true",
        help="Prepend Gradle clean before the selected task.",
    )
    android_parser.add_argument(
        "--accept-licenses",
        action="store_true",
        help="When used with `install-sdk`, pre-accept Android SDK licenses through sdkmanager.",
    )
    android_parser.add_argument(
        "--file",
        help="When used with `strings-add`, resource XML filename such as strings_audio.xml.",
    )
    android_parser.add_argument(
        "--key",
        help="When used with `strings-add`, string resource key to add.",
    )
    android_parser.add_argument(
        "--en",
        help="When used with `strings-add`, English baseline value.",
    )
    android_parser.add_argument(
        "--localized",
        help="When used with `strings-add`, localized fallback value for values-* files. Defaults to --en.",
    )
    android_parser.add_argument(
        "--context",
        help="When used with `strings-add`, optional CONTEXT comment for the English baseline.",
    )
    android_parser.set_defaults(func=cmd_android)
