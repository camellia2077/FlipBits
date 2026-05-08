# KO Sample Global Guardrails

Apply these rules to all Korean sample-text faction profiles under `values-ko`.

1. Output register baseline
- Default narration must use plain declarative style (`-다/-ㄴ다`), not polite UI register.
- Never use `-습니다/입니다` or `-요` unless a specific profile explicitly allows an exception.
- Semantic fidelity has higher priority than rigid sentence-ending uniformity.
- Do not force every sentence into the same ending form if that harms meaning, agency, or imagery.

2. Technical and formatting safety
- Keep Android placeholders unchanged: `%1$s`, `%2$d`, `%%`, etc.
- Do not introduce raw backslash escapes such as `\"`, `\'`, or malformed `\u` sequences in XML text.
- Use XML-safe quoting (`&quot;`) when literal double quotes are required.

3. Consistency discipline
- Preserve recurring faction lexicon and command cadence inside each profile.
- Keep non-target-language guidance in English; keep only necessary Korean grammar anchors.
- Avoid mechanical global ending replacement (for example blind `-합니다 -> -한다` substitution).

4. Stop criteria (must-follow)
- Do not keep rewriting lines once they are already natural, grammatically correct, and faithful to the intended tone.
- Make edits only for high-impact issues:
  - register mismatch (for example polite/honorific style where the faction requires non-polite style)
  - intent drift (for example serious line rendered as comedic, mocking, or casual)
  - clear semantic error or contradiction against source meaning
  - broken placeholders / formatting safety issues
- If a line is acceptable, keep it unchanged.
- Prefer minimal edits over stylistic churn.
