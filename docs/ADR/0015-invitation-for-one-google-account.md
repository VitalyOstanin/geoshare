# ADR-0015: An invitation is issued to one Google account

- Status: Accepted
- Date: 2026-09-30

## Context

An invitation (ADR-0009) lets in everybody who uses its code before it runs
out. A link that is forwarded, overheard or copied lets a stranger into the
group, and the person invited may find the place already taken by nobody in
particular. The invitation should belong to the person it was made for.

That person has no account in the project until their first sign-in, so the
only name an administrator can give them in advance is the address of their
Google account. Sign-in with Google gives the rules that address, verified,
in `request.auth.token.email`.

## Decision

- An administrator making an invitation enters the invited person's Google
  address; the invitation document keeps it.
- The rules let a person join on that code only when the account signing in
  has that address and it is verified.
- An invitation still carries its expiry, and it is still read by nobody but
  the rules.

## Consequences

- A forwarded or stolen link lets nobody else in.
- The administrator has to know the address; a typo, or signing in with
  another account, ends in a refusal, and the joining screen has to say which
  address the invitation was made for.
- The rules, their tests, and the invitation screens on the phone and the page
  change; the rules are deployed to each group's project.
- ADR-0009 stays as it is about the group's name and the invitation text; its
  "anybody holding the code" is replaced by this.
