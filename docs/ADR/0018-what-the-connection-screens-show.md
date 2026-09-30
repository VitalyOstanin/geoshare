# ADR-0018: What the join page and the connection screen show

- Status: Accepted
- Date: 2026-09-30

## Context

A connection link opens `https://<site>.web.app/join` in the phone's browser
and then the application's confirmation screen (ADR-0001). The link may carry
an invitation (ADR-0015, ADR-0017). Before the person signs in, neither the
page nor the application can read the database, so they know only what the
link and `geoshare.json` on the site carry. Four details were open: where a
person without the application gets it, whether the project's owner is shown,
whether the invitation's expiry is shown, and how the invited address is shown.

## Decision

- "Download the application" on the page leads to the repository's GitHub
  releases page.
- Neither the page nor the confirmation screen names the project owner.
- Neither shows when the invitation expires.
- Both show the invited Google address shortened, as `ivan…@example.com`. Only
  this shortened form goes into the link; the full address is known to the
  rules alone.

## Consequences

- Moving to an app store later changes one link on the page.
- Nothing about the project owner is published on the site, since
  `geoshare.json` stays free of names.
- A person learns that an invitation has expired only when joining fails; the
  application then says so and asks for a new one.
- The shortened address tells a person with several Google accounts which one
  to sign in with, without handing the full address to whoever the link is
  forwarded to. Like the names from ADR-0017 it is a caption only: the rules
  compare the signed-in account's verified address with the full one.
