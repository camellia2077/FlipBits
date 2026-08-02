# Find Unused Android String Keys

Use this workflow when you want a report of Android `strings*.xml` keys that appear to have no direct repo usage.

This workflow is report-only. It does not delete keys.

Tool definition for commands and JSON contracts lives in [tools/repo_tooling/android_translate/docs/README.md](../../../tools/repo_tooling/android_translate/docs/README.md).

## When To Use

- suspected stale app text keys in `strings_*.xml`
- suspected stale sample text keys in `audio_samples_*.xml`
- cleanup passes before manually deleting old resource entries

Do not use this workflow as proof that a key is safe to delete without review. Dynamic resource-name construction is not fully resolved by the detector.

## Command

Run the detector:

```powershell
python tools/run.py android-translate unused-keys
```

Optional scoped runs:

```powershell
python tools/run.py android-translate unused-keys --text-type app_text --group strings_audio
python tools/run.py android-translate unused-keys --text-type sample_text --group sacred_machine --key-pattern "_long_"
```

JSON output for agent or script consumption:

```powershell
python tools/run.py android-translate unused-keys --json-output
```

## What It Matches

The report currently treats these as direct usages:

- `R.string.some_key`
- `@string/some_key`
- literal `getIdentifier("some_key", "string", ...)`

It does not prove that a key is unused when references are built dynamically.

## Output

Artifacts are written under:

- `temp/translations/unused_keys/unused_translation_keys.md`
- `temp/translations/unused_keys/unused_translation_keys.json`

Read the JSON first when you want machine-readable inspection. Use the markdown report for manual review.

## Review Rule

Treat every finding as `suspicious unused`, not `safe to delete`.

Before deleting a key, check:

1. dynamic lookups or indirect references
2. product plans for temporarily disabled UI
3. cross-surface usage outside Kotlin/XML conventions the detector can see
