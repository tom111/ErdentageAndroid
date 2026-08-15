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

The initial project-skeleton check passed on `Erdentage_API_36` on
2026-08-15. That check covered installation, a cold activity launch, Compose
semantics, and visual inspection of the rendered screen. The other AVDs are
installed targets for feature-specific and release-matrix testing; their mere
creation is not recorded as an application test pass.
