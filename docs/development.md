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
   switch to high accuracy at a thirty-second interval for as long as it
   stands.
4. When the claim runs out, each device falls back to the mode it was in.

This is what makes the free plan enough: no Cloud Functions, no messaging
service, no server; a position is written only when it moved far enough or
enough time passed, so a device lying still costs almost nothing.

## Collection modes and battery

| # | Mode      | When                                    | Location request | Interval |
|---|-----------|-----------------------------------------|------------------|----------|
| 1 | `Idle`    | nobody looking, inside the geofence     | none             | none     |
| 2 | `Standby` | nobody looking                          | balanced power   | 15 min   |
| 3 | `Moving`  | nobody looking, the device is moving    | balanced power   | 5 min    |
| 4 | `Viewing` | somebody has the map open               | high accuracy    | 30 s     |
| 5 | `Night`   | 23:00–07:00, or battery under 15%       | low power        | 20 min   |

The first that applies wins, in the order `Viewing`, `Idle`, `Night`,
`Moving`, `Standby`: a device inside its geofence stays idle at night too.

High accuracy is reserved for the minutes somebody is actually watching, and a
browser tab left open on a desk stops counting after five minutes. No
mode waits longer than the half hour after which a position is drawn as old,
so a working device never looks like a stopped one.

## Building

Every build, test and lint runs in a container, so for those the machine
needs only podman. Setting a project up, writing the page's configuration and
publishing run the Firebase command line on the machine itself, and need
`node` with `npx` and `python3` besides — the full list is in
[new-project.md](new-project.md).

| #  | Command                 | What it does                                                                |
|----|-------------------------|-----------------------------------------------------------------------------|
| 1  | `tools/build.sh`        | builds the Android application                                              |
| 2  | `tools/test.sh`         | runs the Android unit tests                                                 |
| 3  | `tools/lint.sh`         | formatting check and Android lint                                           |
| 4  | `tools/rules.sh`        | security rules tests against the Firestore emulator                         |
| 5  | `tools/web-test.sh`     | the tests of the page's own logic                                           |
| 6  | `tools/js-lint.sh`      | ESLint over the page and the emulator scripts                               |
| 7  | `tools/sign-release.sh` | signs the release build with the release key                                |
| 8  | `tools/check.sh`        | the checks a change needs, `--all` for every one; CI runs it                |
| 9  | `tools/run-services.sh` | stand-in Firestore and sign-in, filled with stand-in data                   |
| 10 | `tools/run-emulator.sh` | a headless Android emulator; with the line above, a stand without a project |
| 11 | `tools/coverage.sh`     | how much of the code the Android and the page's tests reach                 |

The rest of `tools/` — `project.sh`, `web-config.sh`, `setup-site.sh`,
`read-cost.sh`, `device-log.sh`, `device-sky.sh`, `siren-sound.sh` — says
what it does and how to call it at the top of each file.

`VARIANT=release tools/build.sh` makes the unsigned release package;
`tools/sign-release.sh` signs it where the key is kept and refuses a key whose
fingerprints are not the ones in
[android/release-fingerprints.txt](../android/release-fingerprints.txt).

### Environment variables

The scripts read these from the environment; none has to be set.

| #  | Variable              | Script                         | What it changes                                                    | Default                          |
|----|-----------------------|--------------------------------|--------------------------------------------------------------------|----------------------------------|
| 1  | `VARIANT`             | `build.sh`                     | `debug` or `release`                                               | `debug`                          |
| 2  | `JOBS`                | all containers (`lib.sh`)      | CPUs a container may take                                          | `4`                              |
| 3  | `MEMORY`              | all containers (`lib.sh`)      | memory a container may take                                        | `6g`                             |
| 4  | `ANDROID_SERIAL`      | `gradle.sh`, `device-*.sh`     | the device to install on or read from                              | whatever adb picks               |
| 5  | `EMULATOR_VARIANT`    | `run-emulator.sh` (`lib.sh`)   | system image: `google_apis`, or `default` for one without Google   | `google_apis`                    |
| 6  | `BOOT_TIMEOUT`        | `run-emulator.sh`              | seconds to wait for the emulator to boot                           | `300`                            |
| 7  | `READY_TIMEOUT`       | `run-services.sh`              | seconds to wait for the Firestore and sign-in emulators            | `120`                            |
| 8  | `ADB_HOST_PORT`       | `lib.sh`                       | host port of the emulator's adb server, beside the machine's 5037  | `5038`                           |
| 9  | `ADB_SERVER_SOCKET`   | `lib.sh`                       | the adb server the scripts talk to                                 | `tcp:127.0.0.1:$ADB_HOST_PORT`   |
| 10 | `COST_SCRIPT`         | `read-cost.sh`                 | which measurement under `firebase/tests/cost/` runs                | `read-cost`                      |
| 11 | `SIREN_SOUND_TARGET`  | `siren-sound.sh`               | where the generated siren sound is written                         | `android/.../raw/siren_sos.ogg`  |
| 12 | `FIREBASE_PROJECT`    | `web-config.sh`                | the project the page is configured for                             | the working copy's default       |
| 13 | `FIREBASE_SITE`       | `web-config.sh`                | its Hosting site, where not named after the project                | the project id                   |
| 14 | `FIREBASE_ACCOUNT`    | `web-config.sh`                | the account the Firebase CLI acts as                               | the CLI's own                    |
| 15 | `WEB_CONFIG_TARGET`   | `web-config.sh`                | where the page's configuration is written                          | `web/public/firebase-config.js`  |
| 16 | `GEOSHARE_RELEASE_*`  | `sign-release.sh`              | the release key: keystore, alias, password                         | none; see the script's header    |
| 17 | `HTTPS_PROXY`         | all containers (`lib.sh`)      | handed to image builds and containers when set                     | none                             |
| 18 | `CLOUD_SHELL`         | `project.sh`                   | set by Google Cloud Shell; sign-in without a local browser         | none                             |

The names of the podman resources — `POD_NAME`, `SERVICES_NAME`,
`EMULATOR_NAME`, `SDK_IMAGE`, `EMULATOR_IMAGE`, `FIREBASE_IMAGE`,
`CACHE_VOLUME`, `KEY_VOLUME`, `NPM_VOLUME` — can be changed too, which is only
of use to keep two checkouts apart on one machine. The emulator ports are not
variables: they are fixed in `firebase.json`.

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
