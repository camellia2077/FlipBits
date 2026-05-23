from __future__ import annotations

import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET


REPO_ROOT = Path(__file__).resolve().parents[3]
ANDROID_RES_ROOT = REPO_ROOT / "apps" / "audio_android" / "app" / "src" / "main" / "res"
OUTPUT_PATH = REPO_ROOT / "apps" / "audio_web" / "site" / "data" / "sample-texts.json"

SACRED_MACHINE_FILES = [
    "audio_samples_sacred_machine_abyssal_quarantine.xml",
    "audio_samples_sacred_machine_engine_awakening.xml",
    "audio_samples_sacred_machine_flesh_transcendence.xml",
    "audio_samples_sacred_machine_forge_chronicle.xml",
    "audio_samples_sacred_machine_origin_pilgrimage.xml",
    "audio_samples_sacred_machine_purge_calculus.xml",
    "audio_samples_sacred_machine_rite_of_maintenance.xml",
    "audio_samples_sacred_machine_signal_litany.xml",
]

THEMED_KEY_PATTERN = re.compile(
    r"^audio_sample_(?P<flavor>.+)_(?P<family>themed)_(?P<length>short|long)_(?P<slug>.+)$"
)
ASCII_KEY_PATTERN = re.compile(
    r"^audio_sample_(?P<flavor>pro)_(?P<family>ascii)_(?P<length>short|long)_(?P<slug>.+)$"
)


# Android sample XML is the source of truth. Keys must follow
# audio_sample_<flavor>_<family>_<length>_<slug>, and `short` / `long`
# are parsed from the key contract rather than inferred from entry order.
def read_string_entries(xml_path: Path) -> list[tuple[str, str]]:
    tree = ET.parse(xml_path)
    root = tree.getroot()
    entries: list[tuple[str, str]] = []
    for node in root.findall("string"):
        name = node.attrib.get("name")
        if not name:
            continue
        text = "".join(node.itertext()).strip()
        entries.append((name, text))
    return entries


def values_dir_to_locale(values_dir_name: str) -> str:
    if values_dir_name == "values":
        return "en"
    suffix = values_dir_name.removeprefix("values-")
    if suffix == "zh":
        return "zh-CN"
    return suffix


def build_sacred_machine_payload() -> tuple[dict[str, dict[str, list[dict[str, str]]]], dict[str, str]]:
    sacred_machine: dict[str, dict[str, list[dict[str, str]]]] = {}
    suffix_to_length: dict[str, str] = {}

    for values_dir in sorted(ANDROID_RES_ROOT.glob("values*")):
        locale = values_dir_to_locale(values_dir.name)
        locale_bucket = {"short": [], "long": []}

        for file_name in SACRED_MACHINE_FILES:
            xml_path = values_dir / file_name
            if not xml_path.exists():
                continue
            entries = read_string_entries(xml_path)
            for name, text in entries:
                match = THEMED_KEY_PATTERN.match(name)
                if not match or match.group("flavor") != "sacred_machine":
                    continue
                length = match.group("length")
                slug = match.group("slug")
                locale_bucket[length].append({"id": slug, "text": text})
                suffix_to_length[slug] = length

        if locale_bucket["short"] or locale_bucket["long"]:
            sacred_machine[locale] = locale_bucket

    return sacred_machine, suffix_to_length


def build_ascii_shared_payload(suffix_to_length: dict[str, str]) -> dict[str, dict[str, list[dict[str, str]]]]:
    shared_path = ANDROID_RES_ROOT / "values" / "audio_samples_pro_ascii_shared.xml"
    entries = read_string_entries(shared_path)
    ascii_shared = {"en": {"short": [], "long": []}}

    for name, text in entries:
        match = ASCII_KEY_PATTERN.match(name)
        if not match:
            continue
        slug = match.group("slug")
        length = match.group("length")
        if suffix_to_length.get(slug) != length:
            continue
        ascii_shared["en"][length].append({"id": slug, "text": text})

    return ascii_shared


def main() -> None:
    sacred_machine, suffix_to_length = build_sacred_machine_payload()
    ascii_shared = build_ascii_shared_payload(suffix_to_length)
    payload = {
        "flavor": "sacred_machine",
        "sacred_machine": sacred_machine,
        "ascii_shared": ascii_shared,
    }

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
