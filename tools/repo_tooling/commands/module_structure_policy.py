from __future__ import annotations

from pathlib import Path

from ..constants import ROOT_DIR
from ..errors import ToolError


_NAMED_MODULE_TUS_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_core" / "src" / "common" / "version.cpp": (
        "module bag.common.version;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "codec.cpp": (
        "module bag.flash.codec;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "signal.cpp": (
        "module bag.flash.signal;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "voicing.cpp": (
        "module bag.flash.voicing;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "phy_rules.cpp": (
        "module bag.flash.phy_rules;",
        "import bag.flash.codec;",
        "import bag.flash.signal;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "phy_decode.cpp": (
        "module bag.flash.phy_decode;",
        "import bag.flash.phy_rules;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "phy_encode.cpp": (
        "module bag.flash.phy_encode;",
        "import bag.flash.phy_rules;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "signal_rules.cpp": (
        "module bag.flash.signal_rules;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "signal_layout.cpp": (
        "module bag.flash.signal_layout;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "signal_decode.cpp": (
        "module bag.flash.signal_decode;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "fsk" / "fsk_codec.cpp": (
        "module bag.fsk.codec;",
        "import bag.flash.signal;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pipeline" / "pipeline.cpp": (
        "module bag.pipeline;",
        "import bag.transport.facade;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "mini" / "morse_rules.cpp": (
        "module bag.mini.morse_rules;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "mini" / "phy_decode.cpp": (
        "module bag.mini.phy_decode;",
        "import bag.mini.morse_rules;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "mini" / "phy_encode.cpp": (
        "module bag.mini.phy_encode;",
        "import bag.mini.tone_renderer;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "mini" / "tone_renderer.cpp": (
        "module bag.mini.tone_renderer;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "codec.cpp": (
        "module bag.pro.codec;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_rules.cpp": (
        "module bag.pro.phy_rules;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_decode.cpp": (
        "module bag.pro.phy_decode;",
        "import bag.pro.phy_rules;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_encode.cpp": (
        "module bag.pro.phy_encode;",
        "import bag.pro.tone_renderer;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "tone_renderer.cpp": (
        "module bag.pro.tone_renderer;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "compat" / "frame_codec.cpp": (
        "module bag.transport.compat.frame_codec;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "transport.cpp": (
        "module bag.transport.facade;",
        "import bag.flash.phy_clean;",
        "import bag.pro.phy_clean;",
        "import bag.ultra.phy_clean;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "codec.cpp": (
        "module bag.ultra.codec;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_rules.cpp": (
        "module bag.ultra.phy_rules;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_decode.cpp": (
        "module bag.ultra.phy_decode;",
        "import bag.ultra.phy_rules;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_encode.cpp": (
        "module bag.ultra.phy_encode;",
        "import bag.ultra.tone_renderer;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "tone_renderer.cpp": (
        "module bag.ultra.tone_renderer;",
    ),
}


def run_named_module_tus_policy_checks() -> None:
    failures: list[str] = []
    for path, required_tokens in sorted(_NAMED_MODULE_TUS_RULES.items()):
        if not path.is_file():
            failures.append(f"{path.relative_to(ROOT_DIR)} is missing")
            continue
        content = path.read_text(encoding="utf-8")
        missing_tokens = [token for token in required_tokens if token not in content]
        if missing_tokens:
            missing = ", ".join(missing_tokens)
            failures.append(f"{path.relative_to(ROOT_DIR)} missing: {missing}")

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Named-module structure regression detected in audited source files:\n"
            f"{joined}"
        )


def run_module_structure_policy_checks() -> None:
    run_named_module_tus_policy_checks()
