import os
import sys

# tomllib 是 Python 3.11+ 标准库，若在旧版本中运行则尝试导入第三方库 tomli
try:
    import tomllib
except ImportError:
    try:
        import tomli as tomllib
    except ImportError:
        tomllib = None

def load_themes_from_toml(toml_path: str) -> dict:
    """
    从指定 TOML 配置文件动态加载 Faction Theme 预设。
    解析大类归属结构，并扁平化打平为一个以单个主题名称作为 Key 的字典，
    从而保持命令行 -t/--theme 参数的直接调用兼容。
    """
    if not os.path.exists(toml_path):
        print(f"⚠️ 警告: 未找到主题配置文件: {toml_path}", file=sys.stderr)
        return {}
    
    raw_data = {}
    parsed_via_standard = False
    
    # 优先尝试标准 TOML 解析
    if tomllib is not None:
        try:
            with open(toml_path, "rb") as f:
                raw_data = tomllib.load(f)
                parsed_via_standard = True
        except Exception:
            pass

    # 若标准解析失败（由于未加引号的中文裸键），则采用自定义解析器
    if not parsed_via_standard:
        raw_data = {}
        current_section = None
        try:
            with open(toml_path, "r", encoding="utf-8") as f:
                for line in f:
                    line_str = line.strip()
                    if not line_str or line_str.startswith("#"):
                        continue
                    # 匹配双中括号 [[header]] 或单中括号 [header] 格式
                    if line_str.startswith("[[") and line_str.endswith("]]"):
                        sec_name = line_str[2:-2].strip()
                        current_section = sec_name
                        if current_section not in raw_data:
                            raw_data[current_section] = []
                        raw_data[current_section].append({})
                    elif line_str.startswith("[") and line_str.endswith("]"):
                        sec_name = line_str[1:-1].strip()
                        current_section = sec_name
                        raw_data[current_section] = {}
                    elif "=" in line_str and current_section is not None:
                        key, val = line_str.split("=", 1)
                        key = key.strip()
                        val = val.strip()
                        # 剥除可能存在的双引号或单引号
                        if (val.startswith('"') and val.endswith('"')) or (val.startswith("'") and val.endswith("'")):
                            val = val[1:-1]
                        
                        target = raw_data[current_section]
                        if isinstance(target, list):
                            target[-1][key] = val
                        else:
                            target[key] = val
        except Exception as e:
            print(f"❌ 运行失败: 无法手工解析配置文件 {toml_path}: {e}", file=sys.stderr)
            sys.exit(1)

    # 核心转换职责：将读取到的层级数据，扁平化转成以每个主题自身“name”或 ID 为 Key 的打平字典
    themes_flattened = {}
    for section_key, section_val in raw_data.items():
        if isinstance(section_val, list):
            # 表数组 [[mars_relic]] 格式
            for item in section_val:
                theme_key = item.get("name")
                if theme_key:
                    themes_flattened[theme_key] = item
                # 同时也把原组名（若没有单独的主题冲突）映射到数组第一个元素，保证前向兼容
                if section_key not in themes_flattened and section_val:
                    themes_flattened[section_key] = section_val[0]
        elif isinstance(section_val, dict):
            # 普通表 [mars_relic] 格式
            themes_flattened[section_key] = section_val
            theme_key = section_val.get("name")
            if theme_key:
                themes_flattened[theme_key] = section_val
                
    return themes_flattened
