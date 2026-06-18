from __future__ import annotations

from ..constants import ROOT_DIR


WEB_ROOT = ROOT_DIR / "apps" / "audio_web"
WEB_SITE_DIR = WEB_ROOT / "site"
WEB_TEST_ROOT = ROOT_DIR / "tools" / "web"
WEB_TEST_ARTIFACT_ROOT = ROOT_DIR / "build" / "test-artifacts" / "web"
WEB_PERF_ARTIFACT_ROOT = ROOT_DIR / "build" / "perf-artifacts" / "web"
WEB_NODE_CACHE_ROOT = ROOT_DIR / "build" / "web-node"
ANDROID_RES_ROOT = ROOT_DIR / "apps" / "audio_android" / "app" / "src" / "main" / "res"
SAMPLE_TEXT_OUTPUT_PATH = WEB_SITE_DIR / "data" / "sample-texts.json"
