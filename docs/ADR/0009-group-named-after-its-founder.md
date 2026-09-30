# ADR-0009: A group is named after its founder, and an invitation carries the name

- Status: Accepted; who may use an invitation superseded by ADR-0015
- Date: 2026-09-25 (recorded 2026-09-30)

## Context

Groups used to be made in the console, one per project, under a fixed name.
Once a phone can found a group itself, the name has to come from somewhere.
A name of the founder's choosing would let one account fill the database with
empty groups, each free to make. A person invited also has to know the name:
a rule decides on a write to a path, so the path must be known before the
write.

## Decision

- A group founded on a phone has the founder's uid as its id; the founder is
  alone on the roster and among the administrators when it is made.
- Joining is by an invitation: a document named after the code, readable by
  nobody but the rules, with an expiry. The invited person adds themselves
  and nobody else while holding a join claim that names a live invitation.
- An invitation is handed over as one string, `group.code`.
- The group's name is state the phone keeps, not a constant of the build.

## Consequences

- Every account can found exactly one group.
- Nothing outside `groups` is needed: a lookup table from code to group was
  rejected as a world-readable collection with two unconnected writes per
  invitation.
- A group made in the console before this keeps its old name and names its
  owner in a field the clients cannot write.
