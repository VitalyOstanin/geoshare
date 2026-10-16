# User stories

What each kind of person does with geoshare, written as stories: who, what and
why, with what has to hold for the story to count as working. One file per
role; inside it, one section per story.

## Contents

- [Roles](#roles)
- [How a story is written](#how-a-story-is-written)
- [Status](#status)
- [Records behind the stories](#records-behind-the-stories)

## Roles

| № | Role          | Prefix | File                                 | Who                                                                          |
|---|---------------|--------|--------------------------------------|------------------------------------------------------------------------------|
| 1 | project owner | OWN    | [project-owner.md](project-owner.md) | Set up the group's Firebase project and holds it; outside the database       |
| 2 | administrator | ADM    | [administrator.md](administrator.md) | A member listed in the group's `admins`; the founder is the first one        |
| 3 | member        | MEM    | [member.md](member.md)               | A person on the group's roster, on a device or on the page                   |
| 4 | device        | DEV    | [device.md](device.md)               | A device of a member, as the service on it acts without anybody at the screen|
| 5 | guest         | GST    | [guest.md](guest.md)                 | A person holding a link who is not on any roster yet                         |

The words are those of [GLOSSARY.md](../../GLOSSARY.md).

## How a story is written

```markdown
## ADM-03 Invite a person by their Google address

- Status: done
- ADR: 0015, 0025
- Tests: firebase/tests/rules/membership.test.mjs (invitations)

As an administrator I make an invitation for one Google address, so that only
that person can join on the link and only once.

Acceptance:
- the link admits the named account and nobody else;
- ...
```

- The id is the role's prefix and a number. A number is never given to another
  story, also after the story is dropped.
- `ADR` names the records the story rests on, when there are any.
- `Tests` names the tests that hold the acceptance lines: the rules' tests
  under `firebase/tests/rules`, the Android unit tests by class, the page's
  tests under `web/tests`. A test written for a story names the story's id in
  a comment above it, so the link is found from either side.
- `Not covered` says which acceptance lines no automatic test holds — most
  often what only a device shows: a sound, a notification, a restart — so
  that they are checked by hand before a release.
- Acceptance lines are what a test, or somebody with a device in hand, can
  check.

## Status

- `done` — both the device and, where the story names it, the page do it now.
- `planned` — written down in [TODO.md](../../TODO.md) or decided, not built.
- `partly` — built in one client only, or with a part left for later; the
  story says which.

## Records behind the stories

Each story names its records in `ADR`; the same links the other way round:

| №  | ADR                                                                    | Decision                                                               | Stories                                                        |
|----|------------------------------------------------------------------------|------------------------------------------------------------------------|----------------------------------------------------------------|
| 1  | [0001](../ADR/0001-one-build-settings-at-runtime.md)                   | One build for every group, project settings read at runtime            | OWN-03, MEM-01, GST-03                                         |
| 2  | [0002](../ADR/0002-sign-in-through-the-browser.md)                     | Sign in with Google through the browser flow                           | MEM-01, DEV-04                                                 |
| 3  | [0003](../ADR/0003-project-setup-by-script.md)                         | A group's project is set up by a script                                | OWN-01, OWN-02, OWN-03, OWN-04                                 |
| 4  | [0004](../ADR/0004-firebase-as-the-shared-store.md)                    | Firestore as the shared store, with applications of our own            | MEM-07, DEV-01                                                 |
| 5  | [0005](../ADR/0005-a-project-per-group.md)                             | Every group runs its own Firebase project                              | OWN-01, OWN-06                                                 |
| 6  | [0006](../ADR/0006-maplibre-and-osm-tiles.md)                          | MapLibre on both clients, OpenStreetMap raster tiles                   | MEM-14                                                         |
| 7  | [0007](../ADR/0007-positions-per-device.md)                            | A position per device, not per member                                  | MEM-08, MEM-09, MEM-14, DEV-01                                 |
| 8  | [0008](../ADR/0008-permissions-per-reader-with-expiry.md)              | Permission to watch is given per reader, with an expiry, and read live | ADM-12, MEM-10, DEV-09                                         |
| 9  | [0009](../ADR/0009-group-named-after-its-founder.md)                   | A group is named after its founder, and an invitation carries the name | ADM-01                                                         |
| 10 | [0010](../ADR/0010-collection-modes-and-watch-claims.md)               | Positions are collected by mode, and fast only while somebody watches  | MEM-13, MEM-14, DEV-02, DEV-03, DEV-10                         |
| 11 | [0011](../ADR/0011-search-per-device.md)                               | An emergency search opens one device to the whole group                | ADM-07, ADM-12, MEM-18                                         |
| 12 | [0012](../ADR/0012-stand-in-services-for-development.md)               | Development runs against Firebase emulators, never the real project    | — (development only)                                           |
| 13 | [0013](../ADR/0013-languages.md)                                       | English and Russian, chosen per application                            | MEM-19                                                         |
| 14 | [0014](../ADR/0014-jump-filter-off-by-default.md)                      | The jump filter is a setting, off by default, and always logged        | ADM-09, MEM-17, DEV-07                                         |
| 15 | [0015](../ADR/0015-invitation-for-one-google-account.md)               | An invitation is issued to one Google account                          | ADM-03, MEM-01, GST-04                                         |
| 16 | [0016](../ADR/0016-group-has-a-name.md)                                | A group has a name people choose                                       | ADM-01, ADM-02                                                 |
| 17 | [0017](../ADR/0017-names-in-the-invitation-link.md)                    | The invitation link carries who invited and the group's name           | ADM-03, GST-01                                                 |
| 18 | [0018](../ADR/0018-what-the-connection-screens-show.md)                | What the join page and the connection screen show                      | ADM-03, GST-01, GST-02, GST-04                                 |
| 19 | [0019](../ADR/0019-making-an-invitation-and-naming-a-group.md)         | Making an invitation and naming a group                                | ADM-01, ADM-02, ADM-03, MEM-01, MEM-02, GST-03, GST-04         |
| 20 | [0020](../ADR/0020-who-is-asked-for-the-position.md)                   | Who is asked for the position                                          | MEM-17, DEV-04                                                 |
| 21 | [0021](../ADR/0021-sky-file-in-google-logger-layout.md)                | The sky file is laid out as Google's GNSS logger lays out its own      | DEV-07, DEV-11                                                 |
| 22 | [0022](../ADR/0022-downloads-follow-the-network-choice.md)             | Background downloads follow the member's network choice                | MEM-20                                                         |
| 23 | [0023](../ADR/0023-the-sky-is-held-against-published-orbits.md)        | The sky is held against the published orbits                           | MEM-17, DEV-07                                                 |
| 24 | [0024](../ADR/0024-glonass-joins-the-orbit-check.md)                   | GLONASS joins the orbit check                                          | MEM-17, DEV-07                                                 |
| 25 | [0025](../ADR/0025-an-invitation-admits-once.md)                       | An invitation admits once and lasts at most a day                      | ADM-03, MEM-01, GST-04                                         |
| 26 | [0026](../ADR/0026-several-groups-on-one-device.md)                    | Several groups on one device                                           | ADM-04, ADM-08, ADM-09, MEM-03, MEM-04, MEM-05, MEM-09, DEV-08 |
| 27 | [0027](../ADR/0027-what-an-administrator-sees.md)                      | What an administrator sees and may do                                  | OWN-05, ADM-04, ADM-05, ADM-06, ADM-12, DEV-10                 |
| 28 | [0028](../ADR/0028-a-siren-on-a-lost-phone.md)                         | A siren on a lost phone                                                | ADM-08, MEM-18                                                 |
| 29 | [0029](../ADR/0029-resetting-a-stuck-place.md)                         | Resetting a stuck place from another screen                            | ADM-09, MEM-18                                                 |
| 30 | [0030](../ADR/0030-names-for-oneself.md)                               | Names for people and phones, for oneself alone                         | MEM-16                                                         |
| 31 | [0031](../ADR/0031-a-refused-reader-asks-again.md)                     | A refused reader asks again when the granter says so                   | MEM-10, DEV-09                                                 |
| 32 | [0032](../ADR/0032-scanning-an-invitation-with-camerax-and-zxing.md)   | An invitation is scanned with CameraX and zxing                        | MEM-21                                                         |
| 33 | [0033](../ADR/0033-a-setup-page-finishes-the-project.md)               | A setup page finishes the group's project                              | OWN-09                                                         |
| 34 | [0034](../ADR/0034-the-published-key-is-listed-in-the-repository.md)   | The published key's fingerprints are listed in the repository          | OWN-10                                                         |
| 35 | [0035](../ADR/0035-the-page-serves-its-libraries-itself.md)            | The page serves its libraries itself, one directory per version        | — (how the page is built and served)                           |
| 36 | [0036](../ADR/0036-a-project-holds-one-group.md)                       | A project holds one group, founded once                                | ADM-01                                                         |
| 37 | [0037](../ADR/0037-google-cloud-shell-is-not-supported.md)             | Google Cloud Shell is not a supported place to set up a project        | —                                                              |
| 38 | [0038](../ADR/0038-the-first-group-is-founded-by-the-project-owner.md) | The first group of a project is founded by its owner                   | OWN-04, OWN-09                                                 |

Stories that rest on no record: OWN-07, OWN-10, ADM-10, ADM-11, MEM-06, MEM-11,
MEM-12, MEM-15, MEM-22, DEV-05, DEV-06, DEV-12, DEV-13, DEV-14.
They are either planned, or follow from a record named by a neighbouring story.
