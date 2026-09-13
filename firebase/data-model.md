# Document model

One document per member for what a member is, and one per phone for what a phone
does. History is deliberately not kept: each new fix overwrites the previous one,
which keeps both storage and read counts flat.

```
groups/{groupId}                            members: [uid, ...]
groups/{groupId}/members/{uid}              what a member says about themselves
groups/{groupId}/positions/{uid}.{deviceId} last known position of one phone
groups/{groupId}/watch/{uid}.{deviceId}     viewing marker of one phone
```

The id of a position is the account and the phone joined by a dot. The rules read
the part before the dot, so a member writes only their own documents; the readers
group the markers by the `memberId` inside, which is what gives a phone its
owner's colour, face and name.

The group document is the roster and no client may write it; membership is edited
from the console. A member's own document under it is the opposite: they write it
and nobody else does.

## members/{uid}

| Field         | Type      | Meaning                                                  |
|---------------|-----------|----------------------------------------------------------|
| `displayName` | string    | What the member is called on the other screens             |
| `photoUrl`    | string    | The account picture, as the provider hosts it; null without one |
| `updatedAt`   | timestamp | When the profile was last written; set by the server itself |
| `devices`     | list      | The phones this member carries, at most ten; how a reader learns which documents to read |
| `sharedWith`  | map       | Who may read this member's position, and until when; at most fifty |

`sharedWith` is where a permission is given and taken back, and the rules read a
position's permission from here rather than from the position itself. The counts
are held by the rules because every other client acts on this document: one
subscription per device named, one document per permission weighed.

Every member of the group can read every profile, so who has allowed whom is not
private within the group. That is accepted rather than overlooked: a household
sharing its whereabouts is not a place where the pairs need hiding, and keeping
the permissions where their owner writes them is what makes a withdrawal take
effect at once.

Both values come from the account the member signed in with, and neither client
uploads an image: the picture is the address the provider already serves. A client
writes the document only when what the account says differs from what is stored,
because names change about never and the free tier counts every write.

The marker colour is not stored. It is computed from the id on both sides, so that
a member keeps their colour whether or not they have ever signed in.

A member with no profile is drawn under the first characters of their id, which is
what both clients showed before profiles existed.

## positions/{uid}.{deviceId}

| Field            | Type      | Meaning                                                  |
|------------------|-----------|----------------------------------------------------------|
| `memberId`       | string    | The account the phone is signed in with; the rules require it to be the writer |
| `deviceName`     | string    | What the phone calls itself, shown when a member has more than one on the map |
| `latitude`       | number    | Degrees                                                   |
| `longitude`      | number    | Degrees                                                   |
| `takenAt`        | timestamp | When the fix was taken, by the clock of the phone         |
| `writtenAt`      | timestamp | When the server accepted it; set by the server itself     |
| `mode`           | string    | Collection mode that produced the fix                     |
| `batteryPercent` | number    | Battery level, percent, for the viewer's context          |
| `charging`       | boolean   | Whether a cable is in, which says which way the percentage is going |

Who may read the position is not written here. The permissions live in the
owner's profile, which is where the owner writes them and where the rules read
them from: a copy kept in this document would go on granting what has been
taken back until the phone wrote its next position — ten minutes for a phone
standing still, and never at all for one that is switched off or lost.

A reader counts the age of a position from `writtenAt`, falling back to `takenAt`
while the server stamp is still on its way: `writtenAt` is the one clock every
member shares, and a phone minutes off would otherwise show an age nobody can
explain.

A write happens when the position moved far enough from the last written one, or
enough time passed without a write — both by the standards of the mode: 75 metres
or 10 minutes in the quiet modes, 25 metres or 30 seconds while somebody is
watching. Jitter therefore produces no writes and wakes no subscribers, and the
mode that spends the battery on frequent fixes is the mode that says what they
found.

One document per phone rather than per member, because a member can carry two: a
second phone used to overwrite the first, and the marker showed whichever of them
wrote last. The identifier of a phone is made up on first use and kept in the
application's own storage, so reinstalling makes a new one and leaves the old
document to age on screen until it is deleted.

## watch/{uid}.{deviceId}

| Field      | Type      | Meaning                                            |
|------------|-----------|-----------------------------------------------------|
| `memberId` | string    | The account looking; the rules require it to be the writer |
| `since`    | timestamp | When the claim was made; set by the server itself    |

A claim is worth three minutes from `since`, and the viewer refreshes it every
minute. The moment comes from the server rather than from the viewer, and the
rules accept no other: an end named by the viewer would be worth whatever their
clock said — a clock running slow would never start the fast mode at all, and a
claim dated years ahead would hold every phone of the group in it for good.

Leaving the map deletes the document; a crash or a lost connection lets the claim
expire on its own, so the viewing mode never sticks. The extra two minutes over
the refresh are what keep one failed write from dropping the phones out of their
fast mode.

A browser claims only while its tab is visible and somebody has touched it within
the last quarter of an hour: a tab left open on a desk would otherwise hold every
phone of the group in the most expensive mode there is.

A phone ignores its own marker: answering it would mean keeping the receiver on
for its own map. Only its own, not every marker of its account: a member looking
at the map on one phone is somebody the other phone should be fresh for.

The browser claims under a name it keeps in the storage of the browser itself, so
a reload leaves one marker rather than a trail of them.

## Collection modes

| Mode      | Trigger                          | Priority         | Interval   | Batch window |
|-----------|----------------------------------|------------------|------------|--------------|
| `Idle`    | Inside the current geofence      | none             | —          | —            |
| `Standby` | Outside it, nobody watching      | balanced         | 15 min     | 10 min       |
| `Moving`  | Activity recognised as movement  | balanced         | 5 min      | 5 min        |
| `Viewing` | A live claim in `watch`          | high accuracy    | 15 s       | none         |
| `Night`   | 23:00 to 07:00, nobody watching  | low power        | 20 min     | 5 min        |

`Night` is also forced whenever the battery drops below 15 percent. The names are
written as the phones write them, which is the form the `mode` field carries; both
clients show a phrase of their own in its place on a card.

Asking and waiting together have to come to less than the half hour after which a
position is drawn as old — otherwise a phone that is working perfectly appears to
have stopped — and a test holds every mode to that.

`Idle` and `Moving` are not reached yet: the geofence and the movement signal they
depend on are not implemented, so a phone moves between `Standby`, `Night` and
`Viewing` only.
