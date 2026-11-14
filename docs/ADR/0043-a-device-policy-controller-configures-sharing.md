# ADR-0043: A device policy controller configures sharing

- Status: Accepted
- Date: 2026-11-14

## Context

A device that is managed — set up with a device policy controller (a device
owner or MDM app) — is often one whose sharing is meant to stay on: the person
who set the device up decides that it shares its position, and the person
holding it should not be able to turn that off by accident or on purpose.

Until now the application had no entry for another application at all. Only
the main activity was exported, for the connection link; the sharing service
and the boot receiver were private. Everything that decides whether the
device shares — the switch, the stop button in the notification, leaving a
group, signing out — was in the hands of whoever held the device.

Android already has the two pieces a managed device needs:

- a managed configuration, which the controller sets from its console and the
  application reads through `RestrictionsManager`;
- a bound service, which another application can call, and whose caller the
  platform names by its uid.

A package name proves nothing on its own: anyone can install an application
under a name that is still free. The signing certificate is what tells one
publisher from another.

## Decision

The application declares a managed configuration (`res/xml/app_restrictions.xml`)
with these keys:

| Key                         | Type         | Meaning                                                                                         |
|-----------------------------|--------------|-------------------------------------------------------------------------------------------------|
| `trusted_controllers`       | bundle array | packages, each with the SHA-256 of its signing certificate, allowed to call the control service |
| `connect_link`              | string       | a connection link handed to the usual connection flow when its group is not connected yet       |
| `expected_account`          | string       | the address the member is expected to sign in with; a different one is pointed out              |
| `sharing_enabled`           | bool         | whether the device shares; when set, it takes the place of the member's switch                  |
| `lock_sharing`              | bool         | the switch and the stop button in the notification are unavailable                              |
| `lock_group`                | bool         | leaving a group is unavailable                                                                  |
| `lock_account`              | bool         | signing out and changing the account are unavailable                                            |
| `position_interval_minutes` | integer      | the longest the device goes between two positions while nobody watches, 1 to 20 minutes         |

The configuration is read when the application starts and again whenever the
platform says it changed. A device with no configuration behaves exactly as
before.

An exported bound control service accepts three commands — start, stop and
status — from a caller whose package and signing certificate are both on
`trusted_controllers`. Any other caller is refused, and the refusal says
nothing about why. Status reports whether sharing is wanted, whether a group
is connected, whether the member is signed in, which permissions are granted
and whether the service runs.

Trusted packages and certificates come only from the managed configuration;
the application carries none of its own.

The sign-in stays the member's: a connection link from the configuration opens
the same flow a link from a message does, and the Google sign-in page is still
answered by a person.

The sending frequency is given as an interval rather than as a choice of
mode: a controller thinks in "every so many minutes", and the modes are the
collection policy's own vocabulary, which may change. The interval is a
ceiling laid over the policy, not a replacement of it. `chooseMode` picks the
mode as before; with the interval set, every mode asks, batches and writes
stillness no less often than the interval, and Idle, which asks for nothing,
gives way to Standby. The ceiling never slows anything down: Viewing keeps
asking every thirty seconds, and only its stillness is written sooner when the
interval is shorter than its three minutes. The
range ends at 20 minutes because a position has to be written again before
the half hour after which a marker turns grey, and starts at one minute; a
short interval costs the group writes of its free quota, which is the
controller's choice to make. The member's screen offers no way to change it.

## Consequences

- A managed device can keep sharing on and keep the member in the group
  without anybody watching over the settings screen.
- The control service is reachable by any application, so its safety rests on
  the certificate check alone; a configuration that lists no controllers
  leaves it refusing everybody.
- Locked controls stay on the screen, disabled, with the words "Managed by
  the device's controlling app", so the member sees why they do nothing.
- With an interval set, a device inside its geofence keeps writing instead of
  falling silent, and a device on a low battery keeps the interval though it
  asks at low power: the controller's ceiling outranks the saving.
- A group's administrator can still remove the device from the group; that is
  decided in the group's database and no device setting overrides it.
- Whether a manufacturer's own startup list lets the controller start the
  service after a restart is not settled here; it is checked on a device once
  the entry exists.
