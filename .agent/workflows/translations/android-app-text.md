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

## Process

1. Confirm the English baseline file under `apps/audio_android/app/src/main/res/values/`.
2. Run key alignment when localized keys may be missing:

```powershell
python tools/scripts/android/translate/run.py key-alignment
```

3. Read the relevant `*_key_alignment.task.json` under `temp/translation_key_alignment_reports/`; use the markdown report only as a fallback.
4. If translation judgment is needed, generate app-text task artifacts:

```powershell
python tools/scripts/android/translate/run.py compare --text-type app_text
```

5. For a scoped agent job, prefer:

```powershell
python tools/scripts/android/translate/run.py compare --lang de --text-type app_text --group strings_audio --prompt-mode agent_json --output-dir temp/agent_jobs/job_001/reviews --job-dir temp/agent_jobs/job_001 --no-clean --json-output
```

6. Read the generated `*.task.json` and the referenced prompt doc from `PROMPT_REF`.
7. Produce translation suggestions according to the generated prompt/profile, not a duplicated rule list in this workflow.
8. Apply prepared replacements with the translate tool, then rerun key alignment and Android verification.

```powershell
python tools/scripts/android/translate/run.py build-replacements --input temp/agent_jobs/job_001/suggestions.json --output temp/agent_jobs/job_001/replacements.json --json-output
python tools/scripts/android/translate/run.py replace --json temp/agent_jobs/job_001/replacements.json --summary-out temp/agent_jobs/job_001/replace_result.json --job-dir temp/agent_jobs/job_001 --json-output
```
