# ADR-0013: English and Russian, chosen per application

- Status: Accepted
- Date: 2026-09-13 (recorded 2026-09-30)

## Context

The first users speak Russian; a public project needs English. Both clients
must switch language without the application keeping a setting of its own
that could disagree with the system's.

## Decision

- Android uses per-app language preferences through AppCompat
  (`AppCompatActivity`, `autoStoreLocales`), so on Android 13 and later the
  choice lives in the system's settings for the application and both places
  agree.
- The page loads its words from a dictionary per language and reloads when the
  language changes; the markup keeps the English text as the first frame.
- Strings that must not be translated are marked `translatable="false"`, and a
  test checks both languages are complete.

## Consequences

- The Android theme descends from `Theme.AppCompat`.
- Switching language on the page costs a reload instead of redrawing every
  panel in place.
