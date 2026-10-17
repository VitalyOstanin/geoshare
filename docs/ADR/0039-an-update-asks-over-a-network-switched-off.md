# ADR-0039: An update asks before it comes down over a network switched off

- Status: Accepted
- Date: 2026-10-17

## Context

ADR-0022 lets the member choose the networks the application downloads its
own files over, and puts under that choice only what the application fetches
by itself. The update is not one of those: it starts when the member presses
"Update", so it went over whatever network the device stood on. But the file
is about thirty megabytes, the button does not say so, and a member in
roaming who pressed it to see what happens could pay for every one of them.

## Decision

- Pressing "Update" on a network the member switched off under Downloads,
  roaming among them, opens a question first: the size of the update and
  which network it would go over, with "Download" and "Not now".
- On a network the member allowed, the download starts at once, as before.
- The question is asked on every press and remembers nothing: the press is
  already the member's own, and the answer holds for that one download.
- The daily check for a new release is a request of a few kilobytes and goes
  over any network, as the places shared with the group do.

## Consequences

- An update over a network switched off costs one more tap, and only there.
- Unlike the background downloads of ADR-0022, a network switched off at home
  does not make the update wait; the member can still download it after
  being told the size.
