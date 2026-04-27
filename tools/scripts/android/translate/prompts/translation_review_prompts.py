from __future__ import annotations

PROMPT_VERSION = "v3"


RESOURCE_TEXT_CONTENT_RULE = (
    "When you propose replacement text, use plain natural-language wording inside the JSON string values. "
    "Do not add XML entities, Android resource escapes, or extra surrounding quotes; "
    "the replacement tool will handle resource escaping. "
)

JSON_OUTPUT_RULE = (
    "Return JSON only. Use this format: "
    '{"dir":"values-xx","items":[{"name":"string_name","find":"current substring","replace":"improved substring"}]}. '
    "Use DIR and NAME exactly as shown in the review block. "
    "Do not output xml. "
    "FIND must be an exact substring of the current line, and REPLACE should usually change only the smallest necessary span instead of rewriting the full paragraph. "
    "If no changes are needed, return {\"dir\":\"values-xx\",\"items\":[]} using the same DIR from the review block."
)

PRO_ASCII_EXCLUSION_RULE = (
    "Pro sample keys that contain '_ascii_' are fixed ASCII protocol samples. "
    "They are intentionally excluded from translation review and must not be translated, localized, or rewritten into non-ASCII text. "
)

CHANGE_THRESHOLD_RULE = (
    "Only propose a replacement when the current line has a clear problem. "
    "Clear problems include unnatural phrasing, obvious literal translation, grammar issues, awkward rhythm, meaning drift, incorrect SHORT/LONG length fit, "
    "or terminology that creates external-IP legal or branding risk. "
    "If the current line is already natural, fluent, and safe, do not rewrite it just to make it different. "
    "Do not make stylistic-preference edits, novelty rewrites, or synonym swaps without a clear quality gain. "
    "When unsure, keep the current text unchanged. "
)


def _build_agent_app_text_rule() -> str:
    return (
        "For app UI text, prioritize clarity, brevity, product consistency, and natural target-language UI phrasing. "
        "Prefer stable terminology across related labels, settings, buttons, and descriptions. "
        "Do not turn app UI text into creative prose. "
        "Do not add flourish, atmosphere, or extra explanation unless the current text clearly requires it. "
    )


def _build_agent_sample_text_rule() -> str:
    return (
        "For sample/example prose, prefer natural target-language rhythm, tone, imagery, and sentence flow over literal word-by-word mirroring. "
        "Creative adaptation is acceptable when it clearly improves fluency and preserves the broad meaning and lineup tone. "
        "Do not flatten vivid prose into plain UI-style wording. "
    )


def _build_manual_app_text_rule() -> str:
    return (
        "For app UI text, prefer compact product-style wording rather than explanatory phrases. "
        "Keep terminology stable across related labels, settings, and descriptions. "
        "Do not rewrite for style alone. "
    )


def _build_manual_sample_text_rule() -> str:
    return (
        "For sample/example prose, prefer natural language rhythm over literal word-by-word mirroring. "
        "Preserve atmosphere and lineup tone without adding external-IP terminology. "
    )


def build_english_source_review_prompt_for_text_type(text_type: str) -> str:
    common_prefix = (
        "Evaluate whether the English source lines are clear, natural, concise, and tonally consistent. "
        "Do not compare against a target language. Focus on source wording quality and style. "
    )
    if text_type == "sample_text":
        return (
            common_prefix
            + "For sample text, preserve the intended length class: SHORT samples should remain short, and LONG samples should remain long. "
            + PRO_ASCII_EXCLUSION_RULE
            + RESOURCE_TEXT_CONTENT_RULE
            + CHANGE_THRESHOLD_RULE
            + _build_agent_sample_text_rule()
            + "For stylized titles or proper nouns, preserve the original meaning first; do not accidentally replace one word with a different concept because it looks or sounds similar. "
            + "For user-facing audio, style, or settings terminology, prefer natural product language over clinical, implementation-oriented, or state-flag wording. "
            + JSON_OUTPUT_RULE
        )
    return (
        common_prefix
        + RESOURCE_TEXT_CONTENT_RULE
        + CHANGE_THRESHOLD_RULE
        + _build_agent_app_text_rule()
        + JSON_OUTPUT_RULE
    )


def build_translation_review_prompt_for_text_type(language_tag: str, text_type: str) -> str:
    common_prefix = (
        f"Evaluate whether the [{language_tag}] lines are natural and suitable for [{language_tag}]. "
        "Do not require strict word-for-word translation from English. "
        f"Prefer wording that fits [{language_tag}] sentence structure, grammar, rhythm, pronunciation, and idiom. "
        "Meaning only needs to be broadly consistent with English. "
    )
    if text_type == "sample_text":
        return (
            common_prefix
            + "Preserve lineup tone. "
            + "For sample text, preserve the intended length class: SHORT samples should remain short, and LONG samples should remain long. "
            + PRO_ASCII_EXCLUSION_RULE
            + RESOURCE_TEXT_CONTENT_RULE
            + CHANGE_THRESHOLD_RULE
            + _build_agent_sample_text_rule()
            + "For stylized titles or proper nouns, preserve the original meaning first; do not replace a title word with a different concept because it looks or sounds similar. "
            + "For user-facing audio, style, or settings terminology, prefer natural product language over clinical, implementation-oriented, or state-flag wording. "
            + JSON_OUTPUT_RULE
        )
    return (
        common_prefix
        + RESOURCE_TEXT_CONTENT_RULE
        + CHANGE_THRESHOLD_RULE
        + _build_agent_app_text_rule()
        + JSON_OUTPUT_RULE
    )


def build_manual_english_source_review_prompt_for_text_type(text_type: str) -> str:
    common_prefix = (
        "Evaluate whether the English source lines are clear, natural, concise, and tonally consistent. "
        "Do not compare against a target language. Focus on source wording quality and style. "
    )
    if text_type == "sample_text":
        return (
            common_prefix
            + "For sample text, preserve the intended length class: SHORT samples should remain short, and LONG samples should remain long. "
            + PRO_ASCII_EXCLUSION_RULE
            + CHANGE_THRESHOLD_RULE
            + "Return review notes in plain natural-language text. "
            + _build_manual_sample_text_rule()
            + "For stylized titles or proper nouns, preserve the original meaning first; do not accidentally replace one word with a different concept because it looks or sounds similar. "
        )
    return (
        common_prefix
        + CHANGE_THRESHOLD_RULE
        + "Return review notes in plain natural-language text. "
        + _build_manual_app_text_rule()
    )


def build_manual_translation_review_prompt_for_text_type(language_tag: str, text_type: str) -> str:
    common_prefix = (
        f"Evaluate whether the [{language_tag}] lines are natural and suitable for [{language_tag}]. "
        "Do not require strict word-for-word translation from English. "
        f"Prefer wording that fits [{language_tag}] sentence structure, grammar, rhythm, pronunciation, and idiom. "
        "Meaning only needs to be broadly consistent with English. "
    )
    if text_type == "sample_text":
        return (
            common_prefix
            + "Preserve lineup tone. "
            + "For sample text, preserve the intended length class: SHORT samples should remain short, and LONG samples should remain long. "
            + PRO_ASCII_EXCLUSION_RULE
            + CHANGE_THRESHOLD_RULE
            + "Return review notes in plain natural-language text. "
            + _build_manual_sample_text_rule()
            + "For stylized titles or proper nouns, preserve the original meaning first; do not replace a title word with a different concept because it looks or sounds similar. "
        )
    return (
        common_prefix
        + CHANGE_THRESHOLD_RULE
        + "Return review notes in plain natural-language text. "
        + _build_manual_app_text_rule()
    )


def build_english_source_review_prompt() -> str:
    return build_english_source_review_prompt_for_text_type("sample_text")


def build_translation_review_prompt(language_tag: str) -> str:
    return build_translation_review_prompt_for_text_type(language_tag, "sample_text")


def build_manual_english_source_review_prompt() -> str:
    return build_manual_english_source_review_prompt_for_text_type("sample_text")


def build_manual_translation_review_prompt(language_tag: str) -> str:
    return build_manual_translation_review_prompt_for_text_type(language_tag, "sample_text")


def get_supported_prompt_modes() -> tuple[str, ...]:
    return ("agent_json", "manual_notes")


def build_key_alignment_repair_prompt(language_tag: str) -> str:
    return (
        f"Use this report to repair missing or misaligned [{language_tag}] Android string resources. "
        "Treat EN as the only source of meaning, and use CONTEXT when provided to understand UI role, tone, and constraints. "
        f"For each 'missing localized translation for English base key' entry, add a localized string for [{language_tag}] under the shown DIR/FILE using the exact KEY name. "
        "For each 'localized file is missing for English base counterpart' entry, create the missing localized file and add the English-base keys listed there, using English only as semantic source text. "
        "For 'localized key exists but English base key is missing' and 'localized file has no English base counterpart', treat them as structural drift findings to inspect; do not delete or rewrite anything unless explicitly asked. "
        "Do not add, translate, or rewrite Pro sample keys that contain '_ascii_'; those fixed ASCII protocol samples are intentionally outside this report. "
        "Preserve placeholders such as %1$s, preserve product terms when needed, and keep terminology consistent across related settings labels. "
        "Do not modify English resources. Do not infer a different target file or key than the DIR/FILE/KEY shown in the report."
    )
