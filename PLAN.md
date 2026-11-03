# Erdentage Android port plan

Status: **phase-one feature implementation complete** on 2026-08-16 and
**phase-two implementation complete** on 2026-11-03 (iOS 1.4 parity:
„Zusammen“, the Geburtstags-Zufälle and the small UI changes; see
[Phase two](#phase-two-ios-14-parity)). The permanent signing identity was
created on 2026-08-22; release-signing setup, hosted URLs, listing
screenshots, store workflows, and independent physical-device verification
remain release work.

This plan was prepared on 2026-07-22 after inspecting the SwiftUI source,
tests, screenshots, widgets, and project notes in the iOS repository. Durable
agent rules and parity contracts are in [`AGENTS.md`](AGENTS.md).

## Goal

Create a native Android edition of the family app **Erdentage** that is
recognizably the same product as the iPhone/iPad app and can be developed in
parallel with it.

The Android edition should preserve:

- The day-count and milestone mathematics.
- The multi-person model and “Demnächst” family overview.
- The visual hierarchy, tint palette, gradients, cloud design, German copy,
  light/dark appearance, and adaptive portrait/landscape behavior.
- Sharing, selected-contact import, calendar handoff, and configurable widgets.
- The local-only privacy model: no account, cloud, tracking, advertising, or
  analytics.

The intended stores are Google Play, official F-Droid, and potentially other
free-software Android stores. The app must remain useful on an AOSP device
without Google Play Services.

## Reference baseline

The live source of truth is:

- https://github.com/tom111/ErdentageiOS, cloned next to this repository as
  `../ErdentageiOS`
- Audited baseline commit:
  `4f3ddc22f0b49d14ecf56d7af382ece0a3ca5f99`
- Baseline state: `main`, clean, iOS 1.3 build 16

The iOS app continues to change. Each Android parity task must inspect the
then-current iOS commit and record it in [`docs/parity.md`](docs/parity.md).
Android should port current tested behavior, not freeze the July 2026 snapshot.

## Approved technology decision

| Area | Decision |
| --- | --- |
| Production language | Kotlin |
| UI framework | Jetpack Compose with Material 3 primitives and a custom Erdentage theme |
| App structure | Single activity, edge-to-edge, Compose Navigation, adaptive layouts |
| State | Immutable UI state, unidirectional data flow, ViewModel, StateFlow, coroutines |
| Domain | Pure Kotlin day-count and milestone logic with injected clock/time zone |
| Persistence | Typed JSON DataStore with `kotlinx.serialization` |
| Widgets | Stable Jetpack Glance with responsive layouts and per-instance configuration |
| Build | Gradle Kotlin DSL, Gradle wrapper, version catalog, JDK 17 |
| Android versions | Minimum API 26; compile and target API 36 |
| Application ID | `ch.firsching.erdentage` (code namespace `de.eigenraum.erdentage`) |
| Dependency injection | Small manual `AppContainer`; no DI framework initially |
| Tests | JUnit, repository tests, Compose semantics/UI tests, focused rendered-pixel checks, emulator instrumentation |

### Why Kotlin and Compose

Compose is Android's native recommended UI toolkit and is conceptually close
to SwiftUI. It provides direct access to Android lifecycle, accessibility,
adaptive layouts, widgets, system intents, and emulator tooling.

Flutter, React Native, and a Kotlin Multiplatform conversion are not
recommended. The iOS app is already a working native SwiftUI application, so
those choices would introduce another runtime or require restructuring the iOS
app without providing enough useful shared code. The mathematics can instead
stay independently implemented and cross-checked with shared fixtures.

### Android version decision

- `minSdk = 26` supports Android 8.0 and later, gives a broad compatibility
  range, and supports in-app widget pinning.
- `compileSdk = 36` and `targetSdk = 36` anticipate the Google Play requirement
  for new applications and updates beginning 2026-08-31.
- Re-check these values and the current stable toolchain immediately before
  project creation and before every store release.

The checked-in version catalog is the source of truth for exact dependency
versions. Keep mutually compatible stable releases of Android Studio, Android
Gradle Plugin, Kotlin, Compose BOM, AndroidX, DataStore, and Glance. Avoid
alpha, beta, and RC dependencies unless a required feature has no stable
implementation and the owner approves the risk.

## Release scope

### Phase one

Feature parity targets the shipped iOS 1.3 app. The “Zusammen” page shipped
in iOS 1.4 and is ported in [phase two](#phase-two-ios-14-parity), together
with the other iOS changes since 1.3.

- Android phones.
- Android tablets and resizable/foldable windows through adaptive layouts.
- Portrait and landscape.
- Light and dark modes.
- Responsive home-screen widgets, configurable per person.
- Compatibility with Android lock-screen widget hosts where the OS/OEM makes
  ordinary app widgets available.
- Google Play and F-Droid-ready builds from the same feature set.

### Deferred

Defer Wear OS and complications until after the phone/tablet/widget release.
The paired-only iOS Watch design depends on device synchronization. The usual
Wear OS Data Layer is tied to Google Play Services, which conflicts with the
F-Droid-first dependency policy. A later Wear phase needs an explicit decision
about synchronization, product flavors, and store eligibility.

Also deferred unless separately requested:

- Notifications from the iOS TODO.
- Cloud or cross-device sync.
- Android TV, Automotive, XR, and desktop-specific experiences.
- Any attempt to transfer local iOS data to Android.

Android OS-managed backup and restore is allowed. The app will not provide its
own account, cloud service, or cross-device synchronization, but its local data
may be included in the user's configured system backup. Restore behavior must
preserve schema and behavioral invariants and receive focused testing.

## Functional slices

### 1. Domain and data

- Port the current `Person` model with stable UUIDs.
- Store civil birth components rather than an epoch timestamp.
- Support optional birth hour/minute and local-time rollover semantics.
- Preserve the 20-person cap, 0–100,000 days-off-Earth range, 600,000,000
  displayed-day clamp, and calendar-action retention rules.
- Preserve the overview page sentinel `currentIndex == people.size` through
  saves, reorder, deletion, and widget fallback.
- Preserve owner naming: blank/whitespace or `Ich` displays as `Ich` and uses
  first-person German sharing text.
- Use a versioned Android schema representing the current iOS model. Do not
  port obsolete iOS `UserDefaults` migration keys because Android has no prior
  Erdentage installation.
- Preserve the versioned delta mechanism for future milestone backfills.

### 2. Milestone engine

Port all currently shipped kinds and optimized algorithms:

- Tausender and Zehntausender.
- Primzahlen and the optional mutually exclusive “Nur Zwillinge” filter.
- Palindrome and Quadratzahlen.
- Zweierpotenzen.
- Ascending, descending, and repeated-digit Ziffernfolgen.
- Catalan-Zahlen through C(18).
- Fibonacci-Zahlen using `1, 2, 3, 5, ...`.

Translate the Swift test corpus before building milestone UI. Add shared JSON
fixtures where useful so Swift and Kotlin independently produce the same
targets, dates, ordering, same-target collapse, default sets, and boundary
behavior. Test leap days, invalid dates, DST, time-zone changes, future dates,
large values, corrupted storage, and milestone backfill deltas.

### 3. Main experience

- Empty first-run state and friendly date preset.
- Horizontal person pager with stable keys and trailing “Demnächst” page.
- Top-left people management and top-right share/settings actions.
- Per-person adaptive tint gradient.
- “Erdentage”, display name, large rounded/monospaced day count, optional
  residual hours/minutes, and current-day milestone label.
- Scrollable upcoming milestone rows with one overflow action target each.
- Portrait vertical layout and compact-height landscape two-column layout.
- Pager indicator positioned safely above gesture navigation.

### 4. “Demnächst” overview

Treat this as a custom branded surface rather than replacing it with a generic
Material list.

- Chronologically merge upcoming milestones across all people and cap at 30.
- Reproduce the light sky-to-cream and dark midnight-to-dusk backgrounds.
- Draw alternating cloud bubbles with deterministic seeded wobble.
- Use each person's adaptive tint, outline, and dark-mode glow.
- Match compact phone landscape and tablet sizing from the Swift source.
- Keep cloud positions stable across recomposition and clock refreshes.

### 5. Settings and people management

- Name, birth date, optional time, days outside Earth, tint, milestones, and
  about/feedback sections in the same order and German wording.
- Add, select, delete, and reorder people while retaining UUID selection.
- Manual add and system contact picker paths both land in the editor.
- Read only the one selected contact through its temporary URI grant; never
  request broad contacts access merely for convenience.
- If no complete valid birthday is available, ask for one. Never invent a
  year, and create nothing if the user cancels.
- Put all copy in Android string resources with `de-DE` as the default and pin
  the app to German to match the iOS behavior on non-German devices.

### 6. Platform integrations

- Sharing: Android Sharesheet using `ACTION_SEND`, `text/plain`, and
  `Intent.createChooser`.
- Calendar: permission-free `ACTION_INSERT` against
  `CalendarContract.Events.CONTENT_URI`.
- Contacts: Activity Result contact picker / `ACTION_PICK` with temporary URI
  access and a graceful manual-date fallback.
- Widgets: one stable Glance widget type with responsive size handling and
  per-instance configuration. Each instance can select a fixed person or
  explicitly follow the app's current person. Include generated/static
  previews and refresh near meaningful rollover boundaries.

Android calendar applications do not reliably report that an event was saved.
Preserve privacy and avoid calendar permissions. Persist an action record when
the calendar handoff is launched, but use an Android-specific neutral state
and never represent that record as confirmation that the event was saved.

## Visual strategy

The Android edition should preserve Erdentage's distinctive design, while
system-owned interactions remain native Android experiences.

- Copy the exact adaptive light/dark RGB values from the current Swift
  `PersonTint` source.
- Reproduce the person gradients, overview sky, cloud layout, visual hierarchy,
  spacing, and responsive behavior.
- Use Android's system sans typography for phase one. Do not redistribute
  Apple's San Francisco font or add an unnecessary bundled font dependency.
- Use Material Symbols or project-owned libre vectors instead of SF Symbols.
- Derive an Android adaptive/monochrome icon from the owner-approved SVG
  masters in the iOS repository; do not copy the Icon Composer bundle directly.
- Store asset provenance and license information for F-Droid review.
- Use native Android date/time pickers, switches, contact picker, calendar UI,
  Sharesheet, accessibility semantics, and back behavior.

Major Android surfaces will be run visibly in the emulator for owner
screenshots and feedback. Compare them manually with the iOS reference
screenshots for product parity, while allowing expected differences in system
bars, fonts, and native controls. The owner waived a separate automated
screenshot harness on 2026-08-16.

## Architecture outline

Begin with one application module; widgets can live in the same APK. Add a
module only for a real deployment boundary such as a later Wear app.

```text
de.eigenraum.erdentage
├── app/          # application, Activity, app container, navigation
├── data/         # serializable models, DataStore, repository
├── domain/       # Android-free day-count and milestone logic
├── ui/           # theme and major Compose surfaces
├── platform/     # contacts, sharing, and calendar adapters
└── widget/       # Glance providers, configuration, and state
```

The repository is the sole persisted-state interface. ViewModels expose
immutable state. Composables render that state and emit actions; they do not
query DataStore/content providers or implement milestone math. Use a small
manual app container and fakes at platform boundaries instead of adding a DI
framework preemptively.

## F-Droid and signing plan

Both Erdentage repositories are licensed `Apache-2.0`, published personally
by Thomas Kahle. The Android app started under `GPL-3.0-or-later` and was
relicensed on 2026-11-02, before its first publication, when the iOS
repository received the same license. The Android repository includes the
canonical license text and a `Copyright 2026 Thomas Kahle` notice. Official F-Droid inclusion requires
a FLOSS license for the application, dependencies, and distributable assets.

Distribution rules:

- No Firebase, Crashlytics, Analytics, AdMob, Play Services, proprietary SDK,
  tracking, advertising, or opaque binary dependency.
- Use released FLOSS AndroidX/Compose dependencies from trusted repositories.
- Commit the Gradle wrapper; pin versions; enable dependency verification and
  locking; do not use dynamic dependency versions.
- Keep builds deterministic and buildable from a clean checkout with
  `./gradlew`.
- Generate the Google Play AAB and F-Droid APK from the same source and feature
  set.
- Prepare Fastlane-compatible German listing metadata, screenshots, changelog,
  privacy policy, source URL, and issue tracker.

Choose the permanent signing strategy before the first public artifact. The
preferred goal is one owner-controlled app-signing key across stores: provide
that key to Play App Signing and pursue F-Droid's reproducible upstream-signed
APK process. Secrets and keystores must never enter Git. If F-Droid uses a
different key, users cannot switch stores in place; document that clearly.

The owner created the permanent app-signing identity on 2026-08-22 after a
separate review; its public details are recorded in `docs/signing.md`, while the
private key and password remain outside Git. Password-manager and independent
offline backup confirmation remain required before the first public artifact.
The owner's personal Google Play developer account from 2012 was closed for
inactivity on 2024-04-06 and cannot publish the app. A new personal account will
be required. Under the rules checked on 2026-08-15, it must be verified using a
non-rooted physical Android 10+ device, and Erdentage must run a closed test with
at least 12 opted-in testers for 14 continuous days before applying for
production access. Re-check these rules during release preparation. Do not
enroll the signing key in a service without a separate owner-reviewed
procedure.

The owner chose this publication sequence: GitHub, official F-Droid, obtain a
Pixel 6a or similar physical test device, create the new Google developer
account, then publish through Google Play. `docs/release.md` records the detailed
gates. GitHub under the owner's `tom111` account is the selected upstream host,
but the public repository, issue tracker, privacy-policy hosting, and F-Droid
submission are not established yet. Continue to honor the FLOSS,
reproducibility, and no-Play-Services rules so every channel can ship the same
app and signing identity.

## Emulator and tool setup plan

Current machine:

- Apple Silicon (`arm64`).
- macOS 26.5.2 at the time of inspection.
- Android Studio Quail 3 Patch 1 (`2026.1.3`), Temurin JDK 17, Android SDK
  Platform/Build Tools 36, platform tools, and emulator are installed.
- ARM64 AVDs for the full matrix below are created and documented in
  [`docs/emulators.md`](docs/emulators.md).

Minimum emulator matrix:

| Purpose | Virtual device |
| --- | --- |
| Main development | API 36 resizable phone |
| F-Droid/no-Google validation | API 36 AOSP image without Play Services |
| Compatibility floor | API 26 phone |
| Large screen | API 36 tablet/resizable device |
| Store-oriented smoke test | API 36 Google Play image, without depending on it at runtime |

Test portrait/landscape, resizing and split-screen, light/dark, gesture and
three-button navigation, phone/tablet widths, normal and enlarged fonts,
German and English system locales, Europe/Rome and another DST-observing time
zone, leap days, rollover times, long names, empty state, multiple people, and
the 20-person limit.

Emulators are the primary development environment. By owner decision, GitHub
and official F-Droid publication come before buying the physical Android test
device. After F-Droid and before Google Play production, test OEM launcher
widgets, process death, contact/calendar handoff, backup/restore, and background
refresh on the physical device. Google Play's pre-launch report supplements but
does not replace the AOSP no-Google run.

The owner currently has no Android phone. Development and the GitHub/F-Droid
release work therefore proceed on the completed emulator matrix. A Pixel 6a or
similar device is planned afterward, and its smoke test is mandatory before the
later Google Play release.

## Implementation record

Steps 1-11 are complete. Step 12 is complete for source preparation, the
documented emulator matrix, and creation of the permanent signing identity;
release signing, store workflows, and independent-device verification remain.
Step 13 remains deferred.

1. Lift the planning hold in `AGENTS.md`.
2. Create the initial Git commit, add the owner-chosen license, and scaffold
   the smallest Kotlin/Compose project with deterministic Gradle configuration.
3. Add CI/build verification and the documented emulator definitions.
4. Port the data model, clock/zone abstraction, milestone/day-count logic, and
   translated Swift tests.
5. Add DataStore, repository, schema-version, corruption, sentinel, retention,
   and backfill tests.
6. Establish the theme, exact palette, typography, edge-to-edge insets, sample
   states, previews, and emulator visual-review workflow.
7. Build the empty state, person pager, counter, and milestone list.
8. Build settings and people management, including selected-contact import.
9. Build the custom adaptive “Demnächst” cloud canvas.
10. Add Sharesheet and calendar integrations behind testable adapters.
11. Add configurable responsive Glance widgets and rollover refresh behavior.
12. Complete accessibility, emulator matrix, no-Google validation, store
    metadata, privacy declarations, signing, reproducible builds, and release
    checks.
13. Consider a separately approved Wear OS phase.

Expected verification tasks once they exist include:

```sh
./gradlew test lint assembleDebug
./gradlew connectedDebugAndroidTest
./gradlew assembleRelease bundleRelease
```

Do not claim a task or level of verification until the checked-in build
actually defines and passes it.

## Phase two: iOS 1.4 parity

Planned and approved by the owner on 2026-11-02 and implemented on 2026-11-03
(all six steps; the ledger rows and the verification entry are in
[`docs/parity.md`](docs/parity.md)). The steps below are kept as the record of
what was ported and which rules were kept.

### Reference and scope

- iOS reference: tag `v1.4.2-build19`, commit
  `089cf5a960f4040d03a78cc052738479d4416bc1`. Pin this commit for the whole
  phase so the target does not move; later iOS work is a separate decision.
- The delta since the phase-one baseline `4f3ddc2` is 50 iOS commits, about
  1,800 lines of production Swift and 49 new Swift tests (99 → 148 in
  `MilestoneMathTests.swift`).
- In scope:
  - the “Zusammen” page with its three Wechselwirkungs-Kategorien
    (`.doppelt`, `.zusammenSoAlt`, `.jahrhundert`) and the undated
    “Konstellationen” section;
  - the family-level row budget per category;
  - share and calendar actions on Zusammen rows;
  - the Geburtstags-Zufälle with the per-person „Geburtstag erwähnen“ switch;
  - three small 1.4/1.4.1 UI changes (step 6).
- Already at parity, no work: the written-out year in the „Demnächst“ clouds
  (Android formats `d. MMM uuuu`) and civil-date milestone keys (Android's
  `DayCountLogic.milestoneKey` already spells `target-yyyymmdd[-hhmm]`, like
  iOS's `SharedDefaults.civilDayKey`).
- Not ported, because they are iOS-only: the 1.4.1 pager rewrite for the
  iPhone Duo / iOS 27 and the 1.4.2 pager fix for iOS 17/18.
- Out of scope: iOS 1.5 (internationalisation, π and e days), which has not
  started on iOS, and Wear OS, which stays deferred.

The iOS `AGENTS.md` and `TODO.md` (sections *Wechselwirkungen*,
*Geburtstags-Zufälle*, *Civil dates* and *Backlog from the 1.4 release audit*)
are the specification. Most of the rules there are recorded decisions, not
accidents of implementation; port them as stated.

### 1. Prerequisites

- Make sure a JDK 17 and the Android SDK are available on the machine doing the
  work; `docs/emulators.md` lists the emulator matrix.
- Done 2026-11-02: the pending working-tree changes (the `Person` companion fix
  and the 2026-08-22/23 ledger entries) are committed as `a94a230`.
- Settle the open owner decision from `docs/code-review-2026-08-20.md` on
  newer-schema and malformed records before step 2 changes the schema.

### 2. Data model and page index (no visible UI)

Every later step depends on this one.

- Add `Person.mentionsBirthday: Boolean = true`. A missing field means on, as
  on iOS: nobody opted out of something they had not seen.
- Add family-level state to `PersistedAppState`:
  - the row budget per Wechselwirkungs-Kategorie. A missing value means the
    iOS default (`TogetherKind.defaultLimit`), and zero switches the category
    off; the count is the switch, so there is no separate toggle.
  - the „already in the calendar“ records for Zusammen rows, reusing
    `AddedMilestoneRecord`, its cap and its pruning, because a Zusammen row
    has no owning person to store them on.
- Add a second trailing page at `people.size + 1`, present only from two people
  onward. Introduce a single interpreter of the stored index (iOS:
  `SharedDefaults.pageKind` and `lastPageIndex`) and route every site through
  it:
  - `ErdentageLimits.normalizeCurrentPersonIndex`, which today only preserves
    `index <= count`;
  - the `wasOverview` checks in `PeopleRepository` for move and delete; a
    delete that leaves fewer than two people degrades Zusammen to „Demnächst“;
  - the pager keys and the page indicator;
  - `WidgetSelection`: a follow-current widget treats the Zusammen index like
    „Demnächst“ and falls back to the first person.
- Tests: the sentinel round trips through save, reorder, delete and restore,
  plus the defaults for blobs written before phase two.

### 3. Geburtstags-Zufälle

- Port `BirthdayCoincidence` and the shared name helper
  (`PersonName.ownerLast`, which puts „ich“ last in enumerations). Keep iOS's
  four matching rules:
  - `daysOffEarth` is ignored;
  - the time of day is ignored;
  - stored components match exactly, so 29 February matches only in leap
    years;
  - one ordinal or none: with more than one match, drop the ordinals.
- German grammar: the owner's possessive declines („an meinem Geburtstag“ but
  „Es ist mein Geburtstag“), and a name ending in an s-sound takes a bare
  apostrophe („Lars’ Geburtstag“).
- The switch belongs to the person whose birthday would be named, not to the
  row doing the naming, and it only gates the note; it never adds, removes or
  reorders a row.
- Surfaces: the personal milestone rows (`caption`), the „Demnächst“ clouds
  (`short`), the share texts (`sentence`), and the switch in the person
  editor.
- This is a complete feature on its own. It comes before Zusammen because the
  Zusammen rows attach the same notes.

### 4. Zusammen calculations (domain only)

Port iOS `TogetherLogic.swift` as Android-free Kotlin in `domain/`, close to
line for line. Keep the rules iOS `AGENTS.md` marks as load-bearing:

- All arithmetic runs on date-only day indices with `daysOffEarth` folded into
  each anchor. Each relation is a strictly monotone line, so every pair or
  subset has exactly one crossing day; rows name the target, not the current
  sum.
- Pick the century multiple with a ceiling:
  `k = max(1, ceilDiv(currentSum, 36_525))`.
- Groups have at most three people (`cappedSubsetSize = 3`) at every family
  size, up to the 20-person maximum. Do not reintroduce iOS's removed fallback
  to pairs above twelve people.
- Rank before phrasing: build cheap `(day, indices)` candidates, sort, cut to
  each category's limit, and only then render German sentences and IDs.
- Event IDs come from the sorted participant UUIDs, so reordering people does
  not mint a new ID for an unchanged relation.
- „Already added“ keys use the civil date (`yyyymmdd`), never a day index.
- The Konstellations-Karte drops its second clause when the younger person's
  `daysOffEarth` is non-zero, because the identity only holds without it.
- Phrasing details from the 1.4 audit, for example „1 Tag auseinander“, not
  „1 Tage auseinander“, and no „genau“ in the `.zusammenSoAlt` page hint.

`LocalDate.toEpochDay()` is a suitable day index: unlike iOS's
`startOfDay(Date(0))` it does not depend on the time zone. It must still never
be persisted.

Verification:

- Add `shared-fixtures/together-math.json`, generated from iOS at the pinned
  commit, with crossing dates, participants, and the **exact German sentences**
  for representative families. The phrasing is the largest drift risk, so the
  fixture pins it to the character.
- Translate the 49 new Swift tests.
- Compute off the main thread, as milestones already are, and cache by people,
  current day, limits and time-zone ID. Measure at 20 people on the API 26
  emulator.

### 5. The Zusammen screen

- Pager page with page-specific controls: Zusammen gets its own settings
  button, and „Demnächst“ keeps none.
- One section per category, rarest first and chronological within each; the
  „Konstellationen“ section doubles as the empty state.
- Each row has a share action and a calendar handoff (`ACTION_INSERT`, all-day;
  the record means the handoff was launched, as for milestones), with the green
  check next to the date.
- TalkBack reads headline, caption, then the relative and absolute date, in
  visual order (iOS fixed the VoiceOver equivalent in the 1.4 audit).
- A family-level budget dialog: one count per category, each with its
  explanation; zero switches the category off.
- Visual checks on phone, tablet, landscape and 1.3× font, light and dark, on
  API 26, 36 and 37. Re-check the open Android 17 tablet test from
  `docs/parity.md` at the same time.

### 6. Small UI changes and release material

- „Person löschen“ in the person editor as well as in the people list (iOS
  `67a0f88`).
- Move the About footer (project line, version, mail, Mastodon) from the
  person editor into the people list (iOS `35841ef`).
- Change the Mastodon link to `https://mathe.social/@tomkalei` in the app and
  in `docs/privacy-policy-de.md` (iOS `2317b43`).
- Checks that are probably already fine and need only a test or a look:
  - two actions per row in the people list (Android already offers edit and
    delete per row);
  - a birth time inside a DST gap, which `java.time` moves forward, as iOS has
    since 1.4;
  - cloud column spacing on tablets, which overlapped on iOS before `2b66dde`.
- Release material:
  - the privacy policy and `docs/play-data-safety.md` gain the family-level
    data (row budgets, Zusammen calendar records), still stored only locally;
  - the F-Droid description gains Zusammen;
  - new screenshots;
  - ledger rows in `docs/parity.md` referencing `089cf5a`.

### Open owner decisions

1. **Release order.** Recommended: finish phase two before the first public
   Android release. Android has no users yet, and launching at the 1.3 level
   would mean redoing screenshots, store text and the privacy policy within
   weeks. Releasing first only pays off if an early F-Droid listing matters.
2. **Version name.** Decided 2026-11-02: version numbers match the feature
   level on both platforms (see *Parity with the iOS app* in `AGENTS.md`). The
   first Android release is therefore 1.3 or 1.4, depending on decision 1.
3. **Fixture cross-check.** Whether the iOS repository gets a small test that
   also checks `shared-fixtures/together-math.json`, so neither implementation
   can drift without the other noticing. The fixture currently comes from
   `shared-fixtures/tools/generate-together-math.py`, an independent
   re-implementation written because the Android machine has no Swift
   toolchain; an iOS-generated file should replace it.
4. **Store screenshots.** Phase two changed the first-release screenshot set
   (five pages with the three-person seed, the Zusammen page); they remain an
   owner artifact.

## Decisions approved before starting

The owner has approved:

1. Kotlin and Jetpack Compose.
2. Minimum API 26 and compile/target API 36.
3. Phone + tablet + widgets for phase one, with Wear OS deferred.
4. `Apache-2.0` (originally `GPL-3.0-or-later`, relicensed 2026-11-02), held
   personally by Thomas Kahle.
5. Permanent application ID `de.eigenraum.erdentage`, changed to
   `ch.firsching.erdentage` on 2026-11-03 before publication.
6. Permission-free calendar handoff with an action record but no unreliable
   “saved” claim.
7. Installation of Android Studio, SDK components, and ARM64 emulator images,
   and the start of implementation.
8. Phase two, approved 2026-11-02: port the shipped iOS 1.4 features
   („Zusammen“, Geburtstags-Zufälle, and the small UI changes up to iOS 1.4.2).

The old Google Play developer account is confirmed closed, but replacing it is
release preparation and does not block implementation. The planning hold was
lifted on 2026-08-15 when the project skeleton was created.

## Current reference links

- Compose: <https://developer.android.com/compose>
- Architecture recommendations:
  <https://developer.android.com/topic/architecture/recommendations>
- DataStore: <https://developer.android.com/topic/libraries/architecture/datastore>
- Adaptive Compose apps:
  <https://developer.android.com/develop/ui/compose/build-adaptive-apps>
- Resizable emulator:
  <https://developer.android.com/studio/run/resizable-emulator>
- Glance widgets: <https://developer.android.com/develop/ui/compose/glance>
- Google Play target API requirements:
  <https://support.google.com/googleplay/android-developer/answer/11926878>
- Google Play app signing:
  <https://developer.android.com/studio/publish/app-signing>
- F-Droid inclusion policy: <https://f-droid.org/docs/Inclusion_Policy/>
- F-Droid reproducible builds:
  <https://f-droid.org/docs/Reproducible_Builds/>
