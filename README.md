# geoshare

English · [Русский](README.ru.md)

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/images/hero-dark.webp">
  <img alt="One group on two Android devices and in a browser" src="docs/images/hero-light.webp">
</picture>

Location sharing within a group, such as a family, on Android devices and in
a browser. Members sign in with their ordinary Google accounts, and everything
runs on the free tier of Firebase.

Every member's device reports where it is; everybody the member shares with
sees it on a map — in the Android application or on a web page — with how old
the position is, how wide the answer was and how much charge the device has
left. There is no server of its own to run or pay for: the devices and the page
talk to Cloud Firestore directly, and the security rules are the whole of the
backend. Each group's data stays in a Firebase project of its own.

## Contents

- [What it does](#what-it-does)
- [Getting started](#getting-started)
- [Privacy](#privacy)
- [Free tier limits](#free-tier-limits)
- [False positions](#false-positions)
- [Development](#development)
- [License](#license)

## What it does

- **A map of the group.** One pin per device with the member's face and name;
  members standing close together share one pin until the map is zoomed in.
  A strip under the map lists everybody, and a card opens on a tap with the
  age of the position, when the device was last in touch, its charge, the
  collection mode and the distance from the viewer.
- **Following.** The map can stay on one member, on everybody at once, or
  stay where it is put; a menu over the map picks the mode and says how to
  pick the member to follow.
- **Sharing per person, with an end.** A member chooses whom they share with
  and until when; a permission taken back stops at once, not when the device
  next writes.
- **Joining by a link.** An administrator makes an invitation for one Google
  address and sends the connection link, or shows it as a QR code to scan.
  The link points the device at the group's project and lets that one account
  in, once, within a day.
- **Running the group.** An administrator hands out invitations, removes
  members, makes other administrators and sees whether each device is in
  touch — without seeing whom anybody shares with or where they are.
- **Searching for a member.** When somebody cannot be reached, an
  administrator opens their place to the whole group for six hours, a day or
  three days. Everybody's map rings them in red; their device says so in a
  notification that ends the search with one tap.
- **A siren on a lost device.** A member can make their own device sound, and
  an administrator any device of the group: 22 seconds at full volume on the
  alarm stream, through "Do Not Disturb".
- **Several groups on one device.** A device can be in more than one group,
  each in its own Firebase project; the map shows one group at a time and the
  position goes to every group with that group's own list of who may see it.
- **Names of one's own.** Each member can call the others' devices and people
  whatever they like, for themselves alone.
- **Both themes and two languages.** Light, dark or the system's, in English
  or Russian, on the device and on the page alike.

## Getting started

### Setting up a group

The person setting the group up — the project owner — needs a Google account
with 2-Step Verification turned on (the Firebase console asks for it from
18 October 2026) and a browser, no terminal:

1. Open the setup page, [geoshare-setup.web.app](https://geoshare-setup.web.app/).
   Its first step sends you to the [Firebase console](https://console.firebase.google.com/)
   to create a project and says what to pick there: the free Spark plan is
   enough, and Google Analytics can be turned off.
2. Back on the setup page, sign in and pick the project. The page makes the
   database, the rules, the Android and web apps and the group's own site, and
   walks through the two steps only the console can do
   ([docs/console-steps.md](docs/console-steps.md)).
3. The page ends with the connection link and its QR code.

The project owner sees every member's places and history from the Firebase
console, past the rules; the application says so to everybody before they
join.

[tools/project.sh](tools/project.sh) does the same from a terminal, for a
developer or for Google Cloud Shell; [docs/new-project.md](docs/new-project.md)
describes it.

### Joining

1. Download the application from the
   [releases page](https://github.com/VitalyOstanin/geoshare/releases) and
   install it. It needs Android 8 or later; Google Play services are not
   required.
2. Open the connection link from the invitation, or scan its QR code from the
   application's connection screen.
3. Sign in with Google, with the address the invitation was made for. The
   first time, Google asks whether the group's address
   (`<project>.firebaseapp.com`) may see your email address; it is the
   group's own, so allow it.

## Privacy

- The group's data lives in the project owner's Firebase project and nowhere
  else; geoshare has no server and no analytics.
- Being in the group tells a member who else is in it, not where they are:
  each member names the people who may see them, one by one, each until a
  moment of their choosing.
- The rules read that permission from where its owner writes it, so taking
  it back closes the position at once — even for a device that is switched
  off.
- A member writes only their own documents; nobody can forge another
  member's position.
- An invitation is unreadable to everybody, the administrator who made it
  included; it lets in only the Google account it was made for, once, and
  runs out within a day.
- The project owner reads everything from the Firebase console, past the
  rules: whoever sets the project up should be somebody the group trusts
  with that.
- The document model and the reasons behind every rule are in
  [firebase/data-model.md](firebase/data-model.md), and the rules are held to
  them by tests run against the Firestore emulator.

## Free tier limits

The free Spark plan has daily limits: 50,000 document reads, 20,000 writes
and 20,000 deletes, and 1 GiB of data
([Firestore quotas](https://firebase.google.com/docs/firestore/quotas)). A
family with two devices and the page open now and then uses about 5% of the
reads and writes a day.
Past a limit the database stops answering until the quota resets, around
midnight Pacific time, so the project owner should keep an eye on usage: in
the Firebase console, or with `tools/project.sh show <project-id> --usage`.

## False positions

A satellite receiver can be fed a false signal, and a device under one reports
a place it is not at with full confidence. The application weighs every answer
before writing it: jumps no traveller could make, answers that trace a circle
round a transmitter, satellites whose time does not run along with the
device's, a satellite fix far from what the cell towers and Wi-Fi say, and
places the group has marked as known to be false. A position it does not
trust is held back, and the card says why; a device under a transmitter still
reports that it is in touch and how charged it is.

The project owner records the group's false places in the Firebase console,
as [docs/false-places.md](docs/false-places.md) describes; no screen does it
yet.

## Development

How it works inside, the collection modes, building, testing and the layout of
the repository are in [docs/development.md](docs/development.md).

## License

MIT, see [LICENSE](LICENSE). That is the licence of this repository's own
code; the parts it carries keep their own:

- the application's libraries are listed, each with its licence, on its
  Open source licences screen in the settings;
- the page carries copies of Firebase (Apache-2.0), MapLibre GL JS
  (BSD-3-Clause) and qr (MIT or Apache-2.0) in `web/public/lib/vendor/`, each with its
  licence beside it;
- the map is drawn from OpenStreetMap data, © OpenStreetMap contributors,
  under the [ODbL](https://www.openstreetmap.org/copyright), credited on the
  map itself.
