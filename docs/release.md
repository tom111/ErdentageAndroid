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

## F-Droid release track

The official F-Droid repository builds the app itself from a specific public
source commit. Erdentage therefore needs a complete, non-interactive FLOSS build,
not merely an APK uploaded by the owner. No separate `fdroid` product flavor is
planned: the Play and F-Droid artifacts must continue to use the same source,
features, and dependency set.

Already prepared:

- `GPL-3.0-or-later`, the permanent application ID, and source-visible copyright
  and asset-provenance records.
- Kotlin/Compose production code with no Firebase, GMS/Play Services,
  advertising, analytics, tracking, or opaque checked-in AAR/JAR dependency.
- Pinned Gradle inputs, wrapper, dependency locks/checksums, and an unsigned
  release APK that builds from the command line.
- German Fastlane text metadata and a version-code-specific changelog.

Before submitting to `fdroiddata`:

1. Publish the complete source repository, issue tracker, maintainer contact,
   and release tags. The tagged source must produce the distributed version.
2. Re-audit every dependency and distributable asset for a compatible FLOSS
   license, and inspect the release APK for undeclared proprietary components.
3. Add the owner-approved icon and at least one screenshot or feature graphic
   below `fastlane/metadata/android/de-DE/images/`; keep description and
   changelog files in the upstream repository.
4. Select the public version name/code and create a stable tag such as `v1.0.0`.
   Test a clean checkout with the exact JDK, Android SDK, and Gradle invocation
   that the F-Droid build recipe will use.
5. Fork the public `fdroid/fdroiddata` GitLab repository and add
   `metadata/de.eigenraum.erdentage.yml`. Pin its build entry to the release tag
   or commit, add source/issue/license/contact fields, and configure update
   detection. Derive the exact YAML from the then-current fdroiddata schema; do
   not copy a generic flavor example into this single-variant project.
6. Run `fdroid build de.eigenraum.erdentage:<versionCode>` in the supported
   F-Droid environment and make the fdroiddata CI build pass. The inclusion
   merge request must also record policy compliance, related request-for-
   packaging issues, and that the upstream author submitted or accepts it.
7. Re-check the official inclusion policy and merge-request template immediately
   before submission. F-Droid has no paid developer-registration step, but its
   technical and policy review remains mandatory.

Signing is an explicit owner decision. The normal F-Droid path builds and signs
the APK with an F-Droid-managed per-app key; that APK cannot update an install
signed with the Play/upstream key. The preferred Erdentage path is therefore a
reproducible build: F-Droid independently rebuilds the same APK and, after a
successful match, can publish the owner-signed APK. Record the chosen key,
certificate fingerprint, signing-tool version, source tag, artifact hash, and
reproducibility result without ever committing the private key or credentials.
Use the then-current `Binaries`/`Builds.binary` and
`AllowedAPKSigningKeys` metadata rules rather than assuming today's schema will
remain unchanged.

Research note, checked 2026-08-20: this checklist incorporates the owner's
[shared F-Droid notes](https://chatgpt.com/share/6a874ad0-6470-83ed-9ae1-b6c899b5cf0a)
and was cross-checked against F-Droid's
[inclusion policy](https://f-droid.org/docs/Inclusion_Policy/),
[current app-inclusion merge-request template](https://gitlab.com/fdroid/fdroiddata/-/blob/master/.gitlab/merge_request_templates/App%20inclusion.md),
[store-metadata guidance](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/),
and [reproducible-build documentation](https://f-droid.org/docs/Reproducible_Builds/).
Treat those primary sources as authoritative if this note becomes stale.

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
