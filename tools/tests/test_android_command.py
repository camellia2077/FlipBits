from __future__ import annotations

import argparse
import sys
import unittest
from pathlib import Path
from unittest.mock import patch


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from repo_tooling.errors import ToolError
from repo_tooling.commands import android as android_command


class AndroidCommandTests(unittest.TestCase):
    def test_test_debug_forwards_gradle_tests_filters(self) -> None:
        with (
            patch.object(android_command, "gradle_wrapper", return_value=["gradlew"]),
            patch.object(android_command, "run") as mock_run,
        ):
            android_command.cmd_android(
                argparse.Namespace(
                    action="test-debug",
                    clean=False,
                    tests=[
                        "com.bag.audioandroid.ui.SampleInputSessionUpdaterTest",
                        "com.bag.audioandroid.ui.AudioAndroidViewModelTest.someMethod",
                    ],
                ),
            )

        self.assertEqual(
            [
                "gradlew",
                ":app:testDebugUnitTest",
                "--tests",
                "com.bag.audioandroid.ui.SampleInputSessionUpdaterTest",
                "--tests",
                "com.bag.audioandroid.ui.AudioAndroidViewModelTest.someMethod",
            ],
            mock_run.call_args.args[0],
        )
        self.assertEqual(android_command.ANDROID_GRADLE_ROOT, mock_run.call_args.kwargs["cwd"])

    def test_android_actions_run_resource_escape_autofix_before_gradle(self) -> None:
        with (
            patch.object(android_command, "gradle_wrapper", return_value=["gradlew"]),
            patch.object(android_command, "run") as mock_run,
        ):
            android_command.cmd_android(
                argparse.Namespace(
                    action="assemble-debug",
                    clean=False,
                    tests=[],
                ),
            )

        self.assertEqual(
            [
                "python",
                "tools/repo_tooling/android_translate/run.py",
                "fix-resource-escapes",
                "--res-dir",
                "apps/audio_android/app/src/main/res",
                "--quiet",
            ],
            mock_run.call_args_list[0].args[0],
        )
        self.assertEqual(android_command.ROOT_DIR, mock_run.call_args_list[0].kwargs["cwd"])
        self.assertEqual(
            ["gradlew", ":app:assembleDebug"],
            mock_run.call_args_list[1].args[0],
        )
        self.assertEqual(android_command.ANDROID_GRADLE_ROOT, mock_run.call_args_list[1].kwargs["cwd"])

    def test_non_test_debug_rejects_tests_filters(self) -> None:
        with self.assertRaises(ToolError):
            android_command.cmd_android(
                argparse.Namespace(
                    action="assemble-debug",
                    clean=False,
                    tests=["com.bag.audioandroid.ui.SampleInputSessionUpdaterTest"],
                ),
            )


if __name__ == "__main__":
    unittest.main()
