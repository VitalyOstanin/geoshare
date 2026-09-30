# ADR-0011: An emergency search opens one device to the whole group

- Status: Accepted
- Date: 2026-09-27, per device from 2026-09-29 (recorded 2026-09-30)

## Context

When a member is missing, the group needs to see where their phone is, even
if the member never shared with everybody. The permissions of ADR-0008 are
the member's own choice, and nobody else may widen them.

## Decision

- An administrator opens a search for one device: `searches/{uid}.{deviceId}`,
  the same id as the device's position, with a duration counted from the
  server's clock.
- While it stands, the rules let every member of the group read that one
  position, over whatever the grant says. The member's other devices, grant
  and device list are not opened.
- The search document carries the device's name, so members without a
  permission can still tell which device it is.
- Any administrator, or the member from any of their devices, ends it; the
  place closes at once, not at the phone's next write.
- Several searches can run at once. Searches kept per member by older builds
  stop working.

## Consequences

- An administrator can override a member's privacy for one device and for a
  bounded time; the map marks the searched device so that everybody sees a
  search is on.
- A member who has left the roster cannot be searched for by the group they
  left.
