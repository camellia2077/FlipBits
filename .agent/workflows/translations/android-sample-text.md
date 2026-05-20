# Android Sample Text Translation

Use this workflow for localized audio sample prose in `audio_samples_*.xml`.

## When To Use

- default or random sample input text
- faction/lineup themed sample prose
- language-switching sample text

Do not use this workflow for normal app UI labels or dialogs in `strings_*.xml`; use [android-app-text.md](android-app-text.md).
If the task is specifically to find untranslated English leftovers inside `values-*`, use [check-untranslated-english.md](check-untranslated-english.md) first.

Tool definition for commands and JSON contracts lives in [tools/repo_tooling/android_translate/docs/README.md](/C:/code/WaveBits/tools/repo_tooling/android_translate/docs/README.md).

## Process

1. Identify the sample XML file, such as `audio_samples_sacred_machine_*.xml`.
2. Run `key-alignment` when localized keys may be missing:

```powershell
python tools/run.py android-translate key-alignment
```

3. Read the relevant `*_key_alignment.task.json` under `temp/translations/key_alignment/`; use the markdown report only as a fallback.
4. If translation judgment is needed, generate sample-text task artifacts:

```powershell
python tools/run.py android-translate mixed-language --lang fr --json-output
python tools/run.py android-translate mixed-language-context-audit --lang fr --json-output
```

Use the audit outputs when the risk is untranslated English prose surviving in the localized sample XML.

```powershell
python tools/run.py android-translate compare --text-type sample_text
```

5. For a scoped agent job, prefer:

```powershell
python tools/run.py android-translate compare --lang fr --text-type sample_text --group sacred_machine --prompt-mode agent_json --output-dir temp/agent_jobs/job_001/reviews --job-dir temp/agent_jobs/job_001 --no-clean --json-output
```

6. Read the generated `*.task.json` first.
7. Use the task JSON fields, especially `context`, `sample_length`, `decision_hint`, `ui_surface`, `length_pressure`, `locale_profile`, and `prompt_text_type`.
8. Open the referenced prompt doc from `PROMPT_REF` only when the JSON payload does not already explain the required style and constraints.
9. Produce translation suggestions according to the generated task JSON and prompt/profile, not a duplicated rule list in this workflow.
10. If the job started from suspicious English leftovers, check `needs_translation.md` / `needs_context.md` before editing sample prose.
11. Apply prepared replacements with `android-translate`, then rerun `key-alignment`, translation lint, and Android verification.

```powershell
python tools/run.py android-translate build-replacements --input temp/agent_jobs/job_001/suggestions.json --output temp/agent_jobs/job_001/replacements.json --json-output
python tools/run.py android-translate replace --json temp/agent_jobs/job_001/replacements.json --summary-out temp/agent_jobs/job_001/replace_result.json --job-dir temp/agent_jobs/job_001 --json-output
python tools/run.py android-translate lint --lang fr --json-output
```
