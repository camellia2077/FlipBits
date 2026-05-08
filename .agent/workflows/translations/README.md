# Translation Workflow Index

Use this directory only when a task changes Android localized XML text or needs to repair translation alignment.

## Choose The Workflow

- Use [android-app-text.md](android-app-text.md) for app UI text:
  - `apps/audio_android/app/src/main/res/values/strings_*.xml`
  - labels, buttons, dialogs, validation messages, settings, player UI text
- Use [android-sample-text.md](android-sample-text.md) for audio sample prose:
  - `apps/audio_android/app/src/main/res/values/audio_samples_*.xml`
  - localized sample input text that changes with app language

If the task only changes Kotlin, JNI, playback, or layout code and does not touch XML text, do not expand these workflows.

## What This Workflow Does

This workflow chooses the right translation task type and tool command. It does not duplicate the full translation prompt.

The translate tool already generates the detailed prompt contract:

- review markdown contains `PROMPT_REF`
- shared prompt docs live under generated `_prompts/` folders
- `*.task.json` includes `locale_profile`, `text_type`, `context`, and `sample_length`
- key-alignment also writes per-locale `*_key_alignment.task.json` files with missing keys, English source text, context, nearby localized terms, and suggested insertion points

When generated task artifacts exist, read those artifacts first and follow their prompt/profile fields.

## Shared Commands

Find missing or extra localized keys:

```powershell
python tools/scripts/android/translate/run.py key-alignment
```

Generate scoped translation review/task artifacts:

```powershell
python tools/scripts/android/translate/run.py compare --text-type app_text
python tools/scripts/android/translate/run.py compare --text-type sample_text
```

For generated agent jobs, prefer `*.task.json` over parsing markdown.
