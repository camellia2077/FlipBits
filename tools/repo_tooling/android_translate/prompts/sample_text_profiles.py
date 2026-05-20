from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from core.mixed_language_policy import load_mixed_language_policy


@dataclass(frozen=True)
class SampleTextStyleProfile:
    profile_id: str
    locale_code: str
    group: str
    prompt_ref: str
    prompt_text: str


_PROFILE_ROOT = Path(__file__).resolve().parent / "sample_text_profiles"
_REPO_ROOT = Path(__file__).resolve().parents[5]
_KO_SAMPLE_GROUP_PROFILES: dict[str, tuple[str, str]] = {
    "labyrinth_of_mutability": (
        "sample_text.ko.labyrinth_of_mutability.v1",
        "values-ko/labyrinth_of_mutability.md",
    ),
    "immortal_rot": (
        "sample_text.ko.immortal_rot.v1",
        "values-ko/immortal_rot.md",
    ),
    "exquisite_fall": (
        "sample_text.ko.exquisite_fall.v1",
        "values-ko/exquisite_fall.md",
    ),
    "ancient_dynasty": (
        "sample_text.ko.ancient_dynasty.v1",
        "values-ko/ancient_dynasty.md",
    ),
    "sacred_machine": (
        "sample_text.ko.sacred_machine.v1",
        "values-ko/sacred_machine.md",
    ),
    "scarlet_carnage": (
        "sample_text.ko.scarlet_carnage.v1",
        "values-ko/scarlet_carnage.md",
    ),
}
_ZH_RTW_SAMPLE_GROUP_PROFILES: dict[str, tuple[str, str]] = {
    "ancient_dynasty": (
        "sample_text.zh-rTW.ancient_dynasty.v1",
        "values-zh-rTW/ancient_dynasty.md",
    ),
    "exquisite_fall": (
        "sample_text.zh-rTW.exquisite_fall.v1",
        "values-zh-rTW/exquisite_fall.md",
    ),
    "immortal_rot": (
        "sample_text.zh-rTW.immortal_rot.v1",
        "values-zh-rTW/immortal_rot.md",
    ),
    "labyrinth_of_mutability": (
        "sample_text.zh-rTW.labyrinth_of_mutability.v1",
        "values-zh-rTW/labyrinth_of_mutability.md",
    ),
    "sacred_machine": (
        "sample_text.zh-rTW.sacred_machine.v1",
        "values-zh-rTW/sacred_machine.md",
    ),
    "scarlet_carnage": (
        "sample_text.zh-rTW.scarlet_carnage.v1",
        "values-zh-rTW/scarlet_carnage.md",
    ),
}
_UK_SAMPLE_GROUP_PROFILES: dict[str, tuple[str, str]] = {
    "ancient_dynasty": (
        "sample_text.uk.ancient_dynasty.v1",
        "values-uk/ancient_dynasty.md",
    ),
    "exquisite_fall": (
        "sample_text.uk.exquisite_fall.v1",
        "values-uk/exquisite_fall.md",
    ),
    "immortal_rot": (
        "sample_text.uk.immortal_rot.v1",
        "values-uk/immortal_rot.md",
    ),
    "labyrinth_of_mutability": (
        "sample_text.uk.labyrinth_of_mutability.v1",
        "values-uk/labyrinth_of_mutability.md",
    ),
    "sacred_machine": (
        "sample_text.uk.sacred_machine.v1",
        "values-uk/sacred_machine.md",
    ),
    "scarlet_carnage": (
        "sample_text.uk.scarlet_carnage.v1",
        "values-uk/scarlet_carnage.md",
    ),
}
_DE_SAMPLE_GROUP_PROFILES: dict[str, tuple[str, str]] = {
    "ancient_dynasty": (
        "sample_text.de.ancient_dynasty.v1",
        "values-de/ancient_dynasty.md",
    ),
    "exquisite_fall": (
        "sample_text.de.exquisite_fall.v1",
        "values-de/exquisite_fall.md",
    ),
    "immortal_rot": (
        "sample_text.de.immortal_rot.v1",
        "values-de/immortal_rot.md",
    ),
    "labyrinth_of_mutability": (
        "sample_text.de.labyrinth_of_mutability.v1",
        "values-de/labyrinth_of_mutability.md",
    ),
    "sacred_machine": (
        "sample_text.de.sacred_machine.v1",
        "values-de/sacred_machine.md",
    ),
    "scarlet_carnage": (
        "sample_text.de.scarlet_carnage.v1",
        "values-de/scarlet_carnage.md",
    ),
}
_LA_SAMPLE_GROUP_PROFILES: dict[str, tuple[str, str]] = {
    "ancient_dynasty": (
        "sample_text.la.ancient_dynasty.v1",
        "values-la/ancient_dynasty.md",
    ),
    "exquisite_fall": (
        "sample_text.la.exquisite_fall.v1",
        "values-la/exquisite_fall.md",
    ),
    "immortal_rot": (
        "sample_text.la.immortal_rot.v1",
        "values-la/immortal_rot.md",
    ),
    "labyrinth_of_mutability": (
        "sample_text.la.labyrinth_of_mutability.v1",
        "values-la/labyrinth_of_mutability.md",
    ),
    "sacred_machine": (
        "sample_text.la.sacred_machine.v1",
        "values-la/sacred_machine.md",
    ),
    "scarlet_carnage": (
        "sample_text.la.scarlet_carnage.v1",
        "values-la/scarlet_carnage.md",
    ),
}
_ES_SAMPLE_GROUP_PROFILES: dict[str, tuple[str, str]] = {
    "ancient_dynasty": (
        "sample_text.es.ancient_dynasty.v1",
        "values-es/ancient_dynasty.md",
    ),
    "exquisite_fall": (
        "sample_text.es.exquisite_fall.v1",
        "values-es/exquisite_fall.md",
    ),
    "immortal_rot": (
        "sample_text.es.immortal_rot.v1",
        "values-es/immortal_rot.md",
    ),
    "labyrinth_of_mutability": (
        "sample_text.es.labyrinth_of_mutability.v1",
        "values-es/labyrinth_of_mutability.md",
    ),
    "sacred_machine": (
        "sample_text.es.sacred_machine.v1",
        "values-es/sacred_machine.md",
    ),
    "scarlet_carnage": (
        "sample_text.es.scarlet_carnage.v1",
        "values-es/scarlet_carnage.md",
    ),
}
_FR_SAMPLE_GROUP_PROFILES: dict[str, tuple[str, str]] = {
    "ancient_dynasty": (
        "sample_text.fr.ancient_dynasty.v1",
        "values-fr/ancient_dynasty.md",
    ),
    "exquisite_fall": (
        "sample_text.fr.exquisite_fall.v1",
        "values-fr/exquisite_fall.md",
    ),
    "immortal_rot": (
        "sample_text.fr.immortal_rot.v1",
        "values-fr/immortal_rot.md",
    ),
    "labyrinth_of_mutability": (
        "sample_text.fr.labyrinth_of_mutability.v1",
        "values-fr/labyrinth_of_mutability.md",
    ),
    "sacred_machine": (
        "sample_text.fr.sacred_machine.v1",
        "values-fr/sacred_machine.md",
    ),
    "scarlet_carnage": (
        "sample_text.fr.scarlet_carnage.v1",
        "values-fr/scarlet_carnage.md",
    ),
}
_IT_SAMPLE_GROUP_PROFILES: dict[str, tuple[str, str]] = {
    "ancient_dynasty": (
        "sample_text.it.ancient_dynasty.v1",
        "values-it/ancient_dynasty.md",
    ),
    "exquisite_fall": (
        "sample_text.it.exquisite_fall.v1",
        "values-it/exquisite_fall.md",
    ),
    "immortal_rot": (
        "sample_text.it.immortal_rot.v1",
        "values-it/immortal_rot.md",
    ),
    "labyrinth_of_mutability": (
        "sample_text.it.labyrinth_of_mutability.v1",
        "values-it/labyrinth_of_mutability.md",
    ),
    "sacred_machine": (
        "sample_text.it.sacred_machine.v1",
        "values-it/sacred_machine.md",
    ),
    "scarlet_carnage": (
        "sample_text.it.scarlet_carnage.v1",
        "values-it/scarlet_carnage.md",
    ),
}
_JA_SAMPLE_GROUP_PROFILES: dict[str, tuple[str, str]] = {
    "ancient_dynasty": (
        "sample_text.ja.ancient_dynasty.v1",
        "values-ja/ancient_dynasty.md",
    ),
    "exquisite_fall": (
        "sample_text.ja.exquisite_fall.v1",
        "values-ja/exquisite_fall.md",
    ),
    "immortal_rot": (
        "sample_text.ja.immortal_rot.v1",
        "values-ja/immortal_rot.md",
    ),
    "labyrinth_of_mutability": (
        "sample_text.ja.labyrinth_of_mutability.v1",
        "values-ja/labyrinth_of_mutability.md",
    ),
    "sacred_machine": (
        "sample_text.ja.sacred_machine.v1",
        "values-ja/sacred_machine.md",
    ),
    "scarlet_carnage": (
        "sample_text.ja.scarlet_carnage.v1",
        "values-ja/scarlet_carnage.md",
    ),
}
_PL_SAMPLE_GROUP_PROFILES: dict[str, tuple[str, str]] = {
    "ancient_dynasty": (
        "sample_text.pl.ancient_dynasty.v1",
        "values-pl/ancient_dynasty.md",
    ),
    "exquisite_fall": (
        "sample_text.pl.exquisite_fall.v1",
        "values-pl/exquisite_fall.md",
    ),
    "immortal_rot": (
        "sample_text.pl.immortal_rot.v1",
        "values-pl/immortal_rot.md",
    ),
    "labyrinth_of_mutability": (
        "sample_text.pl.labyrinth_of_mutability.v1",
        "values-pl/labyrinth_of_mutability.md",
    ),
    "sacred_machine": (
        "sample_text.pl.sacred_machine.v1",
        "values-pl/sacred_machine.md",
    ),
    "scarlet_carnage": (
        "sample_text.pl.scarlet_carnage.v1",
        "values-pl/scarlet_carnage.md",
    ),
}
_PT_RBR_SAMPLE_GROUP_PROFILES: dict[str, tuple[str, str]] = {
    "ancient_dynasty": (
        "sample_text.pt-rBR.ancient_dynasty.v1",
        "values-pt-rBR/ancient_dynasty.md",
    ),
    "exquisite_fall": (
        "sample_text.pt-rBR.exquisite_fall.v1",
        "values-pt-rBR/exquisite_fall.md",
    ),
    "immortal_rot": (
        "sample_text.pt-rBR.immortal_rot.v1",
        "values-pt-rBR/immortal_rot.md",
    ),
    "labyrinth_of_mutability": (
        "sample_text.pt-rBR.labyrinth_of_mutability.v1",
        "values-pt-rBR/labyrinth_of_mutability.md",
    ),
    "sacred_machine": (
        "sample_text.pt-rBR.sacred_machine.v1",
        "values-pt-rBR/sacred_machine.md",
    ),
    "scarlet_carnage": (
        "sample_text.pt-rBR.scarlet_carnage.v1",
        "values-pt-rBR/scarlet_carnage.md",
    ),
}


def _build_shared_sample_text_prefix() -> str:
    locked_terms = ", ".join(load_mixed_language_policy().detection.shared_locked_terms)
    return (
        "Shared locked-term policy for sample text:\n"
        f"- Keep locked product/protocol terms exactly as English tokens when they appear: {locked_terms}."
    )


def get_sample_text_style_profile(
    *,
    locale_code: str,
    text_type: str,
    group: str,
) -> SampleTextStyleProfile | None:
    if text_type != "sample_text":
        return None

    normalized_locale = (locale_code or "").strip()
    normalized_group = (group or "").strip()
    if normalized_locale == "ko":
        config = _KO_SAMPLE_GROUP_PROFILES.get(normalized_group)
        global_profile_path = _PROFILE_ROOT / "values-ko" / "_global.md"
    elif normalized_locale == "zh-rTW":
        config = _ZH_RTW_SAMPLE_GROUP_PROFILES.get(normalized_group)
        global_profile_path = _PROFILE_ROOT / "values-zh-rTW" / "_global.md"
    elif normalized_locale == "uk":
        config = _UK_SAMPLE_GROUP_PROFILES.get(normalized_group)
        global_profile_path = _PROFILE_ROOT / "values-uk" / "_global.md"
    elif normalized_locale == "de":
        config = _DE_SAMPLE_GROUP_PROFILES.get(normalized_group)
        global_profile_path = _PROFILE_ROOT / "values-de" / "_global.md"
    elif normalized_locale == "la":
        config = _LA_SAMPLE_GROUP_PROFILES.get(normalized_group)
        global_profile_path = _PROFILE_ROOT / "values-la" / "_global.md"
    elif normalized_locale == "es":
        config = _ES_SAMPLE_GROUP_PROFILES.get(normalized_group)
        global_profile_path = _PROFILE_ROOT / "values-es" / "_global.md"
    elif normalized_locale == "fr":
        config = _FR_SAMPLE_GROUP_PROFILES.get(normalized_group)
        global_profile_path = _PROFILE_ROOT / "values-fr" / "_global.md"
    elif normalized_locale == "it":
        config = _IT_SAMPLE_GROUP_PROFILES.get(normalized_group)
        global_profile_path = _PROFILE_ROOT / "values-it" / "_global.md"
    elif normalized_locale == "ja":
        config = _JA_SAMPLE_GROUP_PROFILES.get(normalized_group)
        global_profile_path = _PROFILE_ROOT / "values-ja" / "_global.md"
    elif normalized_locale == "pl":
        config = _PL_SAMPLE_GROUP_PROFILES.get(normalized_group)
        global_profile_path = _PROFILE_ROOT / "values-pl" / "_global.md"
    elif normalized_locale == "pt-rBR":
        config = _PT_RBR_SAMPLE_GROUP_PROFILES.get(normalized_group)
        global_profile_path = _PROFILE_ROOT / "values-pt-rBR" / "_global.md"
    else:
        return None
    if config is None:
        return None
    profile_id, profile_file = config
    profile_path = _PROFILE_ROOT / profile_file
    shared_prompt = _build_shared_sample_text_prefix()
    global_prompt = global_profile_path.read_text(encoding="utf-8").strip()
    local_prompt = profile_path.read_text(encoding="utf-8").strip()
    prompt_text = f"{shared_prompt}\n\n---\n\n{global_prompt}\n\n---\n\n{local_prompt}"
    prompt_ref = profile_path.relative_to(_REPO_ROOT).as_posix()
    return SampleTextStyleProfile(
        profile_id=profile_id,
        locale_code=normalized_locale,
        group=normalized_group,
        prompt_ref=prompt_ref,
        prompt_text=prompt_text,
    )
