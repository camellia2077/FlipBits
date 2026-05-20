from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

from core.translation_paths import display_language_tag
from core.translation_resources import ResourceFile
from prompts.language_prompt_profiles import LocalePromptProfile
from prompts.sample_text_profiles import SampleTextStyleProfile


@dataclass(frozen=True)
class AgentTaskEntry:
    dir_name: str
    xml: str
    name: str
    sample_length: str | None
    context: str | None
    decision_hint: str
    ui_surface: str
    length_pressure: str
    english: str
    localized: str


_MISSING_TRANSLATION_MARKER = "[MISSING TRANSLATION / 此条目未翻译]"


def infer_decision_hint(*, context: str | None, localized_text: str) -> str:
    context_lower = (context or "").lower()
    if localized_text == _MISSING_TRANSLATION_MARKER:
        return "translate_missing"
    if "must stay strict ascii" in context_lower or "must remain plain ascii" in context_lower:
        return "keep_en"
    if (
        "should stay in english" in context_lower
        or "preserve the english product term" in context_lower
        or "keep the literal format unchanged" in context_lower
        or "brand should stay fixed" in context_lower
        or "should remain lowercase" in context_lower
    ):
        return "keep_en"
    return "review_translation"


def infer_ui_surface(*, resource_file: ResourceFile, key: str, context: str | None) -> str:
    context_lower = (context or "").lower()
    filename = resource_file.filename

    if filename == "strings_settings.xml":
        if key.startswith("config_language_"):
            return "settings/language_picker"
        if key.startswith("brand_theme_") or key.startswith("config_dual_tone_group_"):
            return "settings/theme_picker"
        if key.startswith("palette_"):
            return "settings/palette_picker"
        if key.startswith("config_flash_style_"):
            return "settings/flash_style"
        return "settings/general"

    if filename == "strings_audio.xml":
        if key.startswith("demo_hint_"):
            return "audio/demo_hint"
        if key.startswith("audio_transport_") or key.startswith("transport_mode_"):
            return "audio/transport"
        if key.startswith("audio_input_encoding_") or key.startswith("audio_morse_"):
            return "audio/input_rules"
        if key.startswith("audio_decode_"):
            return "audio/decode"
        if key.startswith("audio_follow_") or key.startswith("audio_playback_view_"):
            return "audio/playback_follow"
        if key.startswith("audio_info_"):
            return "audio/info_panel"
        if key.startswith("status_") or key.startswith("snackbar_"):
            return "audio/status"
        return "audio/general"

    if filename == "strings_validation.xml":
        return "validation/message"
    if filename.startswith("audio_samples_"):
        return f"sample_text/{resource_file.faction}"
    if "app language list" in context_lower:
        return "settings/language_picker"
    return f"{resource_file.text_type}/{resource_file.faction}"


def infer_length_pressure(*, sample_length: str | None, key: str, context: str | None) -> str:
    context_lower = (context or "").lower()
    if sample_length == "SHORT":
        return "tight"
    if sample_length == "LONG":
        return "relaxed"
    if any(
        token in context_lower
        for token in ("keep it short", "keep it concise", "button", "chip", "tab label", "segmented", "compact")
    ):
        return "tight"
    if any(
        token in context_lower
        for token in ("body text", "helper text", "description", "validation message", "status text", "subtitle")
    ):
        return "normal"
    if key.endswith(("_title", "_label")):
        return "tight"
    return "normal"


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
    locale_profile: LocalePromptProfile,
    style_profile: SampleTextStyleProfile | None,
    entries: list[AgentTaskEntry],
) -> dict[str, object]:
    output_dir = Path(output_dir)
    prompt_doc_path = Path(prompt_doc_path)
    payload = {
        "task_version": 3,
        "prompt_mode": prompt_mode,
        "prompt_version": prompt_version,
        "generated_at": generated_at,
        "language": language_code,
        "language_tag": display_language_tag(language_code),
        "locale_profile": {
            "id": locale_profile.profile_id,
            "mode": locale_profile.mode,
            "note": locale_profile.locale_note,
            "identity_rule": locale_profile.identity_rule,
            "app_text_rule": locale_profile.app_text_rule,
            "sample_text_rule": locale_profile.sample_text_rule,
            "key_alignment_rule": locale_profile.key_alignment_rule,
        },
        "execution_contract": {
            "json_first": True,
            "markdown_optional": True,
            "primary_task_fields": (
                "decision_hint",
                "ui_surface",
                "length_pressure",
                "context",
                "locale_profile",
                "style_profile",
            ),
        },
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
                "decision_hint": entry.decision_hint,
                "ui_surface": entry.ui_surface,
                "length_pressure": entry.length_pressure,
                "en": entry.english,
                "localized": entry.localized,
            }
            for entry in entries
        ],
    }
    if style_profile is not None:
        payload["style_profile"] = {
            "id": style_profile.profile_id,
            "locale": style_profile.locale_code,
            "group": style_profile.group,
            "prompt_ref": style_profile.prompt_ref,
            "prompt_text": style_profile.prompt_text,
        }
    return payload


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
        decision_hint=infer_decision_hint(
            context=resource_file.contexts.get(key),
            localized_text=localized_text,
        ),
        ui_surface=infer_ui_surface(
            resource_file=resource_file,
            key=key,
            context=resource_file.contexts.get(key),
        ),
        length_pressure=infer_length_pressure(
            sample_length=resource_file.sample_lengths.get(key),
            key=key,
            context=resource_file.contexts.get(key),
        ),
        english=english_text,
        localized=localized_text,
    )
