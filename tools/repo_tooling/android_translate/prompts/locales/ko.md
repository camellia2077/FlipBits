# Korean Android Translation Locale

[profile_id]
korean_standard

[mode]
standard_korean

[locale_note]
Korean (`values-ko`) is a standard Korean localization for Android UI, playback, and signal-visualization text.

[identity_rule]
Treat this locale as contemporary Korean UI localization.
Avoid literal English word order in strings about playback speed, input rules, byte counts, token lyrics, and low/high bit visualization.

[app_text_rule]
For app UI text, keep labels concise enough for Android buttons, tabs, segmented controls, mini-player sheets, sliders, and narrow cards.
Use compact Korean labels rather than full explanatory sentences for controls and mode chips.
For theme and color translation tasks, also follow `tools/repo_tooling/android_translate/prompts/locales/ko_theme_colors.md`.

[sample_text_rule]
For built-in sample prose, preserve the dramatic audio-signal atmosphere while writing fluent Korean.
Keep pro-mode sample strings strict ASCII when the key or context says pro/ASCII.

[key_alignment_rule]
When filling missing entries, write Korean localization that matches the current XML terminology.
Avoid English-like sentence structure unless the string is intentionally a protocol token or fixed ASCII sample.
For missing compact input summaries, preserve placeholders and mirror the established counter pattern around `%1$d자 • %2$d바이트`.
When working on theme or palette entries, prefer the Korean theme/color rules above over older verbose or overly literal wording.
