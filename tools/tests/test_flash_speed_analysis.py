from __future__ import annotations

import math
import sys
import unittest
from pathlib import Path

import numpy as np


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from repo_tooling.commands.flash_speed_analysis import (
    analyze_flash_speed_audio,
    render_speed_adjusted_pcm,
)


class FlashSpeedAnalysisTests(unittest.TestCase):
    def test_speed_renderer_expands_pcm_at_point_one_speed(self) -> None:
        source = np.array([0, 1000, -1000, 500], dtype=np.int16)

        rendered = render_speed_adjusted_pcm(source, 0.1, 48_000)

        self.assertEqual(40, rendered.size)
        self.assertEqual(np.int16, rendered.dtype)

    def test_spectral_metrics_keep_sine_frequency_near_source(self) -> None:
        sample_rate_hz = 48_000
        frequency_hz = 1200.0
        duration_seconds = 0.25
        sample_count = int(sample_rate_hz * duration_seconds)
        time = np.arange(sample_count) / sample_rate_hz
        source = np.rint(np.sin(2.0 * math.pi * frequency_hz * time) * 12000).astype(np.int16)
        rendered = render_speed_adjusted_pcm(source, 0.5, sample_rate_hz)

        metrics = analyze_flash_speed_audio(
            style="standard",
            speed=0.5,
            sample_rate_hz=sample_rate_hz,
            source_pcm=source,
            rendered_pcm=rendered,
            render_ms=1,
            max_windows=2,
            top_peaks=5,
        )

        self.assertLess(abs(metrics.dominantFreqHzBefore - frequency_hz), 5.0)
        self.assertLess(abs(metrics.dominantFreqHzAfter - frequency_hz), 20.0)
        self.assertGreaterEqual(len(metrics.topSpectralPeaksBeforeHz), 1)
        self.assertGreaterEqual(len(metrics.topSpectralPeaksAfterHz), 1)


if __name__ == "__main__":
    unittest.main()
