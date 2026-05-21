from __future__ import annotations

import sys
import unittest
from pathlib import Path
from unittest.mock import patch


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from repo_tooling.constants import ROOT_DIR
from repo_tooling.errors import ToolError
from repo_tooling.history.collect import normalize_scope
from repo_tooling.history.infer import candidate_topics_for_bucket
from repo_tooling.history.model import ChangedPath


class HistoryPrepTests(unittest.TestCase):
    def test_normalize_scope_keeps_repo_relative_scope(self) -> None:
        self.assertEqual(normalize_scope("libs/audio_io"), "libs/audio_io")
        self.assertEqual(normalize_scope(r"libs\audio_io"), "libs/audio_io")

    def test_normalize_scope_converts_repo_absolute_scope(self) -> None:
        self.assertEqual(normalize_scope(str(ROOT_DIR / "libs" / "audio_io")), "libs/audio_io")

    def test_normalize_scope_rejects_absolute_scope_outside_repo(self) -> None:
        outside = Path("C:/").resolve()
        with self.assertRaises(ToolError):
            normalize_scope(str(outside))

    def test_android_topics_include_semantic_facts_and_triage(self) -> None:
        items = [
            ChangedPath("M", "apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/model/MorseCode.kt"),
            ChangedPath("M", "apps/audio_android/app/src/main/java/com/bag/audioandroid/domain/PayloadFollowViewData.kt"),
            ChangedPath("M", "apps/audio_android/app/src/main/cpp/jni_bridge_helpers.cpp"),
        ]

        diff_text = "\n".join(
            [
                "Wpm10 Wpm15 Wpm20 translateMorseNotation",
                "UltraFrameSection ultraFrameTimeline carrierFrequencyHz",
                "bag_ultra_frame_symbol_entry ultra_frame_timeline",
            ]
        )
        with patch("repo_tooling.history.infer._changed_diff_text", return_value=diff_text):
            topics = candidate_topics_for_bucket("android-app", items)
        by_title = {topic.title: topic for topic in topics}

        self.assertEqual(by_title["Mini Morse speed and notation tools"].recommendation, "history-worthy")
        self.assertIn("10 WPM", " ".join(by_title["Mini Morse speed and notation tools"].key_facts))
        self.assertEqual(by_title["Ultra full-frame visual timeline"].recommendation, "history-worthy")
        self.assertEqual(
            by_title["Android native DTO support for Ultra frame data"].recommendation,
            "supporting implementation",
        )

    def test_libs_topics_include_ultra_frame_codec_facts(self) -> None:
        items = [
            ChangedPath("M", "libs/audio_core/src/ultra/codec_impl.inc"),
            ChangedPath("M", "libs/audio_core/src/transport/follow_payload_timeline_impl.inc"),
            ChangedPath("M", "libs/audio_api/include/bag_api.h"),
        ]

        diff_text = "\n".join(
            [
                "EncodePayloadToFrame DecodeFrameToPayload kCleanFrameV1Preamble Crc16CcittFalse",
                "BuildUltraFrameTimeline payload_begin_sample carrier_freq_hz",
            ]
        )
        with patch("repo_tooling.history.infer._changed_diff_text", return_value=diff_text):
            topics = candidate_topics_for_bucket("libs/audio_core", items)
        by_title = {topic.title: topic for topic in topics}

        self.assertEqual(by_title["Ultra clean frame codec"].recommendation, "history-worthy")
        self.assertIn("CRC", " ".join(by_title["Ultra clean frame codec"].key_facts))
        self.assertIn("payload", " ".join(by_title["Ultra frame-aware follow timeline"].key_facts))


if __name__ == "__main__":
    unittest.main()
