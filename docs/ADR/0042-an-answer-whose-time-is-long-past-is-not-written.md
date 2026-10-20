# ADR-0042: An answer whose time is long past is not written

- Status: Accepted
- Date: 2026-10-20
- Supersedes: ADR-0041

## Context

ADR-0041 left out an answer stamped more than ten minutes behind the device's
clock and named it "a place saved long ago", on the guess that the provider of
an old phone had handed over a place it had saved a year before.

The application's own log on that phone showed otherwise. The phone was lying
at home. On 2026-10-19 at 21:22 its receiver answered with a place 290
kilometres away, using the satellites of the GPS band and none of GLONASS, and
two seconds later with another place 41 metres from the first: a moving
position, not a stored one. The answers were left out, which after 3466 in a
row made the device ask the towers and the Wi-Fi; that fallback handed back
the same satellite answer, 35 057 976 seconds (about 406 days) old by its own
time but fresh by the clock since boot, which is the clock the fallback
measures age on. It was written past the filter. The time the receiver
reported, 2025-09-09, is what a transmitter replaying a recording made then
would give it.

A place saved earlier would carry an old time too, so the rule itself stands;
its name and its reason were wrong.

## Decision

The rule of ADR-0041 is kept as it is: an answer whose own time is more than
ten minutes behind the device's clock is left out, first and for every answer,
those of the towers and the Wi-Fi included, whether or not the member keeps
jumps out, and neither the towers agreeing nor the device coming home lets it
through.

The reason is named for what is seen rather than for a guess at its cause:
`PastTime`, "A time long past". The help explains it as a false signal
replaying a recording, or a place the device saved earlier.

## Consequences

- A replayed recording is caught by the time it carries even where the place
  it names passes every other rule, and even where it reaches the device
  through the fallback to the towers.
- The reason `Saved` was only ever written by test devices, for less than a
  day; the clients do not keep a name for it.
- A device whose own clock runs more than ten minutes fast leaves out every
  answer with this reason, as under ADR-0041.
