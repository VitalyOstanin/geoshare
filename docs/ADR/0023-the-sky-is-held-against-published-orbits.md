# ADR-0023: The sky is held against the published orbits

- Status: Accepted; which systems are read superseded by ADR-0024
- Date: 2026-10-02

## Context

Every rule the jump filter had so far reads either the answers the receiver
gives or what the receiver hears, and all of that is the transmitter's to
choose. The orbits the satellites broadcast are among it: a false sky comes
with orbits that agree with it. The same orbits are also collected by
stations all over the world and published as one file a day, which nothing
near the device can reach.

Tried on the devices' own records: under the transmitter the distances the
signals measured were off by a thousand kilometres and more from the
distances to the published positions, seen from where the device really was;
on clean records nothing was off by more than seventy. Orbits a day old are
within a kilometre and a half of fresh ones, three days old within six, a
week old thousands of kilometres off.

## Decision

- Once a day the service downloads yesterday's merged broadcast orbits from
  the IGS archive at BKG (about 1.2 MB), through the downloads rule of
  ADR-0022; four other names of the same file are tried when the first is
  missing. One file is kept.
- Only GPS and Galileo are read. Both describe an orbit the same way; GLONASS
  would need its orbit integrated forward, and a transmitter on the GPS band
  shows up in GPS and Galileo alike.
- Every epoch of raw measurements is held against the orbits from a place the
  satellites did not give: the towers and the Wi-Fi, or failing them the last
  position the filter wrote, either no older than half an hour.
- An epoch is off its orbits when a satellite heard stands more than 3°
  below the horizon there, or its distance, with the receiver's clock taken
  out by the median, is off by more than 200 km and not by a whole number of
  milliseconds (the receivers get the millisecond wrong now and then).
- Two such epochs in a row, the newer no older than ten seconds, leave the
  fix out under a reason of its own.

## Consequences

- The check needs raw measurements with the time of week decoded, at least
  four satellites, a day-old file and a recent place from the towers; where
  any of it is missing it says nothing rather than something.
- A transmitter imitating the real sky from the device's own place — a
  replay on the spot — is not caught by this; the motion sensors are for that.
- The device fetches about 36 MB a month for it.
