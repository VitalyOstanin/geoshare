# ADR-0019: Making an invitation and naming a group

- Status: Accepted
- Date: 2026-09-30

## Context

ADR-0015 ties an invitation to one Google address, ADR-0016 gives a group a
name, and ADR-0017 puts the inviting member's name and the group's name into
the link. What was left open is how the screens do it: what a group without a
name is called, what becomes of the invitation text in use so far (the group
id and the code joined by a dot, admitting anybody it reaches within a day),
who may make an invitation, and what the application says when the account
signed in is not the one invited.

## Decision

- A group without a name is shown as "Группа · <founder's name>" in Russian
  and "<founder's name>'s group" in English. The name stays in the nominative,
  which is correct for any name.
- The name is entered, optionally, when the group is founded, and an
  administrator renames it from the group panel.
- Making an invitation asks for the invited person's Google address and gives
  back a connection link, to send through the system's share menu or copy.
  The invitation text without an address is no longer made; the field for
  pasting an invitation accepts the link.
- Only administrators make invitations, as before.
- When joining is refused and the signed-in address does not match the
  shortened one from the link, the application names both and offers to switch
  accounts, keeping the invitation for the next sign-in.
- The confirmation for switching to another project shows the invitation's
  captions (group, who invited, for which account) when the link carries them.

## Consequences

- Texts already handed out keep working until they expire, a day at most;
  after that every way into a group goes through a link and an address.
- A renamed group keeps its old name in links sent before the renaming, since
  the name is written into a link when it is made (ADR-0017).
- A person who signed in with the wrong account is told which one to use
  instead of a bare refusal.
