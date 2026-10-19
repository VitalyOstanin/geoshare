# Device

A member's device, as the service on it acts with nobody at the screen. The
stories are written for the member who carries it: what the device has to do
on its own for the others to see a true place at a fair cost of battery.

## Contents

- [DEV-01 Write the place without being asked](#dev-01-write-the-place-without-being-asked)
- [DEV-02 Collect faster while somebody watches](#dev-02-collect-faster-while-somebody-watches)
- [DEV-03 Spend little while standing still](#dev-03-spend-little-while-standing-still)
- [DEV-04 Work without Google Play services](#dev-04-work-without-google-play-services)
- [DEV-05 Get the permissions it needs, once](#dev-05-get-the-permissions-it-needs-once)
- [DEV-06 Start again after the device restarts](#dev-06-start-again-after-the-device-restarts)
- [DEV-07 Leave out answers that cannot be true](#dev-07-leave-out-answers-that-cannot-be-true)
- [DEV-08 Notice being taken off the roster](#dev-08-notice-being-taken-off-the-roster)
- [DEV-09 Pick up a permission as soon as it is given](#dev-09-pick-up-a-permission-as-soon-as-it-is-given)
- [DEV-10 Report that it is alive](#dev-10-report-that-it-is-alive)
- [DEV-11 Keep a log the member can hand over](#dev-11-keep-a-log-the-member-can-hand-over)
- [DEV-12 Wake when somebody needs it](#dev-12-wake-when-somebody-needs-it)
- [DEV-13 Update itself](#dev-13-update-itself)
- [DEV-14 Use the location services of Huawei devices](#dev-14-use-the-location-services-of-huawei-devices)

## DEV-01 Write the place without being asked

- Status: done
- ADR: 0004, 0007
- Tests: rules `positions.test.mjs`; Android `WriteFilterTest`
- Not covered: the service and its notification, on a device

As a member I let a foreground service on my device write my place, so that the
group sees it without my opening the application.

Acceptance:

- the service runs with a notification that says it shares and lets it stop;
- each device writes `positions/{uid}.{deviceId}` of its own;
- the same place is written into every group the device is signed in to.

## DEV-02 Collect faster while somebody watches

- Status: done
- ADR: 0010, 0040
- Tests: rules `access.test.mjs` (watch); Android `WatchingTest`, `ModeSelectionTest`, `CollectionModeTest`

As a member I let my device collect fast only while somebody has the map open,
so that the battery is spent when it is worth it.

Acceptance:

- a viewer renews a watch claim every five minutes; a claim older than seven
  minutes stops counting;
- a map nobody has touched for five minutes, in the application or on the
  page, withdraws its claim, and the next touch makes it again;
- a device that sees a live claim on itself switches to the viewing mode.

## DEV-03 Spend little while standing still

- Status: done
- ADR: 0010
- Tests: Android `GeofenceTest`, `StillnessTest`, `MovementTest`, `ModeSelectionTest`,
  `FixSieveTest`
- Not covered: battery, measured on a device

As a member I let my device stay quiet while I do not move, so that a day at
home costs little battery.

Acceptance:

- moving is learnt from the system's activity transitions;
- standing still inside a 150 m geofence keeps the receiver off;
- an idle device rewrites the same point about every 25 minutes so that it
  does not go grey, and `takenAt` keeps when it was measured.

## DEV-04 Work without Google Play services

- Status: done
- ADR: 0002, 0020
- Tests: Android `SourcesTest`, `AskingTest`, `ProviderNoteTest`, `FixSieveTest`
- Not covered: sign-in on a device without Play services, by hand

As a member with a device or tablet that has no Google Play services I still
sign in and share my place, so that the group is not limited to devices with
Google on them.

Acceptance:

- sign-in goes through the browser;
- without Play services the platform's own providers are asked, and only the
  sources switched on in the settings.

## DEV-05 Get the permissions it needs, once

- Status: done
- ADR: —
- Tests: Android `PermissionStepTest`, `BackgroundWorkTest`, `MovementPermissionTest`, `ManifestTest`

As a member I am led through the location, background, activity and battery
permissions step by step, so that the service is not killed and I know why
each is asked.

Acceptance:

- each step says what it is for;
- for makers that stop background work the application opens their own list
  of allowed applications and lets me say it is done.

## DEV-06 Start again after the device restarts

- Status: partly — built, not yet checked on the devices
- ADR: —
- Tests: Android `BootReceiverTest`, `StartConditionsTest`
- Not covered: a restart of a real device

As a member I let the service start by itself after the device restarts, so
that I do not have to remember to open the application.

Acceptance:

- after a restart the service runs if sharing was on;
- it stays off if sharing was off.

## DEV-07 Leave out answers that cannot be true

- Status: partly — the check against the motion sensors only records, it does
  not refuse yet
- ADR: 0014, 0021, 0023, 0024
- Tests: rules `positions.test.mjs` (answers left out); Android `JumpsTest`, `OrbitCheckTest`, `OrbitsTest`, `SkyTest`, `SlantTest`, `DriftTest`, `MovementTest`, `FalsePlacesTest`, `LeftOutRunTest`

As a member near a place where satellite signals are spoofed I let the device
leave out answers that cannot be true, so that the group does not see me
jump to an airport.

Acceptance:

- satellites standing below the horizon, or at a distance off their published
  orbits, mark the epoch false; two in a row leave the fix out;
- a place that moved while the device stood still is refused;
- a recorded false place marks positions taken there;
- the jump filter is a setting, off by default;
- every answer left out is counted with its reason.

## DEV-08 Notice being taken off the roster

- Status: done
- ADR: 0026
- Tests: rules `leaving.test.mjs` (a member taken off the roster); Android `PresenceTest`
- Not covered: the screen itself

As a member taken off a roster I am told so on that group's map instead of an
empty map, so that I know what happened.

Acceptance:

- "You were removed from the group", with "Remove the group" and "Join by
  link";
- the group's settings say the same;
- the connection is not removed on its own;
- a refusal is read as removal only after the device has seen its account on
  that roster.

## DEV-09 Pick up a permission as soon as it is given

- Status: done
- ADR: 0008, 0031
- Tests: rules `access.test.mjs` (a member touches their own profile to say their sharing changed); Android `RefusalsTest`; web `refusals.test.mjs`

As a member given a permission to see somebody I see their place at once, so
that I do not wait a minute after they share.

Acceptance:

- the one who grants touches their profile; the readers who were refused
  retry at once on that signal;
- without the signal they retry after a quarter of an hour.

## DEV-10 Report that it is alive

- Status: done
- ADR: 0010, 0027
- Tests: rules `access.test.mjs` (devices); Android `PhoneStatusTest`
- Not covered: when the device writes its status

As a member I let my device say when it last wrote, in which mode and whether
it is charging, so that an administrator notices it has gone quiet (ADM-06).

Acceptance:

- the device's status is written when its mode changes, when it is plugged in
  or out, and otherwise as often as an idle device rewrites its place;
- it carries no place.

## DEV-11 Keep a log the member can hand over

- Status: done
- ADR: 0021
- Tests: Android `FileLogTest`, `SkyLogTest`

As a member I let the device keep a log of what it received and left out, and
of the raw satellite measurements, so that a wrong place can be looked into
afterwards.

Acceptance:

- logs roll over and are kept within a limit;
- the satellite file is in GnssLogger's layout, so other tools read it.

## DEV-12 Wake when somebody needs it

- Status: planned
- ADR: —
- Tests: —
- Not covered: planned

As a member whose device is asleep I let a push wake it when somebody opens a
search, calls the siren or resets its place, so that it answers at once and
not at its next own wake-up, without a paid Firebase plan.

Acceptance:

- to be agreed.

## DEV-13 Update itself

- Status: done
- ADR: 0039
- Tests: Android `UpdateRulesTest`, `UpdateCheckTest`, `DownloadPolicyTest`, `LatestReleaseTest`, `ReleaseCheckTest`, `ApkVerifierTest`, `ApkDownloadTest`, `UpdateBlockStateTest`, `ReleaseNotesTest`, `BootReceiverTest` (the service starts again after an update)
- Not covered: the question before an update over a network switched off, which no test opens; the application does not compare signing keys itself; the system installer's screen, the permission page and the "What's new" window, which no test opens

As a member I let the application learn about a new published build and
install it, so that I do not have to look for updates.

Acceptance:

- asks GitHub for the latest release about once a day, from the screen's
  start and from the sharing service, which looks every hour and asks only
  when the last check is a day old;
- the daily look can be switched off in the settings; "Check for updates"
  asks at once and works either way;
- a release newer than the application is announced once, by a notification
  that opens the settings, and the settings button carries a dot while it is
  waiting;
- "What's new" shows the release's changes in a window, in the
  application's language, without the part meant for project owners; a
  release with no Russian text shows its English one, and one with no text
  at all opens its page on GitHub;
- the APK is downloaded when the member presses "Update", with progress and
  a way to cancel, and is kept only if its size and SHA-256 are those the
  release announces; a release without them is no update;
- over a network switched off under Downloads (MEM-20), roaming among them,
  "Update" first asks, with the update's size, "Download" or "Not now";
- the downloaded file is handed to the system's installer; if the
  application is not yet allowed to install applications, the system's page
  for that opens first;
- the sharing service starts again after the update, as it does after a
  boot, unless the device refuses it.

## DEV-14 Use the location services of Huawei devices

- Status: planned
- ADR: —
- Tests: —
- Not covered: planned

As a member with a Huawei device without Google I let the application use the
geofence and activity recognition of Huawei's own services, so that standing
still is as cheap there as on other devices.

Acceptance:

- to be agreed.
