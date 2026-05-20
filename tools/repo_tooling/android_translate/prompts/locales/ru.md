# Russian Android Translation Locale

[profile_id]
russian_standard

[mode]
standard_russian

[locale_note]
Russian (`values-ru`) is a standard Russian localization for Android UI, playback, and signal-visualization text.

[identity_rule]
Treat this locale as contemporary Russian UI localization.
Prefer natural Russian wording, grammar, and compact UI phrasing over literal English word order.

[app_text_rule]
For app UI text, keep labels concise enough for Android buttons, tabs, segmented controls, mini-player sheets, sliders, and narrow cards.
Follow existing project terms where present: аудио, символов, байт, правила ввода, Hex, Morse, payload, Token, nibble, низкая несущая, and высокая несущая.
Do not turn short controls, visualizer mode labels, or transport labels into explanatory sentences.
Keep product mode names mini, flash, pro, and ultra lowercase unless the string begins with a sentence that already capitalizes nearby product terms.

[sample_text_rule]
For built-in sample prose, preserve the dramatic audio-signal atmosphere while writing fluent Russian.
Keep pro-mode sample strings strict ASCII when the key or context says pro/ASCII.

[key_alignment_rule]
When filling missing entries, write Russian localization that matches the current XML terminology.
Avoid literal English syntax in playback, input-rule, byte-count, and visualizer strings.
For missing compact input summaries, preserve placeholders and mirror the established counter pattern around `%1$d символов • %2$d байт`.
