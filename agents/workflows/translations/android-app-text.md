# Android App Text Translation

Use this workflow for Android app UI text in `strings_*.xml`.

## When To Use

- labels
- buttons
- dialogs
- validation messages
- settings text
- player and input UI copy

Do not use this workflow for literary audio sample text in `audio_samples_*.xml`; use [android-sample-text.md](android-sample-text.md).
If the task is specifically to find untranslated English leftovers inside `values-*`, use [check-untranslated-english.md](check-untranslated-english.md) first.

Tool definition for commands and JSON contracts lives in [tools/repo_tooling/android_translate/docs/README.md](../../../tools/repo_tooling/android_translate/docs/README.md).

## Quick Path For A Few New Keys

Use this lighter path when the change is only a small number of new Android UI text keys and you do not need a broader translation review.

1. Add the English baseline keys first:

```powershell
python tools/run.py android strings-add --file strings_settings.xml --key example_key --en "Example text"
```

2. Run key alignment to find localized files that are missing the new keys:

```powershell
python tools/run.py android-translate key-alignment
```

3. Read the generated `*_key_alignment.task.json` first and directly add the missing localized keys in the affected `strings_*.xml` files. Use the markdown report only if you need extra human-readable inspection.
4. Rerun key alignment and Android verification after the edits:

```powershell
python tools/run.py android-translate key-alignment
python tools/run.py android test-debug
```

Use the full workflow below when the change needs translation judgment, broader consistency review, or prepared replacement artifacts.

## Preferred High-Level Paths

Choose one of these two paths first:

1. Scoped entry round-trip
   - Use when the task is a known key subset inside one `strings_*.xml` group.
   - Export editable entries:

```powershell
python tools/run.py android-translate export-entries --lang de --text-type app_text --group strings_settings --key-pattern "^palette_.*_title$" --output temp/agent_jobs/job_001/entries.de.json --json-output
```

   - Edit the exported `proposed_text` values, then convert and apply:

```powershell
python tools/run.py android-translate build-export-replacements --input temp/agent_jobs/job_001/entries.de.edited.json --output temp/agent_jobs/job_001/replacements.json --json-output
python tools/run.py android-translate replace --json temp/agent_jobs/job_001/replacements.json --summary-out temp/agent_jobs/job_001/replace_result.json --job-dir temp/agent_jobs/job_001 --json-output
```

2. Review-to-replacement pipeline
   - Use when the task needs translation judgment or wider EN-vs-localized review.
   - Generate scoped compare artifacts, produce suggestions JSON, then convert with `build-replacements` and apply with `replace`.

Before broader review, optionally run untranslated-English detection when the risk is English fallback text surviving in a non-English locale:

```powershell
python tools/run.py android-translate mixed-language --lang de --json-output
python tools/run.py android-translate mixed-language-context-audit --lang de --json-output
```

## Process

1. Confirm the English baseline file under `apps/audio_android/app/src/main/res/values/`.
2. Run `key-alignment` when localized keys may be missing:

```powershell
python tools/run.py android-translate key-alignment
```

3. Read the relevant `*_key_alignment.task.json` under `temp/translations/key_alignment/`; use the markdown report only as a fallback.
4. If translation judgment is needed, generate app-text task artifacts:

```powershell
python tools/run.py android-translate compare --text-type app_text
```

For direct scoped entry export when you already know the target keys and want a machine-editable JSON instead of review markdown:

```powershell
python tools/run.py android-translate export-entries --lang de --text-type app_text --group strings_settings --key-pattern "^palette_.*_title$" --output temp/agent_jobs/job_001/entries.de.json --json-output
```

5. For a scoped agent job, prefer:

```powershell
python tools/run.py android-translate compare --lang de --text-type app_text --group strings_audio --prompt-mode agent_json --output-dir temp/agent_jobs/job_001/reviews --job-dir temp/agent_jobs/job_001 --no-clean --json-output
```

6. Read the generated `*.task.json` first.
7. Use `decision_hint`, `ui_surface`, and `length_pressure` to decide whether each entry is a compact UI label, a broader description, or a likely no-change item.
8. Open the referenced prompt doc from `PROMPT_REF` only when the task JSON fields and `locale_profile` are not enough.
9. Produce translation suggestions according to the generated task JSON and prompt/profile, not a duplicated rule list in this workflow.
10. If the job started from suspicious English leftovers, check `needs_translation.md` / `needs_context.md` from `mixed-language-context-audit` before editing strings.
11. Apply prepared replacements with `android-translate`, then rerun `key-alignment`, translation lint, and Android verification.

```powershell
python tools/run.py android-translate build-replacements --input temp/agent_jobs/job_001/suggestions.json --output temp/agent_jobs/job_001/replacements.json --json-output
python tools/run.py android-translate replace --json temp/agent_jobs/job_001/replacements.json --summary-out temp/agent_jobs/job_001/replace_result.json --job-dir temp/agent_jobs/job_001 --json-output
python tools/run.py android-translate lint --lang de --json-output
```

If your working artifact is an edited `export-entries` JSON, convert it with:

```powershell
python tools/run.py android-translate build-export-replacements --input temp/agent_jobs/job_001/entries.de.edited.json --output temp/agent_jobs/job_001/replacements.json --json-output
```
