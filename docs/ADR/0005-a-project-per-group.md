# ADR-0005: Every group runs its own Firebase project

- Status: Accepted
- Date: 2026-09-11 (recorded 2026-09-30)

## Context

A public build means groups other than the one the repository started with.
Either all of them share one project run by the authors, or each runs its own.
A shared project would put every group's positions in one place held by
people none of them chose, and its free quotas would be split among all of
them.

Whoever owns a project can read everything in it from the console, past the
rules, and can change the rules. That is the usual position of whoever holds
the storage of any location service; the difference here is that the holder
is one of the group.

## Decision

Each group creates and owns its own Firebase project. The application never
assumes at build time which project it talks to.

## Consequences

- A group's data and quotas are its own; nobody outside it can read them.
- The owner of the project sits above the other members, which is natural for
  a household run by a parent and less so for strangers travelling together.
  The application should say whose account holds the project, so that a
  person joins on terms they can see, rather than pretend the members are
  equal.
- Setting up a project has to be made easy enough for a group to do
  (ADR-0003), and one build has to reach any project (ADR-0001).
