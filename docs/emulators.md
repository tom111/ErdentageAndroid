# Android emulator matrix

The development machine is Apple Silicon, so every AVD uses an ARM64 system
image. AVD definitions live in the normal per-user Android SDK configuration
and are not committed to this repository.

| AVD name | Purpose | Device | System image |
| --- | --- | --- | --- |
| `Erdentage_API_36` | Primary resizable development device | Resizable | Android 16 / API 36, Google APIs, ARM64 |
| `Erdentage_API_36_AOSP` | No-Google and F-Droid validation | Medium Phone | Android 16 / API 36, AOSP, ARM64 |
| `Erdentage_API_26` | Minimum-supported compatibility | Pixel 2 | Android 8.0 / API 26, AOSP, ARM64 |
| `Erdentage_API_36_Tablet` | Large-screen validation | Pixel Tablet | Android 16 / API 36, Google APIs, ARM64 |
| `Erdentage_API_36_Play` | Store-oriented smoke test | Pixel 9 | Android 16 / API 36, Google Play, ARM64 |

Start the primary emulator from Android Studio's Device Manager or from the
command line:

```sh
$ANDROID_HOME/emulator/emulator -avd Erdentage_API_36
```

After it finishes booting, run the instrumentation tests:

```sh
./gradlew connectedDebugAndroidTest
```

## Verification record

- `Erdentage_API_36`: all nine instrumentation tests passed on 2026-08-16.
  Visible checks covered first run, person/settings screens, overview,
  force-stop restoration, 1.3x font scale, landscape, and a real launcher-hosted
  widget at compact and wide resized dimensions.
- `Erdentage_API_26`: all nine instrumentation tests passed and the first-run UI
  cold-launched on 2026-08-16.
- `Erdentage_API_36_AOSP`: the APK installed and cold-launched in German with
  `com.google.android.gms` absent on 2026-08-16.
- `Erdentage_API_36_Tablet`: first run, settings, and the wide custom overview
  were visibly checked on 2026-08-16.
- `Erdentage_API_36_Play`: the APK installed and cold-launched with the Play
  Store package present on 2026-08-16.

These are emulator results. OEM widget behavior, real contact/calendar apps,
backup/restore, and background execution still require the independent
physical-device or managed-device-cloud gate in `docs/release.md`.
