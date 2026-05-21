from __future__ import annotations

import argparse
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from repo_tooling.commands import build as build_command
from repo_tooling.commands import configure as configure_command


class HostBuildToolTests(unittest.TestCase):
    def test_configure_omits_compiler_when_not_configured(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            with (
                patch.dict("os.environ", {"CXX": ""}),
                patch.object(configure_command, "run") as mock_run,
            ):
                configure_command.cmd_configure(
                    argparse.Namespace(
                        build_dir=temp_dir,
                        generator="Ninja",
                        compiler=None,
                    ),
                )

            command = mock_run.call_args.args[0]
            self.assertFalse(any(item.startswith("-DCMAKE_CXX_COMPILER=") for item in command))

    def test_configure_respects_cxx_environment_when_compiler_missing(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            with (
                patch.dict("os.environ", {"CXX": "custom-clang++"}),
                patch.object(configure_command, "run") as mock_run,
            ):
                configure_command.cmd_configure(
                    argparse.Namespace(
                        build_dir=temp_dir,
                        generator="Ninja",
                        compiler=None,
                    ),
                )

            command = mock_run.call_args.args[0]
            self.assertIn("-DCMAKE_CXX_COMPILER=custom-clang++", command)
            self.assertNotIn("-DCMAKE_CXX_COMPILER=None", command)

    def test_configure_ignores_none_string_compiler(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            with (
                patch.dict("os.environ", {"CXX": ""}),
                patch.object(configure_command, "run") as mock_run,
            ):
                configure_command.cmd_configure(
                    argparse.Namespace(
                        build_dir=temp_dir,
                        generator="Ninja",
                        compiler="None",
                    ),
                )

            command = mock_run.call_args.args[0]
            self.assertFalse(any(item.startswith("-DCMAKE_CXX_COMPILER=") for item in command))

    def test_build_auto_configure_does_not_forward_none_compiler_literal(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            with (
                patch.dict("os.environ", {"CXX": ""}),
                patch.object(build_command, "cmake_cache_exists", return_value=False),
                patch.object(configure_command, "cmake_cache_exists", return_value=False),
                patch.object(configure_command, "run") as mock_configure_run,
                patch.object(build_command, "run"),
            ):
                build_command.cmd_build(
                    argparse.Namespace(
                        build_dir=temp_dir,
                        generator="Ninja",
                        compiler=None,
                        configure_if_missing=True,
                        target=None,
                    ),
                )

        configure_command_line = mock_configure_run.call_args.args[0]
        self.assertFalse(any(item.startswith("-DCMAKE_CXX_COMPILER=") for item in configure_command_line))


if __name__ == "__main__":
    unittest.main()
