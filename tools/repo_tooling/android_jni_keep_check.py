from __future__ import annotations

import re
import sys
from dataclasses import dataclass
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
ANDROID_APP_DIR = REPO_ROOT / "apps" / "audio_android" / "app"
CPP_DIR = ANDROID_APP_DIR / "src" / "main" / "cpp"
SOURCE_DIR = ANDROID_APP_DIR / "src" / "main" / "java"
PROGUARD_RULES = ANDROID_APP_DIR / "proguard-rules.pro"

APP_CLASS_PREFIX = "com/bag/audioandroid/"

FIND_CLASS_PATTERN = re.compile(r'FindClass\("(?P<name>com/bag/audioandroid/[^"]+)"\)')
JNI_SIGNATURE_PATTERN = re.compile(r"L(?P<name>com/bag/audioandroid/[^;\"()]+);")
KEEP_RULE_PATTERN = re.compile(r"^-keep\s+class\s+(?P<name>[\w.]+)\s*\{\s*\*;\s*\}", re.MULTILINE)
PACKAGE_PATTERN = re.compile(r"^\s*package\s+([\w.]+)\s*$", re.MULTILINE)
DECL_PATTERN = re.compile(
    r"^\s*(?P<annotations>(?:@\w+(?:\([^)]*\))?\s*)*)"
    r"(?:(?:public|internal|private|protected)\s+)?"
    r"(?:(?:data|enum|sealed|value|annotation)\s+)?"
    r"(class|interface|object)\s+(?P<name>[A-Z]\w*)\b",
    re.MULTILINE,
)


@dataclass(frozen=True)
class DeclaredType:
    fqcn: str
    has_keep: bool
    source: Path


def collect_jni_referenced_classes() -> set[str]:
    referenced: set[str] = set()
    for path in CPP_DIR.rglob("*"):
        if path.suffix not in {".cpp", ".cc", ".cxx", ".h", ".hpp", ".inc"}:
            continue
        text = path.read_text(encoding="utf-8")
        for match in FIND_CLASS_PATTERN.finditer(text):
            referenced.add(match.group("name").replace("/", "."))
        for match in JNI_SIGNATURE_PATTERN.finditer(text):
            name = match.group("name")
            if name.startswith(APP_CLASS_PREFIX):
                referenced.add(name.replace("/", "."))
    return referenced


def collect_declared_types() -> dict[str, DeclaredType]:
    declared: dict[str, DeclaredType] = {}
    for path in SOURCE_DIR.rglob("*"):
        if path.suffix not in {".kt", ".java"}:
            continue
        text = path.read_text(encoding="utf-8")
        package_match = PACKAGE_PATTERN.search(text)
        if package_match is None:
            continue
        package_name = package_match.group(1)
        for match in DECL_PATTERN.finditer(text):
            fqcn = f"{package_name}.{match.group('name')}"
            has_keep = "@Keep" in match.group("annotations")
            declared[fqcn] = DeclaredType(fqcn=fqcn, has_keep=has_keep, source=path)
    return declared


def collect_proguard_keep_rules() -> set[str]:
    text = PROGUARD_RULES.read_text(encoding="utf-8")
    return {match.group("name") for match in KEEP_RULE_PATTERN.finditer(text)}


def main() -> int:
    referenced = collect_jni_referenced_classes()
    declared = collect_declared_types()
    keep_rules = collect_proguard_keep_rules()

    missing: list[str] = []
    for fqcn in sorted(referenced):
        declared_type = declared.get(fqcn)
        has_source_keep = declared_type.has_keep if declared_type is not None else False
        has_proguard_keep = fqcn in keep_rules
        if not has_source_keep and not has_proguard_keep:
            location = str(declared_type.source) if declared_type is not None else "source-not-found"
            missing.append(f"{fqcn} [{location}]")

    if not missing:
        print("[android-jni-keep-check] OK: all JNI-referenced app classes have @Keep or proguard backstop.")
        return 0

    print("[android-jni-keep-check] Missing release keep protection for JNI-referenced classes:", file=sys.stderr)
    for item in missing:
        print(f"  - {item}", file=sys.stderr)
    print(
        "Add @Keep on the source declaration or add a matching '-keep class ... { *; }' rule.",
        file=sys.stderr,
    )
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
