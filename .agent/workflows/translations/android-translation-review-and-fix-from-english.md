# Android Translation Review Against English Baseline

Use this workflow when the task is to check localized XML text against the English baseline and prepare translation updates.

Tool definition for commands and JSON contracts lives in [tools/repo_tooling/android_translate/docs/README.md](/C:/code/WaveBits/tools/repo_tooling/android_translate/docs/README.md).

## When To Use

- verify whether localized text matches current English intent
- spot awkward, outdated, or drifted translations
- find localized entries that still show English text copied from the baseline
- prepare machine-readable translation tasks for agent review

If the task is only key existence/alignment, run `key-alignment` first and follow that workflow.

## Preferred High-Level Paths

Choose one of these paths before dropping to ad hoc scripting:

1. Scoped entry round-trip
   - Use when the target keys are already known and the job is narrow.
   - Flow: `export-entries` -> edit JSON -> `build-export-replacements` -> `replace`
   - This is the fastest path for key-scoped cleanup in files like `strings_settings.xml`.

2. Review-to-replacement pipeline
   - Use when the job needs translation review against current English intent.
   - Flow: `compare` -> read generated `*.task.json` / prompt refs -> suggestions JSON -> `build-replacements` -> `replace`
   - This is the default path for broader wording drift or consistency repair.

3. Untranslated-English triage
   - Use when a non-English locale visibly still shows English text in the app.
   - Flow: `mixed-language --lang <locale>` -> `mixed-language-context-audit --lang <locale>` -> fix `needs_translation` / `needs_context` -> `compare` or `export-entries` as needed
   - This is the default path for English leftovers that are not missing-key problems.

## Command-First Discovery

1. Discover available commands:

```powershell
python tools/run.py android-translate --help
```

2. Discover available dimensions before running scoped jobs:

```powershell
python tools/run.py android-translate list-text-types
python tools/run.py android-translate list-groups --text-type sample_text
python tools/run.py android-translate list-langs
```

## Process

1. Optional: detect untranslated English leftovers before deeper review:

```powershell
python tools/run.py android-translate mixed-language --lang ko --json-output
python tools/run.py android-translate mixed-language-context-audit --lang ko --json-output
```

Read `temp/translations/mixed_language/` and `temp/translations/mixed_language_audit/` first when the issue is English text still visible in a localized build.

2. Optional: generate low-markup EN/localized text inspection files:

```powershell
python tools/run.py android-translate dump-xml-md --lang ko --text-type sample_text --group sacred_machine --with-en --output-dir temp/agent_jobs/job_001/inspect
```

3. Generate scoped compare artifacts for translation review:

```powershell
python tools/run.py android-translate compare --lang ko --text-type sample_text --group sacred_machine --prompt-mode agent_json --output-dir temp/agent_jobs/job_001/reviews --job-dir temp/agent_jobs/job_001 --no-clean --json-output
```

Alternative high-level path for narrow key-scoped edits:

```powershell
python tools/run.py android-translate export-entries --lang ko --text-type app_text --group strings_settings --key-pattern "^palette_.*_title$" --output temp/agent_jobs/job_001/entries.ko.json --json-output
```

This writes structured `en_text/current_text/proposed_text` items plus execution fields such as `decision_hint`, `ui_surface`, and `length_pressure`, so the agent can edit directly without first generating compare markdown.

4. Read task artifacts in this order:
- `*.task.json` (primary)
- `_prompts/*.md` from `prompt_ref` (secondary)
- review markdown `*.md` only as fallback

JSON-first means:
- use `decision_hint` before guessing whether the entry should be translated, kept, or just reviewed
- use `ui_surface` before re-parsing XML names to infer where the string appears
- use `length_pressure` before rewriting compact labels into longer phrasing
- rely on `locale_profile` and optional `style_profile` as the main writing contract

5. Produce suggestions JSON from the task scope.

6. Convert suggestions into minimal replacements:

```powershell
python tools/run.py android-translate build-replacements --input temp/agent_jobs/job_001/suggestions.json --output temp/agent_jobs/job_001/replacements.json --json-output
```

Alternative high-level path when starting from edited export JSON:

```powershell
python tools/run.py android-translate build-export-replacements --input temp/agent_jobs/job_001/entries.ko.edited.json --output temp/agent_jobs/job_001/replacements.json --json-output
```

This converts item-level `current_text -> proposed_text` edits into minimal `find/replace` replacements for the strict `replace` step.

7. Apply replacements and capture result:

```powershell
python tools/run.py android-translate replace --json temp/agent_jobs/job_001/replacements.json --summary-out temp/agent_jobs/job_001/replace_result.json --job-dir temp/agent_jobs/job_001 --json-output
```

8. Re-run translation validation:

```powershell
python tools/run.py android-translate key-alignment --json-output
python tools/run.py android-translate lint --lang ko --json-output
python tools/run.py android-translate fix-resource-escapes --json-output
```

## Notes

- `values/` is the English baseline source of truth.
- `values-*` are localized targets.
- The shared locked-term policy excludes allowed English terms from untranslated-English detection.
- For subgroup-styled sample text (for example `ko` faction profiles), rely on `style_profile` in `*.task.json` instead of copying rules into this workflow file.
