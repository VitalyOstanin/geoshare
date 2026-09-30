# ADR-0016: A group has a name people choose

- Status: Accepted
- Date: 2026-09-30

## Context

A group's id is its founder's account id (ADR-0009), which means nothing to a
person. An invitation, the connection page and the screens need to say which
group this is. A name made from the founder's ("Anna's group") cannot tell two
groups of one person apart and reads oddly for friends or a hiking party.

## Decision

- The group document gets a name, given when the group is founded and changed
  later by an administrator.
- Until a name is given, the screens say "group of <founder's name>".

## Consequences

- The rules check the name's shape (a string of bounded length) on founding
  and on an administrator's write; their tests grow accordingly.
- The founding screen and the group panel on both clients get a field for it.
