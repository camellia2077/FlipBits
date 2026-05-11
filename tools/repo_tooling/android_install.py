from __future__ import annotations

import os
import shutil
import subprocess

from .constants import ANDROID_GRADLE_ROOT
from .errors import ToolError
from .paths import gradle_wrapper
from .process import print_command, run


ANDROID_APP_ID = "com.bag.audioandroid"
DEBUG_APK_OUTPUT_PATH = ANDROID_GRADLE_ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
MIN_DEBUG_APK_BYTES = 10 * 1024 * 1024


def install_debug_fresh(*, clean: bool) -> None:
    command = gradle_wrapper()
    if clean:
        command.append("clean")
    command.append(":app:assembleDebug")
    run(command, cwd=ANDROID_GRADLE_ROOT)

    ensure_debug_apk_ready()
    adb = resolve_adb()

    uninstall_command = [adb, "uninstall", ANDROID_APP_ID]
    print_command(uninstall_command)
    uninstall_result = subprocess.run(uninstall_command)
    if uninstall_result.returncode != 0:
        print(f"[android] adb uninstall returned {uninstall_result.returncode}; continuing with fresh install.")

    install_command = [adb, "install", os.fspath(DEBUG_APK_OUTPUT_PATH)]
    run(install_command)
    print(f"[android] Installed fresh debug APK: {DEBUG_APK_OUTPUT_PATH}")


def ensure_debug_apk_ready() -> None:
    if not DEBUG_APK_OUTPUT_PATH.exists():
        raise ToolError(f"Debug APK was not produced: {DEBUG_APK_OUTPUT_PATH}")
    apk_size = DEBUG_APK_OUTPUT_PATH.stat().st_size
    if apk_size < MIN_DEBUG_APK_BYTES:
        raise ToolError(
            "Debug APK is unexpectedly small; refusing to install a likely incomplete artifact.\n"
            f"APK: {DEBUG_APK_OUTPUT_PATH}\n"
            f"Size: {apk_size} bytes"
        )


def resolve_adb() -> str:
    adb = shutil.which("adb")
    if adb is None:
        raise ToolError("Could not find 'adb' on PATH.")
    return adb
