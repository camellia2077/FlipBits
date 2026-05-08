# Android Translate Tool Guide

## Scope

This directory contains the Android XML translation tooling. Keep this file as a thin navigation guide for agents working on the tool code.

## Command Discovery First

- Agent first step must be command discovery, not document browsing:
  - run `python tools/scripts/android/translate/run.py --help`
  - then run `python tools/scripts/android/translate/run.py <subcommand> --help`
- Treat docs as secondary references only after command-level discovery.

## Primary Command Surface

- Entry point: `python tools/scripts/android/translate/run.py`
- Discoverability commands:
  - `list-text-types`
  - `list-groups --text-type <app_text|sample_text>`
  - `list-langs`
- Translation review generation:
  - `compare`
  - `all`
- XML inspection and repair:
  - `dump-xml-md` (supports `--with-en`)
  - `fix-resource-escapes`
  - `lint`
  - `autofix`
- Translation safety and alignment:
  - `mixed-language`
  - `key-alignment`
    - optional scope: `--lang <locale>`
    - optional strict stale gate: `--fail-on-stale`
- Bulk replacement workflow:
  - `build-replacements`
  - `replace`

## Lint + Autofix Quick Usage

- Run deterministic lint checks:
  - `python tools/scripts/android/translate/run.py lint --lang fr --json-output`
- Apply deterministic low-risk autofixes:
  - `python tools/scripts/android/translate/run.py autofix --lang fr --json-output`
- Note: `autofix` now includes a final Android escape-normalization pass (same core behavior as `fix-resource-escapes`) to prevent AAPT2 string escape failures.
- Typical loop:
  - `autofix` -> `lint` -> `compare`

## Where To Look

- Agent workflows live under [.agent/workflows/translations](/C:/code/WaveBits/.agent/workflows/translations).
- Human web-chat workflow lives in [docs/sop.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/sop.md).
- CLI behavior and JSON contracts live in [docs/cli_contract.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/cli_contract.md).
- Generated prompt/profile text lives under [prompts](/C:/code/WaveBits/tools/scripts/android/translate/prompts).
- Locale-specific rules live under [prompts/locales](/C:/code/WaveBits/tools/scripts/android/translate/prompts/locales).

## Tool Boundaries

- Do not duplicate translation workflow steps in this file.
- Do not store locale prompt policy in this file.
- Keep workflow process in `.agent/workflows/translations`.
- Keep generated prompt content in `prompts/` so the tool output and agent instructions stay aligned.
- Treat `values/` as the English baseline source of truth.
- Treat `values-*` as localized targets; do not assume they are baseline files.
- Prefer relative repository paths in generated docs/workflows/prompts; avoid absolute paths.

## Prompt Profiles

The translate tool loads locale profiles from:

- shared locale constraints: `prompts/locales/_shared.md`
- locale-specific constraints: `prompts/locales/{locale}.md`

Current shared locked English terms (must remain untranslated when present in EN source):

- `flash`, `pro`, `mini`, `ultra`, `ASCII`, `UTF-8`, `Hex`, `Binary`, `Morse`, `Emoji`

For `sample_text`, some locales also load faction-specific style profiles from:

- `prompts/sample_text_profiles/values-{locale}/_global.md`
- `prompts/sample_text_profiles/values-{locale}/{group}.md`

Current faction-style locales include `ko`, `zh-rTW`, and `uk`.

After changing a locale prompt profile, generate a scoped job and inspect the produced `_prompts/*.md` and `*.task.json` files:

```powershell
python tools/scripts/android/translate/run.py compare --lang uk --text-type app_text --group strings_audio --prompt-mode agent_json --output-dir temp/agent_jobs/prompt_probe/reviews --job-dir temp/agent_jobs/prompt_probe --no-clean --json-output
```

The generated artifacts are the source of truth for what future agents will actually see.

For `sample_text` prompt/style debugging, run:

```powershell
python tools/scripts/android/translate/run.py compare --lang uk --text-type sample_text --output-dir temp/agent_jobs/style_probe/reviews --job-dir temp/agent_jobs/style_probe --no-clean --json-output
```

For XML text inspection (minimal md) with EN baseline side-by-side:

```powershell
python tools/scripts/android/translate/run.py dump-xml-md --lang uk --text-type sample_text --with-en --output-dir temp/agent_jobs/xml_dump_uk --no-clean
```
