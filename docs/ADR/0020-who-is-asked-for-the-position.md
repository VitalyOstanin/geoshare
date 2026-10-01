# ADR-0020: Who is asked for the position

- Status: Accepted
- Date: 2026-10-02

## Context

The position came from the fused provider of Play Services alone. A device
without Play Services, such as a tablet with only the platform's own
providers, reported nothing at all. And the fused provider picks its sources
itself: a member who switched the towers and the Wi-Fi off still had them
asked, and their answers were only thrown away on arrival.

## Decision

- The fused provider of Play Services stays the default: it batches and
  blends the sources at the lowest cost.
- The platform's own providers are asked instead when Play Services are
  missing, or when the satellites or the towers with the Wi-Fi are switched
  off in the settings. Then only the sources left on are asked.
- Without Play Services the platform's own blend ("fused") is preferred where
  it exists; otherwise the fast mode asks the receiver and the network, and
  the slower modes ask the network alone.

## Consequences

- Without Play Services there is no geofence and no activity recognition, so
  such a device never enters the quiet or the moving mode and spends a little
  more at home.
- A switched-off source no longer costs battery. With the towers off, a device
  indoors gets no network answer to fall back on; such answers were left out
  before as well.
- The second opinion against spoofing still asks the network, whatever the
  switches say: it is compared, never written.
