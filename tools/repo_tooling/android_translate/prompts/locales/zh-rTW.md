# Taiwan Traditional Chinese Android Translation Locale

[profile_id]
taiwan_traditional_chinese_standard

[mode]
standard_taiwan_traditional_chinese

[locale_note]
Traditional Chinese (`values-zh-rTW`) is a standard Traditional Chinese localization using Taiwan-region written usage.

[identity_rule]
Treat this locale as Traditional Chinese localization using Taiwan-region written usage.
Prefer natural contemporary Traditional Chinese wording, terminology, grammar, punctuation, and UI phrasing familiar to users in Taiwan.
Use Traditional Chinese characters consistently.
Avoid calques, mixed-locale phrasing, or wording that sounds mechanically transferred from English or Simplified Chinese.
Focus on script, locale terminology, punctuation, and natural UI phrasing.

[app_text_rule]
For app UI text, keep labels concise and natural in Taiwan Traditional Chinese.
Use stable Taiwan Traditional Chinese product terminology across related controls, settings, dialogs, and validation messages.
Prefer the clearer Taiwan Traditional Chinese expression when direct transfer from another locale would sound unnatural.
Keep mini-player, tab, segmented-control, and chip labels short.
For theme and color translation tasks, also follow `tools/repo_tooling/android_translate/prompts/locales/zh-rTW_theme_colors.md`.

[sample_text_rule]
For sample prose, preserve tone and atmosphere while writing fluent Taiwan Traditional Chinese.
Avoid mechanically transferred syntax or literal English word order; adapt rhythm and imagery into natural Taiwan Traditional Chinese.
Keep pro-mode sample strings strict ASCII when the key or context says pro/ASCII.

[key_alignment_rule]
When filling missing entries, write Taiwan Traditional Chinese localization.
Use Traditional Chinese characters and Taiwan-region terminology consistently.
Do not copy Simplified Chinese phrasing or mixed-locale wording unless the term is intentionally shared.
For missing compact input summaries, preserve placeholders and mirror the established counter pattern around `%1$d 個字元 • %2$d 位元組`.
When working on theme or palette entries, prefer the Taiwan Traditional Chinese theme/color rules above over older verbose or overly literal wording.
