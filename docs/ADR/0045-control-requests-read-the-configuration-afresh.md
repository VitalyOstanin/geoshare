# ADR-0045: Every control request reads the managed configuration afresh

- Status: Accepted
- Date: 2026-11-15

## Context

ADR-0043 reads the managed configuration when the application starts and
again whenever the platform says it changed, and the control service judged
its callers against that copy.

The platform tells a running process of a new configuration by
`ACTION_APPLICATION_RESTRICTIONS_CHANGED`, delivered to a receiver registered
at run time. A device policy controller typically sets the configuration and
asks the control service to start sharing straight away. When the request
arrived before the broadcast, the copy still named no controller, and the
request was refused — without a word in the log, since a list with nobody on
it was refused before anything was written. On a real device this is the
usual order of things: the application is already running when the
controller is set up.

## Decision

- The control service reads the application restrictions at every request
  and judges the caller against what it read. The read is published as well,
  so the screens and the locks follow it without waiting for the broadcast.
- A read that fails refuses the caller. An older copy is not used for this:
  it may still name a controller the configuration has since dropped.
- A refusal is written to the log with its reason — the configuration could
  not be read, it names no controller, the caller's package is not on the
  list, or the package is listed with another certificate — together with
  the uid and its packages. The same refusal for the same uid is written at
  most once a minute.
- The caller is still told nothing about why it was refused.

## Consequences

- A controller added while the application runs is let in on its first
  request.
- Every request costs a read of the restrictions, which is a call into the
  system; requests come one at a time and rarely, so this is not measurable.
- A controller whose certificate digest is wrong in the configuration can be
  told from one that is not listed at all by reading the log.
