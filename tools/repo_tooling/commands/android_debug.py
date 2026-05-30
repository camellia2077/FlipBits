from __future__ import annotations

import argparse
from pathlib import Path

from ..constants import ROOT_DIR
from ..android_debug import (
    capture,
    crash_summary,
    encode_progress,
    flash_alignment,
    flash_visual_sweep,
    mini_alignment,
    playback_speed,
    saved_audio,
    settings_import,
    tab_navigation,
)


def _write_or_print(summary: str, output: Path | None) -> None:
    if output is None:
        print(summary)
        return
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(summary, encoding="utf-8")
    print(f"Wrote {output}")


def _read_events(log: Path, parser) -> list:
    text = log.read_text(encoding="utf-8", errors="replace")
    return [event for line in text.splitlines() if (event := parser(line)) is not None]


def _output_dir(args: argparse.Namespace, prefix: str) -> Path:
    output_dir = args.output_dir
    if output_dir is None:
        return capture.default_output_dir(prefix)
    if not output_dir.is_absolute():
        output_dir = ROOT_DIR / output_dir
    return output_dir


def cmd_android_debug(args: argparse.Namespace) -> None:
    if args.action == "flash-summary":
        events = _read_events(args.log, flash_alignment.parse_event)
        _write_or_print(flash_alignment.build_summary(events, max_rows=args.max_rows), args.output)
        return
    if args.action == "mini-summary":
        events = _read_events(args.log, mini_alignment.parse_event)
        _write_or_print(mini_alignment.build_summary(events, max_rows=args.max_rows), args.output)
        return
    if args.action == "encode-progress-summary":
        events = _read_events(args.log, encode_progress.parse_event)
        _write_or_print(encode_progress.build_summary(events, max_rows=args.max_rows), args.output)
        return
    if args.action == "playback-speed-summary":
        events = _read_events(args.log, playback_speed.parse_event)
        _write_or_print(playback_speed.build_summary(events, max_rows=args.max_rows), args.output)
        return
    if args.action == "crash-summary":
        lines = crash_summary.read_lines(args.log)
        blocks = crash_summary.extract_blocks(lines, args.context)
        crash_summary.print_summary(args.log, lines, blocks, args.max_blocks)
        return
    if args.action == "device-prep":
        capture.device_prep()
        return
    if args.action == "capture-flash":
        extras: list[str] = []
        play_ms = args.play_ms if args.play_ms is not None else (30000 if args.scenario == "ui" else 6000)
        capture.string_extra(extras, "wb.lang", args.lang)
        capture.string_extra(extras, "wb.scenario", args.scenario)
        capture.string_extra(extras, "wb.flash.style", args.style)
        capture.string_extra(extras, "wb.display", args.display)
        capture.string_extra(extras, "wb.follow.view", args.follow_view)
        capture.string_extra(extras, "wb.visual", args.visual)
        capture.string_extra(extras, "wb.input", args.input_text)
        capture.string_extra(extras, "wb.sample.length", args.sample_length)
        capture.string_extra(extras, "wb.sample.id", args.sample_id)
        capture.float_extra(extras, "wb.playback.speed", args.playback_speed)
        capture.bool_extra(extras, "wb.visual.perf_overlay", args.perf_overlay)
        capture.bool_extra(extras, "wb.encode", not args.no_encode)
        capture.bool_extra(extras, "wb.play", not args.no_play)
        capture.long_extra(extras, "wb.play.ms", play_ms)
        capture.string_extra(extras, "wb.play.end", args.play_end)
        capture.string_extra(extras, "wb.play.script", args.play_script)
        capture.capture_common(
            output_dir=_output_dir(args, f"flash-{args.scenario}-{args.style}"),
            wait_ms=args.wait_ms,
            filters=capture.FLASH_LOGCAT_FILTERS,
            event_parser=flash_alignment.parse_event,
            summary_builder=lambda events: flash_alignment.build_summary(events, max_rows=args.max_rows),
            start=lambda: capture.start_activity(capture.FLASH_ACTION, extras),
        )
        return
    if args.action == "capture-flash-visual-sweep":
        output_dir = _output_dir(args, f"flash-visual-sweep-{args.style}")
        output_dir.mkdir(parents=True, exist_ok=True)
        captures: list[flash_visual_sweep.ModeCapture] = []
        for mode in ("lanes", "pitch", "pulse"):
            mode_dir = output_dir / mode
            extras = []
            capture.string_extra(extras, "wb.scenario", "ui")
            capture.string_extra(extras, "wb.flash.style", args.style)
            capture.string_extra(extras, "wb.display", args.display)
            capture.string_extra(extras, "wb.visual", mode)
            capture.string_extra(extras, "wb.sample.length", args.sample_length)
            capture.string_extra(extras, "wb.sample.id", args.sample_id)
            capture.float_extra(extras, "wb.playback.speed", args.playback_speed)
            capture.bool_extra(extras, "wb.visual.perf_overlay", True)
            capture.bool_extra(extras, "wb.encode", True)
            capture.bool_extra(extras, "wb.play", True)
            capture.long_extra(extras, "wb.play.ms", args.play_ms)
            capture.string_extra(extras, "wb.seek.fractions", args.seek_fractions)
            capture.long_extra(extras, "wb.seek.start.ms", args.seek_start_ms)
            capture.long_extra(extras, "wb.seek.drag.ms", args.seek_drag_ms)
            capture.long_extra(extras, "wb.seek.step.ms", args.seek_step_ms)
            capture.long_extra(extras, "wb.seek.settle.ms", args.seek_settle_ms)
            result = capture.capture_common(
                output_dir=mode_dir,
                wait_ms=args.wait_ms,
                filters=capture.FLASH_LOGCAT_FILTERS,
                event_parser=flash_alignment.parse_event,
                summary_builder=lambda events, max_rows=args.max_rows: flash_alignment.build_summary(events, max_rows=max_rows),
                start=lambda extras=extras: capture.start_activity(capture.FLASH_ACTION, extras),
            )
            captures.append(
                flash_visual_sweep.ModeCapture(
                    mode=mode,
                    raw_log=result.raw_log,
                    summary=result.summary,
                    crash_summary=result.crash_summary,
                )
            )
        summary_path = output_dir / "summary.md"
        _write_or_print(flash_visual_sweep.build_sweep_summary(captures), summary_path)
        return
    if args.action == "capture-mini":
        extras = []
        capture.string_extra(extras, "wb.lang", args.lang)
        capture.string_extra(extras, "wb.scenario", args.scenario)
        capture.string_extra(extras, "wb.mini.speed", args.speed)
        capture.string_extra(extras, "wb.input", args.input_text)
        capture.string_extra(extras, "wb.display", args.display)
        capture.string_extra(extras, "wb.morse.visual", args.morse_visual)
        capture.bool_extra(extras, "wb.lyrics.expand", args.expand_lyrics)
        capture.bool_extra(extras, "wb.visual.perf_overlay", args.perf_overlay)
        capture.bool_extra(extras, "wb.encode", not args.no_encode)
        capture.bool_extra(extras, "wb.play", not args.no_play)
        capture.long_extra(extras, "wb.play.ms", args.play_ms)
        capture.string_extra(extras, "wb.play.end", args.play_end)
        capture.string_extra(extras, "wb.play.script", args.play_script)
        capture.capture_common(
            output_dir=_output_dir(args, f"mini-{args.speed}"),
            wait_ms=args.wait_ms,
            filters=capture.MINI_LOGCAT_FILTERS,
            event_parser=mini_alignment.parse_event,
            summary_builder=lambda events: mini_alignment.build_summary(events, max_rows=args.max_rows),
            start=lambda: capture.start_activity(capture.MINI_ACTION, extras),
        )
        return
    if args.action == "capture-encode-progress":
        extras = []
        capture.string_extra(extras, "wb.lang", args.lang)
        capture.string_extra(extras, "wb.mode", args.mode)
        capture.string_extra(extras, "wb.mini.speed", args.speed)
        capture.string_extra(extras, "wb.input", args.input_text)
        capture.string_extra(extras, "wb.sample.length", args.sample_length)
        capture.string_extra(extras, "wb.sample.id", args.sample_id)
        capture.int_extra(extras, "wb.repeat", args.repeat)
        capture.long_extra(extras, "wb.capture.ms", args.capture_ms)
        capture.long_extra(extras, "wb.poll.ms", args.poll_ms)
        capture.bool_extra(extras, "wb.encode", not args.no_encode)
        capture.capture_common(
            output_dir=_output_dir(args, f"encode-progress-{args.mode}"),
            wait_ms=args.wait_ms,
            filters=capture.ENCODE_PROGRESS_LOGCAT_FILTERS,
            event_parser=encode_progress.parse_event,
            summary_builder=lambda events: encode_progress.build_summary(events, max_rows=args.max_rows),
            start=lambda: capture.start_activity(capture.ENCODE_PROGRESS_ACTION, extras),
        )
        return
    if args.action == "capture-playback-speed":
        capture.capture_manual_logcat_window(
            output_dir=_output_dir(args, "playback-speed"),
            wait_ms=args.wait_ms,
            filters=capture.PLAYBACK_SPEED_LOGCAT_FILTERS,
            event_parser=playback_speed.parse_event,
            summary_builder=lambda events: playback_speed.build_summary(events, max_rows=args.max_rows),
        )
        return
    if args.action == "capture-saved":
        extras = []
        capture.string_extra(extras, "wb.saved.item_id", args.item_id)
        capture.string_extra(extras, "wb.saved.display_name", args.display_name)
        capture.long_extra(extras, "wb.saved.seed_duration_ms", args.seed_duration_ms)
        capture.string_extra(extras, "wb.saved.seed_mode", args.seed_mode)
        capture.bool_extra(extras, "wb.saved.decode", args.decode)
        capture.capture_common(
            output_dir=_output_dir(args, "saved-audio"),
            wait_ms=args.wait_ms,
            filters=capture.SAVED_AUDIO_LOGCAT_FILTERS,
            event_parser=saved_audio.parse_event,
            summary_builder=saved_audio.build_summary,
            start=lambda: capture.start_activity(capture.SAVED_AUDIO_ACTION, extras),
        )
        return
    if args.action == "capture-tab":
        extras = []
        capture.string_extra(extras, "wb.tab", args.tab)
        capture.string_extra(extras, "wb.lang", args.lang)
        capture.capture_common(
            output_dir=_output_dir(args, f"tab-{args.tab}"),
            wait_ms=args.wait_ms,
            filters=capture.TAB_LOGCAT_FILTERS,
            event_parser=tab_navigation.parse_event,
            summary_builder=tab_navigation.build_summary,
            start=lambda: capture.start_activity(capture.APP_TAB_ACTION, extras),
        )
        return
    if args.action == "capture-settings-import":
        extras = []
        capture.string_extra(extras, "wb.lang", args.lang)
        capture.string_extra(extras, "wb.tab", "settings")
        capture.string_extra(extras, "wb.import.confirm", args.confirm)
        capture.string_extra(extras, "wb.import.scope", args.scope)
        capture.capture_common(
            output_dir=_output_dir(args, f"settings-import-{args.scope}-{args.confirm}"),
            wait_ms=args.wait_ms,
            filters=capture.SETTINGS_IMPORT_LOGCAT_FILTERS,
            event_parser=settings_import.parse_event,
            summary_builder=settings_import.build_summary,
            start=lambda: capture.start_activity(capture.APP_TAB_ACTION, extras),
        )
        return
    raise AssertionError(f"Unhandled android-debug action: {args.action}")
