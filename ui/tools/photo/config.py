import os
import sys

# 定义当前配置文件的所在目录
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# 基于相对路径定位到 ui/svg/parts/ 目录
PARTS_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "..", "svg", "parts"))

CORE_RING_PATH = os.path.join(PARTS_DIR, "core_ring.svg")
INNER_GEAR_PATH = os.path.join(PARTS_DIR, "inner_gear.svg")
OUTER_GEAR_PATH = os.path.join(PARTS_DIR, "outer_gear.svg")

SVG_PARTS = {
    "core_ring.svg": CORE_RING_PATH,
    "inner_gear.svg": INNER_GEAR_PATH,
    "outer_gear.svg": OUTER_GEAR_PATH
}

def verify_svg_parts_exist() -> bool:
    """
    检查所有必需的 SVG 组件是否存在。
    如果缺少任何组件，向 stderr 输出缺失的文件路径，并返回 False。
    """
    missing_files = []
    for name, path in SVG_PARTS.items():
        if not os.path.exists(path):
            missing_files.append(path)
            
    if missing_files:
        print("❌ 运行失败: 缺少以下必需的 SVG 组件文件，生成已终止！", file=sys.stderr)
        for path in missing_files:
            print(f"  - 缺失路径: {path}", file=sys.stderr)
        return False
    return True
