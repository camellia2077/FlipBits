# UK Sample Global Guardrails

Apply these rules to all Ukrainian sample-text faction profiles under `values-uk`.

1. Semantic priority
- Preserve core meaning, proposition structure, and imagery from the source line.
- Natural Ukrainian adaptation is required; literal word order is not required.
- Semantic fidelity has higher priority than rigid sentence-ending uniformity.

2. Register baseline
- Default narration should stay literary and controlled, not colloquial chat style.
- Avoid mixed-language phrasing and avoid mechanical transfer from neighboring languages.

3. Technical and formatting safety
- Keep Android placeholders unchanged: `%1$s`, `%2$d`, `%%`, etc.
- Do not introduce malformed escape sequences.
- Use XML-safe quoting (`&quot;`) when literal double quotes are required.

4. Consistency discipline
- Keep recurring faction lexicon stable inside one faction.
- Keep command cadence and worldview consistent per faction profile.

5. Stop criteria (must-follow)
- Do not keep rewriting lines once they are already natural, grammatically correct, and faithful to the intended tone.
- Make edits only for high-impact issues:
  - register mismatch
  - intent drift
  - clear semantic error or contradiction against source meaning
  - broken placeholders / formatting safety issues
- If a line is acceptable, keep it unchanged.
- Prefer minimal edits over stylistic churn.
