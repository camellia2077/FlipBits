from dataclasses import dataclass
from pathlib import Path

try:
    import tomllib
except ModuleNotFoundError:  # pragma: no cover
    import tomli as tomllib  # type: ignore

LANG_CHOICES = ("cpp", "kt", "py", "rs")


@dataclass(frozen=True)
class LanguageConfig:
    lang: str
    display_name: str
    default_paths: list[str]
    extensions: set[str]
    ignore_dirs: set[str]
    ignore_prefixes: tuple[str, ...]
    default_over_threshold: int
    default_under_threshold: int
    default_dir_over_files: int
    over_inclusive: bool


def load_language_config(config_path: Path, lang: str) -> LanguageConfig:
    if not config_path.exists():
        raise FileNotFoundError(f"配置文件不存在: {config_path}")

    with config_path.open("rb") as handle:
        payload = tomllib.load(handle)

    section = payload.get(lang)
    if not isinstance(section, dict):
        raise ValueError(f"配置缺失语言节点: [{lang}]")

    display_name = str(section.get("display_name", lang)).strip() or lang
    default_paths = _as_str_list(section.get("default_paths"), f"{lang}.default_paths")
    extensions = {
        item.lower() for item in _as_str_list(section.get("extensions"), f"{lang}.extensions")
    }
    ignore_dirs = set(_as_str_list(section.get("ignore_dirs"), f"{lang}.ignore_dirs"))
    ignore_prefixes = tuple(
        item.lower()
        for item in _as_str_list(section.get("ignore_prefixes"), f"{lang}.ignore_prefixes")
    )
    default_over_threshold = _as_positive_int(
        section.get("default_over_threshold"),
        f"{lang}.default_over_threshold",
    )
    default_under_threshold = _as_positive_int(
        section.get("default_under_threshold"),
        f"{lang}.default_under_threshold",
    )
    default_dir_over_files = _as_positive_int(
        section.get("default_dir_over_files"),
        f"{lang}.default_dir_over_files",
    )
    over_inclusive = bool(section.get("over_inclusive", False))

    return LanguageConfig(
        lang=lang,
        display_name=display_name,
        default_paths=default_paths,
        extensions=extensions,
        ignore_dirs=ignore_dirs,
        ignore_prefixes=ignore_prefixes,
        default_over_threshold=default_over_threshold,
        default_under_threshold=default_under_threshold,
        default_dir_over_files=default_dir_over_files,
        over_inclusive=over_inclusive,
    )


def _as_str_list(value, field_name: str) -> list[str]:
    if not isinstance(value, list):
        raise ValueError(f"配置字段必须是数组: {field_name}")
    output: list[str] = []
    for item in value:
        if not isinstance(item, str) or not item.strip():
            raise ValueError(f"配置字段元素必须是非空字符串: {field_name}")
        output.append(item.strip())
    return output


def _as_positive_int(value, field_name: str) -> int:
    if not isinstance(value, int) or value <= 0:
        raise ValueError(f"配置字段必须是正整数: {field_name}")
    return value
