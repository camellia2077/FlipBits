# Japanese Android Translation Locale

[profile_id]
japanese_standard

[mode]
standard_japanese

[locale_note]
Japanese (`values-ja`) is a standard Japanese localization for Android UI, theme settings, playback, and signal-visualization text.

[identity_rule]
Treat this locale as contemporary Japanese UI localization.
Prefer natural Japanese wording, punctuation, and product-label rhythm over literal English or Chinese-style transfer.
Do not translate transport mode labels such as mini, flash, pro, and ultra as ordinary adjectives.
Keep placeholders, numeric units, protocol casing, and Android resource syntax exactly valid.

[app_text_rule]
For app UI text, keep labels compact and natural for Android buttons, tabs, segmented controls, mini-player sheets, sliders, and narrow cards.
Do not turn technical labels into explanatory sentences.
For theme and color naming, use natural Japanese product naming rather than stiff Sino-Japanese calques.
For dual-tone theme titles, allow kanji when the result reads naturally as a Japanese title, but avoid hard literal compounds that feel like direct Chinese-source translation.
For dual-tone theme descriptions, prefer short color-list phrasing such as `色A、色B。` or `色A、色B、色C。` instead of lyrical atmosphere copy.
For Material palette names, use a katakana-first naming style and avoid mixing kanji, hiragana, and katakana naming systems unless a term is already a stable product-style Japanese color name.

[sample_text_rule]
For built-in sample prose, preserve the dramatic audio-signal atmosphere while writing fluent Japanese.
Avoid hard literal transfer from English.
Keep pro-mode sample strings strict ASCII when the key or context says pro/ASCII.

[key_alignment_rule]
When filling missing entries, write Japanese localization that matches the current XML terminology.
Keep theme, palette, playback, Visual/Tokens, byte, and low/high wording consistent with neighboring strings.
When working on theme and color entries, follow the Japanese theme/color naming rules above instead of older verbose or overly literal wording.
