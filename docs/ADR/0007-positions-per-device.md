# ADR-0007: A position per device, not per member

- Status: Accepted
- Date: 2026-09-12 (recorded 2026-09-30)

## Context

A member can carry more than one phone under one Google account. With one
position document per member, two phones overwrote each other and the map
jumped between them.

## Decision

- Positions are flat documents `positions/{uid}.{deviceId}`, the owner
  repeated in `memberId`, which the rules require to match the writer.
- The device id is random, made on the first start and kept in the
  application's storage; the device's name is the maker's model name, up to
  40 characters.
- The map shows a marker per device, labelled with the member's name and the
  device's name only when the member has more than one.
- Watch claims are per device too, so a second phone of the same account can
  make the first one collect faster.

## Consequences

- Every per-device feature (searches, sirens) uses the same `{uid}.{deviceId}`
  id.
- A reader finds a member's devices through a list the member publishes,
  since the positions collection cannot be listed.
