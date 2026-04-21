# Icon Notes

This folder keeps the current icon source set and the reasoning behind the main geometry choices.

## Files

- `icon-master.svg`
  Full design master. This file keeps more of the original visual language and is the least constrained by app-icon delivery rules.
- `icon-foreground.svg`
  Foreground-focused icon source. This is the cleanest version for adaptive or layered icon workflows.
- `icon-app.svg`
  Rounded-square app icon variant for product-style presentation.
- `icon-social.png`
  Large exported preview image for repository, documentation, or sharing surfaces.
- `icon.ico`
  Windows icon export.

## Icon Asset Pipeline

- `icon-master.svg` is the design source of truth.
  Use it to define the geometry language, split behavior, stroke logic, and the overall identity of the mark.
- `icon-foreground.svg` is the foreground delivery source.
  Use it when a platform needs a transparent icon foreground without a baked app tile background.
- `icon-app.svg` is the app-tile presentation source.
  Use it for rounded-square product surfaces, previews, and exports that should already include the background tile.
- Android does not consume the SVG files directly.
  Android uses a platform-specific `VectorDrawable` foreground plus adaptive icon XML, while the SVG files remain the visual reference.
- `icon-social.png` and `icon.ico` are exported delivery artifacts.
  They should be regenerated from the SVG source set rather than edited independently.
- The repo standard is "one visual system, multiple platform outputs" rather than "one file directly consumed everywhere."
  Consistency comes from shared geometry and documented parameters, not from forcing every platform to ingest the same raw file format.

## Current Parameters

- Inner pattern: `C · 8 teeth`
- Inner tip radius: `71.5`
- Inner base radius: `66.84`
- Center dashed core radius: `40`
- Stroke width: `3.5`
- Split axis: vertical center split
- Outer gear base radius: `110`
- App tile background: `#E0E0E0`

## Why These Values

- The `C · 8 teeth` inner gear keeps the center mechanical while separating it from a plain concentric-circle treatment.
  This gives the icon a stronger internal structure than a simple round core.
- `71.5 / 66.84` keeps the inner gear compact but still clearly toothed at app-icon sizes.
  The reduced tooth count and boosted height preserve visual presence after downscaling.
- The center dashed core is intentionally retained as an identity layer.
  It uses inverted colors across the split to continue the icon's core left/right reversal language.
- The center dashed core also reduces similarity to more common mechanical emblems.
  With only nested gears, the mark felt too close to familiar mechanical insignia, so the dashed core gives the center a more distinct signature.
- The center dashed core is rendered fully opaque instead of semi-transparent.
  Because this layer now acts as the visual anchor of the center, reducing its opacity weakened the intended focal pull more than it helped with subtlety.
- `3.5` keeps the brass boundary visible at app-icon sizes.
  Thinner strokes looked elegant at large size but weakened too much when the icon was reduced.
- `3.5` also supports the intended structural character.
  The mark should feel weighty and mechanical rather than light or purely electronic, so the outline is intentionally thick enough to suggest larger, denser, more complex physical construction.
- The icon is too small in actual use to benefit much from making the outer outline thin while making the inner center detail noticeably thicker.
  At this scale, that extra weight hierarchy reads weakly and adds complexity without much visual return.
- Because the dashed core is the visual center, it should at least match the main outline weight instead of reading lighter than the structure around it.
  The chosen value is `3.5`, so the center signal stays strong and legible without introducing a separate stroke system.
- The vertical split stays consistent across the icon family.
  This keeps the mark readable even after other inner details were removed.
- The app tile uses `#E0E0E0` to keep the presentation light without feeling warm or antique.
  The icon already contains strong red and brass accents, so a neutral light tile helps the mark feel cleaner and more product-like.

## Design Direction

- Prefer explicit geometric relationships over eye-balled values.
- Allow small optical adjustments when export surfaces need more visual stability.
- Keep `icon-master.svg` freer, and keep `icon-foreground.svg` / `icon-app.svg` stricter.

## Update Guidance

- Start icon changes from the SVG source set first.
  Adjust `icon-master.svg`, then sync the relevant geometry into `icon-foreground.svg` and `icon-app.svg` before touching downstream exports.
- Treat platform outputs as derived artifacts.
  If Android, Windows, or social assets drift from the SVG source set, update the source set first unless the difference is a platform-specific constraint.
- Allow platform-specific implementation when the delivery format requires it.
  Android `VectorDrawable` is allowed to be a hand-maintained translation of the foreground design as long as its geometry and color behavior stay aligned with the SVG source.
- If the inner gear geometry changes, update both `icon-foreground.svg` and `icon-app.svg` together unless there is a strong delivery-specific reason not to.
- If the center dashed core changes, keep its radius, dash pattern, and inverted-color behavior aligned across the foreground and app variants.
- If the stroke width changes, update the gear outline and inner gear together first, then only split them if small-size exports prove they need different weights.
- If the rounded-square background changes, document the new background hex here so future exports keep the same base color.
