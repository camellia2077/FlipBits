import argparse
import os
import re
import sys
from themes import load_themes_from_toml

# 默认配色配置 (对应 mars_relic)
DEFAULT_COLOR_SECONDARY = "#9E1B1B"
DEFAULT_COLOR_PRIMARY   = "#E8E2D0"
DEFAULT_COLOR_OUTLINE   = "#C5A059"

def parse_hex_color(val: str) -> str:
    """
    智能十六进制颜色解析器：
    - 如果输入以 '#' 开头，直接返回。
    - 如果输入符合 3-8 位的纯十六进制字符（例如 FFF, FFFFFF），自动在前面补齐 '#'。
    - 否则（如 CSS 预定义颜色名 'red'、'blue' 等），原样返回，以便兼容 SVG 颜色标准。
    """
    val = val.strip()
    if val.startswith('#'):
        return val
    # 匹配 3, 4, 6, 8 位的纯十六进制颜色字符
    if re.match(r'^[0-9a-fA-F]{3,8}$', val):
        return f"#{val}"
    return val

def parse_args():
    """
    解析命令行参数，支持子命令 (all | launcher | playstore | pure | pure-bg)
    并且支持 Faction Theme 预设导入与自定义输出基础文件名。
    """
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.abspath(os.path.join(script_dir, "..", "..", ".."))
    default_output_dir = os.path.join(root_dir, "temp")

    # 定义支持的子命令列表 and 帮助参数
    subcommands_list = ["all", "launcher", "playstore", "pure", "pure-bg", "-h", "--help"]

    # 检测用户命令行输入中是否显式提供了子命令。
    # 如果没有提供，默认向 sys.argv 注入 'all' 以便默认执行 all 生成逻辑。
    has_subcommand = False
    for arg in sys.argv[1:]:
        if not arg.startswith('-'):
            if arg in subcommands_list:
                has_subcommand = True
            break

    if not has_subcommand and "-h" not in sys.argv and "--help" not in sys.argv:
        sys.argv.insert(1, "all")

    # 创建通用的父解析器，供子解析器共享参数定义
    parent_parser = argparse.ArgumentParser(add_help=False)
    parent_parser.add_argument(
        "-pri", "--primary", "--color-primary",
        dest="color_primary",
        type=parse_hex_color,
        default=DEFAULT_COLOR_PRIMARY,
        help=f"主色 (Primary) - 齿轮右半边/左内圈颜色 (默认: {DEFAULT_COLOR_PRIMARY})"
    )
    parent_parser.add_argument(
        "-sec", "--secondary", "--color-secondary",
        dest="color_secondary",
        type=parse_hex_color,
        default=DEFAULT_COLOR_SECONDARY,
        help=f"辅色 (Secondary) - 齿轮左半边/右内圈颜色 (默认: {DEFAULT_COLOR_SECONDARY})"
    )
    parent_parser.add_argument(
        "-out", "--outline", "--color-outline",
        dest="color_outline",
        type=parse_hex_color,
        default=DEFAULT_COLOR_OUTLINE,
        help=f"轮廓色 (Outline) - 齿轮外圈及边框线颜色 (默认: {DEFAULT_COLOR_OUTLINE})"
    )
    parent_parser.add_argument(
        "--themes-file", "--toml",
        dest="themes_file",
        default=os.path.join(script_dir, "themes_en.toml"),
        help="存储 Faction Theme 的 TOML 配置文件路径 (默认: 同目录下的 themes_en.toml)"
    )
    parent_parser.add_argument(
        "-t", "--theme",
        dest="theme",
        default=None,
        help="选择预设的 Faction Theme (例如: mars_relic, xeno_code, fires_of_fate...)，指定后会自动覆写主色、辅色和轮廓色"
    )
    parent_parser.add_argument(
        "-n", "--name",
        dest="name",
        default=None,
        help="指定输出 PNG 的基础文件名 (例如: my_custom_icon)。若指定了 --theme 且没有手动传该值，则默认使用 theme 名称作为基础文件名；否则默认为 'app_icon'"
    )
    parent_parser.add_argument(
        "--output-dir", "-o",
        default=default_output_dir,
        help=f"输出目标根目录 (默认: {default_output_dir})"
    )
    parent_parser.add_argument(
        "--svg-template",
        default=os.path.join(script_dir, "app_icon.svg"),
        help="SVG 模板文件路径"
    )

    # 创建主解析器
    parser = argparse.ArgumentParser(
        description="FlipBits App Icon CLI Parser - 分层解析子命令并支持 Faction Theme 的图标生成器"
    )

    # 添加子解析器
    subparsers = parser.add_subparsers(dest="subcommand", help="子命令 (all | launcher | playstore | pure | pure-bg)")

    # 1. all
    subparsers.add_parser("all", parents=[parent_parser], help="一键生成所有 4 种规格的图标变体 (默认)")

    # 2. launcher
    launcher_parser = subparsers.add_parser("launcher", parents=[parent_parser], help="只生成手机桌面视觉还原版图标")
    launcher_parser.add_argument(
        "-bg", "--bg-color",
        dest="bg_color",
        type=parse_hex_color,
        default="#FFFFFF",
        help="圆角卡片底色 (默认: #FFFFFF)"
    )

    # 3. playstore
    playstore_parser = subparsers.add_parser("playstore", parents=[parent_parser], help="只生成 Google Play 商店大圆角版图标")
    playstore_parser.add_argument(
        "-bg", "--bg-color",
        dest="bg_color",
        type=parse_hex_color,
        default="#FFFFFF",
        help="圆角卡片底色 (默认: #FFFFFF)"
    )

    # 4. pure
    subparsers.add_parser("pure", parents=[parent_parser], help="只生成纯前景镂空无背景透明图标")

    # 5. pure-bg
    pure_bg_parser = subparsers.add_parser("pure-bg", parents=[parent_parser], help="只生成带纯色背景的镂空大图标")
    pure_bg_parser.add_argument(
        "-bg", "--bg-color",
        dest="bg_color",
        type=parse_hex_color,
        default=None,
        help="纯色背景色 (默认: 使用 primary 颜色)"
    )

    args = parser.parse_args()

    # 动态加载 themes.toml 配置文件
    themes_dict = load_themes_from_toml(args.themes_file)

    # 如果指定了预设主题，验证并覆盖颜色配置
    if args.theme is not None:
        if args.theme not in themes_dict:
            print(f"❌ 错误: 指定的主题 '{args.theme}' 在配置文件 '{args.themes_file}' 中不存在。", file=sys.stderr)
            print(f"可用主题: {', '.join(themes_dict.keys())}", file=sys.stderr)
            sys.exit(1)
        colors = themes_dict[args.theme]
        try:
            args.color_primary = parse_hex_color(colors["primary"])
            args.color_secondary = parse_hex_color(colors["secondary"])
            args.color_outline = parse_hex_color(colors["outline"])
            args.theme_name = colors.get("name", args.theme)
        except KeyError as e:
            print(f"❌ 错误: 主题 '{args.theme}' 缺少必需的颜色键: {e}", file=sys.stderr)
            sys.exit(1)
        if args.name is None:
            args.name = args.theme
    else:
        if args.name is None:
            args.name = "app_icon"
        args.theme_name = None

    args.loaded_themes = themes_dict
    return args
