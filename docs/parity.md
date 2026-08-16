# Android parity ledger

Every implementation task must record the current iOS reference commit,
behavioral coverage, tests, visual evidence, and intentional Android
deviations here.

## Current reference

- iOS repository: `/Users/tom/SynologyDrive/projects/ErdentageiOS`
- Commit: `95aa5b595ec8cad1ed8c7ebcf8c039e7a3588eae`
- Branch/worktree: `main`; committed parity sources clean, unrelated
  `TODO.md` modification present
- Inspected: 2026-08-16

## Ledger

| Area | iOS reference | Android status | Verification | Deviation |
| --- | --- | --- | --- | --- |
| Project skeleton | `95aa5b5` | Complete | `test lint assembleDebug`; `assembleRelease bundleRelease`; API 36 instrumentation test; visible launch and screenshot inspected 2026-08-15 | Native Android build and lifecycle |
| Domain model and day count | `95aa5b5` | Complete | JVM tests cover civil dates, invalid data, zone changes, DST gaps/overlaps, future dates, residual time, rollover, limits, UUIDs, and the overview sentinel | Uses `java.time` with injected clock/zone |
| Milestone engine | `95aa5b5` | Complete | Shared fixture JSON plus JVM tests cover all kinds, strict-next boundaries, large values, current-day detection, filtering, same-target collapse, ordering, and dates | Pure optimized Kotlin port |
| Persistence and repository | `95aa5b5` | Complete | File-backed JVM tests cover JSON schema, defaults, corruption replacement, concurrent transactions, cap, selection, reorder/delete, retention, and physically persisted backfill deltas | Typed app-private JSON DataStore; no obsolete iOS-key migration |
| Theme and tint foundation | `95aa5b5` | In progress | Exact adaptive tint mapping tests; rendered gradient pixel test; light/dark phone previews; owner emulator review pending | Neutral Material scheme and system sans; no screenshot harness by owner decision |
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

### 2026-08-16 — DataStore repository

- All 51 JVM test methods pass; 14 exercise the serializer and repository with
  real DataStore I/O, including concurrent writes and corruption replacement.
- Android OS-managed backup remains enabled; the app-private DataStore file is
  covered by the existing default backup rules.
- The packaged APK requests no network, contacts, or calendar permission. Its
  only declared permission is the app-scoped AndroidX dynamic-receiver guard.

### 2026-08-16 — visual foundation

- Added the canonical light/dark person surface and lantern colors, neutral
  Material schemes, zero-letter-spacing typography, restrained shapes, and a
  reusable top-to-center person tint gradient.
- Debug-only fixed people provide deterministic light/dark phone previews
  without adding sample data to the production repository.
- An emulator pixel test verifies the rendered top tint and the transition to
  the neutral background at the vertical midpoint and below.
- All 54 JVM tests, both API 36 instrumentation tests, lint, debug/release APK
  assembly, and release AAB bundling pass; the verified APK cold-launches.
- The owner waived an automated screenshot harness. Visible emulator runs,
  owner screenshots/feedback, Compose previews, semantics tests, and focused
  pixel assertions form the visual verification workflow.

## Scope decisions

The current committed iOS source contains a second trailing `TogetherPageView`
(“Zusammen”) when at least two people exist, tagged at `people.count + 1`.
On 2026-08-16 the owner confirmed that this feature is still under development
and not fixed. Android phase one targets the shipped iOS 1.3 feature set
without “Zusammen”, retaining the single “Demnächst” sentinel at
`people.size`. Reconsider the additional page only after its iOS behavior is
finalized and separately approved.
