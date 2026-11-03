# App signing identity

The Android app-signing key is independent of the iOS app: Apple signs the
iOS build through its own certificate and provisioning system, while an
Android APK is signed with a self-generated keystore. "One key" in these
documents means one Android key shared by the GitHub, F-Droid and Google
Play artifacts, so users can move between stores in place.

## Current state (2026-11-03)

The Android maintainer (Moritz Firsching) created and holds the signing key
for `ch.firsching.erdentage` on 2026-11-03. Public identity:

- Alias: `erdentage`
- Container: PKCS#12
- Key: RSA 4096 bit, self-signed certificate SHA384withRSA
- Subject: `CN=Moritz Firsching, OU=Erdentage, O=Moritz Firsching, C=DE`
- Validity: 2026-11-03 through 2126-10-10
- Certificate SHA-256 fingerprint:
  `E2:93:80:D0:66:E5:DD:64:9B:A0:99:8D:B9:A7:1C:A9:96:CC:8A:98:D5:75:15:7C:21:B8:28:99:67:9D:54:AA`
- Initial encrypted PKCS#12 SHA-256 checksum:
  `55bbd90e4058667a41719546a6f15179933b725b1acda361201841fd40f53688`

The private key and its password stay outside Git; keep a password-manager
copy and an independent offline backup before the first public artifact.
The first signed build from this identity (`assembleRelease bundleRelease`
on 2026-11-03, version 0.1.0 (1), v2 and v3 signatures, apksigner from
build-tools 36.0.0) verified with that certificate; it is a dry run, not a
release.

The identity Thomas Kahle generated on 2026-08-22 for `de.eigenraum.erdentage`
(RSA 4096, alias `erdentage-app`, certificate SHA-256
`82:7D:B4:BA:37:00:7F:3D:7C:FD:90:9B:C3:C6:9F:50:3A:EE:15:CD:13:DA:BC:0B:A0:1B:02:F6:E7:B1:8F:D6`)
never signed an artifact and is not used for this package.

## How the key reaches each channel

- **Local release build.** `app/build.gradle.kts` reads an untracked
  `keystore.properties` at the repository root (`storeFile`, `storePassword`,
  `keyAlias`, `keyPassword`); both that file and keystores are ignored by
  Git. With the file present, `./gradlew assembleRelease bundleRelease`
  produces a signed APK (v1 to v3 signatures) and a signed AAB; without it the
  release stays unsigned, which is what a clean checkout and the F-Droid
  rebuild use.
- **GitHub Releases.** Upload the signed APK from that build with its SHA-256.
- **F-Droid, reproducible route.** fdroiddata rebuilds the tagged source,
  compares it with the upstream APK and, on a match, publishes the
  upstream-signed APK; `AllowedAPKSigningKeys` names this certificate. The
  key never leaves the maintainer's machine.
- **Google Play.** Enrol the same key as the *app signing key* under Play App
  Signing with the "use an existing key" path: Play's PEPK tool encrypts the
  private key for upload. Then Play-distributed APKs carry this certificate
  too, matching GitHub and F-Droid. A separate upload key is optional; if
  used, create it the same way and register it in the console.

For every public release, record the source tag and commit, version name and
code, signing-tool version, artifact SHA-256 checksum, signing-certificate
fingerprint, and reproducibility result.
