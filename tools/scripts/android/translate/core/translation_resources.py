from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass
from pathlib import Path
import xml.etree.ElementTree as ET

from core.translation_paths import (
    DEFAULT_RES_DIRECTORY,
    get_faction_from_filename,
    get_text_type_from_filename,
    iter_translation_text_xml_paths,
)


@dataclass(frozen=True)
class ResourceFile:
    filename: str
    text_type: str
    faction: str
    strings: dict[str, str]
    sample_lengths: dict[str, str | None]
    contexts: dict[str, str | None]


def infer_sample_lengths(filename: str, strings: dict[str, str]) -> dict[str, str | None]:
    sample_lengths = {key: None for key in strings}
    if get_text_type_from_filename(filename) != "sample_text":
        return sample_lengths

    themed_keys = [key for key in strings if key.startswith("audio_sample_") and "_themed_" in key]
    if not themed_keys:
        return sample_lengths

    sample_lengths[themed_keys[0]] = "SHORT"
    for key in themed_keys[1:]:
        sample_lengths[key] = "LONG"
    return sample_lengths


@dataclass(frozen=True)
class ParsedXmlStrings:
    strings: dict[str, str]
    contexts: dict[str, str | None]


class AndroidStringResourceRepository:
    def __init__(self, res_dir: Path | str = DEFAULT_RES_DIRECTORY) -> None:
        self.res_dir = Path(res_dir)
        self.base_values_dir = self.res_dir / "values"

    def ensure_base_directory(self) -> None:
        if not self.base_values_dir.exists():
            raise FileNotFoundError(f"找不到基础英文目录 {self.base_values_dir}")

    def extract_strings_from_xml(self, xml_path: Path) -> ParsedXmlStrings:
        strings_dict: dict[str, str] = {}
        contexts_dict: dict[str, str | None] = {}
        try:
            parser = ET.XMLParser(target=ET.TreeBuilder(insert_comments=True))
            tree = ET.parse(xml_path, parser=parser)
            pending_context: str | None = None
            for child in tree.getroot():
                if child.tag is ET.Comment:
                    comment_text = (child.text or "").strip()
                    if comment_text.startswith("CONTEXT:"):
                        pending_context = comment_text.removeprefix("CONTEXT:").strip()
                    continue

                if child.tag != "string":
                    pending_context = None
                    continue

                name = child.get("name")
                text = "".join(child.itertext()).strip()
                if name:
                    strings_dict[name] = text
                    contexts_dict[name] = pending_context
                pending_context = None
        except Exception as exc:
            print(f"解析出错 {xml_path}: {exc}")
        return ParsedXmlStrings(strings=strings_dict, contexts=contexts_dict)

    def load_base_resource_files(
        self,
        *,
        string_filter: Callable[[str, str], bool] | None = None,
    ) -> dict[str, ResourceFile]:
        resource_files: dict[str, ResourceFile] = {}
        # The English `values/` directory is the only structural baseline for
        # translation alignment. Style languages such as `values-la` still count
        # as localized outputs, even if their prose intentionally mixes Latin and
        # English for the project's stylized Latinized presentation.
        for xml_path in iter_translation_text_xml_paths(self.base_values_dir):
            parsed = self.extract_strings_from_xml(xml_path)
            strings = parsed.strings
            contexts = parsed.contexts
            if string_filter is not None:
                strings = {
                    key: text
                    for key, text in strings.items()
                    if string_filter(key, text)
                }
                contexts = {
                    key: contexts.get(key)
                    for key in strings
                }
            if not strings:
                continue
            resource_files[xml_path.name] = ResourceFile(
                filename=xml_path.name,
                text_type=get_text_type_from_filename(xml_path.name),
                faction=get_faction_from_filename(xml_path.name),
                strings=strings,
                sample_lengths=infer_sample_lengths(xml_path.name, strings),
                contexts=contexts,
            )
        return resource_files

    def iter_localized_directories(self) -> list[tuple[str, Path]]:
        return [
            (path.name.replace("values-", ""), path)
            for path in sorted(self.res_dir.iterdir())
            if path.is_dir() and path.name.startswith("values-")
        ]

    def localized_xml_names(self, folder_path: Path, base_files: dict[str, ResourceFile]) -> list[str]:
        return sorted(path.name for path in folder_path.glob("*.xml") if path.name in base_files)

    def load_localized_strings(self, folder_path: Path, filename: str) -> dict[str, str]:
        return self.extract_strings_from_xml(folder_path / filename).strings
