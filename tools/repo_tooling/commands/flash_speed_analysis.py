from __future__ import annotations

import argparse
import csv
import json
import math
import sys
import time
import wave
from array import array
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any

from .roundtrip import ensure_cli_binary
from ..artifacts import slugify, test_artifacts_root, unique_directory, write_utf8
from ..constants import DEFAULT_GENERATOR, ROOT_DIR
from ..errors import ToolError
from ..paths import resolve_build_dir
from ..process import run_capture


FLASH_STYLES = ("standard", "hostility", "litany", "collapse", "zeal", "void")
DEFAULT_FLASH_SPEED_ANALYSIS_TEXT = "Flash speed analysis 0123456789"

SPEED_ADJUSTED_RENDER_WINDOW_SECONDS = 0.030
MIN_SPEED_ADJUSTED_RENDER_WINDOW_SAMPLES = 256
MAX_SPEED_ADJUSTED_RENDER_WINDOW_SAMPLES = 2048
DEFAULT_SAMPLE_RATE_HZ = 48_000
MINIMUM_RENDER_WEIGHT = 0.000001


@dataclass
class FlashSpeedAudioMetrics:
    style: str
    speed: float
    sampleRateHz: int
    sourceSamples: int
    renderedSamples: int
    sourceDurationMs: int
    renderedDurationMs: int
    renderMs: int
    dominantFreqHzBefore: float
    dominantFreqHzAfter: float
    freqDeltaPct: float
    spectralCentroidBefore: float
    spectralCentroidAfter: float
    centroidDeltaPct: float
    topSpectralPeaksBeforeHz: list[float]
    topSpectralPeaksAfterHz: list[float]
    rmsBefore: float
    rmsAfter: float
    peakBefore: int
    peakAfter: int


def cmd_flash_speed_analysis(args: argparse.Namespace) -> None:
    speed = _validate_speed(args.speed)
    styles = _resolve_styles(args.style)
    build_dir = resolve_build_dir(args.build_dir)
    cli_binary = ensure_cli_binary(
        argparse.Namespace(
            build_dir=str(build_dir),
            generator=args.generator,
        ),
        build_dir,
    )

    output_dir = _resolve_output_dir(args, build_dir, speed)
    write_utf8(output_dir / "input.txt", args.text)

    source_dir = output_dir / "source"
    rendered_dir = output_dir / "rendered"
    logs_dir = output_dir / "logs"
    source_dir.mkdir(parents=True, exist_ok=True)
    rendered_dir.mkdir(parents=True, exist_ok=True)
    logs_dir.mkdir(parents=True, exist_ok=True)

    metrics: list[FlashSpeedAudioMetrics] = []
    for style in styles:
        wav_path = source_dir / f"flash-{style}.wav"
        encode = run_capture(
            [
                str(cli_binary),
                "encode",
                "--mode",
                "flash",
                "--flash-style",
                style,
                "--text",
                args.text,
                "--out",
                str(wav_path),
            ]
        )
        write_utf8(logs_dir / f"{style}.encode.stdout.txt", encode.stdout)
        write_utf8(logs_dir / f"{style}.encode.stderr.txt", encode.stderr)
        if encode.returncode != 0:
            raise ToolError(f"Flash encode failed for style `{style}`. See {logs_dir}.")

        sample_rate_hz, source_pcm = read_mono_pcm16_wav(wav_path)
        render_start = time.perf_counter()
        rendered_pcm = render_speed_adjusted_pcm(source_pcm, speed, sample_rate_hz)
        render_ms = int(round((time.perf_counter() - render_start) * 1000.0))
        if args.write_rendered_wav:
            write_mono_pcm16_wav(rendered_dir / f"flash-{style}-{_speed_label(speed)}x.wav", sample_rate_hz, rendered_pcm)

        metrics.append(
            analyze_flash_speed_audio(
                style=style,
                speed=speed,
                sample_rate_hz=sample_rate_hz,
                source_pcm=source_pcm,
                rendered_pcm=rendered_pcm,
                render_ms=render_ms,
                max_windows=args.max_windows,
                top_peaks=args.top_peaks,
            )
        )

    write_metrics(output_dir, metrics, speed, styles, args.text, args.write_rendered_wav)
    print(f"Flash speed audio analysis: {output_dir}")
    print(f"Summary: {output_dir / 'summary.md'}")


def _validate_speed(speed: float) -> float:
    if not math.isfinite(speed) or speed <= 0:
        raise ToolError("--speed must be a positive finite number.")
    return min(max(speed, 0.1), 4.0)


def _resolve_styles(styles: list[str] | None) -> list[str]:
    if not styles:
        return list(FLASH_STYLES)
    resolved: list[str] = []
    for style in styles:
        if style not in FLASH_STYLES:
            raise ToolError(f"Unsupported Flash style `{style}`.")
        if style not in resolved:
            resolved.append(style)
    return resolved


def _resolve_output_dir(args: argparse.Namespace, build_dir: Path, speed: float) -> Path:
    if args.out_dir:
        output_dir = Path(args.out_dir)
        if not output_dir.is_absolute():
            output_dir = ROOT_DIR / output_dir
        output_dir.mkdir(parents=True, exist_ok=True)
        return output_dir

    case_name = args.case_name or f"flash-speed-{_speed_label(speed)}x"
    return unique_directory(test_artifacts_root(build_dir) / "flash-speed-analysis" / slugify(case_name))


def read_mono_pcm16_wav(path: Path) -> tuple[int, "Any"]:
    with wave.open(str(path), "rb") as wav:
        channels = wav.getnchannels()
        sample_width = wav.getsampwidth()
        sample_rate_hz = wav.getframerate()
        frame_count = wav.getnframes()
        raw = wav.readframes(frame_count)
    if sample_width != 2:
        raise ToolError(f"Expected PCM16 WAV for {path}, got sample width {sample_width}.")
    samples = array("h")
    samples.frombytes(raw)
    if sys.byteorder != "little":
        samples.byteswap()
    if channels == 1:
        return sample_rate_hz, _np_array(samples)
    if channels <= 0:
        raise ToolError(f"Invalid channel count {channels} in {path}.")
    mono = array("h", (samples[index] for index in range(0, len(samples), channels)))
    return sample_rate_hz, _np_array(mono)


def write_mono_pcm16_wav(path: Path, sample_rate_hz: int, pcm: "Any") -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    clipped = _np().clip(pcm, -32768, 32767).astype(_np().int16)
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate_hz)
        wav.writeframes(clipped.tobytes())


def render_speed_adjusted_pcm(source_pcm: "Any", playback_speed: float, sample_rate_hz: int) -> "Any":
    np = _np()
    source = np.asarray(source_pcm, dtype=np.float64)
    if source.size == 0:
        return np.array([], dtype=np.int16)
    output_length = max(1, int(math.ceil(source.size / playback_speed)))
    if source.size == 1:
        return np.full(output_length, int(source[0]), dtype=np.int16)

    window_size = _speed_adjusted_render_window_size(sample_rate_hz, source.size)
    synthesis_hop = max(1, window_size // 2)
    analysis_hop = max(1, _round_half_up(synthesis_hop * playback_speed))
    output = np.zeros(output_length, dtype=np.float64)
    weights = np.zeros(output_length, dtype=np.float64)
    window = np.hanning(window_size)

    window_number = 0
    output_offset = 0
    source_last_index = source.size - 1
    while output_offset < output_length:
        source_offset = min(window_number * analysis_hop, source_last_index)
        local_end = min(output_length - output_offset, window_size)
        if local_end > 0:
            target_slice = slice(output_offset, output_offset + local_end)
            source_indices = np.minimum(source_offset + np.arange(local_end), source_last_index)
            window_slice = window[:local_end]
            output[target_slice] += source[source_indices] * window_slice
            weights[target_slice] += window_slice
        window_number += 1
        output_offset += synthesis_hop

    absolute_indices = np.arange(output_length)
    fallback_indices = np.clip(np.floor(absolute_indices * playback_speed + 0.5).astype(np.int64), 0, source_last_index)
    rendered = np.where(weights > MINIMUM_RENDER_WEIGHT, output / np.maximum(weights, MINIMUM_RENDER_WEIGHT), source[fallback_indices])
    return np.clip(np.rint(rendered), -32768, 32767).astype(np.int16)


def _speed_adjusted_render_window_size(sample_rate_hz: int, source_sample_count: int) -> int:
    target = _round_half_up(max(sample_rate_hz, DEFAULT_SAMPLE_RATE_HZ) * SPEED_ADJUSTED_RENDER_WINDOW_SECONDS)
    even_target = target if target % 2 == 0 else target + 1
    window_size = min(max(even_target, MIN_SPEED_ADJUSTED_RENDER_WINDOW_SAMPLES), MAX_SPEED_ADJUSTED_RENDER_WINDOW_SAMPLES)
    window_size = min(window_size, max(source_sample_count, 2))
    return window_size if window_size % 2 == 0 else max(window_size - 1, 2)


def analyze_flash_speed_audio(
    *,
    style: str,
    speed: float,
    sample_rate_hz: int,
    source_pcm: "Any",
    rendered_pcm: "Any",
    render_ms: int,
    max_windows: int,
    top_peaks: int,
) -> FlashSpeedAudioMetrics:
    source_spectrum = spectrum_metrics(source_pcm, sample_rate_hz, max_windows=max_windows, top_peaks=top_peaks)
    rendered_spectrum = spectrum_metrics(rendered_pcm, sample_rate_hz, max_windows=max_windows, top_peaks=top_peaks)
    rms_before, peak_before = amplitude_metrics(source_pcm)
    rms_after, peak_after = amplitude_metrics(rendered_pcm)
    return FlashSpeedAudioMetrics(
        style=style,
        speed=speed,
        sampleRateHz=sample_rate_hz,
        sourceSamples=int(source_pcm.size),
        renderedSamples=int(rendered_pcm.size),
        sourceDurationMs=_duration_ms(source_pcm.size, sample_rate_hz),
        renderedDurationMs=_duration_ms(rendered_pcm.size, sample_rate_hz),
        renderMs=render_ms,
        dominantFreqHzBefore=source_spectrum["dominantFreqHz"],
        dominantFreqHzAfter=rendered_spectrum["dominantFreqHz"],
        freqDeltaPct=_delta_pct(source_spectrum["dominantFreqHz"], rendered_spectrum["dominantFreqHz"]),
        spectralCentroidBefore=source_spectrum["spectralCentroidHz"],
        spectralCentroidAfter=rendered_spectrum["spectralCentroidHz"],
        centroidDeltaPct=_delta_pct(source_spectrum["spectralCentroidHz"], rendered_spectrum["spectralCentroidHz"]),
        topSpectralPeaksBeforeHz=source_spectrum["topPeaksHz"],
        topSpectralPeaksAfterHz=rendered_spectrum["topPeaksHz"],
        rmsBefore=rms_before,
        rmsAfter=rms_after,
        peakBefore=peak_before,
        peakAfter=peak_after,
    )


def spectrum_metrics(pcm: "Any", sample_rate_hz: int, *, max_windows: int, top_peaks: int) -> dict[str, Any]:
    np = _np()
    samples = np.asarray(pcm, dtype=np.float64)
    if samples.size == 0:
        return {"dominantFreqHz": 0.0, "spectralCentroidHz": 0.0, "topPeaksHz": []}

    peak = float(np.max(np.abs(samples)))
    if peak <= 0.0:
        return {"dominantFreqHz": 0.0, "spectralCentroidHz": 0.0, "topPeaksHz": []}

    window_size = min(65_536, samples.size)
    if window_size < 256:
        window_size = samples.size
    active = np.flatnonzero(np.abs(samples) >= max(256.0, peak * 0.02))
    if active.size == 0:
        start_indices = np.array([0], dtype=np.int64)
    else:
        first = int(max(0, active[0] - window_size // 2))
        last = int(min(max(0, samples.size - window_size), active[-1]))
        count = max(1, min(max_windows, 1 + (last - first) // max(1, window_size)))
        start_indices = np.linspace(first, last, num=count, dtype=np.int64)

    power_sum = None
    frequencies = np.fft.rfftfreq(window_size, d=1.0 / sample_rate_hz)
    analysis_mask = (frequencies >= 20.0) & (frequencies <= min(sample_rate_hz / 2.0, 12_000.0))
    window = np.hanning(window_size)
    for start in start_indices:
        segment = samples[int(start) : int(start) + window_size]
        if segment.size < window_size:
            segment = np.pad(segment, (0, window_size - segment.size))
        segment = (segment - np.mean(segment)) * window
        power = np.abs(np.fft.rfft(segment)) ** 2
        power_sum = power if power_sum is None else power_sum + power

    assert power_sum is not None
    power = power_sum[analysis_mask]
    freqs = frequencies[analysis_mask]
    total_power = float(np.sum(power))
    if total_power <= 0.0:
        return {"dominantFreqHz": 0.0, "spectralCentroidHz": 0.0, "topPeaksHz": []}
    dominant_freq_hz = float(freqs[int(np.argmax(power))])
    spectral_centroid_hz = float(np.sum(freqs * power) / total_power)
    return {
        "dominantFreqHz": dominant_freq_hz,
        "spectralCentroidHz": spectral_centroid_hz,
        "topPeaksHz": top_spectral_peaks(freqs, power, count=top_peaks),
    }


def top_spectral_peaks(freqs: "Any", power: "Any", *, count: int) -> list[float]:
    if count <= 0 or power.size == 0:
        return []
    np = _np()
    remaining = np.asarray(power, dtype=np.float64).copy()
    selected: list[float] = []
    min_separation_hz = 20.0
    while len(selected) < count:
        index = int(np.argmax(remaining))
        peak_power = float(remaining[index])
        if peak_power <= 0.0:
            break
        peak_freq = float(freqs[index])
        selected.append(peak_freq)
        nearby = np.abs(freqs - peak_freq) <= min_separation_hz
        remaining[nearby] = 0.0
    return selected


def amplitude_metrics(pcm: "Any") -> tuple[float, int]:
    np = _np()
    samples = np.asarray(pcm, dtype=np.float64)
    if samples.size == 0:
        return 0.0, 0
    rms = float(np.sqrt(np.mean(samples * samples)))
    peak = int(np.max(np.abs(samples)))
    return rms, peak


def write_metrics(
    output_dir: Path,
    metrics: list[FlashSpeedAudioMetrics],
    speed: float,
    styles: list[str],
    text: str,
    write_rendered_wav: bool,
) -> None:
    metric_dicts = [asdict(metric) for metric in metrics]
    json_payload = {
        "mode": "flash",
        "speed": speed,
        "styles": styles,
        "text": text,
        "source": "CLI-generated Flash WAV PCM",
        "renderer": "host mirror of Android SpeedAdjustedPcmRenderer",
        "renderedWavWritten": write_rendered_wav,
        "metrics": metric_dicts,
    }
    (output_dir / "metrics.json").write_text(json.dumps(json_payload, indent=2) + "\n", encoding="utf-8")

    with (output_dir / "metrics.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(metric_dicts[0].keys()))
        writer.writeheader()
        writer.writerows(metric_dicts)

    write_utf8(output_dir / "summary.md", build_summary(metrics, speed, text, write_rendered_wav))


def build_summary(metrics: list[FlashSpeedAudioMetrics], speed: float, text: str, write_rendered_wav: bool) -> str:
    sorted_metrics = sorted(metrics, key=lambda metric: abs(metric.centroidDeltaPct), reverse=True)
    lines = [
        "# Flash Speed Audio Analysis",
        "",
        f"- speed: `{_speed_label(speed)}x`",
        f"- text: `{text}`",
        "- source: CLI-generated Flash WAV PCM",
        "- renderer: host mirror of Android `SpeedAdjustedPcmRenderer`",
        f"- rendered WAV: `{'written' if write_rendered_wav else 'not written'}`",
        "",
        "| style | sourceSamples | renderedSamples | renderMs | dominantBeforeHz | dominantAfterHz | freqDeltaPct | centroidBeforeHz | centroidAfterHz | centroidDeltaPct | rmsBefore | rmsAfter | peakBefore | peakAfter |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for metric in sorted_metrics:
        lines.append(
            "| "
            + " | ".join(
                [
                    metric.style,
                    str(metric.sourceSamples),
                    str(metric.renderedSamples),
                    str(metric.renderMs),
                    _fmt(metric.dominantFreqHzBefore),
                    _fmt(metric.dominantFreqHzAfter),
                    _fmt(metric.freqDeltaPct),
                    _fmt(metric.spectralCentroidBefore),
                    _fmt(metric.spectralCentroidAfter),
                    _fmt(metric.centroidDeltaPct),
                    _fmt(metric.rmsBefore),
                    _fmt(metric.rmsAfter),
                    str(metric.peakBefore),
                    str(metric.peakAfter),
                ]
            )
            + " |"
        )
    lines.extend(
        [
            "",
            "## Top Spectral Peaks",
            "",
            "| style | beforeHz | afterHz |",
            "|---|---:|---:|",
        ]
    )
    for metric in sorted_metrics:
        lines.append(
            f"| {metric.style} | {_fmt_peaks(metric.topSpectralPeaksBeforeHz)} | "
            f"{_fmt_peaks(metric.topSpectralPeaksAfterHz)} |"
        )
    lines.extend(
        [
            "",
            "Interpretation notes:",
            "",
            "- Treat `hostility` and `zeal` as subjective comparison samples, not confirmed-good controls.",
            "- Use this report before changing Flash speed rendering when investigating Flash style-specific pitch perception.",
            "- If PCM metrics look stable but device playback still sounds wrong, inspect the Android playback/output chain next.",
        ]
    )
    return "\n".join(lines) + "\n"


def _np() -> Any:
    try:
        import numpy as np
    except ImportError as exc:
        raise ToolError("Flash speed audio analysis requires numpy. Install tools requirements first.") from exc
    return np


def _np_array(samples: array) -> "Any":
    return _np().frombuffer(samples, dtype=_np().int16).copy()


def _round_half_up(value: float) -> int:
    return int(math.floor(value + 0.5))


def _duration_ms(samples: int, sample_rate_hz: int) -> int:
    return int(round(samples * 1000.0 / max(1, sample_rate_hz)))


def _delta_pct(before: float, after: float) -> float:
    if abs(before) <= 0.000001:
        return 0.0
    return (after - before) * 100.0 / before


def _fmt(value: float) -> str:
    return f"{value:.2f}"


def _fmt_peaks(values: list[float]) -> str:
    return ", ".join(_fmt(value) for value in values)


def _speed_label(speed: float) -> str:
    return f"{speed:g}"


def add_flash_speed_analysis_parser(artifact_subparsers: argparse._SubParsersAction[argparse.ArgumentParser]) -> None:
    parser = artifact_subparsers.add_parser(
        "flash-speed-analysis",
        help="Compare Flash source PCM against speed-adjusted rendered PCM.",
        description=(
            "Generate Flash WAV source PCM for selected styles, render speed-adjusted PCM with a host mirror "
            "of the Android renderer, and write spectral/amplitude comparison metrics."
        ),
    )
    parser.add_argument(
        "--build-dir",
        default="build/dev",
        help="Build directory used to locate or build the FlipBits CLI. Defaults to build/dev.",
    )
    parser.add_argument(
        "--generator",
        default=DEFAULT_GENERATOR,
        help="CMake generator to use if auto-configuring/building the CLI target.",
    )
    parser.add_argument("--speed", type=float, default=0.1, help="Playback speed to render. Defaults to 0.1.")
    parser.add_argument(
        "--style",
        action="append",
        choices=FLASH_STYLES,
        help="Flash style to analyze. Repeat to choose multiple. Defaults to all six Flash styles.",
    )
    parser.add_argument(
        "--text",
        default=DEFAULT_FLASH_SPEED_ANALYSIS_TEXT,
        help="Input text encoded into each Flash style.",
    )
    parser.add_argument("--case-name", help="Optional output case directory name.")
    parser.add_argument("--out-dir", help="Optional output directory. Defaults under build/test-artifacts/.")
    parser.add_argument(
        "--max-windows",
        type=int,
        default=8,
        help="Maximum FFT windows sampled per PCM file for spectral metrics. Defaults to 8.",
    )
    parser.add_argument(
        "--top-peaks",
        type=int,
        default=5,
        help="Number of separated spectral peaks to report for source/rendered PCM. Defaults to 5.",
    )
    parser.add_argument(
        "--write-rendered-wav",
        action="store_true",
        help="Also write rendered speed-adjusted WAV files. Disabled by default to keep artifacts small.",
    )
    parser.set_defaults(func=cmd_flash_speed_analysis)
