# ADR-0037: Google Cloud Shell is not a supported place to set up a project

- Status: Accepted
- Date: 2026-10-16

## Context

ADR-0003 named Google Cloud Shell as the fallback for a project owner without
a prepared machine: it has `bash`, Node.js and `curl`, and the project owner
is already signed in to Google there. Since then the setup page (ADR-0033)
has become the path for a project owner who is not a programmer, and
`tools/project.sh` is run by programmers, who install Node.js in minutes.

Keeping Cloud Shell working means checking every change of the script there:
which Node.js it carries, how the Firebase command line signs in without a
local browser, that no container is at hand to read the debug key. Nobody
asks for it, and it has never been checked end to end.

## Decision

- Google Cloud Shell is no longer named or supported. `tools/project.sh` runs
  on a machine with Node.js 20 or later.
- The script no longer reads `CLOUD_SHELL`. A shell without a browser of its
  own still signs in with `firebase login --no-localhost`, as
  `docs/new-project.md` says.
- This replaces the Cloud Shell fallback of ADR-0003; the rest of ADR-0003
  stands as ADR-0033 left it.

## Consequences

- One place fewer to check after a change of `tools/project.sh`.
- Somebody without Node.js installs it or uses the setup page.
- The story of setting up a project without a prepared machine is withdrawn:
  the setup page (OWN-09) is that path now.
