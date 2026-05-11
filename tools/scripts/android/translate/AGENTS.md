# Android Translate Tool Guide

The Android translation tool has moved out of `tools/scripts` into the formal tools package.

- Preferred entry point: `python tools/run.py android-translate <subcommand>`
- Compatibility entry point: `python tools/scripts/android/translate/run.py <subcommand>`
- Tool code: `tools/repo_tooling/android_translate/`

Use command discovery first:

```powershell
python tools/run.py android-translate --help
python tools/run.py android-translate list-text-types --help
```

