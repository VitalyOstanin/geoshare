# ADR-0033: A setup page finishes the group's project

- Status: Accepted
- Date: 2026-10-05

## Context

ADR-0003 made a script the way a group's project is set up, and turned down
a setup page in the browser because it would ask for `cloud-platform`: a
scope that keeps the OAuth app unverified, capped and behind a warning until
Google verifies it. The script asks for a terminal, which a person founding a
family group usually has not got, and Google Cloud Shell is no easier for
them.

Only one call of the setup needs `cloud-platform`: creating the project. The
Firebase console's own wizard creates it, and it is also the one place where
a new account accepts the Firebase terms. Everything after it was tried from
a page on 2026-10-05 with the scopes `firebase`, `datastore` and
`service.management`: enabling the Firestore API, the database, the web and
Android applications and their fingerprints, the rules, the hosting site,
deploying the page and `geoshare.json`, and checking that sign-in with Google
is on. Writing the authorized domains of sign-in is refused with these
scopes; the console adds `<id>.web.app` and `<id>.firebaseapp.com` by itself,
which is enough while the site is named after the project.

## Decision

A founder creates the project with the Firebase console's wizard, then a
setup page finishes it:

- the page lives on a Firebase Hosting site of the developer's own project,
  beside the web OAuth client it signs in with;
- it asks for `firebase`, `datastore` and `service.management` through
  Google Identity Services' token model, lists the founder's projects and
  sets up the chosen one; every step finds what exists before making it, so
  running it again is harmless;
- the group's site is the one named after the project, and nothing else: if
  that name is taken, the page stops and says so instead of choosing another
  name;
- the page deploys to the group's site the same files it is served from;
- the two console steps (`docs/console-steps.md`) stay with the founder; the
  page checks the first and asks the founder to confirm the second;
- it ends with the founder's connection link and its QR code; the
  application's connection screen leads to the page.

`tools/project.sh` stays for the developer and for an agent; ADR-0003 is
superseded for the founder's path only.

## Consequences

- No terminal for the founder; a browser on a computer or on the phone is
  enough.
- The OAuth client stays unverified until Google verifies it: the founder
  passes a warning screen, and at most 100 founders over the client's
  lifetime can use it. Members never sign in to it.
- The setup is written twice, in the script and in the page, and the two have
  to follow API changes together.
- A project whose site is not named after it cannot be finished by the page.
