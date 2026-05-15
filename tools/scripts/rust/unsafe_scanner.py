from __future__ import annotations

import argparse
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path


DEFAULT_ROOT = Path("apps/audio_cli/rust/src")
DEFAULT_OUTPUT = Path("temp/unsafe_usage_report.txt")
RUST_FILE_PATTERN = "*.rs"
UNSAFE_PATTERN = re.compile(r"\bunsafe\b")

# Patterns that suggest an FFI call or declaration
FFI_INDICATORS = [
    r"extern\s+\"C\"",
    r"bag_",
    r"core_",
    r"audio_io_",
    r"audio_api_",
    r"validate_",
    r"poll_",
    r"push_",
    r"destroy_",
    r"free_",
    r"cancel_",
    r"encode_",
    r"decode_",
    r"_message\(",
    r"_version\(",
]

# Patterns that suggest memory/pointer manipulation (raw-slice/cstr)
MEM_INDICATORS = {
    "raw-slice": [r"from_raw_parts", r"as_ptr", r"as_mut_ptr", r"ptr::", r"\.cast"],
    "cstr": [r"CStr::from_ptr", r"to_ptr"],
}


@dataclass(frozen=True)
class Match:
    path: Path
    lineno: int
    line: str
    category: str
    is_boundary: bool  # True if it looks like a clean FFI/memory boundary


def classify_usage(line: str, context_lines: list[str]) -> tuple[str, bool]:
    """Classifies unsafe usage based on the line and surrounding context."""
    # Combine line and context for classification
    text = (line + " " + " ".join(context_lines)).strip()
    
    # Check for extern declaration
    if 'extern "C"' in text:
        return "extern", True
    
    # Check for Drop implementation (usually a boundary)
    if "Drop for" in text:
        return "drop-impl", True
        
    # Check for memory/pointer boundaries
    for cat, indicators in MEM_INDICATORS.items():
        if any(re.search(ind, text) for ind in indicators):
            return cat, True
            
    # Check for FFI calls
    if any(re.search(ind, text) for ind in FFI_INDICATORS):
        # A boundary should ideally be simple. If we see complex control flow 
        # (if/loop/match) in the context, it might be "scattered business logic".
        complex_keywords = [r"\bif\b", r"\bloop\b", r"\bmatch\b", r"\bwhile\b"]
        is_complex = any(re.search(kw, text) for kw in complex_keywords)
        # Also check for multiple statements (crude check)
        is_multi_stmt = text.count(";") > 2 # allow a couple for setup
        
        return "ffi-call", not (is_complex or is_multi_stmt)
        
    # If it's just a raw dereference or something else
    if "*" in text and any(x in text for x in [".", "->", "ptr"]):
        return "ptr-deref", False
        
    return "other", False


def scan_file(path: Path) -> list[Match]:
    matches: list[Match] = []
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
        for i, line in enumerate(lines):
            if UNSAFE_PATTERN.search(line):
                # Look ahead up to 10 lines for context (to skip safety comments etc.)
                context = lines[i+1 : i+11]
                category, is_boundary = classify_usage(line, context)
                matches.append(
                    Match(
                        path=path, 
                        lineno=i + 1, 
                        line=line.strip(), 
                        category=category,
                        is_boundary=is_boundary
                    )
                )
    except Exception as e:
        print(f"Error reading {path}: {e}", file=sys.stderr)
    return matches


def iter_rust_files(root: Path) -> list[Path]:
    return sorted(root.rglob(RUST_FILE_PATTERN))


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Scan Rust sources for unsafe usage. Aims to identify FFI boundaries vs scattered logic."
    )
    parser.add_argument(
        "root",
        nargs="?",
        default=str(DEFAULT_ROOT),
        help=f"Root directory to scan. Defaults to {DEFAULT_ROOT}.",
    )
    parser.add_argument(
        "-o", "--output",
        default=str(DEFAULT_OUTPUT),
        help=f"Output file path. Defaults to {DEFAULT_OUTPUT}.",
    )
    args = parser.parse_args()

    root = Path(args.root)
    if not root.exists():
        print(f"Scan root does not exist: {root}", file=sys.stderr)
        return 1

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    all_matches: list[Match] = []
    rust_files = iter_rust_files(root)
    for path in rust_files:
        all_matches.extend(scan_file(path))

    by_file: dict[Path, list[Match]] = defaultdict(list)
    category_totals: Counter[str] = Counter()
    for match in all_matches:
        by_file[match.path].append(match)
        category_totals[match.category] += 1

    report_lines = []
    report_lines.append(f"Rust Unsafe Usage Audit Report")
    report_lines.append(f"===============================")
    report_lines.append(f"Scanning: {root}")
    report_lines.append("")
    report_lines.append("GOAL: Shrink unsafe usage to auditable FFI boundaries.")
    report_lines.append("")
    
    boundary_count = sum(1 for m in all_matches if m.is_boundary)
    scattered_count = len(all_matches) - boundary_count
    
    report_lines.append(f"Overall Health Summary:")
    report_lines.append(f"  [B] FFI/Mem Boundaries: {boundary_count}")
    report_lines.append(f"  [S] Scattered/Complex:  {scattered_count}")
    
    health_score = (boundary_count / len(all_matches) * 100) if all_matches else 100
    report_lines.append(f"  Health Score: {health_score:.1f}%")
    report_lines.append("")

    for path in sorted(by_file):
        matches = by_file[path]
        local_counts = Counter(match.category for match in matches)
        local_scattered = sum(1 for m in matches if not m.is_boundary)
        
        status = "[CLEAN BOUNDARIES]" if local_scattered == 0 else f"[SCATTERED: {local_scattered}]"
        
        report_lines.append(f"{path} {status}")
        report_lines.append("  Categories: " + ", ".join(f"{name}={count}" for name, count in sorted(local_counts.items())))
        for match in matches:
            prefix = "  [B]" if match.is_boundary else "  [S]"
            report_lines.append(f"{prefix} {match.lineno:>4}: [{match.category}] {match.line}")
        report_lines.append("")

    report_lines.append("Category totals:")
    for name, count in sorted(category_totals.items()):
        report_lines.append(f"  {name}: {count}")
    
    report_lines.append("")
    summary = (
        f"Summary: {len(by_file)} files with unsafe, {len(all_matches)} total matches, "
        f"{len(rust_files)} Rust files scanned."
    )
    report_lines.append(summary)

    report_content = "\n".join(report_lines)
    
    # Print to console
    print(report_content)
    
    # Write to file
    try:
        output_path.write_text(report_content, encoding="utf-8")
        print(f"\nReport written to: {output_path}")
    except Exception as e:
        print(f"Error writing report to {output_path}: {e}", file=sys.stderr)

    return 0


if __name__ == "__main__":
    sys.exit(main())
