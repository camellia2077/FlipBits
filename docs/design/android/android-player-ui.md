# Android Player UI

## Purpose

This document defines the Android player-facing UI rules for `apps/audio_android`.
Use it when changing:

- mini player
- bottom tab dock
- player detail sheet
- playback progress / visualization area
- transport controls
- lyrics / raw follow presentation

## Dock System

The bottom playback area is a single dock system made of:

- mini player
- bottom navigation bar

They should read as one shared playback base layer.

Rules:

- Both must use the same dock container color source.
- Layer separation should come from shadow, spacing, and controls, not from unrelated base colors.
- Mini player should stay fully opaque so text remains readable over scrolling content.

Code truth:

- `playerDockContainerColor(uiState)` in `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidThemeMappings.kt`

## Player Control Colors

Player controls should not hand-pick theme colors per component.
Playback UI uses shared helpers so related controls stay aligned across `dual-tone` and `Material` themes.

Theme-system rule:

- `Material`
  - player controls may follow the shared single-color `ColorScheme` mapping.
- `dual-tone`
  - player controls must start from `BrandThemeCatalog.kt`'s `backgroundColor` / `accentColor` / `outlineColor`.
  - shared helpers may still read from `MaterialTheme.colorScheme`, but only after those slots have been intentionally mapped from dual-tone visual roles.
  - do not treat `primaryContainer` / `surfaceVariant` as the player's final dual-tone semantics by default.

Current shared entries:

- `playerDockContainerColor(uiState)`
- `playerSegmentedButtonColors()`
- `playerChromeColors()`

These should cover:

- dock containers
- player segmented buttons
- transport controls
- current-line raw annotation chip

For current dual-tone mapping:

- dock container
  - stays on the `backgroundColor` side, with a restrained `accentColor` mix when separation is needed
- player segmented selected state
  - uses the `accentColor` lane
- player segmented idle container
  - should stay on the `backgroundColor` side instead of drifting into unrelated Material container semantics
- support-detail rails or inactive tone guides
  - may use an `outlineColor`-backed support tone when the component is acting as a visualization aid rather than a selected-state chrome

## Segmented Buttons

The following controls are in one visual family and should use the same segmented-button color logic:

- `Visual / Lyrics`
- `FSK lanes / Tone energy`
- `Hex / Binary`

Rule:

- Reuse `playerSegmentedButtonColors()`.
- Do not hard-code ad hoc `SegmentedButtonDefaults.colors(...)` inside one player component unless the design explicitly forks that control family.

## Transport Controls

Playback transport controls are part of the player chrome, not generic page actions.

They should share one color language for:

- `accentColor` action
- neutral action
- disabled action

Rule:

- Reuse `playerChromeColors()`.

## Lyrics And Raw Annotation

Lyrics mode is player UI, not a generic result card.

Rules:

- Current-line annotation chip should use player chrome tokens, not its own local theme recipe.
- Text token highlighting may still use the active playback `accentColor` lane, but should remain visually compatible with the rest of player chrome.
- Raw browsing that belongs to decode result cards can keep its own UI identity; player follow UI should stay in the player family.

## Layout Guidance

The player detail sheet behaves like a player surface, not a normal content form.

Rules:

- The playback viewport and the dock should remain visually stable while content scrolls.
- Any scrollable page shown above the dock must respect provided `contentPadding`.
- Avoid reintroducing fixed bottom spacers when shared scaffold padding already exists.

## When To Update This Doc

Update this document when changing:

- dock layering rules
- player color ownership
- segmented-button family rules
- lyrics/raw presentation rules
- transport-control visual hierarchy
