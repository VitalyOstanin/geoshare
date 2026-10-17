# Security policy

## Contents

- [Supported versions](#supported-versions)
- [Reporting a vulnerability](#reporting-a-vulnerability)
- [What to include](#what-to-include)
- [What not to send](#what-not-to-send)
- [What happens next](#what-happens-next)
- [What counts as a vulnerability](#what-counts-as-a-vulnerability)
- [What the setup site is trusted with](#what-the-setup-site-is-trusted-with)

## Supported versions

Fixes go into the latest release of the Android application and the current
page and rules in `master`. Earlier releases are not updated: the releases
after 0.1.1 offer the latest version themselves; on 0.1.1 and earlier install
the latest one by hand.

## Reporting a vulnerability

Do not open a public issue. A group's positions are where its members are
right now, and a description of a hole published before the fix is a ready
way to find them.

Report it privately through GitHub instead:
[Report a vulnerability](https://github.com/VitalyOstanin/geoshare/security/advisories/new).
Only the maintainer sees the report.

## What to include

1. The application version, or the commit of the page and the rules.
2. Steps to reproduce, on a project of your own.
3. What an attacker gets: whose positions, names or devices, and with what
   access to begin with.
4. A suggested fix, if you have one.

## What not to send

Real positions, real Google accounts, Firebase project identifiers and
configuration, invitation links and codes. Reproduce on a project you set up
for the purpose, with made-up members; the structure is enough.

## What happens next

A report is answered within a week. A confirmed vulnerability is fixed in a
separate release, and the description is published after it, as a GitHub
Security Advisory crediting the reporter if they agree.

## What counts as a vulnerability

For example: the Firestore rules letting someone read a position they were
not given, or keep reading after the grant has ended; joining a group without
a valid invitation, or on an invitation made for another account; a member
changing another member's devices, names or grants; the page or the
application running code or loading data from a link that is not the
group's own project; a position or a name leaving the device anywhere other
than the group's Firebase project.

The one exception to running code from elsewhere is the update: the
application asks GitHub for the latest release of this repository, downloads
its APK over https from GitHub's own hosts, holds it against the size and the
SHA-256 digest GitHub states for it, and only then hands it to the system's
installer, which refuses a package signed with a different key than the
installed one. Installing anything else, getting past that check, or
substituting the answer so that another package is installed is a
vulnerability.

Not a vulnerability: anything that needs the project owner's own Google
account or the Firebase console, since the owner holds the whole project by
design; and failures that only stop the application or the page from working
— open an ordinary issue for those.

## What the setup site is trusted with

The setup page sets up a new group's project with its project owner's own
token, and what it deploys there, the Firestore rules and the group's page,
it takes from the setup site as the site serves them at that moment
(ADR-0033). Nothing checks them against the repository on the way. Whoever
holds the Hosting project of the setup site therefore decides the rules and
the page of every group set up while they hold it; groups set up before are
not touched, and `tools/project.sh update` deploys from a working copy of the
repository, not from the site.

To check a group set up through the site, compare its rules with the
repository: `curl -s https://geoshare-setup.web.app/firestore.rules` against
`firebase/firestore.rules` at the release tag, as the release steps in
[docs/development.md](docs/development.md) do. The rules deployed to the
group's own project are the same file with the project owner's address
written in.
