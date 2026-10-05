# ADR-0031: A refused reader asks again when the granter says so

- Status: Accepted
- Date: 2026-10-05

## Context

A position is read only while the owner's grant names the reader (ADR-0008).
A subscription the rules refused is dead: Firestore does not tell it that the
grant changed afterwards. The clients made each refused subscription again
after a minute, so a member who had just been shown a place waited up to a
minute for the marker, while the one who granted watched the other's screen.

## Decision

- After a grant is written, the member who gave it touches `updatedAt` in
  their own profile, `members/{uid}`, with the server's time. Every client
  already listens to the profiles.
- A client that sees the latest `updatedAt` among the profiles move makes all
  its refused subscriptions again at once.
- The one-minute retry stays for whatever the signal misses.
- The touch is best-effort: a failed one leaves the timer to do the work.

## Consequences

- A new reader sees the place within seconds of the grant.
- Every change of any member's grant makes every client in the group retry
  its refusals once: a handful of reads, in a group of a household's size.
- The rules needed no change: the profile already allowed `updatedAt`.
