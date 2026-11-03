# Asset provenance

## Erdentage application icon

- Android resources: `app/src/main/res/drawable/ic_launcher_foreground.xml`,
  `ic_launcher_monochrome.xml`, `ic_splash_mark.xml`, and the
  `mipmap-anydpi*` adaptive-icon files.
- Source master: `IconComposer/Icon-Flat.svg` at iOS reference commit
  `4f3ddc22f0b49d14ecf56d7af382ece0a3ca5f99` in the adjacent Erdentage iOS
  repository.
- Author and copyright holder: Thomas Kahle, 2026.
- License: Apache-2.0, matching this Android application.
- Transformation: the source geometry and colors were translated to Android
  vector paths, scaled into the adaptive-icon safe zone, and supplemented by
  a project-owned monochrome line version. The splash mark reuses those paths
  at a smaller scale appropriate to Android's system splash container; it is
  not a separate or redrawn logo. No Apple framework asset, SF Symbol, font, or
  Icon Composer bundle is distributed.

## Project-owned interface vectors

The small group, minus, widget-preview, and launcher vectors in `app/src/main/res`
were created for this Android application by Thomas Kahle/OpenAI Codex and are
licensed Apache-2.0 with the application.

## Store listing graphics

`fastlane/metadata/android/de-DE/images/icon.png` and `featureGraphic.png`
were generated on 2026-11-03 from the launcher vector above with ImageMagick
and Pillow; the wordmark uses the Inter typeface (SIL Open Font License),
which is not redistributed. Screenshots and their reproduction are described
in `docs/screenshots.md`. Same Apache-2.0 license as the application.
