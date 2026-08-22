# Code review — Erdentage Android

Date: 2026-08-20
Reviewed commit: `4b974e3` (branch `master`, clean worktree at review start)
Scope: all of `app/src` (~7 000 lines Kotlin), `AndroidManifest.xml`, resources,
Gradle configuration, `AGENTS.md` behavioural contracts, and the test suite.
Baseline: `./gradlew --offline testDebugUnitTest` passes.

No source files were changed while producing this report.

---

## 1. Correctness bugs

### 1.1 The overview cloud jitter is mathematically dead

`ui/overview/OverviewPage.kt:342`

```kotlin
fun jitter(key: String, range: Dp): Dp {
    val positive = fnv1a(key).ushr(11)                          // ≤ 2^53
    val unit = positive.toDouble() / Long.MAX_VALUE.toDouble()  // ÷ 2^63
    return range * ((unit * 2.0 - 1.0).toFloat())
}
```

`ushr(11)` caps the numerator at 2^53, but the divisor is `Long.MAX_VALUE` ≈ 2^63.
`unit` therefore never exceeds 0.00098, and `unit * 2 - 1` is always in
`[-1.0, -0.998]`. Measured factors for `range = 18.dp`:

| key | jitter |
| --- | --- |
| `a-x` | −17.9684 dp |
| `b-x` | −17.96492 dp |
| `c-x` | −17.97132 dp |
| `other-target-x` | −17.9936 dp |
| `zzz-y` | −17.99753 dp |

Every cloud receives essentially the full negative offset, so there is no
per-cloud wobble at all — only a uniform shift left by `jitter` and up by
`jitter / 2`. On a portrait phone `layoutStart` is 0, so every even-index cloud
lands exactly on the `coerceIn` floor of `4.dp` (`OverviewPage.kt:164`) and every
odd-index cloud sits within 0.04 dp of the same x. The result is strict
left/right alternation with zero visible variation, which contradicts the
`AGENTS.md` visual contract ("deterministic seeded wobble", "alternating cloud
positions").

The correct divisor is `(1L shl 53).toDouble()` — exactly what `cloudShape`'s own
`nextUnit()` at `OverviewPage.kt:303` already does correctly.

`OverviewCloudMathTest` passes vacuously: −17.97 satisfies `in -18f..18f`, and
`assertNotEquals(first, other)` holds because the values still differ in the
third decimal place. A regression test should assert that the jitter actually
spans a meaningful fraction of `range` across a set of keys.

### 1.2 Contact import accepts future birthdays

`platform/SelectedContactReader.kt:31` and `MainActivity.kt:57`

`ContactImportLogic.completeDate()` validates calendar validity but not that the
date lies in the past. A provider that returns, say, `2030-01-01` flows straight
into `contactDraft` and bypasses the picker path — which *does* enforce
`datePicker.maxDate = today` (`MainActivity.kt:176`).

The consequence is already documented by
`DayCountLogicTest.futureAndInvalidBirthDatesProduceDefensiveSnapshots`:
`days = 0`, `hasBirthDate = true`. The user gets a person permanently frozen at
0 Erdentage with no signal that anything is wrong. Both hand-entry paths forbid
future dates; only the import path does not.

### 1.3 The `daysOffEarth` field snaps to 0 when cleared

`ui/settings/PersonSettingsDialog.kt:356`

```kotlin
onValueChange = { raw ->
    if (raw.all(Char::isDigit)) {
        onValueChange(raw.toIntOrNull()?.coerceIn(0, MAX_DAYS_OFF_EARTH) ?: 0)
    }
}
```

`"".all(Char::isDigit)` is vacuously `true`, so clearing the field evaluates
`"".toIntOrNull() ?: 0` and immediately re-renders as `"0"`. The user cannot
clear the field to type a fresh number. The same happens for any input longer
than the `Int` range (`toIntOrNull()` → null → 0).

Separately, driving an `OutlinedTextField` from `Int.toString()` resets the
cursor to the end of the text on every keystroke, so editing in the middle of the
number jumps the caret.

### 1.4 Open editor state is lost on rotation

`ui/ErdentageApp.kt:71`

`editingPerson`, `editorCreatesPerson` and `returnToManager` use `remember`,
while `managePeopleVisible` on line 70 correctly uses `rememberSaveable`.

Rotating the device (or surviving process death) while the settings sheet is open
drops the dialog together with every unsaved edit. Because opening the editor
sets `managePeopleVisible = false`, the user is dumped back onto the pager rather
than the manager they came from.

### 1.5 No error path on the DataStore flow

`data/PeopleRepository.kt:37`

```kotlin
override val state: Flow<PersistedAppState> = dataStore.data
    .map(PersistedAppState::normalized)
    .distinctUntilChanged()
```

`dataStore.data` re-throws non-corruption `IOException`s to its collectors, and
there is no `.catch {}`. A single transient read failure permanently cancels the
ViewModel's `init` collector (`app/ErdentageViewModel.kt:35`), leaving the UI
stuck on the blank `Box` at `MainActivity.kt:82` for the remaining life of the
process. The same applies to `WidgetStateObserver`
(`widget/WidgetUpdateScheduler.kt:87`) — a `SupervisorJob` isolates siblings from
each other, it does not restart a failed child.

### 1.6 `updatePerson` is last-write-wins by construction

`app/ErdentageViewModel.kt:79`

```kotlin
repository.updatePerson(person.id) { person }
```

The transform discards its input, so any change written to that person between
dialog-open and save is silently overwritten. `addedMilestones` survives today
only because the settings draft happens to carry it along. Prefer
`updatePerson(id) { stored -> stored.copy(...) }` so the repository's
read-modify-write is actually a merge.

---

## 2. Data loss and robustness

### 2.1 Any schema mismatch silently wipes all data

`data/AppStateSerializer.kt:22` and `data/AppStateDataStore.kt:19`

The guard is `state.schemaVersion != PersistedAppState.CURRENT_SCHEMA_VERSION`,
so it fires on version **2 as readily as on version 0**, and
`ReplaceFileCorruptionHandler` then resets to `PersistedAppState()`.

Installing an older APK over a newer one — a staged Play rollback, a sideload, or
Play/F-Droid version skew — deletes every tracked person with no warning and no
retained copy of the previous file.

A single `Person` carrying a malformed UUID (`domain/Person.kt:32`,
`require(isCanonicalUuid(id))`) triggers the identical wipe through the
`IllegalArgumentException` catch on line 30.

Suggested direction: treat `schemaVersion < CURRENT` as a migration input rather
than corruption, reject `schemaVersion > CURRENT` without destroying the file,
and consider preserving the offending payload alongside the reset state.

### 2.2 Contact birthday probably never resolves in practice

`platform/SelectedContactReader.kt:107`

The temporary URI grant from `ACTION_PICK` covers the returned contact URI, not
the appended `Contacts.Entity.CONTENT_DIRECTORY` (`…/entities`) sub-path.
Querying that path without `READ_CONTACTS` normally raises `SecurityException`,
which `safely` swallows, yielding `null` and always prompting the user for the
date.

Nothing crashes — this is exactly the graceful fallback `AGENTS.md` asks for —
but the accompanying goal ("prefer a usable name **and** birthday") would then
never be met. This deserves one on-device verification against the AOSP
Contacts provider before release.

Related: `ContactImportLogic.parseBirthday`
(`platform/SelectedContactReader.kt:81`) only matches strictly zero-padded
`uuuu-MM-dd` and `--MM-dd`. Providers that store `1980-1-5`, or an ISO value with
a trailing time component, return `null`.

---

## 3. Performance and battery

### 3.1 Every page swipe costs a disk write, widget IPC and a WorkManager reschedule

`ui/ErdentageApp.kt:106` collects `snapshotFlow { pagerState.settledPage }` and
calls `onPageSelected` for each settle. That chain is:

`setCurrentPersonIndex` → `dataStore.updateData` (file write + fsync) → flow
emission → `WidgetStateObserver` (`widget/WidgetUpdateScheduler.kt:88`) →
`ErdentageGlanceWidget().updateAll(...)` on every installed widget **and**
`enqueueUniqueWork(REPLACE)` (WorkManager DB write, potentially a JobScheduler
reschedule).

Swiping through 20 people performs all of that 20 times. Debouncing the page
write, and gating the widget refresh on a change that actually affects widget
content, would remove most of it.

### 3.2 Cloud shapes are re-allocated on every recomposition

`ui/overview/OverviewPage.kt:160`

```kotlin
shape = cloudShape(OverviewCloudMath.fnv1a(id))
```

No `remember`, so up to 30 fresh `GenericShape` instances are built per
recomposition, plus 30 string re-encodings inside `fnv1a`. The changed `Shape`
identity invalidates the outline used by `Modifier.shadow`, `Surface(shape = …)`
and `border` for each cloud. This repeats on every clock tick — every 60 seconds
whenever any person has a known birth time.

### 3.3 All milestone math runs on the main thread, for every person, every tick

`app/ErdentageViewModel.kt:112`

`viewModelScope` defaults to `Dispatchers.Main.immediate`, so
`ErdentageUiState.create` (`app/ErdentageUiState.kt:40`) recomputes
`upcomingMilestones` — trial-division `nextPrime`/`nextTwinPrime`,
`nextDigitSequence`, and the rest — for all 20 people, plus a 30-item sort, on
the UI thread.

This is cheap at realistic day counts, but near `MAX_DISPLAYED_DAYS`
(600 000 000) `nextTwinPrime` alone is hundreds of thousands of divisions per
person.

Line 118 also ticks the *entire* tree once per minute if any single person has a
known time, even when the visible page belongs to a date-only person or is the
overview.

### 3.4 Main-thread I/O

- `MainActivity.kt:56` — `SelectedContactReader(contentResolver).read(uri)`
  performs two cross-process ContentProvider queries inside the activity-result
  callback, on the UI thread.
- `widget/WidgetConfigurationActivity.kt:76` — `WidgetSelectionStore(this)` is
  constructed inside the composition, so `getSharedPreferences` (first-load disk
  read) is re-invoked on every recomposition.

---

## 4. Smaller findings

| Location | Finding |
| --- | --- |
| `platform/GermanLocaleContext.kt:12` | `Locale.setDefault(locale)` inside what reads like a pure context transform, called from three `attachBaseContext` overrides. Mutates process-global state for every library and every implicit-locale `String.format`. |
| `domain/MilestoneMath.kt:168` | `error("No Int digit sequence exists after …")` in a public pure function, for `after >= 1_111_111_111`. Unreachable today because of the 600 M clamp, but unguarded. |
| `widget/ErdentageGlanceWidget.kt:90` | `ErdentageWidgetReceiver` does not override `onEnabled`/`onUpdate`, and `scheduleNext` is otherwise only reached from the configuration activity, the worker, the time receiver, or a state change. A widget restored from backup or surviving an app update has no pending rollover work. |
| `widget/WidgetUpdateScheduler.kt:104` | `WidgetTimeChangeReceiver` creates a new, never-cancelled `CoroutineScope` per broadcast. |
| `ui/settings/PersonSettingsDialog.kt:598` | `TimePickerDialog(..., true)` hardcodes 24-hour mode instead of consulting `DateFormat.is24HourFormat(context)`. |
| `platform/ShareLauncher.kt:18` | No `try`/`catch` around `startActivity`, unlike `launchCalendarHandoff` which handles both `ActivityNotFoundException` and `SecurityException`. |
| `app/build.gradle.kts:34` | `isMinifyEnabled = false` for release. Intentional if it serves F-Droid reproducibility; worth a comment recording that. |

---

## 5. Areas verified as correct

The pure domain layer holds up well under scrutiny:

- `nextPalindrome` — checked the mirror path, the middle-digit carry, the
  all-nines cascade, and the case where the carry would run off the front
  (unreachable, because a leading 9 always mirrors to the maximum).
- `nextPerfectSquare` — the `root..root + 2` window absorbs IEEE-754 error;
  `sqrt` of an exactly representable perfect square is exact below 2^53.
- `nextDigitSequence` — ascending (`start in 1..10 - length`), descending
  (`high in max(1, length - 1)..9`) and repdigit generation are each consistent
  with `isDigitSequence`, and the `Int.MAX_VALUE` guard correctly excludes
  `9876543210`.
- Fibonacci and Catalan bounds — no `Int` overflow within the display clamp;
  `nextCatalan` correctly returns `null` past C(18) rather than fabricating a
  target.
- `elapsedCalendarParts` — handles the spring-forward gap correctly, and
  `DayCountLogicTest.elapsedPartsMatchFoundationAcrossSpringDstGap` covers it.
- `ErdentageLimits.normalizeCurrentPersonIndex` — correctly preserves the
  trailing overview sentinel (`index == people.size`) instead of clamping it to
  the last person, on save, reorder, delete and widget fallback.
- All `stringResource`/`getString` call sites were cross-checked against the
  placeholder counts in `res/values/strings.xml`. No mismatches, no missing
  resources.

---

## 6. Suggested triage order

1. **1.1** cloud jitter — brand-defining surface, one-line fix, needs a real test.
2. **2.1** schema-mismatch data wipe — highest user cost, and hardest to
   apologise for after release.
3. **1.2** future-dated contact import — small validation gap with a silent,
   confusing outcome.
4. **1.4** / **1.3** editor state loss and the astronaut field — visible everyday
   friction.
5. **1.5** / **1.6** DataStore error path and the merge semantics of
   `updatePerson`.
6. **3.1**–**3.4** performance work, ideally before the closed test that Play
   production access requires.
7. **2.2** verify contact-birthday behaviour on a real device before release.

---

## 7. Maintainer disposition

Validated on 2026-08-20 against Android commit `947774d` and current iOS HEAD
`9e52c6913f213f0cd93dc5c69b02b77e64dd9c63`. Findings were reproduced before
changes were made; suggestions were not applied wholesale.

| Finding | Disposition | Result |
| --- | --- | --- |
| 1.1 Cloud jitter | Accepted | Corrected the 53-bit normalization, added a range-spread regression test, and remembered cloud shapes by stable event ID. |
| 1.2 Future contact birthdays | Accepted | Complete dates now require an injected `today` and future dates fall back to the bounded picker. |
| 1.3 Astronaut field | Accepted | The field owns saveable `TextFieldValue` state, may remain empty while editing, preserves the caret, and ignores overflow instead of converting it to zero. |
| 1.4 Editor restoration | Accepted | Editor route flags and the compact serialized draft survive saved-instance restoration. An emulator test covers unsaved edits and returning to the people manager. |
| 1.5 DataStore read errors | Accepted with a safer mechanism | Transient `IOException`s retry with bounded exponential backoff. The repository does not emit a fake empty family state that could be mistaken for data loss. |
| 1.6 Last-write-wins editor | Accepted | Editor-controlled fields merge into the latest stored person; calendar handoff history is retained. |
| 2.1 Schema mismatch wipe | Deferred until the first schema change | The owner rejected a speculative compatibility path as a current release fix: public `versionCode`s must increase monotonically, Android does not normally replace a newer install with an older APK, and the app does not opt into restoring data from a newer app version. Before schema 2 is introduced, add an explicit migration plus tests that preserve older data and reject newer schemas without rewriting the payload. |
| 2.2 Contact entity permission | Main claim rejected; parser concern accepted | Android's provider documentation explicitly grants temporary access to the selected contact and demonstrates appending `Contacts.Entity.CONTENT_DIRECTORY`. No broad contacts permission will be added. Parsing now also accepts non-padded fields and trailing time text. Real-provider smoke coverage remains a release gate. |
| 3.1 Page-swipe work | Partly accepted | Identical selections now short-circuit. Real page changes remain immediately durable because selection is product state and drives follow-current widgets; debounce was not added without profiling because it can lose the last selection. |
| 3.2 Shape allocation | Accepted | Shapes and their hashes are remembered by stable IDs. |
| 3.3 Main-thread milestone math | Partly accepted | UI-state construction now runs on `Dispatchers.Default`. The minute cadence remains while the foreground state contains any timed person because residual minutes are user-visible; further narrowing needs profiling and a page-aware state design. |
| 3.4 Main-thread I/O | Accepted | Contact queries run on `Dispatchers.IO`; widget selection is read once before composition. |
| Global locale mutation | Accepted | The app still wraps each Android context with German resources but no longer changes the process-wide default locale. |
| Digit-sequence terminal error | Not actionable in the supported domain | The call site clamps to 600 million and the failure begins above 1.1 billion. Changing the public result type would add churn without reachable protection. |
| Widget restore/update schedule | Accepted | Receiver updates now enqueue an immediate worker, which renders and schedules the next rollover. |
| Per-broadcast coroutine scope | Rejected as a leak finding | The short-lived scope completes with its single coroutine and the `goAsync()` result is finished in `finally`; no retained owner or repeating job was found. |
| 24-hour picker | Accepted | The native picker now follows the device's 12/24-hour preference. |
| Share failure | Accepted | Missing or blocked share handlers now produce a localized, non-crashing fallback. |
| Release minification | No change | Shrinking is currently disabled deliberately; enabling and validating R8 is release work, not a correctness fix. |

Android references used to resolve 2.2:

- [Content provider access through a contact picker](https://developer.android.com/guide/topics/providers/content-provider-basics)
- [Contacts Provider entity query example](https://developer.android.com/identity/providers/contacts-provider)
- [`ContactsContract.Contacts.Entity`](https://developer.android.com/reference/android/provider/ContactsContract.Contacts.Entity)
- [`Event.START_DATE` is provider-entered text](https://developer.android.com/reference/android/provider/ContactsContract.CommonDataKinds.Event)
