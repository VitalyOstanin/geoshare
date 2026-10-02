# Architecture decision records

Each record states one decision as it was taken. The body of an accepted
record is not rewritten; a changed decision gets a new record that supersedes
the old one, and only the old record's status changes. Records 0004 to 0014
were written afterwards, on 2026-09-30, for decisions taken earlier; their
date is the decision's.

| №    | Decision                                                                                                             | Status                              | Date       |
|------|----------------------------------------------------------------------------------------------------------------------|-------------------------------------|------------|
| 0001 | [One build for every group, settings read at runtime](0001-one-build-settings-at-runtime.md)                         | Accepted                            | 2026-09-30 |
| 0002 | [Sign in with Google through the browser flow](0002-sign-in-through-the-browser.md)                                  | Accepted                            | 2026-09-30 |
| 0003 | [A group's project is set up by a script](0003-project-setup-by-script.md)                                           | Accepted                            | 2026-09-30 |
| 0004 | [Firestore as the shared store, with applications of our own](0004-firebase-as-the-shared-store.md)                  | Accepted                            | 2026-09-11 |
| 0005 | [Every group runs its own Firebase project](0005-a-project-per-group.md)                                             | Accepted                            | 2026-09-11 |
| 0006 | [MapLibre on both clients, OpenStreetMap raster tiles](0006-maplibre-and-osm-tiles.md)                               | Accepted                            | 2026-09-11 |
| 0007 | [A position per device, not per member](0007-positions-per-device.md)                                                | Accepted                            | 2026-09-12 |
| 0008 | [Permission to watch is given per reader, with an expiry, and read live](0008-permissions-per-reader-with-expiry.md) | Accepted                            | 2026-09-12 |
| 0009 | [A group is named after its founder, and an invitation carries the name](0009-group-named-after-its-founder.md)      | Accepted, partly superseded by 0015 | 2026-09-25 |
| 0010 | [Positions are collected by mode, and fast only while somebody watches](0010-collection-modes-and-watch-claims.md)   | Accepted                            | 2026-09-13 |
| 0011 | [An emergency search opens one device to the whole group](0011-search-per-device.md)                                 | Accepted                            | 2026-09-27 |
| 0012 | [Development runs against Firebase emulators, never the real project](0012-stand-in-services-for-development.md)     | Accepted                            | 2026-09-12 |
| 0013 | [English and Russian, chosen per application](0013-languages.md)                                                     | Accepted                            | 2026-09-13 |
| 0014 | [The jump filter is a setting, off by default, and always logged](0014-jump-filter-off-by-default.md)                | Accepted                            | 2026-09-13 |
| 0015 | [An invitation is issued to one Google account](0015-invitation-for-one-google-account.md)                           | Accepted                            | 2026-09-30 |
| 0016 | [A group has a name people choose](0016-group-has-a-name.md)                                                         | Accepted                            | 2026-09-30 |
| 0017 | [The invitation link carries who invited and the group's name](0017-names-in-the-invitation-link.md)                 | Accepted                            | 2026-09-30 |
| 0018 | [What the join page and the connection screen show](0018-what-the-connection-screens-show.md)                   | Accepted                            | 2026-09-30 |
| 0019 | [Making an invitation and naming a group](0019-making-an-invitation-and-naming-a-group.md)                          | Accepted                            | 2026-09-30 |
| 0020 | [Who is asked for the position](0020-who-is-asked-for-the-position.md)                                           | Accepted                            | 2026-10-02 |
| 0021 | [The sky file is laid out as Google's GNSS logger lays out its own](0021-sky-file-in-google-logger-layout.md)      | Accepted                            | 2026-10-02 |
| 0022 | [Background downloads follow the member's network choice](0022-downloads-follow-the-network-choice.md)        | Accepted                            | 2026-10-02 |
| 0023 | [The sky is held against the published orbits](0023-the-sky-is-held-against-published-orbits.md)              | Accepted                            | 2026-10-02 |
