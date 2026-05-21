# Android Player UI

更新时间：2026-05-21

## Purpose

This document defines the Android player-facing UI rules for `apps/audio_android`.
Use it when changing:

- mini player
- bottom tab dock
- player detail sheet
- playback progress / visualization area
- transport controls
- lyrics / raw follow presentation

## Detail Page Structure

The mini player opens a full player surface, not a classic modal form. The detail page is split into three layers:

- `PlayerScaffold.kt`
  - owns the app-level bottom dock overlay: mini player + bottom navigation
  - computes content bottom padding so normal pages remain scrollable behind the dock
  - hosts the expanded player surface through `playerShellOverlay`
- `PlayerSurfaceHost.kt`
  - owns the expanded player surface chrome
  - provides the top collapse/share/save bar
  - handles drag-to-collapse nested scroll
  - hosts the saved-audio queue sheet above the detail page when needed
- `PlayerDetailSheet.kt`
  - owns the detail page content inside the expanded surface
  - keeps the playback display in the scrollable area
  - keeps timeline + transport controls in the fixed bottom dock

Do not put global overlay, top chrome, and playback content responsibilities back into one file. The current split is intentional: the scaffold controls app chrome, the surface host controls expanded-player chrome, and the detail sheet controls player content.

## Detail Page Code Map

Use these entry points when changing the detail page:

- Open/collapse wiring:
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidMainScaffold.kt`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidNavigationActions.kt`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/AudioAndroidViewModel.kt`
- Mini player entry:
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/MiniPlayerBar.kt`
- Expanded player shell:
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/PlayerScaffold.kt`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/PlayerSurfaceHost.kt`
- Detail content:
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/PlayerDetailSheet.kt`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/PlayerDetailLayoutPolicy.kt`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/PlaybackDisplaySectionState.kt`
- Playback display:
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/AudioPlaybackDisplayBlock.kt`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/PlaybackDisplaySection.kt`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/PlaybackDisplayLayoutModel.kt`
- Bottom dock:
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/AudioPlaybackTimelineBlock.kt`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/AudioPlaybackTransportControls.kt`
- Saved playback queue:
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/PlayerQueueSheetHost.kt`
  - `apps/audio_android/app/src/main/java/com/bag/audioandroid/ui/screen/SavedAudioPickerSheet.kt`

## Detail Page Layout Contract

The expanded detail page is a vertical player surface:

- top chrome belongs to `PlayerSurfaceHost`
- display content belongs to the scrollable area in `PlayerDetailSheet`
- timeline and transport controls belong to `PlayerDetailBottomDock`
- queue selection is an overlay owned by `PlayerSurfaceHost`

Layout-sensitive behavior lives in:

- `PlayerDetailLayoutPolicy.kt`
  - recovers unused vertical gap for Visual + lyrics preview
  - applies token preview bonus space for supported token pages
  - owns shared detail constants such as horizontal padding and recovery caps
- `PlaybackDisplaySectionState.kt`
  - owns local detail-page display mode state
  - owns Flash visual mode, Mini Morse visual mode, and lyrics expanded state
  - handles debug automation requests through state callbacks
- `PlaybackDisplayLayoutModel.kt`
  - computes mode-specific display layout constraints for Visual / Mix / Lyrics

Rules:

- Keep display-mode state local to the detail page unless a real cross-screen requirement appears.
- Keep playback progress and transport controls in the bottom dock so they do not scroll away.
- Use `PlaybackVerticalLayout` logs before changing recovery gap or dock height behavior.
- Add new mode-specific display behavior under `PlaybackDisplaySection` / `PlaybackDisplayLayoutModel`, not by branching the whole `PlayerDetailSheet`.
- Do not add fixed bottom spacers to the scroll content to compensate for the bottom dock. The current layout measures the display and dock and applies policy-level recovery where needed.

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
