# ADR-0041: A place saved long ago is not written as the current one

- Status: Superseded by ADR-0042
- Date: 2026-10-20

## Context

A position carries two moments: the one the answer was taken at
(`takenAt`, from the provider) and the one it was written at (`writtenAt`).
The rules that judge an answer before it is written compare it with earlier
answers, with the towers and the Wi-Fi and with the device's sensors, but
none of them looked at how old the answer itself was.

On 2026-10-19 an old phone in a group wrote a position taken on 2025-09-09,
406 days before it was written, far from where the phone was. The provider
most likely handed over the place it had saved the last time it was used as
if it were current. No rule left it out, and the map went on showing it as
the device's last position.

## Decision

An answer whose own moment is more than ten minutes behind the device's
clock is left out with the reason `Saved` ("A place saved long ago"), and
counted among the answers left out like any other. The rule is checked first
and for every answer, those of the towers and the Wi-Fi included, and an
answer it leaves out cannot be let through by the towers agreeing with it or
by the device coming home. It holds whether or not the member keeps jumps
out: that setting is about answers that may be true, and this one says
where the device was on another day.

Ten minutes lets through an answer that took seconds or minutes to arrive,
and stops a place saved on an earlier day. An answer whose moment is ahead of
the device's clock is not judged by this rule.

## Consequences

- A device whose provider first answers with a saved place writes nothing
  until it has a current answer, instead of showing the group a place it may
  have left a year ago.
- A device whose own clock runs more than ten minutes fast leaves out every
  answer as saved long ago, and the card says so with the reason; such a
  clock is already wrong for everything else that is timed.
- A position written before this rule stays in the database until the device
  writes the next one.
