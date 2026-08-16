# Erdentage for Android

Native Android port of the German family app Erdentage. The app is being built
with Kotlin and Jetpack Compose for phones, tablets, and Android widgets while
preserving behavior and visual identity from the iOS source.

The domain and persistence layers are in place, along with a functional
first-run birth-date flow, person counter, milestone feed, pager, and initial
“Demnächst” overview. See [`PLAN.md`](PLAN.md) for the roadmap,
[`docs/parity.md`](docs/parity.md) for parity status, and
[`docs/emulators.md`](docs/emulators.md) for the local Android test matrix.

## Build

Prerequisites:

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0

```sh
./gradlew test lint assembleDebug
```

Open this directory in Android Studio or start the documented primary AVD,
then run `./gradlew connectedDebugAndroidTest` for the emulator smoke test.

## License

Copyright (C) 2026 Thomas Kahle. Licensed under
[`GPL-3.0-or-later`](LICENSE).
