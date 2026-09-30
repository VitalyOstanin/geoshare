# ADR-0008: Permission to watch is given per reader, with an expiry, and read live

- Status: Accepted
- Date: 2026-09-12, revised 2026-09-13 (recorded 2026-09-30)

## Context

Being in a group should not by itself mean being watched by everybody in it.
A member decides who sees them and for how long. The first design kept the
permission list in the member's profile and copied it into every position
document the phone wrote. A review found the weakness: withdrawing a
permission took effect only when the phone next wrote, which for a phone that
is switched off, lost or has the application removed is never.

## Decision

- A member's grant document holds `sharedWith`: reader's uid → the moment the
  permission ends. "No end" is the far date `9999-12-31T00:00:00Z`, so rules
  and clients compare one kind of value.
- The rules read the permission from the owner's grant on every read of a
  position, not from a copy inside the position, and allow the read only
  while it has not run out.
- The positions collection cannot be listed; a reader reads the documents of
  the devices a member publishes.
- Without a grant, or without an entry in it, the read is refused: nobody
  watches a member until the member shares.

## Consequences

- Withdrawing a permission works at once, whatever state the owner's phone is
  in.
- Each position read costs one more document read. For a handful of phones
  that is well inside the free quota.
- The list of readers is no longer exposed to everybody a position is shared
  with.
