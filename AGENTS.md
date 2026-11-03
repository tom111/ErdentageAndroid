# AGENTS.md

Guidance for coding agents working on the Android port of **Erdentage**.

The product and implementation roadmap is maintained separately in
[`PLAN.md`](PLAN.md). This file contains durable operating rules and parity
contracts; `PLAN.md` records proposed choices, phases, and owner decisions.

## Implementation status

The repository owner lifted the planning hold on 2026-08-15. Phase-one feature
implementation was completed on 2026-08-16, including the documented Android
emulator matrix. Phase two (iOS 1.4 parity) was implemented on 2026-11-03. Normal implementation, testing, dependency downloads, and
emulator work described by this file and `PLAN.md` are authorized. Signing
operations, store-account actions, publication assets, and any expansion of
product scope still require their documented approvals.

## Mission

Build a native Android edition of the existing German family app Erdentage.
The Android app should be recognizably the same product as the SwiftUI app:
the same data model, day-count and milestone semantics, information
architecture, colors, visual hierarchy, playful “Demnächst” page, privacy
posture, and user-facing German copy. Translate platform integrations into
their Android equivalents instead of mechanically transliterating Swift.

The intended distribution channels are Google Play and F-Droid (and
potentially other free-software Android stores). The app must therefore be
useful without Google Play Services and buildable entirely from published
source.

## Parity with the iOS app

| App | Repository | Maintainer |
| --- | --- | --- |
| iOS, iPadOS, watchOS | https://github.com/tom111/ErdentageiOS | Thomas Kahle |
| Android | https://github.com/tom111/ErdentageAndroid | Moritz Firsching |

Clone both repositories side by side in the same parent folder, so that
`../ErdentageiOS` resolves from here and `../ErdentageAndroid` from there.

Both apps are meant to stay at feature parity. The rules, identical in both
repositories' `AGENTS.md`:

- **One product, two apps.** A feature is defined by whichever app ships it
  first; the other app ports it. Shipped behavior is the reference. A feature
  still in development on one side is not final and is not ported yet.
- **Every release with new or changed behavior opens a porting issue** in the
  other repository, labelled `parity`, titled like „Port iOS 1.4: Zusammen“.
  It links the release tag and the commits or docs that specify the behavior.
  A release that only adapts to its own platform (an OS fix, a new device)
  needs no issue.
- **Version numbers match the feature level.** The same `x.y` on both
  platforms means the same features. Patch numbers (1.4.1, 1.4.2) are free for
  platform-only releases.
- **Changing shared behavior differently needs agreement first.** That covers
  the milestone math, the Wechselwirkungen, the German wording and the stored
  data. Discuss it in an issue in the repository that would deviate.
- **Cross-repository references:** in issues, pull requests and commit
  messages, write `tom111/ErdentageiOS#12` or `tom111/ErdentageiOS@089cf5a`
  (and the same with `ErdentageAndroid`); GitHub links both directions. In
  checked-in files, use full URLs.
- **Where parity is recorded:** Android keeps the per-area ledger in
  `docs/parity.md`. The shared test fixtures live in
  `ErdentageAndroid/shared-fixtures/` and are generated from the app that
  defined the behavior.

For this repository that means: work through the open `parity` issues, and
when an Android release ships behavior iOS does not have yet, open the porting
issue in `tom111/ErdentageiOS`. Everything iOS shipped up to 1.4.2 is ported
under the phases in `PLAN.md`.

## Live source of truth

For everything iOS has shipped, the reference implementation is the iOS
repository, https://github.com/tom111/ErdentageiOS, cloned next to this one as
`../ErdentageiOS`:

- Initial audit baseline: commit
  `4f3ddc22f0b49d14ecf56d7af382ece0a3ca5f99`, branch `main`, clean worktree,
  iOS version 1.3 build 16, inspected on 2026-07-22

The iOS app is being developed in parallel. The commit above is historical
context, not a permanently frozen specification. At the start of every parity
task:

1. Read this file and the iOS repository's `AGENTS.md` completely.
2. Record `git -C ../ErdentageiOS rev-parse HEAD` in the
   task notes or parity ledger.
3. Inspect the current Swift source and tests for the feature being ported.
4. Never modify the iOS repository unless the user explicitly requests it.

When prose and code disagree, current tested iOS behavior wins. Ask before
copying an apparent iOS bug. The Android code must not import, execute, or
depend on Swift at runtime.

Useful visual references live in:

- `AppStore/Screenshots-1.3/`
- `Erdentage/PersonPageView.swift`
- `Erdentage/OverviewPageView.swift`
- `Erdentage/SettingsView.swift`
- `Erdentage/ManagePeopleView.swift`
- `Erdentage/MilestoneRow.swift`
- `ErdentageWidget/ErdentageWidget.swift`

The pure behavior is primarily defined by:

- `Erdentage/SharedDefaults.swift`
- `Erdentage/DayCountLogic.swift`
- `Erdentage/MilestoneLogic.swift`
- `ErdentageTests/MilestoneMathTests.swift`

## Approved technical decisions

These decisions govern the project unless the owner explicitly changes them.

| Area | Decision |
| --- | --- |
| Language | Kotlin only for production Android code |
| UI | Jetpack Compose with Material 3 primitives and a custom Erdentage theme |
| App shape | Single-activity, edge-to-edge, Compose Navigation, adaptive layouts |
| Architecture | Unidirectional data flow; immutable UI state; `ViewModel`, `StateFlow`, coroutines; repository-backed data layer; pure domain logic |
| Persistence | Typed JSON `DataStore` using `kotlinx.serialization`; no Room unless the model later becomes relational |
| Widgets | Jetpack Glance, stable release only, with responsive home-screen layouts and per-instance person configuration |
| Build | Gradle Kotlin DSL, Gradle wrapper, version catalog, JDK 17, pinned dependency versions |
| SDK | `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26` |
| App ID | `ch.firsching.erdentage` (lowercase and permanent once published; changed from `de.eigenraum.erdentage` on 2026-11-03 by the Android maintainer, unpublished; the code namespace stays `de.eigenraum.erdentage`) |
| Tests | JUnit pure-domain tests, repository tests, Compose UI/semantics tests, focused rendered-pixel checks, and emulator instrumentation |
| Dependency injection | A small manual `AppContainer`; do not add Hilt/Koin for this app unless complexity proves it necessary |

Why this stack:

- Compose is Android's recommended native UI toolkit and maps naturally from
  SwiftUI's declarative state model without rewriting the already-working iOS
  app.
- Kotlin keeps platform APIs, widgets, lifecycle, accessibility, and adaptive
  layouts straightforward.
- Flutter, React Native, and Kotlin Multiplatform would add another runtime or
  a cross-platform migration while leaving the existing SwiftUI app in place.
  There is currently too little shared code to justify that complexity.
- API 26 (Android 8.0) is a reasonable compatibility floor for a new offline
  app, supports in-app widget pinning, and remains easy to emulate. Raising or
  lowering it is a product decision, not an incidental coding choice.
- Target API 36 anticipates Google Play's requirement for new apps and updates
  beginning 2026-08-31. Re-check store rules immediately before release.

Use the current stable Android Studio, Android Gradle Plugin, Kotlin, Compose
BOM, AndroidX, DataStore, and Glance versions that are mutually compatible at
the moment scaffolding begins. Do not use alpha/beta/RC libraries without a
documented feature need and owner approval. Never use dynamic versions such
as `+` or version ranges.

## Product scope

### Phase-one scope

Ship phone and tablet support plus Android home-screen widgets first. Make the
normal widget compatible with Android lock-screen widget hosts where the OS
and OEM expose them; Android does not have a direct equivalent of every iOS
accessory widget family.

Defer a Wear OS companion and complications to a later phase. The iOS Watch
app depends on paired-device synchronization, while the usual Wear OS Data
Layer introduces a Google Play Services dependency that conflicts with the
F-Droid-first dependency policy. Do not add a Wear module or proprietary
wearable dependency until the owner explicitly chooses a distribution and
sync strategy.

### In-scope iOS behavior

Phase-one parity targets the shipped iOS 1.3 feature set. Phase two, approved
on 2026-11-02 and implemented on 2026-11-03, extends it to the shipped iOS 1.4
features at tag `v1.4.2-build19`: the “Zusammen” page (`TogetherPageView`, a
second trailing page at `people.size + 1` from two people onward), its
family-level row budgets, and the Geburtstags-Zufälle. The steps and the rules
to keep are in `PLAN.md` under *Phase two: iOS 1.4 parity*. On Android the
Wechselwirkungs-Arithmetik and its German phrasing live in
`domain/TogetherLogic.kt` (phrasing deliberately not in string resources, so
`shared-fixtures/together-math.json` can pin it), the page index is read only
through `ErdentageLimits.pageKind`, and the persisted schema is 2.

- Up to 20 tracked people.
- A horizontal pager with one page per person and a trailing “Demnächst” page.
- Per-person name, civil birth date, optional time, days outside Earth, tint,
  enabled milestone kinds, and calendar-action history.
- Add, delete, reorder, select, and import one chosen contact.
- Current count, residual hours/minutes when the time is known, current-day
  milestone labels, and upcoming milestones.
- A merged chronological family overview capped at 30 rows.
- Text sharing through the system Sharesheet.
- Calendar handoff through the system calendar app.
- Light/dark appearance, portrait/landscape layouts, phone/tablet layouts,
  accessibility, and German-only UI.
- Responsive, configurable home-screen widgets, including a selected-person
  variant and the follow-current-person behavior where Android can represent
  it clearly.

### Explicit non-goals unless the owner expands scope

- No account, server, cloud sync, analytics, ads, tracking, purchases, or
  subscription.
- No network dependency and normally no `INTERNET` permission.
- No attempt to share local data directly with the iOS installation.
- No notification feature merely because it appears in the iOS TODO; it is not
  shipped behavior.
- No Wear OS, Android TV, Automotive, XR, or desktop-specific experience in
  phase one.

The app may participate in Android's OS-managed backup and restore. This is
distinct from an app-owned account or synchronization service. Restore
behavior must preserve schema, UUID, selection-sentinel, and milestone
semantics and be tested for process and device restoration where practical.

## Behavioral contracts to preserve

### Person and app state

Use stable UUID strings. Persist civil date components, not an epoch timestamp:
year/month/day and optional hour/minute must reconstruct in the user's current
time zone with the proleptic Gregorian calendar. Travelling must not silently
shift a birthday to a neighboring date. Inject clock and zone dependencies
into domain logic so time behavior is deterministic in tests.

The current page index permits the trailing overview sentinel
`index == people.size`. Preserve that sentinel during save, reorder, delete,
widget fallback, and any future sync. Do not clamp it to the last person.

An empty or whitespace-only name, and the literal name `Ich`, represent the
device owner. Display `Ich` and use first-person German share wording. Named
people use third-person wording. Calendar titles omit an owner prefix but keep
a named person's prefix.

Keep these defensive limits consistent on every surface:

- `maxPeople = 20`
- `daysOffEarth = 0..100_000`
- `maxDisplayedDays = 600_000_000`
- calendar-action history cap = 200, while retaining currently visible keys

Android has no legacy iOS `UserDefaults` installation to migrate. Start with a
versioned Android schema representing the current `Person` model; do not port
obsolete iOS 1.0 keys just for superficial similarity. Preserve the iOS
versioned *delta* rule for future milestone backfills: a new release adds only
kinds introduced after the user's stored marker so a manual opt-out stays off.

### Day and time behavior

- Date-only people roll over at local midnight.
- Time-known people roll over at the stored local birth hour/minute and show
  residual hours and minutes.
- Main UI refreshes only at the next meaningful boundary while visible; avoid
  a perpetual high-frequency timer.
- Widget data is recalculated from current person data and scheduled/refreshed
  near the next meaningful rollover within Android's background limits.
- Validate leap days, invalid component combinations, daylight-saving
  transitions, future dates, negative intervals, and time-zone changes.
- Format numbers and dates with `de-DE`; day counts use German grouping, e.g.
  `16.796`.

### Milestones

Port the algorithms and their tests, not just the labels. The catalog is:

- Tausender
- Zehntausender
- Primzahlen
- optional “Nur Zwillinge” prime filter
- Palindrome
- Quadratzahlen
- Zweierpotenzen
- Ziffernfolgen: ascending, descending, or repeated digits
- Catalan-Zahlen through C(18)
- Fibonacci-Zahlen using the sequence `1, 2, 3, 5, ...`

Fresh people enable every top-level kind except twin primes. The UI presents
ordinary primes and twin-only primes as mutually exclusive even though the
engine tolerates both. Collapse candidates with the same target into one row
with all captions. Preserve strict “next” semantics and current-day detection.
Keep the optimized generation strategies from `MilestoneLogic.swift`; do not
replace them with scans that can stall near the display clamp.

Before building UI around the port, translate the Swift test corpus into
Kotlin and, where practical, create shared language-neutral fixture JSON for
the Swift and Kotlin suites. Test boundaries, very large values, invalid data,
same-target collapse, ordering, backfill deltas, and retention.

## Visual contract

“Identical” means shared product identity and layout behavior, not illegally
redistributing Apple-only assets or forcing iOS system chrome onto Android.
Distinctive Erdentage surfaces should be visually close; system pickers,
Sharesheet, calendar, contact picker, switches, and accessibility behavior
should remain native Android experiences.

### Person pages

- Paint edge-to-edge behind status/navigation bars while applying safe insets
  to interactive content.
- Use the exact light/dark RGB palette from `PersonTint` in
  `MilestoneLogic.swift`; white is a deliberate user-selected no-tint option
  and is never assigned automatically.
- Match the top-to-center tint-to-background gradient.
- Keep people management at top-left and share/settings at top-right with
  minimum 48 dp touch targets.
- Center “Erdentage”, then the person's display name, then a very large bold
  rounded/monospaced day count. Do not bundle Apple's San Francisco font;
  use Android's system rounded sans or a documented libre font.
- Keep the “Heute: …” line and the scrollable upcoming list with uniform row
  rhythm and one overflow action target per row.
- Portrait stacks count and milestones vertically. Compact-height landscape
  uses equal-width side-by-side columns. Wide tablets must not merely stretch
  text to fill the screen; compare against the current iPad screenshots.
- Keep a visible pager indicator without colliding with gesture navigation.

Do not copy SF Symbols; their license is Apple-platform-specific. Use the
closest Material Symbols or small project-owned vector artwork. Avoid the
large Material Icons Extended dependency when importing a handful of vectors
is sufficient.

### “Demnächst” overview

This is brand-defining, not a generic list. Reproduce the light sky-to-cream
and dark midnight-to-dusk gradients, alternating cloud positions, deterministic
seeded wobble, per-person adaptive fills/outlines, glow in dark mode, compact
phone landscape sizing, tablet sizing, chronological order, and 30-row cap.
Custom Compose `Canvas`/`Shape` code is appropriate here. Wobble must remain
stable across recomposition and clock ticks.

### Settings and people management

Match the section order and wording from the Swift source: name; birth date;
time-known toggle and optional time; astronaut adjustment; tint swatches;
milestone picker and explanations; about/feedback. Keep newly added people on
the editor path. Reordering must keep the same selected UUID, including while
the overview sentinel is selected.

Store every German string in Android string resources from day one even though
only German ships. Declare `de-DE` as the unqualified/default resource locale
and pin the app locale to German to match iOS behavior on an English device.
Do not scatter hardcoded strings through composables.

### Icons and artwork

The iOS SVG masters are under `IconComposer/`, with the flat master at
`IconComposer/Icon-Flat.svg`. When implementation starts, derive a proper
Android adaptive icon (foreground, background, monochrome where applicable)
from owner-approved source masters. Do not copy the Apple Icon Composer bundle
as though Android could consume it. Document asset provenance and license.

## Android translations of Apple integrations

| Apple implementation | Android implementation and constraint |
| --- | --- |
| SwiftUI page `TabView` | Compose `HorizontalPager` with stable page keys and explicit overview sentinel |
| `TimelineView` | Lifecycle-aware state plus a delay to the next minute/midnight/birth-time boundary |
| `UserDefaults` app group | Typed app-private DataStore; expose snapshots to widgets through a repository safe for widget execution |
| `ShareLink` | `ACTION_SEND` with `text/plain` through `Intent.createChooser` |
| `CNContactPickerViewController` | Activity Result contact picker / `ACTION_PICK`, using its temporary URI grant; do not request broad `READ_CONTACTS` |
| EventKit editor | `ACTION_INSERT` on `CalendarContract.Events.CONTENT_URI`; do not request calendar read/write permissions |
| WidgetKit | Stable Jetpack Glance plus an Android widget configuration activity |
| SF Symbols | Material Symbols or project-owned libre vectors |
| WatchConnectivity | Deferred; do not silently add Play Services |

Contact import must read only the selected contact. Prefer a usable name and
birthday, but providers vary: if the birthday or year cannot be read through
the temporary grant, ask the user for it. Never fabricate a year. Cancel must
create nothing. Test the graceful fallback without a contacts app or handler.

Calendar intents deliberately hand control to a calendar app and require no
calendar permission. They do not reliably tell Erdentage whether the user
actually saved the event. Persist that the handoff action was launched, but do
not show a persistent “saved” checkmark or claim that the event was saved.
Handle the absence of a compatible calendar app without crashing.

## Architecture after the hold is lifted

Keep one app module initially; widgets can live in the same APK/module. Add a
module only for a real deployment boundary such as a later Wear app. A sensible
package layout is:

```text
de.eigenraum.erdentage
├── app/          # Activity, application container, top-level navigation
├── data/         # serialized models, DataStore serializer, repository
├── domain/       # pure day-count, milestone, formatting-input logic
├── ui/           # theme and one package/file group per major surface
├── platform/     # contacts, sharing, calendar adapters
└── widget/       # Glance providers, configuration, widget state
```

The repository is the single source of persisted app state. ViewModels expose
immutable state and accept explicit user actions. Composables render state and
send actions; they do not read DataStore, query content providers, or contain
milestone algorithms. Keep domain code Android-free wherever possible so it
runs as fast host-side unit tests.

Avoid a monolithic screen file. Mirror the iOS split by major surface while
using idiomatic Android names. Use interfaces and fakes at platform boundaries,
not abstraction layers around every Kotlin function.

## F-Droid and store constraints

Both Erdentage repositories are licensed `Apache-2.0`, with `Thomas Kahle` as
the personal copyright holder. The Android app started under
`GPL-3.0-or-later` and was relicensed before its first publication, on
2026-11-02, when the iOS repository received the same license. The canonical
license text and a `NOTICE` (`Copyright 2026 Thomas Kahle`) are checked in. Official F-Droid
inclusion requires the complete app and its dependencies to be free software.

Hard rules:

- No Firebase, Crashlytics, Google Analytics, AdMob, Play Services, proprietary
  SDK, tracking library, ad library, or closed binary dependency.
- AndroidX/Compose libraries are acceptable only at released FLOSS versions;
  audit every new dependency and asset license.
- Prefer Maven Central, Google's AndroidX Maven repository, and the Gradle
  Plugin Portal as appropriate. Do not check in opaque JAR/AAR files.
- Commit the Gradle wrapper and pin all versions. Enable dependency
  verification/locking when the project is created.
- Keep release builds deterministic: no timestamps, machine paths, random
  version codes, or secrets embedded in outputs. Build from a clean checkout
  with `./gradlew`, not an IDE-only action.
- Prepare upstream Fastlane-compatible German metadata, screenshots, changelog,
  source URL, issue tracker, and privacy policy when release work begins.
- Produce an AAB for Google Play and an APK for F-Droid from the same source and
  feature set. Do not ship a privacy-reduced “free” flavor and a tracking Play
  flavor.

The owner chose cross-store updates with one owner-controlled app-signing key
and created that identity on 2026-08-22; its public details are recorded in
`docs/signing.md`. Provide that key to Play App Signing only through a separately
approved enrollment procedure and pursue F-Droid's reproducible-build/
upstream-signed APK path. Never put keystores, passwords, service-account files,
or signing properties in Git. If a store ever uses a different key, document
that users cannot switch stores in place.

The owner's personal Google Play developer account from 2012 was closed for
inactivity on 2024-04-06 and cannot publish the app. Google Play publication
will use a new personal developer account. Under the rules checked on
2026-08-15, production access for that account requires verification with a
non-rooted physical Android 10+ device and a closed test with at least 12
opted-in testers for 14 continuous days. Re-check these rules during release
preparation. Account creation and its signing choices remain owner-controlled
external actions.

Before release, re-check Google Play target-SDK rules, data-safety declarations,
content rating, developer identity verification, package registration, signing,
and F-Droid inclusion/reproducibility policies; these rules change over time.

## Emulator-first development and verification

No physical Android phone is currently available. The Mac is Apple Silicon
(`arm64`, macOS 26.5.2). At the initial audit there was no Android Studio,
Android SDK, AVD, usable JDK, `adb`, or emulator installed. Android Studio,
Temurin JDK 17, the SDK, and the required ARM64 emulator images are now
installed.

Keep the ARM64 AVD matrix documented in `docs/emulators.md`. The minimum useful
matrix is:

| Purpose | Suggested virtual device |
| --- | --- |
| Primary development | API 36 resizable phone, AOSP or Google APIs ARM64 image |
| No-Google/F-Droid check | API 36 AOSP image with no Play Services |
| Minimum-supported check | API 26 phone image |
| Large screen | API 36 tablet/resizable device |
| Store-realistic smoke test | API 36 Google Play image, without adding a runtime dependency on it |

Exercise portrait and landscape, compact and wide windows, split-screen/window
resize, light and dark themes, gesture and three-button navigation, font scales
1.0 and at least 1.3, display scaling, German and English device locales,
`Europe/Rome` and another DST-observing time zone, leap days, and local
midnight/birth-time rollovers. Seed multiple people, long names, all milestone
kinds, empty state, and 20-person state.

Use Compose previews for rapid iteration, then run each visible UI milestone
in the emulator for owner screenshots and feedback. Compare major surfaces
manually against the iOS screenshots, accounting for platform
status/navigation bars and fonts rather than demanding meaningless
whole-screen pixel identity. The owner explicitly waived an automated
screenshot harness on 2026-08-16. Add semantics tests for content descriptions,
traversal, touch target sizes, and large text, plus focused rendered-pixel
checks when they protect a concrete visual contract.

Emulators are the normal development target. By explicit owner decision, the
GitHub and official F-Droid releases precede purchase of a physical Android
device. After F-Droid and before Google Play production, obtain at least one
independent physical-device smoke test for OEM launcher widgets,
calendar/contact handoff, process death, backup/restore, and background
rollover behavior. Google Play's pre-launch report can supplement, not replace,
the AOSP no-Google test.

Once a project exists, the normal verification gate should include equivalents
of:

```sh
./gradlew test lint assembleDebug
./gradlew connectedDebugAndroidTest
./gradlew assembleRelease bundleRelease
```

Only list tasks that the checked-in build actually defines. Never claim
emulator or release verification if only host-side tests ran.

## Implementation and release sequence

Steps 1-8 are complete. The source preparation, emulator matrix, and permanent
signing identity portions of step 9 are complete; signed/reproducible artifacts,
key backup confirmation, and the independent-device/store gates remain. Step 10
is deferred unless separately approved.

1. Lift the hold; create the initial Git commit; add the chosen license;
   scaffold the minimal Kotlin/Compose project and deterministic CI/build
   configuration.
2. Add the pure data model, clock/zone abstraction, day-count logic, milestone
   logic, and translated Swift test corpus.
3. Add DataStore and repository tests, including corruption/default/backfill
   behavior and the overview sentinel.
4. Establish theme, palette, typography, edge-to-edge insets, sample state,
   Compose previews, and emulator visual-review evidence.
5. Build the person pager and empty state, then settings and people management.
6. Build the custom “Demnächst” cloud canvas and adaptive phone/tablet/
   landscape behavior.
7. Add share, selected-contact import, and calendar handoff behind testable
   adapters.
8. Add responsive, per-instance Glance widgets and rollover/update behavior.
9. Complete accessibility, no-Google AVD testing, store metadata, privacy
   disclosures, signing, reproducible release builds, and release checklists.
10. Consider Wear OS only as a separately approved follow-up.

Maintain `docs/parity.md` for every future parity change. Record the reference
iOS commit, Android status, tests, visual evidence, and any intentional
platform deviation. “Looks similar” is not a completed parity item without
behavior and verification.

## Owner decisions

The owner has approved:

- Kotlin + Jetpack Compose with API 26 minimum and API 36 compile/target.
- Phone + tablet + widgets for phase one, with Wear OS deferred.
- `Apache-2.0` (relicensed from `GPL-3.0-or-later` on 2026-11-02 to match the
  iOS repository), held personally by Thomas Kahle.
- The permanent application ID, originally `de.eigenraum.erdentage`; changed
  to `ch.firsching.erdentage` on 2026-11-03 for the Android maintainer's own
  store listing, before publication.
- Installation of the required Android development tools and the start of
  implementation. The planning hold was lifted with the project skeleton on
  2026-08-15.
- Participate in Android OS-managed backup and restore.
- Use permission-free calendar handoff and record that it was launched, not
  that the event was saved.
- Use one configurable widget type; following the app's current person is an
  option alongside selecting a fixed person.
- Use Android's system sans typography for phase one; do not bundle Apple's
  fonts or an unnecessary third-party font.
- Develop and verify as far as possible with emulators because no physical
  Android phone is currently available.
- Follow the owner-approved publication order: GitHub, official F-Droid, obtain
  a Pixel 6a or similar physical test device, create the new Google developer
  account, then publish through Google Play. The GitHub repository name and all
  external creation/push/store actions still require owner confirmation.
- Use the one owner-controlled signing identity created on 2026-08-22 for
  upstream APKs, reproducible F-Droid delivery, and later Play App Signing;
  keep all private material and credentials outside Git.
- Phase one targeted the shipped iOS 1.3 feature set. On 2026-11-02 the owner
  approved phase two: port the shipped iOS 1.4 features („Zusammen“,
  Geburtstags-Zufälle, small UI changes up to iOS 1.4.2), as planned in
  `PLAN.md`.

## Primary external references

- Android Compose: <https://developer.android.com/compose>
- Android architecture recommendations:
  <https://developer.android.com/topic/architecture/recommendations>
- DataStore: <https://developer.android.com/topic/libraries/architecture/datastore>
- Adaptive Compose apps:
  <https://developer.android.com/develop/ui/compose/build-adaptive-apps>
- Resizable emulator:
  <https://developer.android.com/studio/run/resizable-emulator>
- Glance widgets: <https://developer.android.com/develop/ui/compose/glance>
- Calendar intents:
  <https://developer.android.com/identity/providers/calendar-provider>
- Contact picker intents:
  <https://developer.android.com/guide/components/intents-common#Contacts>
- Google Play target API requirements:
  <https://support.google.com/googleplay/android-developer/answer/11926878>
- Google Play app signing:
  <https://developer.android.com/studio/publish/app-signing>
- F-Droid inclusion policy: <https://f-droid.org/docs/Inclusion_Policy/>
- F-Droid reproducible builds:
  <https://f-droid.org/docs/Reproducible_Builds/>
