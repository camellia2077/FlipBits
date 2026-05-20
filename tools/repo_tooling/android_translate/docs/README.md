# Android Translate Docs

This directory is the single definition entrypoint for the Android XML translation tool.

Use this layer only for tool definition:
- command surface
- JSON contract
- output directories
- command-specific behavior

Do not use this directory as the project workflow entry.
- Project entry: [apps/audio_android/AGENTS.md](/C:/code/WaveBits/apps/audio_android/AGENTS.md)
- Execution steps: [.agent/workflows/translations/README.md](/C:/code/WaveBits/.agent/workflows/translations/README.md)

## Start Here

- Tool overview and command map: [sop.md](/C:/code/WaveBits/tools/repo_tooling/android_translate/docs/sop.md)
- Stable CLI and task JSON contract: [cli_contract.md](/C:/code/WaveBits/tools/repo_tooling/android_translate/docs/cli_contract.md)
- Tool architecture: [architecture.md](/C:/code/WaveBits/tools/repo_tooling/android_translate/docs/architecture.md)

## Main Commands

- `key-alignment`
  - Missing or extra localized keys.
  - See [check_translation_key_alignment.md](/C:/code/WaveBits/tools/repo_tooling/android_translate/docs/check_translation_key_alignment.md)
- `compare`
  - EN vs localized review artifacts and task JSON.
  - See [compare_translation_quality.md](/C:/code/WaveBits/tools/repo_tooling/android_translate/docs/compare_translation_quality.md)
- `export-entries` / `build-export-replacements` / `replace`
  - Narrow JSON-first edit/apply path.
  - See [apply_translation_replacements.md](/C:/code/WaveBits/tools/repo_tooling/android_translate/docs/apply_translation_replacements.md)
- `mixed-language` / `mixed-language-context-audit`
  - Suspicious untranslated English discovery and triage.
  - See [check_mixed_language.md](/C:/code/WaveBits/tools/repo_tooling/android_translate/docs/check_mixed_language.md)
- `untranslated-equals-english`
  - Exact `localized == EN` discovery for Latin-script locales.
  - Use this when the main risk is full English leftovers rather than cross-script leakage.

## JSON-First Contract

Generated task artifacts are JSON-first.

- Read `*.task.json` before markdown.
- Use markdown only when the JSON payload lacks enough inspection detail.
- Prefer `execution_contract`, `locale_profile`, `style_profile`, and per-entry execution fields before re-parsing raw XML.

## Output Directories

All generated tool outputs live under `temp/translations/`.

- Reviews: `temp/translations/reviews`
- Key alignment: `temp/translations/key_alignment`
- Mixed language: `temp/translations/mixed_language`
- Mixed language audit: `temp/translations/mixed_language_audit`
- Entry exports: `temp/translations/entry_exports`
- XML dump: `temp/translations/xml_dump`

## Directory Map

- `run.py`
  - Unified CLI entrypoint
- `commands/`
  - Command implementations
- `core/`
  - Shared parsing, JSON, reporting, replacement, and policy helpers
- `prompts/`
  - Shared prompt/profile definitions
- `docs/`
  - Tool definition and command docs
