from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from scripts.loc.internal.report_diff import build_scan_diff
from scripts.loc.internal.report_models import (
    LineFileMatch,
    PathScanResult,
    ScanReport,
    ScanSpec,
    ScopePartReport,
    ScopeReport,
)
from scripts.loc.internal.responsibility_analyzers import ResponsibilityRiskResult


class LocReportDiffTests(unittest.TestCase):
    @staticmethod
    def _risk(path: str, *, score: int, lines: int, fingerprint: str, source: str, fanout: int) -> ResponsibilityRiskResult:
        return ResponsibilityRiskResult(
            path=path,
            lines=lines,
            score=score,
            priority="P2",
            summary="sample",
            state_signal_hits=0,
            top_level_composables=1,
            role_kinds=[],
            mode_branch_hits=0,
            dependency_fanout=fanout,
            is_small_file=lines < 120,
            content_fingerprint=fingerprint,
            implementation_sources=[source],
        )

    def test_diff_tracks_added_removed_changed_and_unchanged_entries(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            baseline_path = Path(temp_dir) / "baseline.json"
            baseline_path.write_text(
                json.dumps(
                    {
                        "results": [
                            {
                                "path": "tools",
                                "matched_files": [
                                    {"path": "tools/changed.py", "lines": 100, "priority": "P2"},
                                    {"path": "tools/stable.py", "lines": 80, "priority": "P3"},
                                    {"path": "tools/removed.py", "lines": 60, "priority": "P3"},
                                ],
                            }
                        ]
                    },
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )

            report = ScanReport(
                generated_at="2026-08-02T00:00:00+08:00",
                status="ok",
                lang="py",
                scan=ScanSpec(mode="over", threshold=200),
                results=(
                    PathScanResult(
                        path="tools",
                        matched_files=(
                            LineFileMatch(path="tools/changed.py", lines=120, priority="P1"),
                            LineFileMatch(path="tools/stable.py", lines=80, priority="P3"),
                            LineFileMatch(path="tools/added.py", lines=240, priority="P0"),
                        ),
                    ),
                ),
            )

            diff = build_scan_diff(baseline_path=baseline_path, report=report)

        self.assertEqual(diff["summary"], {"added": 1, "removed": 1, "changed": 1, "unchanged": 1})
        statuses = {item["path"]: item["status"] for item in diff["entries"]}
        self.assertEqual(statuses["tools/changed.py"], "changed")
        self.assertEqual(statuses["tools/stable.py"], "unchanged")
        self.assertEqual(statuses["tools/removed.py"], "removed")
        self.assertEqual(statuses["tools/added.py"], "added")

    def test_scope_diff_keeps_same_path_entries_separate_by_language_part(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            baseline_path = Path(temp_dir) / "scope_baseline.json"
            baseline_path.write_text(
                json.dumps(
                    {
                        "scope": "audio_android",
                        "parts": [
                            {
                                "part": "cpp",
                                "results": [
                                    {"path": "shared/main.cpp", "matched_files": [{"path": "shared/main.cpp", "lines": 100}]}
                                ],
                            },
                            {
                                "part": "kt",
                                "results": [
                                    {"path": "shared/main.cpp", "matched_files": [{"path": "shared/main.cpp", "lines": 80}]}
                                ],
                            },
                        ],
                    },
                    ensure_ascii=False,
                ),
                encoding="utf-8",
            )

            report = ScopeReport(
                generated_at="2026-08-02T00:00:00+08:00",
                status="ok",
                scope="audio_android",
                display_name="apps/audio_android",
                parts=(
                    ScopePartReport(
                        part="cpp",
                        display_name="C++",
                        report=ScanReport(
                            generated_at="2026-08-02T00:00:00+08:00",
                            status="ok",
                            lang="cpp",
                            scan=ScanSpec(mode="over", threshold=200),
                            results=(PathScanResult(path="shared", matched_files=(LineFileMatch(path="shared/main.cpp", lines=120),)),),
                        ),
                    ),
                    ScopePartReport(
                        part="kt",
                        display_name="Kotlin",
                        report=ScanReport(
                            generated_at="2026-08-02T00:00:00+08:00",
                            status="ok",
                            lang="kt",
                            scan=ScanSpec(mode="over", threshold=180),
                            results=(PathScanResult(path="shared", matched_files=(LineFileMatch(path="shared/main.cpp", lines=80),)),),
                        ),
                    ),
                ),
            )

            diff = build_scan_diff(baseline_path=baseline_path, report=report)

        self.assertEqual(diff["summary"], {"added": 0, "removed": 0, "changed": 1, "unchanged": 1})
        changed = [item for item in diff["entries"] if item["status"] == "changed"]
        self.assertEqual(changed[0]["part"], "cpp")

    def test_responsibility_inventory_distinguishes_threshold_drop_and_fragmentation(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            baseline_path = Path(temp_dir) / "baseline.json"
            before = self._risk(
                "src/owner.cpp",
                score=6,
                lines=240,
                fingerprint="before",
                source="src/owner.cpp",
                fanout=2,
            )
            baseline_path.write_text(
                json.dumps(
                    {
                        "results": [
                            {
                                "path": "src",
                                "matched_files": [before.to_dict()],
                                "scanned_files": [before.to_dict()],
                            }
                        ]
                    }
                ),
                encoding="utf-8",
            )
            owner_after = self._risk(
                "src/owner.cpp",
                score=3,
                lines=100,
                fingerprint="owner-after",
                source="src/owner.cpp",
                fanout=3,
            )
            fragment_a = self._risk(
                "src/a.cpp",
                score=2,
                lines=70,
                fingerprint="duplicate",
                source="src/a.cpp",
                fanout=1,
            )
            fragment_b = self._risk(
                "src/b.cpp",
                score=2,
                lines=70,
                fingerprint="duplicate",
                source="src/b.cpp",
                fanout=1,
            )
            report = ScanReport(
                generated_at="2026-08-09T00:00:00+08:00",
                status="ok",
                lang="cpp",
                scan=ScanSpec(mode="responsibility_risk", threshold=5),
                results=(
                    PathScanResult(
                        path="src",
                        matched_files=(),
                        scanned_files=(owner_after, fragment_a, fragment_b),
                    ),
                ),
            )

            diff = build_scan_diff(baseline_path=baseline_path, report=report)

        statuses = {item["path"]: item["status"] for item in diff["entries"]}
        self.assertEqual(statuses["src/owner.cpp"], "below_threshold")
        self.assertEqual(diff["fragmentation_delta"]["file_count"], 2)
        self.assertEqual(diff["fragmentation_delta"]["small_file_count"], 3)
        self.assertEqual(diff["dependency_fanout_delta"]["total"], 3)
        self.assertEqual(diff["duplicate_owner"]["after"][0]["paths"], ["src/a.cpp", "src/b.cpp"])


if __name__ == "__main__":
    unittest.main()
