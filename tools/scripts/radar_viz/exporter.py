# exporter.py
# 职责：将已绘制的雷达图保存为 PNG
# 依赖：config.py (颜色)

import os
import matplotlib.pyplot as plt

import config


# Flash 模式统一输出到 {lang}/flash/ 子目录
_FLASH_MODES = {"Standard", "Hostility", "Litany", "Collapse", "Zeal", "Void"}


def _resolve_out_path(mode_key, lang, script_dir):
    """根据模式和语言确定输出文件的完整路径。"""
    base_dir = os.path.join(script_dir, lang)
    if mode_key in _FLASH_MODES:
        out_dir = os.path.join(base_dir, "flash")
        filename = f"Flash[{mode_key}].png"
    elif mode_key.startswith("Mini "):
        out_dir = base_dir
        parts = mode_key.split()
        speed = parts[1] if len(parts) >= 2 else ""
        filename = f"Mini[{speed}WPM].png"
    else:
        out_dir = base_dir
        safe_name = mode_key.replace(" ", "_")
        filename = f"{safe_name}.png"
    os.makedirs(out_dir, exist_ok=True)
    return os.path.join(out_dir, filename)


def save_and_title(fig, mode_key, lang='en', script_dir=None):
    """
    保存已绘制的雷达图为透明背景 PNG（不加标题）。
    """
    if script_dir is None:
        here = os.path.dirname(os.path.abspath(__file__))
        script_dir = os.path.normpath(os.path.join(here, "..", "..", "..", "temp"))

    ax = fig.gca()

    # 清理旧标题 text artist（若有）
    for t in list(ax.texts):
        if getattr(t, '_is_title', False):
            t.remove()

    # 保存
    out_path = _resolve_out_path(mode_key, lang, script_dir)
    plt.savefig(out_path, facecolor='none', transparent=True, dpi=config.EXPORT_DPI_RADAR)
    print(f"[Generated ({lang.upper()})] -> {out_path}")
