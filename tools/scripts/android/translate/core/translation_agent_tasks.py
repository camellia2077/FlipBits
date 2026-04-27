from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

from core.translation_paths import display_language_tag
from core.translation_resources import ResourceFile


@dataclass(frozen=True)
class AgentTaskEntry:
    dir_name: str
    xml: str
    name: str
    sample_length: str | None
    context: str | None
    english: str
    localized: str


def build_agent_task_payload(
    *,
    output_dir: Path | str,
    prompt_mode: str,
    prompt_version: str,
    generated_at: str,
    language_code: str,
    text_type: str | None,
    group: str | None,
    prompt_text_type: str,
    prompt_doc_path: Path | str,
    entries: list[AgentTaskEntry],
) -> dict[str, object]:
    output_dir = Path(output_dir)
    prompt_doc_path = Path(prompt_doc_path)
    return {
        "task_version": 1,
        "prompt_mode": prompt_mode,
        "prompt_version": prompt_version,
        "generated_at": generated_at,
        "language": language_code,
        "language_tag": display_language_tag(language_code),
        "text_type": text_type or "",
        "prompt_text_type": prompt_text_type,
        "group": group or "",
        "prompt_ref": str(prompt_doc_path.relative_to(output_dir)),
        "entry_count": len(entries),
        "entries": [
            {
                "dir": entry.dir_name,
                "xml": entry.xml,
                "name": entry.name,
                "sample_length": entry.sample_length,
                "context": entry.context,
                "en": entry.english,
                "localized": entry.localized,
            }
            for entry in entries
        ],
    }


def write_agent_task_payload(path: Path | str, payload: dict[str, object]) -> str:
    output_path = Path(path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return str(output_path)


def build_agent_task_entry(
    *,
    folder_name: str,
    resource_file: ResourceFile,
    key: str,
    english_text: str,
    localized_text: str,
) -> AgentTaskEntry:
    return AgentTaskEntry(
        dir_name=folder_name,
        xml=f"{folder_name}/{resource_file.filename}",
        name=key,
        sample_length=resource_file.sample_lengths.get(key),
        context=resource_file.contexts.get(key),
        english=english_text,
        localized=localized_text,
    )
