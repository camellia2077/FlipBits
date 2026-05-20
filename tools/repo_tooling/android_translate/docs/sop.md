# Android Translate SOP

## Goal

This SOP is the human workflow for polishing Android localized strings with English as review reference.

It is optimized for:

- localized sample text
- split `strings_*.xml` app text
- small, precise edits instead of full-paragraph rewrites

The replacement step is intentionally strict so it can update XML safely.

Execution-step workflow now lives in:

- [README.md](/C:/code/WaveBits/tools/repo_tooling/android_translate/docs/README.md)

## Human Workflow

1. Generate review markdown.

```powershell
python tools/run.py android-translate compare
```

2. Open the generated review file under:

- `temp/translations/reviews/<lang>/app_text/...`
- `temp/translations/reviews/<lang>/sample_text/...`

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

- `tools/repo_tooling/android_translate/replacements.json`

5. Run replace.

```powershell
python tools/run.py android-translate replace --json tools/repo_tooling/android_translate/replacements.json
```

6. If the JSON has a high-confidence syntax issue such as unescaped quotes inside a JSON string, use:

```powershell
python tools/run.py android-translate replace --json tools/repo_tooling/android_translate/replacements.json --auto-fix-json
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
python tools/run.py android-translate compare
```

Run mixed-language report:

```powershell
python tools/run.py android-translate mixed-language
```

Run translation key alignment report:

```powershell
python tools/run.py android-translate key-alignment
```

Run replace:

```powershell
python tools/run.py android-translate replace --json tools/repo_tooling/android_translate/replacements.json
```

Run replace with JSON auto-fix:

```powershell
python tools/run.py android-translate replace --json tools/repo_tooling/android_translate/replacements.json --auto-fix-json
```
