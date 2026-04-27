from __future__ import annotations

from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[5]
DEFAULT_RES_DIRECTORY = REPO_ROOT / "apps" / "audio_android" / "app" / "src" / "main" / "res"
TEXT_TYPES = ("app_text", "sample_text")
FACTIONS = (
    "ancient_dynasty",
    "exquisite_fall",
    "immortal_rot",
    "labyrinth_of_mutability",
    "sacred_machine",
    "scarlet_carnage",
)
APP_TEXT_GROUPS = (
    "strings_about",
    "strings_audio",
    "strings_common",
    "strings_saved",
    "strings_settings",
    "strings_validation",
)

TRANSLATION_TEXT_XML_PATTERNS = (
    "audio_samples_*.xml",
    "strings.xml",
    "strings_*.xml",
)


def get_text_type_from_filename(filename: str) -> str:
    if filename.startswith("audio_samples_"):
        return "sample_text"
    return "app_text"


def get_faction_from_filename(filename: str) -> str:
    if filename.startswith("strings_"):
        stem = Path(filename).stem
        if stem in APP_TEXT_GROUPS:
            return stem
    for faction in FACTIONS:
        if filename.startswith(f"audio_samples_{faction}"):
            return faction
    return "other"


def get_review_groups_for_text_type(text_type: str) -> tuple[str, ...]:
    if text_type == "app_text":
        return APP_TEXT_GROUPS + ("other",)
    return FACTIONS + ("other",)


def is_pro_sample_key(key: str) -> bool:
    return key.startswith("audio_sample_") and "_ascii_" in key


def display_language_tag(lang_code: str) -> str:
    if lang_code == "pt-rBR":
        return "PT-BR"
    return lang_code.upper()


def humanize_name(value: str) -> str:
    return value.replace("_", " ").title()


def iter_translation_text_xml_paths(directory: Path) -> list[Path]:
    xml_paths: list[Path] = []
    for pattern in TRANSLATION_TEXT_XML_PATTERNS:
        xml_paths.extend(sorted(directory.glob(pattern)))
    return xml_paths
