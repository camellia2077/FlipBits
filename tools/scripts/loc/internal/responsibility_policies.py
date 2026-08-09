from __future__ import annotations

from pathlib import Path

from .responsibility_analyzers import ResponsibilityRiskResult


class ResponsibilityPolicy:
    """Language-specific refactoring guidance kept outside presentation code."""

    @staticmethod
    def is_test_path(path: str) -> bool:
        normalized = path.replace("\\", "/").lower()
        return "/test/" in normalized or normalized.endswith("/test") or "/tests/" in normalized

    @classmethod
    def stop_signal(cls, *, lang: str, item: ResponsibilityRiskResult, max_hotspot_score: int) -> str | None:
        if lang == "cpp":
            if cls.is_test_path(item.path) and item.priority in {"P2", "P3"}:
                return "pause: test files often centralize fixture helpers; split only when a test owner becomes hard to read."
            if item.priority == "P0" or max_hotspot_score >= 4:
                return "continue: choose one C++ extraction candidate with a clear owner boundary and keep behavior unchanged."
            if item.lines <= 650 and max_hotspot_score <= 2:
                return "pause: remaining C++ hotspots are modest; continue only for a named behavior change."
            return "review manually: split only where the candidate maps to an existing module or platform boundary."
        if lang != "kt":
            return None
        path_name = Path(item.path).name
        if path_name.endswith("Dialogs.kt") and item.lines <= 900:
            return "pause: dialog/import/export code is a coherent responsibility; avoid splitting only to reduce line count."
        if item.lines <= 750 and max_hotspot_score <= 2:
            return "pause: file is still flagged, but remaining hotspots are modest; continue only for a named behavior change."
        if path_name.endswith("State.kt") and item.lines <= 900 and max_hotspot_score <= 3:
            return "pause: state orchestration has already been narrowed; prefer moving to the next larger file."
        if item.lines >= 1200 or max_hotspot_score >= 3:
            return "continue: choose one extraction candidate, keep behavior unchanged, and validate immediately."
        return "review manually: only continue if a candidate has a clear file boundary and low dependency surface."

    @staticmethod
    def validation_hints(*, lang: str, item: ResponsibilityRiskResult) -> tuple[str, ...]:
        if lang == "kt":
            hints = ["android compileDebugKotlin or :app:compileDebugKotlin --rerun-tasks"]
            path_name = Path(item.path).name
            if "ConfigThemeAppearance" in path_name:
                hints.append("run ConfigThemeAppearanceSectionImportErrorTest when import/export helpers move")
                hints.append("run compileDebugUnitTestKotlin if internal test helpers move")
            if "Flash" in path_name or "Playback" in path_name:
                hints.append("for visual/playback changes, prefer debug device check or focused existing UI tests")
            return tuple(hints)
        if lang == "py":
            return ("run the focused unit tests for the moved helper module",)
        if lang == "cpp":
            path = item.path.replace("\\", "/")
            if "apps/audio_android/app/src/main/cpp" in path:
                return (
                    "python tools/run.py android assemble-debug",
                    "python tools/run.py android test-debug when JNI-facing state or DTO shape changes",
                )
            if "libs/audio_api" in path:
                return ("python tools/run.py test-lib audio_api --build-dir build/dev",)
            if "libs/audio_core" in path:
                return (
                    "python tools/run.py test-lib audio_core --build-dir build/dev",
                    "python tools/run.py test-lib audio_api --build-dir build/dev when C ABI behavior is affected",
                )
            if "libs/audio_io" in path:
                return ("python tools/run.py test-lib audio_io --build-dir build/dev",)
            return ("python tools/run.py verify --build-dir build/dev --skip-android",)
        return ()

    @staticmethod
    def candidate_validation(path: str) -> str:
        normalized_path = path.replace("\\", "/")
        if "tools/repo_tooling/commands/web.py" in normalized_path or "tools/repo_tooling/web/" in normalized_path:
            return "python -m unittest tools.tests.test_web_tools; python tools/run.py web test"
        if "tools/repo_tooling/android_debug/" in normalized_path:
            return "python -m unittest tools.tests.test_android_debug"
        if "tools/scripts/loc/" in normalized_path:
            return "python tools/scripts/loc/run.py --lang py --responsibility-risk"
        if "apps/audio_android/app/src/main/cpp" in normalized_path:
            return "python tools/run.py android assemble-debug"
        if "libs/audio_api" in normalized_path:
            return "python tools/run.py test-lib audio_api --build-dir build/dev"
        if "libs/audio_core" in normalized_path:
            return "python tools/run.py test-lib audio_core --build-dir build/dev"
        if "libs/audio_io" in normalized_path:
            return "python tools/run.py test-lib audio_io --build-dir build/dev"
        path_name = Path(path).name
        if "ConfigThemeAppearance" in path_name:
            return "compileDebugKotlin; if import/export moved, run ConfigThemeAppearanceSectionImportErrorTest"
        if "Flash" in path_name:
            return "compileDebugKotlin; verify Flash visual playback path if canvas/runtime code moved"
        if "Playback" in path_name:
            return "compileDebugKotlin; run compileDebugUnitTestKotlin after test-facing helper moves"
        return "compile the owning module and run focused tests if helpers are test-visible"
