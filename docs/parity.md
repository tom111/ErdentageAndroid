# Android parity ledger

Every implementation task must record the current iOS reference commit,
behavioral coverage, tests, visual evidence, and intentional Android
deviations here.

## Current reference

- iOS repository: `/Users/tom/SynologyDrive/projects/ErdentageiOS`
- Commit: `95aa5b595ec8cad1ed8c7ebcf8c039e7a3588eae`
- Branch/worktree: `main`; uncommitted in-development “Zusammen” source,
  project, shared-defaults, management, overview, and test changes present;
  those changes remain outside the approved Android scope
- Inspected: 2026-08-16

## Ledger

| Area | iOS reference | Android status | Verification | Deviation |
| --- | --- | --- | --- | --- |
| Project skeleton | `95aa5b5` | Complete | `test lint assembleDebug`; `assembleRelease bundleRelease`; API 36 instrumentation test; visible launch and screenshot inspected 2026-08-15 | Native Android build and lifecycle |
| Domain model and day count | `95aa5b5` | Complete | JVM tests cover civil dates, invalid data, zone changes, DST gaps/overlaps, future dates, residual time, rollover, limits, UUIDs, and the overview sentinel | Uses `java.time` with injected clock/zone |
| Milestone engine | `95aa5b5` | Complete | Shared fixture JSON plus JVM tests cover all kinds, strict-next boundaries, large values, current-day detection, filtering, same-target collapse, ordering, and dates | Pure optimized Kotlin port |
| Persistence and repository | `95aa5b5` | Complete | File-backed JVM tests cover JSON schema, defaults, corruption replacement, concurrent transactions, cap, selection, reorder/delete, retention, and physically persisted backfill deltas | Typed app-private JSON DataStore; no obsolete iOS-key migration |
| Theme and tint foundation | `95aa5b5` | In progress | Exact adaptive tint mapping tests; rendered gradient pixel test; light/dark phone previews; owner emulator review pending | Neutral Material scheme and system sans; no screenshot harness by owner decision |
| Person pager | `95aa5b5` | In progress | Compose tests cover empty state, person render, single overview sentinel, swipe, indicator, top controls, and share copy; visible API 36 launch | Milestone-row actions pending; Android combines edit/share in one overflow menu |
| Demnächst overview | `95aa5b5` | In progress | Merged chronological 30-row feed runs in pager and instrumentation test | Exact sky colors present; custom cloud canvas/wobble pending |
| Settings and people | `95aa5b5` | In progress | Compose tests cover add/edit entry, reorder/delete actions, and saving a named draft; JVM tests cover time clamps and prime/twin selection; visible phone review | Contact import pending; Android uses accessible up/down commands rather than iOS drag-edit mode |
| Platform integrations | `95aa5b5` | In progress | Current-person text reaches `ACTION_SEND` through a chooser; Compose test verifies named share copy | Contact picker and calendar handoff pending |
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

### 2026-08-16 — first functional UI

- The runtime now collects DataStore through a lifecycle-aware `ViewModel`;
  the scaffold-only title screen is gone.
- A fresh install offers a native three-spinner birth-date picker with the iOS
  5,000-day preset, persists the owner person, and renders the live count and
  upcoming milestones using German formatting. The spinner presentation was
  selected after owner feedback so the year is independently selectable.
- The Compose pager has stable person keys, exactly one trailing “Demnächst”
  sentinel, persisted selection, an inset-safe indicator, and compact-height
  side-by-side person layout.
- The initial “Demnächst” implementation merges, sorts, and caps events at 30
  over the exact adaptive sky gradient. The custom cloud canvas remains a
  later parity slice.
- Four API 36 instrumentation tests pass, covering activity launch, empty
  state, person-to-overview swiping, pager semantics, and rendered gradient
  pixels. All 56 JVM tests, lint, debug/release APK assembly, and release AAB
  bundling also pass. The final debug APK was reinstalled and visibly checked
  on the emulator without clearing its saved app data.

### 2026-08-16 — multi-person management and options

- Added inset-safe people and overflow controls to person pages. The overflow
  menu edits the current person or shares the current German day-count copy;
  controls remain hidden on the trailing “Demnächst” page.
- Added a full-screen people manager with stable-UUID selection, add, edit,
  delete confirmation, accessible up/down reorder commands, active-person
  indication, distinct default tints, and the 20-person cap. A new manual
  person is persisted only after the editor is confirmed, so cancel leaves no
  stub record.
- Added the per-person editor for name, independent-wheel date, optional time,
  days outside Earth, all tint swatches, mutually exclusive ordinary/twin
  prime selection, every other milestone kind, and version information.
- Native date/time picker actions are explicitly German on an English device.
  The picker, person top controls, people list, and editor were visibly
  inspected on `Erdentage_API_36`; the numeric stepper was revised after the
  first phone-width render exposed an overlap.
- All 60 JVM tests and seven API 36 instrumentation tests pass, along with
  lint, debug/release APK assembly, and release AAB bundling. Dependency
  verification and locking cover the small Compose core icon artifact; the
  project-owned group/minus vectors avoid the extended icon bundle.

## Scope decisions

The current committed iOS source contains a second trailing `TogetherPageView`
(“Zusammen”) when at least two people exist, tagged at `people.count + 1`.
On 2026-08-16 the owner confirmed that this feature is still under development
and not fixed. Android phase one targets the shipped iOS 1.3 feature set
without “Zusammen”, retaining the single “Demnächst” sentinel at
`people.size`. Reconsider the additional page only after its iOS behavior is
finalized and separately approved.
