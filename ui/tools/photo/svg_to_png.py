import os
import sys

# 确保脚本所在目录在 sys.path 中，以正确导入同目录下的 icon_renderer
script_dir = os.path.dirname(os.path.abspath(__file__))
if script_dir not in sys.path:
    sys.path.insert(0, script_dir)

from icon_renderer import run_icon_generation

def convert_svg_to_png(
    svg_template_path: str,
    output_dir: str,
    color_primary: str,
    color_secondary: str,
    color_outline: str,
    subcommand: str = "all",
    bg_color: str = None,
    base_name: str = "app_icon"
):
    """
    核心转换服务接口：调用底层渲染引擎生成 PNG 图标变体。
    """
    run_icon_generation(
        svg_template_path=svg_template_path,
        output_dir=output_dir,
        color_primary=color_primary,
        color_secondary=color_secondary,
        color_outline=color_outline,
        subcommand=subcommand,
        bg_color=bg_color,
        base_name=base_name
    )
