# ADR-0029: Resetting a stuck place from another screen

- Status: Accepted
- Date: 2026-10-05

## Context

The jump filter (ADR-0014) holds on to the last place it believed, and a
wrong one makes it refuse every honest answer after: the marker stays where
it is however far the member walks. The phone's own button settles that, but
the phone is not always in reach of whoever sees the marker stuck.

## Decision

- A reset is a document per phone, `resets/{uid}.{deviceId}`, made like a
  siren's (ADR-0028), heard over the same subscription.
- The same people may ask as for a siren: an administrator for any phone of
  the group, a member for their own.
- A request is written whole — who asked and when, by the server's clock — so
  that the phone tells a request it has carried out from a new one. The phone
  forgets its last place, takes the next one however far it is, writes when
  it did, and says in a notification who asked.
- The asking screen shows "waiting for the device", "the device took it", or
  that it did not answer, for five minutes after.
- Each group keeps its own carried-out request (ADR-0026).

## Consequences

- A stuck marker is mended without the phone in hand, while the phone is
  awake; as with the siren, waking it needs a push.
- A reset is told on the member's phone, so it is never done behind their
  back.
