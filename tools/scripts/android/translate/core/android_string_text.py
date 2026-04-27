from __future__ import annotations

import string
from xml.sax.saxutils import escape, unescape

_SUPPORTED_SINGLE_CHAR_ESCAPES = {"\\", "'", '"', "@", "?", "n", "t"}


def decode_android_string_resource_text(raw_text: str) -> str:
    """
    Convert XML text from an Android <string> node into the literal text we want
    review JSON and replacement matching to operate on.
    """
    xml_unescaped = unescape(raw_text)
    return _unescape_android_string(xml_unescaped)


def encode_android_string_resource_text(literal_text: str) -> str:
    """
    Convert literal replacement text into Android-safe string resource text.
    """
    android_escaped = _escape_android_string(literal_text)
    return escape(android_escaped)


def find_high_risk_android_string_resource_patterns(raw_text: str) -> list[str]:
    """
    Inspect the Android-escaped inner text of a <string> node and report
    patterns that frequently make aapt reject the resource.
    """
    xml_unescaped = unescape(raw_text)
    risks: list[str] = []
    index = 0
    while index < len(xml_unescaped):
        char = xml_unescaped[index]
        if char == "'":
            risks.append("contains raw ASCII apostrophe; write plain text in JSON and persist as \\' in XML")
            index += 1
            continue

        if char != "\\":
            index += 1
            continue

        if index == len(xml_unescaped) - 1:
            risks.append("ends with a trailing backslash")
            break

        next_char = xml_unescaped[index + 1]
        if next_char == "u":
            code_point = xml_unescaped[index + 2 : index + 6]
            if len(code_point) != 4 or any(ch not in string.hexdigits for ch in code_point):
                risks.append("contains an invalid \\u escape sequence")
                index += 2
            else:
                risks.append("contains a manual \\u escape sequence; JSON should contain literal Unicode text")
                index += 6
            continue

        if next_char not in _SUPPORTED_SINGLE_CHAR_ESCAPES:
            risks.append(f"contains unsupported Android escape \\{next_char}")
        index += 2

    return risks


def normalize_android_string_resource_text(raw_text: str) -> str:
    """
    Canonicalize the inner text of an Android <string> node.

    This is intended for high-confidence repair flows:
    - decode the current Android/XML representation into literal text
    - re-encode that literal text with the repository's Android-safe escaping

    Example:
    - d'accento   -> d\\'accento
    - Необов'язково -> Необов\\'язково
    """
    return encode_android_string_resource_text(decode_android_string_resource_text(raw_text))


def _escape_android_string(value: str) -> str:
    if not value:
        return value

    escaped_chars: list[str] = []
    for index, char in enumerate(value):
        if char == "\\":
            escaped_chars.append("\\\\")
        elif char == "'":
            escaped_chars.append("\\'")
        elif char == '"':
            escaped_chars.append('\\"')
        elif index == 0 and char in ("@", "?"):
            escaped_chars.append(f"\\{char}")
        else:
            escaped_chars.append(char)
    return "".join(escaped_chars)


def _unescape_android_string(value: str) -> str:
    if "\\" not in value:
        return value

    result: list[str] = []
    index = 0
    while index < len(value):
        char = value[index]
        if char != "\\" or index == len(value) - 1:
            result.append(char)
            index += 1
            continue

        next_char = value[index + 1]
        if next_char in ("\\", "'", '"', "@", "?"):
            result.append(next_char)
            index += 2
            continue

        if next_char == "n":
            result.append("\n")
            index += 2
            continue

        if next_char == "t":
            result.append("\t")
            index += 2
            continue

        # Preserve unsupported escapes literally so review/matching logic does
        # not silently drop the backslash and hide malformed resource text.
        result.append("\\")
        result.append(next_char)
        index += 2

    return "".join(result)
