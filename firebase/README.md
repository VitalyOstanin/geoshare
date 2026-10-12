# The Firebase side

Everything here is either a file the project deploys or a note about a setting
that only exists in the console. There is no server code: the devices and the map
page talk to Firestore directly, and the rules below are what stands between
them.

## Files

| File                     | What it is                                                  |
|--------------------------|-------------------------------------------------------------|
| `firestore.rules`        | Who may read and write what                                 |
| `firestore.indexes.json` | Composite indexes; empty, every query is by document        |
| `data-model.md`          | The documents and their fields                              |
| `tests/`                 | The rules, checked against the emulator                     |
| `emulator/`              | Seeds for the emulator: a stand-in group and a showcase one |

`firebase.json` in the repository root points at the first two and configures
the emulator.

## Setting the project up

A project is created by the setup page or by `tools/project.sh`; the app reads
its settings from `geoshare.json` at run time, so nothing is put into the
build. The steps are in:

- [Getting started](../README.md#getting-started) in the root README;
- [docs/new-project.md](../docs/new-project.md), creating a project;
- [docs/console-steps.md](../docs/console-steps.md), the settings that exist
  only in the console.

## Deploying the rules

Either paste `firestore.rules` into the rules editor in the console, or use the
command line from a checkout:

```
firebase login
firebase deploy --only firestore:rules --project <project-id>
```

On a machine with no browser of its own, `firebase login --no-localhost` prints
a link and then exits; the code that link hands back goes into a second command,
`firebase login <code>`, rather than into the first one's input.

The project identifier is not committed: `.firebaserc` is ignored, so either the
`--project` argument carries it or `firebase use --add` writes it locally.

## Checking the rules

```
tools/rules.sh
```

Starts the Firestore emulator and runs `tests/` against it. Nothing reaches the
real service: the run uses the project id `demo-geoshare`, and the `demo-`
prefix is what keeps both the emulator and the client library local. That also
means the check works before the console project exists.
