# ADR-0028: A siren on a lost phone

- Status: Accepted
- Date: 2026-10-05

## Context

A search (ADR-0011) shows where a device is, to a few tens of metres. Under a
cushion or in a bag at home that is not enough: the phone has to be heard.
The phone's own sound may be off, and "Do Not Disturb" may be on.

## Decision

- A siren is a document per phone, `sirens/{uid}.{deviceId}`, the id a
  position's. The phone hears its own document over the subscription its
  service already holds.
- An administrator may call any phone of the group; a member may call their
  own phones. Nobody else reads the document.
- A call is written whole: who called and when, by the server's clock, so
  that a phone switched on later tells a stale call from a new one. The phone
  writes when it starts sounding; anybody allowed to call may stop it, the
  stop naming who and when.
- The phone sounds for 22 seconds on the alarm stream at full volume, with
  the application's own siren, the device's alarm sound if that cannot be
  loaded. "Do Not Disturb" is lifted just enough for alarms; total silence
  only when the owner gave the application access to the notification policy.
- Each group keeps its own answered call (ADR-0026), so a call in one group
  does not hide a call in another.
- A screen that called tells the call as waiting, sounding, stopped or not
  answered, and keeps saying it for five minutes after it ended.

## Consequences

- A phone asleep with its service held back by the system hears the call
  only when it next wakes; waking it needs a push, not on the free plan
  today.
- The member always learns from their own phone's screen who called.
- Nobody outside the administrators learns which phone went missing when.
