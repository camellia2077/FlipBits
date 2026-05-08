# Android Translate Tool Guide

## Scope

This directory contains the Android XML translation tooling. Keep this file as a thin navigation guide for agents working on the tool code.

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

## Prompt Profiles

The translate tool loads locale profiles from `prompts/locales/{locale}.md`.

After changing a locale prompt profile, generate a scoped job and inspect the produced `_prompts/*.md` and `*.task.json` files:

```powershell
python tools/scripts/android/translate/run.py compare --lang uk --text-type app_text --group strings_audio --prompt-mode agent_json --output-dir temp/agent_jobs/prompt_probe/reviews --job-dir temp/agent_jobs/prompt_probe --no-clean --json-output
```

The generated artifacts are the source of truth for what future agents will actually see.
