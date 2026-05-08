# dump_xml_text_md.py

## Purpose

Generate minimal `.md` artifacts from Android `values*/` XML files for plain text inspection.

This command is intentionally **not** a translation review prompt. It is a low-markup dump format to inspect current XML text content with minimal syntax noise.

## Command

```powershell
python tools/scripts/android/translate/run.py dump-xml-md --lang ko --text-type sample_text --group exquisite_fall
```

Include EN side-by-side:

```powershell
python tools/scripts/android/translate/run.py dump-xml-md --lang ko --text-type sample_text --group exquisite_fall --with-en
```

## Output layout

Default output directory:

- `temp/xml_text_checks`

Per file output example:

- `temp/xml_text_checks/values-ko/sample_text/audio_samples_exquisite_fall_dissolution_of_ego.xml.md`

## File format (minimal md)

Each output file uses plain key-value lines:

```text
DIR: values-ko
XML: values-ko/audio_samples_exquisite_fall_dissolution_of_ego.xml

NAME: audio_sample_example_key
SAMPLE_LENGTH: SHORT
EN: ...
TEXT: ...
```

No headings, tables, or bullet-heavy markdown are used.

## Filters

- `--lang`: restrict to one localized folder (for example `ko`, `ja`, `zh-rTW`)
- `--text-type`: `app_text` or `sample_text`
- `--group`: faction or app group (for example `exquisite_fall`, `strings_audio`)
- `--output-dir`: custom output directory
- `--no-clean`: keep existing files in output directory
- `--with-en`: include `EN:` line from `values/` baseline for each key

## Notes

- Baseline `values/` files are included by default for side-by-side inspection behavior.
- The parser reads `<string name="...">...</string>` content and emits normalized plain text.
