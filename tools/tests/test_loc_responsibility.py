from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from scripts.loc.internal.config import load_language_config
from scripts.loc.internal.report_formatter import ReportFormatter
from scripts.loc.internal.report_models import DetailReport, ScanSpec
from scripts.loc.internal.responsibility_analyzers import ResponsibilityRiskResult
from scripts.loc.internal.responsibility_metrics import (
    ResponsibilityFunctionHotspot,
    ResponsibilityMetrics,
)
from scripts.loc.internal.responsibility_plugins import create_responsibility_language_plugin
from scripts.loc.internal.responsibility_policies import ResponsibilityPolicy
from scripts.loc.internal.responsibility_scoring import (
    CppResponsibilityScorer,
    KotlinResponsibilityScorer,
    PythonResponsibilityScorer,
)


CONFIG_PATH = TOOLS_DIR / "scripts" / "loc" / "scan_lines.toml"


PYTHON_SAMPLE = """\
import json
import os
import requests
import subprocess


class ToolManager:
    pass


class ReportParser:
    pass


def validate_input(value):
    return bool(value)


def resolve_path(value):
    return value


def parse_data(value):
    return json.loads(value)


def write_report(path, value):
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(value)


def render_output(value):
    print(value)


def load_remote(url):
    return requests.get(url)


def run_mode(mode):
    if mode == "fast":
        return subprocess.run(["tool"], check=True)
    return None
"""


KOTLIN_SAMPLE = """\
@Composable
fun SettingsSection(viewMode: String) {
    val state = remember { mutableStateOf(false) }
    LaunchedEffect(viewMode) { }
    if (viewMode == "advanced") { }
}

@Composable
fun PreviewCard() { }

@Composable
fun ModeSwitcher() { }
"""


CPP_SAMPLE = """\
#include <jni.h>
#include "audio_api.h"

extern "C" {
bag_start() {
    return 0;
}
}

jobject BuildViewData(JNIEnv* env) {
    return nullptr;
}

int ParsePacket(const char* value) {
    return value ? 1 : 0;
}

int EncodePacket() {
    return 0;
}

void CancelOperation() {
}
"""


def metrics(**overrides: int | list[str]) -> ResponsibilityMetrics:
    values: dict[str, object] = {
        "line_count": 100,
        "state_signal_hits": 0,
        "top_level_symbol_count": 0,
        "role_kinds": [],
        "mode_branch_hits": 0,
    }
    values.update(overrides)
    return ResponsibilityMetrics(**values)


class LocScoringTests(unittest.TestCase):
    def test_language_scorers_have_stable_priority_boundaries(self) -> None:
        kt = load_language_config(CONFIG_PATH, "kt")
        py = load_language_config(CONFIG_PATH, "py")
        cpp = load_language_config(CONFIG_PATH, "cpp")

        kt_result = KotlinResponsibilityScorer(kt).assess(
            file_path=Path("Settings.kt"),
            metrics=metrics(
                line_count=180,
                state_signal_hits=4,
                top_level_symbol_count=3,
                role_kinds=["Section", "Card"],
                mode_branch_hits=2,
            ),
        )
        self.assertEqual((kt_result.score, kt_result.priority), (6, "P1"))
        self.assertEqual(
            [risk.value for risk in kt_result.dominant_risks or []],
            ["stateful_side_effects", "mode_branching"],
        )

        py_result = PythonResponsibilityScorer(py).assess(
            file_path=Path("commands/sample.py"),
            metrics=metrics(
                line_count=220,
                state_signal_hits=4,
                top_level_symbol_count=6,
                role_kinds=["Manager", "Parser"],
                mode_branch_hits=2,
                io_kind_count=3,
                rule_helper_count=6,
                responsibility_verb_kind_count=3,
                command_layer_leak_hits=3,
            ),
        )
        self.assertEqual((py_result.score, py_result.priority), (11, "P0"))
        self.assertEqual((py_result.confidence, py_result.decision), ("high", "refactor_candidate"))
        self.assertEqual(
            [risk.value for risk in py_result.dominant_risks or []],
            [
                "command_layer_leak",
                "io_surface_breadth",
                "rule_helper_density",
                "mixed_responsibility_verbs",
                "stateful_side_effects",
                "mode_branching",
            ],
        )

        cpp_result = CppResponsibilityScorer(cpp).assess(
            file_path=Path("jni_bridge.cpp"),
            metrics=metrics(
                line_count=360,
                state_signal_hits=3,
                top_level_symbol_count=10,
                role_kinds=["Bridge", "Parser", "Runtime"],
                mode_branch_hits=3,
                io_kind_count=3,
                rule_helper_count=8,
                responsibility_verb_kind_count=3,
                interop_surface_hits=2,
                resource_lifecycle_hits=6,
            ),
        )
        self.assertEqual((cpp_result.score, cpp_result.priority), (12, "P0"))
        self.assertIn("interop_surface_breadth", [risk.value for risk in cpp_result.dominant_risks or []])


class LocPluginSampleTests(unittest.TestCase):
    def test_python_sample_collects_mixed_responsibility_evidence(self) -> None:
        plugin = create_responsibility_language_plugin(load_language_config(CONFIG_PATH, "py"))
        self.assertIsNotNone(plugin)
        collected = plugin.collect_metrics(
            file_path=Path("tools/repo_tooling/commands/sample.py"),
            text=PYTHON_SAMPLE,
        )
        self.assertEqual(collected.top_level_symbol_count, 9)
        self.assertEqual(collected.io_kind_count, 5)
        self.assertEqual(collected.mode_branch_hits, 1)
        self.assertGreaterEqual(collected.rule_helper_count, 3)
        self.assertGreaterEqual(collected.responsibility_verb_kind_count, 4)
        self.assertGreaterEqual(collected.command_layer_leak_hits, 4)

        details = plugin.collect_details(
            file_path=Path("tools/repo_tooling/commands/sample.py"),
            text=PYTHON_SAMPLE,
            metrics=collected,
            assessment=None,
        )
        self.assertTrue(details.function_hotspots)
        self.assertTrue(details.anchors)
        self.assertEqual(details.move_sets, [])

    def test_kotlin_and_cpp_samples_are_supported_by_the_plugin_factory(self) -> None:
        kt = create_responsibility_language_plugin(load_language_config(CONFIG_PATH, "kt"))
        kt_metrics = kt.collect_metrics(file_path=Path("Settings.kt"), text=KOTLIN_SAMPLE)
        self.assertEqual(kt_metrics.top_level_symbol_count, 3)
        self.assertEqual(kt_metrics.role_kinds, ["Section", "Card", "Switcher"])
        self.assertGreaterEqual(kt_metrics.state_signal_hits, 3)

        cpp = create_responsibility_language_plugin(load_language_config(CONFIG_PATH, "cpp"))
        cpp_metrics = cpp.collect_metrics(file_path=Path("jni_bridge.cpp"), text=CPP_SAMPLE)
        self.assertGreaterEqual(cpp_metrics.interop_surface_hits, 2)
        self.assertGreaterEqual(cpp_metrics.rule_helper_count, 3)
        self.assertGreaterEqual(cpp_metrics.responsibility_verb_kind_count, 3)

    def test_cpp_metrics_attribute_local_inc_fragments_to_the_source_owner(self) -> None:
        cpp = create_responsibility_language_plugin(load_language_config(CONFIG_PATH, "cpp"))
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            fragment = root / "operation_impl.inc"
            fragment.write_text(
                "\n".join(f"void Step{index}() {{}}" for index in range(200)),
                encoding="utf-8",
            )
            owner = root / "operation.cpp"
            owner_text = '#include "operation_impl.inc"\nvoid Run() {}\n'
            owner.write_text(owner_text, encoding="utf-8")

            collected = cpp.collect_metrics(file_path=owner, text=owner_text)

        self.assertGreaterEqual(collected.line_count, 202)
        self.assertGreaterEqual(collected.top_level_symbol_count, 201)


def responsibility_result(*, path: str, lang: str, lines: int, priority: str, hotspot_score: int) -> ResponsibilityRiskResult:
    return ResponsibilityRiskResult(
        path=path,
        lines=lines,
        score=6,
        priority=priority,
        summary="sample mixed responsibilities",
        state_signal_hits=1,
        top_level_composables=3,
        role_kinds=["Section"],
        mode_branch_hits=1,
        dominant_risks=["mode_branching"],
        function_hotspots=[
            ResponsibilityFunctionHotspot(
                name="sampleHotspot",
                kind="composable" if lang == "kt" else "function",
                start_line=10,
                end_line=30,
                score=hotspot_score,
                summary="sample hotspot",
                risks=["mode_branching"],
                evidence=["mode branch"],
            )
        ],
    )


class LocFormatterAndPolicyTests(unittest.TestCase):
    def test_kotlin_stop_and_validation_policy_are_stable(self) -> None:
        item = responsibility_result(path="ConfigThemeAppearanceDialogs.kt", lang="kt", lines=800, priority="P1", hotspot_score=2)
        self.assertIn("pause:", ResponsibilityPolicy.stop_signal(lang="kt", item=item, max_hotspot_score=2))
        hints = ResponsibilityPolicy.validation_hints(lang="kt", item=item)
        self.assertIn("ConfigThemeAppearanceSectionImportErrorTest", " ".join(hints))

    def test_formatter_consumes_extracted_cpp_policy(self) -> None:
        item = responsibility_result(path="fixtures/tests/jni_fixture.cpp", lang="cpp", lines=500, priority="P2", hotspot_score=2)
        formatted = ReportFormatter(display_name="C++", over_inclusive=False, lang="cpp").format_detail_report(
            DetailReport(
                generated_at="2026-08-02T00:00:00+08:00",
                status="ok",
                lang="cpp",
                scan=ScanSpec(mode="responsibility_risk", threshold=5),
                result=item,
            )
        )
        self.assertIn("pause:", formatted.stop_signal)
        self.assertIn("verify --build-dir build/dev --skip-android", " ".join(formatted.validation_hints))
        self.assertEqual(formatted.extraction_candidates, ())

    def test_candidate_validation_is_selected_by_policy_not_formatter(self) -> None:
        self.assertIn("scripts/loc/run.py", ResponsibilityPolicy.candidate_validation("tools/scripts/loc/internal/report_formatter.py"))
        self.assertIn("test-lib audio_core", ResponsibilityPolicy.candidate_validation("libs/audio_core/src/rules.cpp"))


if __name__ == "__main__":
    unittest.main()
