# ADR-0003: A group's project is set up by a script

- Status: Accepted; the project owner's path superseded by ADR-0033
- Date: 2026-09-30
- Note: `tools/new-project.sh` named below is now `tools/project.sh create`

## Context

Somebody has to create a group's Firebase project, register the apps, create
the database, deploy the rules and the page, and publish `geoshare.json`
(ADR-0001). Two ways were weighed:

- A setup page in the browser calling Google's APIs with the project owner's
  consent. The calls themselves work from a browser, but the Firebase terms
  can be accepted only in the console, and an OAuth app asking for
  `cloud-platform` stays unverified — capped at 100 users over its lifetime
  and behind a warning screen — until it passes Google's verification, which
  needs a domain of its own and possibly a paid security assessment.
- A script run from a terminal with the Firebase command line, whose own OAuth
  client carries none of those limits.

## Decision

`tools/new-project.sh` is the way a project is made. It is written for a
programmer or an AI agent to run, with Google Cloud Shell as a fallback for
somebody without a prepared machine. One step stays in the console: enabling
sign-in with Google, which is the only thing that makes the OAuth client.

The script ends with the project owner's connection link
(`https://<site>.web.app/join`) and its QR code. The project owner needs
no invitation code: the link carries the project, and the project owner
either creates their group or returns to it. Members get invitation links
from the application, since an invitation belongs to a group and the group
exists only after the project owner's first sign-in.

## Consequences

- No verification by Google and no user cap to manage.
- Setting up a group needs somebody comfortable with a terminal, or an agent
  acting for them; the console step needs a short written instruction.
- The script has to stay non-interactive and report its result in a form an
  agent can read.
- A setup page remains possible later, if a domain and verification become
  worth having.
