from __future__ import annotations

import argparse
from pathlib import Path

from core.translation_paths import DEFAULT_RES_DIRECTORY
from core.translation_replacement_runner import (
    DEFAULT_JSON_PATH,
    ReplaceCommandResult,
    apply_translation_replacements,
)
from core.xml_string_replacement import apply_replacement_in_string


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Read replacement rules from a JSON file and update localized Android string XML files.\n"
            "The JSON must include a top-level dir plus items containing name, find, and replace."
        )
    )
    parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    parser.add_argument(
        "--json",
        default=str(DEFAULT_JSON_PATH),
        help=(
            "Path to the replacement JSON file. "
            "Defaults to replacements.json in the same directory as this script."
        ),
    )
    parser.add_argument(
        "--auto-fix-json",
        action="store_true",
        help="Apply high-confidence JSON syntax repairs before running replace.",
    )
    return parser.parse_args()


def run() -> int:
    args = parse_args()
    return apply_translation_replacements(
        res_dir=Path(args.res_dir),
        json_path=Path(args.json),
        auto_fix_json=args.auto_fix_json,
    ).exit_code


__all__ = [
    "DEFAULT_JSON_PATH",
    "ReplaceCommandResult",
    "apply_replacement_in_string",
    "apply_translation_replacements",
]


if __name__ == "__main__":
    raise SystemExit(run())
