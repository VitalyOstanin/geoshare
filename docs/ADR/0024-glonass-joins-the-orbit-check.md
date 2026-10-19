# ADR-0024: GLONASS joins the orbit check

- Status: Accepted
- Date: 2026-10-02
- Supersedes: the point of ADR-0023 that only GPS and Galileo are read

## Contents

- [Context](#context)
- [Decision](#decision)
- [Consequences](#consequences)

## Context

ADR-0023 left GLONASS out: its orbits are not ellipses but a position, a
speed and the pull of the Sun and the Moon every half hour, and the position
at any other moment has to be found by following the satellite through
Earth's gravity. A transmitter on the GPS band does show up in GPS and
Galileo alike, but a receiver that leans on GLONASS where GPS is drowned out
is left unchecked.

Tried on the devices' own records:

- followed from yesterday's file, an honest GLONASS satellite is off by
  about 1.5 km, the same for all of them, with a spread of a tenth of that;
  never more than 2.4 km, against the 200 the check allows;
- a GLONASS satellite tells only the time of the Moscow day it sent at, and
  only the frequency channel shared by two satellites on opposite sides of
  Earth, when its slot is not yet known;
- false GLONASS skies were heard: whole seconds and 5.6 s ahead, and once
  stamped with GPS time instead of Moscow time. The last does not agree with
  the satellites' own Doppler, so it is the transmitter's, not the receiver's
  slip.

## Decision

- GLONASS records of the daily file are read too: four lines each, the
  unhealthy ones left out. Their time is UTC and is moved to the GPS scale.
- The position at the moment of the epoch is found by Runge-Kutta of the
  fourth order with a one-minute step, in Earth's turning frame, with the
  flattening term and the record's pull of the Sun and the Moon, as the
  interface document of the system describes. Where the satellite was last
  followed is remembered, so that the next epoch costs a step.
- The time a GLONASS signal travelled is taken against the Moscow time of
  day: the receiver's GPS time less the 18 leap seconds plus three hours.
- A satellite heard only by its channel is held against each satellite on
  that channel, and the one standing higher above the horizon of the place is
  taken: the other one is on the far side of Earth.
- The limits of ADR-0023 stay as they are: 3° below the horizon, 200 km not
  a whole number of milliseconds, two epochs in a row.

## Consequences

- An epoch is checkable sooner: GLONASS satellites count towards the four,
  and a phone that hears GLONASS clearly where GPS is weak is checked.
- A GLONASS signal stamped with GPS time comes out about three hours off and
  is reported as off its orbits, as it should be.
- Following an orbit for up to a day costs about 1,500 small steps once per
  satellite; after that each epoch costs one.
- No new download: the GLONASS records are in the same file.
