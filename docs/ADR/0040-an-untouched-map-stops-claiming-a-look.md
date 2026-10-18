# ADR-0040: A map nobody touches stops claiming that somebody is looking

- Status: Accepted
- Date: 2026-10-18

## Context

ADR-0010 makes the devices of a group collect every thirty seconds while
somebody has the map open, through a watch claim the viewer renews. The page
already treated a tab nobody had touched for five minutes as nobody looking
and took its claim back. The Android client tied its claim to the screen being
on and the map being in front, and nothing else.

A device left on a charger with the screen kept on and the map in front
renewed its claim around the clock. From 2026-10-16 one such device held the
whole group in the viewing mode for two days: about 200 writes and 250 reads
an hour instead of about 30, roughly a quarter of the free daily writes, and
every member's battery spent on a map nobody was looking at.

## Decision

The Android client takes the same rule as the page: a map not touched for five
minutes (`IDLE_AFTER`, the same value as the page's `IDLE_AFTER_MS`) stops
counting as somebody looking. The claim is withdrawn and the device's own
collection hears that the map has closed; the next touch claims again at once,
and coming back to the map counts as a touch. Touches are taken from the
activity, on the monotonic clock.

## Consequences

- A map forgotten on a lit screen costs the group at most five minutes of the
  viewing mode, and then the expiry of the claim (ADR-0010) on top.
- A member who follows somebody without touching the screen for five minutes
  sees the markers age at the slower pace until they touch it; following a
  moving member normally involves touching the map now and then.
- Both clients now end a look by the same rule, so a reader of the claims need
  not care which kind of client wrote them.
