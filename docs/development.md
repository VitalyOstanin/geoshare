# Development

How geoshare works inside, how to build it and where things are. What the
project is for and how to start using it is in the [README](../README.md).

## Contents

- [How it works](#how-it-works)
- [Collection modes and battery](#collection-modes-and-battery)
- [Building](#building)
- [Layout](#layout)
- [Further reading](#further-reading)

## How it works

1. Each device runs a foreground service holding an open Firestore
   subscription and writes one document per device with its last position.
2. A screen with the map in front of somebody — a device or a browser tab —
   writes a short-lived claim that it is looking, and refreshes it while it
   is being looked at.
3. The devices see the claim over the connection they already have open and
   switch to high accuracy at a fifteen-second interval for as long as it
   stands.
4. When the claim runs out, each device falls back to the mode it was in.

This is what makes the free plan enough: no Cloud Functions, no messaging
service, no server; a position is written only when it moved far enough or
enough time passed, so a device lying still costs almost nothing.

## Collection modes and battery

| # | Mode      | When                              | Location request | Interval |
|---|-----------|-----------------------------------|------------------|----------|
| 1 | `Standby` | nobody looking                    | balanced power   | 15 min   |
| 2 | `Viewing` | somebody has the map open         | high accuracy    | 30 s     |
| 3 | `Night`   | 23:00–07:00, or battery under 15% | low power        | 20 min   |

High accuracy is reserved for the minutes somebody is actually watching, and a
browser tab left open on a desk stops counting after a quarter of an hour. No
mode waits longer than the half hour after which a position is drawn as old,
so a working device never looks like a stopped one.

## Building

Every build and test runs in a container, so the machine needs only podman.

| # | Command                   | What it does                                          |
|---|---------------------------|-------------------------------------------------------|
| 1 | `tools/build.sh`          | builds the Android application                        |
| 2 | `tools/test.sh`           | runs the Android unit tests                           |
| 3 | `tools/lint.sh`           | formatting check and Android lint                     |
| 4 | `tools/rules.sh`          | security rules tests against the Firestore emulator   |
| 5 | `tools/web-test.sh`       | the tests of the page's own logic                     |
| 6 | `tools/js-lint.sh`        | ESLint over the page and the emulator scripts         |
| 7 | `tools/sign-release.sh`   | signs the release build with the release key          |

`VARIANT=release tools/build.sh` makes the unsigned release package;
`tools/sign-release.sh` signs it where the key is kept and refuses a key whose
fingerprints are not the ones in
[android/release-fingerprints.txt](../android/release-fingerprints.txt).

## Layout

| # | Directory   | Contents                                                          |
|---|-------------|-------------------------------------------------------------------|
| 1 | `android/`  | Kotlin and Jetpack Compose application, Gradle build              |
| 2 | `web/`      | the map page and the setup page, plain JavaScript with MapLibre   |
| 3 | `firebase/` | security rules, indexes, document model, rules tests              |
| 4 | `docs/`     | architecture decisions, stories, console steps, project setup     |
| 5 | `tools/`    | build, test, setup and release entry points                       |

## Further reading

- [GLOSSARY.md](../GLOSSARY.md): the words used throughout, and the documents
  the database holds with how they relate.
- [docs/ADR](ADR/README.md): the decisions behind the design.
- [firebase/data-model.md](../firebase/data-model.md): the document model and
  the reasons behind every rule.
- [docs/new-project.md](new-project.md): setting a group's project up from a
  terminal.
