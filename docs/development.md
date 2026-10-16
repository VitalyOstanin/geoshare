# Development

How geoshare works inside, how to build it and where things are. What the
project is for and how to start using it is in the [README](../README.md).

## Contents

- [How it works](#how-it-works)
- [Collection modes and battery](#collection-modes-and-battery)
- [Building](#building)
- [Releasing](#releasing)
  - [Checking a release](#checking-a-release)
  - [Rolling back](#rolling-back)
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
`node` 20 or later with `npx` besides — the full list is in
[new-project.md](new-project.md). Running `npm test` or `npm run coverage` in
`web/` or `firebase/tests/` directly on the machine needs `node` 24.12 or later
(the `engines` of both manifests, as in the test container, and what the
Firebase SDK in them requires); the build scripts do not need it.

| #  | Command                      | What it does                                                                |
|----|------------------------------|-----------------------------------------------------------------------------|
| 1  | `tools/build.sh`             | builds the Android application                                              |
| 2  | `tools/test.sh`              | runs the Android unit tests                                                 |
| 3  | `tools/lint.sh`              | formatting, length of functions (detekt) and Android lint                   |
| 4  | `tools/rules.sh`             | security rules tests against the Firestore emulator                         |
| 5  | `tools/web-test.sh`          | the tests of the page's own logic                                           |
| 6  | `tools/js-lint.sh`           | ESLint over the page and the emulator scripts                               |
| 7  | `tools/md-lint.sh`           | markdownlint over the documents                                             |
| 8  | `tools/serve-web.sh`         | the page at 127.0.0.1:5000, with the headers and the policy Hosting sends   |
| 9  | `tools/sign-release.sh`      | signs the release build with the release key                                |
| 10 | `tools/check.sh`             | the checks a change needs, `--all` for every one; CI runs it                |
| 11 | `tools/run-services.sh`      | stand-in Firestore and sign-in, filled with stand-in data                   |
| 12 | `tools/run-emulator.sh`      | a headless Android emulator; with the line above, a stand without a project |
| 13 | `tools/coverage.sh`          | how much of the code the tests reach; fails under the floors CI checks too  |
| 14 | `tools/test-instrumented.sh` | smoke tests on the emulator: start-up, settings, licences, connecting       |
| 15 | `tools/shell-lint.sh`        | ShellCheck over every shell script, at the version in `versions.env`        |
| 16 | `tools/page-stand.sh`        | the page against stand-in sign-in and Firestore, `--showcase` to fill them  |
| 17 | `tools/landing-shots.sh`     | the application's screenshots for the landing page, full size, in `shots/`  |
| 18 | `tools/landing-steps.sh`     | the setup page's first two steps for the landing page, into `shots/`        |
| 19 | `tools/landing-images.sh`    | the landing page's pictures, three widths each, from `shots/`               |

The rest of `tools/` — `coverage-floor.sh`, `project.sh`, `web-config.sh`,
`setup-site.sh`, `read-cost.sh`, `device-log.sh`, `device-sky.sh`,
`siren-sound.sh`, `signing.sh`, `vendor-web.sh` — says what it does and how to
call it at the top of each file.

The page serves Firebase, MapLibre GL JS and the QR encoder itself, from
`web/public/lib/vendor/<package>-<version>/` (ADR-0035). Their versions are
pinned in `web/package.json`, where Dependabot raises them; the page's tests
then fail until `tools/vendor-web.sh` has copied the new versions in and
`lib/vendor` is committed with the bump.

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
| 18 | `TIMEOUT`             | `test-instrumented.sh`         | how long the instrumented run may take as a whole                  | `30m`                            |
| 19 | `WEB_PORT`            | `serve-web.sh`                 | the port the page is served on                                     | `5000`                           |
| 20 | `GRADLE_TIMEOUT`      | `gradle.sh`                    | how long one Gradle run may take as a whole                        | `60m`                            |
| 21 | `STAND_WEB_PORT`      | `page-stand.sh`                | the port the stand's page is served on                             | `5000`                           |
| 22 | `STAND_FIRESTORE_PORT`| `page-stand.sh`                | the port of the stand's Firestore emulator                         | `18080`                          |
| 23 | `STAND_AUTH_PORT`     | `page-stand.sh`                | the port of the stand's sign-in emulator                           | `19099`                          |
| 24 | `READY_TIMEOUT`       | `page-stand.sh`                | seconds to wait for the stand's emulators                          | `120`                            |
| 25 | `SHOTS_DIR`           | `landing-shots.sh`             | where the full-size screenshots go                                 | `shots/<date>`                   |
| 26 | `SHOTS_DIR`           | `landing-steps.sh`             | the same, for the setup page's steps                               | `shots/<date>`                   |
| 27 | `WAIT_TIMEOUT`        | `landing-shots.sh`             | seconds to wait for a screen to show what is tapped next           | `30`                             |
| 28 | `SETUP_URL`           | `landing-steps.sh`             | the setup page the steps are taken from                            | the published setup page         |
| 29 | `QUALITY`             | `landing-images.sh`            | WebP quality of the pictures                                       | `80`                             |

The names of the podman resources — `POD_NAME`, `SERVICES_NAME`,
`EMULATOR_NAME`, `STAND_NAME`, `SDK_IMAGE`, `EMULATOR_IMAGE`, `FIREBASE_IMAGE`,
`CACHE_VOLUME`, `KEY_VOLUME`, `NPM_VOLUME`, `MODULES_VOLUME` — can be changed
too, which is only of use to keep two checkouts apart on one machine. The ports of the
emulators the Android stand talks to are not variables: they are fixed in
`firebase.json`; only the page stand's are, since its browser reaches them from
the machine.

## Releasing

1. Raise `appVersionName` in `android/gradle.properties`; the version code is
   worked out from it, 0.1.0 being 100.
2. Give the version's section of [CHANGELOG.md](../CHANGELOG.md) its date
   and say under **Project owners** whether the release needs
   `tools/project.sh update`, and in which order
   ([Updating a group's project](../firebase/README.md#updating-a-groups-project)).
   `tools/release-notes.sh <version>` prints what the release will say.
3. Run the package that will be published. The checks build and test the
   debug build only, and R8 shrinks the release one: a class that Firestore
   or reflection reaches only at run time can be removed without any check
   noticing. Build and sign it, `VARIANT=release tools/build.sh` and then
   `tools/sign-release.sh`, install `build/geoshare-<version>.apk` on a
   device or on the emulator of `tools/run-emulator.sh`, and go through
   starting, connecting to a group and seeing positions on the map.
4. Tag the commit on master with an annotated tag, `git tag -a v<version>`,
   and push the tag. `release.yml` runs the checks of `ci.yml`, refuses a
   lightweight tag, a tag off master, a tag that differs from
   `appVersionName` and notes without a date, then builds, signs, checks
   what the package says about itself and publishes it with its checksum and
   its bill of materials, `geoshare-<version>.cdx.json` in CycloneDX.

### Checking a release

Every package published since the repository became public carries two
attestations, signed for the run that built it: where it was built, and the
bill of materials it was built from. 0.1.0 has none: it was released while the
repository was private, and GitHub attests only for public repositories on the
free plan. Its checksum in `SHA256SUMS` is what there is to check it by. With
the [GitHub CLI](https://cli.github.com/):

```sh
gh attestation verify geoshare-<version>.apk --repo VitalyOstanin/geoshare
gh attestation verify geoshare-<version>.apk --repo VitalyOstanin/geoshare \
    --predicate-type https://cyclonedx.org/bom
```

The first says the package came out of `release.yml` in this repository, at
the tagged commit; the second, that the attached bill of materials is the one
built with it. A package that fails either did not come from here.

### Rolling back

Android does not install a package over one with a higher version code, so a
bad release is not taken back by pointing at an older one:

1. Revert the change on master and release it as the next patch version,
   following the steps above. Devices update to it like to any other.
2. Mark the bad release on GitHub as a pre-release and say in its notes what
   is wrong, so that nobody new installs it.
3. If the release changed the rules or the page, each project owner checks out
   the tag of the previous release and runs `tools/project.sh update` from it,
   once the devices have the fixed release: the older rules may refuse what
   the bad one writes. The rules usually need no rollback at all, since a
   release only widens them (see
   [Updating a group's project](../firebase/README.md#updating-a-groups-project)).

Nothing in the application is switched on or off from outside: there is no
server of the project's own to hold such switches. A feature that misbehaves
is taken out by a release.

## Layout

| # | Directory   | Contents                                                          |
|---|-------------|-------------------------------------------------------------------|
| 1 | `android/`  | Kotlin and Jetpack Compose application, Gradle build              |
| 2 | `web/`      | the map page and the setup page, plain JavaScript with MapLibre   |
| 3 | `firebase/` | security rules, indexes, document model, rules tests              |
| 4 | `docs/`     | architecture decisions, stories, console steps, project setup     |
| 5 | `tools/`    | build, test, setup and release entry points                       |
| 6 | `site/`     | the landing page in two languages, its pictures and fonts         |
| 7 | `.github/`  | CI, release and landing page workflows, Dependabot settings       |

## Further reading

- [docs/architecture.md](architecture.md): accounts, projects, sites, the
  group and the clients, and what a project gets for free.
- [GLOSSARY.md](../GLOSSARY.md): the words used throughout, and the documents
  the database holds with how they relate.
- [docs/ADR](ADR/README.md): the decisions behind the design.
- [firebase/data-model.md](../firebase/data-model.md): the document model and
  the reasons behind every rule.
- [docs/new-project.md](new-project.md): setting a group's project up from a
  terminal.
