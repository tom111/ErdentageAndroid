# Release checklist

Phase-one feature implementation is complete. The permanent app-signing
identity exists, but the repository does not yet contain a configured,
owner-approved public release.

## Completed in source

- Permanent application ID `com.firsching.erdentage` (changed from
  `de.eigenraum.erdentage` on 2026-11-03, unpublished), Apache-2.0 license,
  pinned dependencies, dependency locking, and checksum verification.
- The same source and feature set build an Android App Bundle for Google Play
  and an APK suitable for later F-Droid work.
- German Fastlane-compatible title, short description, full description, and
  version-code-1 changelog under `fastlane/metadata/android/de-DE/`.
- German privacy-policy source and Play data-safety answer draft.
- Adaptive, round, and monochrome launcher icons with documented provenance.
- No-Google AOSP, API 26, phone, tablet, Google Play image, and launcher-widget
  emulator checks recorded in `docs/parity.md`.
- One owner-controlled cross-store signing identity, whose public fingerprint
  and handling rules are recorded in `docs/signing.md`.

## Required before the first public artifact

1. Make the source repository public. It exists since 2026-11-02 as the
   private `tom111/ErdentageAndroid`. Enable its issue tracker, and publish
   maintainer/support contact and the privacy policy at stable HTTPS URLs.
   Intended home: https://thomas-kahle.de/erdentage/ (the iOS product page);
   the Android policy from `docs/privacy-policy-de.md` needs its own page
   there, for example `/erdentage/datenschutz-android/`, because Google Play
   requires a privacy-policy URL on the listing and the iOS page describes
   App-Group and Watch specifics that do not apply.
2. Review the publisher postal/contact details in the privacy policy.
3. The first public release is `1.4.0` / version code `1`, set on
   2026-11-03 (version numbers follow the iOS feature level, see
   `AGENTS.md`). Raise the version code with every later upload.
4. Confirm that the encrypted PKCS#12 file and password are recoverable from the
   owner's password manager, create a second independent offline backup, and
   configure release signing without placing paths, passwords, or key material
   in Git.
5. Review the listing images under `fastlane/metadata/android/de-DE/images/`
   (icon, feature graphic, phone and tablet screenshots, captured 2026-11-03
   as described in `docs/screenshots.md`) and replace any the owner wants
   changed.
6. Re-run the full build gate from a clean checkout, sign the release APK with
   the recorded identity, record checksums, and verify reproducibility before
   uploading it to GitHub Releases.

Additional Google Play gates may follow later without blocking the initial
GitHub/F-Droid track:

1. Obtain a Pixel 6a or similar physical Android device and smoke-test OEM
   widgets, contact/calendar handoff, backup/restore, process death, and
   background rollover.
2. Create and verify a new personal Play developer account with that physical
   device and pay the current registration fee.
3. Enroll the recorded app-signing identity with Play App Signing using the
   owner-reviewed key-transfer flow; create a separate upload key if requested.
4. Capture the Play-specific feature graphic and 512 px store icon, then
   complete content rating, app-access, ads, target-audience, data-safety, and
   privacy declarations.
5. Complete the required closed test and Play pre-launch reports before
   requesting production access.

## Account, device, and verification gates

Established with the owner on 2026-08-22.

**Play developer account.** The owner's original developer account was closed
for *inactivity*, not terminated for a policy violation. Google's official
inactivity page confirms that the old fee is not refunded, but does not provide
a reactivation procedure. Current, non-authoritative
[Play community guidance](https://support.google.com/googleplay/android-developer/thread/452910803)
reports that the Google account already attached to the closed developer account
cannot be reused for a new registration. Use the owner's separate Google
account for new onboarding, and contact Play support before paying if the signup
flow raises any linked-account concern. Expect a new one-time 25 USD fee.

**Physical device verification.** Since early 2024 a new personal developer
account must clear a device-verification step before it may publish: sign in to
the Play Console mobile app as the account owner on a *non-rooted physical*
Android device running **Android 10 or higher**. Emulators, iPhones, and desktop
browsers do not satisfy it, which is why an earlier attempt from an iPhone
failed. This is a hard blocker on the Play track.

Candidate device: a used Pixel 6a, budget under 100 EUR. It runs Android 16 and
Google's five-year update window reaches July 2027, so it can exercise the real
`targetSdk = 36` surface instead of only the emulator matrix. This is a
conditional recommendation: Google identified a subset of Pixel 6a devices with
a battery-overheating risk, applied capacity/charging limits after 400 cycles,
and ended its support-options program on 2026-07-08. Check battery condition and
device history before purchase. Devices stuck below Android 13 satisfy Google's
verification minimum but provide little value for current-target testing.

**Closed testing.** Personal accounts created after 2023-11-13 need at least 12
testers opted in *continuously* for the 14 days preceding the production-access
request. A tester who opts out stops counting and must begin a new consecutive
period if they return; if fewer than 12 qualifying testers remain, the
application is not eligible. Recruit a buffer above 12 and ask participants to
remain opted in until the request is filed.

**Android Developer Verification, a future sideload gate.** From 2026-09-30,
Google's first regional phase applies on certified Android devices in Brazil,
Indonesia, Singapore, and Thailand only to installations from named
participating stores. F-Droid and direct GitHub sideloads are not among those
stores, so their installation behavior does not change at that first deadline.
Google currently plans a global expansion to all apps on certified devices in
2027. It also documents an advanced opt-in flow for power users, ordinary ADB
installation, and a free limited-distribution account for up to 20 devices;
none is a practical substitute for unrestricted public distribution. Plan to
verify the owner and register the package/signing certificate before the global
phase, while re-checking the policy because the rollout is still changing.

Primary sources for this section, checked 2026-08-22: Play Console help on
[device verification](https://support.google.com/googleplay/android-developer/answer/14316361),
[closed-testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465),
[closure of inactive developer accounts](https://support.google.com/googleplay/android-developer/answer/11605267),
[Android Developer Verification timeline](https://developer.android.com/developer-verification),
[verification FAQ](https://support.google.com/android-developer-console/answer/16561738),
[package/key registration](https://support.google.com/googleplay/android-developer/answer/16761053),
[Pixel update window](https://support.google.com/pixelphone/answer/4457705),
and [Pixel 6a battery guidance](https://support.google.com/pixelphone/answer/16340779).
Treat those as authoritative if this note becomes stale.

## Distribution channel order

1. **GitHub Releases** carrying the signed APK and matching source tag. This is
   also the upstream binary required for the intended reproducible F-Droid
   route.
2. **fdroiddata** inclusion per the F-Droid release track below. Review timing
   is outside the owner's control because F-Droid builds from source and checks
   policy compliance.
3. **Physical Android device**, preferably a used Pixel 6a or similar, followed
   by the independent-device smoke test and Play device verification.
4. **New personal Google Play developer account** using a Google account not
   already attached to the inactive closed developer account.
5. **Google Play release**, after signing enrollment, console declarations,
   pre-launch reports, and the required closed test.

The sideload/F-Droid-first order makes the app available without waiting for
Play's external gates. By explicit owner decision, the documented emulator
matrix is the pre-F-Droid device evidence; the independent physical-device gate
follows F-Droid and must pass before Play publication.

## F-Droid release track

The official F-Droid repository builds the app itself from a specific public
source commit. Erdentage therefore needs a complete, non-interactive FLOSS build,
not merely an APK uploaded by the owner. No separate `fdroid` product flavor is
planned: the Play and F-Droid artifacts must continue to use the same source,
features, and dependency set.

Already prepared:

- `Apache-2.0`, the permanent application ID, and source-visible copyright
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
   `metadata/com.firsching.erdentage.yml`. Pin its build entry to the release tag
   or commit, add source/issue/license/contact fields, and configure update
   detection. Derive the exact YAML from the then-current fdroiddata schema; do
   not copy a generic flavor example into this single-variant project.
6. Run `fdroid build com.firsching.erdentage:<versionCode>` in the supported
   F-Droid environment and make the fdroiddata CI build pass. The inclusion
   merge request must also record policy compliance, related request-for-
   packaging issues, and that the upstream author submitted or accepts it.
7. Re-check the official inclusion policy and merge-request template immediately
   before submission. F-Droid has no paid developer-registration step, but its
   technical and policy review remains mandatory.

The owner chose one owner-controlled identity for every store; its public data
is recorded in `docs/signing.md`. The normal F-Droid path would instead use an
F-Droid-managed per-app key, preventing in-place switches from the Play/upstream
build. Erdentage therefore targets a reproducible build: F-Droid independently
rebuilds the APK and, after a successful match, publishes the owner-signed APK.
Record the signing-tool version, source tag, artifact hash, and reproducibility
result without ever committing the private key or credentials. Use the
then-current `Binaries`/`Builds.binary` and
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

## Next-session checkpoint

Start here after 2026-08-22:

1. Confirm that both the encrypted `.p12` and public `.pem` are attached to the
   owner's password-manager item; arrange an independent offline backup later,
   before the first public artifact.
2. Make `tom111/ErdentageAndroid` (private since 2026-11-02) public with
   Issues enabled, and add its source/issue URLs to metadata. Changing the
   visibility is an external action requiring owner confirmation.
3. Choose the first public version (`versionName` and monotonically increasing
   `versionCode`).
4. Add owner-approved Fastlane images, configure local signing without tracked
   secrets, build and inspect the signed APK, and establish the reproducible
   GitHub/F-Droid release flow.
5. After the GitHub and official F-Droid releases, obtain the physical test
   device; only then begin Google developer-account onboarding and the later
   Play track.

No app-feature implementation remains in the phase-one scope. The last full
gate passed 73 JVM tests, lint, debug/release APK and release AAB assembly, and
12 API 36 instrumentation tests; no production source changed afterward.

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
