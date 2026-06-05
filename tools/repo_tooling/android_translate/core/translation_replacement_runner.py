from __future__ import annotations

import difflib
from dataclasses import dataclass
from pathlib import Path
import sys
from typing import Iterable

from core.android_resource_smoke_check import run_android_resource_smoke_check_with_options
from core.replacement_json_preflight import load_replacement_json_with_preflight
from core.replacement_entries import ReplacementEntry, load_replacement_entries
from core.translation_paths import DEFAULT_RES_DIRECTORY
from core.android_resource_escapes import run_fix_android_resource_escapes
from core.ko_resource_guardrails import autofix_ko_xml_text, validate_ko_values_xml_files
from core.xml_string_replacement import (
    AppliedReplacement,
    apply_replacement_in_string,
    load_localized_directory_index,
    resolve_string_name_path,
)

DEFAULT_JSON_PATH = Path(__file__).resolve().parents[1] / "replacements.json"
ANSI_RED = "\033[31m"
ANSI_GREEN = "\033[32m"
ANSI_RESET = "\033[0m"


def configure_console_output() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if callable(reconfigure):
            reconfigure(encoding="utf-8", errors="replace")


@dataclass(frozen=True)
class ReplaceCommandResult:
    exit_code: int
    json_path: str | None
    dir_name: str | None
    applied_replacements: int
    already_applied_count: int
    skipped_unchanged_count: int
    failed_not_found_count: int
    failed_ambiguous_count: int
    failed_validation_count: int
    validation_error_count: int
    write_completed: bool
    write_ok: bool
    smoke_check_ran: bool
    smoke_check_ok: bool
    errors: tuple[str, ...]
    touched_files: tuple[str, ...] = ()
    applied_targets: tuple[str, ...] = ()
    auto_fix_applied: bool = False
    auto_fix_summary: str | None = None


@dataclass(frozen=True)
class ReplaceBatchCommandResult:
    exit_code: int
    file_count: int
    processed_count: int
    success_count: int
    failure_count: int
    write_completed_count: int
    write_ok_count: int
    smoke_check_requested: bool
    smoke_check_ran: bool
    smoke_check_ok: bool
    results: tuple[ReplaceCommandResult, ...]
    errors: tuple[str, ...] = ()


SENTENCE_BREAK_CHARS = ".!?。！？;；\n"

def build_colored_character_diff(original_text: str, updated_text: str) -> str:
    parts: list[str] = []
    matcher = difflib.SequenceMatcher(a=original_text, b=updated_text)
    for opcode, a_start, a_end, b_start, b_end in matcher.get_opcodes():
        if opcode == "equal":
            parts.append(original_text[a_start:a_end])
        elif opcode == "delete":
            parts.append(f"{ANSI_RED}{original_text[a_start:a_end]}{ANSI_RESET}")
        elif opcode == "insert":
            parts.append(f"{ANSI_GREEN}{updated_text[b_start:b_end]}{ANSI_RESET}")
        elif opcode == "replace":
            parts.append(f"{ANSI_RED}{original_text[a_start:a_end]}{ANSI_RESET}")
            parts.append(f"{ANSI_GREEN}{updated_text[b_start:b_end]}{ANSI_RESET}")
    return "".join(parts)


def print_applied_diffs(applied_replacements: list[AppliedReplacement]) -> None:
    if not applied_replacements:
        return

    print("Applied diffs:")
    for replacement in applied_replacements:
        old_excerpt, new_excerpt = build_changed_sentence_excerpt(
            replacement.original_text,
            replacement.updated_text,
        )
        print(f"{replacement.xml_path} | {replacement.string_name}")
        print(f"- OLD: {old_excerpt}")
        print(f"+ NEW: {new_excerpt}")
        print(f"  DIFF: {build_colored_character_diff(old_excerpt, new_excerpt)}")


def build_changed_sentence_excerpt(
    original_text: str,
    updated_text: str,
) -> tuple[str, str]:
    prefix_length = 0
    max_prefix = min(len(original_text), len(updated_text))
    while prefix_length < max_prefix and original_text[prefix_length] == updated_text[prefix_length]:
        prefix_length += 1

    original_suffix_length = len(original_text)
    updated_suffix_length = len(updated_text)
    while (
        original_suffix_length > prefix_length
        and updated_suffix_length > prefix_length
        and original_text[original_suffix_length - 1] == updated_text[updated_suffix_length - 1]
    ):
        original_suffix_length -= 1
        updated_suffix_length -= 1

    original_start, original_end = expand_to_sentence_boundary(
        original_text,
        change_start=prefix_length,
        change_end=original_suffix_length,
    )
    updated_start, updated_end = expand_to_sentence_boundary(
        updated_text,
        change_start=prefix_length,
        change_end=updated_suffix_length,
    )

    return (
        original_text[original_start:original_end].strip(),
        updated_text[updated_start:updated_end].strip(),
    )


def expand_to_sentence_boundary(
    text: str,
    *,
    change_start: int,
    change_end: int,
) -> tuple[int, int]:
    start = max(0, min(change_start, len(text)))
    end = max(start, min(change_end, len(text)))

    while start > 0 and text[start - 1] not in SENTENCE_BREAK_CHARS:
        start -= 1
    while start < len(text) and text[start].isspace():
        start += 1

    while end < len(text) and text[end] not in SENTENCE_BREAK_CHARS:
        end += 1
    if end < len(text):
        end += 1
    while end < len(text) and text[end].isspace():
        end += 1

    return start, end


def apply_translation_replacements(
    *,
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    json_path: Path | str = DEFAULT_JSON_PATH,
    run_smoke_check: bool = False,
    auto_fix_json: bool = False,
    quiet: bool = False,
    emit_text: bool = True,
) -> ReplaceCommandResult:
    configure_console_output()
    res_dir = Path(res_dir)
    json_path = Path(json_path)
    if not res_dir.exists():
        error = f"Error: res directory not found: {res_dir}"
        if emit_text:
            print(error, file=sys.stderr)
        return ReplaceCommandResult(
            exit_code=1,
            json_path=str(json_path),
            dir_name=None,
            applied_replacements=0,
            already_applied_count=0,
            skipped_unchanged_count=0,
            failed_not_found_count=0,
            failed_ambiguous_count=0,
            failed_validation_count=0,
            validation_error_count=0,
            write_completed=False,
            write_ok=False,
            smoke_check_ran=False,
            smoke_check_ok=False,
            errors=(error,),
        )
    if not json_path.exists():
        error = f"Error: replacement JSON not found: {json_path}"
        if emit_text:
            print(error, file=sys.stderr)
        return ReplaceCommandResult(
            exit_code=1,
            json_path=str(json_path),
            dir_name=None,
            applied_replacements=0,
            already_applied_count=0,
            skipped_unchanged_count=0,
            failed_not_found_count=0,
            failed_ambiguous_count=0,
            failed_validation_count=0,
            validation_error_count=0,
            write_completed=False,
            write_ok=False,
            smoke_check_ran=False,
            smoke_check_ok=False,
            errors=(error,),
        )

    preflight_result = load_replacement_json_with_preflight(json_path, auto_fix=auto_fix_json)
    if not preflight_result.ok:
        error = preflight_result.error or "Replacement JSON preflight failed."
        if emit_text:
            print(error, file=sys.stderr)
        return ReplaceCommandResult(
            exit_code=1,
            json_path=str(json_path),
            dir_name=None,
            applied_replacements=0,
            already_applied_count=0,
            skipped_unchanged_count=0,
            failed_not_found_count=0,
            failed_ambiguous_count=0,
            failed_validation_count=0,
            validation_error_count=0,
            write_completed=False,
            write_ok=False,
            smoke_check_ran=False,
            smoke_check_ok=False,
            errors=(error,),
            auto_fix_applied=False,
            auto_fix_summary=preflight_result.fix_summary,
        )
    if preflight_result.changed and preflight_result.fix_summary is not None and emit_text and not quiet:
        print(f"Replacement JSON auto-fix applied: {preflight_result.fix_summary}")

    try:
        replacement_batch = load_replacement_entries(preflight_result.repaired_text or preflight_result.raw_text)
    except ValueError as exc:
        error = str(exc)
        if emit_text:
            if not quiet:
                print("Validation errors:")
            print(error)
        return ReplaceCommandResult(
            exit_code=2,
            json_path=str(json_path),
            dir_name=None,
            applied_replacements=0,
            already_applied_count=0,
            skipped_unchanged_count=0,
            failed_not_found_count=0,
            failed_ambiguous_count=0,
            failed_validation_count=1,
            validation_error_count=1,
            write_completed=False,
            write_ok=False,
            smoke_check_ran=False,
            smoke_check_ok=True,
            errors=(error,),
            auto_fix_applied=preflight_result.changed,
            auto_fix_summary=preflight_result.fix_summary,
        )
    if not replacement_batch.items:
        if emit_text and not quiet:
            print("No replacement entries found.")
        return ReplaceCommandResult(
            exit_code=0,
            json_path=str(json_path),
            dir_name=replacement_batch.dir_name,
            applied_replacements=0,
            already_applied_count=0,
            skipped_unchanged_count=0,
            failed_not_found_count=0,
            failed_ambiguous_count=0,
            failed_validation_count=0,
            validation_error_count=0,
            write_completed=True,
            write_ok=True,
            smoke_check_ran=False,
            smoke_check_ok=True,
            errors=(),
            auto_fix_applied=preflight_result.changed,
            auto_fix_summary=preflight_result.fix_summary,
        )

    target_dir = res_dir / replacement_batch.dir_name
    if target_dir.exists() and target_dir.is_dir():
        if target_dir.name == "values-ko":
            ko_files = [path for path in sorted(target_dir.glob("*.xml")) if path.is_file()]
            for ko_file in ko_files:
                original = ko_file.read_text(encoding="utf-8")
                fixed, changed = autofix_ko_xml_text(original)
                if changed > 0:
                    ko_file.write_text(fixed, encoding="utf-8")
            guardrail_issues = validate_ko_values_xml_files(ko_files)
            if guardrail_issues:
                guidance = (
                    "KO resource guardrail check failed before XML replacement.\n"
                    "Fix guidance for agent:\n"
                    "1) Replace raw \\\" with &quot; inside string bodies.\n"
                    "2) Replace raw \\' with ' unless Android escaping explicitly requires otherwise.\n"
                    "3) Remove malformed \\u escapes; use plain Unicode characters directly.\n"
                    "4) Re-run: python tools/run.py android-translate fix-resource-escapes --res-dir apps/audio_android/app/src/main/res\n"
                )
                details = "\n".join(
                    f"- {issue.file}\n  reason: {issue.reason}\n  sample: {issue.sample}"
                    for issue in guardrail_issues
                )
                error = f"{guidance}\n{details}"
                if emit_text:
                    print(error)
                return ReplaceCommandResult(
                    exit_code=2,
                    json_path=str(json_path),
                    dir_name=replacement_batch.dir_name if 'replacement_batch' in locals() else "values-ko",
                    applied_replacements=0,
                    already_applied_count=0,
                    skipped_unchanged_count=0,
                    failed_not_found_count=0,
                    failed_ambiguous_count=0,
                    failed_validation_count=len(guardrail_issues),
                    validation_error_count=len(guardrail_issues),
                    write_completed=False,
                    write_ok=False,
                    smoke_check_ran=False,
                    smoke_check_ok=True,
                    errors=(error,),
                    auto_fix_applied=preflight_result.changed,
                    auto_fix_summary=preflight_result.fix_summary,
                )
        run_fix_android_resource_escapes(
            res_dir=res_dir,
            files=[str(path) for path in sorted(target_dir.glob("*.xml")) if path.is_file()],
            quiet=True,
            emit_text=False,
        )

    localized_dir, name_index, xml_text_cache, invalid_xml_errors = load_localized_directory_index(
        res_dir,
        replacement_batch.dir_name,
    )

    applied_total = 0
    already_applied_total = 0
    skipped_unchanged_total = 0
    failed_not_found_total = 0
    failed_ambiguous_total = 0
    applied_replacements: list[AppliedReplacement] = []
    all_errors: list[str] = list(invalid_xml_errors)

    if localized_dir is None:
        if emit_text and not quiet:
            print("Validation errors:")
        if emit_text:
            for error in all_errors:
                print(error)
        return ReplaceCommandResult(
            exit_code=2,
            json_path=str(json_path),
            dir_name=replacement_batch.dir_name,
            applied_replacements=0,
            already_applied_count=0,
            skipped_unchanged_count=0,
            failed_not_found_count=0,
            failed_ambiguous_count=0,
            failed_validation_count=len(all_errors),
            validation_error_count=len(all_errors),
            write_completed=False,
            write_ok=False,
            smoke_check_ran=False,
            smoke_check_ok=True,
            errors=tuple(all_errors),
            auto_fix_applied=preflight_result.changed,
            auto_fix_summary=preflight_result.fix_summary,
        )

    deduped_entries: dict[tuple[str, str], ReplacementEntry] = {}
    for entry in replacement_batch.items:
        dedupe_key = (
            entry.name,
            entry.find,
        )
        existing = deduped_entries.get(dedupe_key)
        if existing is not None and existing.replace != entry.replace:
            all_errors.append(
                "conflicting replace values for the same dir/name/find target:\n"
                f"  dir: {replacement_batch.dir_name}\n"
                f"  name: {entry.name}\n"
                f"  find: {entry.find}\n"
                f"  replace A: {existing.replace}\n"
                f"  replace B: {entry.replace}"
            )
            continue
        deduped_entries[dedupe_key] = entry

    for entry in deduped_entries.values():
        xml_path, resolve_errors = resolve_string_name_path(
            name_index,
            entry.name,
            dir_name=replacement_batch.dir_name,
        )
        if resolve_errors:
            all_errors.extend(resolve_errors)
            continue

        assert xml_path is not None
        xml_text = xml_text_cache.get(xml_path)
        if xml_text is None:
            try:
                xml_text = xml_path.read_text(encoding="utf-8")
            except Exception as exc:
                all_errors.append(f"{xml_path} | unreadable XML: {exc}")
                continue

        try:
            attempt = apply_replacement_in_string(
                xml_path,
                string_name=entry.name,
                find_text=entry.find,
                replace_text=entry.replace,
                xml_text=xml_text,
            )
        except ValueError as exc:
            all_errors.append(str(exc))
            continue

        if attempt.status == "applied" and attempt.original_text is not None and attempt.updated_text is not None:
            applied_total += 1
            applied_replacements.append(
                AppliedReplacement(
                    xml_path=xml_path,
                    string_name=entry.name,
                    original_text=attempt.original_text,
                    updated_text=attempt.updated_text,
                )
            )
            xml_text_cache[xml_path] = xml_path.read_text(encoding="utf-8")
            continue
        if attempt.status == "already_applied":
            already_applied_total += 1
            continue
        if attempt.status == "unchanged":
            skipped_unchanged_total += 1
            continue
        if attempt.status == "not_found":
            failed_not_found_total += 1
        elif attempt.status == "ambiguous":
            failed_ambiguous_total += 1
        if attempt.error is not None:
            all_errors.append(attempt.error)

    if emit_text and not quiet:
        print(f"Applied replacements: {applied_total}")
        if already_applied_total > 0:
            print(f"Already applied: {already_applied_total}")
        print_applied_diffs(applied_replacements)

    if applied_total > 0:
        touched_files = sorted({str(replacement.xml_path) for replacement in applied_replacements})
        run_fix_android_resource_escapes(
            res_dir=res_dir,
            files=touched_files,
            quiet=True,
            emit_text=False,
        )
    else:
        touched_files = []

    applied_targets = [
        f"{replacement.xml_path}::{replacement.string_name}"
        for replacement in applied_replacements
    ]

    smoke_check_failed = False
    smoke_check_ran = run_smoke_check and applied_total > 0
    if run_smoke_check and applied_total > 0:
        try:
            smoke_check_result = run_android_resource_smoke_check_with_options(quiet=quiet)
        except FileNotFoundError as exc:
            error = f"Smoke check setup error: {exc}"
            if emit_text:
                print(error, file=sys.stderr)
            return ReplaceCommandResult(
                exit_code=1,
                json_path=str(json_path),
                dir_name=replacement_batch.dir_name,
                applied_replacements=applied_total,
                already_applied_count=already_applied_total,
                skipped_unchanged_count=skipped_unchanged_total,
                failed_not_found_count=failed_not_found_total,
                failed_ambiguous_count=failed_ambiguous_total,
                failed_validation_count=len(all_errors),
                validation_error_count=len(all_errors),
                write_completed=True,
                write_ok=len(all_errors) == 0,
                smoke_check_ran=smoke_check_ran,
                smoke_check_ok=False,
                errors=tuple([*all_errors, error]),
                touched_files=tuple(touched_files),
                applied_targets=tuple(applied_targets),
                auto_fix_applied=preflight_result.changed,
                auto_fix_summary=preflight_result.fix_summary,
            )
        if not smoke_check_result.ok:
            smoke_check_failed = True
            if emit_text:
                print(
                    "Smoke check failed: Android resource compilation did not pass after replacements.",
                    file=sys.stderr,
                )

    if all_errors:
        if emit_text and not quiet:
            print("Applied replacements: 0")
            print("Validation errors:")
        if emit_text:
            for error in all_errors:
                print(error)
        return ReplaceCommandResult(
            exit_code=4 if smoke_check_failed else 2,
            json_path=str(json_path),
            dir_name=replacement_batch.dir_name,
            applied_replacements=applied_total,
            already_applied_count=already_applied_total,
            skipped_unchanged_count=skipped_unchanged_total,
            failed_not_found_count=failed_not_found_total,
            failed_ambiguous_count=failed_ambiguous_total,
            failed_validation_count=len(all_errors),
            validation_error_count=len(all_errors),
            write_completed=True,
            write_ok=False,
            smoke_check_ran=smoke_check_ran,
            smoke_check_ok=not smoke_check_failed,
            errors=tuple(all_errors),
            touched_files=tuple(touched_files),
            applied_targets=tuple(applied_targets),
            auto_fix_applied=preflight_result.changed,
            auto_fix_summary=preflight_result.fix_summary,
        )

    if smoke_check_failed:
        return ReplaceCommandResult(
            exit_code=3,
            json_path=str(json_path),
            dir_name=replacement_batch.dir_name,
            applied_replacements=applied_total,
            already_applied_count=already_applied_total,
            skipped_unchanged_count=skipped_unchanged_total,
            failed_not_found_count=failed_not_found_total,
            failed_ambiguous_count=failed_ambiguous_total,
            failed_validation_count=0,
            validation_error_count=0,
            write_completed=True,
            write_ok=True,
            smoke_check_ran=smoke_check_ran,
            smoke_check_ok=False,
            errors=("Smoke check failed: Android resource compilation did not pass after replacements.",),
            touched_files=tuple(touched_files),
            applied_targets=tuple(applied_targets),
            auto_fix_applied=preflight_result.changed,
            auto_fix_summary=preflight_result.fix_summary,
        )

    if emit_text and not quiet:
        print("All replacements applied successfully.")
    return ReplaceCommandResult(
        exit_code=0,
        json_path=str(json_path),
        dir_name=replacement_batch.dir_name,
        applied_replacements=applied_total,
        already_applied_count=already_applied_total,
        skipped_unchanged_count=skipped_unchanged_total,
        failed_not_found_count=failed_not_found_total,
        failed_ambiguous_count=failed_ambiguous_total,
        failed_validation_count=0,
        validation_error_count=0,
        write_completed=True,
        write_ok=True,
        smoke_check_ran=smoke_check_ran,
        smoke_check_ok=True,
        errors=(),
        touched_files=tuple(touched_files),
        applied_targets=tuple(applied_targets),
        auto_fix_applied=preflight_result.changed,
        auto_fix_summary=preflight_result.fix_summary,
    )


def run_replace_batch(
    *,
    res_dir: Path | str = DEFAULT_RES_DIRECTORY,
    json_paths: Iterable[Path | str],
    run_smoke_check: bool = False,
    auto_fix_json: bool = False,
    quiet: bool = False,
    emit_text: bool = True,
) -> ReplaceBatchCommandResult:
    paths = [Path(path) for path in json_paths]
    if not paths:
        error = "replace-batch requires at least one replacement JSON path."
        return ReplaceBatchCommandResult(
            exit_code=2,
            file_count=0,
            processed_count=0,
            success_count=0,
            failure_count=1,
            write_completed_count=0,
            write_ok_count=0,
            smoke_check_requested=run_smoke_check,
            smoke_check_ran=False,
            smoke_check_ok=not run_smoke_check,
            results=(),
            errors=(error,),
        )

    results: list[ReplaceCommandResult] = []
    for index, path in enumerate(paths):
        result = apply_translation_replacements(
            res_dir=res_dir,
            json_path=path,
            run_smoke_check=False,
            auto_fix_json=auto_fix_json,
            quiet=quiet,
            emit_text=emit_text,
        )
        results.append(result)
        if emit_text and not quiet:
            dir_label = result.dir_name or "<unknown-dir>"
            print(
                f"[replace-batch] {index + 1}/{len(paths)} {Path(path).name} -> "
                f"{dir_label} exit={result.exit_code} write_ok={result.write_ok}"
            )

    any_applied = any(result.applied_replacements > 0 for result in results)
    smoke_check_ran = False
    smoke_check_ok = not run_smoke_check
    batch_errors: list[str] = []
    if run_smoke_check and any_applied:
        smoke_check_ran = True
        try:
            smoke_result = run_android_resource_smoke_check_with_options(quiet=quiet)
        except FileNotFoundError as exc:
            smoke_check_ok = False
            batch_errors.append(f"Smoke check setup error: {exc}")
        else:
            smoke_check_ok = smoke_result.ok
            if not smoke_result.ok:
                batch_errors.append(
                    "Smoke check failed: Android resource compilation did not pass after batch replacements."
                )

    success_count = sum(1 for result in results if result.exit_code == 0)
    failure_count = len(results) - success_count
    write_completed_count = sum(1 for result in results if result.write_completed)
    write_ok_count = sum(1 for result in results if result.write_ok)
    if batch_errors:
        exit_code = 3 if failure_count == 0 else 4
    elif failure_count > 0:
        exit_code = 2
    else:
        exit_code = 0

    return ReplaceBatchCommandResult(
        exit_code=exit_code,
        file_count=len(paths),
        processed_count=len(results),
        success_count=success_count,
        failure_count=failure_count,
        write_completed_count=write_completed_count,
        write_ok_count=write_ok_count,
        smoke_check_requested=run_smoke_check,
        smoke_check_ran=smoke_check_ran,
        smoke_check_ok=smoke_check_ok,
        results=tuple(results),
        errors=tuple(batch_errors),
    )
