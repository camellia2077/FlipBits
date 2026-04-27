from __future__ import annotations

from datetime import datetime, timezone
import json
from pathlib import Path


def utc_now_string() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")


def normalize_path_string(path: str | Path) -> str:
    return str(Path(path).resolve())


def manifest_path_for_job_dir(job_dir: str | Path) -> Path:
    return Path(job_dir).resolve() / "job_manifest.json"


def load_manifest(path: Path) -> dict[str, object]:
    if not path.exists():
        return {}
    try:
        raw_payload = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return {}
    return raw_payload if isinstance(raw_payload, dict) else {}


def update_job_manifest(job_dir: str | Path, patch: dict[str, object]) -> str:
    manifest_path = manifest_path_for_job_dir(job_dir)
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    payload = load_manifest(manifest_path)
    payload.update(patch)
    payload["job_dir"] = normalize_path_string(manifest_path.parent)
    payload["manifest_version"] = 1
    payload["updated_at"] = utc_now_string()
    manifest_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return str(manifest_path)


def build_compare_manifest_patch(result) -> dict[str, object]:
    return {
        "compare": {
            "output_dir": normalize_path_string(result.output_dir),
            "prompt_mode": result.prompt_mode,
            "prompt_version": result.prompt_version,
            "english_review_paths": [normalize_path_string(path) for path in result.english_review_paths],
            "localized_review_paths": [normalize_path_string(path) for path in result.localized_review_paths],
            "prompt_doc_paths": [normalize_path_string(path) for path in result.prompt_doc_paths],
            "task_json_paths": [normalize_path_string(path) for path in result.task_json_paths],
        }
    }


def build_replace_manifest_patch(
    *,
    json_path: str,
    summary_out: str | None,
    payload: dict[str, object],
) -> dict[str, object]:
    return {
        "replace": {
            "input_json": normalize_path_string(json_path),
            "result_json": normalize_path_string(summary_out) if summary_out else None,
            "result": payload,
        }
    }
