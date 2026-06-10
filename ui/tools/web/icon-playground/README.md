# Icon Playground

This folder keeps the local browser playground used to tune the icon geometry and preview decisions before they are copied into the formal assets under `ui/`.

## Scope

- Explore inner gear variants, stroke weight, split-color behavior, and the center dashed core.
- Compare icon behavior against different rounded-square background presets.
- Surface the current geometry parameters so the chosen values can be copied into:
  - `ui/icon-foreground.svg`
  - `ui/icon-app.svg`
  - `ui/README.md`

## Boundaries

- This playground is a design helper, not a production UI.
- It does not automatically overwrite the formal SVG assets.
- Any accepted design change should be manually synchronized into the formal icon files.

## Files

- `gear-thickness-playground.html`
  Main entry page.
- `gear-thickness-playground.css`
  Layout and control styling.
- `gear-thickness-playground-geometry.js`
  Geometry generation and path-shape logic.
- `gear-thickness-playground-model.js`
  UI state and preset data.
- `gear-thickness-playground-render.js`
  SVG and metrics rendering.
- `gear-thickness-playground.js`
  Event wiring and page bootstrap.
