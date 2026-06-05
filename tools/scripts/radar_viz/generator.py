# generator.py
# 职责：雷达图生成管线控制器，封装数据解析、渲染流程和生命周期管理。
# 支持上下文管理器（with 语句）以确保 matplotlib 资源安全释放。

import numpy as np
import data
import exporter
from radar import RadarChart


class RadarGenerator:
    """
    雷达图生成管线控制器，封装数据解析、渲染流程和生命周期管理。
    支持上下文管理器（with 语句）以确保 matplotlib 资源安全释放。
    """
    def __init__(self, mode_key: str = None, vals: list[float] | np.ndarray = None, lang: str = 'en', out_dir: str = None):
        self.mode_key = mode_key
        self.lang = lang
        self.out_dir = out_dir

        if mode_key is not None:
            self.mode_data = data.DATA_DICT[self.mode_key]
            self.vals = self.mode_data["vals"]
        elif vals is not None:
            self.vals = list(vals)
        else:
            raise ValueError("Either mode_key or vals must be provided.")

        self.chart = None

    def __enter__(self):
        self.chart = RadarChart(self.vals)
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.chart:
            self.chart.close()

    def generate_panel(self):
        """完整生成静态雷达图面板（含装甲、网格、数据、标签与标题）并导出。"""
        if not self.chart:
            raise RuntimeError("Generator must be used within a 'with' context.")
        if self.mode_key is None:
            raise ValueError("mode_key must be provided to generate a titled panel.")
        self.chart.draw_armor()
        self.chart.draw_grids()
        self.chart.draw_data(self.vals)
        self.chart.draw_labels(self.vals, self.mode_key, lang=self.lang)
        exporter.save_and_title(self.chart.fig, self.mode_key, lang=self.lang, script_dir=self.out_dir)

    def generate_frame(self, out_path: str, dpi: int = 150):
        """渲染动画单帧：仅装甲 + 网格 + 数据多边形（无标签），直接保存到指定路径。"""
        if not self.chart:
            raise RuntimeError("Generator must be used within a 'with' context.")
        self.chart.draw_armor()
        self.chart.draw_grids()
        self.chart.draw_data(self.vals)
        self.chart.fig.savefig(out_path, facecolor='none', transparent=True, dpi=dpi)
