# compare_translation_quality.py

## Purpose

`compare_translation_quality.py` generates `.md` review files with plaintext-style content that compare the English Android string resources with each localized `values-*` resource directory.

It is intended for human or AI review of translation quality. The goal is not strict word-for-word translation. Reviewers should check whether the localized line is natural for the target language and broadly consistent with the English meaning and lineup tone.

The current Android resource layout is split-aware:

- app UI text uses split English baseline files such as `strings_common.xml`, `strings_audio.xml`, `strings_saved.xml`, `strings_settings.xml`, `strings_about.xml`, and `strings_validation.xml`
- sample text continues to live in `audio_samples_*.xml`

## Default paths

- Input: `apps/audio_android/app/src/main/res`
- Output: `temp/ai_translation_reviews`

The script clears the output directory before writing fresh review files, so older flat-layout files do not remain mixed with the newer `app_text` / `sample_text` layout. If you need to keep previous artifacts for an isolated agent job, use `--no-clean`.

The script resolves these paths from the repository root, so it can be run from the repository root with:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py"
```

The unified outer entrypoint is:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py"
```

`compare_translation_quality.py` itself now acts as a library-style module and no longer serves as the direct entrypoint.

If you want machine-readable output for agent callers:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py compare --json-output"
```

If you want a narrow agent job instead of a full refresh:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py compare --lang de --text-type sample_text --group ancient_dynasty --output-dir temp/agent_jobs/job_001/reviews --no-clean --json-output"
```

## Compare filters

`run.py compare` now supports scoped generation:

- `--lang de`: only write localized review files for one language directory
- `--text-type sample_text`: only write one text bucket
- `--group ancient_dynasty`: only write one review group
- `--prompt-mode agent_json`: generate JSON-return prompts for agent/web AI workflows
- `--prompt-mode manual_notes`: generate plain review-note prompts for manual review workflows
- `--job-dir temp/agent_jobs/job_001`: update `temp/agent_jobs/job_001/job_manifest.json` with generated review paths
- `--output-dir temp/agent_jobs/job_001/reviews`: keep agent artifacts away from the shared default folder
- `--no-clean`: keep existing output files in the target directory

When `--lang` is used, English source review files are still generated because they remain the semantic baseline for the localized review.
Generated review files now also include `PROMPT_MODE`, `PROMPT_VERSION`, `GENERATED_AT`, and `PROMPT_REF` header lines so an agent can tell whether a pasted review file is current, which prompt contract it was built for, and which shared prompt document to read.

To reduce token duplication, `compare` now writes shared prompt documents under each language directory, such as `temp/ai_translation_reviews/de/_prompts`. Review files reference those prompt docs instead of inlining the full prompt text into every `.md`.

## What it generates

For English source text, the script writes `.md` review files under `temp/ai_translation_reviews/en`, first grouped by text type:

- `app_text`: normal app-visible Android strings such as `strings_common.xml`, `strings_audio.xml`, and other `strings_*.xml`
- `sample_text`: sample-resource XML files such as `audio_samples_*.xml`

Within each text type, the script writes one review file per review group.

- `sample_text` uses lineup groups such as `ancient_dynasty` and `sacred_machine`
- `app_text` uses split file groups such as `strings_about`, `strings_audio`, `strings_common`, `strings_saved`, `strings_settings`, and `strings_validation`

For each localized Android resource directory, the script also writes review files grouped the same way. For example:

- `temp/ai_translation_reviews/en/sample_text/ancient_dynasty.md`
- `temp/ai_translation_reviews/ja/sample_text/ancient_dynasty.md`
- `temp/ai_translation_reviews/zh-rTW/sample_text/sacred_machine.md`
- `temp/ai_translation_reviews/de/app_text/strings_settings.md`

Each generated file keeps the `.md` extension, but now uses lighter Markdown with only the minimum structure needed for review. The output keeps headings and field labels, while avoiding list-heavy formatting to reduce token cost when pasted into prompts.

English-only source review files look like:

```text
# English Source Review

## Sample Text / Ancient Dynasty
PROMPT_REF: ../_prompts/agent_json.md

FILE: audio_samples_ancient_dynasty_somatic_stripping.xml
KEY: audio_sample_ancient_dynasty_themed_alloy_hand_no_warmth
DIR: values
XML: values/audio_samples_ancient_dynasty_somatic_stripping.xml
NAME: audio_sample_ancient_dynasty_themed_alloy_hand_no_warmth
EN: The immortal alloy hand closes, and no warmth answers inside it
```

Localized translation review files look like:

```text
# Translation Review EN vs [JA]

## Sample Text / Ancient Dynasty
PROMPT_REF: ../_prompts/agent_json.md

FILE: audio_samples_ancient_dynasty_somatic_stripping.xml
KEY: audio_sample_ancient_dynasty_themed_alloy_hand_no_warmth
DIR: values-ja
XML: values-ja/audio_samples_ancient_dynasty_somatic_stripping.xml
NAME: audio_sample_ancient_dynasty_themed_alloy_hand_no_warmth
EN: The immortal alloy hand closes, and no warmth answers inside it
JA: 不朽合金の手が閉じる。その内側に、もう温度は返らない。
```

The target-language label is parsed from the `values-*` folder name. Examples: `values-ja` becomes `[JA]`, and `values-zh-rTW` becomes `[ZH-RTW]`.

## Text type classification

The script classifies files by filename before writing output:

- `sample_text`: filenames starting with `audio_samples_`
- `app_text`: every other app-facing string XML included in the comparison set, which now mainly means split `strings_*.xml` files

This allows normal app strings and themed sample strings to be reviewed separately.

Within `app_text`, the generated review files now follow the split app resource groups directly, such as `strings_about.md` or `strings_settings.md`. Each entry still keeps its original `FILE:` line and also includes exact `XML:` and `NAME:` fields so downstream replacement can target a precise resource entry.

## Sample length guard

Sample review output now includes `SAMPLE_LENGTH: SHORT` or `SAMPLE_LENGTH: LONG` for themed sample entries.

This is intentional. In this repository, each sample lineup file contains both short and long sample prose. Reviewers should preserve that length class:

- `SHORT` should stay short and punchy
- `LONG` should stay long and fully developed

Do not approve a translation that turns a short sample into a long paragraph, or collapses a long sample into a short slogan.

## Review standard

The generated prompt asks reviewers to judge whether the target-language text is natural and suitable for that language. In this repository, localized sample text may intentionally adapt sentence structure, rhythm, grammar, and idiom rather than translate every English word exactly.

The shared prompt docs now also include the stop criteria:

- only change lines with a clear problem
- do not rewrite already natural and safe lines just to make them different
- do not make purely stylistic or novelty edits
- when unsure, keep the current text unchanged

The generated prompt now requires JSON-only output so the review result can be consumed by follow-up tooling. The required shape is:

```json
{
  "dir": "values-ja",
  "items": [
    {
      "name": "audio_sample_example_key",
      "find": "current substring",
      "replace": "improved substring"
    }
  ]
}
```

Use `DIR` and `NAME` exactly as shown in the review block. Do not output `xml`. `find` must be an exact substring of the current localized line, and `replace` should usually target only the smallest necessary span instead of rewriting the whole paragraph. If no changes are needed, reviewers should return the same `dir` with an empty `items` array.

The prompt wording no longer mixes "plain natural-language text only" with "return JSON only". The JSON envelope is mandatory, and the prose inside each `replace` value should stay plain natural language without manual Android or XML escaping.

Acceptable localized text should:

- Keep the broad meaning of the English source.
- Preserve the same lineup tone, such as `Ancient Dynasty`, `Sacred Machine`, or `Labyrinth of Mutability`.
- Sound natural in the target language.
- Prefer language-specific sentence rhythm and grammar over literal English structure.
- Preserve the intended `SHORT` / `LONG` sample length class when the report includes `SAMPLE_LENGTH`.

## Pro sample exception

Pro sample strings use `asciiResId` in `AndroidSampleInputTextProvider.kt`. Their resource keys contain `_ascii_` and now live in the shared English baseline file `values/audio_samples_pro_ascii_shared.xml`, for example:

- `audio_sample_pro_ascii_alloy_hand_no_warmth`
- `audio_sample_pro_ascii_caliper_oil_rite`

These Pro sample texts are fixed ASCII input for the Pro transport mode. They are intentionally English/ASCII in every language, so this script filters them out and does not generate translation comparison entries for them.

## Notes for agents

- Do not treat missing Pro `_ascii_` entries in generated review files as a bug. They are filtered out from both English-only and localized review files because they are shared ASCII protocol samples rather than localized text.
- Do not assume `app_text` means a single `strings.xml`; the current Android baseline is split across multiple `strings_*.xml` files.
- If a new sample lineup is added, update `FACTIONS` in the script so output files are grouped correctly.
- If the Android resource path changes, update `DEFAULT_RES_DIRECTORY`.
- If the generated output location changes, update `DEFAULT_OUTPUT_DIRECTORY`.
- XML scanning, resource parsing, and Markdown writing are intentionally shared with `check_mixed_language.py`; keep parsing and output formatting decoupled from report logic.
