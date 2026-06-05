from __future__ import annotations

import json
from pathlib import Path
import sys


def emit_json_payload(payload: dict[str, object]) -> None:
    reconfigure = getattr(sys.stdout, "reconfigure", None)
    if callable(reconfigure):
        reconfigure(encoding="utf-8", errors="replace")
    print(json.dumps(payload, ensure_ascii=False, indent=2))


def write_json_payload(path: str | Path, payload: dict[str, object]) -> None:
    output_path = Path(path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def compare_payload(result) -> dict[str, object]:
    errors = [result.error] if result.error else []
    return {
        "ok": result.exit_code == 0,
        "command": "compare",
        "exit_code": result.exit_code,
        "summary": {
            "english_review_files": result.english_review_files,
            "localized_review_files": result.localized_review_files,
            "localized_languages": result.localized_languages,
            "english_review_paths": list(result.english_review_paths),
            "localized_review_paths": list(result.localized_review_paths),
            "prompt_doc_paths": list(result.prompt_doc_paths),
            "task_json_paths": list(result.task_json_paths),
            "prompt_mode": result.prompt_mode,
            "prompt_version": result.prompt_version,
        },
        "artifacts": {
            "output_dir": str(result.output_dir),
        },
        "errors": errors,
    }


def compare_error_payload(*, output_dir: str, error: str) -> dict[str, object]:
    return {
        "ok": False,
        "command": "compare",
        "exit_code": 2,
        "summary": {
            "english_review_files": 0,
            "localized_review_files": 0,
            "localized_languages": 0,
            "english_review_paths": [],
            "localized_review_paths": [],
            "prompt_doc_paths": [],
            "task_json_paths": [],
        },
        "artifacts": {
            "output_dir": output_dir,
        },
        "errors": [error],
    }


def mixed_language_payload(result) -> dict[str, object]:
    return {
        "ok": result.exit_code == 0,
        "command": "mixed-language",
        "exit_code": result.exit_code,
        "summary": {
            "suspicious_issue_count": result.suspicious_issue_count,
            "report_file_count": result.report_file_count,
            "task_json_count": len(result.task_json_paths),
        },
        "artifacts": {
            "output_dir": str(result.output_dir),
            "task_json_paths": list(result.task_json_paths),
        },
        "errors": [],
    }


def key_alignment_payload(result) -> dict[str, object]:
    return {
        "ok": result.exit_code == 0,
        "command": "key-alignment",
        "exit_code": result.exit_code,
        "summary": {
            "alignment_issue_count": result.alignment_issue_count,
            "stale_issue_count": result.stale_issue_count,
            "report_file_count": result.report_file_count,
            "task_json_paths": list(result.task_json_paths),
        },
        "artifacts": {
            "output_dir": str(result.output_dir),
        },
        "errors": [],
    }


def mixed_language_context_audit_payload(result) -> dict[str, object]:
    return {
        "ok": result.exit_code == 0,
        "command": "mixed-language-context-audit",
        "exit_code": result.exit_code,
        "summary": {
            "total_entries": result.total_entries,
            "keep_en_count": result.keep_en_count,
            "needs_translation_count": result.needs_translation_count,
            "needs_context_count": result.needs_context_count,
            "missing_context_count": result.missing_context_count,
            "report_file_count": result.report_file_count,
        },
        "artifacts": {
            "output_dir": str(result.output_dir),
            "json_path": result.json_path,
        },
        "errors": [],
    }


def untranslated_equals_english_payload(result) -> dict[str, object]:
    return {
        "ok": result.exit_code == 0,
        "command": "untranslated-equals-english",
        "exit_code": result.exit_code,
        "summary": {
            "total_entries": result.total_entries,
            "keep_en_count": result.keep_en_count,
            "needs_translation_count": result.needs_translation_count,
            "needs_context_count": result.needs_context_count,
            "missing_context_count": result.missing_context_count,
            "report_file_count": result.report_file_count,
            "per_language": result.per_language,
        },
        "artifacts": {
            "output_dir": str(result.output_dir),
            "summary_json_path": result.summary_json_path,
        },
        "errors": [],
    }


def unused_keys_payload(result) -> dict[str, object]:
    return {
        "ok": result.exit_code == 0,
        "command": "unused-keys",
        "exit_code": result.exit_code,
        "summary": {
            "suspicious_unused_key_count": result.suspicious_unused_key_count,
            "scanned_file_count": result.scanned_file_count,
            "scanned_string_reference_count": result.scanned_string_reference_count,
            "report_file_count": result.report_file_count,
        },
        "artifacts": {
            "output_dir": str(result.output_dir),
            "summary_json_path": result.summary_json_path,
        },
        "errors": [],
    }


def replace_payload(
    result,
    *,
    json_path: str | None = None,
    summary_out: str | None = None,
    normalize_path: callable | None = None,
) -> dict[str, object]:
    payload = {
        "ok": result.exit_code == 0,
        "command": "replace",
        "exit_code": result.exit_code,
        "summary": {
            "applied_replacements": result.applied_replacements,
            "already_applied_count": result.already_applied_count,
            "skipped_unchanged_count": result.skipped_unchanged_count,
            "failed_not_found_count": result.failed_not_found_count,
            "failed_ambiguous_count": result.failed_ambiguous_count,
            "failed_validation_count": result.failed_validation_count,
            "validation_error_count": result.validation_error_count,
            "write_completed": result.write_completed,
            "write_ok": result.write_ok,
            "smoke_check_ran": result.smoke_check_ran,
            "smoke_check_ok": result.smoke_check_ok,
            "auto_fix_applied": result.auto_fix_applied,
            "status_counts": {
                "applied": result.applied_replacements,
                "already_applied": result.already_applied_count,
                "skipped_unchanged": result.skipped_unchanged_count,
                "failed_not_found": result.failed_not_found_count,
                "failed_ambiguous": result.failed_ambiguous_count,
                "failed_validation": result.failed_validation_count,
            },
        },
        "artifacts": {
            "dir": result.dir_name,
            "touched_files": list(result.touched_files),
            "applied_targets": list(result.applied_targets),
        },
        "errors": list(result.errors),
    }
    normalize = normalize_path or (lambda value: value)
    if json_path is not None:
        payload["artifacts"]["input_json"] = normalize(json_path)
    if summary_out:
        payload["artifacts"]["summary_out"] = normalize(summary_out)
    if result.auto_fix_summary is not None:
        payload["summary"]["auto_fix_summary"] = result.auto_fix_summary
    return payload


def replace_batch_payload(
    result,
    *,
    json_paths: list[str],
    summary_out: str | None = None,
    normalize_path: callable | None = None,
) -> dict[str, object]:
    normalize = normalize_path or (lambda value: value)
    item_payloads = [
        replace_payload(
            item,
            json_path=item.json_path,
            normalize_path=normalize,
        )
        for item in result.results
    ]
    payload = {
        "ok": result.exit_code == 0,
        "command": "replace-batch",
        "exit_code": result.exit_code,
        "summary": {
            "file_count": result.file_count,
            "processed_count": result.processed_count,
            "success_count": result.success_count,
            "failure_count": result.failure_count,
            "write_completed_count": result.write_completed_count,
            "write_ok_count": result.write_ok_count,
            "smoke_check_requested": result.smoke_check_requested,
            "smoke_check_ran": result.smoke_check_ran,
            "smoke_check_ok": result.smoke_check_ok,
        },
        "artifacts": {
            "input_jsons": [normalize(path) for path in json_paths],
            "results": item_payloads,
        },
        "errors": list(result.errors),
    }
    if summary_out:
        payload["artifacts"]["summary_out"] = normalize(summary_out)
    return payload


def smoke_check_payload(result, *, normalize_path: callable | None = None) -> dict[str, object]:
    normalize = normalize_path or (lambda value: value)
    return {
        "ok": result.ok,
        "command": "smoke-check",
        "exit_code": 0 if result.ok else 3,
        "summary": {
            "smoke_check_ran": True,
            "smoke_check_ok": result.ok,
            "return_code": result.return_code,
        },
        "artifacts": {
            "command": [normalize(part) if index == 0 else part for index, part in enumerate(result.command)],
        },
        "errors": [] if result.ok else ["Smoke check failed: Android resource compilation did not pass."],
    }


def fix_resource_escapes_payload(result) -> dict[str, object]:
    return {
        "ok": result.exit_code == 0,
        "command": "fix-resource-escapes",
        "exit_code": result.exit_code,
        "summary": {
            "files_checked": result.files_checked,
            "files_updated": result.files_updated,
            "strings_updated": result.strings_updated,
        },
        "artifacts": {
            "updated_files": list(result.updated_files),
        },
        "errors": [],
    }
