# apply_translation_replacements.py

## Purpose

`apply_translation_replacements.py` reads a JSON file of replacement rules and updates localized Android string XML files under the Android `res` root.

It is intended for controlled bulk text replacement when you already know:

- which localized XML entry should change
- which exact substring in that entry should be replaced
- what the replacement text should be

The script is strict by design. Before writing any replacement, it resolves a unique localized XML target and a unique `<string name="...">` inside that file, then checks that `find` appears exactly once in the current string text.

This avoids silently overwriting the wrong string when a short replacement fragment would otherwise be ambiguous.

It also avoids continuing on malformed XML. The script now performs a fail-fast structure check before indexing or replacing any candidate file.

## Default behavior

- Input Android res root: `apps/audio_android/app/src/main/res`
- JSON file: defaults to `tools/scripts/android/translate/replacements.json`
- Target XML lookup: by `xml` selector plus `name`

So if you run the script without `--json`, it will try to read:

- `tools/scripts/android/translate/replacements.json`

## JSON format

The JSON file must be a list of objects.

Each object must contain:

- `dir`
- `items`

Each item must contain:

- `name`
- `find`
- `replace`

Example:

```json
{
  "dir": "values-zh-rTW",
  "items": [
    {
      "name": "audio_sample_ancient_dynasty_themed_alloy_hand_no_warmth",
      "find": "分子鍵之名",
      "replace": "分子鍵名"
    }
  ]
}
```

`dir` should usually come directly from the `DIR:` line in the review markdown.
`name` should usually come directly from the `NAME:` line in the review markdown.
`find` must be an exact substring of the current localized string text.
The review SOP is:

1. `compare` shows English plus the current target-language text, together with exact `DIR`, `XML`, and `NAME`
2. the reviewer outputs JSON that points to that directory and string name, and replaces only the necessary substring
3. `replace` resolves the string name within that directory, verifies `find` is unique in the current string, and writes the updated text back into the matching localized XML

Do not use placeholders such as:

- `[MISSING TRANSLATION / 此条目未翻译]`

If placeholder text appears in `find`, the script now fails immediately.

Before JSON parsing, the tool also runs a preflight syntax check.
By default this only reports the error and stops. If you opt in with `--auto-fix-json`,
the tool will apply only high-confidence repairs before replace.

Current high-confidence repair scope:

- escape obviously unbalanced double quotes inside JSON string values

The tool does not try to guess broad structural damage such as missing braces,
broken arrays, or heavily misaligned quotes.

## Usage

Run from the repository root:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/apply_translation_replacements.py"
```

That uses the default JSON path:

```text
tools/scripts/android/translate/replacements.json
```

Or pass an explicit JSON file:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/apply_translation_replacements.py --json apps/audio_android/app/src/main/res/values-zh-rTW/replacements.json"
```

Optional custom res root:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/apply_translation_replacements.py --res-dir apps/audio_android/app/src/main/res --json apps/audio_android/app/src/main/res/values-zh-rTW/replacements.json"
```

If preflight finds a high-confidence JSON syntax issue and you want the tool to repair it first:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py replace --auto-fix-json"
```

If you want machine-readable output for agent callers:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py replace --json-output"
```

If you also want to persist the structured result as a job artifact:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py replace --json temp/agent_jobs/job_001/replacements.json --summary-out temp/agent_jobs/job_001/replace_result.json --job-dir temp/agent_jobs/job_001 --json-output"
```

## Validation rules

For each JSON entry, the script checks:

1. `dir` resolves to exactly one `values` / `values-*` directory under `res`
2. `name` resolves to exactly one `<string name="...">` across the translation XML files in that directory
3. `find` appears exactly once in that string's current text
4. only that one substring occurrence is replaced with `replace`

If any of these checks fail, that entry is reported as an error and is not applied.

This means the script assumes the review markdown has already given the tool an exact language directory and string name, and the JSON only needs to describe the local text delta.

The indexed text resource set is split-aware and currently includes:

- `audio_samples_*.xml`
- legacy `strings.xml` when still present
- split app UI files such as `strings_common.xml`, `strings_audio.xml`, `strings_saved.xml`, `strings_settings.xml`, `strings_about.xml`, and `strings_validation.xml`

Before replacement, candidate XML files are also checked for structure problems:

1. root tag must be `<resources>`
2. there must be no unexpected text directly under `<resources>`
3. there must be no unexpected trailing text after `<string>` elements

If a file fails these checks, the script stops using that file and reports the exact reason.

The same structure check is run twice:

1. before replacement
2. after generating the would-be updated XML text

Only when both checks pass will the file actually be written.

## Output behavior

The script prints:

- total applied replacement count
- character-level colored diffs for each applied replacement
- validation errors, if any

Exit codes:

- `0`: all replacements applied successfully
- `1`: invalid path or setup error
- `2`: one or more validation errors occurred

`--json-output` returns the same exit codes, but prints a single JSON object instead of human-readable text.

`--summary-out` writes that same structured result JSON to a file, which is useful for agent job folders and audit trails.
If `--job-dir` is also provided, `replace` updates `<job-dir>/job_manifest.json` with the input JSON path and result JSON path.

## Notes

- `find` must match the current XML text exactly, including punctuation differences.
- This script intentionally scans only text resources that participate in translation workflows: `audio_samples_*.xml`, legacy `strings.xml`, and split `strings_*.xml`.
- It does not scan unrelated `values/` XML such as `themes.xml` or `ic_launcher_colors.xml`, so those files do not produce noise or get touched by the replacement flow.
- The script rewrites only the matched `<string>` inner text and does not rewrite the full XML formatting.
- If an `xml` selector matches multiple files under `res`, the script will report ambiguity instead of guessing.
- If `find` appears multiple times in the target string, the script will report ambiguity instead of guessing which span to replace.
- For speed, the script scans localized `values-*` text resources once, builds an in-memory selector index, and reuses cached file contents during replacement.
- After applying replacements, the script prints a terminal diff with red removed characters and green added characters.
- The fail-fast XML check was added because earlier malformed files could otherwise keep being modified and hide the real source of the corruption.
- The replacement is only committed when both the original XML and the post-replacement XML pass the same structure validation.
- Before writing an updated file, the script also runs a local Android-string risk pass and fails fast on patterns such as raw ASCII apostrophes, trailing backslashes, unsupported escape sequences, or malformed `\u` escapes.
- After at least one replacement is written, the default flow also runs an Android resource smoke check with `:app:mergeDebugResources` so `aapt`-level string issues surface immediately.
- Placeholder report text such as `[MISSING TRANSLATION / 此条目未翻译]` is explicitly rejected because it is not a valid current localized source string.

## Post-replacement smoke check

By default, `run.py replace` now runs a lightweight Android resource compile step after replacements are written:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py replace"
```

The smoke check uses:

- `apps/audio_android/gradlew(.bat) :app:mergeDebugResources`

This is intended to catch Android resource parsing/escaping failures early, before a full `assemble-debug`.

If you explicitly need to skip it:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py replace --skip-smoke-check"
```

Additional exit codes:

- `3`: replacements were applied, but the Android resource smoke check failed
- `4`: validation errors occurred and the Android resource smoke check also failed
