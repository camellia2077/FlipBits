from __future__ import annotations

from dataclasses import dataclass


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


DEFAULT_PROFILE = LocalePromptProfile(
    locale_code="default",
    profile_id="default_translation",
    mode="standard_translation",
    identity_rule=(
        "Treat this locale as a normal target-language localization. "
        "Prefer natural native phrasing over literal English structure, while keeping the broad source meaning intact. "
    ),
    app_text_rule=(
        "For app UI text, optimize for clarity, brevity, stable terminology, and immediate usability. "
        "Do not turn short product labels into literary prose. "
    ),
    sample_text_rule=(
        "For sample prose, preserve lineup tone and atmosphere, but still write as natural target-language text rather than calqued English. "
    ),
    key_alignment_rule=(
        "When filling missing entries, write normal target-language localization that matches the existing locale's tone and terminology. "
    ),
    locale_note=(
        "This locale uses the standard translation profile: natural target-language wording first, with no special pseudo-language register. "
    ),
)


HIGH_GOTHIC_LA_PROFILE = LocalePromptProfile(
    locale_code="la",
    profile_id="high_gothic_dog_latin",
    mode="stylized_dog_latin",
    identity_rule=(
        "This locale is High Gothic, not classical Latin and not a standard translation locale. "
        "Treat it as a deliberate Dog Latin / pseudo-Latin / Latinized-English register built for immersion, ritual atmosphere, liturgical religious texture, and classical authority. "
        "A controlled mixture of Latin, ecclesiastical-sounding diction, invented pseudo-Latin phrasing, and selective English carry-over is acceptable when it strengthens the setting voice. "
    ),
    app_text_rule=(
        "For app UI text, keep labels usable and recognizable, but still favor elevated, ceremonial, archaic wording over plain utilitarian phrasing. "
        "Do not force strict classical correctness if it weakens product clarity or the High Gothic voice. "
    ),
    sample_text_rule=(
        "For sample prose, atmosphere outranks literal fidelity. "
        "Prefer ornate vocabulary, denser phrasing, longer cadence, subordinate clauses, and a solemn liturgical register when that strengthens immersion. "
        "Do not flatten the writing into simple textbook Latin or plain English paraphrase. "
        "The goal is convincing High Gothic mood, not academically correct Latin. "
    ),
    key_alignment_rule=(
        "When filling missing entries, write in the repository's High Gothic house style rather than attempting faithful classical Latin translation. "
        "Preserve atmosphere, ceremonial gravity, liturgical religious texture, and worldbuilding voice. "
        "Short UI labels may stay compact, but sample prose should prefer elevated diction and immersive cadence. "
    ),
    locale_note=(
        "High Gothic (`values-la`) is a stylized Dog Latin / pseudo-Latin locale. It intentionally prioritizes immersion, ritual atmosphere, liturgical religious texture, and classical authority over true Latin accuracy or literal translation fidelity. "
    ),
)


PROFILE_BY_LOCALE = {
    "la": HIGH_GOTHIC_LA_PROFILE,
}


def get_locale_prompt_profile(locale_code: str | None) -> LocalePromptProfile:
    if not locale_code:
        return DEFAULT_PROFILE
    normalized = locale_code.strip()
    return PROFILE_BY_LOCALE.get(normalized, DEFAULT_PROFILE)
