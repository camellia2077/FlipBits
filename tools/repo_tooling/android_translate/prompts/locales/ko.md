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
Preserve FlipBits protocol tokens such as mini, flash, pro, ultra, ASCII, UTF-8, Hex, Binary, Morse, Hz, payload, token, byte, and nibble when the existing Korean locale keeps them as technical terms.

[app_text_rule]
For app UI text, keep labels concise enough for Android buttons, tabs, segmented controls, mini-player sheets, sliders, and narrow cards.
Follow existing project terms where present: 오디오, 자, 바이트, 입력 규칙, 토큰, 니블, 페이로드, 낮은 캐리어, 높은 캐리어, 레인, 에너지, and 피치.
Use compact Korean labels rather than full explanatory sentences for controls and mode chips.
Keep protocol tokens and ASCII/UTF-8/Morse terms stable.

[sample_text_rule]
For built-in sample prose, preserve the dramatic audio-signal atmosphere while writing fluent Korean.
Keep pro-mode sample strings strict ASCII when the key or context says pro/ASCII.

[key_alignment_rule]
When filling missing entries, write Korean localization that matches the current XML terminology.
Avoid English-like sentence structure unless the string is intentionally a protocol token or fixed ASCII sample.
For missing compact input summaries, preserve placeholders and mirror the established counter pattern around `%1$d자 • %2$d바이트`.
