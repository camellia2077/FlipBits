# Brazilian Portuguese sample text global profile

Use natural Brazilian Portuguese for themed sample prose while preserving lineup tone and broad EN intent.

Hard constraints:
- Keep SHORT samples short and LONG samples long.
- Keep pro/ASCII sample keys strict ASCII when context indicates pro/ASCII.
- Prefer fluent pt-BR syntax over literal English order.
- Preserve imperative force when EN uses commands.

Stop rule:
- Do not rewrite lines already natural and semantically aligned.
- Only change when there is a clear issue (meaning drift, grammar, awkwardness, or length mismatch).
