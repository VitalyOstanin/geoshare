# Document model

One document per member per collection. History is deliberately not kept: each new
fix overwrites the previous one, which keeps both storage and read counts flat.

```
groups/{groupId}                  members: [uid, ...]
groups/{groupId}/members/{uid}    display name, marker colour
groups/{groupId}/positions/{uid}  last known position
groups/{groupId}/watch/{uid}      viewing marker
```

`groups/{groupId}/members` is not written by any client yet: members are told apart
by their id, and the colour is computed from it on both sides.

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
