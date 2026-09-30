# ADR-0002: Sign in with Google through the browser flow

- Status: Accepted
- Date: 2026-09-30

## Context

The application signed in through Credential Manager with a Google ID token
(`GetGoogleIdOption` with the project's web client id). That flow needs an
Android OAuth client for this package and signing key in the same Google Cloud
project. The pair of package and SHA-1 is unique across all projects, so only
one project can ever hold it: in any other group's project the sign-in fails
with `[28444] Developer console is not set up correctly`. With one build for
every group (ADR-0001) that rules the flow out.

Firebase Auth's own browser flow for the `google.com` provider
(`startActivityForSignInWithProvider`) needs only the SHA-1 registered in the
Firebase Android app and sign-in with Google enabled. It was tried on a phone
against a second project and signed in.

## Decision

The application signs in with `FirebaseAuth.startActivityForSignInWithProvider`
and an `OAuthProvider` for `google.com`. Credential Manager, the Google ID
library and the google-services Gradle plugin are removed.

## Consequences

- Sign-in works in any group's project without a per-project OAuth client for
  the application.
- The account is picked in a browser tab rather than in the system's account
  sheet; it takes a step more and depends on a browser being installed.
- The activity can be destroyed while the browser is open, so a pending
  sign-in result has to be picked up on return.
- The build no longer needs `google-services.json`.
