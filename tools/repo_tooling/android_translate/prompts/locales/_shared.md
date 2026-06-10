# Shared Android Translation Locale Constraints

[profile_id]
shared_constraints

[mode]
shared_rules

[locale_note]
Shared constraints that apply to every locale profile unless explicitly overridden.

[identity_rule]
Never translate these product/protocol terms; preserve exact English spelling and casing: flash, pro, mini, ultra, ASCII, UTF-8, Hex, Binary, Morse, Emoji, Token, Tokens, Debug, Demo mode, Animation, Mix, English, Deutsch, Español, Português, Français, Italiano, Русский, Українська, 한국어, 日本語, 简体中文, 繁體中文, Polski, Altum Gothicum.
Apply this locked-term rule automatically; locale-specific profiles should only add exceptions or language-specific phrasing guidance when truly necessary.

[app_text_rule]
Keep the non-translatable term list unchanged in UI labels, hints, validation text, and settings descriptions: flash, pro, mini, ultra, ASCII, UTF-8, Hex, Binary, Morse, Emoji, Token, Tokens, Debug, Demo mode, Animation, Mix, English, Deutsch, Español, Português, Français, Italiano, Русский, Українська, 한국어, 日本語, 简体中文, 繁體中文, Polski, Altum Gothicum.

[sample_text_rule]
When sample text contains locked protocol/product terms, keep them exactly as English tokens: flash, pro, mini, ultra, ASCII, UTF-8, Hex, Binary, Morse, Emoji, Token, Tokens, Debug, Demo mode, Animation, Mix, English, Deutsch, Español, Português, Français, Italiano, Русский, Українська, 한국어, 日本語, 简体中文, 繁體中文, Polski, Altum Gothicum.

[key_alignment_rule]
If an English source string contains any locked term, localized output must retain the same English token form: flash, pro, mini, ultra, ASCII, UTF-8, Hex, Binary, Morse, Emoji, Token, Tokens, Debug, Demo mode, Animation, Mix, English, Deutsch, Español, Português, Français, Italiano, Русский, Українська, 한국어, 日本語, 简体中文, 繁體中文, Polski, Altum Gothicum.
