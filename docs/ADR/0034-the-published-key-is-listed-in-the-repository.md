# ADR-0034: The published key's fingerprints are listed in the repository

- Status: Accepted
- Date: 2026-10-06

## Context

Google sign-in checks the calling application against the fingerprints
registered for the group's Android app. Until now `tools/project.sh` took
them from `gradle signingReport`, which reads the debug key in the build
container, and the setup page (ADR-0033) took them from `--sha` when its
site was built. Neither knew the key that signs the application on the
releases page (ADR-0018), and Google Cloud Shell has no container to read
any key from.

## Decision

- The application on the releases page is signed with one release key. The
  key and its password stay off the repository and off any build machine;
  the build produces an unsigned package and it is signed where the key is.
- The SHA-1 and SHA-256 of that key are listed in
  `android/release-fingerprints.txt`. They are not a secret: anyone holding
  the application reads them off its certificate.
- `tools/project.sh` registers them for every project it sets up, and adds
  the debug key's when `gradle signingReport` can be read. `--sha` and
  `--sha-from` still replace both.
- `tools/setup-site.sh` takes them when no `--sha` is given, so the projects
  the setup page finishes accept the published application.

## Consequences

- A project set up from Cloud Shell or from the setup page signs in with the
  application from the releases page without anyone typing a fingerprint.
- Projects set up before this list only the debug key; they need the
  release fingerprints added once, by `tools/project.sh` or in the console.
- Losing the release key means a new key, a new list and projects that
  refuse the new builds until it is added; installed applications cannot be
  updated in place.
