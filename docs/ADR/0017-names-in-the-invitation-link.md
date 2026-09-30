# ADR-0017: The invitation link carries who invited and the group's name

- Status: Accepted
- Date: 2026-09-30

## Context

The page a link opens and the phone that takes it should say who invited the
person and to which group. Neither can read that from the database: an
invitation is read by nobody but the rules, and the page is not signed in.

## Decision

The application writes the inviting member's name and the group's name into
the link, after the `#` beside the group id and the code, when it makes the
invitation. The page and the phone show them as they are.

## Consequences

- No change to the database or the rules for this, and the page needs no
  sign-in to show the names.
- The link is longer, and anybody holding it sees the names.
- The names can be altered by whoever edits the link. They are a caption
  only: whether the person may join is still decided by the rules from the
  code and the address (ADR-0015).
