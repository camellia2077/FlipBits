from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re

from core.translation_agent_tasks import infer_length_pressure, infer_ui_surface
from core.translation_lint import run_translation_lint
from core.translation_paths import DEFAULT_RES_DIRECTORY, display_language_tag
from core.translation_resources import AndroidStringResourceRepository, ResourceFile


_LOCKED_TERM_PATTERN = re.compile(r"Locked term '(.+?)' appears in EN but not in localized text\.")


@dataclass(frozen=True)
class LockedTermSuggestionItem:
    lang: str
    file: str
    key: str
    missing_terms: tuple[str, ...]
    context: str | None
    ui_surface: str
    length_pressure: str
    english: str
    localized: str
    suggestion_strategy: str
    suggested_localized: str | None
    rationale: str


@dataclass(frozen=True)
class LockedTermSuggestionResult:
    exit_code: int
    language: str | None
    suggestion_count: int
    safe_replace_count: int
    suggestions: tuple[LockedTermSuggestionItem, ...]
    errors: tuple[str, ...]


def _extract_locked_terms(message: str) -> tuple[str, ...]:
    match = _LOCKED_TERM_PATTERN.search(message)
    if not match:
        return ()
    return (match.group(1),)


def _choose_strategy(
    *,
    key: str,
    english_text: str,
    localized_text: str,
    missing_terms: tuple[str, ...],
    context: str | None,
    ui_surface: str,
) -> tuple[str, str | None, str]:
    context_lower = (context or "").lower()
    stripped_en = english_text.strip()

    if not missing_terms:
        return ("review_and_preserve_locked_terms", None, "Could not parse missing locked term from lint message.")

    if len(missing_terms) == 1 and stripped_en == missing_terms[0]:
        return (
            "replace_full_with_locked_term",
            english_text,
            "The English source is itself the locked term, so the safest fix is to keep that exact term.",
        )

    if ui_surface == "audio/playback_follow" and stripped_en in {"Binary", "Hex", "Morse", "Tokens", "Mix"}:
        return (
            "replace_full_with_english_label",
            english_text,
            "This is a short protocol/view-mode label and should preserve the English product/protocol term exactly.",
        )

    if key.startswith("demo_hint_") or ui_surface == "audio/demo_hint":
        return (
            "revise_sentence_keep_locked_term",
            None,
            "This is a short demo hint sentence. Keep the locked term, but revise the surrounding localized wording manually.",
        )

    if ui_surface == "audio/transport" or "shown under the input field" in context_lower:
        return (
            "revise_sentence_keep_locked_term",
            None,
            "This helper text should stay localized, but the locked term(s) must remain in English.",
        )

    if localized_text.strip() == english_text.strip():
        return (
            "replace_full_with_english_source",
            english_text,
            "The localized text is still identical to English; preserving the source is the safest locked-term fix here.",
        )

    return (
        "review_and_preserve_locked_terms",
        None,
        "Preserve the missing locked term(s), but review the full sentence manually before applying a replacement.",
    )


def suggest_locked_term_fixes(
    *,
    res_dir: str | Path = DEFAULT_RES_DIRECTORY,
    lang: str | None = None,
) -> LockedTermSuggestionResult:
    repository = AndroidStringResourceRepository(res_dir)
    repository.ensure_base_directory()
    base_files = repository.load_base_resource_files()
    lint_result = run_translation_lint(res_dir=str(res_dir), lang=lang)

    if lang:
        localized_dirs = [(code, path) for code, path in repository.iter_localized_directories() if code == lang]
        if not localized_dirs:
            return LockedTermSuggestionResult(
                exit_code=2,
                language=lang,
                suggestion_count=0,
                safe_replace_count=0,
                suggestions=(),
                errors=(f"localized language folder not found: {lang}",),
            )
    else:
        localized_dirs = repository.iter_localized_directories()

    localized_dir_map = {code: path for code, path in localized_dirs}
    localized_cache: dict[tuple[str, str], dict[str, str]] = {}
    for lang_code, folder_path in localized_dirs:
        for filename in base_files:
            file_path = folder_path / filename
            if file_path.exists():
                localized_cache[(lang_code, filename)] = repository.load_localized_strings(folder_path, filename)

    suggestions: list[LockedTermSuggestionItem] = []
    safe_replace_count = 0

    for issue in lint_result.issues:
        if issue.rule != "locked_term_missing":
            continue
        issue_path = Path(issue.file)
        lang_code = issue_path.parent.name.removeprefix("values-")
        if lang and lang_code != lang:
            continue
        filename = issue_path.name
        resource_file = base_files.get(filename)
        if resource_file is None:
            continue
        english_text = resource_file.strings.get(issue.key)
        if english_text is None:
            continue
        localized_text = localized_cache.get((lang_code, filename), {}).get(issue.key)
        if localized_text is None:
            continue
        context = resource_file.contexts.get(issue.key)
        ui_surface = infer_ui_surface(resource_file=resource_file, key=issue.key, context=context)
        length_pressure = infer_length_pressure(
            sample_length=resource_file.sample_lengths.get(issue.key),
            key=issue.key,
            context=context,
        )
        missing_terms = _extract_locked_terms(issue.message)
        strategy, suggested_localized, rationale = _choose_strategy(
            key=issue.key,
            english_text=english_text,
            localized_text=localized_text,
            missing_terms=missing_terms,
            context=context,
            ui_surface=ui_surface,
        )
        if suggested_localized is not None:
            safe_replace_count += 1
        suggestions.append(
            LockedTermSuggestionItem(
                lang=lang_code,
                file=f"values-{lang_code}/{filename}",
                key=issue.key,
                missing_terms=missing_terms,
                context=context,
                ui_surface=ui_surface,
                length_pressure=length_pressure,
                english=english_text,
                localized=localized_text,
                suggestion_strategy=strategy,
                suggested_localized=suggested_localized,
                rationale=rationale,
            )
        )

    suggestions.sort(key=lambda item: (item.lang, item.file, item.key))
    return LockedTermSuggestionResult(
        exit_code=0,
        language=lang,
        suggestion_count=len(suggestions),
        safe_replace_count=safe_replace_count,
        suggestions=tuple(suggestions),
        errors=(),
    )


def build_locked_term_suggestion_payload(result: LockedTermSuggestionResult) -> dict[str, object]:
    return {
        "ok": result.exit_code == 0,
        "command": "locked-term-suggestions",
        "exit_code": result.exit_code,
        "summary": {
            "language": result.language,
            "language_tag": display_language_tag(result.language) if result.language else None,
            "suggestion_count": result.suggestion_count,
            "safe_replace_count": result.safe_replace_count,
        },
        "suggestions": [
            {
                "lang": item.lang,
                "language_tag": display_language_tag(item.lang),
                "file": item.file,
                "key": item.key,
                "missing_terms": list(item.missing_terms),
                "context": item.context,
                "ui_surface": item.ui_surface,
                "length_pressure": item.length_pressure,
                "en": item.english,
                "localized": item.localized,
                "suggestion_strategy": item.suggestion_strategy,
                "suggested_localized": item.suggested_localized,
                "rationale": item.rationale,
            }
            for item in result.suggestions
        ],
        "errors": list(result.errors),
    }
