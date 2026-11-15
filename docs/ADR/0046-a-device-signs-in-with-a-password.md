# ADR-0046: A device without a Google account signs in with a password

- Status: Accepted
- Date: 2026-11-15

## Context

The application signs in with Google, through the browser (ADR-0002). Some
devices must not carry a Google account at all, a child's device set up by a
device policy controller (a device owner or MDM app) among them, and a
browser sign-in there leaves a Google session behind or cannot be finished.
Such a device still needs an account in the group's project, with an address
an administrator can make an invitation out to (ADR-0015).

Firebase Authentication signs in with an address and a password as well. The
rules judge every access by `request.auth.uid` and look at the address only
in `invitedAccount()` and in the founding bound to the project owner
(ADR-0038), both of which require `email_verified`; nothing in them asks how
the account signed in.

## Decision

- The sign-in screen offers "Sign in with email" beside the Google button in
  every build: an address, a password and nothing else. There is no
  registration, no confirmation mail and no password reset in the
  application.
- The project owner makes such an account from their own computer with
  `tools/project.sh add-account`, and changes its password with
  `tools/project.sh set-password`. The address can be any the owner reads,
  `name+device@gmail.com` included: to Firebase that is an address of its own.
- The account is made already verified (`emailVerified: true`), so that an
  invitation made out to its address lets it join; nobody would read a
  confirmation mail.
- The script calls the Identity Toolkit API, the same calls the Admin SDK
  makes for `createUser` and `updateUser`, with the token of the firebase
  command line the owner is already signed in to. No service account key is
  made or kept on the owner's computer.
- The password comes from a file only its owner can read (mode 600 or 400);
  the script refuses any other, and never prints the password or puts it on
  a command line.
- The Email/Password provider is off in a new project. It is turned on in the
  console or with `tools/project.sh enable-email-sign-in`, only in a project
  that needs it; `add-account` and `set-password` stop and say so while it is
  off.
- The group's page keeps sign-in with Google only. It is opened in a member's
  own browser, where Google works; the password accounts are for devices
  that cannot have one. It can be added later without changing this record.
- The rules do not change. Their tests show that a verified password account
  joins on an invitation to its address and that an unverified one does not.

## Consequences

- A device joins and shares with no Google account on it; the account is the
  owner's to make and to change, and a changed password asks that device to
  sign in again.
- With the provider on, anybody holding the project's public API key can
  register an address and a password through the API, as anybody can sign in
  with a new Google account. Such an account is unverified: it can join on no
  invitation and found no group bound to the owner, and it reaches nothing
  that is not open to every signed-in account already.
- The debug build's stand-in sign-in (ADR-0012, ADR-0044) is unchanged; it
  signs in the same way, to the emulators.
- The sign-in screen has a second way in, with its own failures to say:
  a wrong address or password, and too many attempts. A wrong credential is
  said only for this sign-in: Google's browser flow throws the same
  exceptions for a credential of its own that is malformed or expired, and
  for an account an administrator disabled, and nothing was typed there.
