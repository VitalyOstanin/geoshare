# Setting up a group's Firebase project

For a programmer or an AI agent running `tools/new-project.sh` on behalf of the
project owner — the person whose Google account will hold the project. Why a
script and not a page: [ADR-0003](ADR/0003-project-setup-by-script.md).

## Contents

- [What comes out](#what-comes-out)
- [Where to run it](#where-to-run-it)
- [Before the first run](#before-the-first-run)
- [Steps](#steps)
- [Reading the result with --json](#reading-the-result-with---json)
- [Signing key fingerprints](#signing-key-fingerprints)
- [Things not to do](#things-not-to-do)
- [When something fails](#when-something-fails)

## What comes out

A Firebase project on the free Spark plan with the Firestore database, the
access rules, the web page at `https://<site>.web.app/`, and the application's
settings at `https://<site>.web.app/geoshare.json`. The run ends with the
project owner's connection link, `https://<site>.web.app/join`, and a QR code
of it. The project owner opens it on their phone; the application takes the
project's settings from the site ([ADR-0001](ADR/0001-one-build-settings-at-runtime.md))
and signs in with Google. Members are invited later from the application.

## Where to run it

- A machine with this repository, `bash`, `node` with `npx`, `python3` and
  `curl`.
- Or Google Cloud Shell in the project owner's browser: it has all of these,
  and the project owner is already signed in to Google there.

## Before the first run

1. Sign the Firebase command line in as the project owner:
   `npx firebase-tools login`, or `npx firebase-tools login --no-localhost` in
   Cloud Shell or any shell without a browser of its own. This is the only
   sign-in the script needs; without it the script stops right away.
2. If the project owner's Google account has never used Firebase, they open
   <https://console.firebase.google.com/> once and accept the terms. The terms
   cannot be accepted from the command line.
3. Pick a project id: 6–30 characters, lowercase letters, digits and hyphens,
   unique across Google Cloud. A random suffix helps, e.g. `hiking-7a1c3f`. The
   page's address is taken from it.

## Steps

1. Create everything the script can create:

   ```sh
   tools/new-project.sh create <project-id> [--location <region>] [--name <display name>] [--json]
   ```

   Every step checks what is already there, so a run that stopped half way is
   started again with the same command.

2. The project owner does the two console steps from
   [console-steps.md](console-steps.md) ([Russian](console-steps.ru.md)). The
   script prints both links with the project filled in.

3. Check that the project is ready:

   ```sh
   tools/new-project.sh configs <project-id> [--json]
   ```

4. Hand the connection link and the QR code to the project owner.

## Reading the result with --json

With `--json` everything meant for a person goes to standard error, and
standard output gets one JSON object at the end:

```json
{
  "projectId": "hiking-7a1c3f",
  "site": "hiking-7a1c3f",
  "connectLink": "https://hiking-7a1c3f.web.app/join",
  "settingsUrl": "https://hiking-7a1c3f.web.app/geoshare.json",
  "manualSteps": [{"what": "...", "url": "..."}],
  "done": false
}
```

- `manualSteps` is what is left for the project owner in the console; show it
  to them as it is, links included.
- `done` becomes `true` only in the output of `configs`. The script cannot read
  back whether sign-in with Google was turned on: ask the project owner, or let
  the first sign-in on the phone tell.
- A non-zero exit code means the run stopped; the reason is the last lines of
  standard error.

## Signing key fingerprints

Sign-in with Google accepts only an application signed with a key whose
fingerprint the project knows. `create` takes the fingerprints from
`tools/signing.sh`, which reads them from the build container of this
repository. Where the application is not built on this machine, pass
`--sha-from <project-id>` to copy them from a project that already works with
the same build. Without either, the script warns and the fingerprints have to
be added in the console under Project settings, Your apps, Android.

## Things not to do

- Do not switch the project to the Blaze plan, and do not turn on Firebase AI
  Logic. The API keys of the project are public by design and are limited to
  Firebase services; AI Logic is one of them, so with it on anybody holding the
  key could spend the project's quota.
- Do not copy `build/new-project/<id>/firebase-config.js` over the working
  copy's own configuration unless this working copy is meant to talk to the
  new project from now on.

## When something fails

| №  | Message                                                     | What to do                                                                                        |
|----|-------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| 1  | `not logged in to the firebase command line`                | run the login from [Before the first run](#before-the-first-run)                                  |
| 2  | the project cannot be created, terms not accepted           | the project owner accepts the terms in the console, then the same command again                   |
| 3  | `waiting for firestore.googleapis.com` repeats, then stops  | the API takes a while to come on; run the same command again                                      |
| 4  | `no free name for the Hosting site`                         | pick another project id                                                                           |
| 5  | `no fingerprints in the signing report`                     | see [Signing key fingerprints](#signing-key-fingerprints)                                         |
| 6  | the page says `redirect_uri_mismatch` on sign-in            | step 2 of [console-steps.md](console-steps.md) was skipped                                        |
