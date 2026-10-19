# ADR-0036: A project holds one group, founded once

- Status: Accepted, supplemented by 0038
- Date: 2026-10-13

## Contents

- [Context](#context)
- [Decision](#decision)
- [Consequences](#consequences)

## Context

ADR-0005 gives every group a Firebase project of its own: the data and the
free quotas of a project are the group's, and the person who owns the project,
and can read all of it from the console, is one of the group.

The rules did not hold to that. `founding()` let any signed-in account create
a group named after itself, so one project could hold as many groups as
accounts that signed in to it. The project's configuration is public on the
group's site, and signing in with Google admits any account, so anybody who
found the site could found a group of their own, spend the project's quotas
on it and keep data there that the project owner could read. The clients
offered founding to everybody without a group, because a group document is
closed to whoever is not on it and a device could not tell an empty project
from one whose group it is not in.

A second group of the same account was not possible in one project either way:
the group's id is its founder's account id.

## Decision

- A project holds one group. The document `project/group` names it, in its
  only field `group`.
- That document is written once, in the batch that founds the group; the rules
  refuse a founding that does not write it, a `project/group` written without
  its group, and any change to it or deletion of it from a client. Any
  signed-in account may read it by its path; nobody may list the collection.
- Before offering to found a group, a client reads `project/group`. When it
  names a group, the client says that the project already has one, that a
  group of one's own needs a project of its own, and offers only joining by an
  invitation. When it cannot be read, the client offers both ways, as before;
  the rules refuse a second founding.
- A second group, of the same account or of another, is a second project, set
  up the same way as the first (ADR-0003, ADR-0033). One device can be in both
  (ADR-0026).
- A project whose group was founded before this decision gets `project/group`
  written once by hand, from the console or with the project owner's
  credentials.

## Consequences

- The quotas and the data of a project are one group's again, and a stranger
  who reaches the site can sign in but write nothing.
- Every person signing in without a group costs one more read.
- Several groups cost several projects, and a Google account on the free plan
  can hold only a handful of them; more are asked of Firebase.
- A group can no longer be replaced from a client: since groups are never
  deleted (the rules refuse it), a project whose group is abandoned stays that
  group's, and a new start is a new project.
