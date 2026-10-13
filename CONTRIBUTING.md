# Contributing

geoshare is my pet project, built for my own use. Anyone who finds it useful
is welcome to use it.

Patches are welcome, and I am glad to review them. A change to the
architecture — how the data is stored, what the security rules allow, how
the application talks to the project, a new dependency or service — is
better discussed first: open an issue describing the idea, and send the pull
request once we have agreed on the approach. That saves work on a change
that might not fit.

## Contents

- [Before you start](#before-you-start)
- [Building and checking](#building-and-checking)
- [Changes](#changes)
- [Reporting a problem](#reporting-a-problem)

## Before you start

The application and the page read the positions of real people. A change that
widens who may read what goes through the security rules in
[firebase/firestore.rules](firebase/firestore.rules) and their tests first, and
the clients follow.

Read [GLOSSARY.md](GLOSSARY.md) before naming anything: the project says
"device" and "administrator" on the screen and in the documents, and a new
field takes a word from there or adds one.

Decisions that shape the project are recorded in [docs/ADR](docs/ADR). A change
that goes against one comes with a new ADR that supersedes it.

## Building and checking

[docs/development.md](docs/development.md) describes the build, the scripts in
`tools/` and the stand-in database and emulator that need no Firebase project.

Before sending a change, run the checks it needs:

```sh
tools/check.sh
```

It reads the working tree and runs only the checks the changed files can
affect; `tools/check.sh --all` runs every one.

## Changes

- Fixes and small improvements can come straight as a pull request; larger
  or architectural ones start with an issue, as said at the top.
- One change per pull request, with tests for what it fixes or adds.
- A change a user would notice gets a line in [CHANGELOG.md](CHANGELOG.md)
  under the unreleased version.
- Text on the screen comes in English and Russian.

## Reporting a problem

Open an issue with the version from the bottom of the application's settings,
the Android version and model, and what happened against what you expected.

A security problem is not an ordinary issue: report it privately as
[SECURITY.md](SECURITY.md) describes.
