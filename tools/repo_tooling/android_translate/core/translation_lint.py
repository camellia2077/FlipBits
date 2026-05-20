from __future__ import annotations

from dataclasses import dataclass
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

from core.android_resource_escapes import run_fix_android_resource_escapes
from core.mixed_language_policy import load_mixed_language_policy

HEX_LITERAL_PATTERN = re.compile(r"#RRGGBB")
PLACEHOLDER_PATTERN = re.compile(r"%\d+\$[sdf]")


def _contains_locked_term(source_text: str, term: str) -> bool:
    if term.isalpha() and term.islower():
        return re.search(rf"(?<![A-Za-z]){re.escape(term)}(?![A-Za-z])", source_text, flags=re.IGNORECASE) is not None
    return term in source_text


def _missing_locked_term(localized_text: str, term: str) -> bool:
    if term.isalpha() and term.islower():
        return re.search(rf"(?<![A-Za-z]){re.escape(term)}(?![A-Za-z])", localized_text, flags=re.IGNORECASE) is None
    return term not in localized_text


@dataclass
class LintIssue:
    file: str
    key: str
    level: str
    rule: str
    message: str


@dataclass
class LintResult:
    issues: list[LintIssue]
    checked_files: int

    @property
    def exit_code(self) -> int:
        return 2 if any(issue.level == "error" for issue in self.issues) else 0


@dataclass
class AutofixResult:
    changed_files: list[str]
    total_replacements: int
    escape_files_updated: int
    escape_strings_updated: int


def _issue_fingerprint(issue: LintIssue, *, res_dir: str) -> str:
    try:
        rel_path = Path(issue.file).resolve().relative_to(Path(res_dir).resolve()).as_posix()
    except Exception:
        rel_path = Path(issue.file).name
    return f"{rel_path}|{issue.key}|{issue.rule}|{issue.level}"


def load_lint_baseline(*, baseline_file: str) -> set[str]:
    path = Path(baseline_file)
    if not path.exists():
        return set()
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return set()
    issues = payload.get("issues")
    if not isinstance(issues, list):
        return set()
    fingerprints: set[str] = set()
    for item in issues:
        if not isinstance(item, dict):
            continue
        fp = item.get("fingerprint")
        if isinstance(fp, str) and fp:
            fingerprints.add(fp)
    return fingerprints


def save_lint_baseline(*, baseline_file: str, issues: list[LintIssue], res_dir: str) -> None:
    path = Path(baseline_file)
    path.parent.mkdir(parents=True, exist_ok=True)
    rows: list[dict[str, str]] = []
    for issue in issues:
        fingerprint = _issue_fingerprint(issue, res_dir=res_dir)
        try:
            rel_path = Path(issue.file).resolve().relative_to(Path(res_dir).resolve()).as_posix()
        except Exception:
            rel_path = Path(issue.file).name
        rows.append(
            {
                "fingerprint": fingerprint,
                "file": rel_path,
                "key": issue.key,
                "level": issue.level,
                "rule": issue.rule,
            }
        )
    rows.sort(key=lambda item: item["fingerprint"])
    payload = {
        "version": 1,
        "issues": rows,
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def filter_new_lint_issues(*, issues: list[LintIssue], baseline_fingerprints: set[str], res_dir: str) -> list[LintIssue]:
    if not baseline_fingerprints:
        return list(issues)
    out: list[LintIssue] = []
    for issue in issues:
        if _issue_fingerprint(issue, res_dir=res_dir) not in baseline_fingerprints:
            out.append(issue)
    return out


def _iter_xml_files(res_dir: str, lang: str | None) -> list[Path]:
    root = Path(res_dir)
    dirs: list[Path]
    if lang:
        dirs = [root / f"values-{lang}"]
    else:
        dirs = [p for p in root.iterdir() if p.is_dir() and p.name.startswith("values-")]
    files: list[Path] = []
    for d in dirs:
        if not d.exists():
            continue
        files.extend(sorted(d.glob("*.xml")))
    return files


def _english_map(res_dir: str) -> dict[str, dict[str, str]]:
    base = Path(res_dir) / "values"
    out: dict[str, dict[str, str]] = {}
    if not base.exists():
        return out
    for path in base.glob("*.xml"):
        mapping: dict[str, str] = {}
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        for s in root.findall("string"):
            name = s.attrib.get("name", "")
            mapping[name] = s.text or ""
        out[path.name] = mapping
    return out


def run_translation_lint(*, res_dir: str, lang: str | None = None) -> LintResult:
    issues: list[LintIssue] = []
    files = _iter_xml_files(res_dir, lang)
    en = _english_map(res_dir)
    locked_terms = load_mixed_language_policy().detection.shared_locked_terms
    for path in files:
        raw_text = path.read_text(encoding="utf-8")
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as exc:
            issues.append(LintIssue(str(path), "-", "error", "xml_parse", str(exc)))
            continue
        en_map = en.get(path.name, {})
        for s in root.findall("string"):
            key = s.attrib.get("name", "")
            text = s.text or ""
            if "`r`n" in text:
                issues.append(LintIssue(str(path), key, "error", "literal_crlf_token", "Contains literal `r`n token."))
            if "RRGGBB" in text and not HEX_LITERAL_PATTERN.search(text):
                issues.append(LintIssue(str(path), key, "warn", "hex_literal_format", "Expected #RRGGBB literal style."))
            en_text = en_map.get(key)
            if en_text is not None:
                src_ph = sorted(PLACEHOLDER_PATTERN.findall(en_text))
                dst_ph = sorted(PLACEHOLDER_PATTERN.findall(text))
                if src_ph != dst_ph:
                    issues.append(
                        LintIssue(
                            str(path),
                            key,
                            "error",
                            "placeholder_mismatch",
                            f"Placeholder mismatch. en={src_ph}, loc={dst_ph}",
                        )
                    )
                for term in locked_terms:
                    if _contains_locked_term(en_text, term) and _missing_locked_term(text, term):
                        issues.append(
                            LintIssue(
                                str(path),
                                key,
                                "error",
                                "locked_term_missing",
                                f"Locked term '{term}' appears in EN but not in localized text.",
                            )
                        )
    return LintResult(issues=issues, checked_files=len(files))


def run_translation_autofix(*, res_dir: str, lang: str | None = None) -> AutofixResult:
    files = _iter_xml_files(res_dir, lang)
    changed_files: list[str] = []
    total = 0
    for path in files:
        text = path.read_text(encoding="utf-8")
        updated = text
        updated = updated.replace("\\'", "'")
        updated = updated.replace("`r`n", " ")
        updated = updated.replace("`#RRGGBB`", "#RRGGBB")
        if updated != text:
            total += 1
            path.write_text(updated, encoding="utf-8")
            changed_files.append(str(path))
    # Always run Android-string escape normalization as the final safety pass.
    escape_result = run_fix_android_resource_escapes(
        res_dir=res_dir,
        files=[str(path) for path in files],
        quiet=True,
        emit_text=False,
    )
    for path in escape_result.updated_files:
        if path not in changed_files:
            changed_files.append(path)
    total += escape_result.strings_updated
    return AutofixResult(
        changed_files=changed_files,
        total_replacements=total,
        escape_files_updated=escape_result.files_updated,
        escape_strings_updated=escape_result.strings_updated,
    )
