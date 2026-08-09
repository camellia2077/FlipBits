from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from scripts.loc.internal.config import load_scan_lines_config
from scripts.loc.internal.report_formatter import ScopeReportFormatter
from scripts.loc.internal.report_writers import JsonReportWriter, MarkdownReportWriter
from scripts.loc.internal.scope_builder import ScopeReportBuilder


CONFIG_PATH = TOOLS_DIR / "scripts" / "loc" / "scan_lines.toml"


class LocScopeTests(unittest.TestCase):
    def test_scope_builder_aggregates_multiple_language_parts(self) -> None:
        config = load_scan_lines_config(CONFIG_PATH)
        scope_config = config.scopes["audio_android"]
        language_configs = tuple(config.languages[lang] for lang in scope_config.languages)

        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            (root / "sample.cpp").write_text("int main() {}\n", encoding="utf-8")
            (root / "Sample.kt").write_text("fun sample() {}\n", encoding="utf-8")

            result = ScopeReportBuilder(
                config=scope_config,
                language_configs=language_configs,
            ).build_line_scan(
                generated_at="2026-08-02T00:00:00+08:00",
                paths=[root],
                mode="over",
                thresholds={"cpp": 0, "kt": 0},
            )

        self.assertEqual([part.part for part in result.report.parts], ["cpp", "kt"])
        self.assertEqual(result.report.summary["matched_files"], 2)
        self.assertEqual(result.report.summary["parts"], 2)
        artifact_roots = {artifact.relative_output_path.parts[0] for artifact in result.artifacts}
        self.assertEqual(artifact_roots, {"cpp", "kotlin"})

    def test_scope_writers_emit_scope_root_and_parts(self) -> None:
        config = load_scan_lines_config(CONFIG_PATH)
        scope_config = config.scopes["audio_android"]
        language_configs = tuple(config.languages[lang] for lang in scope_config.languages)
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            (root / "sample.cpp").write_text("int main() {}\n", encoding="utf-8")
            (root / "Sample.kt").write_text("fun sample() {}\n", encoding="utf-8")
            result = ScopeReportBuilder(
                config=scope_config,
                language_configs=language_configs,
            ).build_line_scan(
                generated_at="2026-08-02T00:00:00+08:00",
                paths=[root],
                mode="over",
                thresholds={"cpp": 0, "kt": 0},
            )
            formatter = ScopeReportFormatter(
                display_name=scope_config.display_name,
                over_inclusive_by_lang={item.lang: item.over_inclusive for item in language_configs},
                display_name_by_lang={item.lang: item.display_name for item in language_configs},
            )
            json_path = root / "scope.json"
            md_path = root / "scope.md"
            JsonReportWriter.write_scope_report(json_path, result.report)
            MarkdownReportWriter(formatter.formatter_for("cpp")).write_scope_report(
                md_path,
                result.report,
                formatter,
            )

            payload = json.loads(json_path.read_text(encoding="utf-8"))
            markdown = md_path.read_text(encoding="utf-8")

        self.assertEqual(payload["scope"], "audio_android")
        self.assertEqual([part["part"] for part in payload["parts"]], ["cpp", "kt"])
        self.assertIn("Part: C++", markdown)
        self.assertIn("Part: Kotlin", markdown)


if __name__ == "__main__":
    unittest.main()
