# Project owner

The person who set up the group's Firebase project and holds it. The role is
outside the database: the project owner is usually also the group's founder,
but the stories here are about the project, not about the group.

## Contents

- [OWN-01 Make the group's project from one script](#own-01-make-the-groups-project-from-one-script)
- [OWN-02 Turn on sign-in with Google in the console](#own-02-turn-on-sign-in-with-google-in-the-console)
- [OWN-03 Hand the project to the phones as a link](#own-03-hand-the-project-to-the-phones-as-a-link)
- [OWN-04 Keep the rules and the page up to date](#own-04-keep-the-rules-and-the-page-up-to-date)
- [OWN-05 See what the project spends of the free tier](#own-05-see-what-the-project-spends-of-the-free-tier)
- [OWN-06 Look after several projects from one machine](#own-06-look-after-several-projects-from-one-machine)
- [OWN-07 Remove the project](#own-07-remove-the-project)
- [OWN-08 Set up the project without a prepared machine](#own-08-set-up-the-project-without-a-prepared-machine)
- [OWN-09 Set up the project from the phone](#own-09-set-up-the-project-from-the-phone)
- [OWN-10 Use the published build without building one](#own-10-use-the-published-build-without-building-one)

## OWN-01 Make the group's project from one script

- Status: done
- ADR: 0003, 0005
- Tests: —
- Not covered: the script is run by hand against a project; no automatic test

As a project owner I run one script that makes the group's Firebase project,
so that the group's data lives in a project of its own on the free plan and no
build of the application has to know about it.

Acceptance:
- `tools/project.sh create` makes the project, registers the Android
  application and the page, adds the fingerprints of the signing keys, creates
  the database and deploys the rules and the page;
- the page serves `geoshare.json` with the project's settings beside it;
- a second run after the console step finishes what the first could not.

## OWN-02 Turn on sign-in with Google in the console

- Status: done
- ADR: 0003
- Tests: —
- Not covered: checked by hand with `show --wait`

As a project owner I do the one step the script cannot, by a page of
instructions, so that the members can sign in with their Google accounts.

Acceptance:
- `docs/console-steps.md` (and its Russian copy) names every click;
- `tools/project.sh show --wait` waits until the provider is on and says so.

## OWN-03 Hand the project to the phones as a link

- Status: done
- ADR: 0001, 0003
- Tests: Android `ConnectLinkTest` (the owner's link), `ConnectFlowTest`

As a project owner I get my own connection link and its QR code at the end of
the setup, so that my phone connects to the project without an invitation and
I can found the group.

Acceptance:
- the script ends with `https://<site>.web.app/join` and a QR code of it;
- the phone that opens the link fetches the settings and asks to sign in;
- the first signed-in person founds the group (ADM-01).

## OWN-04 Keep the rules and the page up to date

- Status: done
- ADR: 0003
- Tests: —
- Not covered: checked by hand on the projects in use

As a project owner I bring a project already in use up to the current rules
and page with one command, so that the group gets fixes without being set up
again.

Acceptance:
- `tools/project.sh update <project>` deploys the rules, the page and
  `geoshare.json`;
- the data already in the database stays.

## OWN-05 See what the project spends of the free tier

- Status: done
- ADR: 0027
- Tests: —
- Not covered: checked by hand

As a project owner I see the reads, writes and storage of the last days
against the free tier, so that I learn in time whether the group outgrows it.

Acceptance:
- `tools/project.sh show --usage` prints how much of the free plan the
  project has used today, `--days N` the N quota days before it;
- administrators who do not own the project do not see them.

## OWN-06 Look after several projects from one machine

- Status: done
- ADR: 0005
- Tests: —
- Not covered: checked by hand with two accounts

As a project owner of several groups (a family, a trip) I name the project on
every run and pick the Google account that holds it, so that one machine
looks after all of them without a working copy each.

Acceptance:
- every command takes the project id;
- `--account <address>` picks one of the accounts the command line is
  signed in to.

## OWN-07 Remove the project

- Status: done
- ADR: —
- Tests: —
- Not covered: not run against a real project

As a project owner I remove a project the group no longer uses, so that
nothing is left running in my account.

Acceptance:
- `tools/project.sh delete <project> --yes` asks Google to shut the project
  down, and does nothing without `--yes`;
- the project can be restored for 30 days.

## OWN-08 Set up the project without a prepared machine

- Status: partly — the script runs in Cloud Shell, but there it cannot read
  the fingerprint of the published build and only warns
- ADR: 0003
- Tests: —
- Not covered: not covered

As a project owner who is not a programmer I set up the project from Google
Cloud Shell in a browser, following one page, so that I need no computer of
my own prepared for it.

Acceptance:
- `docs/new-project.md` leads from an empty account to the connection link;
- the fingerprint of the key the published application is signed with is
  known to the script without a build container.

## OWN-09 Set up the project from the phone

- Status: planned
- ADR: —
- Tests: —
- Not covered: planned

As a project owner without a computer I make the group's project from the
application itself, so that the whole setup happens on one phone.

Acceptance:
- to be agreed.

## OWN-10 Use the published build without building one

- Status: planned
- ADR: —
- Tests: —
- Not covered: planned

As a project owner I give the members the application from the repository's
releases page, signed with one key, so that nobody has to build it.

Acceptance:
- a signed release APK is published on GitHub Releases;
- "Download the application" on the page leads there (ADR-0018).
