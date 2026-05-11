from __future__ import annotations

import shutil
import subprocess

from .build_config import load_build_config
from .errors import ToolError
from .process import print_command, run


def install_android_sdk_components(*, accept_licenses: bool) -> None:
    config = load_build_config()
    sdkmanager = resolve_sdkmanager()
    if accept_licenses:
        accept_android_sdk_licenses(sdkmanager)
    run([sdkmanager, *config.android_sdk.components])


def resolve_sdkmanager() -> str:
    sdkmanager = shutil.which("sdkmanager")
    if sdkmanager is None:
        raise ToolError("Could not find 'sdkmanager' on PATH.")
    return sdkmanager


def accept_android_sdk_licenses(sdkmanager: str) -> None:
    command = [sdkmanager, "--licenses"]
    print_command(command)
    completed = subprocess.run(
        command,
        input="y\n" * 128,
        text=True,
    )
    if completed.returncode != 0:
        raise SystemExit(completed.returncode)
