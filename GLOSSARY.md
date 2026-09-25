# Glossary

The words this project uses, and the things they name. Written for somebody
reading the code or the rules for the first time, and for anybody deciding what
to call a new field.

Two halves. The first is the vocabulary: one word, what it means here, and
where it is defined. The second describes the entities the database holds and
how they relate — the same ground an ERD would cover, in sentences rather than
in a diagram.

## Contents

- [How to read this](#how-to-read-this)
- [Preferred wording](#preferred-wording)
- [The group and who is in it](#the-group-and-who-is-in-it)
- [Getting into a group](#getting-into-a-group)
- [What a member publishes](#what-a-member-publishes)
- [Positions](#positions)
- [How a position was arrived at](#how-a-position-was-arrived-at)
- [Watching, and how old is too old](#watching-and-how-old-is-too-old)
- [Judging an answer before it is written](#judging-an-answer-before-it-is-written)
- [What a phone keeps to itself](#what-a-phone-keeps-to-itself)
- [The clients and the stand-in stand](#the-clients-and-the-stand-in-stand)
- [Entities and relations](#entities-and-relations)
  - [The entities](#the-entities)
  - [How they relate](#how-they-relate)
  - [What holds them together](#what-holds-them-together)
  - [Where each document lives](#where-each-document-lives)

## How to read this

A term is canonical when it appears in the leftmost column of a table below.
Identifiers in code, fields in documents, words shown to a member and words in
comments are all meant to use the canonical term rather than a synonym, so that
one thing is called one thing in the rules, in both clients and on screen.

Where a term names a field or a type, the definition is the code, and the
column beside it says where that code is. Where the two disagree, the code is
right and this file is out of date.

## Preferred wording

| № | Prefer        | Avoid                                   | Why |
|---|---------------|-----------------------------------------|-----|
| 1 | member        | user, contact, friend                   | The person is in a group; "user" says nothing about which one, and the id in every document is `memberId` |
| 2 | group         | circle, team, channel                   | The group is the document everything else hangs under, and its name is the account of whoever founded it |
| 3 | roster        | member list, participants               | The roster is the `members` array of the group document; "list" is also what the panel of the page is called |
| 4 | administrator | admin, owner, moderator                 | The field is `admins`, and the word in the interface is spelled out; an owner in this project is the owner of a document, not of the group |
| 5 | position      | location, coordinates, point            | A position is one document: a place, when it was taken, and how. `location` is the platform's word and appears only where the platform's API does |
| 6 | fix           | reading, sample, measurement            | A fix is one answer from a provider, before anything decides whether to write it. A position is what gets written |
| 7 | invitation    | invite, link, token                     | The noun is `invitation`; the eight characters inside it are the code |
| 8 | code          | password, key, secret                   | It lets one more person in for a day and grants nothing else |
| 9 | grant         | permission document, sharing settings   | One document per member holding both halves of what they give out: their phones and their readers |
| 10 | permission   | share, subscription                     | A permission is one entry of `sharedWith`: one reader, and the moment it runs out |
| 11 | device       | phone id, installation                  | In the data a device is an identifier a phone made for itself; "phone" is the thing in somebody's pocket and is used in prose |
| 12 | watch claim  | presence, heartbeat, online status      | It says somebody had the map open a moment ago, not that they are online |
| 13 | stale        | offline, lost, disconnected             | A marker goes stale when its position is older than half an hour; nothing here knows whether a phone is connected |
| 14 | left out     | rejected, filtered, dropped             | An answer left out was collected and logged; it moved no marker |
| 15 | false place  | spoof zone, jamming area                | A place where receivers are known to be told a lie, as the group recorded it |

Two words are deliberately not restricted. **Household** is used in prose for
the people in a group, where "group" would read as the document; it names no
field. **Family** appears as the name of the group that predates invitations —
the one whose document id is literally `family` — and as the project's own
description of who it is for.

## The group and who is in it

| № | Term | Means | Defined in |
|---|------|-------|------------|
| 1 | group | The document everything else hangs under, and the unit of access: reading anything at all requires being on its roster | `firebase/firestore.rules`, `match /groups/{groupId}` |
| 2 | group id | The name of that document. For a group founded on a phone it is the account id of the founder; the household that predates invitations is named `family` | `firestore.rules`, `founding()` |
| 3 | roster | The `members` array: the account ids of everybody in the group. At most 50 | `firestore.rules`, `rosterFits()` |
| 4 | administrator | An account in the `admins` array. Hands out invitations, takes members off the roster, edits the group's settings and its false places | `firestore.rules`, `administering()` |
| 5 | standing | Where the account signed in stands with respect to a group, as the screens decide what to offer: `Ungrouped`, `Outsider`, `Member`, `Administrator`, `Unrun` | `android/.../membership/Roster.kt` |
| 6 | unrun group | A group with an empty `admins` array: nobody may change its roster, and the screens say so rather than offering controls every write would refuse | `Roster.kt`, `firestore.rules` |
| 7 | member id | An account id, as the sign-in provider issues it. The key of a profile, of a grant and of a join claim, and the first half of a position id | `android/.../auth/SignInController.kt` |
| 8 | current group | Which group this phone writes into, remembered on the phone itself and given out as a standing with a turn counter, so that founding or joining rebuilds every subscription | `android/.../service/CurrentGroup.kt` |

## Getting into a group

| № | Term | Means | Defined in |
|---|------|-------|------------|
| 1 | invitation | A document whose name is the code, holding who made it and when it runs out. Nobody may read it — the rules read it on a writer's behalf | `firestore.rules`, `match /invitations/{code}` |
| 2 | code | Eight characters from A–Z and 2–9 drawn at random. What is handed out leaves out the letters read for digits | `android/.../membership/Codes.kt` |
| 3 | invitation text | What a person is actually sent: the group's name and the code joined by a dot, because a write has to know its path before it is made | `Codes.kt`, `invitationFrom()` |
| 4 | join claim | A document the person joining writes under their own account id, holding the code they are using. It is how a code reaches the rules, which cannot be told anything except through the database | `firestore.rules`, `match /joins/{uid}` |
| 5 | joining | The write that adds one's own account to the roster and changes nothing else, allowed only while a join claim names an invitation that has not run out | `firestore.rules`, `joining()` |
| 6 | founding | The write that creates a group: named after the founder, with the founder alone on the roster and alone among the administrators | `firestore.rules`, `founding()` |

## What a member publishes

| № | Term | Means | Defined in |
|---|------|-------|------------|
| 1 | profile | The name and the account picture of one member, written by that member and read by the whole group, so a marker can carry a person rather than the first characters of an id | `firestore.rules`, `match /members/{uid}`; `android/.../map/MemberProfile.kt` |
| 2 | display name | The name the account carries. A member whose account has none writes no profile at all | `MemberProfile.kt`, `SignInController.kt` |
| 3 | grant | One document per member holding what they give out: the devices they carry and the readers they allow | `firestore.rules`, `match /grants/{uid}`; `android/.../map/MemberGrant.kt` |
| 4 | devices | The identifiers of the phones this member carries. A reader learns from here which position documents to read, because the collection of positions cannot be listed. At most 10 | `MemberGrant.kt` |
| 5 | shared with | The readers this member allows, each with the moment their permission runs out. At most 50 | `MemberGrant.kt` |
| 6 | permission | One entry of `sharedWith`: one reader and one expiry. Weighed against the server's clock on every read of a position | `firestore.rules`, `mayRead()` |

## Positions

| № | Term | Means | Defined in |
|---|------|-------|------------|
| 1 | position | Where one phone last said it was, as one document: the place, the moments, how it was found and how the phone is doing | `firestore.rules`, `match /positions/{positionId}`; `android/.../map/MemberPosition.kt` |
| 2 | position id | The account id and the device identifier joined by a dot. One document per phone, so two phones of one member do not overwrite each other | `MemberPosition.kt`, `firestore.rules`, `ownsPosition()` |
| 3 | taken at | When the fix was taken, by the clock of the phone that took it | `MemberPosition.kt` |
| 4 | written at | When the database accepted the write, by the server's clock. The one moment a reader can trust, and what a marker's age is measured from | `firestore.rules`, `positionShape()` |
| 5 | device name | What a phone calls itself, shown when a member has more than one on the map | `MemberPosition.kt` |
| 6 | accuracy, width | How wide the provider said its answer is, in metres. Drawn as a ring around the marker; absent where the provider named none | `MemberPosition.kt`, `web/public/lib/width.js` |
| 7 | left out count, reason, since | What a phone says while its answers are being thrown away and it is therefore writing no positions: how many, why, and since when. Without it a phone under a transmitter looks like a phone in a drawer | `firestore.rules`, `leftOutShape()` |
| 8 | settling | Whether the receiver had not yet settled since the phone was switched on | `firestore.rules`, `positionShape()` |

## How a position was arrived at

| № | Term | Means | Defined in |
|---|------|-------|------------|
| 1 | found by | How the phone says it reached the answer, in this project's own words rather than in the provider's | `android/.../collection/FoundBy.kt` |
| 2 | Satellites | Worked out from satellites the receiver was hearing at the time | `FoundBy.kt` |
| 3 | Assistance | Worked out elsewhere by an assistance service, from the towers and the Wi-Fi around the phone, and sent over the network | `FoundBy.kt` |
| 4 | Unheard | Credited to satellites the receiver measured nothing off | `FoundBy.kt` |
| 5 | OtherRadios | Worked out from the towers and the Wi-Fi, by the phone or by a service | `FoundBy.kt` |
| 6 | Pretended | Put there by another application on the phone | `FoundBy.kt` |
| 7 | Unsaid | Arrived with nothing said about how it was reached | `FoundBy.kt` |
| 8 | source switch | What a member turns off in the settings, as one switch per kind of answer: `SourceChoice`. An answer from a source switched off is still collected and logged and moves no marker | `android/.../collection/Sources.kt` |
| 9 | collection mode | How hard the phone is working and how readily it says what it found: `Idle`, `Standby`, `Moving`, `Viewing`, `Night`. `Viewing` is the only one that turns the receiver on | `android/.../collection/CollectionMode.kt` |

## Watching, and how old is too old

| № | Term | Means | Defined in |
|---|------|-------|------------|
| 1 | watch claim | A document saying that one phone had the map open, written per phone and read by the whole group. It carries the moment it was made, by the server's clock; the phones add the three minutes themselves | `firestore.rules`, `match /watch/{watchId}`; `android/.../collection/Watching.kt` |
| 2 | watcher | A member behind a claim that has not run out — what the strip under the map names | `Watching.kt`, `web/public/lib/watchers.js` |
| 3 | stale | A position older than half an hour. The marker is drawn grey, and every mode that asks for anything has to write again before that | `android/.../collection/Freshness.kt`, `web/public/lib/age.js` |
| 4 | following | How the map moves itself: after one member, after everybody, or not at all | `web/public/lib/following.js`, `android/.../map/Following.kt` |

## Judging an answer before it is written

The phone decides whether an answer is worth writing. These are the terms that
decision is made in.

| № | Term | Means | Defined in |
|---|------|-------|------------|
| 1 | trail | The places the phone has actually been — written positions only, never answers that were left out, so an hour of a false signal never becomes familiar. 50 places, three days | `android/.../collection/Trail.kt` |
| 2 | familiar place | Somewhere the trail holds three visits within 300 metres, spread over at least an hour | `Trail.kt` |
| 3 | anchor | The place a new answer is measured against after a restart. Good for an hour, or for twelve where it stands in a familiar place | `Trail.kt` |
| 4 | jump | An answer that would have taken more than the allowed speed to reach from the anchor. Left out rather than written | `android/.../collection/Jumps.kt` |
| 5 | circling | A run of answers lying round a circle rather than along a road — the shape of a transmitter overhead rather than of a journey | `android/.../collection/Circling.kt` |
| 6 | circle limits | The eight numbers circling is judged by. Held in the group's settings so that narrowing them does not mean publishing a build | `Circling.kt`, `firestore.rules`, `settingNumbers()` |
| 7 | false place | A place the group has recorded as one where receivers are told a lie: a centre, a radius and a name. Read by everybody in the group, written by an administrator | `firestore.rules`, `match /falsePlaces/{placeId}`; `android/.../collection/FalsePlaces.kt` |
| 8 | geofence, dwelt | The fence around the place the phone last wrote, and the crossing that counts: entering changes nothing, staying does | `android/.../collection/Standing.kt` |

## What a phone keeps to itself

Not everything the application reasons about is in the database. These live on
the phone and are named here because the code names them.

| № | Term | Means | Defined in |
|---|------|-------|------------|
| 1 | profile told | What this phone has already had accepted about its member: the group, the account, the name, the picture and the device. Compared whole, so joining another group is an introduction from scratch | `android/.../map/PublishedProfile.kt` |
| 2 | device identity | The identifier a phone made for itself on first launch, used in its position id, its watch id and its member's grant | `android/.../service/DeviceIdentity.kt` |
| 3 | standing (current group) | The name of the group this phone writes into, with a counter that grows on every move, so that subscriptions rebuild even when the name comes back the same | `android/.../service/CurrentGroup.kt` |

## The clients and the stand-in stand

| № | Term | Means | Defined in |
|---|------|-------|------------|
| 1 | the application | The Android build: collects positions, writes them, and shows the group on a map | `android/` |
| 2 | the page | The web client on hosting: signs in and shows the same group on the same background, reading the same documents | `web/public/` |
| 3 | stand-in services | The Firestore and sign-in emulators a debug build talks to instead of the real project | `tools/run-services.sh`, `firebase/emulator/` |
| 4 | seed | What fills the stand-in database before a run: the accounts, a group with three of them on the roster, a profile and a grant each | `firebase/emulator/seed.mjs` |
| 5 | stand-in account | One of the five accounts the seed creates. `Parent`, `Spouse` and `Child` are on the roster of `family`; `Stranger` and `Newcomer` are on no roster at all and exist for the way into a group. These are names of test accounts, not roles: the role is `Outsider` | `seed.mjs`, `android/app/src/debug/.../StandInServices.kt` |

## Entities and relations

Everything lives under one group document. There is no collection outside
`groups`, and no rule reaching one: a request no `match` reaches is refused, so
the group is both the unit of access and the root of the data.

### The entities

**Group** — `groups/{groupId}`. Fields: `members` (array of account ids, at
most 50), `admins` (array, a subset of `members`, at least one after any write
an administrator makes). The id is the founder's account id for anything
founded on a phone. Created by its founder, updated either by somebody adding
themselves on an invitation or by an administrator editing the roster, and
never deleted — deleting it would leave the profiles, grants and positions
underneath it with nothing to say who may read them.

**Member** — not a document. A member is an account whose id appears in
`members`. Everything a member has in the database is keyed by that id in a
subcollection, which is why membership itself stays in the group document and
out of every client's reach: writing one's own profile grants nothing.

**Member profile** — `groups/{groupId}/members/{uid}`. Fields: `displayName`
(at most 100 characters), `photoUrl` (nullable, at most 500), `updatedAt`.
Written by the member it describes, read by everybody in the group. At most one
per member per group, and the same account in another group has told that group
nothing.

**Grant** — `groups/{groupId}/grants/{uid}`. Fields: `devices` (at most 10
device identifiers), `sharedWith` (a map of reader account id to expiry, at
most 50 entries). Written by its member; read by that member and by the readers
named inside it, one document at a time — the collection cannot be listed,
because handing a member the whole household's phones is the leak this
collection exists to close.

**Device** — not a document either. A device is an identifier a phone made for
itself; it appears in its member's `devices`, as the second half of a position
id and as the second half of a watch id.

**Position** — `groups/{groupId}/positions/{uid}.{deviceId}`. Fields:
`memberId`, `deviceName`, `latitude`, `longitude`, `takenAt`, `writtenAt`,
`mode`, `batteryPercent`, `charging`, `accuracyMetres`, `foundBy`, `settling`,
`leftOutCount`, `leftOutReason`, `leftOutSince`. One document per phone rather
than per member. Written by the account named in its id; read by that account
and by whoever holds an unexpired permission in the owner's grant.

**Watch claim** — `groups/{groupId}/watch/{uid}.{deviceId}`. Fields:
`memberId`, `since` (the server's clock). Written by the phone looking, read by
everybody in the group, deleted when the map is closed and otherwise left to
age out three minutes after it was made.

**Invitation** — `groups/{groupId}/invitations/{code}`. Fields: `createdBy`,
`expiresAt`. The code is the document's name rather than a field, so an
invitation cannot be found except by knowing it. Created and deleted by an
administrator, never updated — changing one in place would push out the expiry
of a code already handed round — and read by nobody at all except the rules.

**Join claim** — `groups/{groupId}/joins/{uid}`. Field: `code`, held to eight
characters of A–Z and 2–9 because the rules build a document path out of it.
Written and read by the account it is named after, and by the rules while they
weigh that account's own addition to the roster.

**False place** — `groups/{groupId}/falsePlaces/{placeId}`. Fields:
`latitude`, `longitude`, `radiusMetres` (at most 50 km), `named` (optional, at
most 100 characters). Read by everybody in the group, including somebody who
joined an hour ago; written by an administrator.

**Group setting** — `groups/{groupId}/settings/{key}`. Today one key,
`circling`, holding the eight numbers a run of answers is judged against:
`answers`, `withinSeconds`, `roundness`, `widestMetres`, `tightestMetres`,
`steadiness`, `turnedDegrees`, `slowestMps`. Read by everybody in the group,
because every phone weighs its own answers against it; written by an
administrator.

### How they relate

| № | From | To | How many | Tied by |
|---|------|----|----------|---------|
| 1 | Group | Member | one group holds 1–50 members | the `members` array |
| 2 | Group | Administrator | one group holds 0–50, and at least 1 after any administrator's write; every administrator is also a member | the `admins` array, checked against `members` |
| 3 | Member | Member profile | 0 or 1 per group | document id = account id |
| 4 | Member | Grant | 0 or 1 per group | document id = account id |
| 5 | Member | Device | 0–10 | the `devices` list of the grant |
| 6 | Device | Position | 0 or 1 | position id = `{account id}.{device id}` |
| 7 | Device | Watch claim | 0 or 1 at a time | watch id = `{account id}.{device id}` |
| 8 | Member | Permission | 0–50 readers, each with an expiry | the `sharedWith` map of the grant |
| 9 | Permission | Position | one permission opens every position of that member's devices | the rules read the owner's grant on each position read |
| 10 | Group | Invitation | 0 or more, each good for a day | subcollection; the code is the document name |
| 11 | Invitation | Join claim | one claim names one code; one code lets in everybody who uses it before it runs out | the `code` field, matched against the invitation's document name |
| 12 | Account | Join claim | 0 or 1 per group | document id = account id |
| 13 | Group | False place | 0 or more | subcollection |
| 14 | Group | Group setting | 0 or 1 today (`circling`) | subcollection, key checked by the rules |

### What holds them together

These are the invariants the rules enforce, stated as sentences because they
are what a reader of the data should be able to assume.

| № | Invariant | Enforced by |
|---|-----------|-------------|
| 1 | A group founded on a phone is named after its founder, who is alone on the roster and alone among its administrators at that moment | `founding()` |
| 2 | Nobody adds anybody but themselves, and only while holding a join claim naming an invitation that has not run out | `joining()`, `invitationStands()` |
| 3 | An administrator's write leaves at least one administrator, and every administrator on the roster | `administering()` |
| 4 | A roster never exceeds 50 members | `rosterFits()` |
| 5 | A profile, a grant, a join claim and a position belong to the account they are named after, and nobody else writes them | `isOwner()`, `isSelf()`, `ownsPosition()` |
| 6 | A position carries the account of its writer in `memberId`, and its `writtenAt` is the server's own clock | `positionShape()` |
| 7 | A position may be read only by its owner or by somebody holding an unexpired permission in the owner's grant, read live rather than copied | `mayRead()`, `grant()` |
| 8 | A grant is read one document at a time; the collection is never listed | `allow get` only on `grants/{uid}` |
| 9 | Invitations are read by nobody, and never updated once made | `allow read: if false`, `allow update: if false` |
| 10 | A watch claim carries the server's moment, so an expiry cannot be chosen by the viewer's clock | `watchShape()` |
| 11 | Nothing outside `groups` is reachable at all, and a subcollection stays shut until a rule names it | the absence of any `match /{document=**}` |

### Where each document lives

| № | Path | Who reads it | Who writes it |
|---|------|--------------|---------------|
| 1 | `groups/{groupId}` | members | founder on creation; a joiner adding themselves; administrators |
| 2 | `groups/{groupId}/members/{uid}` | members | the member it names |
| 3 | `groups/{groupId}/grants/{uid}` | the member it names, and the readers inside it | the member it names |
| 4 | `groups/{groupId}/positions/{uid}.{deviceId}` | the owner, and readers with an unexpired permission | the owner |
| 5 | `groups/{groupId}/watch/{uid}.{deviceId}` | members | the phone it names |
| 6 | `groups/{groupId}/invitations/{code}` | nobody; the rules read it | administrators |
| 7 | `groups/{groupId}/joins/{uid}` | the account it names; the rules read it | the account it names |
| 8 | `groups/{groupId}/falsePlaces/{placeId}` | members | administrators |
| 9 | `groups/{groupId}/settings/circling` | members | administrators |
