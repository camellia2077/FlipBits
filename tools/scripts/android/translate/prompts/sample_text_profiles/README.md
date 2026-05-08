# Sample Text Style Profiles

Store sample-text style overlays by Android resource-locale folder naming, aligned with `res/values-*`.

Directory pattern:

- `sample_text_profiles/values-<locale>/<group>.md`

Examples:

- `sample_text_profiles/values-ko/labyrinth_of_mutability.md`
- `sample_text_profiles/values-ko/immortal_rot.md`
- `sample_text_profiles/values-ko/exquisite_fall.md`
- `sample_text_profiles/values-ko/ancient_dynasty.md`

Notes:

- Keep profile prose in English for non-target-language instruction layers.
- Keep only necessary target-language anchors (grammar endings, key lexicon) inside the profile text.
- `prompt_ref` emitted to `*.task.json` must remain repository-relative.
