# Android Sample Text Translation

Use this workflow for localized audio sample prose in `audio_samples_*.xml`.

## When To Use

- default or random sample input text
- faction/lineup themed sample prose
- language-switching sample text

Do not use this workflow for normal app UI labels or dialogs in `strings_*.xml`; use [android-app-text.md](android-app-text.md).

## Process

1. Identify the sample XML file, such as `audio_samples_sacred_machine_*.xml`.
2. Run key alignment when localized keys may be missing:

```powershell
python tools/scripts/android/translate/run.py key-alignment
```

3. Read the relevant `*_key_alignment.task.json` under `temp/translation_key_alignment_reports/`; use the markdown report only as a fallback.
4. If translation judgment is needed, generate sample-text task artifacts:

```powershell
python tools/scripts/android/translate/run.py compare --text-type sample_text
```

5. For a scoped agent job, prefer:

```powershell
python tools/scripts/android/translate/run.py compare --lang fr --text-type sample_text --group sacred_machine --prompt-mode agent_json --output-dir temp/agent_jobs/job_001/reviews --job-dir temp/agent_jobs/job_001 --no-clean --json-output
```

6. Read the generated `*.task.json` and the referenced prompt doc from `PROMPT_REF`.
7. Use the task JSON fields, especially `context`, `sample_length`, `locale_profile`, and `prompt_text_type`.
8. Produce translation suggestions according to the generated prompt/profile, not a duplicated rule list in this workflow.
9. Apply prepared replacements with the translate tool, then rerun key alignment and Android verification.

```powershell
python tools/scripts/android/translate/run.py build-replacements --input temp/agent_jobs/job_001/suggestions.json --output temp/agent_jobs/job_001/replacements.json --json-output
python tools/scripts/android/translate/run.py replace --json temp/agent_jobs/job_001/replacements.json --summary-out temp/agent_jobs/job_001/replace_result.json --job-dir temp/agent_jobs/job_001 --json-output
```
