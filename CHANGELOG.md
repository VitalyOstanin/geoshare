# Changelog

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

- [0.1.0](#010---2026-10-14)

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
