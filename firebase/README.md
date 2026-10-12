# The Firebase side

Everything here is either a file the project deploys or a note about a setting
that only exists in the console. There is no server code: the devices and the map
page talk to Firestore directly, and the rules below are what stands between
them.

## Contents

- [Files](#files)
- [Setting the project up](#setting-the-project-up)
- [Deploying the rules](#deploying-the-rules)
- [Updating a group's project](#updating-a-groups-project)
- [Checking the rules](#checking-the-rules)

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

## Updating a group's project

Every group's project is updated by its project owner, in their own time,
while the members' devices take a new application whenever they do. For a
while one set of rules serves several versions of the application, and the
rules change with that in mind:

- A change to the rules widens what is allowed and narrows nothing an
  application still in use writes: a new field is accepted beside the old
  one, a new collection is allowed before any application writes to it.
  Refusing what an older application writes waits for a later release, once
  that application is out of use, and its notes say so.
- Every version's section of [CHANGELOG.md](../CHANGELOG.md) says under
  **Project owners** whether the release needs `tools/project.sh update` and
  in which order: the rules first when the new application writes something
  the old rules refuse, the application first when the rules stop accepting
  what the old one wrote.
- The update deploys the working copy, so the release's tag is checked out
  first:

  ```
  git checkout v0.2.0
  tools/project.sh update <project-id>
  ```

- Going back is the same with the earlier tag. Rules taken back past a
  widening refuse what the newer application writes, so the devices go back
  to the earlier application before the rules do.

## Checking the rules

```
tools/rules.sh
```

Starts the Firestore emulator and runs `tests/` against it. Nothing reaches the
real service: the run uses the project id `demo-geoshare`, and the `demo-`
prefix is what keeps both the emulator and the client library local. That also
means the check works before the console project exists.
