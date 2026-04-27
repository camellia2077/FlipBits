# Android Translate SOP

## Goal

This SOP is the human workflow for polishing Android localized strings with English as review reference.

It is optimized for:

- localized sample text
- split `strings_*.xml` app text
- small, precise edits instead of full-paragraph rewrites

The replacement step is intentionally strict so it can update XML safely.

Agent-specific workflow now lives in:

- [AGENTS.md](/C:/code/WaveBits/tools/scripts/android/translate/AGENTS.md)

## Human Workflow

1. Generate review markdown.

```powershell
py "C:\code\WaveBits\tools\scripts\android\translate\run.py" compare
```

2. Open the generated review file under:

- `temp/ai_translation_reviews/<lang>/app_text/...`
- `temp/ai_translation_reviews/<lang>/sample_text/...`

3. Paste the review markdown into your web AI chat and ask it to return JSON only.

Each review block includes:

- `DIR`
- `FILE`
- `KEY`
- `XML`
- `NAME`
- `EN`
- target-language text such as `IT`, `JA`, `ZH`, etc.

4. Save the returned JSON into:

- `tools/scripts/android/translate/replacements.json`

5. Run replace.

```powershell
py "C:\code\WaveBits\tools\scripts\android\translate\run.py" replace
```

Windows wrapper:

```powershell
"C:\code\WaveBits\tools\scripts\android\translate\replace.cmd"
```

6. If the JSON has a high-confidence syntax issue such as unescaped quotes inside a JSON string, use:

```powershell
"C:\code\WaveBits\tools\scripts\android\translate\replace_auto_fix_json.cmd"
```

## Review Rules

- English is review reference only.
- Replacement does not locate by English text.
- `find` must be an exact substring of the current localized string.
- `replace` should usually change only the smallest necessary span.
- Do not rewrite the full paragraph unless the paragraph truly needs a full rewrite.
- Inside JSON string values, use plain natural-language replacement text. Do not add XML entities or manual Android escaping.

## Common Commands

Generate review markdown:

```powershell
py "C:\code\WaveBits\tools\scripts\android\translate\run.py" compare
```

Run mixed-language report:

```powershell
py "C:\code\WaveBits\tools\scripts\android\translate\run.py" mixed-language
```

Run translation key alignment report:

```powershell
py "C:\code\WaveBits\tools\scripts\android\translate\run.py" key-alignment
```

Run replace:

```powershell
py "C:\code\WaveBits\tools\scripts\android\translate\run.py" replace
```

Run replace with JSON auto-fix:

```powershell
py "C:\code\WaveBits\tools\scripts\android\translate\run.py" replace --auto-fix-json
```
