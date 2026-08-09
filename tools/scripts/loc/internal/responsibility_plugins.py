from __future__ import annotations

from .config import LanguageConfig
from .responsibility_plugin_base import ResponsibilityLanguagePlugin
from .responsibility_plugins_cpp import CppResponsibilityPlugin
from .responsibility_plugins_kt import KotlinResponsibilityPlugin
from .responsibility_plugins_py import PythonResponsibilityPlugin


def create_responsibility_language_plugin(
    config: LanguageConfig,
) -> ResponsibilityLanguagePlugin | None:
    if config.lang == "kt":
        return KotlinResponsibilityPlugin(config)
    if config.lang == "py":
        return PythonResponsibilityPlugin(config)
    if config.lang == "cpp":
        return CppResponsibilityPlugin(config)
    return None