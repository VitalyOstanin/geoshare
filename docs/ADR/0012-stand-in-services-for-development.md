# ADR-0012: Development runs against Firebase emulators, never the real project

- Status: Accepted
- Date: 2026-09-12 (recorded 2026-09-30)

## Context

Working on the exchange needs several accounts and devices at once, and
nothing done while developing may reach a group's real data. An Android
emulator has no Google account to sign in with.

## Decision

- The Firestore and Auth emulators run under a `demo-` project id, which needs
  no credentials and cannot reach a real project, and are filled with
  stand-in accounts and positions.
- A debug build on an Android emulator points Firebase at them and signs in
  with the stand-in accounts' email and password. A debug build on a phone is
  untouched.
- The redirection and the stand-in accounts live only in the debug sources;
  the release build carries an empty version of the same names.
- The Firestore rules are tested against the same emulator.

## Consequences

- Both clients can be exercised end to end without phones or real accounts.
- The release build contains no test accounts or passwords.
