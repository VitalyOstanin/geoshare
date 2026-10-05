# ADR-0027: What an administrator sees and may do

- Status: Accepted
- Date: 2026-10-05

## Context

An administrator runs the group's roster (ADR-0009) but owns nothing: the
project belongs to the project owner, who reads the whole database from the
console anyway. Before the group panel, the rules gave an administrator no
more than any member, so a phone that had gone quiet could not be told from a
member who had simply stopped sharing, and nothing said who could hand the
running of the group to whom.

Two things pull apart. Running a household needs to know whether its phones
are alive. Watching it needs to know where its members are and whom they
share with — and that is not what running it is for.

## Decision

- The phones of a member live in a document of their own,
  `devices/{uid}`: the model, the name its owner gave it, when it was last in
  touch, its collection mode, its charge and whether it is on a cable. No
  place. An administrator may read it; so may the owner and whoever the
  owner's grant lets watch them.
- A phone writes its entry when its mode changes, when it is plugged in or
  out, and otherwise every time an idle phone rewrites its place (ADR-0010).
- The grant — whom a member shares with and until when — stays readable by
  its owner and by the readers named in it, not by administrators.
- An administrator reads a member's position only as any member does: through
  the member's grant, or through a standing search (ADR-0011).
- Any administrator may make any member an administrator. An administrator
  who is not the founder may not take the founder off the roster or off the
  administrators, and takes off the roster only a member who runs nothing:
  taking off another administrator is two writes, the role first.
- The last administrator cannot resign, and an administrator has to be on the
  roster.
- How much of the free plan the project spends is shown to the project owner
  by `tools/project.sh show --usage`, not to administrators.

## Consequences

- The group panel tells a silent phone from one whose marker is stuck
  without revealing where anyone is.
- A member's sharing stays private from the people who run the group.
- An administrator looking for a lost member has to open a search, which
  every member sees (ADR-0011), rather than look quietly.
- Ten phones per member at most: the rules weigh the entries one by one, and
  a map cannot be walked.
