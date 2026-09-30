# ADR-0006: MapLibre on both clients, OpenStreetMap raster tiles

- Status: Accepted
- Date: 2026-09-11 (recorded 2026-09-30)

## Context

Both the page and the phone draw a map. The Google Maps SDK for Android needs
billing enabled on the project even where the map loads are free, and enabling
billing moves a Firebase project from Spark to Blaze, taking away the hard cap
that keeps it free. The page started on Leaflet and the phone on osmdroid;
Leaflet was set aside by the owner's choice, and its last release was then
more than a year old.

## Decision

- The page uses MapLibre GL JS, the phone MapLibre Native.
- The background is OpenStreetMap raster tiles, which need no key and no
  payment account.

## Consequences

- One map model on both clients: markers, camera and following behave alike.
- No billing account anywhere in a group's project.
- The tile servers' usage policy applies; another background (for example a
  free tier of a regional provider) can be added as a choice later.
