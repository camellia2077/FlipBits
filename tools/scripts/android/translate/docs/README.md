# Translate Docs

- [sop.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/sop.md)
- [architecture.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/architecture.md)
- [cli_contract.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/cli_contract.md)
- [compare_translation_quality.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/compare_translation_quality.md)
- [apply_translation_replacements.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/apply_translation_replacements.md)
- [check_mixed_language.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/check_mixed_language.md)
- [check_translation_key_alignment.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/check_translation_key_alignment.md)

Human workflow first:

- [sop.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/sop.md)

Agent workflow now lives separately:

- [AGENTS.md](/C:/code/WaveBits/tools/scripts/android/translate/AGENTS.md)

## Directory Map

- `docs/`: human-facing SOP and command docs
- `AGENTS.md`: agent-facing workflow, scoped job flow, and machine-readable artifacts
- `templates/`: JSON templates
- `run.py`: unified CLI entrypoint
- `run.cmd` / `replace.cmd` / `replace_auto_fix_json.cmd`: Windows wrappers
- `commands/`: command implementations
- `core/`: shared path, parsing, reporting, JSON, and Android string helpers
- `prompts/`: review prompt definitions

## Main Outputs

### `temp/ai_translation_reviews`

Review markdown comparing English source text and localized strings.

### `temp/mixed_language_reports`

Reports for suspicious mixed-language content.

### `tools/scripts/android/translate/replacements.json`

Default human workflow input for `replace`.

If you are running agent jobs, prefer the isolated job flow described in:

- [AGENTS.md](/C:/code/WaveBits/tools/scripts/android/translate/AGENTS.md)
