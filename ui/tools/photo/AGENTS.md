# App Icon Generator Tool (ui/tools/photo/)

这个目录存放了自适应应用图标的生成与渲染工具，支持从矢量 SVG 模板批量栅格化输出不同规范规格的静态 PNG 变体。

---

## 目录结构与职责说明

* **`main.py`** (入口调度器)
  * 执行最前置的依赖检查（校验三个底层零件 SVG 是否存在）。
  * 引入 `cli` 解析命令行参数。
  * 自动在指定输出目录下拼装并创建子文件夹 `logo_photos/`。
  * 调度核心转换器接口并传入子命令、背景色及文件基准名执行生成。
* **`cli.py`** (分层命令行解析)
  * 定义了多级子命令（`all`、`launcher`、`playstore`、`pure`、`pure-bg`）。
  * 参数通过 `parents` 父解析器进行共享复用，包含：主色（`-pri`）、辅色（`-sec`）、轮廓色（`-out`）、预设主题（`-t` / `--theme`）、自定义输出基准名（`-n` / `--name`）、输出根目录（`-o`）和模板路径（`--svg-template`）。
  * 支持自动检测并载入指定的 Faction Theme 配色，当指定 `--theme` 且未提供 `--name` 时，自动使用主题名作为生成文件名。
* **`themes.py`** (预设 Faction 主题 Catalog 解析器)
  * 提供 `load_themes_from_toml(toml_path)` 接口，使用标准库的 `tomllib` (Python 3.11+) 或 `tomli` 动态加载和解析主题配色文件。
* **`themes.toml`** (阵营主题配色数据库)
  * 声明了项目官方的 18 个 Faction Theme（阵营主题）的 HEX 颜色配置（包含 `primary`、`secondary` 和 `outline` 配色，提取自 Android 端 `FactionThemeCatalog.kt`）。
* **`config.py`** (依赖零件验证)
  * 使用动态相对路径（`../../svg/parts/`）检测三个核心 SVG 矢量零件是否存在：`core_ring.svg`、`inner_gear.svg`、`outer_gear.svg`。
  * 若缺失任一零件，拦截后续生成并提示错误路径。
* **`svg_to_png.py`** (核心接口包装)
  * 提供 `convert_svg_to_png` 接口，解耦 CLI 解析与底层渲染生成逻辑。
* **`icon_renderer.py`** (图片像素栅格化渲染器)
  * 利用 `cairosvg` 和 `Pillow` 进行矢量的拼装、上色和裁剪。
  * 接受 `base_name` 参数作为生成文件的前缀（例如 `{base_name}_launcher_visual_scale80.png`）。
  * 定义了 4 种输出变体规格：
    1. 桌面 Launcher 版 (`{base_name}_launcher_visual_scale80.png`)：圆角 16.67% 的自适应卡片。卡片背景色可通过 `-bg` 定制（默认 `#FFFFFF`）。
    2. Play 商店版 (`{base_name}_playstore_expressive_scale85.png`)：大圆角 30% 自适应卡片。卡片背景色可通过 `-bg` 定制（默认 `#FFFFFF`）。
    3. 纯透明底镂空版 (`{base_name}_pure_foreground_only.png`)：透明背景，95% 尺寸齿轮。
    4. 纯色背景镂空版 (`{base_name}_pure_with_bg.png`)：全画布纯色填充背景。背景色可通过 `-bg` 定制（默认使用主题 `primary` 配色）。
* **`app_icon.svg`** (图标模板)
  * 包含占位符标记（如 `{COLOR_PRIMARY}`、`{COLOR_SECONDARY}` 等），运行时会被填充为具体的 Hex 颜色进行动态渲染。
* **`web/`** (网页动效控制台)
  * 提供网页端齿轮动画交互预览，支持在此微调动效、导出当前时间帧单张 SVG 图像。

---

## 常用运行命令

```bash
# 1. 一键生成所有 4 种变体 (默认使用 mars_relic 配色，输出名默认为 app_icon)
python ui/tools/photo/main.py

# 2. 传入预设 Faction Theme 快速生成 (例如: xeno_code, 输出文件名会自动命名为 xeno_code_xxx.png)
python ui/tools/photo/main.py all -t xeno_code

# 3. 指定自定义的主题 TOML 配置文件并使用其中的主题配色
python ui/tools/photo/main.py all --toml path/to/my_themes.toml -t my_custom_theme

# 4. 指定 Faction Theme 并自定义输出文件名 (输出名为 custom_alloy_xxx.png)
python ui/tools/photo/main.py all -t ancient_alloy -n custom_alloy

# 5. 只生成 Play 商店规格 of 图标，并使用 fires_of_fate 主题
python ui/tools/photo/main.py playstore -t fires_of_fate

# 6. 只生成纯透明底的镂空图标，并使用 sepulcher_cyan 主题
python ui/tools/photo/main.py pure -t sepulcher_cyan

# 7. 只生成带纯色背景的镂空图标，并指定 dynasty_revival 主题 (背景色默认为该主题的 primary)
python ui/tools/photo/main.py pure-bg -t dynasty_revival

# 8. 生成纯色背景镂空图，指定主题的同时重写背景底色为特定 Hex 颜色
python ui/tools/photo/main.py pure-bg -t toxic_effluence -bg FF00FF
```

---

## Faction Themes 预设列表
默认从同目录下的 `themes.toml` 中加载。可以在 `-t` / `--theme` 参数中传入以下预设名称：
* 圣机派（Sacred Machine）：`mars_relic` (默认), `scarlet_guard`, `black_crimson_rite`, `xeno_code`
* 血肉杀戮派（Scarlet Carnage）：`blood_soaked_ivory`, `brass_forge`
* 变化迷宫派（Labyrinth of Mutability）：`fires_of_fate`, `arcane_abyss`
* 奢华陨落派（Exquisite Fall）：`ecstatic_rapture`, `velvet_nightmare`
* 不朽腐烂派（Immortal Rot）：`toxic_effluence`, `plague_mire`
* 古代王朝派（Ancient Dynasty）：`dynasty_revival`, `sepulcher_cyan`, `tomb_sigil`, `ancient_alloy`, `void_fluctuation`, `crimson_decree`

---

## 后续修改与新增指南

1. **添加/修改主题**：
   * 直接修改 `themes.toml` 文件，以 TOML 格式增加或修改对应阵营配色。
   * 支持通过 `--toml` (或 `--themes-file`) 命令指定自定义的 TOML 配置文件。
2. **添加新的 PNG 图标输出变体**：
   * 在 `icon_renderer.py` 的 `AdaptiveIconRenderer` 类中，根据需要实现新的拼装方法。
   * 修改 `icon_renderer.py` 底部的 `run_icon_generation()`，增加条件分支判定并调用对应的方法。
