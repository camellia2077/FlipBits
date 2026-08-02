# Fix Android XML Resource Escapes

Use this workflow when Android string resources fail to compile because localized XML contains unsafe Android escape syntax.

Typical triggers:

- raw ASCII apostrophe `'` inside localized text that later causes `aapt` / `mergeDebugResources` failures
- legacy `\'` sequences that are meant to protect apostrophes but still break Android resource compilation in current files
- suspicious Android backslash escapes inside `<string>` values that should be normalized through the shared string encoder/decoder
- replacement batches that touched localized XML and now need a resource-safe normalization pass

## Command

Run the built-in repair command:

```bash
python tools/run.py android-translate fix-resource-escapes --res-dir apps/audio_android/app/src/main/res
```

Scope to explicit files when needed:

```bash
python tools/run.py android-translate fix-resource-escapes \
  apps/audio_android/app/src/main/res/values-it/strings_settings.xml \
  apps/audio_android/app/src/main/res/values-fr/strings_settings.xml
```

Quiet mode:

```bash
python tools/run.py android-translate fix-resource-escapes --quiet
```

## What It Repairs

The command normalizes `<string>` node text with the same escaping rules used by the Android translation tooling.

Current repair behavior includes:

- converting risky apostrophe-containing strings into Android-safe quoted string literals
- normalizing legacy `\'` apostrophe escapes into the same Android-safe quoted form
- normalizing other supported Android string escapes through the shared string encoder/decoder

Practical examples:

- `d'accento` -> `"d'accento"`
- `d\'accento` -> `"d'accento"`
- `Необов'язково` -> `"Необов'язково"`

Non-goals:

- guessing the intended text behind malformed manual `\u` escapes
- silently rewriting ambiguous broken resource text when the original literal meaning is unclear

## When To Run It

- after applying translation replacements
- after manual XML edits in `values-*/*.xml`
- when Android build fails in `mergeDebugResources` or `aapt` with invalid unicode / invalid string resource errors
- when translation lint or code review finds suspicious apostrophe escaping in localized resources

## Recommended Follow-Up

After repair, rerun the usual checks:

```bash
python tools/run.py android-translate key-alignment --res-dir apps/audio_android/app/src/main/res
python tools/run.py android-translate lint --res-dir apps/audio_android/app/src/main/res
cd apps/audio_android
.\gradlew.bat app:assembleDebug
```
