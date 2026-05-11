from __future__ import annotations

from pathlib import Path

from .constants import ANDROID_GRADLE_ROOT
from .errors import ToolError


RELEASE_SIGNING_PROPERTIES_PATH = ANDROID_GRADLE_ROOT / "app" / "release-signing.properties"
RELEASE_SIGNING_DIRECTORY_PATH = ANDROID_GRADLE_ROOT / "app"
RELEASE_APK_OUTPUT_PATH = (
    ANDROID_GRADLE_ROOT / "app" / "build" / "outputs" / "apk" / "release" / "FlipBits-release.apk"
)
STAGING_APK_OUTPUT_PATH = (
    ANDROID_GRADLE_ROOT / "app" / "build" / "outputs" / "apk" / "staging" / "FlipBits-staging.apk"
)


def ensure_release_signing_config_exists() -> None:
    if not RELEASE_SIGNING_PROPERTIES_PATH.exists():
        raise ToolError(
            "Missing Android release signing file.\n"
            f"Directory: {RELEASE_SIGNING_DIRECTORY_PATH}\n"
            "Please place the required file at:\n"
            f"{RELEASE_SIGNING_PROPERTIES_PATH}"
        )

    signing_properties = read_release_signing_properties()
    store_file_value = signing_properties.get("storeFile")
    if not store_file_value:
        raise ToolError(
            "Missing 'storeFile' in Android release signing config.\n"
            f"Please update: {RELEASE_SIGNING_PROPERTIES_PATH}"
        )

    resolved_keystore_path = resolve_release_keystore_path(store_file_value)
    if resolved_keystore_path.exists():
        return

    raise ToolError(
        "Missing Android release keystore file.\n"
        f"Configured by: {RELEASE_SIGNING_PROPERTIES_PATH}\n"
        f"Directory: {RELEASE_SIGNING_DIRECTORY_PATH}\n"
        f"Configured storeFile: {store_file_value}\n"
        f"Resolved keystore path: {resolved_keystore_path}"
    )


def read_release_signing_properties() -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in RELEASE_SIGNING_PROPERTIES_PATH.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def resolve_release_keystore_path(store_file_value: str) -> Path:
    keystore_path = Path(store_file_value).expanduser()
    if keystore_path.is_absolute():
        return keystore_path
    return (RELEASE_SIGNING_DIRECTORY_PATH / keystore_path).resolve()


def print_release_apk_path_if_present() -> None:
    if RELEASE_APK_OUTPUT_PATH.exists():
        print(f"Release APK: {RELEASE_APK_OUTPUT_PATH}")


def print_staging_apk_path_if_present() -> None:
    if STAGING_APK_OUTPUT_PATH.exists():
        print(f"Staging APK: {STAGING_APK_OUTPUT_PATH}")
