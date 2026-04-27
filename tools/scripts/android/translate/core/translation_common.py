from __future__ import annotations

# Compatibility facade for older imports. New code should prefer importing from:
# - translation_paths.py
# - translation_resources.py
# - translation_reporting.py

from core.translation_paths import (
    DEFAULT_RES_DIRECTORY,
    FACTIONS,
    REPO_ROOT,
    TEXT_TYPES,
    display_language_tag,
    get_faction_from_filename,
    get_text_type_from_filename,
    humanize_name,
    is_pro_sample_key,
    iter_translation_text_xml_paths,
)
from core.translation_reporting import (
    MinimalMarkdownReportWriter,
    OutputDirectoryManager,
    ReportFileBlock,
    ReportKeyBlock,
)
from core.translation_resources import (
    AndroidStringResourceRepository,
    ResourceFile,
    infer_sample_lengths,
)

__all__ = [
    "AndroidStringResourceRepository",
    "DEFAULT_RES_DIRECTORY",
    "FACTIONS",
    "MinimalMarkdownReportWriter",
    "OutputDirectoryManager",
    "REPO_ROOT",
    "ReportFileBlock",
    "ReportKeyBlock",
    "ResourceFile",
    "TEXT_TYPES",
    "display_language_tag",
    "get_faction_from_filename",
    "get_text_type_from_filename",
    "humanize_name",
    "infer_sample_lengths",
    "is_pro_sample_key",
    "iter_translation_text_xml_paths",
]
