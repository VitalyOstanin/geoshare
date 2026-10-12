# Document model

One document per member for what a member is, and one per device for what a
device does. History is deliberately not kept: each new fix overwrites the
previous one, which keeps both storage and read counts flat.

```
groups/{groupId}                            members: [uid, ...], admins: [uid, ...], owner?
groups/{groupId}/invitations/{code}         a code that lets one more person in
groups/{groupId}/joins/{uid}                the code somebody is holding, while they join
groups/{groupId}/falsePlaces/{placeId}      where a false signal is known to put devices
groups/{groupId}/settings/{key}             what the group has settled about judging answers
groups/{groupId}/searches/{uid}.{deviceId}  the group looking for a device, which opens its place to all of it
groups/{groupId}/sirens/{uid}.{deviceId}    a device told to sound loud for a while, so it can be found
groups/{groupId}/resets/{uid}.{deviceId}    a device told to forget its anchor and judge its place afresh
groups/{groupId}/members/{uid}              what a member says about themselves
groups/{groupId}/grants/{uid}               who may watch a member (and, for older builds, their devices)
groups/{groupId}/devices/{uid}              a member's devices and what each last said about itself
groups/{groupId}/labels/{uid}               what a member calls the others' devices and people, for themselves
groups/{groupId}/positions/{uid}.{deviceId} last known position of one device
groups/{groupId}/watch/{uid}.{deviceId}     viewing marker of one device
```

The id of a position is the account and the device joined by a dot. The rules
read the part before the dot, so a member writes only their own documents; the
readers group the markers by the `memberId` inside, which is what gives a device
its owner's colour, face and name.

A device's `deviceId` is the id the system gives it for this application
(`ANDROID_ID`), which survives a reinstall, so the names given to the device and
its place on the map survive it too. Where the system gives none, the
application makes a random one and keeps it. A device that went by a random id
before takes back, once, everything it wrote under it: the position, the viewing
marker, its entry in `devices` and its place in `grants.devices`.

The group document is the roster. A member's own document under it is the
opposite of the roster: they write it and nobody else does.

## Contents

- [groups/{groupId}](#groupsgroupid)
- [invitations/{code}](#invitationscode)
- [joins/{uid}](#joinsuid)
- [falsePlaces/{placeId}](#falseplacesplaceid)
- [settings/{key}](#settingskey)
- [searches/{uid}.{deviceId}](#searchesuiddeviceid)
- [sirens/{uid}.{deviceId}](#sirensuiddeviceid)
- [resets/{uid}.{deviceId}](#resetsuiddeviceid)
- [members/{uid}](#membersuid)
- [grants/{uid}](#grantsuid)
- [devices/{uid}](#devicesuid)
- [labels/{uid}](#labelsuid)
- [positions/{uid}.{deviceId}](#positionsuiddeviceid)
- [watch/{uid}.{deviceId}](#watchuiddeviceid)
- [Collection modes](#collection-modes)

## groups/{groupId}

| Field     | Type   | Meaning                                                              |
|-----------|--------|----------------------------------------------------------------------|
| `members` | list   | Who is in the group; every other rule reads membership here           |
| `admins`  | list   | Who may change the roster besides the founder; a subset of `members` |
| `owner`   | string | The founder, where the group is not named after them; console only   |
| `name`    | string | What people call the group; optional, 1–60 characters (ADR-0016)     |

A group is created by the person who will run it, alone on both lists, and is
named after their account: the rules allow a group to be made under the
founder's own id and under no other name. A name of their choosing would let
one account fill the database with groups nobody is in, a document apiece and
each of them free to make; a name that is the account itself gives everybody
exactly one group to make. It follows that the name is not a secret and not
something anybody types: an invitation carries it beside the code, as the one
string `group.code`, because the rule weighing somebody's addition to a roster
weighs a write to a path, and the path has to be known before the write is
made.

The household that started before any of this has a group named `family`, and
it keeps that name: the rule names the founder only while a group is being
created, and that one was created before the rule existed.

After that the roster changes in exactly three ways, and the rules allow
nothing else:

1. **Somebody joins.** The write adds the caller and only the caller, leaves
   every other field alone, and is allowed only while an invitation they hold
   is on file and has not run out.
2. **An administrator edits it.** They add or remove members and hand the
   running of the group to another member. The administrators have to stay a
   non-empty subset of the members, so the last one cannot resign into thin air
   and somebody outside the group cannot go on running it.

   The founder — the member the group is named after — may do all of that.
   Any other administrator takes off the roster only a member who runs
   nothing, so removing an administrator is two writes: the role first, the
   place second. Nor can they touch the founder's role or place. The founder
   owns the project and reads the whole database from the console anyway, so
   nothing is hidden from them; the other administrators own nothing.
3. **Somebody leaves.** The write takes the caller, and only the caller, off
   `members` and `admins` and changes nothing else. An administrator may not
   leave a roster nobody could change any more: after them there is still an
   administrator, or the founder is still on it. A member who runs nothing
   leaves any group. The application sends it in one batch with the deletion
   of what the member wrote there — their profile, grants, devices, labels and
   every device's position and watch — and the rules weigh each of those
   deletions on its own, as the member's own data (ADR-0026).

Somebody an administrator takes off the roster is no longer shown to anyone by
the permissions they gave: a position, a grant or a list of devices is read by
a permission only while its owner is on the roster, since they can no longer
withdraw it themselves. They can still delete their own profile, grant, devices
and positions, which is how a device that finds itself taken off clears what it
left behind.

The group document is never deleted: its members, profiles, grants and
positions would be left behind with nothing to say who may read them.

The founder is whoever `owner` names, or else the account the group is named
after. The founder runs the group without being listed in `admins`, as long as
they are on the roster, and nobody else takes them off it or out of the running
of it; an administrator who is not the founder takes another administrator off
the roster in two writes, the role first. `admins` may be empty only while the
founder is on the roster. A write from a client changes `members` and `admins`,
or `name` alone, and nothing else, so `owner` and any other field of the
document are written from the console.

`name` is given, if at all, when the group is founded, and an administrator
changes it later in a write of its own. It decides nothing: until there is one
the screens call the group after its founder (ADR-0019).

A household that started before invitations existed has no `admins` field and
no `owner`. Its roster is therefore still edited from the console, which is
what the rules do when nobody is named: `admins` is read through a default of
nothing, and nothing is what may change the roster. Naming its owner in
`owner` hands it to them.

## invitations/{code}

| Field       | Type      | Meaning                                     |
|-------------|-----------|---------------------------------------------|
| `createdBy` | string    | The administrator who made it                |
| `expiresAt` | timestamp | When it stops letting anybody in             |
| `email`     | string    | The Google address it lets in (ADR-0015)     |

The code is the name of the document rather than a field, so an invitation
cannot be found except by knowing it. Nobody reads this collection -- not even
the administrator who made it -- because a reader would hold every code of the
group. The rules read it, and a rule reads without the caller's permission.

An invitation cannot be edited once it is out, only withdrawn by deleting it:
editing would let an expiry be pushed out for a code already handed round.

A code is spent by joining (ADR-0025). The person joining writes their claim,
adds themselves to the roster and deletes the invitation in one batch; the
rules let the roster grow only when the invitation is gone at the end of that
batch, and let the invitation go only when the same batch puts its holder on
the roster. A code therefore admits once, and only the account whose verified
address is `email`, compared without regard to case. An invitation made before
addresses existed has no `email` and admits whoever holds it first; a new one
cannot be made without an address. `expiresAt` may be at most 25 hours after
the invitation is written.

## joins/{uid}

| Field  | Type   | Meaning                              |
|--------|--------|--------------------------------------|
| `code` | string | The code this person is holding      |

A rule cannot be told anything except through the database: the write it weighs
is the roster, and a code has nowhere to ride in it. So the person joining
writes the code here first, and the rule that weighs their addition to the
roster reads it from here.

The document is theirs and nobody else's -- reading it would be reading a code,
writing it would be putting one into somebody's hand -- and its shape is held to
eight letters and digits, because the rules build a document path out of it.

## falsePlaces/{placeId}

| Field          | Type   | Meaning                                                    |
|----------------|--------|------------------------------------------------------------|
| `latitude`     | number | Degrees                                                      |
| `longitude`    | number | Degrees                                                      |
| `radiusMetres` | number | How far around the point to distrust; at most 50 000         |
| `named`        | string | What the place is, so a log line says something readable     |

Where a false signal puts the devices of this group. Held here rather than in
the build: which places those are is a fact about one household's surroundings,
and a build carrying them would hand them to everybody who downloads it.

Everybody in the group reads the collection, including somebody who joined an
hour ago on a code — a device that does not know the places takes the lie as its
anchor, and that first fix of a session is the one no other rule can catch. Only
an administrator writes it, for the same reason they are the ones who edit the
roster: a place added wrongly is ground the group's devices can no longer be
placed on.

The radius is not the spread of the false answers — those sit within metres of
each other — but the length of the road the receiver draws away from the point
while it is being fed.

The devices subscribe to the collection and keep a copy in a file of their own,
because the fix these places exist to catch is the first one after the service
starts, and a device waiting for its subscription has nothing to catch it with.
Whatever cannot be read as a place is left out rather than thrown, so one
document typed wrong in the console does not stop the rest from arriving.

## settings/{key}

One document per subject. Today there is one: `circling`, the limits of the
rule that reads the shape of a run of answers.

| Field            | Type   | Meaning                                                      |
|------------------|--------|--------------------------------------------------------------|
| `answers`        | int    | How many answers in a row are fitted to a circle; at least 4   |
| `withinSeconds`  | number | How long those answers may be spread over                      |
| `roundness`      | number | How far off the circle they may sit, as a share of its radius   |
| `widestMetres`   | number | The widest circle worth calling a circle                       |
| `tightestMetres` | number | The tightest one                                               |
| `steadiness`     | number | How much the speed may vary, as a share of its average          |
| `turnedDegrees`  | number | How far round the circle the answers have to have gone          |
| `slowestMps`     | number | The slowest the answers may move and still be a road            |

Every field may be absent, and what is absent keeps the value the build
carries. The build's limits catch a transmitter of any shape; a group that has
watched one over its own neighbourhood knows its size and speed and can narrow
them, which recognises the same circle in fewer answers. Those numbers describe
the transmitter rather than the application, which is why they are here and not
in a release anybody can download.

Everybody in the group reads it — every device weighs its own answers against it
— and only an administrator writes it, because limits set wrongly either catch
nothing or refuse honest positions. The rules hold the fields to numbers of the
right kind; what the numbers mean together is weighed on the device, and a
document that does not stand as limits is ignored whole rather than applied in
part.

## searches/{uid}.{deviceId}

| Field       | Type      | Meaning                                                        |
|-------------|-----------|----------------------------------------------------------------|
| `openedBy`  | string    | The administrator who opened the search                        |
| `startedAt` | timestamp | When it was opened; set by the server itself                   |
| `renewedBy` | string    | The administrator who last wrote it, the opener included       |
| `renewedAt` | timestamp | When it was last written; set by the server itself             |
| `hours`     | int       | How long it lasts from `renewedAt`; 1 to 72                    |
| `phoneName` | string    | The device's name as the administrator saw it; optional, ≤ 100 |

The group is looking for this device of the member. The id is the account and
the device joined by a dot, the same as the device's position, so several
devices may be looked for at once, of one member or of several. While the search
stands — from `renewedAt` for `hours` by the server's clock — everybody in the
group reads that device's position, whatever the grant's `sharedWith` says; the
member's other devices, their grant and their list of devices stay as they were.
The rule that opens the position reads the search here, so calling it off closes
it at once rather than when the device next writes, for the same reason the
permissions are read from the grant.

Since the list of devices stays closed, the search carries the device's name for
whoever has nothing else to call it by.

Everybody in the group reads the collection: every map marks the device looked
for, and a search names nobody's place. Only an administrator opens one, renews
it, or opens it again once it has run out; a renewal keeps `openedBy` and
`startedAt`, so the family sees how long the device has been looked for. Any
administrator calls it off by deleting the document, and so does the member
whose device it is: they are found the moment they say so. A member who leaves
the roster is no longer searched for, whatever the document says.

Three days at most, because a search forgotten on an administrator's screen
would otherwise leave the member's place open to the whole group for good. The
clients offer six hours, a day and three days.

## sirens/{uid}.{deviceId}

| Field       | Type      | Meaning                                                                                  |
|-------------|-----------|------------------------------------------------------------------------------------------|
| `calledBy`  | string    | Who rang the device                                                                      |
| `calledAt`  | timestamp | When; set by the server itself                                                           |
| `heardAt`   | timestamp | When the device started sounding; set by the server, written by the device's own account |
| `stoppedBy` | string    | Who stopped it early: the device's owner or a caller                                     |
| `stoppedAt` | timestamp | When it was stopped; set by the server itself                                            |

One document per device, its id made the same way as a position's. The device
listens to its own document over the subscription its service already holds, and
sounds for twenty seconds through the alarm stream when a call arrives that is
newer than the last one it answered and at most a minute old — a device switched
back on an hour later does not ring for a call nobody is waiting on.

An administrator rings any device in the group, and a member their own devices:
the device looked for is most often the member's other device, lost at home.
Nobody else reads the document. A call is written whole and replaces what the
last one left, so `heardAt` and `stopped*` always belong to the call beside
them; the device adds `heardAt` alone, and a stop adds the two `stopped*` fields
alone. The caller's screen reads `heardAt` to tell a device that answered from
one that is switched off. Nothing deletes a siren; it is only called again.

## resets/{uid}.{deviceId}

| Field     | Type      | Meaning                                                                        |
|-----------|-----------|--------------------------------------------------------------------------------|
| `askedBy` | string    | Who asked the device to forget where it thinks it is                           |
| `askedAt` | timestamp | When; set by the server itself                                                 |
| `doneAt`  | timestamp | When the device forgot; set by the server, written by the device's own account |

One document per device, its id made the same way as a position's. The jump
filter holds on to the last place it believed, and a wrong one makes the device
refuse every honest answer after it; the device's own button settles that, and
this document is the same button pressed from somebody else's screen. The device
listens over the subscription its service already holds and forgets once for
every request newer than the last one it carried out, provided the request is at
most a day old, then writes `doneAt` so that the asking screen can tell a device
that did from one not heard from since.

The same people as for a siren: an administrator asks any device in the group, a
member their own. Nobody else reads the document. A request is written whole and
replaces what the last one left; nothing deletes it.

## members/{uid}

| Field         | Type      | Meaning                                                  |
|---------------|-----------|----------------------------------------------------------|
| `displayName` | string    | What the member is called on the other screens             |
| `photoUrl`    | string    | The account picture, as the provider hosts it; null without one |
| `updatedAt`   | timestamp | When the profile was last written; set by the server itself |

Everybody in the group reads this document, so nothing else belongs in it. Which
devices a member carries and whom they let watch are answers to "where is this
person", and they live in the grant below, where only the people concerned can
read them; the rules refuse a profile carrying any other field.

Both values come from the account the member signed in with, and neither client
uploads an image: the picture is the address the provider already serves. The
rules take only an address on `lh<digits>.googleusercontent.com` over https,
where the provider keeps account pictures (the rest of the domain serves
content people put there themselves), and both clients draw nothing from anywhere
else: every member's device fetches the picture, and an address of the member's
choosing would have each of them call on that server. A client
writes the document only when what the account says differs from what is stored,
because names change about never and the free tier counts every write.

The marker colour is not stored. It is computed from the id on both sides, so that
a member keeps their colour whether or not they have ever signed in.

A member with no profile is drawn under the first characters of their id, which is
what both clients showed before profiles existed.

## grants/{uid}

| Field        | Type | Meaning                                                                                   |
|--------------|------|-------------------------------------------------------------------------------------------|
| `devices`    | list | The devices this member carries, at most ten; how a reader learns which documents to read |
| `sharedWith` | map  | Who may read this member's position, and until when; at most fifty                        |

Read one document at a time and never as a collection: listing it would hand
every member the whole household's devices, which is what keeping it apart from
the profile is for. A member reads their own grant, and so does anybody their
`sharedWith` names while that permission still stands.

`sharedWith` is where a permission is given and taken back, and the rules read a
position's permission from here rather than from a copy inside the position. A
copy would be cheaper, but it would only change when the device writes its next
position, so a withdrawal would take effect at the device's convenience -- ten
minutes for a device standing still, an hour and a half at night, never at all
for a device that is switched off.

A permission carries the moment it runs out, and one meant to last is written as
a date far enough away to read as never: the rules compare a timestamp, and a
missing value would be a case of its own in every one of them.

The counts are held by the rules because every other client acts on this
document: one subscription per device named, one document per permission
weighed. Rules cannot walk a list, so the size is what can be checked, and both
clients cut the list to the same number before they subscribe.

## devices/{uid}

One entry per device of the member, under the device's id:

| Field            | Type      | Meaning                                                         |
|------------------|-----------|-----------------------------------------------------------------|
| `model`          | string    | The model as the manufacturer writes it                         |
| `name`           | string    | What the owner calls the device, shown to everybody who sees it |
| `onlineAt`       | timestamp | When the device was last in touch, set by the server            |
| `mode`           | string    | The collection mode it was in                                   |
| `batteryPercent` | int       | The charge left, 0 to 100                                       |
| `charging`       | bool      | Whether it was on a cable                                       |

None of it is a place, so it is written apart from the position: when a mode
begins, when the cable goes in or out, and once an hour whatever else happens.
The filter that holds a position back when the answers look forged does not hold
these back, so a device under a transmitter still says it is in touch and on its
cable. Readers lay an entry over the device's position when it is the newer of
the two.

Read one document at a time by the member, by the readers their grant names, and
by the administrators: an administrator is shown the devices and whether they
are working, not whom they are shared with or where they are. The rules hold the
number of devices to ten and every entry to the fields above, each of its type
and, for the strings, no longer than a name; the readers still drop an entry
that is not what a device writes.

A device also names itself in `grants/{uid}.devices`, which is what the builds
before this document read, and the readers follow the devices of both lists —
but only where the grant could be read, since the grant is the permission.

## labels/{uid}

| Field     | Type | Meaning                                                                  |
|-----------|------|--------------------------------------------------------------------------|
| `devices` | map  | A device's address, `{uid}.{deviceId}`, to the name this member gives it |
| `members` | map  | A member's id to the name this member gives them                         |

The member's own names for the others, read and written by the member alone —
not by the person named and not by an administrator. Up to five hundred devices
and fifty people.

A device is shown under the viewer's own name for it, else the `name` its owner
gave it in `devices`, else its `model`. A person is shown under the viewer's own
name for them, else the name in their profile, else the start of their id. Both
clients keep a copy of the document — the application in its own storage, the
page in `localStorage` — so the names are there before the database answers.

## positions/{uid}.{deviceId}

| Field            | Type      | Meaning                                                                         |
|------------------|-----------|---------------------------------------------------------------------------------|
| `memberId`       | string    | The account the device is signed in with; the rules require it to be the writer |
| `latitude`       | number    | Degrees                                                                         |
| `longitude`      | number    | Degrees                                                                         |
| `takenAt`        | timestamp | When the fix was taken, by the clock of the device                              |
| `writtenAt`      | timestamp | When the server accepted it; set by the server itself                           |
| `mode`           | string    | Collection mode that produced the fix                                           |
| `batteryPercent` | number    | Battery level, percent, for the viewer's context                                |
| `charging`       | boolean   | Whether a cable is in, which says which way the percentage is going             |

Who may read the position is not written here. The permissions live in the
owner's profile, which is where the owner writes them and where the rules read
them from: a copy kept in this document would go on granting what has been taken
back until the device wrote its next position — ten minutes for a device
standing still, and never at all for one that is switched off or lost.

A reader counts the age of a position from `writtenAt`, falling back to
`takenAt` while the server stamp is still on its way: `writtenAt` is the one
clock every member shares, and a device minutes off would otherwise show an age
nobody can explain.

A write happens when the position moved far enough from the last written one, or
enough time passed without a write — both by the standards of the mode: 75
metres or 10 minutes in the quiet modes, 50 metres or 3 minutes while somebody
is watching. Jitter therefore produces no writes and wakes no subscribers, and
the mode that spends the battery on frequent fixes is the mode that says what
they found. The time in the watched mode is long because one open map puts every
device of the group into it: twenty devices lying still and repeating their
place every half minute would spend the free quota of writes within a working
day.

One document per device rather than per member, because a member can carry two:
a second device used to overwrite the first, and the marker showed whichever of
them wrote last. The identifier of a device is made up on first use and kept in
the application's own storage, so reinstalling makes a new one and leaves the
old document to age on screen until it is deleted.

## watch/{uid}.{deviceId}

| Field      | Type      | Meaning                                            |
|------------|-----------|----------------------------------------------------|
| `memberId` | string    | The account looking; the rules require it to be the writer |
| `since`    | timestamp | When the claim was made; set by the server itself    |

A claim is worth seven minutes from `since`, and the viewer refreshes it every
five, trying a failed refresh again within a minute. Every device of the group
listens to the whole collection and reads each refresh, so in a group of twenty
a refresh a minute would cost over a thousand reads an hour for one open map.
The moment comes from the server rather than from the viewer, and the rules
accept no other: an end named by the viewer would be worth whatever their clock
said — a clock running slow would never start the fast mode at all, and a claim
dated years ahead would hold every device of the group in it for good.

Leaving the map deletes the document; a crash or a lost connection lets the
claim expire on its own, so the viewing mode never sticks. The extra two minutes
over the refresh are what keep one failed write from dropping the devices out of
their fast mode.

A browser claims only while its tab is visible and somebody has touched it
within the last quarter of an hour: a tab left open on a desk would otherwise
hold every device of the group in the most expensive mode there is.

A device answers only the markers of those who may see it: its owner's other
devices, and the members its owner's grant names in `sharedWith`, for no longer
than that permission lasts. A viewer who cannot see a device has nothing to gain
from it being fast, and in a group of twenty one open map would otherwise hold
every device in the most expensive mode for the sake of the few on that map.
Until the device has read its grant it answers every marker, as it did before.

A device ignores its own marker: answering it would mean keeping the receiver on
for its own map. Only its own, not every marker of its account: a member looking
at the map on one device is somebody the other device should be fresh for.

The browser claims under a name it keeps in the storage of the browser itself, so
a reload leaves one marker rather than a trail of them.

## Collection modes

| Mode      | Trigger                          | Priority         | Interval   | Batch window |
|-----------|----------------------------------|------------------|------------|--------------|
| `Idle`    | Inside the current geofence      | none             | —          | —            |
| `Standby` | Outside it, nobody watching      | balanced         | 15 min     | 10 min       |
| `Moving`  | Activity recognised as movement  | balanced         | 5 min      | 5 min        |
| `Viewing` | A live claim in `watch`          | high accuracy    | 30 s       | none         |
| `Night`   | 23:00 to 07:00, nobody watching  | low power        | 20 min     | 5 min        |

`Night` is also forced whenever the battery drops below 15 percent. The names
are written as the devices write them, which is the form the `mode` field
carries; both clients show a phrase of their own in its place on a card.

Asking and waiting together have to come to less than the half hour after which
a position is drawn as old — otherwise a device that is working perfectly
appears to have stopped — and a test holds every mode to that.

`Idle` and `Moving` are not reached yet: the geofence and the movement signal
they depend on are not implemented, so a device moves between `Standby`, `Night`
and `Viewing` only.
