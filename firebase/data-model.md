# Document model

One document per member per collection. History is deliberately not kept: each new
fix overwrites the previous one, which keeps both storage and read counts flat.

```
groups/{groupId}                  members: [uid, ...]
groups/{groupId}/members/{uid}    what a member says about themselves
groups/{groupId}/positions/{uid}  last known position
groups/{groupId}/watch/{uid}      viewing marker
```

The group document is the roster and no client may write it; membership is edited
from the console. A member's own document under it is the opposite: they write it
and nobody else does.

## members/{uid}

| Field         | Type      | Meaning                                                  |
|---------------|-----------|----------------------------------------------------------|
| `displayName` | string    | What the member is called on the other screens             |
| `photoUrl`    | string    | The account picture, as the provider hosts it; null without one |
| `updatedAt`   | timestamp | When the profile was last written; set by the server itself |

Both values come from the account the member signed in with, and neither client
uploads an image: the picture is the address the provider already serves. A client
writes the document only when what the account says differs from what is stored,
because names change about never and the free tier counts every write.

The marker colour is not stored. It is computed from the id on both sides, so that
a member keeps their colour whether or not they have ever signed in.

A member with no profile is drawn under the first characters of their id, which is
what both clients showed before profiles existed.

## positions/{uid}

| Field            | Type      | Meaning                                                  |
|------------------|-----------|----------------------------------------------------------|
| `latitude`       | number    | Degrees                                                   |
| `longitude`      | number    | Degrees                                                   |
| `takenAt`        | timestamp | When the fix was taken, by the clock of the phone         |
| `writtenAt`      | timestamp | When the server accepted it; set by the server itself     |
| `mode`           | string    | Collection mode that produced the fix                     |
| `batteryPercent` | number    | Battery level, percent, for the viewer's context          |

A reader counts the age of a position from `writtenAt`, falling back to `takenAt`
while the server stamp is still on its way: `writtenAt` is the one clock every
member shares, and a phone minutes off would otherwise show an age nobody can
explain.

A write happens when the position moved at least 75 metres from the last written
one, or 10 minutes passed without a write. Jitter therefore produces no writes and
wakes no subscribers.

## watch/{uid}

| Field   | Type      | Meaning                                               |
|---------|-----------|--------------------------------------------------------|
| `until` | timestamp | Marker is ignored once this moment has passed           |

The viewer sets `until` three minutes ahead and refreshes it every minute. Leaving
the map deletes the document; a crash or a lost connection lets the marker expire
on its own, so the viewing mode never sticks. The extra minute over the refresh is
what keeps one failed write from dropping the phones out of their fast mode.

A phone ignores its own marker: answering it would mean keeping the receiver on
for its own map.

## Collection modes

| Mode      | Trigger                          | Priority         | Interval   | Batch window |
|-----------|----------------------------------|------------------|------------|--------------|
| `Idle`    | Inside the current geofence      | none             | —          | —            |
| `Standby` | Outside it, nobody watching      | balanced         | 15 min     | 45 min       |
| `Moving`  | Activity recognised as movement  | balanced         | 5 min      | 15 min       |
| `Viewing` | A live marker in `watch`         | high accuracy    | 15 s       | none         |
| `Night`   | 23:00 to 07:00, nobody watching  | low power        | 30 min     | 90 min       |

`Night` is also forced whenever the battery drops below 15 percent. The names are
written as the phones write them, which is the form the `mode` field carries.
