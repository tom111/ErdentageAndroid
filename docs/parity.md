# Android parity ledger

Every implementation task must record the current iOS reference commit,
behavioral coverage, tests, visual evidence, and intentional Android
deviations here.

## Current reference

- iOS repository: https://github.com/tom111/ErdentageiOS, cloned as
  `../ErdentageiOS`
- Current iOS HEAD inspected: `df8d951d7bb0e5da488e74be50fac17a55ea9e57`
- Branch/worktree: `main`, clean
- Approved shipped-1.3 parity baseline:
  `4f3ddc22f0b49d14ecf56d7af382ece0a3ca5f99`
- Inspected: 2026-08-20
- Phase-two reference (iOS 1.4 parity, approved 2026-11-02): tag
  `v1.4.2-build19`, commit `089cf5a960f4040d03a78cc052738479d4416bc1`

## Ledger

| Area | iOS reference | Android status | Verification | Deviation |
| --- | --- | --- | --- | --- |
| Project skeleton | `4f3ddc2` | Complete | `test lint assembleDebug`; `assembleRelease bundleRelease`; API 36 instrumentation test; visible launch and screenshot inspected 2026-08-15 | Native Android build and lifecycle |
| Domain model and day count | `4f3ddc2` | Complete | JVM tests cover civil dates, invalid data, zone changes, DST gaps/overlaps, future dates, residual time, rollover, limits, UUIDs, and the overview sentinel | Uses `java.time` with injected clock/zone |
| Milestone engine | `4f3ddc2` | Complete | Shared fixture JSON plus JVM tests cover all kinds, strict-next boundaries, large values, current-day detection, filtering, same-target collapse, ordering, and dates | Pure optimized Kotlin port |
| Persistence and repository | `4f3ddc2` | Complete | File-backed JVM tests cover JSON schema, defaults, corruption replacement, concurrent transactions, cap, selection, reorder/delete, retention, and physically persisted backfill deltas | Typed app-private JSON DataStore; no obsolete iOS-key migration |
| Theme and tint foundation | `4f3ddc2` | Complete | Exact tint tests, rendered gradient pixels, light/dark review, phone/tablet/landscape and 1.3x font-scale emulator passes | Neutral Material scheme and Android system sans; screenshot harness waived by owner |
| Person pager | `df8d951` | Complete | Compose tests cover empty state, person render, sentinel, swipe, indicator, direct share/settings controls, milestone actions, management, and contact import | Material share/settings icons replace SF Symbols |
| Demnächst overview | `4f3ddc2` | Complete | Chronological 30-row cap, deterministic cloud-math test, visible phone/landscape/tablet renders, large-font correction | Compose shapes reproduce seeded wobble rather than SwiftUI paths verbatim |
| Settings and people | `df8d951` | Complete | Add/edit/delete/reorder/import flows, exact linked-footer spacing, and setting logic tested; phone and tablet first-run editors visibly checked | Accessible up/down commands replace iOS drag-edit mode; Android-native pickers |
| Platform integrations | `4f3ddc2` | Complete | Sharesheet and milestone-copy Compose tests; permission-free calendar drafts and action history; selected-contact parser/fallback tests | Calendar record means handoff launched, never confirmation of save |
| Home-screen widget | `4f3ddc2` | Complete | Selection/fallback JVM tests; real API 36 launcher registration, configuration, fixed/follow-current rendering, and live resize to wide layout | One responsive Glance widget; ordinary Android lock-screen host compatibility only where OEM exposes it |
| Data model and page index (phase two) | `089cf5a` | Planned | — | — |
| Geburtstags-Zufälle | `089cf5a` | Planned | — | — |
| Zusammen calculations | `089cf5a` | Planned | — | — |
| Zusammen screen and budgets | `089cf5a` | Planned | — | — |
| Small 1.4/1.4.1 UI changes | `089cf5a` | Planned | — | — |
| Release preparation | Android-specific | In progress | Unsigned APK/AAB build; metadata, privacy/data-safety drafts, adaptive icon, AOSP and Play-image smoke tests; permanent cross-store signing identity recorded | Signed/reproducible artifact, key backup confirmation, hosted URLs, screenshots, store submissions, Play account/closed test, and independent-device gate remain |

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
- At this checkpoint, the initial “Demnächst” implementation merged, sorted,
  and capped events at 30 over the exact adaptive sky gradient. The custom
  cloud canvas was completed in the later parity entry below.
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

### 2026-08-16 — remaining iOS 1.3 parity

- Added balanced per-milestone overflow actions for German text sharing and a
  permission-free calendar `ACTION_INSERT` handoff. The app records only that
  the handoff was launched and uses neutral UI wording.
- Added selected-contact import through the system picker with no broad
  contacts permission. Complete birthdays enter the editor directly; missing
  or yearless birthdays open a date picker and cancel creates nothing.
- Replaced the provisional overview list with deterministic alternating cloud
  shapes, adaptive tint fills/outlines, light/dark skies, tablet sizing, and
  compact-height behavior. A 1.3x-font landscape pass exposed clipped captions;
  cloud height/spacing now follows system font scale and the corrected render
  keeps all visible text inside its shape.
- Added one responsive Glance widget with fixed-person and follow-current
  configuration, immediate repository updates, next-rollover WorkManager
  scheduling, time/time-zone rescheduling, deletion cleanup, and OS-backup
  exclusion for widget IDs. A real Pixel launcher instance rendered correctly
  at 2x2 and after live resizing to a wide milestone layout.
- Added lifecycle-aware foreground refresh, system time/date/zone handling,
  Android backup rules, adaptive/round/monochrome icons, asset provenance,
  German listing metadata, privacy policy, data-safety draft, and release
  checklist.
- All 66 JVM tests pass. Nine instrumentation tests pass on both API 36 and the
  API 26 compatibility floor. The APK cold-launches on the API 36 AOSP image
  with Play Services absent, on the tablet image, and on the Google Play image.
  The overview sentinel was also verified across force-stop/cold-relaunch after
  its DataStore transaction completed.

### 2026-08-20 — live parity polish and emulator fixtures

- Inspected iOS HEAD `df8d951d7bb0e5da488e74be50fac17a55ea9e57`.
  Its current person chrome uses separate Share and Settings actions, and its
  milestone caption source includes the round-thousand kinds.
- Replaced Android's two-item person overflow menu with separate Material Share
  and Settings buttons. All three top controls have explicit 48 dp bounds and
  direct content descriptions.
- Restored `nächster Tausender` and `nächster Zehntausender` above personal
  milestone rows and in milestone-capable widgets. Round values remain untagged
  in calendar/share suffixes and overview cloud badges, matching iOS
  `eventTitleTag` behavior.
- Fixed the linked About footer by inserting separators in the annotated text
  rather than relying on resource-edge whitespace, which Android trims.
- Debuggable builds now seed exactly three deterministic, representative people
  whenever the repository is empty; non-debuggable release builds still start
  empty and never receive those fixtures.
- Replaced the oversized default Android 12 splash rendering with the stable
  AndroidX SplashScreen API and a smaller source-derived Erdentage mark on an
  adaptive neutral background. Cold-start frames and the launcher icon were
  visibly inspected on `Erdentage_API_36`.
- `test lint assembleDebug assembleRelease bundleRelease` passed with updated
  dependency locking and checksum verification. Eleven API 36 instrumentation
  tests cover the runtime three-person seed, direct top actions, exact footer
  spacing, and round-number captions in addition to the existing UI contract.

### 2026-08-20 — reviewed robustness pass

- Triaged `docs/code-review-2026-08-20.md` against Android commit `737726b` and
  iOS HEAD `df8d951d7bb0e5da488e74be50fac17a55ea9e57`; the report now records accepted,
  partial, rejected, and owner-decision findings instead of treating every
  suggestion as a required change.
- Corrected deterministic cloud jitter, retained cloud shapes across
  recomposition, rejected future contact birthdays, broadened defensive
  birthday parsing, and moved contact-provider and milestone computation work
  off the main thread.
- Preserved unsaved person edits and navigation across saved-instance restore,
  made the astronaut-day field naturally editable, and changed editor saves to
  merge with the newest calendar history.
- Added transient DataStore read retry without substituting an apparently empty
  family, removed process-global locale mutation, hardened share handoff, used
  the native time-format preference, and restored widget rollover scheduling
  after widget updates.
- 73 JVM tests and 12 API 36 instrumentation tests pass. The new emulator test
  covers draft restoration, manager return, and empty-to-number input. The
  newer-schema/malformed-record data-loss policy remains an explicit owner
  decision before its serializer behavior changes.

### 2026-08-22 — Android 17 gate and a blocking serialization defect

Added `Erdentage_API_37` and `Erdentage_API_37_Tablet` (Android 17 / API 37.1,
Google APIs, 16 KB pages, ARM64), each mirroring the device profile, RAM, heap,
and resolution of its API 36 counterpart so that only the operating system
differs. See `docs/emulators.md`.

Running the suite against them surfaced a defect that predates them and is not
Android-version specific. At HEAD `d2e87bf`, five of twelve instrumentation
tests fail on Android 16 and Android 17 alike, and on the phone profile the app
process crashes and aborts the run outright:

    java.lang.IllegalAccessError: Field
    'de.eigenraum.erdentage.domain.Person.Companion' is inaccessible to class
    'de.eigenraum.erdentage.ui.PersonEditorStateCodec'
        at PersonEditorStateCodec.encode(PersonEditorStateCodec.kt:29)

Cause: `Person` declares a `private companion object` for `isCanonicalUuid`.
The `@Serializable` plugin places the generated `serializer()` into that same
private companion. `PersonEditorStateCodec` then calls the reified
`ErdentageJson.encodeToString(person)` and `decodeFromString<Person>(value)`,
which inline a direct reference to `Person.Companion` at the call site in
another class. The Kotlin compiler accepts this; ART rejects it at runtime.

`PersonEditorStateCodec.kt` was introduced by `d19514e` on 2026-08-20 at 21:52.
The build outputs present in the working tree were produced at 20:31 the same
evening, before that commit, so the previously recorded green runs were made
against an APK that did not yet contain the file. The suite had never been run
against `d19514e`.

Impact is not confined to tests: the affected path is the person editor's
`rememberSaveable` saver, so any real save or restore of an in-progress editor
hits it.

Candidate fix, not yet applied: drop `private` from `Person`'s companion and
keep `isCanonicalUuid` private inside it, which preserves the encapsulation the
private companion was meant to provide while making the generated `serializer()`
reachable. Alternatives are passing `Person.serializer()` explicitly at every
call site, or moving the helper out of the companion entirely.

Re-run the full suite on `Erdentage_API_37`, `Erdentage_API_37_Tablet`, and
`Erdentage_API_36` after the fix. Until then no statement about Android 17
compatibility is supported by evidence — the observation so far is only that no
failure unique to Android 17 has appeared.

The fix was applied and measured on 2026-08-23; see the next entry.

### 2026-08-23 — serialization fix applied, Android 17 measured

`Person`'s companion is no longer private, and `isCanonicalUuid` is private
inside it. The helper has exactly one caller, the `init` block of `Person`
itself, and a private member of a companion stays reachable from the containing
class, so the encapsulation the private companion provided is unchanged while
the generated `serializer()` becomes reachable from `PersonEditorStateCodec`.
`./gradlew test lint assembleDebug` passes.

Full suite after the fix, one device at a time:

| Device | Android | Result |
| --- | --- | --- |
| `Erdentage_API_36` | 16 | 12 of 12 |
| `Erdentage_API_36_Tablet` | 16 | 12 of 12 |
| `Erdentage_API_37` | 17 | 12 of 12 |
| `Erdentage_API_37_Tablet` | 17 | 11 of 12 |

The `IllegalAccessError` is gone everywhere. One residual remains, and it is the
first observation in this project that separates Android 17 from Android 16:
`editorDraftAndManagerReturnSurviveStateRestoration` fails intermittently and
only on the Android 17 large-screen profile.

    java.lang.AssertionError: Failed to perform text input.
    Failed to assert the following: (is enabled)
    The node is no longer in the tree, last known semantics:
    Node #1366 ... Tag: 'days_off_earth_input'

Post-fix tally: Android 17 tablet failed 3 of 6 runs; Android 16 tablet failed 0
of 6; the Android 17 phone passed. The same test was also the single failure on
the Android 17 tablet in the first pre-fix run of 2026-08-22, while the Android
17 phone passed that run, so the asymmetry predates the fix and is not an
artifact of it.

Reading, explicitly not proven: the composable is removed from the tree while
the test is still interacting with it, which is what an activity recreation
arriving at a different moment would look like. Android 17 makes resizability
mandatory on large screens, so a configuration change the app did not previously
receive on a tablet is the obvious candidate. The sample is small and the effect
is intermittent, so this is a lead, not a diagnosis.

Open, in order: raise the repetition count until the failure rate is stable;
then determine whether the test races a configuration change or the app genuinely
loses editor state. No user-visible defect has been demonstrated — a Compose test
losing a node is not by itself evidence that a person's unsaved edits are lost on
a real tablet.

Verdict: Android 17 is clean on the phone profile. The large-screen profile has
one open intermittent question, which is a release note for the tablet claim in
the ledger, not a blocker for the phone.

## Scope decisions

The current committed iOS source contains a second trailing `TogetherPageView`
(“Zusammen”) when at least two people exist, tagged at `people.count + 1`.
On 2026-08-16 the owner confirmed that this feature is still under development
and not fixed. Android phase one targets the shipped iOS 1.3 feature set
without “Zusammen”, retaining the single “Demnächst” sentinel at
`people.size`. Reconsider the additional page only after its iOS behavior is
finalized and separately approved.

**Update 2026-11-02:** iOS shipped „Zusammen“ in 1.4, and the owner approved
porting it together with the other iOS changes since 1.3 (the
Geburtstags-Zufälle and three small UI changes). The reference is tag
`v1.4.2-build19`. The steps are in `PLAN.md` under *Phase two: iOS 1.4
parity*. The five phase-two rows in the ledger above track them.
