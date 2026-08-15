# Android parity ledger

Every implementation task must record the current iOS reference commit,
behavioral coverage, tests, visual evidence, and intentional Android
deviations here.

## Current reference

- iOS repository: `/Users/tom/SynologyDrive/projects/ErdentageiOS`
- Commit: `95aa5b595ec8cad1ed8c7ebcf8c039e7a3588eae`
- Branch/worktree: `main`, clean
- Inspected: 2026-08-15

## Ledger

| Area | iOS reference | Android status | Verification | Deviation |
| --- | --- | --- | --- | --- |
| Project skeleton | `95aa5b5` | Complete | `test lint assembleDebug`; `assembleRelease bundleRelease`; API 36 instrumentation test; visible launch and screenshot inspected 2026-08-15 | Native Android build and lifecycle |
| Domain model and day count | `95aa5b5` | Not started | Swift tests identified for translation | None planned |
| Milestone engine | `95aa5b5` | Not started | Swift tests identified for translation | None planned |
| Person pager | `95aa5b5` | Not started | Pending | Native Compose pager |
| Demnächst overview | `95aa5b5` | Not started | Pending | Native Compose canvas |
| Settings and people | `95aa5b5` | Not started | Pending | Native Android controls |
| Platform integrations | `95aa5b5` | Not started | Pending | Android intents and selected-contact access |
| Home-screen widget | `95aa5b5` | Not started | Pending | One configurable Glance widget type |
