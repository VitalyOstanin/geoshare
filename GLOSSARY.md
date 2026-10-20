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
- [Words that carry more than one meaning here](#words-that-carry-more-than-one-meaning-here)
- [The group and who is in it](#the-group-and-who-is-in-it)
- [Getting into a group](#getting-into-a-group)
- [What a member publishes](#what-a-member-publishes)
- [Positions](#positions)
- [How a position was arrived at](#how-a-position-was-arrived-at)
- [Watching, and how old is too old](#watching-and-how-old-is-too-old)
- [Judging an answer before it is written](#judging-an-answer-before-it-is-written)
- [What a device keeps to itself](#what-a-device-keeps-to-itself)
- [The clients and the stand-in stand](#the-clients-and-the-stand-in-stand)
- [Entities and relations](#entities-and-relations)
  - [The entities](#the-entities)
  - [How they relate](#how-they-relate)
  - [What holds them together](#what-holds-them-together)
  - [Where each document lives](#where-each-document-lives)

## How to read this

A term is canonical when it appears in the "Term" column of a table below, or
in the "Prefer" column of the next section. Identifiers in code, fields in
documents, words shown to a member and words in comments are all meant to use
the canonical term rather than a synonym, so that one thing is called one thing
in the rules, in both clients and on screen.

Where a term names a field or a type, the definition is the code, and the
column beside it says where that code is. Where the two disagree, the code is
right and this file is out of date.

## Preferred wording

| №   | Prefer        | Not this                              | Why                                                                                                                                                                                                                                                                                                           |
|-----|---------------|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1   | member        | user, contact, friend                 | The person is in a group; "user" says nothing about which one. The field naming a person is `memberId`, and every document about one is named after their account                                                                                                                                             |
| 2   | roster        | member list, participants             | The roster is the `members` array of the group document. "Member list" would also collide with the strip of members the page draws under its map                                                                                                                                                              |
| 3   | administrator | moderator, admin                      | The array is `admins` and the word written out is administrator; on screen the same thing is said as "runs the group". Names in code may shorten it to `admin` as the array does — `AdminPanel`, `adminOpen`, `#admin-*` and `.admin-*` on the page — but text on screen, comments and documents write it out |
| 4   | invitation    | invite                                | The noun is `invitation`; the eight characters inside it are the code                                                                                                                                                                                                                                         |
| 5   | grant         | permission document, sharing settings | One document per member holding both halves of what they give out: their devices and their readers                                                                                                                                                                                                            |
| 6   | watch claim   | heartbeat, online status              | It says somebody had the map open a moment ago, not that they are online                                                                                                                                                                                                                                      |
| 7   | stale         | disconnected                          | A marker goes stale when its position is older than half an hour; nothing here knows whether a device is connected                                                                                                                                                                                            |
| 8   | false place   | spoof zone, jamming area              | A place where receivers are known to be told a lie, as the group recorded it                                                                                                                                                                                                                                  |
| 9   | join claim    | join request                          | It is a document the joiner writes for the rules to read; nothing asks anybody for anything                                                                                                                                                                                                                   |
| 10  | project owner | organiser, founder                    | The person who set up the group's Firebase project and holds it. "Founder" is a group's first administrator, a role in the database; the project owner is a role outside it, over the whole project                                                                                                           |
| 11  | device        | phone                                 | geoshare runs on any Android device, a tablet included, and the screens say device; "phone" is left to code identifiers that predate the word                                                                                                                                                                 |
| 12  | in touch      | online                                | On screen, `knownAt` of a position and `onlineAt` of a device list are both called "in touch" ("на связи"); "online" is not used for them. The word promises only that the device was heard from at that moment, not that it is connected now                                                                 |

A word from the platform's own API is not a synonym and is left alone:
`currentUser` from Firebase, `Location` and its permissions from Android, a
notification channel. These name what the platform calls them.

## Words that carry more than one meaning here

Several everyday words are already taken by something specific. They are not
banned — they are worth knowing before reaching for one.

| №   | Word              | What it means here                                                                                                                                                                                                            | Not this                                                                        |
|-----|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| 1   | fix, position     | A fix is one answer from a provider; a position is a fix the device decided to write                                                                                                                                          | either word for the other: most fixes never become positions                    |
| 2   | circle            | The shape a spoofed run of answers is fitted to                                                                                                                                                                               | a group of people                                                               |
| 3   | owner             | The account a document is named after, who may write it                                                                                                                                                                       | somebody who runs the group — that is an administrator                          |
| 4   | key               | The name of a document in `settings`, and the API key in the emulator's URLs                                                                                                                                                  | a code, which is what an invitation is opened with                              |
| 5   | subscription      | A listener on the database                                                                                                                                                                                                    | a permission, which is one entry of `sharedWith`                                |
| 6   | share, sharing    | Giving out one's own position: `sharedWith`, "Share my position"                                                                                                                                                              | a permission, which is one entry of that map                                    |
| 7   | rejected, dropped | Prose for an answer the device threw away                                                                                                                                                                                     | the fields, which are `leftOutCount`, `leftOutReason` and `leftOutSince`        |
| 8   | point             | The place this device reports, as the settings screen says it                                                                                                                                                                 | a position document                                                             |
| 9   | standing          | Where an account stands with respect to a group: `Ungrouped`, `Outsider`, `Member`, `Administrator`, `Unrun`. One meaning only — the group this device writes into is a `Writing`, and how a permission stands is a `Sharing` | either of those two, which were called standings until the words were separated |
| 10  | refused           | The rules answering that this client may not ask: `PERMISSION_DENIED`. What a client already holds is untrue from then on, and a subscription given something to forget says so                                               | a connection that dropped, after which the last answer is as true as it was     |

The verb is free: a device standing still, a standing question, a refusal still
standing. What is held to one meaning is the noun.

Two more words are deliberately not restricted. **Household** is used in prose
for the people in a group, where "group" would read as the document; it names
no field. **Family** appears as the name of the group that predates
invitations — the one whose document id is literally `family` — and as the
project's own description of who it is for.

## The group and who is in it

| №   | Term            | Means                                                                                                                                                                                                                                                                                                                | Defined in                                                                                                                                                  |
|-----|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1   | group           | The document everything else hangs under, and the unit of access: reading anything at all requires being on its roster                                                                                                                                                                                               | `firebase/firestore.rules`, `match /groups/{groupId}`                                                                                                       |
| 2   | group id        | The name of that document. For a group founded on a device it is the account id of the founder; the household that predates invitations is named `family`                                                                                                                                                            | `firestore.rules`, `founding()`                                                                                                                             |
| 3   | roster          | The `members` array: the account ids of everybody in the group. At most 50                                                                                                                                                                                                                                           | `firestore.rules`, `rosterFits()`                                                                                                                           |
| 4   | administrator   | An account in the `admins` array. Hands out invitations, puts members on the roster and takes them off, hands the running of the group to another member, and edits the group's settings and its false places                                                                                                        | `firestore.rules`, `administering()`                                                                                                                        |
| 5   | standing        | Where the account signed in stands with respect to a group, as the screens decide what to offer: `Ungrouped`, `Outsider`, `Member`, `Administrator`, `Unrun`. The page names four of them — an account outside a group and a group not yet made are one answer there, because from outside they cannot be told apart | `android/.../membership/Roster.kt`, `web/public/lib/membership.js`                                                                                          |
| 6   | unrun group     | A group with an empty `admins` array: nobody may change its roster, and the screens say so rather than offering controls every write would refuse                                                                                                                                                                    | `Roster.kt`, `firestore.rules`                                                                                                                              |
| 7   | member id       | An account id, as the sign-in provider issues it. The name of a profile, of a grant and of a join claim, the first half of a position id, and a field of its own inside a position and a watch claim                                                                                                                 | `android/.../auth/AuthState.kt`                                                                                                                             |
| 8   | current group   | Which group this device writes into, remembered on the device itself and given out as a writing with a turn counter, so that founding or joining rebuilds every subscription                                                                                                                                         | `android/.../service/CurrentGroup.kt`                                                                                                                       |
| 9   | project owner   | The Google account that created the group's Firebase project and holds it. Reads everything from the console past the rules and deploys them; hands out the connection link. On the device they are a member like any other, usually the group's first administrator                                                 | `docs/ADR/0005-a-project-per-group.md`; `docs/ADR/0033-a-setup-page-finishes-the-project.md`                                                                |
| 10  | connection link | `https://<site>.web.app/join`: what points a device at a group's project. With an invitation after the `#` it also carries the group id, the code and the captions — `g`, `c`, `by`, `n`, `to` — so one link both connects and lets the person join                                                                  | `tools/project.sh`, `web/public/setup-app.js`; `docs/ADR/0001-one-build-settings-at-runtime.md`; `docs/ADR/0019-making-an-invitation-and-naming-a-group.md` |
| 11  | group name      | The `name` field of the group document: a caption administrators give the group and change, at most 60 characters. It decides nothing; a group without one is shown as the founder's group, "Group · Anna"                                                                                                           | `firestore.rules`, `nameFits()` and `renaming()`; `android/.../map/GroupCaption.kt`, `web/public/lib/group-name.js`                                         |
| 12  | setup page      | The page on the developer's own site, `https://geoshare-setup.web.app/`, that finishes a project the project owner has created in the Firebase console: the database, the rules, the applications, the group's site and its settings. It ends with the connection link                                               | `web/public/setup.html`, `web/public/setup-app.js`; `docs/ADR/0033-a-setup-page-finishes-the-project.md`                                                    |

## Getting into a group

| №   | Term              | Means                                                                                                                                                                                                                                                                                   | Defined in                                                                            |
|-----|-------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| 1   | invitation        | A document whose name is the code, holding who made it, when it runs out and the Google address it is for. It lets in only an account signed in with that verified address, as many times as it is used before it runs out. Nobody may read it — the rules read it on a writer's behalf | `firestore.rules`, `match /invitations/{code}`, `invitedAccount()`                    |
| 2   | code              | Eight characters. The rules accept A–Z and 2–9; what is handed out is drawn from those minus the letters read for digits, so a code can be read aloud                                                                                                                                   | `android/.../membership/Codes.kt`                                                     |
| 3   | invitation text   | The group id and the code joined by a dot, because a write has to know its path before it is made. What was sent before invitations named an address; still read when pasted, no longer handed out — the connection link is sent instead                                                | `android/.../membership/Codes.kt`, `web/public/lib/membership.js`, `invitationFrom()` |
| 4   | join claim        | A document the person joining writes under their own account id, holding the code they are using. It is how a code reaches the rules, which cannot be told anything except through the database                                                                                         | `firestore.rules`, `match /joins/{uid}`                                               |
| 5   | joining           | The write that adds one's own account to the roster and changes nothing else, allowed only while a join claim names an invitation that has not run out                                                                                                                                  | `firestore.rules`, `joining()`                                                        |
| 6   | founding          | The write that creates a group: named after the founder, with the founder alone on the roster and alone among the administrators, in one batch with `project/group`, so once per project                                                                                                | `firestore.rules`, `founding()`                                                       |
| 7   | shortened address | The invited address as a link and the screens show it: the first four characters of the name, or only the first when the name is four or shorter, then `…` and the domain whole. The full address is known to the rules alone                                                           | `android/.../project/ConnectLink.kt`, `web/public/lib/join.js`, `shortenedAddress()`  |

## What a member publishes

| №   | Term         | Means                                                                                                                                                                                  | Defined in                                                                    |
|-----|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| 1   | profile      | The name and the account picture of one member, written by that member and read by the whole group, so a marker can carry a person rather than the first characters of an id           | `firestore.rules`, `match /members/{uid}`; `android/.../map/MemberProfile.kt` |
| 2   | display name | The name the account carries. A member whose account has none writes no profile at all                                                                                                 | `MemberProfile.kt`, `SignInController.kt`                                     |
| 3   | grant        | One document per member holding what they give out: the devices they carry and the readers they allow                                                                                  | `firestore.rules`, `match /grants/{uid}`; `android/.../map/MemberGrant.kt`    |
| 4   | device       | One Android device, phone or tablet, as the data knows it: an identifier the device made for itself on first launch. Prose calls it a device as well; geoshare is not for phones alone | `android/.../service/DeviceIdentity.kt`                                       |
| 5   | devices      | The identifiers of the devices this member carries. A reader learns from here which position documents to read, because the collection of positions cannot be listed. At most 10       | `MemberGrant.kt`                                                              |
| 6   | shared with  | The readers this member allows, each with the moment their permission runs out. At most 50                                                                                             | `MemberGrant.kt`                                                              |
| 7   | permission   | One entry of `sharedWith`: one reader and one expiry. Weighed against the server's clock on every read of a position                                                                   | `firestore.rules`, `mayRead()`                                                |
| 8   | sharing      | How a permission stands, as the screen has to say it: `Never` for none or one that has run out, `Endless` for one with no end named, `Until` for one counting down                     | `android/.../map/ShareSpan.kt`, `Sharing`                                     |
| 9   | span         | The length a permission is given for: nothing, an hour, eight hours, a day, or without an end. What a member picks; the moment it ends is worked out from it                           | `ShareSpan.kt`, `ShareSpan`                                                   |

## Positions

| №   | Term                          | Means                                                                                                                                                                                                                                                         | Defined in                                                                              |
|-----|-------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| 1   | fix                           | One answer from a provider, as it arrived. Most are never written: a fix becomes a position only once the device has decided it is worth saying                                                                                                               | `android/.../collection/WriteFilter.kt`                                                 |
| 2   | position                      | Where one device last said it was, as one document: the place, the moments, how it was found and how the device is doing                                                                                                                                      | `firestore.rules`, `match /positions/{positionId}`; `android/.../map/MemberPosition.kt` |
| 3   | position id                   | The account id and the device identifier joined by a dot. One document per device, so two devices of one member do not overwrite each other                                                                                                                   | `MemberPosition.kt`, `firestore.rules`, `ownsPosition()`                                |
| 4   | taken at                      | When the fix was taken, by the clock of the device that took it                                                                                                                                                                                               | `MemberPosition.kt`                                                                     |
| 5   | written at                    | When the database accepted the write, by the server's clock. The one clock every member shares, so it is what the news is dated by — the age a card shows and the half hour after which a marker goes grey. The moment of the position itself is capped by it | `firestore.rules`, `positionShape()`; `web/public/lib/age.js`, `positionMoments()`      |
| 6   | device name                   | What a device calls itself, shown when a member has more than one on the map                                                                                                                                                                                  | `MemberPosition.kt`                                                                     |
| 7   | accuracy, width               | How wide the provider said its answer is, in metres. Drawn as a ring around the marker; absent where the provider named none                                                                                                                                  | `MemberPosition.kt`, `web/public/lib/width.js`                                          |
| 8   | left out count, reason, since | What a device says while its answers are being thrown away and it is therefore writing no positions: how many, why, and since when. Without it a device under a transmitter looks like a device in a drawer                                                   | `firestore.rules`, `leftOutShape()`                                                     |
| 9   | settling                      | Whether the receiver had not yet settled since the device was switched on                                                                                                                                                                                     | `firestore.rules`, `positionShape()`                                                    |

## How a position was arrived at

| №   | Term            | Means                                                                                                                                                                                 | Defined in                                 |
|-----|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------|
| 1   | found by        | How the device says it reached the answer, in this project's own words rather than in the provider's                                                                                  | `android/.../collection/FoundBy.kt`        |
| 2   | Satellites      | Worked out from satellites the receiver was hearing at the time                                                                                                                       | `FoundBy.kt`                               |
| 3   | Assistance      | Worked out elsewhere by an assistance service, from the towers and the Wi-Fi around the device, and sent over the network                                                             | `FoundBy.kt`                               |
| 4   | Unheard         | Credited to satellites the receiver measured nothing off                                                                                                                              | `FoundBy.kt`                               |
| 5   | OtherRadios     | Worked out from the towers and the Wi-Fi, by the device or by a service                                                                                                               | `FoundBy.kt`                               |
| 6   | Pretended       | Put there by another application on the device                                                                                                                                        | `FoundBy.kt`                               |
| 7   | Unsaid          | Arrived with nothing said about how it was reached                                                                                                                                    | `FoundBy.kt`                               |
| 8   | source switch   | What a member turns off in the settings, as one switch per kind of answer: `SourceChoice`. An answer from a source switched off is still collected and logged, and it moves no marker | `android/.../collection/Sources.kt`        |
| 9   | collection mode | How hard the device is working and how readily it says what it found: `Idle`, `Standby`, `Moving`, `Viewing`, `Night`. `Viewing` is the only one that turns the receiver on           | `android/.../collection/CollectionMode.kt` |

## Watching, and how old is too old

| №   | Term        | Means                                                                                                                                                                                                      | Defined in                                                                        |
|-----|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| 1   | watch claim | A document saying that one device had the map open, written per device and read by the whole group. It carries the moment it was made, by the server's clock; the devices add the seven minutes themselves | `firestore.rules`, `match /watch/{watchId}`; `android/.../collection/Watching.kt` |
| 2   | watcher     | A member behind a claim that has not run out — what the strip under the map names                                                                                                                          | `Watching.kt`, `web/public/lib/watchers.js`                                       |
| 3   | stale       | A position half an hour old, counted from the moment the server stamped it. The marker is drawn grey, and every mode that asks for anything has to write again before that                                 | `android/.../collection/Freshness.kt`, `web/public/lib/age.js`                    |
| 4   | following   | How the map moves itself: after one member, after everybody, or not at all                                                                                                                                 | `web/public/lib/following.js`, `android/.../map/Following.kt`                     |

## Judging an answer before it is written

The device decides whether an answer is worth writing. These are the terms that
decision is made in.

| №  | Term                | Means                                                                                                                                                                                                                                                                          | Defined in                                                                                 |
|----|---------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| 1  | trail               | The places the device has actually been — written positions only, never answers that were left out, so an hour of a false signal never becomes familiar. 50 places, three days                                                                                                 | `android/.../collection/Trail.kt`                                                          |
| 2  | familiar place      | Somewhere the trail holds three visits within 300 metres, spread over at least an hour                                                                                                                                                                                         | `Trail.kt`                                                                                 |
| 3  | anchor              | The place a new answer is measured against after a restart. Good for an hour, or for twelve where it stands in a familiar place                                                                                                                                                | `Trail.kt`                                                                                 |
| 4  | jump                | An answer that would have taken more than the allowed speed to reach from the anchor. Left out rather than written                                                                                                                                                             | `android/.../collection/Jumps.kt`                                                          |
| 5  | circling            | A run of answers lying round a circle rather than along a road — the shape of a transmitter overhead rather than of a journey                                                                                                                                                  | `android/.../collection/Circling.kt`                                                       |
| 6  | circle limits       | The eight numbers circling is judged by. Held in the group's settings so that narrowing them does not mean publishing a build                                                                                                                                                  | `Circling.kt`, `firestore.rules`, `settingNumbers()`                                       |
| 7  | false place         | A place the group has recorded as one where receivers are told a lie: a centre, a radius and a name. Read by everybody in the group, written by an administrator                                                                                                               | `firestore.rules`, `match /falsePlaces/{placeId}`; `android/.../collection/FalsePlaces.kt` |
| 8  | geofence, dwelt     | The fence around the place the device last wrote, and the crossing that counts: entering changes nothing, staying does                                                                                                                                                         | `android/.../collection/Geofence.kt`                                                       |
| 9  | foundBy, logWording | `Judgement.foundBy` is the found-by of the fix just judged. `logWording` is the plain-English wording of a found-by that the log uses; the clients have their own, translated                                                                                                  | `android/.../collection/Judgement.kt`, `FoundBy.kt`                                        |
| 10 | recentAnswers       | The recent answers of the satellites, newest last, written or left out. Route repetition reads it, because the answers a rule refused are the ones that draw the route                                                                                                         | `android/.../collection/Repeats.kt`, `Judgement.kt`                                        |
| 11 | restsOn             | The satellites the fix rests on: for each constellation, how many were heard and how many went into the fix. Empty where the epoch is missing or too old, which says nothing either way. `restsOnSatellites`, `restsOnNoSatellite` and `restsOnOneBand` ask it a question each | `android/.../collection/Constellations.kt`, `ProviderNote.kt`, `Judgement.kt`              |
| 12 | watchEnds           | The moments up to which each watch claim still counts, one per watcher. The mode is chosen from them: any one still ahead means somebody is looking                                                                                                                            | `android/.../service/ModeReview.kt`, `collection/ModeSelection.kt`                         |
| 13 | saved place         | A place the provider kept from an earlier answer and hands over as if it were current. An answer more than ten minutes behind the device's clock is taken for one and left out with the reason `Saved`                                                                         | `android/.../collection/Judgement.kt`                                                      |

## What a device keeps to itself

Not everything the application reasons about is in the database. These live on
the device and are named here because the code names them.

| №   | Term            | Means                                                                                                                                                                                              | Defined in                                                    |
|-----|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| 1   | profile told    | What this device has already had accepted about its member: the group, the account, the name, the picture and the device. Compared whole, so joining another group is an introduction from scratch | `android/.../map/PublishedProfile.kt`                         |
| 2   | device identity | The identifier a device made for itself on first launch, used in its position id, its watch id and its member's grant                                                                              | `android/.../service/DeviceIdentity.kt`                       |
| 3   | writing         | Which group this device is writing into: the name, with a counter that grows on every move, so that subscriptions rebuild even when the name comes back the same                                   | `android/.../service/CurrentGroup.kt`, `CurrentGroup.Writing` |

## The clients and the stand-in stand

| №   | Term              | Means                                                                                                                                                                                                                                                              | Defined in                                                 |
|-----|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| 1   | the application   | The Android build: collects positions, writes them, and shows the group on a map                                                                                                                                                                                   | `android/`                                                 |
| 2   | the page          | The web client on hosting: signs in and shows the same group on the same background, reading the same documents                                                                                                                                                    | `web/public/`                                              |
| 3   | stand-in services | The Firestore and sign-in emulators a debug build talks to instead of the real project                                                                                                                                                                             | `tools/run-services.sh`, `firebase/emulator/`              |
| 4   | seed              | What fills the stand-in database before a run: the accounts, a group with three of them on the roster, a profile and a grant each, and a position for the two the device under test is meant to be looking at                                                      | `firebase/emulator/seed.mjs`                               |
| 5   | stand-in account  | One of the five accounts the seed creates. `Parent`, `Spouse` and `Child` are on the roster of `family`; `Stranger` and `Newcomer` are on no roster at all and exist for the way into a group. These are names of test accounts, not roles: the role is `Outsider` | `seed.mjs`, `android/app/src/debug/.../StandInServices.kt` |

## Entities and relations

Everything lives under one group document, and a project holds one group
(ADR-0036). Outside `groups` only `project/group` is reachable, and only to
read — a request no `match` reaches is refused, whatever else the project
keeps in the same database — so the group is both the unit of access and the
root of the data.

### The entities

**Group** — `groups/{groupId}`. Fields: `members` (array of account ids, at
most 50), `admins` (array, a subset of `members`, at least one after any write
an administrator makes). The id is the founder's account id for anything
founded on a device. Created by its founder, updated either by somebody adding
themselves on an invitation or by an administrator editing the roster, and
never deleted — deleting it would leave the profiles, grants and positions
underneath it with nothing to say who may read them.

**Project's group** — `project/group`. One field, `group`: the id of the one
group the project holds. Written in the batch that founds the group and never
changed or deleted from a client; any signed-in account may read it, which is
how a client knows not to offer founding a second group.

**Member** — not a document. A member is an account whose id appears in
`members`. Everything a member has in the database is keyed by that id in a
subcollection, which is why membership itself stays in the group document,
where no client may set it for themselves: writing one's own profile grants
nothing.

**Member profile** — `groups/{groupId}/members/{uid}`. Fields: `displayName`
(at most 100 characters), `photoUrl` (nullable, at most 500), `updatedAt` (when
the profile was last written).
Written by the member it describes, read by everybody in the group. At most one
per member per group, and the same account in another group has told that group
nothing.

**Grant** — `groups/{groupId}/grants/{uid}`. Fields: `devices` (at most 10
device identifiers), `sharedWith` (a map of reader account id to expiry, at
most 50 entries). Written by its member; read by that member and by the readers
named inside it, one document at a time — the collection cannot be listed,
because handing a member the whole household's devices is the leak this
collection exists to close.

**Device** — not a document either. A device is an identifier the device made for
itself; it appears in its member's `devices`, as the second half of a position
id and as the second half of a watch id.

**Position** — `groups/{groupId}/positions/{uid}.{deviceId}`. Fields:
`memberId`, `latitude`, `longitude`, `takenAt`, `writtenAt`, `mode`,
`batteryPercent`, `charging`, `accuracyMetres`, `foundBy`, `settling`,
`leftOutCount`, `leftOutReason`, `leftOutSince`. One document per device rather
than per member. Written by the account named in its id; read by that account
and by whoever holds an unexpired permission in the owner's grant.

**Watch claim** — `groups/{groupId}/watch/{uid}.{deviceId}`. Fields:
`memberId`, `since` (the server's clock). Written by the device looking,
refreshed every five minutes while the map is up and touched now and then,
and deleted when it is closed or left untouched for five minutes (ADR-0040). A
claim nobody deleted stays in the database and simply stops counting: the
readers work out who is looking by leaving out claims older than seven minutes,
rather than by anything arriving at the moment one runs out.

**Invitation** — `groups/{groupId}/invitations/{code}`. Fields: `createdBy`,
`expiresAt`, `email` (the Google address it is for; invitations made before
the field existed have none and let in any account). The code is the document's name rather than a field, so an
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
because every device weighs its own answers against it; written by an
administrator.

**Device list** — `groups/{groupId}/devices/{uid}`. One entry per device of
the member, under the device's id: `model`, `name`, `onlineAt` (the server's
clock), `mode`, `batteryPercent`, `charging`. Nothing in it is a place, so it is
written apart from the position and is not held back with it. Written by its
member; read one document at a time by that member, by the readers their grant
names, and by the administrators. At most ten entries.

**Labels** — `groups/{groupId}/labels/{uid}`. Fields: `devices` (a device's
address `{uid}.{deviceId}` to the name this member gives it, at most 500) and
`members` (a member's id to the name this member gives them, at most 50). A
member's own names for the others, read and written by that member alone.

**Search** — `groups/{groupId}/searches/{uid}.{deviceId}`. Fields: `openedBy`,
`startedAt`, `renewedBy`, `renewedAt` (both times the server's), `hours` (1 to
72), `phoneName` (optional). While it stands, everybody in the group reads that
device's position whatever the grant says. Read by everybody in the group;
opened and renewed by an administrator; called off by deleting it, by any
administrator or by the member whose device it is.

**Siren** — `groups/{groupId}/sirens/{uid}.{deviceId}`. Fields: `calledBy`,
`calledAt`, `heardAt`, `stoppedBy`, `stoppedAt`. A call that makes the device
sound for twenty seconds. Written by an administrator for any device, by a
member for their own; the device adds `heardAt`. Read by the same people and by
nobody else. Never deleted, only called again.

**Reset** — `groups/{groupId}/resets/{uid}.{deviceId}`. Fields: `askedBy`,
`askedAt`, `doneAt`. A request that the device forget the last place it
believed — the anchor a *jump* is measured from — pressed from somebody else's
screen. The same writers and readers as a siren; the device adds `doneAt`.
Never deleted, only asked again.

### How they relate

| №   | From       | To             | How many                                                                                                   | Tied by                                                          |
|-----|------------|----------------|------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| 1   | Group      | Member         | one group holds 1–50 members                                                                               | the `members` array                                              |
| 2   | Group      | Administrator  | one group holds 0–50, and at least 1 after any administrator's write; every administrator is also a member | the `admins` array, checked against `members`                    |
| 3   | Member     | Member profile | 0 or 1 per group                                                                                           | document id = account id                                         |
| 4   | Member     | Grant          | 0 or 1 per group                                                                                           | document id = account id                                         |
| 5   | Member     | Device         | 0–10                                                                                                       | the `devices` list of the grant                                  |
| 6   | Device     | Position       | 0 or 1                                                                                                     | position id = `{account id}.{device id}`                         |
| 7   | Device     | Watch claim    | 0 or 1 at a time                                                                                           | watch id = `{account id}.{device id}`                            |
| 8   | Member     | Permission     | 0–50 readers, each with an expiry                                                                          | the `sharedWith` map of the grant                                |
| 9   | Permission | Position       | one permission opens every position of that member's devices                                               | the rules read the owner's grant on each position read           |
| 10  | Group      | Invitation     | 0 or more, each carrying its own expiry; the application sets a day                                        | subcollection; the code is the document name                     |
| 11  | Invitation | Join claim     | one claim names one code; one code lets in everybody who uses it before it runs out                        | the `code` field, matched against the invitation's document name |
| 12  | Account    | Join claim     | 0 or 1 per group                                                                                           | document id = account id                                         |
| 13  | Group      | False place    | 0 or more                                                                                                  | subcollection                                                    |
| 14  | Group      | Group setting  | 0 or 1 today (`circling`)                                                                                  | subcollection, key checked by the rules                          |
| 15  | Member     | Device list    | 0 or 1 per group                                                                                           | document id = account id                                         |
| 16  | Member     | Labels         | 0 or 1 per group                                                                                           | document id = account id                                         |
| 17  | Device     | Search         | 0 or 1                                                                                                     | search id = `{account id}.{device id}`                           |
| 18  | Device     | Siren          | 0 or 1                                                                                                     | siren id = `{account id}.{device id}`                            |
| 19  | Device     | Reset          | 0 or 1                                                                                                     | reset id = `{account id}.{device id}`                            |

### What holds them together

These are the invariants the rules enforce, stated as sentences because they
are what a reader of the data should be able to assume.

| №   | Invariant                                                                                                                                                                                                   | Enforced by                                          |
|-----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| 1   | A group founded on a device is named after its founder, who is alone on the roster and alone among its administrators at that moment                                                                        | `founding()`                                         |
| 2   | Somebody outside the group adds themselves and nobody else, and only while holding a join claim naming an invitation that has not run out. Putting another person on the roster is an administrator's write | `joining()`, `invitationStands()`, `administering()` |
| 3   | An administrator's write leaves at least one administrator, and every administrator on the roster                                                                                                           | `administering()`                                    |
| 4   | A roster never exceeds 50 members                                                                                                                                                                           | `rosterFits()`                                       |
| 5   | A profile, a grant, a join claim and a position belong to the account they are named after, and nobody else writes them                                                                                     | `isOwner()`, `isSelf()`, `ownsPosition()`            |
| 6   | A position carries the account of its writer in `memberId`, and its `writtenAt` is the server's own clock                                                                                                   | `positionShape()`                                    |
| 7   | A position may be read only by its owner or by somebody holding an unexpired permission in the owner's grant, read live rather than copied                                                                  | `mayRead()`, `grant()`                               |
| 8   | A grant is read one document at a time; the collection is never listed                                                                                                                                      | `allow get` only on `grants/{uid}`                   |
| 9   | Invitations are read by nobody, and never updated once made                                                                                                                                                 | `allow read: if false`, `allow update: if false`     |
| 10  | A watch claim carries the server's moment, so an expiry cannot be chosen by the viewer's clock                                                                                                              | `watchShape()`                                       |
| 11  | Nothing outside `groups` is reachable at all, and a subcollection stays shut until a rule names it                                                                                                          | the absence of any `match /{document=**}`            |

### Where each document lives

| №   | Path                                          | Who reads it                                                    | Who writes it                                                   |
|-----|-----------------------------------------------|-----------------------------------------------------------------|-----------------------------------------------------------------|
| 1   | `groups/{groupId}`                            | members                                                         | founder on creation; a joiner adding themselves; administrators |
| 2   | `groups/{groupId}/members/{uid}`              | members                                                         | the member it names                                             |
| 3   | `groups/{groupId}/grants/{uid}`               | the member it names, and the readers inside it                  | the member it names                                             |
| 4   | `groups/{groupId}/positions/{uid}.{deviceId}` | the owner, and readers with an unexpired permission             | the owner                                                       |
| 5   | `groups/{groupId}/watch/{uid}.{deviceId}`     | members                                                         | the device it names                                             |
| 6   | `groups/{groupId}/invitations/{code}`         | nobody; the rules read it                                       | administrators                                                  |
| 7   | `groups/{groupId}/joins/{uid}`                | the account it names; the rules read it                         | the account it names                                            |
| 8   | `groups/{groupId}/falsePlaces/{placeId}`      | members                                                         | administrators                                                  |
| 9   | `groups/{groupId}/settings/circling`          | members                                                         | administrators                                                  |
| 10  | `groups/{groupId}/devices/{uid}`              | the member it names, the readers in their grant, administrators | the member it names                                             |
| 11  | `groups/{groupId}/labels/{uid}`               | the member it names                                             | the member it names                                             |
| 12  | `groups/{groupId}/searches/{uid}.{deviceId}`  | members                                                         | administrators; deleted also by the member it names             |
| 13  | `groups/{groupId}/sirens/{uid}.{deviceId}`    | administrators, the member it names                             | administrators, the member it names; the device adds `heardAt`  |
| 14  | `groups/{groupId}/resets/{uid}.{deviceId}`    | administrators, the member it names                             | administrators, the member it names; the device adds `doneAt`   |
