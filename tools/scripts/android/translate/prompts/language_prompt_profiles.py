from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class LocalePromptProfile:
    locale_code: str
    profile_id: str
    mode: str
    identity_rule: str
    app_text_rule: str
    sample_text_rule: str
    key_alignment_rule: str
    locale_note: str


PROFILE_DIRECTORY = Path(__file__).resolve().parent / "locales"
PROFILE_FIELDS = {
    "profile_id",
    "mode",
    "locale_note",
    "identity_rule",
    "app_text_rule",
    "sample_text_rule",
    "key_alignment_rule",
}


def _parse_profile_file(path: Path) -> dict[str, str]:
    values: dict[str, list[str]] = {}
    current_key: str | None = None

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.rstrip()
        if not line or line.lstrip().startswith("#"):
            continue
        if line.startswith("[") and line.endswith("]"):
            key = line[1:-1].strip()
            if key not in PROFILE_FIELDS:
                raise ValueError(f"Unknown locale prompt profile field '{key}' in {path}")
            current_key = key
            values.setdefault(current_key, [])
            continue
        if current_key is None:
            raise ValueError(f"Locale prompt profile text appears before a field header in {path}: {line}")
        values[current_key].append(line.strip())

    parsed = {}
    for key, parts in values.items():
        value = " ".join(parts).strip()
        parsed[key] = value if key in {"profile_id", "mode"} else _ensure_sentence_gap(value)
    missing = sorted(PROFILE_FIELDS.difference(parsed))
    if missing:
        raise ValueError(f"Locale prompt profile {path} is missing fields: {', '.join(missing)}")
    return parsed


def _ensure_sentence_gap(value: str) -> str:
    if not value:
        return value
    return value if value.endswith(" ") else f"{value} "


def _load_profile(locale_code: str) -> LocalePromptProfile:
    path = PROFILE_DIRECTORY / f"{locale_code}.md"
    parsed = _parse_profile_file(path)
    return LocalePromptProfile(
        locale_code=locale_code,
        profile_id=parsed["profile_id"],
        mode=parsed["mode"],
        identity_rule=parsed["identity_rule"],
        app_text_rule=parsed["app_text_rule"],
        sample_text_rule=parsed["sample_text_rule"],
        key_alignment_rule=parsed["key_alignment_rule"],
        locale_note=parsed["locale_note"],
    )


def get_locale_prompt_profile(locale_code: str | None) -> LocalePromptProfile:
    normalized = locale_code.strip() if locale_code else "default"
    path = PROFILE_DIRECTORY / f"{normalized}.md"
    if not path.exists():
        normalized = "default"
    return _load_profile(normalized)
