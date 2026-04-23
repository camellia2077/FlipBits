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
    default_responsibility_risk_threshold: int
    responsibility_line_threshold: int
    responsibility_state_signal_threshold: int
    responsibility_top_level_composable_threshold: int
    responsibility_role_kind_threshold: int
    responsibility_mode_branch_threshold: int


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
    default_responsibility_risk_threshold = _as_positive_int_with_default(
        section.get("default_responsibility_risk_threshold"),
        f"{lang}.default_responsibility_risk_threshold",
        default=5,
    )
    responsibility_line_threshold = _as_positive_int_with_default(
        section.get("responsibility_line_threshold"),
        f"{lang}.responsibility_line_threshold",
        default=default_over_threshold,
    )
    responsibility_state_signal_threshold = _as_positive_int_with_default(
        section.get("responsibility_state_signal_threshold"),
        f"{lang}.responsibility_state_signal_threshold",
        default=4,
    )
    responsibility_top_level_composable_threshold = _as_positive_int_with_default(
        section.get("responsibility_top_level_composable_threshold"),
        f"{lang}.responsibility_top_level_composable_threshold",
        default=3,
    )
    responsibility_role_kind_threshold = _as_positive_int_with_default(
        section.get("responsibility_role_kind_threshold"),
        f"{lang}.responsibility_role_kind_threshold",
        default=2,
    )
    responsibility_mode_branch_threshold = _as_positive_int_with_default(
        section.get("responsibility_mode_branch_threshold"),
        f"{lang}.responsibility_mode_branch_threshold",
        default=2,
    )

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
        default_responsibility_risk_threshold=default_responsibility_risk_threshold,
        responsibility_line_threshold=responsibility_line_threshold,
        responsibility_state_signal_threshold=responsibility_state_signal_threshold,
        responsibility_top_level_composable_threshold=responsibility_top_level_composable_threshold,
        responsibility_role_kind_threshold=responsibility_role_kind_threshold,
        responsibility_mode_branch_threshold=responsibility_mode_branch_threshold,
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


def _as_positive_int_with_default(value, field_name: str, *, default: int) -> int:
    if value is None:
        return default
    return _as_positive_int(value, field_name)
