# check_mixed_language.py

## Purpose

`check_mixed_language.py` scans Android localized string resources and reports strings that appear to contain untranslated or mixed-language fragments.

It is intended as a fast lint-style helper for localization review. It writes separate `.md` reports for app UI text and sample text, grouped by language. Each report lists suspicious keys, the localized text, and the English source text.

The current Android resource layout is split-aware:

- app UI text now uses split English baseline files such as `strings_common.xml`, `strings_audio.xml`, `strings_saved.xml`, `strings_settings.xml`, `strings_about.xml`, and `strings_validation.xml`
- sample text still uses `audio_samples_*.xml`

## Default paths

- Input: `apps/audio_android/app/src/main/res`
- Output: `temp/mixed_language_reports/<lang>/app_text/mixed_language_report.md` and `temp/mixed_language_reports/<lang>/sample_text/mixed_language_report.md`

The script resolves these paths from the repository root, so it can be run from the repository root with:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py"
```

The unified outer entrypoint is:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py"
```

`check_mixed_language.py` itself now acts as a library-style module and no longer serves as the direct entrypoint.


## Output split

Reports are split by language first, then by responsibility:

- `temp/mixed_language_reports/<lang>/app_text/mixed_language_report.md` for app UI strings, such as `strings_common.xml`, `strings_audio.xml`, and other `strings_*.xml` files.
- `temp/mixed_language_reports/<lang>/sample_text/mixed_language_report.md` for sample text XML files whose names start with `audio_samples_`.

This makes it easier to send only app UI text or only sample text to a reviewer or another agent.

For sample reports, entries also include `SAMPLE_LENGTH: SHORT` or `SAMPLE_LENGTH: LONG` so reviewers can keep the original short/long intent in mind while fixing suspicious mixed-language fragments.

## Detection strategy

The script compares the English `values/` resource set with every localized `values-*` directory.

For app UI text, that means it compares split English baseline `strings_*.xml` files against matching localized `strings_*.xml` files. For sample text, it compares `audio_samples_*.xml` files the same way.

This checker is intentionally conservative. It now focuses only on high-confidence cross-script mistakes:

- For CJK target languages (`zh`, `zh-rTW`, `ja`, `ko`), it looks for suspicious Latin/English chunks in the localized text. For example, a Japanese or Korean string that accidentally contains `stolen floor` will be reported unless the words are explicitly whitelisted.
- For non-CJK target languages (`en`, `de`, `es`, `fr`, `it`, `pl`, `pt-rBR`, `ru`, `uk`, `la`, etc.), it only looks for suspicious Chinese/Japanese/Korean character chunks in the localized text.

It no longer tries to detect English n-gram leakage across Latin-script languages. That broader strategy created too many false positives for this repository, especially with technical terms, shared Latin roots, and the stylized `la` variant used in this project.

Each language report keeps the `.md` extension, but now uses lighter Markdown with only headings and field labels so the report is cheaper to paste into prompts:

```text
# Mixed Language Report (Split Strings) [APP_TEXT][DE]
 
## DE | Latin N-gram Check
TOTAL_ISSUES: 12

FILE: strings_audio.xml
KEY: audio_input_editor_inline_hint
ISSUE: 可疑漏翻/混杂: TXT editor
TR: Längere Passagen bleiben hier eingeklappt. Öffne den TXT-Editor...
EN: Longer passages stay collapsed here. Open the TXT editor...
```

## Pro ASCII exception

Pro mode is a special case in this repository.

Pro sample text is not normal localized prose. It is fixed protocol/encoding input and must remain ASCII in every language. These sample keys contain `_ascii_` and now come from the shared English baseline file `values/audio_samples_pro_ascii_shared.xml`, for example:

- `audio_sample_pro_ascii_alloy_hand_no_warmth`
- `audio_sample_pro_ascii_thread_pulls_the_hero`

For those keys, the script does not check language mixing. It only verifies that the localized resource contains ASCII-range characters.

Pro UI strings can also legitimately include protocol terms such as `ASCII`, `byte`, `Token`, `Nibble`, or `0x`. Keys like these are treated as Pro ASCII context and skipped for mixed-language detection:

- `audio_pro_visual_token_mapping`
- `audio_pro_visual_ascii_byte`
- `audio_pro_visual_byte_binary`
- `validation_pro_ascii_only`

This prevents false positives where expected protocol words are reported as mixed language.

## values-la exception

`values-la` is not treated as a standard target-language translation set for mixed-language purposes.

In this project, `la` is a deliberate stylized Latinized English variant, not classical Latin and not a normal "translate English away" locale. Because of that:

- the checker still scans `values-la`
- Pro ASCII sample rules still apply normally
- but ordinary English residual detection is skipped for `values-la`

This keeps the checker aligned with the product design instead of flagging the entire `la` resource set as suspicious.

## Whitelist

`WHITELIST` contains short technical terms that are allowed across languages, such as:

- `ascii`
- `ui`
- `id`
- `hex`
- `ok`

Add to the whitelist only when a term is intentionally shared across localized UI and should not be translated.

## Notes for agents

- This script is heuristic. A report item is suspicious, not automatically wrong.
- The checker is intentionally narrow: CJK locales are checked for Latin leakage, and non-CJK locales are checked only for CJK leakage.
- Do not flag Pro sample `_ascii_` strings as untranslated English. They must remain ASCII for all languages.
- If new Pro protocol UI keys are added and false-positive, update `is_pro_ascii_context_key()`.
- If new non-translated technical terms are expected across languages, update `WHITELIST`.
- For app UI text, expect report `FILE:` blocks to point at split `strings_*.xml` files rather than a single monolithic `strings.xml`.
- Output reports are generated under `temp/mixed_language_reports/<lang>/app_text/` and `temp/mixed_language_reports/<lang>/sample_text/` and should usually not be committed.
- XML scanning, resource parsing, and Markdown writing are intentionally shared with `compare_translation_quality.py`; keep parsing and output formatting decoupled from report logic.
