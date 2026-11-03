# Google Play data safety draft

This is the repository source for the Play Console answers. Re-check the
questionnaire wording immediately before submission.

## Collection and sharing

- **Does the app collect or share user data?** No.
- The developer receives no names, birth dates, usage records, diagnostics,
  identifiers, contacts, calendar contents, or other user data.
- The app has no account, backend, analytics, advertising, tracking, crash
  reporting, or `INTERNET` permission.
- Family-level values added in 1.4 (the „Zusammen“ row budgets per category
  and the history of „Zusammen“ calendar handoffs) are stored in the same
  local file as the people and leave the device only through the OS backup
  described below.
- Android OS-managed backup or device transfer may copy the local app data to
  the backup provider chosen by the user. This is platform storage controlled
  by the user/OS and not collection by Thomas Kahle. Widget-instance IDs are
  explicitly excluded from backup.

## User-initiated system handoffs

- Contact import uses the system picker and a temporary URI grant for exactly
  one selected contact. Erdentage reads only a usable display name and birthday
  and requests no broad contacts permission.
- Calendar handoff launches `ACTION_INSERT` for personal milestones and for
  „Zusammen“ rows alike; the calendar app receives the proposed title and date
  only after the user chooses that action. Erdentage
  requests no calendar permission and does not claim the event was saved.
- Sharing launches the Android Sharesheet. Data goes to another app only after
  the user invokes sharing and chooses a destination.

## Deletion and security

- There is no account-deletion requirement because no account exists.
- People can be deleted in the app. Uninstalling removes local app data, subject
  to the user's Android backup-provider retention and restore settings.
- No data is transmitted by Erdentage, so encryption in transit is not
  applicable.

## Packaged permissions

The release manifest intentionally has no `INTERNET`, `READ_CONTACTS`,
`READ_CALENDAR`, or `WRITE_CALENDAR` permission. AndroidX WorkManager contributes
system permissions for wake locks, boot completion, network-state observation,
and foreground-service capability so scheduled widget refreshes remain within
Android's background-execution framework; these permissions do not transmit
user data.
