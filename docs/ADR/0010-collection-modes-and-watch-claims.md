# ADR-0010: Positions are collected by mode, and fast only while somebody watches

- Status: Accepted
- Date: 2026-09-11, modes settled 2026-09-13 (recorded 2026-09-30)

## Context

A phone that measures its position often drains its battery; one that
measures rarely shows a stale point exactly when somebody looks. The free
plan's quotas also bound how often a phone may write.

## Decision

- A position is fresh on demand: a viewer's client writes a watch claim per
  device (`watch/{uid}.{deviceId}`, the server's time in `since`) and renews
  it every minute while the map is open; a phone that sees a live claim on
  itself collects fast. A claim older than three minutes stops counting.
- Otherwise the phone picks a mode by what it knows cheaply:
  - moving — from the system's activity transitions, which a low-power chip
    counts without the receiver;
  - staying inside a 150 m geofence with five-minute responsiveness, where the
    receiver is not switched on at all;
  - idle — the same point rewritten about every 25 minutes so that it does not
    turn grey, while `takenAt` keeps the moment it was actually measured.

## Consequences

- The battery cost follows how often somebody looks, not a fixed interval.
- The age of a point on screen is counted from `writtenAt` with `takenAt` as a
  fallback, and the card says when the point was measured.
- A phone asleep enough to miss the subscription stays slow until it wakes;
  waking it by push is a separate question.
