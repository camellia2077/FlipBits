import sys
import os

# 确保脚本所在目录在 sys.path 中，以正确导入同目录下的其他组件
script_dir = os.path.dirname(os.path.abspath(__file__))
if script_dir not in sys.path:
    sys.path.insert(0, script_dir)

from config import verify_svg_parts_exist
from cli import parse_args
from svg_to_png import convert_svg_to_png

# 解决 Windows 命令行下非 UTF-8 编码的 Unicode 输出报错问题
if sys.stdout:
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except AttributeError:
        pass

def main():
    # 1. 校验必需的 SVG 组件是否存在
    if not verify_svg_parts_exist():
        sys.exit(1)

    # 2. 解析命令行参数
    args = parse_args()

    # 3. 无论默认还是指定输出路径，在输出的文件夹下面，新建一个 logo_photos 文件夹
    final_output_dir = os.path.join(args.output_dir, "logo_photos")
    os.makedirs(final_output_dir, exist_ok=True)

    # 4. 输出解析到的配置参数
    bg_val = getattr(args, "bg_color", None)
    print("--------------------------------------------------")
    print(f" 子命令 (Subcommand): {args.subcommand}")
    print(f" 主题配置文件 (TOML): {args.themes_file}")
    if getattr(args, "theme", None) is not None:
        theme_str = f"{args.theme}"
        if getattr(args, "theme_name", None):
            theme_str += f" ({args.theme_name})"
        print(f" 预设主题 (Theme):     {theme_str}")
    print(f" 文件基准名 (Name):    {args.name}")
    print(f" primary   颜色:  {args.color_primary}")
    print(f" secondary 颜色:  {args.color_secondary}")
    print(f" outline   颜色:  {args.color_outline}")
    if bg_val is not None:
        print(f" bg_color  颜色:  {bg_val}")
    print(f" 图标输出目录:   {final_output_dir}")
    print(f" SVG 模板路径:   {args.svg_template}")
    print("--------------------------------------------------")

    # 5. 调用核心转换服务生成 PNG 变体
    try:
        convert_svg_to_png(
            svg_template_path=args.svg_template,
            output_dir=final_output_dir,
            color_primary=args.color_primary,
            color_secondary=args.color_secondary,
            color_outline=args.color_outline,
            subcommand=args.subcommand,
            bg_color=bg_val,
            base_name=args.name
        )
        print(f"🎉 目标变体图标生成完毕！文件保存在：{final_output_dir}")
    except Exception as e:
        print(f"❌ 运行失败: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
