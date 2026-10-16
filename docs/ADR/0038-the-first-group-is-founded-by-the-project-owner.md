# ADR-0038: The first group of a project is founded by its owner

- Status: Accepted
- Date: 2026-10-17

## Context

ADR-0036 limits a project to one group, so that a stranger who reaches the
site cannot found another and spend the project's quotas. It leaves the first
founding open: while `project/group` does not exist, `founding()` lets any
signed-in account create the group, because the rules are one file for every
project and name nobody.

The setup page (ADR-0033) releases the site before the project owner opens it
and founds the group. In that time anybody who knows the address
`<project>.web.app` can sign in with Google and found the group first. The
project owner then reads that the project already has a group, and a group
cannot be replaced from a client. ADR-0036 says a stranger "can sign in but
write nothing"; that holds only after the founding.

## Decision

- `firebase/firestore.rules` gets a function `foundingAllowed()`, which
  `founding()` requires. In the repository it is `return true;` followed by
  the marker `// @project-owner`, so the file is valid for any project and
  for the emulator tests.
- Whatever deploys the rules to a project replaces the marker line with a
  check on the account that does the deploying:
  `request.auth.token.email_verified == true` and
  `request.auth.token.email.lower() == '<owner address, lower-cased>'`. The
  setup page does it with the address of the Google account that signed in
  (`rulesForOwner` in `web/public/lib/owner-rules.js`); `tools/project.sh`
  does it for the copy of the rules it deploys, with `--account` or else the
  Firebase command line's default account, through the same module.
- The substitution refuses an address that is not a plain `name@host`, and
  rules without the marker; it never deploys open rules by failing silently.
- Only the first founding is bound. Later writes depend on the roster as
  before, and `project/group` is still written once, in the batch of
  `founding()` (ADR-0036).
- A project deployed before this decision keeps open rules until the project
  owner runs `tools/project.sh update` or the setup page again.

## Consequences

- The group of a project can only be founded by the account that owns it,
  from the moment its rules are deployed; a stranger who finds the site first
  is refused.
- The project owner must found the group with the Google account that
  deployed the rules. An owner who deployed with one account and wants to
  found with another redeploys the rules with that one.
- Rules in the repository, deployed by hand with `firebase deploy`, stay open
  to every account until bound; the two supported ways of deploying bind
  them.
- A rule that names an e-mail address is only as good as the verified claim
  in the token, which Google sign-in provides.
- ADR-0036 is supplemented, not replaced: its decisions stand.
