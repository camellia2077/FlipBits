# Brazilian Portuguese Android Translation Locale

[profile_id]
brazilian_portuguese_standard

[mode]
standard_brazilian_portuguese

[locale_note]
Brazilian Portuguese (`values-pt-rBR`) is a standard Brazilian Portuguese localization for users who read Portuguese in Brazil.

[identity_rule]
Treat this locale as Brazilian Portuguese, not European Portuguese.
Prefer Brazilian product wording, spelling, and UI terminology.
Avoid European Portuguese-only terms when a clearer Brazilian expression exists, especially for app, file, screen, playback, and settings text.
Preserve WaveBits protocol tokens such as mini, flash, pro, ultra, ASCII, UTF-8, Hex, Binary, Morse, Hz, payload, token, byte, and nibble when the existing Brazilian Portuguese locale keeps them as technical terms.

[app_text_rule]
For app UI text, keep labels compact enough for Android buttons, tabs, segmented controls, mini-player sheets, sliders, and narrow cards.
Follow existing project terms where present: áudio, caracteres, bytes, regras de entrada, Visual, Hex, Morse, payload, token, nibble, portadora baixa, and portadora alta.
Use "arquivo" rather than European Portuguese "ficheiro" if file wording is needed.
Do not turn protocol labels or visualizer labels into explanatory sentences.

[sample_text_rule]
For built-in sample prose, preserve the dramatic audio-signal atmosphere in fluent Brazilian Portuguese.
Keep pro-mode sample strings strict ASCII, without accents, when the key or context says pro/ASCII.

[key_alignment_rule]
When filling missing entries, write Brazilian Portuguese localization.
Use Brazilian Portuguese terminology consistently and avoid European Portuguese variants unless already established in the same XML file.
For missing compact input summaries, preserve placeholders and mirror the established counter pattern around `%1$d caracteres • %2$d bytes`.
