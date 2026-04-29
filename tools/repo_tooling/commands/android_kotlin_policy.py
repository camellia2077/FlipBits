from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re

from ..constants import ANDROID_APP_DIR
from ..errors import ToolError

KOTLIN_SOURCE_ROOT = ANDROID_APP_DIR / "src" / "main" / "java"

FLASH_WIRE_BRANCH_ALLOWED: frozenset[str] = frozenset(
    {
        "apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/model/FlashVoicingStyleOption.kt",
        "apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/model/FlashWireValues.kt",
    }
)

FLASH_WIRE_BRANCH_PATTERNS: tuple[re.Pattern[str], ...] = (
    re.compile(r"\bsignalProfileValue\s*(?:==|!=)\s*\d+\b"),
    re.compile(r"\bvoicingFlavorValue\s*(?:==|!=)\s*\d+\b"),
    re.compile(r"\bwhen\s*\([^)]*\bsignalProfileValue\b[^)]*\)"),
    re.compile(r"\bwhen\s*\([^)]*\bvoicingFlavorValue\b[^)]*\)"),
)


@dataclass(frozen=True)
class KotlinPolicyViolation:
    path: Path
    line_number: int
    line: str
    pattern: str


def _repo_relative(path: Path) -> str:
    return path.as_posix().split("WaveBits/", 1)[-1]


def _is_allowed(path: Path) -> bool:
    return _repo_relative(path) in FLASH_WIRE_BRANCH_ALLOWED


def _iter_kotlin_files(root: Path) -> list[Path]:
    return sorted(path for path in root.rglob("*.kt") if path.is_file())


def find_android_kotlin_policy_violations() -> list[KotlinPolicyViolation]:
    violations: list[KotlinPolicyViolation] = []
    for path in _iter_kotlin_files(KOTLIN_SOURCE_ROOT):
        if _is_allowed(path):
            continue
        for line_index, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
            for pattern in FLASH_WIRE_BRANCH_PATTERNS:
                if pattern.search(line):
                    violations.append(
                        KotlinPolicyViolation(
                            path=path,
                            line_number=line_index,
                            line=line.strip(),
                            pattern=pattern.pattern,
                        )
                    )
    return violations


def run_android_kotlin_policy_checks() -> None:
    violations = find_android_kotlin_policy_violations()
    if not violations:
        return

    lines = [
        "Android Kotlin policy failed.",
        "Do not branch on flash wire ints outside FlashVoicingStyleOption.",
        "Add a semantic helper on FlashVoicingStyleOption or a named wire constant at the boundary.",
        "",
        "Violations:",
    ]
    for violation in violations:
        lines.append(
            f"- {_repo_relative(violation.path)}:{violation.line_number}: {violation.line}"
        )
    raise ToolError("\n".join(lines))


def cmd_android_kotlin_policy() -> None:
    run_android_kotlin_policy_checks()
    print("Android Kotlin policy checks passed.")
