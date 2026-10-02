# ADR-0022: Background downloads follow the member's network choice

- Status: Accepted
- Date: 2026-10-02

## Context

The spoofing check is to compare the sky the receiver hears with the orbits
published for it, which means downloading a file of about 1.2 MB a day. It is
the first thing the application downloads on its own, and others may follow.
Roaming can cost the member real money, and a download nobody asked for is
the last place it should go.

## Decision

- The settings carry three switches, Wi-Fi, the mobile network at home and
  roaming, in a section of their own. Out of the box the first two are on
  and roaming is off.
- A network switched off at home means the download waits for another one.
  Roaming switched off means the member is asked each time, by a quiet
  notification with "Download" and "Not now". "Download" lets one download
  go; "Not now", or swiping the notification away, holds the question back
  for a day.
- Only what the application downloads by itself falls under this. The map
  is fetched because the member opened it, and the places shared with the
  group are a few kilobytes the application exists for; both go over any
  network.
- Under a VPN the networks underneath the tunnel are read, since the tunnel
  itself says nothing about roaming.

## Consequences

- In roaming the spoofing check works only after the member says yes, once
  a day, unless they switch roaming on.
- A member who switches both home networks off gets no background downloads
  at all; the settings say which networks are on, and nothing more.
- Every later background download has to go through the same gate rather
  than fetch on its own.
