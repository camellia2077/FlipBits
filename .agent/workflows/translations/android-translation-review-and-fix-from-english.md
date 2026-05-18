# Android Translation Review Against English Baseline

Use this workflow when the task is to check localized XML text against the English baseline and prepare translation updates.

## When To Use

- verify whether localized text matches current English intent
- spot awkward, outdated, or drifted translations
- prepare machine-readable translation tasks for agent review

If the task is only key existence/alignment, run `key-alignment` first and follow that workflow.

## Preferred High-Level Paths

Choose one of these two paths before dropping to ad hoc scripting:

1. Scoped entry round-trip
   - Use when the target keys are already known and the job is narrow.
   - Flow: `export-entries` -> edit JSON -> `build-export-replacements` -> `replace`
   - This is the fastest path for key-scoped cleanup in files like `strings_settings.xml`.

2. Review-to-replacement pipeline
   - Use when the job needs translation review against current English intent.
   - Flow: `compare` -> read generated `*.task.json` / prompt refs -> suggestions JSON -> `build-replacements` -> `replace`
   - This is the default path for broader wording drift or consistency repair.

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

1. Optional: generate low-markup EN/localized text inspection files:

```powershell
python tools/run.py android-translate dump-xml-md --lang ko --text-type sample_text --group sacred_machine --with-en --output-dir temp/agent_jobs/job_001/inspect
```

2. Generate scoped compare artifacts for translation review:

```powershell
python tools/run.py android-translate compare --lang ko --text-type sample_text --group sacred_machine --prompt-mode agent_json --output-dir temp/agent_jobs/job_001/reviews --job-dir temp/agent_jobs/job_001 --no-clean --json-output
```

Alternative high-level path for narrow key-scoped edits:

```powershell
python tools/run.py android-translate export-entries --lang ko --text-type app_text --group strings_settings --key-pattern "^palette_.*_title$" --output temp/agent_jobs/job_001/entries.ko.json --json-output
```

This writes structured `en_text/current_text/proposed_text` items that can be edited directly without first generating compare markdown.

3. Read task artifacts in this order:
- `*.task.json` (primary)
- `_prompts/*.md` from `prompt_ref` (secondary)
- review markdown `*.md` only as fallback

4. Produce suggestions JSON from the task scope.

5. Convert suggestions into minimal replacements:

```powershell
python tools/run.py android-translate build-replacements --input temp/agent_jobs/job_001/suggestions.json --output temp/agent_jobs/job_001/replacements.json --json-output
```

Alternative high-level path when starting from edited export JSON:

```powershell
python tools/run.py android-translate build-export-replacements --input temp/agent_jobs/job_001/entries.ko.edited.json --output temp/agent_jobs/job_001/replacements.json --json-output
```

This converts item-level `current_text -> proposed_text` edits into minimal `find/replace` replacements for the strict `replace` step.

6. Apply replacements and capture result:

```powershell
python tools/run.py android-translate replace --json temp/agent_jobs/job_001/replacements.json --summary-out temp/agent_jobs/job_001/replace_result.json --job-dir temp/agent_jobs/job_001 --json-output
```

7. Re-run translation validation:

```powershell
python tools/run.py android-translate key-alignment --json-output
python tools/run.py android-translate fix-resource-escapes --json-output
```

## Notes

- `values/` is the English baseline source of truth.
- `values-*` are localized targets.
- For subgroup-styled sample text (for example `ko` faction profiles), rely on `style_profile` in `*.task.json` instead of copying rules into this workflow file.
