import os
import io
import cairosvg
from PIL import Image, ImageDraw

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
        output_dir: str = None,
        base_name: str = "app_icon"
    ):
        self.svg_template = svg_template
        self.canvas_size = canvas_size
        self.app_icon_size = app_icon_size
        self.output_dir = output_dir
        self.base_name = base_name
        
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

    def generate_with_background(self, variant_name: str, radius_ratio: float, logo_scale: float, bg_color: str = "#FFFFFF"):
        """
        [公共业务方法] 生成带指定背景色圆角矩形卡片的自适应图标变体
        """
        canvas, draw = self._create_transparent_canvas()
        
        # 1. 绘制指定背景色的圆角背景
        corner_radius = int(self.app_icon_size * radius_ratio)
        bg_x0 = (self.canvas_size - self.app_icon_size) // 2
        bg_y0 = (self.canvas_size - self.app_icon_size) // 2
        bg_x1 = bg_x0 + self.app_icon_size
        bg_y1 = bg_y0 + self.app_icon_size
        
        draw.rounded_rectangle(
            (bg_x0, bg_y0, bg_x1, bg_y1),
            radius=corner_radius,
            fill=bg_color
        )
        
        # 2. 渲染 Logo (大小相对于背景卡片缩放)
        logo_size = int(self.app_icon_size * logo_scale)
        logo_image = self._render_svg_to_image(logo_size)
        
        # 3. 将 Logo 居中贴到白色背景上
        logo_x = (self.canvas_size - logo_image.width) // 2
        logo_y = (self.canvas_size - logo_image.height) // 2
        canvas.paste(logo_image, (logo_x, logo_y), logo_image)
        
        # 4. 保存
        output_filename = os.path.join(self.output_dir, f"{self.base_name}_{variant_name}.png")
        canvas.save(output_filename, format="PNG")
        print(f" - 已生成: {self.base_name}_{variant_name}.png (背景色: {bg_color}, 圆角: {radius_ratio:.2%}, 缩放: {logo_scale:.2%})")

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
        output_filename = os.path.join(self.output_dir, f"{self.base_name}_{variant_name}.png")
        canvas.save(output_filename, format="PNG")
        print(f" - 已生成: {self.base_name}_{variant_name}.png (纯前景镂空无背景, 缩放大小: {logo_size}px / {logo_scale:.2%})")

    def generate_foreground_with_solid_background(self, variant_name: str, logo_scale: float, bg_color: str):
        """
        [公共业务方法] 生成带纯色背景（无卡片圆角，全画布填充）的镂空大图标变体
        """
        # 创建带有指定纯色背景的画布
        canvas = Image.new("RGBA", (self.canvas_size, self.canvas_size), bg_color)
        
        # 1. 渲染 Logo 前景 (直接相对于 1080px 大画布进行放大缩放)
        logo_size = int(self.canvas_size * logo_scale)
        logo_image = self._render_svg_to_image(logo_size)
        
        # 2. 居中贴合
        logo_x = (self.canvas_size - logo_image.width) // 2
        logo_y = (self.canvas_size - logo_image.height) // 2
        canvas.paste(logo_image, (logo_x, logo_y), logo_image)
        
        # 3. 保存
        output_filename = os.path.join(self.output_dir, f"{self.base_name}_{variant_name}.png")
        canvas.save(output_filename, format="PNG")
        print(f" - 已生成: {self.base_name}_{variant_name}.png (纯色背景: {bg_color}, 缩放大小: {logo_size}px / {logo_scale:.2%})")


def run_icon_generation(
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
    加载 SVG 模板，填充颜色，并根据指定的子命令渲染对应的 PNG 变体。
    """
    if not os.path.exists(svg_template_path):
        raise FileNotFoundError(f"SVG 模板文件未找到: {svg_template_path}")
        
    with open(svg_template_path, 'r', encoding='utf-8') as f:
        svg_content = f.read()
        
    # 静态虚线圆环，直接使用原始的 SVG `<use>` 结构引用 `<g id="data-core">`
    static_core_ring = (
        f'    <use href="#data-core" color="{color_primary}" clip-path="url(#left-wave-clip)" />\n'
        f'    <use href="#data-core" color="{color_secondary}" clip-path="url(#right-wave-clip)" />'
    )
    
    # 填充颜色，并替换动画默认参数（无旋转，进度100%显示）
    svg_filled = (svg_content
                  .replace("{COLOR_PRIMARY}", color_primary)
                  .replace("{COLOR_SECONDARY}", color_secondary)
                  .replace("{COLOR_OUTLINE}", color_outline)
                  .replace("{OUTER_ROTATION}", "0")
                  .replace("{MIDDLE_ROTATION}", "0")
                  .replace("{CORE_RING_PATHS}", static_core_ring))
    
    # 实例化渲染器 (注入 SVG 源码和基础画布大小参数)
    renderer = AdaptiveIconRenderer(
        svg_template=svg_filled,
        canvas_size=1080,
        app_icon_size=900,
        output_dir=output_dir,
        base_name=base_name
    )
    
    # 变体 1: 手机桌面视觉还原版 (模拟 Launcher 遮罩裁剪效果，16.67% 圆角，80% 齿轮)
    if subcommand in ("all", "launcher"):
        card_bg = bg_color if (subcommand == "launcher" and bg_color is not None) else "#FFFFFF"
        renderer.generate_with_background(
            variant_name="launcher_visual_scale80",
            radius_ratio=18 / 108,
            logo_scale=0.80,
            bg_color=card_bg
        )
    
    # 变体 2: Google Play Store 新版大圆角规范 (30% 圆角，85% 齿轮缩放)
    if subcommand in ("all", "playstore"):
        card_bg = bg_color if (subcommand == "playstore" and bg_color is not None) else "#FFFFFF"
        renderer.generate_with_background(
            variant_name="playstore_expressive_scale85",
            radius_ratio=0.30,
            logo_scale=0.85,
            bg_color=card_bg
        )
    
    # 变体 3: 纯 Icon 镂空版 (无白底背景，齿轮尺寸调大，缩放直接基于 1080px 画立的 95%)
    if subcommand in ("all", "pure"):
        renderer.generate_foreground_only(
            variant_name="pure_foreground_only",
            logo_scale=0.95
        )
    
    # 变体 4: 纯 Icon 填色背景版 (使用指定 bg_color，未指定则默认使用 primary，尺寸与纯 Icon 镂空版一致 95%)
    if subcommand in ("all", "pure-bg"):
        solid_bg = bg_color if (subcommand == "pure-bg" and bg_color is not None) else color_primary
        renderer.generate_foreground_with_solid_background(
            variant_name="pure_with_bg",
            logo_scale=0.95,
            bg_color=solid_bg
        )
