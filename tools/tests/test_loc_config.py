import tempfile
import textwrap
import unittest
from pathlib import Path
import sys


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from scripts.loc.internal.config import load_language_config, load_scan_lines_config, load_scope_config


class LocConfigTests(unittest.TestCase):
    def test_global_excluded_languages_are_applied_to_all_scan_modes(self) -> None:
        config_text = """
            [global]
            excluded_languages = ["md"]

            [md]
            display_name = "Markdown"
            default_paths = ["docs"]
            extensions = [".md"]
            ignore_dirs = []
            ignore_prefixes = []
            default_over_threshold = 100
            default_under_threshold = 20
            default_dir_over_files = 3
            over_inclusive = false

            [cpp]
            display_name = "C++"
            default_paths = ["src"]
            extensions = [".cpp", ".md"]
            ignore_dirs = []
            ignore_prefixes = []
            default_over_threshold = 100
            default_under_threshold = 20
            default_dir_over_files = 3
            over_inclusive = false
        """

        with tempfile.TemporaryDirectory() as temp_dir:
            config_path = Path(temp_dir) / "scan_lines.toml"
            config_path.write_text(textwrap.dedent(config_text), encoding="utf-8")

            config = load_scan_lines_config(config_path)
            md_config = load_language_config(config_path, "md")
            cpp_config = load_language_config(config_path, "cpp")

        self.assertEqual(config.global_config.excluded_languages, frozenset({"md"}))
        self.assertEqual(md_config.extensions, set())
        self.assertEqual(cpp_config.extensions, {".cpp"})

    def test_scope_config_groups_existing_language_configs(self) -> None:
        config_path = TOOLS_DIR / "scripts" / "loc" / "scan_lines.toml"

        config = load_scan_lines_config(config_path)
        android = load_scope_config(config_path, "audio_android")

        self.assertIn("audio_api", config.scopes)
        self.assertEqual(android.display_name, "apps/audio_android")
        self.assertEqual(android.languages, ("cpp", "kt"))
        self.assertEqual(android.default_paths, ["apps/audio_android"])

    def test_scope_rejects_unknown_language(self) -> None:
        config_text = """
            [cpp]
            display_name = "C++"
            default_paths = ["src"]
            extensions = [".cpp"]
            ignore_dirs = []
            ignore_prefixes = []
            default_over_threshold = 100
            default_under_threshold = 20
            default_dir_over_files = 3
            over_inclusive = false

            [scopes.bad_scope]
            display_name = "Bad"
            default_paths = ["src"]
            languages = ["kt"]
        """

        with tempfile.TemporaryDirectory() as temp_dir:
            config_path = Path(temp_dir) / "scan_lines.toml"
            config_path.write_text(textwrap.dedent(config_text), encoding="utf-8")

            with self.assertRaisesRegex(ValueError, "未配置语言"):
                load_scan_lines_config(config_path)


if __name__ == "__main__":
    unittest.main()
