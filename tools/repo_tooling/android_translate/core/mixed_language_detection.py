from __future__ import annotations

import re


# 这份检查故意收窄为“高置信度跨脚本异常检测”：
# - CJK 语言（中文/日文/韩文）只检查明显的拉丁脚本残留
# - 其他语言只检查明显的 CJK 残留
# 不再尝试做欧美语言之间的互查或英文 n-gram 检测，避免高误报。
CJK_LANGUAGE_PREFIXES = ("zh", "ja", "ko")
MIXED_LANGUAGE_SKIP_CODES = {"la"}

# 白名单：允许在任何语言中合法保留的英文代号/专有名词（需全小写）
WHITELIST = {
    "led", "ascii", "ping", "ui", "id", "ip", "app", "mac", "hex", "ack", "ok"
}

PLACEHOLDER_RE = re.compile(r"%\d*\$?[sdf]")
NON_LATIN_CHUNK_RE = re.compile(r"[a-zA-Z][a-zA-Z\s\-]*[a-zA-Z]|[a-zA-Z]")
WORD_SPLIT_RE = re.compile(r"[\s\-]+")
CJK_CHUNK_RE = re.compile(r"[\u3400-\u4DBF\u4E00-\u9FFF\u3040-\u30FF\uAC00-\uD7AF]+")


def clean_text(text: str) -> str:
    """清理 Android 特殊占位符和转义符，避免影响英文检测"""
    if not text:
        return ""
    text = PLACEHOLDER_RE.sub("", text)
    text = text.replace("\\n", " ").replace("\\t", " ").replace("\\", "")
    return text


def is_pro_ascii_context_key(key: str) -> bool:
    """
    Pro 模式本身就是 ASCII/byte/token 可视化语境。
    这些 key 在任何语言下都允许保留 ASCII、byte、Token 等英文协议词，不能按语言混杂处理。
    """
    return key.startswith("audio_pro_") or key == "validation_pro_ascii_only"


def check_ascii_range(target_text: str) -> list[str]:
    """
    Pro 示例文本在所有语言下都必须保持 ASCII。
    因此这里只检查字符范围，不按目标语言检查是否混入英文或其他语言。
    """
    cleaned = clean_text(target_text)
    return sorted({f"U+{ord(char):04X}({char})" for char in cleaned if ord(char) > 0x7F})


def is_cjk_language(lang_code: str) -> bool:
    """判断目标语言是否属于中日韩资源目录。"""
    return any(lang_code.startswith(prefix) for prefix in CJK_LANGUAGE_PREFIXES)


def should_skip_mixed_language_detection(lang_code: str) -> bool:
    """
    某些目录不是“目标语言翻译”，而是英语的风格化变体。
    例如 values-la 当前承载的是战锤风格的拉丁化英文；若按普通翻译去做英文残留检测会误报整份资源。
    """
    return lang_code in MIXED_LANGUAGE_SKIP_CODES


def describe_detection_strategy(lang_code: str) -> tuple[bool, str]:
    is_cjk_lang = is_cjk_language(lang_code)
    skip_mixed_language = should_skip_mixed_language_detection(lang_code)
    if skip_mixed_language:
        return is_cjk_lang, "Style-English Skip (仅保留 Pro ASCII 检查)"
    if is_cjk_lang:
        return is_cjk_lang, "CJK Check (仅查拉丁脚本残留)"
    return is_cjk_lang, "Non-CJK Check (仅查中日韩残留)"


def check_cjk_language_for_latin_chunks(target_text: str) -> list[str]:
    """
    针对中文、日文、韩文：
    直接寻找译文中的英文字母片段。如果该片段不完全由白名单词组构成，则判定为混杂。
    """
    cleaned = clean_text(target_text)
    chunks = NON_LATIN_CHUNK_RE.findall(cleaned)

    suspicious: list[str] = []
    for chunk in chunks:
        chunk = chunk.strip()
        if not chunk:
            continue

        words = WORD_SPLIT_RE.split(chunk)
        is_safe = all(word.lower() in WHITELIST or len(word) == 1 for word in words)
        if not is_safe:
            suspicious.append(chunk)

    return suspicious


def check_non_cjk_language_for_cjk_chunks(target_text: str) -> list[str]:
    """
    针对非中日韩语言：
    仅检查是否残留了明显的中日韩字符片段。
    这样能高置信度抓出“欧美语种里混进中文/日文/韩文”这类问题，
    同时避免欧美语言之间互查导致的大量误报。
    """
    cleaned = clean_text(target_text)
    return CJK_CHUNK_RE.findall(cleaned)
