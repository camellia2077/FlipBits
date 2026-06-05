import cairosvg
from PIL import Image
import io
import sys
import os
from PIL import ImageDraw

# ==========================================
# 配色配置区 (修改此处 HEX 颜色可全局更新图标)
# ==========================================
COLOR_DARK_RED = "#9E1B1B"   # 齿轮左半边填充色 / 右半边虚线内圈色
COLOR_BEIGE    = "#E8E2D0"   # 齿轮右半边填充色 / 左半边虚线内圈色
COLOR_GOLD     = "#C5A059"   # 齿轮外圈轮廓线 / 中边框线色

# ==========================================
# ==========================================
# 【原 因】
# 原版 SVG 采用了非常优雅的相对比例写法：设定 pathLength="8" 和 stroke-dasharray="0.5 0.5"。
# 现代浏览器（如 Chrome/Edge/Illustrator）的现代渲染引擎支持这一特性，会自动将周长等分为 8 份。
# 但 Python 使用的 `cairosvg`（底层基于 cairo）是一个较老的渲染引擎，它**不支持解析 pathLength 属性**。
# 当 cairosvg 忽略了 pathLength 时，它将 "0.5" 错误地理解成了绝对的 "0.5 个像素"。
# 由于圆的半径是 40（周长约 251 像素），它硬生生地画出了约 500 条 0.5 像素的细线，导致肉眼看起来像是一圈密集的实线。
#
# 【解决方案】
# 放弃高级的相对比例属性，改用“硬编码的绝对像素值”来适配旧版渲染引擎：
# 1. 计算绝对周长：C = 2 * π * r = 2 * 3.1415926 * 40 ≈ 251.327
# 2. 计算平均分段：我们需要 8 个实线段和 8 个空白段。
# 3. 单段总长：251.327 / 8 ≈ 31.416
# 4. 拆分实线与空白（对半分）：31.416 / 2 ≈ 15.708 像素
# 5. 计算偏移量（保证虚线的交界点完美对齐中轴）：15.708 / 2 ≈ 7.854 像素
#
# 【最终代码修改】
# 从 <circle> 标签中删除：pathLength="8"
# 将属性替换为：stroke-dasharray="15.708 15.708" stroke-dashoffset="7.854"
# ==========================================

# SVG 代码模板
svg_code = f"""
<svg xmlns="http://www.w3.org/2000/svg" width="432" height="432" viewBox="0 0 432 432">
  <defs>
    <clipPath id="left-wave-clip"><rect x="0" y="0" width="128" height="256" /></clipPath>
    <clipPath id="right-wave-clip"><rect x="128" y="0" width="128" height="256" /></clipPath>
    <path id="gear-shape" d="M 242.02 112.99 L 242.02 143.01 L 236.33 147.10 A 110 110 0 0 1 231.37 165.62 L 234.25 172.01 L 219.24 198.01 L 212.26 198.71 A 110 110 0 0 1 198.71 212.26 L 198.01 219.24 L 172.01 234.25 L 165.62 231.37 A 110 110 0 0 1 147.10 236.33 L 143.01 242.02 L 112.99 242.02 L 108.90 236.33 A 110 110 0 0 1 90.38 231.37 L 83.99 234.25 L 57.99 219.24 L 57.29 212.26 A 110 110 0 0 1 43.74 198.71 L 36.76 198.01 L 21.75 172.01 L 24.63 165.62 A 110 110 0 0 1 19.67 147.10 L 13.98 143.01 L 13.98 112.99 L 19.67 108.90 A 110 110 0 0 1 24.63 90.38 L 21.75 83.99 L 36.76 57.99 L 43.74 57.29 A 110 110 0 0 1 57.29 43.74 L 57.99 36.76 L 83.99 21.75 L 90.38 24.63 A 110 110 0 0 1 108.90 19.67 L 112.99 13.98 L 143.01 13.98 L 147.10 19.67 A 110 110 0 0 1 165.62 24.63 L 172.01 21.75 L 198.01 36.76 L 198.71 43.74 A 110 110 0 0 1 212.26 57.29 L 219.24 57.99 L 234.25 83.99 L 231.37 90.38 A 110 110 0 0 1 236.33 108.90 Z M 192.13 96.38 L 199.35 123.32 L 194.84 128.00 A 66.84 66.84 0 0 1 192.56 145.30 L 195.71 150.98 L 181.76 175.14 L 175.26 175.26 A 66.84 66.84 0 0 1 161.42 185.88 L 159.62 192.13 L 132.68 199.35 L 128.00 194.84 A 66.84 66.84 0 0 1 110.70 192.56 L 105.02 195.71 L 80.86 181.76 L 80.74 175.26 A 66.84 66.84 0 0 1 70.12 161.42 L 63.87 159.62 L 56.65 132.68 L 61.16 128.00 A 66.84 66.84 0 0 1 63.44 110.70 L 60.29 105.02 L 74.24 80.86 L 80.74 80.74 A 66.84 66.84 0 0 1 94.58 70.12 L 96.38 63.87 L 123.32 56.65 L 128.00 61.16 A 66.84 66.84 0 0 1 145.30 63.44 L 150.98 60.29 L 175.14 74.24 L 175.26 80.74 A 66.84 66.84 0 0 1 185.88 94.58 Z" stroke="{COLOR_GOLD}" stroke-width="3" stroke-linejoin="round" />
    <g id="gear-rotated">
      <use href="#gear-shape" transform="translate(128 128) rotate(15) translate(-128 -128)" />
    </g>
    <g id="data-core">
      <circle cx="128" cy="128" r="40" fill="none" stroke="currentColor" stroke-width="3.5" stroke-dasharray="15.708 15.708" stroke-dashoffset="7.854" />
    </g>
  </defs>

  <g transform="scale(1.6875)">
    <path d="M 198.13 114.05 L 198.13 141.95 L 192.56 145.30 A 66.84 66.84 0 0 1 185.88 161.42 L 187.45 167.72 L 167.72 187.45 L 161.42 185.88 A 66.84 66.84 0 0 1 145.30 192.56 L 141.95 198.13 L 114.05 198.13 L 110.70 192.56 A 66.84 66.84 0 0 1 94.58 185.88 L 88.28 187.45 L 68.55 167.72 L 70.12 161.42 A 66.84 66.84 0 0 1 63.44 145.30 L 57.87 141.95 L 57.87 114.05 L 63.44 110.70 A 66.84 66.84 0 0 1 70.12 94.58 L 68.55 88.28 L 88.28 68.55 L 94.58 70.12 A 66.84 66.84 0 0 1 110.70 63.44 L 114.05 57.87 L 141.95 57.87 L 145.30 63.44 A 66.84 66.84 0 0 1 161.42 70.12 L 167.72 68.55 L 187.45 88.28 L 185.88 94.58 A 66.84 66.84 0 0 1 192.56 110.70 Z" fill="{COLOR_DARK_RED}" clip-path="url(#left-wave-clip)" />
    <path d="M 198.13 114.05 L 198.13 141.95 L 192.56 145.30 A 66.84 66.84 0 0 1 185.88 161.42 L 187.45 167.72 L 167.72 187.45 L 161.42 185.88 A 66.84 66.84 0 0 1 145.30 192.56 L 141.95 198.13 L 114.05 198.13 L 110.70 192.56 A 66.84 66.84 0 0 1 94.58 185.88 L 88.28 187.45 L 68.55 167.72 L 70.12 161.42 A 66.84 66.84 0 0 1 63.44 145.30 L 57.87 141.95 L 57.87 114.05 L 63.44 110.70 A 66.84 66.84 0 0 1 70.12 94.58 L 68.55 88.28 L 88.28 68.55 L 94.58 70.12 A 66.84 66.84 0 0 1 110.70 63.44 L 114.05 57.87 L 141.95 57.87 L 145.30 63.44 A 66.84 66.84 0 0 1 161.42 70.12 L 167.72 68.55 L 187.45 88.28 L 185.88 94.58 A 66.84 66.84 0 0 1 192.56 110.70 Z" fill="{COLOR_BEIGE}" clip-path="url(#right-wave-clip)" />

    <g>
      <use href="#gear-rotated" fill="{COLOR_BEIGE}" clip-path="url(#left-wave-clip)" fill-rule="evenodd" />
      <use href="#gear-rotated" fill="{COLOR_DARK_RED}" clip-path="url(#right-wave-clip)" fill-rule="evenodd" />
    </g>

    <use href="#data-core" color="{COLOR_BEIGE}" clip-path="url(#left-wave-clip)" />
    <use href="#data-core" color="{COLOR_DARK_RED}" clip-path="url(#right-wave-clip)" />

    <path d="M 198.13 114.05 L 198.13 141.95 L 192.56 145.30 A 66.84 66.84 0 0 1 185.88 161.42 L 187.45 167.72 L 167.72 187.45 L 161.42 185.88 A 66.84 66.84 0 0 1 145.30 192.56 L 141.95 198.13 L 114.05 198.13 L 110.70 192.56 A 66.84 66.84 0 0 1 94.58 185.88 L 88.28 187.45 L 68.55 167.72 L 70.12 161.42 A 66.84 66.84 0 0 1 63.44 145.30 L 57.87 141.95 L 57.87 114.05 L 63.44 110.70 A 66.84 66.84 0 0 1 70.12 94.58 L 68.55 88.28 L 88.28 68.55 L 94.58 70.12 A 66.84 66.84 0 0 1 110.70 63.44 L 114.05 57.87 L 141.95 57.87 L 145.30 63.44 A 66.84 66.84 0 0 1 161.42 70.12 L 167.72 68.55 L 187.45 88.28 L 185.88 94.58 A 66.84 66.84 0 0 1 192.56 110.70 Z" fill="none" stroke="{COLOR_GOLD}" stroke-width="3" stroke-linejoin="round" stroke-linecap="round" />
  </g>
</svg>
"""

# 解决 Windows 命令行下非 UTF-8 编码的 Unicode 输出报错问题
if sys.stdout:
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except AttributeError:
        pass


class AdaptiveIconRenderer:
    """
    自适应图标渲染器类。
    职责说明：
    - 管理图标渲染的基础画布参数与保存路径。
    - 负责后台调用 cairosvg 进行矢量的栅格化渲染。
    - 负责白底圆角矩形背景的组装与透明镂空图的生成。
    """
    def __init__(
        self,
        svg_template: str,
        canvas_size: int = 1080,
        app_icon_size: int = 900,
        output_dir: str = None
    ):
        self.svg_template = svg_template
        self.canvas_size = canvas_size
        self.app_icon_size = app_icon_size
        self.output_dir = output_dir or os.path.dirname(os.path.abspath(__file__))
        
        # 确保输出目录存在
        os.makedirs(self.output_dir, exist_ok=True)

    def _render_svg_to_image(self, size: int) -> Image.Image:
        """
        [内部私有职责] 将 SVG 文本转化为指定大小的 Pillow RGBA 图像
        """
        logo_bytes = cairosvg.svg2png(
            bytestring=self.svg_template.encode('utf-8'),
            output_width=size,
            output_height=size
        )
        return Image.open(io.BytesIO(logo_bytes))

    def _create_transparent_canvas(self) -> tuple[Image.Image, ImageDraw.ImageDraw]:
        """
        [内部私有职责] 创建干净的透明画布和画笔
        """
        canvas = Image.new("RGBA", (self.canvas_size, self.canvas_size), (0, 0, 0, 0))
        return canvas, ImageDraw.Draw(canvas)

    def generate_with_background(self, variant_name: str, radius_ratio: float, logo_scale: float):
        """
        [公共业务方法] 生成带白色圆角矩形卡片的自适应图标变体
        """
        canvas, draw = self._create_transparent_canvas()
        
        # 1. 绘制白色圆角背景
        corner_radius = int(self.app_icon_size * radius_ratio)
        bg_x0 = (self.canvas_size - self.app_icon_size) // 2
        bg_y0 = (self.canvas_size - self.app_icon_size) // 2
        bg_x1 = bg_x0 + self.app_icon_size
        bg_y1 = bg_y0 + self.app_icon_size
        
        draw.rounded_rectangle(
            (bg_x0, bg_y0, bg_x1, bg_y1),
            radius=corner_radius,
            fill="#FFFFFF"
        )
        
        # 2. 渲染 Logo (大小相对于背景卡片缩放)
        logo_size = int(self.app_icon_size * logo_scale)
        logo_image = self._render_svg_to_image(logo_size)
        
        # 3. 将 Logo 居中贴到白色背景上
        logo_x = (self.canvas_size - logo_image.width) // 2
        logo_y = (self.canvas_size - logo_image.height) // 2
        canvas.paste(logo_image, (logo_x, logo_y), logo_image)
        
        # 4. 保存
        output_filename = os.path.join(self.output_dir, f"app_icon_{variant_name}.png")
        canvas.save(output_filename, format="PNG")
        print(f" - 已生成: app_icon_{variant_name}.png (圆角: {radius_ratio:.2%}, 缩放: {logo_scale:.2%})")

    def generate_foreground_only(self, variant_name: str, logo_scale: float):
        """
        [公共业务方法] 生成无圆角背景、纯透明底的镂空大图标
        """
        canvas, _ = self._create_transparent_canvas()
        
        # 1. 渲染 Logo 前景 (直接相对于 1080px 大画布进行放大缩放)
        logo_size = int(self.canvas_size * logo_scale)
        logo_image = self._render_svg_to_image(logo_size)
        
        # 2. 居中贴合
        logo_x = (self.canvas_size - logo_image.width) // 2
        logo_y = (self.canvas_size - logo_image.height) // 2
        canvas.paste(logo_image, (logo_x, logo_y), logo_image)
        
        # 3. 保存
        output_filename = os.path.join(self.output_dir, f"app_icon_{variant_name}.png")
        canvas.save(output_filename, format="PNG")
        print(f" - 已生成: app_icon_{variant_name}.png (纯前景镂空无背景, 缩放大小: {logo_size}px / {logo_scale:.2%})")


if __name__ == "__main__":
    print("开始生成 App 图标变体 (基于 Class 职责划分)...")
    
    # 实例化渲染器 (注入 SVG 源码和基础画布大小参数)
    renderer = AdaptiveIconRenderer(
        svg_template=svg_code,
        canvas_size=1080,
        app_icon_size=900
    )
    
    # 变体 1: 手机桌面视觉还原版 (模拟 Launcher 遮罩裁剪效果，16.67% 圆角，80% 齿轮)
    renderer.generate_with_background(
        variant_name="launcher_visual_scale80",
        radius_ratio=18 / 108,
        logo_scale=0.80
    )
    
    # 变体 2: Google Play Store 新版大圆角规范 (30% 圆角，85% 齿轮缩放)
    renderer.generate_with_background(
        variant_name="playstore_expressive_scale85",
        radius_ratio=0.30,
        logo_scale=0.85
    )
    
    # 变体 3: 纯 Icon 镂空版 (无白底背景，齿轮尺寸调大，缩放直接基于 1080px 画布的 95%)
    renderer.generate_foreground_only(
        variant_name="pure_foreground_only",
        logo_scale=0.95
    )
    
    print(f"🎉 全部变体生成完毕！文件保存在：{renderer.output_dir}")