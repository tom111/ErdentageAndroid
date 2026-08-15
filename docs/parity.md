# Android parity ledger

Every implementation task must record the current iOS reference commit,
behavioral coverage, tests, visual evidence, and intentional Android
deviations here.

## Current reference

- iOS repository: `/Users/tom/SynologyDrive/projects/ErdentageiOS`
- Commit: `95aa5b595ec8cad1ed8c7ebcf8c039e7a3588eae`
- Branch/worktree: `main`; committed parity sources clean, unrelated
  `TODO.md` modification present
- Inspected: 2026-08-15

## Ledger

| Area | iOS reference | Android status | Verification | Deviation |
| --- | --- | --- | --- | --- |
| Project skeleton | `95aa5b5` | Complete | `test lint assembleDebug`; `assembleRelease bundleRelease`; API 36 instrumentation test; visible launch and screenshot inspected 2026-08-15 | Native Android build and lifecycle |
| Domain model and day count | `95aa5b5` | Complete | JVM tests cover civil dates, invalid data, zone changes, DST gaps/overlaps, future dates, residual time, rollover, limits, UUIDs, sentinel, JSON schema, retention, and backfill deltas | Uses `java.time` with injected clock/zone; DataStore repository is a separate pending slice |
| Milestone engine | `95aa5b5` | Complete | Shared fixture JSON plus JVM tests cover all kinds, strict-next boundaries, large values, current-day detection, filtering, same-target collapse, ordering, and dates | Pure optimized Kotlin port |
| Person pager | `95aa5b5` | Not started | Pending | Native Compose pager |
| Demnächst overview | `95aa5b5` | Not started | Pending | Native Compose canvas |
| Settings and people | `95aa5b5` | Not started | Pending | Native Android controls |
| Platform integrations | `95aa5b5` | Not started | Pending | Android intents and selected-contact access |
| Home-screen widget | `95aa5b5` | Not started | Pending | One configurable Glance widget type |

## Verification log

### 2026-08-15 — model and domain core

- `./gradlew test lint assembleDebug assembleRelease bundleRelease` passed
  with dependency locking and checksum verification enabled.
- 37 JVM test methods passed across milestone fixtures/math, civil-date and
  day-count behavior, DST, model normalization/serialization, retention, and
  backfill behavior.
- `./gradlew connectedDebugAndroidTest` passed on `Erdentage_API_36`.
- Foundation and `java.time` produced the same next-rollover instants for the
  Europe/Berlin spring DST gap and autumn overlap.
