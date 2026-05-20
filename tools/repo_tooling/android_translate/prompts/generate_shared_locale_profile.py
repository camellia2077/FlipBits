from __future__ import annotations

from pathlib import Path

from core.mixed_language_policy import load_mixed_language_policy


def build_shared_locale_profile_text() -> str:
    policy = load_mixed_language_policy()
    locked_terms = ", ".join(policy.detection.shared_locked_terms)
    lines = [
        "# Shared Android Translation Locale Constraints",
        "",
        "[profile_id]",
        "shared_constraints",
        "",
        "[mode]",
        "shared_rules",
        "",
        "[locale_note]",
        "Shared constraints that apply to every locale profile unless explicitly overridden.",
        "",
        "[identity_rule]",
        f"Never translate these product/protocol terms; preserve exact English spelling and casing: {locked_terms}.",
        "Apply this locked-term rule automatically; locale-specific profiles should only add exceptions or language-specific phrasing guidance when truly necessary.",
        "",
        "[app_text_rule]",
        f"Keep the non-translatable term list unchanged in UI labels, hints, validation text, and settings descriptions: {locked_terms}.",
        "",
        "[sample_text_rule]",
        f"When sample text contains locked protocol/product terms, keep them exactly as English tokens: {locked_terms}.",
        "",
        "[key_alignment_rule]",
        f"If an English source string contains any locked term, localized output must retain the same English token form: {locked_terms}.",
        "",
    ]
    return "\n".join(lines)


def ensure_generated_shared_locale_profile(path: Path) -> None:
    rendered = build_shared_locale_profile_text()
    if path.exists():
        current = path.read_text(encoding="utf-8")
        if current == rendered:
            return
    path.write_text(rendered, encoding="utf-8")
