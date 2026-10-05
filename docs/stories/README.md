# User stories

What each kind of person does with geoshare, written as stories: who, what and
why, with what has to hold for the story to count as working. One file per
role; inside it, one section per story.

## Contents

- [Roles](#roles)
- [How a story is written](#how-a-story-is-written)
- [Status](#status)

## Roles

| № | Role          | Prefix | File                                   | Who                                                                         |
|---|---------------|--------|----------------------------------------|-----------------------------------------------------------------------------|
| 1 | project owner | OWN    | [project-owner.md](project-owner.md)   | Set up the group's Firebase project and holds it; outside the database      |
| 2 | administrator | ADM    | [administrator.md](administrator.md)   | A member listed in the group's `admins`; the founder is the first one       |
| 3 | member        | MEM    | [member.md](member.md)                 | A person on the group's roster, on a phone or on the page                   |
| 4 | device        | DEV    | [device.md](device.md)                 | A phone of a member, as the service on it acts without anybody at the screen |
| 5 | guest         | GST    | [guest.md](guest.md)                   | A person holding a link who is not on any roster yet                        |

The words are those of [GLOSSARY.md](../../GLOSSARY.md).

## How a story is written

```markdown
## ADM-03 Invite a person by their Google address

- Status: done
- ADR: 0015, 0025

As an administrator I make an invitation for one Google address, so that only
that person can join on the link and only once.

Acceptance:
- the link admits the named account and nobody else;
- ...
```

- The id is the role's prefix and a number. A number is never given to another
  story, also after the story is dropped.
- `ADR` names the records the story rests on, when there are any.
- `Tests` names the tests that hold the acceptance lines, once they are
  written.
- Acceptance lines are what a test, or somebody with a phone in hand, can
  check.

## Status

- `done` — both the phone and, where the story names it, the page do it now.
- `planned` — written down in [TODO.md](../../TODO.md) or decided, not built.
- `partly` — built in one client only, or with a part left for later; the
  story says which.
