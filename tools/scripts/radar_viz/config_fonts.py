# config_fonts.py
import os

# ==========================================
# [ 字体路径配置 - Font Path Configuration ]
# ==========================================
LOCAL_FONT_DIR = r"C:\Users\17596\AppData\Local\Microsoft\Windows\Fonts"
SYS_FONT_DIR = r"C:\Windows\Fonts"

FONT_JETBRAINS_REGULAR = os.path.join(LOCAL_FONT_DIR, "JetBrainsMono-Regular.ttf")
FONT_JETBRAINS_BOLD = os.path.join(LOCAL_FONT_DIR, "JetBrainsMono-Bold.ttf")
FONT_IBM_PLEX_REGULAR = os.path.join(LOCAL_FONT_DIR, "IBMPlexMono-Regular.ttf")
FONT_IBM_PLEX_BOLD = os.path.join(LOCAL_FONT_DIR, "IBMPlexMono-Bold.ttf")
FONT_ZH_PATH = r"C:\Users\17596\Downloads\Noto_Sans_SC\static\NotoSansSC-Bold.ttf"

# ==========================================
# [ 字体大小配置 - Font Size Configuration ]
# ==========================================



# 数值变大：标题向上移（远离图表中心）
# 数值变小：标题向下移（靠近图表中心）
# 1.0是标题文本正好在八边形线段上方
SIZE_RADAR_TITLE_ZH = 56    # 中文主标题大小
POS_Y_RADAR_TITLE_ZH = 1.1 # 中文主标题垂直位置

SIZE_RADAR_TITLE_EN = 30    # 英文主标题大小
POS_Y_RADAR_TITLE_EN = 1.12 # 英文主标题垂直位置


# 2. 图例说明书 (Legend Panel)
SIZE_LEGEND_TITLE_ZH = 32  # 中文图例主标题
SIZE_LEGEND_LABEL_ZH = 30  # 中文图例维度标签
SIZE_LEGEND_DESC_ZH = 20   # 中文图例说明正文
POS_X_LEGEND_LABEL_ZH = 0.04 # 中文标签X坐标 (靠近边框，终端感)
POS_X_LEGEND_DESC_ZH = 0.22  # 中文说明正文X坐标

SIZE_LEGEND_TITLE_EN = 32  # 英文图例主标题
SIZE_LEGEND_LABEL_EN = 20  # 英文图例维度标签
SIZE_LEGEND_DESC_EN = 18   # 英文图例说明正文
POS_X_LEGEND_LABEL_EN = 0.04 # 英文标签X坐标
POS_X_LEGEND_DESC_EN = 0.35  # 英文说明正文X坐标

from config import (
    TRANSMISSION_SPEED, ENCODING_EFFICIENCY, TRANSMISSION_STABILITY,
    COMPATIBILITY, PARSING_DIFFICULTY, ELECTRONIC_FEEL,
    EMOTIONAL_RICHNESS, SENSE_OF_RITUAL
)

# ==========================================
# [ 雷达图布局微调 - Radar Layout Fine-tuning ]
# ==========================================



# 1. 雷达图本体 (Radar Chart)
SIZE_RADAR_LABEL_ZH = 64    # 中文维度标签大小

# --- 中文版微调 (Chinese Offsets) ---
# 值增大：远离圆心；值减小：靠近圆心
RADAR_LABEL_RADIUS_OFFSETS_ZH = {
    # --- 侧翼维度 (Side) ---
    ENCODING_EFFICIENCY:    1.40, # 编码效率
    TRANSMISSION_STABILITY: 1.40, # 传输稳定
    EMOTIONAL_RICHNESS:     1.40, # 情感丰富度
    ELECTRONIC_FEEL:        1.40, # 电子感

    # --- 垂直维度 (Vertical) ---
    TRANSMISSION_SPEED:      0.30,  # 传输速度
    SENSE_OF_RITUAL:         0.30,  # 仪式感

    COMPATIBILITY:           0.50, # 兼容性
    PARSING_DIFFICULTY:      0.50, # 解析难度
}

RADAR_LABEL_ANGLE_OFFSETS_ZH = {
    TRANSMISSION_SPEED: 0.0, ENCODING_EFFICIENCY: 0.0, TRANSMISSION_STABILITY: 0.0,
    COMPATIBILITY: 0.0, PARSING_DIFFICULTY: 0.0, ELECTRONIC_FEEL: 0.0,
    EMOTIONAL_RICHNESS: 0.0, SENSE_OF_RITUAL: 0.0
}



SIZE_RADAR_LABEL_EN = 24    # 英文维度标签大小
# --- 英文版微调 (English Offsets) ---
RADAR_LABEL_RADIUS_OFFSETS_EN = {
    TRANSMISSION_SPEED:      -0.10,
    ENCODING_EFFICIENCY:      0.40,
    TRANSMISSION_STABILITY:   0.40,
    COMPATIBILITY:           -0.10,
    PARSING_DIFFICULTY:      -0.10,
    ELECTRONIC_FEEL:          0.40,
    EMOTIONAL_RICHNESS:       0.40,
    SENSE_OF_RITUAL:         -0.10,
}

RADAR_LABEL_ANGLE_OFFSETS_EN = {
    TRANSMISSION_SPEED: 0.0, ENCODING_EFFICIENCY: 0.0, TRANSMISSION_STABILITY: 0.0,
    COMPATIBILITY: 0.0, PARSING_DIFFICULTY: 0.0, ELECTRONIC_FEEL: 0.0,
    EMOTIONAL_RICHNESS: 0.0, SENSE_OF_RITUAL: 0.0
}


