from __future__ import annotations

import json
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path


@dataclass(frozen=True)
class MixedLanguageDetectionPolicy:
    cjk_language_prefixes: tuple[str, ...]
    mixed_language_skip_codes: frozenset[str]
    shared_locked_terms: tuple[str, ...]
    global_allowed_latin_words: frozenset[str]
    per_language_allowed_latin_words: dict[str, frozenset[str]]


@dataclass(frozen=True)
class MixedLanguageAuditPolicy:
    keep_en_exact: frozenset[str]
    per_language_keep_exact: dict[str, frozenset[str]]
    keep_en_context_hints: tuple[str, ...]
    needs_translation_context_hints: tuple[str, ...]


@dataclass(frozen=True)
class MixedLanguagePolicy:
    detection: MixedLanguageDetectionPolicy
    audit: MixedLanguageAuditPolicy


POLICY_PATH = Path(__file__).resolve().parents[1] / "policies" / "mixed_language_policy.json"


@lru_cache(maxsize=1)
def load_mixed_language_policy() -> MixedLanguagePolicy:
    payload = json.loads(POLICY_PATH.read_text(encoding="utf-8"))
    detection_payload = payload["detection"]
    audit_payload = payload["audit"]

    detection = MixedLanguageDetectionPolicy(
        cjk_language_prefixes=tuple(payload["cjk_language_prefixes"]),
        mixed_language_skip_codes=frozenset(payload["mixed_language_skip_codes"]),
        shared_locked_terms=tuple(payload["shared_locked_terms"]),
        global_allowed_latin_words=frozenset(
            item.lower() for item in detection_payload["global_allowed_latin_words"]
        ),
        per_language_allowed_latin_words={
            lang_code: frozenset(item.lower() for item in words)
            for lang_code, words in detection_payload["per_language_allowed_latin_words"].items()
        },
    )
    audit = MixedLanguageAuditPolicy(
        keep_en_exact=frozenset(item.lower() for item in audit_payload["keep_en_exact"]),
        per_language_keep_exact={
            lang_code: frozenset(item.lower() for item in words)
            for lang_code, words in audit_payload.get("per_language_keep_exact", {}).items()
        },
        keep_en_context_hints=tuple(item.lower() for item in audit_payload["keep_en_context_hints"]),
        needs_translation_context_hints=tuple(
            item.lower() for item in audit_payload["needs_translation_context_hints"]
        ),
    )
    return MixedLanguagePolicy(detection=detection, audit=audit)
