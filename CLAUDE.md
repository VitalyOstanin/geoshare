# geoshare

Notes for an AI agent working in this repository.

## Contents

- [Stories, records and tests change together](#stories-records-and-tests-change-together)
- [The README in two languages](#the-readme-in-two-languages)
- [Before a release](#before-a-release)

## Stories, records and tests change together

What the application does for each role is written down in
[docs/stories/](docs/stories/README.md), and why it does it that way in
[docs/ADR/](docs/ADR/README.md). A change of behaviour changes, in the same
commit:

1. the story: its status, its acceptance lines, its `Tests` and
   `Not covered`; a new behaviour gets a new story with a new id;
2. the tests that hold it — the rules' tests first, since the rules decide
   who may do what; a test written for a story names the story's id in a
   comment above it;
3. a new ADR when the change is a decision somebody would otherwise have to
   reconstruct from the code; an accepted ADR's body is never rewritten, a
   new one supersedes it;
4. the table of records against stories at the end of
   `docs/stories/README.md`.

`web/tests/stories.test.mjs` checks that every record and test a story names
exists and that the table matches the stories. It does not check that the
acceptance lines are true; the tests named do.

## The README in two languages

`README.md` is in English and `README.ru.md` beside it says the same in
Russian; each links to the other at the top. A change to one changes the
other in the same commit: what the project does, how to get it and how to
set a group up must never read differently in the two. The screenshots are
shared, and each language keeps its own wording rather than a word-for-word
translation. `CHANGELOG.md` and `CHANGELOG.ru.md` go together the same way:
the application shows a member the section in its own language, so an entry
in one is an entry in the other in the same commit.

## Before a release

Two steps of [Releasing](docs/development.md#releasing) are never skipped,
however small the release:

1. every document is read through again and brought up to date with what
   the release changes, not only the ones a change obviously touches;
2. every screenshot and video is taken again from the commit that will be
   released, the landing page's and the READMEs' alike.
