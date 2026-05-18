from __future__ import annotations

import argparse
from pathlib import Path
import subprocess
import sys


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from repo_tooling.android_debug import capture, flash_alignment
from repo_tooling.android_debug import flash_visual_sweep
from repo_tooling.android_debug import mini_alignment
from repo_tooling.commands import android_debug


def test_start_activity_quotes_shell_extras_with_spaces(monkeypatch) -> None:
    calls: list[list[str]] = []

    def fake_run_adb(args: list[str], *, capture: bool = False, check: bool = True):
        calls.append(args)
        return subprocess.CompletedProcess(["adb", *args], 0, "", "")

    monkeypatch.setattr(capture, "run_adb", fake_run_adb)

    capture.start_activity(
        capture.FLASH_ACTION,
        ["--es", "wb.input", "flash capture smoke", "--el", "wb.play.ms", "1000"],
    )

    assert calls == [
        [
            "shell",
            "am start -n com.bag.audioandroid/.MainActivity "
            "-a com.bag.audioandroid.DEBUG_FLASH_SCENARIO "
            "--es wb.input 'flash capture smoke' --el wb.play.ms 1000",
        ]
    ]


def test_string_extra_keeps_whitespace_value() -> None:
    extras: list[str] = []
    capture.string_extra(extras, "wb.input", " \n ")
    assert extras == ["--es", "wb.input", " \n "]


def test_capture_flash_defaults_ui_play_ms_to_longer_duration(monkeypatch, tmp_path) -> None:
    started: list[tuple[str, list[str]]] = []
    captured: list[dict] = []

    def fake_start_activity(action: str, extras: list[str]) -> None:
        started.append((action, list(extras)))

    def fake_capture_common(**kwargs):
        kwargs["start"]()
        captured.append(kwargs)
        return capture.CaptureResult(
            raw_log=tmp_path / "raw.log",
            summary=tmp_path / "summary.md",
            crash_summary=tmp_path / "crash-summary.txt",
        )

    monkeypatch.setattr(capture, "start_activity", fake_start_activity)
    monkeypatch.setattr(capture, "capture_common", fake_capture_common)

    android_debug.cmd_android_debug(
        argparse.Namespace(
            action="capture-flash",
            output_dir=tmp_path,
            wait_ms=90000,
            scenario="ui",
            style="litany",
            display="mix",
            lang="ja",
            follow_view="binary",
            visual="lanes",
            input_text=None,
            sample_length="long",
            sample_id=None,
            playback_speed=0.1,
            play_ms=None,
            no_encode=False,
            no_play=False,
            max_rows=24,
        )
    )

    assert captured[0]["wait_ms"] == 90000
    assert started[0][0] == capture.FLASH_ACTION
    assert started[0][1][started[0][1].index("wb.display") + 1] == "mix"
    assert started[0][1][started[0][1].index("wb.lang") + 1] == "ja"
    assert started[0][1][started[0][1].index("wb.follow.view") + 1] == "binary"
    assert started[0][1][started[0][1].index("wb.playback.speed") + 1] == "0.1"
    assert "--el" in started[0][1]
    assert started[0][1][started[0][1].index("wb.play.ms") + 1] == "30000"


def test_capture_flash_defaults_headless_play_ms_to_short_duration(monkeypatch, tmp_path) -> None:
    started: list[list[str]] = []

    def fake_start_activity(_action: str, extras: list[str]) -> None:
        started.append(list(extras))

    def fake_capture_common(**kwargs):
        kwargs["start"]()
        return capture.CaptureResult(
            raw_log=tmp_path / "raw.log",
            summary=tmp_path / "summary.md",
            crash_summary=tmp_path / "crash-summary.txt",
        )

    monkeypatch.setattr(capture, "start_activity", fake_start_activity)
    monkeypatch.setattr(capture, "capture_common", fake_capture_common)

    android_debug.cmd_android_debug(
        argparse.Namespace(
            action="capture-flash",
            output_dir=tmp_path,
            wait_ms=12000,
            scenario="headless",
            style="litany",
            display="lyrics",
            lang=None,
            follow_view="binary",
            visual="lanes",
            input_text="flash smoke",
            sample_length=None,
            sample_id=None,
            playback_speed=1.0,
            play_ms=None,
            no_encode=False,
            no_play=False,
            max_rows=24,
        )
    )

    assert started[0][started[0].index("wb.play.ms") + 1] == "6000"
    assert started[0][started[0].index("wb.input") + 1] == "flash smoke"


def test_capture_common_writes_summary_and_crash_files_without_device(monkeypatch, tmp_path) -> None:
    started: list[bool] = []
    raw_text = (
        "05-11 15:25:06.660 D/FlashAutomation( 4229): "
        "received scenario=ui style=litany visual=ToneTracks encode=true play=true "
        "playMs=30000 requestId=158 input=text chars=14\n"
        "05-11 15:25:07.156 D/FlashAutomation( 4229): "
        "inputResolved requestId=158 source=text sampleId= chars=14 payloadBytes=14 style=litany\n"
        "05-11 15:25:10.256 D/FlashLyricsPerf( 4229): "
        "playing=true sample=60393 token=0 tokenText=flash tokenStart=59535 tokenEnd=897435 "
        "tokenProgress=0.00 displayLine=0 displayRange=0-3 sourceLine=0 "
        "sourceLineText=flash_ui_smoke byte=0 bit=0 tone=true textTokens=3 lineRanges=1\n"
    )

    monkeypatch.setattr(capture, "device_prep", lambda: None)
    monkeypatch.setattr(capture.time, "sleep", lambda _seconds: None)
    monkeypatch.setattr(capture, "run_adb", lambda *_args, **_kwargs: None)

    def fake_dump_logcat(path: Path, _filters: list[str]) -> None:
        path.write_text(raw_text, encoding="utf-8")

    monkeypatch.setattr(capture, "dump_logcat", fake_dump_logcat)

    result = capture.capture_common(
        output_dir=tmp_path,
        wait_ms=1,
        filters=capture.FLASH_LOGCAT_FILTERS,
        event_parser=flash_alignment.parse_event,
        summary_builder=lambda events: flash_alignment.build_summary(events, max_rows=8),
        start=lambda: started.append(True),
    )

    assert started == [True]
    assert result.raw_log.read_text(encoding="utf-8") == raw_text
    summary = result.summary.read_text(encoding="utf-8")
    assert "Scenario: scenario=ui style=litany" in summary
    assert "Input: source=text" in summary
    assert "Lyrics active-token rows: 1" in summary
    crash_summary = result.crash_summary.read_text(encoding="utf-8")
    assert "total_lines: 3" in crash_summary
    assert "pattern_hits:" in crash_summary


def test_capture_mini_exposes_language_display_and_expand(monkeypatch, tmp_path) -> None:
    started: list[list[str]] = []

    def fake_start_activity(_action: str, extras: list[str]) -> None:
        started.append(list(extras))

    def fake_capture_common(**kwargs):
        kwargs["start"]()
        return capture.CaptureResult(
            raw_log=tmp_path / "raw.log",
            summary=tmp_path / "summary.md",
            crash_summary=tmp_path / "crash-summary.txt",
        )

    monkeypatch.setattr(capture, "start_activity", fake_start_activity)
    monkeypatch.setattr(capture, "capture_common", fake_capture_common)

    android_debug.cmd_android_debug(
        argparse.Namespace(
            action="capture-mini",
            output_dir=tmp_path,
            wait_ms=20000,
            scenario="ui",
            speed="fast",
            lang="fr",
            display="visual",
            morse_visual="horizontal",
            expand_lyrics=True,
            perf_overlay=True,
            input_text=None,
            play_ms=6000,
            play_end="pause",
            play_script="700:pause,820:resume",
            no_encode=False,
            no_play=False,
            max_rows=24,
        )
    )

    assert started[0][started[0].index("wb.lang") + 1] == "fr"
    assert started[0][started[0].index("wb.display") + 1] == "visual"
    assert started[0][started[0].index("wb.morse.visual") + 1] == "horizontal"
    assert started[0][started[0].index("wb.lyrics.expand") + 1] == "true"
    assert started[0][started[0].index("wb.visual.perf_overlay") + 1] == "true"
    assert started[0][started[0].index("wb.play.end") + 1] == "pause"
    assert started[0][started[0].index("wb.play.script") + 1] == "700:pause,820:resume"


def test_mini_summary_includes_mini_visual_perf_rollup(tmp_path) -> None:
    raw_log = tmp_path / "mini-raw.log"
    raw_log.write_text(
        "\n".join(
            [
                "05-19 10:00:00.000 D/MiniAutomation(12345): received scenario=ui speed=standard playMs=10000 input=text text=mini_sync_test sampleLength=short",
                "05-19 10:00:00.100 D/MiniAutomation(12345): inputResolved source=text sampleId= chars=14 payloadBytes=14",
                "05-19 10:00:01.000 D/MiniVisualPerf(12345): reason=interval drawAvgMs=0.80 rawUpdate/s=44.0 rawStepMaxMs=26.7 smoothStepMaxMs=19.4 visualErrorMs=8.1 windowStepMaxMs=12.3",
                "05-19 10:00:02.000 D/MiniVisualPerf(12345): reason=interval drawAvgMs=1.10 rawUpdate/s=45.0 rawStepMaxMs=30.2 smoothStepMaxMs=21.5 visualErrorMs=-9.6 windowStepMaxMs=18.8",
                "05-19 10:00:02.100 D/MiniAlignmentPerf(12345): speed=standard playing=true frameSamples=480 visualSample=1200 lyricsSample=1195 sampleDelta=5 visualGroup=1 visualBitOffset=2 token=0 tokenText=mini tokenProgress=0.25 lyricBitOffset=3 tone=true",
            ]
        ),
        encoding="utf-8",
    )

    events = [event for line in raw_log.read_text(encoding="utf-8").splitlines() if (event := mini_alignment.parse_event(line)) is not None]
    summary = mini_alignment.build_summary(events, max_rows=8)

    assert "- MiniVisualPerf rows: 2" in summary
    assert "Latest MiniVisualPerf: reason=interval drawAvgMs=1.10 rawUpdate/s=45.0" in summary
    assert "Peak MiniVisualPerf: drawAvgMs=1.10 rawUpdate/s=45.0 rawStepMaxMs=30.2 smoothStepMaxMs=21.5 visualErrorMs(abs)=9.6 windowStepMaxMs=18.8" in summary
    assert "## Mini Visual Perf" in summary
    assert "| 05-19 10:00:02.000 | interval | 1.10 | 45.0 | 30.2 | 21.5 | -9.6 | 18.8 |" in summary


def test_capture_tab_uses_tab_action(monkeypatch, tmp_path) -> None:
    started: list[tuple[str, list[str]]] = []

    def fake_start_activity(action: str, extras: list[str]) -> None:
        started.append((action, list(extras)))

    def fake_capture_common(**kwargs):
        kwargs["start"]()
        return capture.CaptureResult(
            raw_log=tmp_path / "raw.log",
            summary=tmp_path / "summary.md",
            crash_summary=tmp_path / "crash-summary.txt",
        )

    monkeypatch.setattr(capture, "start_activity", fake_start_activity)
    monkeypatch.setattr(capture, "capture_common", fake_capture_common)

    android_debug.cmd_android_debug(
        argparse.Namespace(
            action="capture-tab",
            output_dir=tmp_path,
            wait_ms=3000,
            tab="settings",
            lang="zh",
        )
    )

    assert started == [
        (
            capture.APP_TAB_ACTION,
            ["--es", "wb.tab", "settings", "--es", "wb.lang", "zh"],
        )
    ]


def test_capture_settings_import_uses_settings_import_action(monkeypatch, tmp_path) -> None:
    started: list[tuple[str, list[str]]] = []

    def fake_start_activity(action: str, extras: list[str]) -> None:
        started.append((action, list(extras)))

    def fake_capture_common(**kwargs):
        kwargs["start"]()
        return capture.CaptureResult(
            raw_log=tmp_path / "raw.log",
            summary=tmp_path / "summary.md",
            crash_summary=tmp_path / "crash-summary.txt",
        )

    monkeypatch.setattr(capture, "start_activity", fake_start_activity)
    monkeypatch.setattr(capture, "capture_common", fake_capture_common)

    android_debug.cmd_android_debug(
        argparse.Namespace(
            action="capture-settings-import",
            output_dir=tmp_path,
            wait_ms=3000,
            lang="en",
            confirm="overwrite",
            scope="current",
        )
    )

    assert started == [
        (
            capture.APP_TAB_ACTION,
            [
                "--es",
                "wb.lang",
                "en",
                "--es",
                "wb.tab",
                "settings",
                "--es",
                "wb.import.confirm",
                "overwrite",
                "--es",
                "wb.import.scope",
                "current",
            ],
        )
    ]


def test_capture_settings_import_accepts_batch_scope(monkeypatch, tmp_path) -> None:
    started: list[tuple[str, list[str]]] = []

    def fake_start_activity(action: str, extras: list[str]) -> None:
        started.append((action, list(extras)))

    def fake_capture_common(**kwargs):
        kwargs["start"]()
        return capture.CaptureResult(
            raw_log=tmp_path / "raw.log",
            summary=tmp_path / "summary.md",
            crash_summary=tmp_path / "crash-summary.txt",
        )

    monkeypatch.setattr(capture, "start_activity", fake_start_activity)
    monkeypatch.setattr(capture, "capture_common", fake_capture_common)

    android_debug.cmd_android_debug(
        argparse.Namespace(
            action="capture-settings-import",
            output_dir=tmp_path,
            wait_ms=3000,
            lang=None,
            confirm="none",
            scope="all",
        )
    )

    assert started == [
        (
            capture.APP_TAB_ACTION,
            ["--es", "wb.tab", "settings", "--es", "wb.import.confirm", "none", "--es", "wb.import.scope", "all"],
        )
    ]


def test_capture_flash_visual_sweep_runs_all_modes(monkeypatch, tmp_path) -> None:
    started: list[tuple[str, str]] = []

    def fake_start_activity(action: str, extras: list[str]) -> None:
        mode = extras[extras.index("wb.visual") + 1]
        started.append((action, mode))

    def fake_capture_common(**kwargs):
        kwargs["output_dir"].mkdir(parents=True, exist_ok=True)
        raw_log = kwargs["output_dir"] / "raw.log"
        raw_log.write_text(
            "05-14 06:28:58.462 D/FlashAutomation(24394): received scenario=ui style=standard visual=lanes playMs=14000 input=sample sampleLength=long\n",
            encoding="utf-8",
        )
        summary = kwargs["output_dir"] / "summary.md"
        summary.write_text("mode summary", encoding="utf-8")
        crash_summary = kwargs["output_dir"] / "crash-summary.txt"
        crash_summary.write_text("", encoding="utf-8")
        kwargs["start"]()
        return capture.CaptureResult(raw_log=raw_log, summary=summary, crash_summary=crash_summary)

    monkeypatch.setattr(capture, "start_activity", fake_start_activity)
    monkeypatch.setattr(capture, "capture_common", fake_capture_common)

    android_debug.cmd_android_debug(
        argparse.Namespace(
            action="capture-flash-visual-sweep",
            output_dir=tmp_path,
            wait_ms=1000,
            style="standard",
            display="visual",
            sample_length="long",
            sample_id=None,
            playback_speed=1.0,
            play_ms=14000,
            seek_fractions="0.15,0.35,0.55,0.75",
            seek_start_ms=1500,
            seek_drag_ms=480,
            seek_step_ms=16,
            seek_settle_ms=700,
            max_rows=24,
        )
    )

    assert started == [
        (capture.FLASH_ACTION, "lanes"),
        (capture.FLASH_ACTION, "pitch"),
        (capture.FLASH_ACTION, "pulse"),
    ]
    assert (tmp_path / "summary.md").exists()


def test_flash_visual_sweep_summary_extracts_basic_metrics(tmp_path) -> None:
    raw_log = tmp_path / "raw.log"
    raw_log.write_text(
        "\n".join(
            [
                "05-14 06:28:58.462 D/FlashVisualPerf(24394): mode=Lanes playing=true drawAvgMs=1.25 drawMaxMs=3.50 primitives=216 visiblePrimitives=36 visualErrorMs=-4.25",
                "05-14 06:28:59.552 D/FlashLyricsPerf(24394): previewMotion sample=1135527 token=9 scrubbing=true direct=true resolvedTx=0.0 targetTx=0.0 deltaTx=0.0",
                "05-14 06:28:59.640 D/FlashVisualPerf(24394): forced reason=seek-immediate requestId=1 index=0 fraction=0.150 target=1135527 displayed=1135527 raw=1135527 smooth=1135527 readoutSample=1135527",
                "05-14 06:29:00.351 D/FlashVisualPerf(24394): forced reason=seek-settled requestId=1 index=0 fraction=0.150 target=1135527 displayed=1165906 raw=1165137 smooth=1165906 readoutSample=1165906",
            ]
        ),
        encoding="utf-8",
    )
    summary = flash_visual_sweep.build_sweep_summary(
        [
            flash_visual_sweep.ModeCapture(
                mode="lanes",
                raw_log=raw_log,
                summary=tmp_path / "summary.md",
                crash_summary=tmp_path / "crash-summary.txt",
            )
        ]
    )

    assert "| lanes |" in summary
    assert "216" in summary
    assert "0.00" in summary
    assert "30379" in summary
