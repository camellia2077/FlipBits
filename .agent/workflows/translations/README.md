# Translation Workflows

Use this directory only for execution steps.

Do not use this directory as the project entry or tool-definition layer.
- Project entry: [apps/audio_android/AGENTS.md](../../../apps/audio_android/AGENTS.md)
- Tool definition: [tools/repo_tooling/android_translate/docs/README.md](../../../tools/repo_tooling/android_translate/docs/README.md)

## Choose One Workflow

1. [android-app-text.md](android-app-text.md)
   - App UI text in `strings_*.xml`
   - Labels, dialogs, validation messages, settings, player copy

2. [android-sample-text.md](android-sample-text.md)
   - Localized prose in `audio_samples_*.xml`
   - Faction/lineup sample text and language-switching samples

3. [check-untranslated-english.md](check-untranslated-english.md)
   - Non-English locale still shows English text
   - `mixed-language` + `mixed-language-context-audit`

4. [android-translation-review-and-fix-from-english.md](android-translation-review-and-fix-from-english.md)
   - EN-baseline review and replacement pipeline
   - `compare` / `export-entries` / `build-replacements` / `replace`

5. [find-unused-android-string-keys.md](find-unused-android-string-keys.md)
   - Suspicious unused `strings*.xml` keys
   - `unused-keys`

6. [fix-android-resource-escapes.md](fix-android-resource-escapes.md)
   - Repair Android XML resource escaping before or after translation edits
   - `fix-resource-escapes`

## Shared Rule

When generated task artifacts exist, use `*.task.json` first and markdown second.
