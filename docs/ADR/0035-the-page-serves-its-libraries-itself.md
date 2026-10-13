# ADR-0035: The page serves its libraries itself, one directory per version

- Status: Accepted
- Date: 2026-10-13

## Context

The page imported the Firebase SDK from `www.gstatic.com` by a versioned
address. A module import cannot carry a Subresource Integrity hash, so the
page ran whatever that server answered, with the signed-in member's token in
reach. MapLibre GL JS and the QR encoder were already copied into
`web/public/lib/vendor/`, but under names without a version, while the
hosting keeps `lib/vendor` in a browser's cache for a year as immutable: a new
MapLibre would have reached browsers a year after the page that needed it.
Nothing said when any of the three had a new version.

## Decision

- The page loads no script from another server. Firebase (app, auth,
  firestore), MapLibre GL JS and the QR encoder are served from
  `web/public/lib/vendor/<package>-<version>/`, each with its licence text.
- Their versions are pinned exactly in the `devDependencies` of
  `web/package.json`, with a `package-lock.json` beside it. Dependabot, which
  already watches `/web`, raises them.
- `tools/vendor-web.sh` installs the pinned packages with `npm ci`, which
  checks them against the lock, and copies the files the page uses into
  `lib/vendor`. The Firebase builds are the ones gstatic serves; the import of
  `firebase-app.js` by its gstatic address becomes a relative one.
- The page's tests fail while `lib/vendor` and the pins disagree, while any
  module is imported from another server, and while the rules tests use
  another Firebase than the page.
- `www.gstatic.com` leaves the page's `script-src`.

## Consequences

- What the page runs is what the repository holds, and a version reaches a
  browser on the first visit after the release that brings it.
- A Dependabot pull request for these packages is red until someone runs
  `tools/vendor-web.sh` on its branch and commits the result; that is the
  notice that a library changed.
- The repository carries about a megabyte of Firebase builds, replaced at
  every bump.
