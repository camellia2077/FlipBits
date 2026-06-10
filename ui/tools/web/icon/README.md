# Android Icon Tools

This folder keeps browser-only helpers for Android icon work.

## Files

- `android-icon-preview.html`
  Main entry page for checking the Android launcher icon without building the app.
- `android-icon-preview.css`
  Styling for the preview page.
- `android-icon-preview.js`
  Adaptive mask rendering, controls, and preview state.

## Intent

- Keep Android launcher preview concerns separate from the more general icon geometry playground.
- Make it easy to compare circle vs rounded-square styling, background color, and whether the icon feels too empty or too full.
- Treat the preview as a fast visual aid, not as a numerically exact match for real-device launcher scaling.
- Treat the preview as a fast visual aid before updating Android resources such as:
  - `apps/audio_android/app/src/main/res/drawable/ic_launcher_foreground_adaptive.xml`
  - `apps/audio_android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  - `apps/audio_android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
