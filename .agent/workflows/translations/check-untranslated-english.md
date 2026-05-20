# Check Untranslated English

Use this workflow when a localized Android XML set may still contain English that was never translated.

Tool definition for commands and JSON contracts lives in [tools/repo_tooling/android_translate/docs/README.md](/C:/code/WaveBits/tools/repo_tooling/android_translate/docs/README.md).

This workflow has two parts:
1. find which locale still contains untranslated English
2. use the translation tooling to repair the target locale against the English baseline

Do not use this workflow for missing-key detection; use `key-alignment` for that.

## Part 1: Find Untranslated English

Choose the discovery command by language family.

For Latin-script locales where the main risk is `localized == EN`:

```powershell
python tools/run.py android-translate untranslated-equals-english --json-output
python tools/run.py android-translate untranslated-equals-english --lang de --json-output
```

Use this for locales such as:
- `de`
- `it`
- `fr`
- `es`
- `pt-rBR`
- `pl`

For CJK or Cyrillic locales where script-residual detection is still useful:

```powershell
python tools/run.py android-translate mixed-language --lang ja --json-output
python tools/run.py android-translate mixed-language-context-audit --lang ja --json-output
```

Use this for locales such as:
- `zh`
- `zh-rTW`
- `ja`
- `ko`
- `ru`
- `uk`

Read discovery outputs this way:
- `keep_en`: fixed English terms, protocol terms, brand names, native language names, or strict ASCII samples that should stay
- `needs_translation`: real untranslated English that should be localized
- `needs_context`: English that still needs stronger product context before deciding keep vs translate
- `missing_context`: English baseline keys that still lack `<!-- CONTEXT: ... -->`

## Part 2: Repair The Target Locale

Once you know the target locale and affected scope, use one of these paths.

For a narrow known key set:

```powershell
python tools/run.py android-translate export-entries --lang de --text-type app_text --group strings_settings --key-pattern "^brand_theme_.*_description$" --output temp/agent_jobs/job_001/entries.de.json --json-output
```

Edit `proposed_text`, then convert and apply:

```powershell
python tools/run.py android-translate build-export-replacements --input temp/agent_jobs/job_001/entries.de.edited.json --output temp/agent_jobs/job_001/replacements.json --json-output
python tools/run.py android-translate replace --json temp/agent_jobs/job_001/replacements.json --summary-out temp/agent_jobs/job_001/replace_result.json --job-dir temp/agent_jobs/job_001 --json-output
```

For broader EN-vs-localized review:

```powershell
python tools/run.py android-translate compare --lang de --text-type app_text --group strings_settings --prompt-mode agent_json --output-dir temp/agent_jobs/job_001/reviews --job-dir temp/agent_jobs/job_001 --no-clean --json-output
```

Then:
- read `*.task.json` first
- use `context`, `decision_hint`, `ui_surface`, `length_pressure`, and `locale_profile`
- generate suggestions JSON
- run `build-replacements`
- run `replace`

After edits, verify:

```powershell
python tools/run.py android-translate key-alignment --lang de --json-output
python tools/run.py android-translate lint --lang de --json-output
python tools/run.py android-translate fix-resource-escapes --json-output
```

## Notes

- The locked English term list comes from the shared translation policy, not ad hoc comments.
- For Latin-script locales, `untranslated-equals-english` is the primary discovery step.
- For CJK/Cyrillic locales, `mixed-language` plus `mixed-language-context-audit` remains the primary discovery step.
- Keep markdown titles in English and short when you add follow-up audit docs.
