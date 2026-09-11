# Document model

One document per member per collection. History is deliberately not kept: each new
fix overwrites the previous one, which keeps both storage and read counts flat.

```
groups/{groupId}                  members: [uid, ...]
groups/{groupId}/members/{uid}    display name, marker colour
groups/{groupId}/positions/{uid}  last known position
groups/{groupId}/watch/{uid}      viewing marker
```

## positions/{uid}

| Field      | Type      | Meaning                                            |
|------------|-----------|----------------------------------------------------|
| `lat`      | number    | Latitude, degrees                                  |
| `lon`      | number    | Longitude, degrees                                 |
| `accuracy` | number    | Horizontal accuracy, metres                        |
| `ts`       | timestamp | When the fix was taken, not when it was written     |
| `battery`  | number    | Battery level, percent, for the viewer's context    |
| `speed`    | number    | Metres per second, absent when unknown              |
| `mode`     | string    | Collection mode that produced the fix               |

A write happens when the position moved at least 75 metres from the last written
one, or 10 minutes passed without a write. Jitter therefore produces no writes and
wakes no subscribers.

## watch/{uid}

| Field   | Type      | Meaning                                               |
|---------|-----------|--------------------------------------------------------|
| `until` | timestamp | Marker is ignored once this moment has passed           |

The viewer sets `until` two minutes ahead and refreshes it every minute. Closing
the map deletes the document; a crash or a lost connection lets the marker expire
on its own, so the viewing mode never sticks.

## Collection modes

| Mode       | Trigger                          | Priority         | Interval   | Batch window |
|------------|----------------------------------|------------------|------------|--------------|
| `idle`     | Inside the current geofence      | none             | —          | —            |
| `standby`  | Outside it, nobody watching      | balanced         | 15 min     | 45 min       |
| `moving`   | Activity recognised as movement  | balanced         | 5 min      | 15 min       |
| `viewing`  | A live marker in `watch`         | high accuracy    | 15 s       | none         |
| `night`    | 23:00 to 07:00, nobody watching  | low power        | 30 min     | 90 min       |

`night` is also forced whenever the battery drops below 15 percent.
