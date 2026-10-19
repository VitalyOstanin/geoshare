# ADR-0026: Several groups on one device

- Status: Accepted
- Date: 2026-10-03

## Contents

- [Context](#context)
- [Decision](#decision)
- [Consequences](#consequences)

## Context

A group is a Firebase project of its own (ADR-0005), and a device learned its
one project from a connection link (ADR-0001). One person is often in more
than one group at once — the household, and a hike for a week — with the same
Google account and the same device.

ADR-0001 kept the project in the default Firebase app, which cannot be made
again in a running process, so changing project meant restarting it, and
being in two groups at once was not possible at all. Firebase allows any
number of named apps in one process, each with its own Auth and Firestore,
and each project signs in on its own (ADR-0002): the same person has a
different account id in every group.

## Decision

- Every group the device is in is a connection: the project's settings, the
  group's name in it and a Firebase app of its own. The first project a device
  was ever connected to keeps the default app, where its stored sign-in lives;
  every other one gets an app named after its project. Joining one more group
  starts its app in the running process, without a restart.
- An app is not deleted when its group is left: the screens and the service
  let go of their subscriptions only afterwards, and joining the project again
  in the same run takes the same app back.
- The screens show one group at a time, chosen from a chip under the row of
  round buttons; the chip is drawn only while there are two groups or more.
  The connection screen no longer asks whether to switch project: a link to
  another project adds a group beside the others.
- The position is written into every group the device is signed in to, with
  each group's own list of who may see it. A siren, a search and a request to
  forget the place are heard from every group; a notification about one names
  the group while there is more than one. Each group's answered siren and
  carried-out request are kept apart.
- Leaving a group takes the member off its roster and deletes what they wrote
  there in one batch, signs out of that project and forgets the connection. A
  member whose going would leave nobody to run the group only takes it off the
  device. The rules let a member take themselves, and nobody else, off the
  roster.
- Forgetting a device and signing out happen in every group the device is
  signed in to.
- A device taken off a roster is told so instead of being shown the map, and
  is offered to remove the group or to join again by link; the connection is
  not removed on its own. The rules show the group document only to its
  members, so a refusal is read as having been taken off only after the device
  has seen its account on that roster before.

## Consequences

- Supersedes the consequence of ADR-0001 that changing project means
  restarting the process; its other decisions stand.
- Every place that reached the default Firestore or Auth goes through the
  connection it is about, so a group's data never leaks into another group's
  writes.
- A member signs in once per group, in the browser, as they did for the first.
- A process keeps the apps of the groups left in it until it ends.
- A device taken off a group while the application was closed names the group
  by its project until it reads the roster of a group it is still in; the
  last name read is not kept across runs.
