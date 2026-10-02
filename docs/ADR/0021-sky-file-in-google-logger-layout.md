# ADR-0021: The sky file is laid out as Google's GNSS logger lays out its own

- Status: Accepted
- Date: 2026-10-02

## Context

The sky file records what the receiver reported beside each position: the
receiver's clock, the gains, every satellite and every raw measurement. Its
rows took the names of the rows Google's GnssLogger writes, but not their
layout: the clock was a row of its own, the system was a word, the fields
stood in another order, and the head was written again at every start.
The tools made for that logger, such as gnss_lib_py, read none of it, and
every look at the data needed a converter of our own first.

## Decision

- The rows GnssLogger 3.1 writes (`Raw`, `Fix`, `Status`, `Agc`, `Nav`)
  are written as it writes them: the same names, the same fields in the
  same order, the clock in every `Raw` and `Agc` row, the system as
  Android's number, the provider as `GPS`, `FLP` or `NLP`, a navigation
  message as a decimal number to a byte.
- Fields the platform does not hand over (the satellite's own position and
  clock, the ionosphere's coefficients, the solution type) are left empty
  rather than left out.
- The rows of our own (`NoFix`, `Slant`, `Drift`, `Apart`) stay beside them
  under their own names, with the system as a number as well. A `Clock` row
  is written only for an epoch with no raw row to carry the clock.
- The head names the application's version, Android's, the phone and its
  receiver, and is written once at the top of each file, the second one
  after a rollover included. A file begun under the earlier layout is put
  aside as the previous one at start.

## Consequences

- gnss_lib_py reads the file as it is: the raw rows with their clock and
  systems, and the positions with their providers.
- The file grows by about a fifth for the same span of time, the clock
  being repeated in every raw row; with two files of 2 MB each it covers
  that much less.
- A navigation message no longer carries its own moment, as it does not in
  GnssLogger: it is placed by the blocks on either side of it.
- Files already on the phones keep the earlier layout; a reader of both has
  to take the column names from each file's own head rows.
