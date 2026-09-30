# ADR-0014: The jump filter is a setting, off by default, and always logged

- Status: Accepted
- Date: 2026-09-13 (recorded 2026-09-30)

## Context

Near jamming or spoofing of satellite signals a phone reports positions
kilometres away for seconds or minutes. Dropping such points hides the
problem; keeping them sends the marker across the map. What the jumps look
like on real phones was not yet known.

## Decision

- The phone keeps a log of what it received and what it would drop, and has a
  filter that drops implausible jumps.
- The filter is a setting on each device and is off by default, so that raw
  data is collected first.

## Consequences

- Devices where jumps are known can switch the filter on; the default may
  change once the logs show what the filter should do.
- The log costs storage on the phone and has to be cleaned up after debugging.
