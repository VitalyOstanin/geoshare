# ADR-0044: A debug build signs in a stand-in account from the shell

- Status: Accepted
- Date: 2026-11-15

## Context

ADR-0012 lets a debug build on an Android emulator sign in with the stand-in
accounts, through buttons on the sign-in screen. A check that drives the
application from outside — another application calling the control service of
ADR-0043, for instance — runs with nobody to press them, and without a session
every start is refused as not signed in.

A command from the shell is what such a check has. An exported receiver is the
usual way to take one, and it must not become a way in for anything else on
the device.

## Decision

The debug build declares a receiver, in its own manifest and sources, for two
actions:

| Action                                                       | Extra                    | Effect                                                            |
|--------------------------------------------------------------|--------------------------|-------------------------------------------------------------------|
| `io.github.vitalyostanin.geoshare.debug.STAND_IN_SIGN_IN`    | `email`, optional        | signs in the named stand-in account, the first of them by default |
| `io.github.vitalyostanin.geoshare.debug.STAND_IN_SIGN_OUT`   | —                        | signs the current group's session out                             |

- The receiver requires the sender to hold `android.permission.DUMP`, which the
  shell holds and an installed application cannot be granted.
- It does nothing unless the build already talks to the stand-in services, so
  on a phone it never reaches a real project, and it accepts only the
  addresses the seed made.
- The outcome is the broadcast's result: `-1` with the account and its uid, or
  `0` with the reason, so the command that sent it prints the answer.
- The release build has neither the receiver nor its code.

The group needs no command of its own: the seed puts the first stand-in
account on the roster of the group the debug build connects to, so signing it
in is joining.

## Consequences

- An emulator can be brought to a signed-in member of the stand-in group with
  one `adb shell am broadcast`, and the control service then starts and stops
  sharing as it would on a managed device.
- The debug build has one more exported component than the release build;
  the manifest test, which reads the main manifest, still describes the
  release.
