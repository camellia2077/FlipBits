from __future__ import annotations

import subprocess
from dataclasses import dataclass
from pathlib import Path

from core.translation_paths import REPO_ROOT


ANDROID_GRADLE_ROOT = REPO_ROOT / "apps" / "audio_android"
GRADLE_WRAPPER_WINDOWS = ANDROID_GRADLE_ROOT / "gradlew.bat"
GRADLE_WRAPPER_UNIX = ANDROID_GRADLE_ROOT / "gradlew"
RESOURCE_SMOKE_TASK = ":app:mergeDebugResources"


@dataclass(frozen=True)
class SmokeCheckResult:
    ok: bool
    command: tuple[str, ...]
    return_code: int


def run_android_resource_smoke_check() -> SmokeCheckResult:
    return run_android_resource_smoke_check_with_options()


def run_android_resource_smoke_check_with_options(*, quiet: bool = False) -> SmokeCheckResult:
    command = _gradle_wrapper_command() + (RESOURCE_SMOKE_TASK,)
    if not quiet:
        print("[translate] Running Android resource smoke check")
        print("+ " + " ".join(command) + f" (cwd={ANDROID_GRADLE_ROOT})")
    completed = subprocess.run(
        command,
        cwd=ANDROID_GRADLE_ROOT,
        check=False,
    )
    return SmokeCheckResult(
        ok=completed.returncode == 0,
        command=command,
        return_code=completed.returncode,
    )


def _gradle_wrapper_command() -> tuple[str, ...]:
    if GRADLE_WRAPPER_WINDOWS.exists():
        return (str(GRADLE_WRAPPER_WINDOWS),)
    if GRADLE_WRAPPER_UNIX.exists():
        return (str(GRADLE_WRAPPER_UNIX),)
    raise FileNotFoundError(
        "Could not find the Android Gradle wrapper under "
        f"{ANDROID_GRADLE_ROOT}."
    )
