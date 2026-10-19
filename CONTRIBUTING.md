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
- [What changes together](#what-changes-together)
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
- A change a user would notice gets a line under the unreleased version in
  both changelogs, as [below](#what-changes-together) says.
- Text on the screen comes in English and Russian.
- A commit's subject starts with the part it changes, capitalised and
  followed by a colon: `Android:`, `Page:`, `Setup page:`, `Rules:`,
  `Tools:`, `Docs:`, `Release:`. A change across several parts goes without
  one. The rest of the subject says what changed for whoever uses that part.

## What changes together

What the application does for each role is written down in
[docs/stories/](docs/stories/README.md), and why it does it that way in
[docs/ADR/](docs/ADR/README.md). A change of behaviour changes, in the same
commit:

| № | What                                 | When                                                                                                                                                             |
|---|--------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | the story                            | its status, its acceptance lines, its `Tests` and `Not covered`; a new behaviour gets a new story with a new id                                                  |
| 2 | the tests that hold it               | the rules' tests first, since the rules decide who may do what; a test written for a story names the story's id in a comment above it                            |
| 3 | an ADR                               | when the change is a decision somebody would otherwise have to reconstruct from the code; an accepted ADR's body is never rewritten, a new one supersedes it     |
| 4 | the table of records against stories | at the end of `docs/stories/README.md`                                                                                                                           |
| 5 | the wording                          | a new field, type or on-screen word takes its name from [GLOSSARY.md](GLOSSARY.md), or adds the word there                                                       |
| 6 | `README.md` and `README.ru.md`       | what the project does, how to get it and how to set a group up must not read differently in the two; the screenshots are shared, each language keeps its wording |
| 7 | `CHANGELOG.md` and `CHANGELOG.ru.md` | the application shows a member the section in their language, so an entry in one is an entry in the other                                                        |

`web/tests/stories.test.mjs` checks that every record and test a story names
exists and that the table matches the stories. It does not check that the
acceptance lines are true; the tests named do. It fails on a change that
leaves the table or a story out of step, which is the usual sign that a row
above was skipped.

## Reporting a problem

Open an issue with the version from the bottom of the application's settings,
the Android version and model, and what happened against what you expected.

A security problem is not an ordinary issue: report it privately as
[SECURITY.md](SECURITY.md) describes.
