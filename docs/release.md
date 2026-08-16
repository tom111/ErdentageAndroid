# Release checklist

Phase-one feature implementation is complete, but the repository does not yet
contain a publishable signed release.

## Completed in source

- Permanent application ID `de.eigenraum.erdentage`, GPL-3.0-or-later license,
  pinned dependencies, dependency locking, and checksum verification.
- The same source and feature set build an Android App Bundle for Google Play
  and an APK suitable for later F-Droid work.
- German Fastlane-compatible title, short description, full description, and
  version-code-1 changelog under `fastlane/metadata/android/de-DE/`.
- German privacy-policy source and Play data-safety answer draft.
- Adaptive, round, and monochrome launcher icons with documented provenance.
- No-Google AOSP, API 26, phone, tablet, Google Play image, and launcher-widget
  emulator checks recorded in `docs/parity.md`.

## Required before the first public build

1. Create and verify a new personal Google Play developer account. Re-check the
   then-current device-verification, closed-testing, and production-access rules.
2. Choose public source hosting and an issue tracker. Publish the privacy policy
   and support contact at stable HTTPS URLs; enter real URLs in Play Console and
   later F-Droid metadata.
3. Review the publisher postal/contact details in the privacy policy.
4. Choose the public `versionName`/`versionCode`. The project deliberately
   remains `0.1.0` / `1` until that release decision.
5. Follow a separately owner-reviewed signing procedure. Create one
   owner-controlled key offline, back it up securely, enroll it with Play App
   Signing as intended, and never commit the keystore or credentials.
6. Capture owner-approved phone and tablet listing screenshots plus any required
   Play feature graphic and 512 px store icon. The automated screenshot harness
   was waived; store images are a manual owner review artifact.
7. Complete content rating, app-access, ads, target-audience, data-safety, and
   privacy declarations in Play Console.
8. Run at least one independent physical-device or managed-device-cloud smoke
   test for OEM widgets, contact/calendar handoff, backup/restore, process death,
   and background rollover. A real Android device is also required for any Play
   account verification that cannot be performed in an emulator.
9. Run Google Play pre-launch reports and the required closed test before
   requesting production access.

## Build gate

From a clean checkout with JDK 17 and Android SDK 36:

```sh
./gradlew test lint assembleDebug
./gradlew connectedDebugAndroidTest
./gradlew assembleRelease bundleRelease
```

Expected unsigned outputs:

- `app/build/outputs/apk/release/app-release-unsigned.apk`
- `app/build/outputs/bundle/release/app-release.aab`

Do not upload an artifact until its version, signing identity, checksums, and
source commit have been recorded in release notes.
