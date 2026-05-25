from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from repo_tooling.web.emscripten import (
    cache_contains_stale_emscripten_path,
    candidate_tool_paths,
)
from repo_tooling.web.sample_texts import (
    build_sample_text_payload,
    export_sample_texts,
    values_dir_to_locale,
)


def write_xml(path: Path, entries: dict[str, str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    body = "\n".join(
        f'    <string name="{name}">{value}</string>'
        for name, value in entries.items()
    )
    path.write_text(f"<resources>\n{body}\n</resources>\n", encoding="utf-8")


class WebToolTests(unittest.TestCase):
    def test_values_dir_to_locale_matches_android_resource_qualifiers(self) -> None:
        self.assertEqual(values_dir_to_locale("values"), "en")
        self.assertEqual(values_dir_to_locale("values-zh"), "zh-CN")
        self.assertEqual(values_dir_to_locale("values-zh-rTW"), "zh-rTW")
        self.assertEqual(values_dir_to_locale("values-ja"), "ja")

    def test_build_sample_text_payload_uses_key_contract_for_lengths(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            res_root = Path(temp_dir)
            write_xml(
                res_root / "values" / "audio_samples_sacred_machine_signal_litany.xml",
                {
                    "audio_sample_sacred_machine_themed_short_alpha": "Alpha short",
                    "audio_sample_sacred_machine_themed_long_alpha": "Alpha long",
                    "unrelated_key": "Ignored",
                },
            )
            write_xml(
                res_root / "values-zh" / "audio_samples_sacred_machine_signal_litany.xml",
                {
                    "audio_sample_sacred_machine_themed_short_alpha": "甲短",
                },
            )
            write_xml(
                res_root / "values" / "audio_samples_pro_ascii_shared.xml",
                {
                    "audio_sample_pro_ascii_short_alpha": "ASCII short",
                    "audio_sample_pro_ascii_long_missing": "Ignored length mismatch",
                },
            )

            payload = build_sample_text_payload(android_res_root=res_root)

        self.assertEqual(payload["flavor"], "sacred_machine")
        sacred = payload["sacred_machine"]
        self.assertEqual(sacred["en"]["short"], [{"id": "alpha", "text": "Alpha short"}])
        self.assertEqual(sacred["en"]["long"], [{"id": "alpha", "text": "Alpha long"}])
        self.assertEqual(sacred["zh-CN"]["short"], [{"id": "alpha", "text": "甲短"}])
        ascii_shared = payload["ascii_shared"]
        self.assertEqual(ascii_shared["en"]["short"], [{"id": "alpha", "text": "ASCII short"}])
        self.assertEqual(ascii_shared["en"]["long"], [])

    def test_export_sample_texts_writes_json_payload(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_root = Path(temp_dir)
            res_root = temp_root / "res"
            output_path = temp_root / "site" / "data" / "sample-texts.json"
            write_xml(
                res_root / "values" / "audio_samples_sacred_machine_signal_litany.xml",
                {
                    "audio_sample_sacred_machine_themed_short_alpha": "Alpha short",
                },
            )
            write_xml(
                res_root / "values" / "audio_samples_pro_ascii_shared.xml",
                {
                    "audio_sample_pro_ascii_short_alpha": "ASCII short",
                },
            )

            export_sample_texts(android_res_root=res_root, output_path=output_path)
            payload = json.loads(output_path.read_text(encoding="utf-8"))

        self.assertEqual(payload["sacred_machine"]["en"]["short"][0]["text"], "Alpha short")
        self.assertEqual(payload["ascii_shared"]["en"]["short"][0]["text"], "ASCII short")

    def test_cache_contains_stale_emscripten_path_detects_wrong_cached_toolchain(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            build_dir = Path(temp_dir)
            (build_dir / "CMakeCache.txt").write_text(
                "CMAKE_TOOLCHAIN_FILE:FILEPATH=C:/old/emscripten/cmake/Modules/Platform/Emscripten.cmake\n",
                encoding="utf-8",
            )

            self.assertTrue(
                cache_contains_stale_emscripten_path(
                    build_dir,
                    Path("C:/new/emsdk/upstream/emscripten"),
                )
            )
            self.assertFalse(
                cache_contains_stale_emscripten_path(
                    build_dir,
                    Path("C:/old/emscripten"),
                )
            )

    def test_candidate_tool_paths_include_emsdk_and_emscripten_roots(self) -> None:
        candidates = candidate_tool_paths("emcmake", env={"EMSDK": "C:/emsdk"})
        rendered = {str(path).replace("\\", "/") for path in candidates}
        self.assertIn("C:/emsdk/emcmake.bat", rendered)
        self.assertIn("C:/emsdk/upstream/emscripten/emcmake.bat", rendered)


if __name__ == "__main__":
    unittest.main()
