# Ukrainian Android Translation Locale

[profile_id]
ukrainian_standard

[mode]
standard_ukrainian

[locale_note]
Ukrainian (`values-uk`) is a standard Ukrainian localization for Ukrainian users. Treat it as its own target-language locale with contemporary Ukrainian usage.

[identity_rule]
Treat this locale as standard Ukrainian localization for Ukrainian users.
Prefer natural contemporary Ukrainian wording, terminology, grammar, rhythm, and UI phrasing.
Do not treat Ukrainian as interchangeable with any neighboring language.
Avoid calques, mixed-language phrasing, or wording that sounds mechanically transferred from another language.
Preserve WaveBits protocol tokens such as mini, flash, pro, ultra, ASCII, UTF-8, Hex, Binary, Morse, Hz, payload, token, and byte when the existing Ukrainian locale keeps them as technical product terms.

[app_text_rule]
For app UI text, keep labels concise and natural in Ukrainian.
Use stable Ukrainian product terminology across related controls, settings, dialogs, and validation messages.
Prefer the clearer Ukrainian expression when direct transfer from another language would sound unnatural.
Follow existing Ukrainian terms for this app where present: символів/байтів for character and byte counters, Правила введення for Input rules, Лірика for Lyrics, Бінарний for Binary, несуча for carrier, and нібл for nibble.
Keep mini-player, tab, segmented-control, and chip labels short.

[sample_text_rule]
For sample prose, preserve tone and atmosphere while writing fluent Ukrainian.
Avoid mechanically transferred syntax or literal English word order; adapt rhythm and imagery into natural Ukrainian.
Keep pro-mode sample strings strict ASCII or established ASCII transliteration when the key or context says pro/ASCII.

[key_alignment_rule]
When filling missing entries, write Ukrainian localization for Ukrainian users.
Do not use mixed-language phrasing or grammar patterns that do not fit natural Ukrainian.
Match the existing Ukrainian locale's terminology while improving any obviously unnatural transfer wording encountered nearby.
For missing compact input summaries, preserve placeholders and mirror the established counter pattern around `%1$d символів • %2$d байтів`.
