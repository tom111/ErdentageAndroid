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
| `Erdentage_API_37` | Android 17 forward compatibility | Resizable | Android 17 / API 37.1, Google APIs, 16 KB pages, ARM64 |
| `Erdentage_API_37_Tablet` | Android 17 large-screen adaptivity | Pixel Tablet | Android 17 / API 37.1, Google APIs, 16 KB pages, ARM64 |

Start the primary emulator from Android Studio's Device Manager or from the
command line:

```sh
$ANDROID_HOME/emulator/emulator -avd Erdentage_API_36
```

After it finishes booting, run the instrumentation tests:

```sh
./gradlew connectedDebugAndroidTest
```

## Android 17 forward gate

Added 2026-08-22. Android 17 shipped in June 2026, so a substantial share of real
users already runs it while the app still declares `targetSdk = 36`. The two
API 37 devices answer a question the API 36 matrix cannot: does the app behave
on an operating system newer than its declared target, where compatibility mode
gates most but not all new restrictions? The large-screen entry exists because
Android 17 makes resizability mandatory on tablets and foldables, which is the
change most likely to affect this app's tablet layout and widgets.

Both deliberately reuse the device profile, RAM, heap, and resolution of their
API 36 counterparts — `Erdentage_API_37` mirrors `Erdentage_API_36` and
`Erdentage_API_37_Tablet` mirrors `Erdentage_API_36_Tablet` — so that any
difference in results is attributable to the operating system rather than to
hardware configuration. The images use 16 KB memory pages, matching current
devices; the app has no native code, so that is a fidelity choice rather than a
risk. No AOSP system image exists for API 37, so the no-Google and F-Droid
validation stays on `Erdentage_API_36_AOSP`.

Compare two devices by running the suite against one at a time. Concurrent runs
distort results:

```sh
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest
```

A second, later target is the `targetSdk = 37` bump that Google Play requires
from August 2027; these AVDs are where that is rehearsed.

Boot the emulators fresh for each measurement session. On 2026-08-23 two long-
lived API 37 instances degraded after many install/uninstall cycles: `adb
devices` still listed them as `device`, but the Gradle plugin refused them with

    Skipping device 'Erdentage_API_37(AVD)' for ':app:': Unknown API Level
     > : No compatible devices connected.

even though the devices reported `ro.build.version.sdk=37`, `codename=REL`, and
no preview flag. Installing `platforms;android-37.1` did not help, and
`--no-configuration-cache` appeared to help once but not on retry. Restarting the
adb server then revealed both as `offline`. Killing the emulator processes and
booting fresh resolved it immediately. Read `Unknown API Level` as a degraded
device, not as a real SDK incompatibility.

## Debug sample state

Debuggable builds seed exactly three fixed people when the local repository is
empty: `Ich`, time-known leap-day example `Lina`, and long-name/astronaut example
`Egon-Steffen-Mike`. This makes a clean install or instrumentation reset useful
for immediate visual testing. Existing people and edits are preserved; the seed
is not applied while any person remains. Non-debuggable release builds never
receive sample people and retain the production first-run flow.

## Account-free calendar fixture

Google authentication cannot be faked globally by an Android emulator. The
primary `Erdentage_API_36` AVD therefore uses a local calendar fixture for
repeatable handoff testing without storing a Google identity:

- Etar `1.0.57` (`ws.xsoh.etar`, version code 57) was installed on 2026-08-20
  from F-Droid's official
  [`ws.xsoh.etar_57.apk`](https://f-droid.org/repo/ws.xsoh.etar_57.apk).
- Downloaded APK SHA-256:
  `dae01d93c8920aa63845868db2190bcc4aa6ff8410b1289de1ce27b1bb89c599`.
  Android APK signature verification passed; the signing-certificate SHA-256
  is `3f3176c3ce189c98054ff9e1d32daecf00a41572f4c7bd2b2f80607252ddb06e`.
- Etar has a visible local `LOCAL` account calendar named
  `Erdentage Testkalender`. Calendar access is allowed, optional contact access
  was denied, and no cloud account is configured.
- Google Calendar is disabled only for emulator user 0, leaving Etar as the
  sole `ACTION_INSERT` handler. Restore it with:

  ```sh
  $ANDROID_HOME/platform-tools/adb shell pm enable --user 0 com.google.android.calendar
  ```

- A real Erdentage handoff was saved and read back from Android's calendar
  provider on 2026-08-20: the title, all-day interval, and selected local
  calendar were correct. This verifies the app-to-calendar contract but does
  not replace a physical-device check or prove behavior of Google Calendar.

If Google-specific behavior is needed, the owner can sign into the Google Play
AVD interactively with a dedicated test account and let Quick Boot preserve its
local AVD state. Never put account credentials in repository scripts, Gradle
properties, screenshots, or committed emulator artifacts.

## Verification record

- `Erdentage_API_36`: all nine instrumentation tests passed on 2026-08-16.
  Visible checks covered first run, person/settings screens, overview,
  force-stop restoration, 1.3x font scale, landscape, and a real launcher-hosted
  widget at compact and wide resized dimensions.
- `Erdentage_API_36`: all eleven instrumentation tests passed on 2026-08-20.
  Additional visible checks covered the launcher icon, smaller system splash,
  three-person debug seed, direct Share/Settings controls, round-number
  captions, and corrected About-footer spacing.
- `Erdentage_API_26`: all nine instrumentation tests passed and the first-run UI
  cold-launched on 2026-08-16.
- `Erdentage_API_36_AOSP`: the APK installed and cold-launched in German with
  `com.google.android.gms` absent on 2026-08-16.
- `Erdentage_API_36_Tablet`: first run, settings, and the wide custom overview
  were visibly checked on 2026-08-16.
- `Erdentage_API_36_Play`: the APK installed and cold-launched with the Play
  Store package present on 2026-08-16.

- `Erdentage_API_37` and `Erdentage_API_37_Tablet`: created 2026-08-22 and
  booted to Android 17 / API 37. Their first runs exposed the `d19514e`
  serialization defect, which reproduced identically on Android 16 and was
  therefore not Android 17 specific. See `docs/parity.md`, 2026-08-22.
- After that defect was fixed on 2026-08-23, all twelve instrumentation tests
  passed on `Erdentage_API_36`, `Erdentage_API_36_Tablet`, and `Erdentage_API_37`.
  `Erdentage_API_37_Tablet` passed eleven of twelve, with
  `editorDraftAndManagerReturnSurviveStateRestoration` failing intermittently —
  three of six runs there against zero of six on `Erdentage_API_36_Tablet`. That
  is the only Android 17 specific observation on record and it is still open;
  see `docs/parity.md`, 2026-08-23.

These are emulator results. By owner decision they are the pre-F-Droid device
evidence. OEM widget behavior, real contact/calendar apps, backup/restore, and
background execution remain for the physical-device gate after F-Droid and
before Google Play, as recorded in `docs/release.md`.
