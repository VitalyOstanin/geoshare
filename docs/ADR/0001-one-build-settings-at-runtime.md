# ADR-0001: One build for every group, project settings read at runtime

- Status: Accepted; the process restart on a change of project superseded by ADR-0026
- Date: 2026-09-30
- Note: `tools/new-project.sh` named below is now `tools/project.sh create`

## Context

Every group that uses geoshare runs its own Firebase project: its positions
live in a database it owns, under rules it deployed, on a free plan whose
limits are its own. The build used to carry one project, compiled in from
`google-services.json`, so each group would need a build of its own — signed,
distributed and updated separately. That does not scale past the group the
repository started with, and a public build must not name anybody's project.

## Decision

One APK serves every group and carries no project at all.

- A group's site serves `geoshare.json` next to its page: `version`,
  `projectId`, `applicationId`, `apiKey`, `gcmSenderId`, `site`.
  `tools/new-project.sh` writes and deploys it.
- The phone keeps that document and starts Firebase with it
  (`FirebaseApp.initializeApp` with `FirebaseOptions`) before anything touches
  the database. The automatic start from `google-services.json`
  (`FirebaseInitProvider`) is removed.
- Without settings the application does not start its service and asks for a
  connection link.
- Phones already in use move over the same way — by opening the connection
  link — rather than through a one-off migration out of the old build.

None of the values in `geoshare.json` is a secret: Firebase API keys limited
to Firebase services are identifiers, and the data is guarded by the rules.

## Consequences

- A new group needs no build of its own; the same release goes to everybody.
- The connection link becomes the only way a phone learns its project, so it
  has to exist before the new build reaches any phone.
- Changing project means restarting the process, since the default Firebase
  app cannot be re-created in place.
- A group must not turn on services a public key would open to anybody, such
  as Firebase AI Logic, in its project.
- Watching somebody in another group's project needs a second, named Firebase
  app; the design leaves room for it but does not provide it.
