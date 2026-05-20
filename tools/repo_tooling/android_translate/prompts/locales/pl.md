# Polish Android Translation Locale

[profile_id]
polish_standard

[mode]
standard_polish

[locale_note]
Polish (`values-pl`) is a standard Polish localization for Android UI, playback, and signal-visualization text.

[identity_rule]
Treat this locale as contemporary Polish UI localization.
Use natural Polish case, gender, and word order rather than literal English structure.

[app_text_rule]
For app UI text, keep labels compact enough for Android buttons, tabs, segmented controls, mini-player sheets, sliders, and narrow cards.
Follow existing project terms where present: audio, znaków, bajtów, reguły wejścia, Hex, Morse, payload, token, nibble, niska nośna, and wysoka nośna.
Do not turn short controls, visualizer mode labels, or transport labels into explanatory sentences.

[sample_text_rule]
For built-in sample prose, preserve the dramatic audio-signal atmosphere while writing fluent Polish.
Keep pro-mode sample strings strict ASCII, without Polish diacritics, when the key or context says pro/ASCII.

[key_alignment_rule]
When filling missing entries, write Polish localization that matches the current XML terminology.
Be careful with Polish case and number around placeholders; do not change placeholders or numeric units casually.
For missing compact input summaries, preserve placeholders and mirror the established counter pattern around `%1$d znaków • %2$d bajtów`.
