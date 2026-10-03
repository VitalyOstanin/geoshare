# ADR-0025: An invitation admits once and lasts at most a day

- Status: Accepted
- Date: 2026-10-03

## Context

An invitation is made out to one Google account (ADR-0015), but nothing spent
it: joining was two writes, the code and then the roster, and nothing made a
third follow them to mark the code used. So the account it was made for could
join again on the same link while it lasted — after being taken off the
roster, for one — and an invitation made before addresses existed admitted
whoever held it, as many times as it was used.

The rules did not hold the expiry either. The application sets it a day ahead,
but an administrator writing to the database directly could make a code that
never runs out.

Security rules weigh a batched write as a whole, and `getAfter()` and
`existsAfter()` read the database as the batch leaves it. A rule can
therefore ask that one document be gone at the end of the very write that
changes another.

## Decision

- Joining is one batch: the claim to the code, the person added to the roster,
  and the invitation deleted.
- The rules let the roster grow only when the invitation the claim names
  stands before the write and is gone after it, and let the person joining
  delete an invitation only when the same write puts them on the roster on
  that very code.
- An invitation may not expire later than 25 hours after it is written: the
  day the application gives it and an hour's grace for a phone whose clock
  runs ahead of the server's.

## Consequences

- A code lets one person in once. Somebody taken off the roster needs a new
  invitation to come back.
- A phone or a page still writing the three as separate writes is refused by
  the new rules, and the new client's batch is refused by the old ones, which
  let only an administrator delete an invitation. The rules, the page and the
  application change together in each group's project.
- `firebase/data-model.md` no longer says that a code admits more than once.
- ADR-0015 stands; this adds to it.
