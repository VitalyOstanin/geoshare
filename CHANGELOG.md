# Changelog

English · [Русский](CHANGELOG.ru.md)

What changed in each release of the application, and what the project owner
of a group has to do after it. The section of a version is what its page on
the releases page says; the release is refused while the section is missing
or carries no date.

Every version says under **Project owners** whether the release needs
`tools/project.sh update` — the rules and the group's page — and whether that
comes before or after the devices take the new application. The rule behind
it is in
[Updating a group's project](firebase/README.md#updating-a-groups-project).

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the versions follow [Semantic Versioning](https://semver.org/).

## Contents

- [Unreleased](#unreleased)
- [0.2.0](#020---2026-10-17)
- [0.1.1](#011---2026-10-16)
- [0.1.0](#010---2026-10-14)

## [Unreleased]

### Added

- "Project website" at the end of the settings opens the project's website,
  its Russian page on a device in Russian.

### Changed

- "Update" over a network switched off under Downloads, roaming among them,
  first asks, with the update's size, "Download" or "Not now" (ADR-0039).

### Fixed

- A map left in front on a screen that stays lit, with nobody touching it,
  no longer keeps every device of the group collecting every thirty seconds:
  after five minutes without a touch it stops counting as somebody looking,
  as the page already did (ADR-0040).
- A person who is not the project owner and tries to create the group in an
  empty project is told that only the project owner can create it and to ask
  them for an invitation, instead of a general refused write; the map's
  screen for a device without a group shows the refusal too.
- A member who already shares their place is no longer asked whom to share
  it with when the application starts on a slow connection.

### Project owners

- `tools/project.sh update` brings the page's line for a refused founding.
  The rules do not change, so it may come before or after the devices take
  the new application.

## [0.2.0] - 2026-10-17

### Added

- The application looks for a new version once a day, says so in a
  notification and with a dot on the settings button, and downloads and
  installs it from the settings. The daily look can be switched off there;
  the "Check for updates" button still works.
- "What's new" shows what the new version changes, in the application's
  language, before it is installed.

### Fixed

- Times of day follow the system's 24-hour setting, also where it differs
  from the language: American English on a 24-hour clock shows "20:33", not
  "8:33 PM".
- The screens before a device is connected follow the chosen theme; in the
  dark one the status bar's icons no longer sit white on a light ground.
- The connection screen no longer says that the project owner sees a
  history of places: none is kept.

### Project owners

- `tools/project.sh update` brings the check that only the project owner
  founds the group (ADR-0038), the checks of the devices a member grants
  and of the labels a member gives, and the page's fixes. The new rules refuse
  nothing the application in use writes, so it may come before or after the
  devices take the new application.

## [0.1.1] - 2026-10-16

The first release built in the open: its package carries attestations of
where it was built and what from.

### Changed

- The application is built with Firebase BoM 35; the group's page runs on
  Firebase 13 and MapLibre GL JS 6.14.0.
- The setup page puts a group's page online four files at a time: about
  half a minute instead of four.
- The setup page names the account it signed in with, and its console links
  open in that account rather than in the first one of the browser.
- The setup page and the README say what the consoles will ask: 2-Step
  Verification, the identical OAuth client notice and the consent screen of
  the first sign-in.
- `tools/project.sh` needs Node.js 20 or later instead of Python 3.

### Fixed

- The names on a member's card on the group's page stay readable while the
  siren sounds: the buttons go under them.
- A device that loses the network keeps collecting positions; what it
  writes meanwhile is sent when the database is back.
- A position is judged against the one written just before it, also when
  two arrive at once.
- A false place or a setting typed with a wrong type in the console is
  ignored instead of stopping the device from reading the others.
- The times on a member's card follow the language chosen in the
  application.
- An account whose domain is longer than about 54 characters is no longer
  told that its own invitation is meant for another account.
- An entry of a member's document that is not a device no longer pushes a
  device off the member's list.
- The setup page says that setup-config.json is broken instead of failing
  a step later.
- A project owner who made the group on a device finds it on the group's
  page and on another device, instead of being told the project already
  has a group.
- An invitation waiting for its sign-in is kept when another project is
  connected.
- A group taken off the device is signed out of before its project is
  connected again.
- "Leave the group" names the group it leaves.
- The card that stands in place of the map scrolls on a device turned on
  its side, and keeps the group name typed into it.
- An account on no roster no longer says it has the map open.
- The group's page reads a link with fields both before and after the `#`
  the way the application does.
- The setup page no longer fails a step on one poll lost to the network,
  takes the settings of geoshare's own app in a project that holds another,
  lists every page of the account's projects and registers fingerprints
  written in colon-separated pairs.

### Project owners

- `tools/project.sh update` brings the page's fixes and Firebase 13; the
  rules have not changed, so it may come before or after the devices take
  the new application.

## [0.1.0] - 2026-10-14

The first release.

### Added

- A map of the group in the Android application and on the group's page:
  one pin per device, the age of each position, its width and the charge
  left.
- Sharing per person, each permission with an end.
- Joining by an invitation link or its QR code, once and within a day.
- Running the group: invitations, removing members, other administrators.
- Searching for a member, and a siren on a lost device; a stuck pin is
  reset from the device's card.
- Several groups on one device, and names of one's own for people and
  devices.
- False positions left out: the device holds every answer against the
  rules described in the README before writing it.
- Reporting a problem from the application and the page: an issue on
  GitHub with the end of the log, names, places and ids left out. Nothing
  is sent until it is submitted on GitHub.
- The licences of the libraries the application is built from.

### Project owners

- Nothing to update: a project set up by the setup page or by
  `tools/project.sh` already has the rules and the page of this version.

[Unreleased]: https://github.com/VitalyOstanin/geoshare/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/VitalyOstanin/geoshare/releases/tag/v0.2.0
[0.1.1]: https://github.com/VitalyOstanin/geoshare/releases/tag/v0.1.1
[0.1.0]: https://github.com/VitalyOstanin/geoshare/releases/tag/v0.1.0
