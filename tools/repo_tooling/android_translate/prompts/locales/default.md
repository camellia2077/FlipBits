# Default Android Translation Locale

[profile_id]
default_translation

[mode]
standard_translation

[locale_note]
This locale uses the standard translation profile for the FlipBits Android app. FlipBits converts text into audible signal modes, plays generated audio, and shows Visual/Tokens views that explain bytes, tokens, and low/high bit signals.

[identity_rule]
Treat this locale as a normal target-language localization.
Do not translate transport mode labels such as mini, flash, pro, and ultra as ordinary adjectives.
Keep placeholders, numeric units, and protocol casing exactly valid for Android string resources.

[app_text_rule]
For app UI text, keep labels compact because many strings appear in Android buttons, tabs, segmented controls, mini-player sheets, sliders, and narrow cards.
Do not turn technical labels into explanatory sentences.

[sample_text_rule]
For built-in sample prose, preserve the dramatic audio-signal atmosphere: lamps, sealed machinery, watches, gates, furnaces, signals, vessels, and old records.
Do not add external-IP terms or faction names.
Keep pro-mode sample strings strict ASCII when the key or context says pro/ASCII.

[key_alignment_rule]
When filling missing entries, match the current XML file's established terminology for audio, characters/bytes, input rules, Visual/Tokens, Hex/Binary/Morse, payload, token, and low/high carriers.
Add only the missing localized resource entries requested by the report unless the nearby existing label has clearly drifted from the English source.
