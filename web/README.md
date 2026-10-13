# The pages

Three pages, served from the group's own Firebase Hosting site:

- the map: the same positions the devices write, in a browser. Each member
  signs in with their own Google account and sees whom the others share with
  them (ADR-0008); while it is open, the page writes the marker that puts the
  devices into their fast collection mode;
- the join page (`join.html`): what an invitation link opens, which hands the
  invitation over to the application;
- the setup page (`setup.html`): walks somebody without an invitation through
  creating a Firebase project of their own.

## Contents

- [Running it](#running-it)
- [Tests](#tests)
- [Layout](#layout)

## Running it

The page needs the Firebase configuration of the project, which is not in the
repository — it names the project. Write it once per working copy:

```sh
tools/web-config.sh
```

Then serve `web/public` over http and open it. A file URL will not do: the page
is made of ES modules.

```sh
python3 -m http.server --directory web/public 8477
```

Publishing goes through the Firebase command line, at the version the
repository pins in `tools/versions.env`:

```sh
npx "firebase-tools@$(sed -n 's/^FIREBASE_TOOLS_VERSION=//p' tools/versions.env)" deploy --only hosting
```

## Tests

```sh
tools/web-test.sh
```

They cover the page's own logic — the age of a position, the order of the
members and the colour each one is drawn in. The colour test repeats the
expectations of the Android test on purpose: a member has to be the same colour
whichever client is looking at them, and the two hashes agreeing is what makes
that true.

Nothing here talks to Firebase: the rules the clients run into are covered by
`tools/rules.sh` against the emulator instead.

## Layout

| #  | File                          | What it holds                                                   |
|----|-------------------------------|-----------------------------------------------------------------|
| 1  | `public/index.html`           | The map page's two screens: signing in, and the map             |
| 2  | `public/app.js`               | What happens when on the map page: sign-in, subscriptions       |
| 3  | `public/page/*.js`            | The parts of the map page: menus, panels, windows, strip        |
| 4  | `public/lib/store.js`         | Firebase: the account, the group chosen, the pause, the marker  |
| 5  | `public/lib/store-*.js`       | Firebase by subject: positions, the group, searches and sirens  |
| 6  | `public/lib/map.js`           | MapLibre: the background, the markers and their bubbles         |
| 7  | `public/lib/map-*.js`         | The card over the map, and following                            |
| 8  | `public/lib/age.js`           | How old a position is, and when it goes grey                    |
| 9  | `public/lib/members.js`       | The order of the members and their colours                      |
| 10 | `public/join.html`            | The join page an invitation link opens                          |
| 11 | `public/join-app.js`          | Reading the invitation and handing it to the application        |
| 12 | `public/setup.html`           | The setup page for a group of one's own                         |
| 13 | `public/setup-app.js`         | The setup page's steps                                          |
| 14 | `public/setup-privacy.html`   | The privacy policy the setup page links to                      |
| 15 | `public/lib/setup-*.js`       | Creating the project and its Hosting site                       |
| 16 | `public/styles/*.css`         | One file per part; the map takes what the panel leaves          |
| 17 | `public/lib/vendor/`          | Firebase, MapLibre GL JS, the QR encoder: one directory each    |
| 18 | `public/lib/unexpected.js`    | The banner every page shows for a failure nothing else caught   |
| 19 | `public/lib/fault.js`         | What such a failure said, with everything private taken out     |
