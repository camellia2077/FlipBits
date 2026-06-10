# radar.py
# 职责：雷达图核心绘制逻辑
# 以 RadarChart 类封装一次绘图会话的共享状态 (fig, ax, angles, view_limit)，
# 消除函数间反复传递相同参数的冗余。

import numpy as np
import matplotlib.pyplot as plt
from matplotlib.font_manager import FontProperties
from matplotlib.patches import Polygon

import config
import config_fonts

# Flash 模式使用 JetBrains Mono，其余使用 IBM Plex Mono
_FLASH_MODES = {"Standard", "Hostility", "Litany", "Collapse", "Zeal", "Void"}


class RadarChart:
    """
    一次雷达图绘制会话的封装。

    典型用法：
        chart = RadarChart(vals)
        chart.draw_armor()
        chart.draw_grids()
        chart.draw_data(vals)
        chart.draw_labels(vals, mode_key, lang='en')
        # ... 保存后切换语言 ...
        chart.draw_labels(vals, mode_key, lang='zh')
        chart.close()
    """

    VIEW_LIMIT = 10.5  # 雷达图径向上限（略大于 Hex-10 装甲）

    def __init__(self, vals):
        """初始化极坐标系并缓存会话状态。"""
        self.fig, self.ax = plt.subplots(figsize=config.FIG_SIZE_RADAR, subplot_kw=dict(polar=True))
        self._apply_axes_scale()

        n = len(config.LABELS_EN)
        angles = np.linspace(0, 2 * np.pi, n, endpoint=False)
        self.angles = np.concatenate((angles, [angles[0]]))

        # Counter-Clockwise；起始偏移 112.5° → 左上顶点
        # 使 0-3 (Machine/Left) 落在左侧，4-7 (Spirit/Right) 落在右侧
        self.ax.set_theta_direction(1)
        self.ax.set_theta_offset(5 * np.pi / 8)

        self.ax.set_ylim(0, self.VIEW_LIMIT)
        self.ax.set_yticklabels([])
        self.ax.set_xticklabels([])  # 隐藏默认的角度刻度 (0, 45, 90...)
        self.ax.spines['polar'].set_visible(False)
        self.ax.grid(False)

    # ------------------------------------------------------------------
    # 背景层
    # ------------------------------------------------------------------

    def draw_armor(self, limit_val=10):
        """绘制背景面板与黄铜边框（Hex-10 上限）。"""
        octagon_v = np.full_like(self.angles, limit_val)
        self.ax.fill(self.angles, octagon_v, color=config.PRIMARY_COLOR, zorder=0)
        
        # 使用 Polygon 确保起点和终点正确闭合（miter join），解决角部线条重叠露出的穿模问题
        poly = Polygon(np.column_stack([self.angles[:-1], octagon_v[:-1]]), 
                       closed=True, fill=False, edgecolor=config.OUTLINE_COLOR, 
                       linewidth=9, joinstyle='miter', zorder=1)
        self.ax.add_patch(poly)

    def draw_grids(self):
        """绘制内部参考网格（2 / 4 / 6 / 8 刻度），采用对称分段以确保顶点连线上必有交点且完全轴对称。"""
        fig_width = self.fig.get_figwidth()
        scale_factor = fig_width / 10.0
        grid_linewidth = 1.2 * scale_factor

        for g_val in [2, 4, 6, 8]:
            # 每一层都分 3 段，加长中间直线段长度，并缩窄与两端折角线段之间的间隙
            intervals = [(0.0, 0.23), (0.31, 0.69), (0.77, 1.0)]

            for i in range(8):
                theta_1 = self.angles[i]
                theta_2 = self.angles[i+1]

                # 顶点 1 与 顶点 2 在极坐标映射到 Cartesian 空间下的直角坐标
                v1 = np.array([g_val * np.cos(theta_1), g_val * np.sin(theta_1)])
                v2 = np.array([g_val * np.cos(theta_2), g_val * np.sin(theta_2)])

                for s, e in intervals:
                    # 在直角坐标空间线性插值 5 个点，保证每一段小虚线在视觉上都是绝对笔直的八边形边框
                    t_vals = np.linspace(s, e, 5)
                    pts = np.array([(1 - t) * v1 + t * v2 for t in t_vals])

                    # 重新转换回极坐标系用于 matplotlib 极坐标轴绘图
                    thetas = np.arctan2(pts[:, 1], pts[:, 0])
                    rs = np.hypot(pts[:, 0], pts[:, 1])

                    self.ax.plot(thetas, rs, color=config.GRID_COLOR,
                                 linewidth=grid_linewidth, alpha=0.25, zorder=1)

    # ------------------------------------------------------------------
    # 数据层
    # ------------------------------------------------------------------

    def draw_data(self, vals):
        """绘制单层雷达多边形。"""
        self._draw_standard(vals)

    def _draw_standard(self, values):
        """单层雷达多边形；若任意维度突破 Hex-10 则切换为深红。"""
        pv = np.concatenate((values, [values[0]]))
        color = '#CC0000' if any(v > 10 for v in values) else config.SECONDARY_COLOR

        # 使用 Polygon 确保闭合（round join）
        poly = Polygon(np.column_stack([self.angles[:-1], values]), 
                       closed=True, fill=False, edgecolor=color, 
                       linewidth=5, joinstyle='round', zorder=3, clip_on=False)
        self.ax.add_patch(poly)
        
        self.ax.fill(self.angles, pv, color=color, alpha=0.6,
                     zorder=2, clip_on=False)

    # ------------------------------------------------------------------
    # 标签层
    # ------------------------------------------------------------------

    def draw_labels(self, vals, mode_key, lang='en'):
        """绘制维度标签与数值，支持中/英文切换。"""
        # 清理旧的维度标签（防止中英文标签重叠）
        for t in list(self.ax.texts):
            if getattr(t, '_is_dim_label', False):
                t.remove()

        labels = config.LABELS_EN if lang == 'en' else config.LABELS_ZH

        # ---------------------------------------------------------------
        # 字体准备
        # ---------------------------------------------------------------
        if lang == 'zh':
            label_font = FontProperties(fname=config_fonts.FONT_ZH_PATH,
                                        weight='bold', size=config_fonts.SIZE_RADAR_LABEL_ZH)
        else:
            label_font = self._get_font_props(mode_key, weight='bold',
                                              size=config_fonts.SIZE_RADAR_LABEL_EN)

        # ---------------------------------------------------------------
        # 逐维度渲染
        # ---------------------------------------------------------------
        if lang == 'zh':
            radius_map = config_fonts.RADAR_LABEL_RADIUS_OFFSETS_ZH
            angle_map  = config_fonts.RADAR_LABEL_ANGLE_OFFSETS_ZH
            x_offset_map = config_fonts.RADAR_LABEL_X_OFFSETS_ZH
        else:
            radius_map = config_fonts.RADAR_LABEL_RADIUS_OFFSETS_EN
            angle_map  = config_fonts.RADAR_LABEL_ANGLE_OFFSETS_EN
            x_offset_map = config_fonts.RADAR_LABEL_X_OFFSETS_EN

        for i, angle in enumerate(self.angles[:-1]):
            r_offset     = radius_map.get(i, 0.0)
            a_offset_deg = angle_map.get(i, 0.0)
            theta        = angle + np.deg2rad(a_offset_deg)
            r            = self.VIEW_LIMIT + r_offset

            # Apply X-axis offset in Cartesian space (keeps Y/vertical position constant)
            x_offset = x_offset_map.get(i, 0.0)
            if x_offset != 0.0:
                theta_offset = 5 * np.pi / 8
                alpha = theta + theta_offset
                cx = r * np.cos(alpha)
                cy = r * np.sin(alpha)
                cx += x_offset
                r = np.hypot(cx, cy)
                alpha_new = np.arctan2(cy, cx)
                theta = alpha_new - theta_offset

            curr_label   = labels[i]
            curr_val_str = f"{vals[i]}"
            combined     = f"{curr_label} {curr_val_str}"

            # 双层渲染：黑色层（文字+括号），红色层（数字）
            # 中文用全角空格（U+3000）替换 CJK 字符保持宽度一致
            black_chars, red_chars = [], []
            for c in combined:
                if c.isdigit() or c == '.':
                    black_chars.append(' ')
                    red_chars.append(c)
                else:
                    black_chars.append(c)
                    red_chars.append('　' if ord(c) > 0x7F else ' ')

            t = self.ax.text(theta, r, ''.join(black_chars),
                             color=config.TEXT_COLOR, fontproperties=label_font,
                             ha='center', va='center',
                             zorder=10, clip_on=False)
            t._is_dim_label = True

            self.ax.text(theta, r, ''.join(red_chars),
                         color=config.SECONDARY_COLOR, fontproperties=label_font,
                         ha='center', va='center',
                         zorder=11, clip_on=False)._is_dim_label = True

        # 隐藏原始坐标轴刻度
        self.ax.set_xticks(self.angles[:-1])
        self.ax.set_xticklabels([])

    # ------------------------------------------------------------------
    # 生命周期
    # ------------------------------------------------------------------

    def close(self):
        """释放 matplotlib figure 资源。"""
        plt.close(self.fig)

    # ------------------------------------------------------------------
    # 私有辅助
    # ------------------------------------------------------------------

    @staticmethod
    def _get_font_props(mode_key, weight='regular', size=None):
        """始终选择 IBM Plex Mono 作为英文字体。"""
        if size is None:
            size = config_fonts.SIZE_RADAR_LABEL_EN

        path = (config_fonts.FONT_IBM_PLEX_BOLD if weight == 'bold'
                else config_fonts.FONT_IBM_PLEX_REGULAR)

        return FontProperties(fname=path, size=size)

    def _apply_axes_scale(self):
        """缩放雷达图区本体，不改变 figure 导出的背景尺寸。"""
        scale = float(config.RADAR_AXES_SCALE)
        if not 0 < scale <= 1:
            raise ValueError("config.RADAR_AXES_SCALE must be within (0, 1].")

        margin = (1.0 - scale) / 2.0
        self.ax.set_position([margin, margin, scale, scale])

    @staticmethod
    def _shift_polar(angle, radius, offset, direction='perpendicular'):
        """在笛卡尔空间施加偏移后转回极坐标。

        direction: 'perpendicular'（法线方向，远离圆心）或 'radial'（径向）
        返回: (new_r, new_theta)
        """
        cx = radius * np.cos(angle)
        cy = radius * np.sin(angle)

        if direction == 'perpendicular':
            nx, ny = -np.sin(angle), np.cos(angle)
        else:  # radial
            nx, ny = np.cos(angle), np.sin(angle)

        cx += offset * nx
        cy += offset * ny

        return np.hypot(cx, cy), np.arctan2(cy, cx)
