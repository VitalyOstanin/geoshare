# The pages

Three pages, served from the group's own Firebase Hosting site:

- the map: the same positions the devices write, in a browser. Each member
  signs in with their own Google account and sees whom the others share with
  them (ADR-0008); while it is open, the page writes the marker that puts the
  devices into their fast collection mode;
- the join page (`join.html`): what an invitation link opens, which hands the
  invitation over to the application;
- the setup page (`setup.html`): walks somebody without an invitation through
  creating a Firebase project of their own, and links the privacy policy
  (`setup-privacy.html`) that goes with it.

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

Then serve it and open <http://127.0.0.1:5000/>. A file URL will not do: the
page is made of ES modules. The script serves it the way Hosting does, with the
rewrites and the headers in `firebase.json`, so what the Content-Security-Policy
refuses is refused here before a deploy rather than on the site after one;
`WEB_PORT` picks another port.

```sh
tools/serve-web.sh
```

Publishing goes through the Firebase command line, at the version the
repository pins in `tools/versions.env`:

```sh
firebase="firebase-tools@$(sed -n 's/^FIREBASE_TOOLS_VERSION=//p' tools/versions.env)"
npx --yes "$firebase" deploy --only hosting
```

## Tests

```sh
tools/web-test.sh
```

That runs `npm test` in `web/`: every `tests/*.test.mjs` under `node --test`,
four files at a time with 30 seconds to each test. They cover the page's own
logic, the parts that touch neither the document nor the map — the age and the
width of a position, the order, names and colours of the members, clusters,
following and selection, the group's roster and what a person may do in it,
searches, sirens and resets, the join link, the setup steps, the words of both
languages (each key in both, and the pages' dictionaries apart), the version
against the application's, and the story ids the tests cite against
`docs/stories/`. A few read the page itself rather than a module: the cache
headers in `firebase.json`, the contrast of the alarm colours in the
stylesheets, the vendored libraries. Several repeat the expectations of the
Android tests on purpose: a member has to be the same colour, a position the
same age and width, whichever client is looking at them, and the two sides
agreeing is what makes that true.

The same run with a coverage table at the end, which is what `npm run coverage`
does (`tools/coverage.sh` runs it together with the Android side):

```sh
tools/web-test.sh --coverage
```

It fails below 94 % of lines or of branches over `public/**`, vendored
libraries left out, and then runs `tests/coverage-reach.mjs`. Node leaves a
script no test loads out of its table instead of showing it at nought, so that
step checks the scripts against the report and against `tests/unreached.txt`,
the list of those that stay out because they draw on the map or the document,
or talk to Firebase: a new script in neither fails the run, and so does a listed
one the tests have started to load. `tests/stylesheets.mjs` is the helper that
reads the page's stylesheets as one text for the tests that look into them.

While editing, `tools/web-test.sh --watch` keeps the run going in the terminal
and reruns the tests as files are saved; it cannot be combined with
`--coverage`.

Nothing here talks to Firebase: the rules the clients run into are covered by
`tools/rules.sh` against the emulator instead.

## Layout

| #  | File                                                                                 | What it holds                                                                                                                   |
|----|--------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| 1  | `public/index.html`                                                                  | The map page's two screens: signing in, and the map                                                                             |
| 2  | `public/app.js`                                                                      | What happens when on the map page: sign-in, subscriptions                                                                       |
| 3  | `public/boot-sign-in.js`, `public/boot-theme.js`                                     | Read before the first paint: whether the browser was signed in, and the theme                                                   |
| 4  | `public/page/*.js`                                                                   | The parts of the map page: menus, panels, windows, strip, and the buttons for searches, sirens and resets                       |
| 5  | `public/lib/store.js`                                                                | Firebase: the account, the group chosen, the pause, the marker                                                                  |
| 6  | `public/lib/store-*.js`                                                              | Firebase by subject: positions, the group, searches and sirens                                                                  |
| 7  | `public/lib/position-data.js`, `group-data.js`                                       | What the documents of the positions and of the group hold, read for the page                                                    |
| 8  | `public/lib/refusals.js`, `link.js`                                                  | Subscriptions the rules refused, and whether the database is answering                                                          |
| 9  | `public/lib/map.js`                                                                  | MapLibre: the background, the markers and their bubbles                                                                         |
| 10 | `public/lib/map-*.js`                                                                | The card over the map, and following                                                                                            |
| 11 | `public/lib/following.js`, `selection.js`                                            | The rules for following a group and for whose card is open                                                                      |
| 12 | `public/lib/clusters.js`, `stacking.js`, `faces.js`                                  | Members in one place as one marker, which marker lies over which, the circle for a member                                       |
| 13 | `public/lib/age.js`, `width.js`, `distance.js`, `units.js`, `percent.js`             | How old, how wide and how far a position is, a charge written as a percentage                                                   |
| 14 | `public/lib/found.js`, `modes.js`, `mode-help.js`, `reason-help.js`, `help-table.js` | How a position was found and collected, said in words, and the help tables on them                                              |
| 15 | `public/lib/false-places.js`, `outside-the-box.js`                                   | Positions that lie where a false signal puts devices; a click outside a window                                                  |
| 16 | `public/lib/members.js`, `order.js`, `labels.js`, `group-name.js`                    | The order of the members and their colours, one order for ids on both clients, the names a member gives, what a group is called |
| 17 | `public/lib/membership.js`, `group-lines.js`, `card-actions.js`, `watchers.js`       | What a person may do in a group, the group seen whole, who is offered the buttons on a card, who is looking                     |
| 18 | `public/lib/claim-writer.js`                                                         | Writing and withdrawing the marker, one write to a refresh                                                                      |
| 19 | `public/lib/owner-rules.js`                                                          | Binding the first founding of a group to the project owner when the rules are deployed (ADR-0038)                               |
| 20 | `public/lib/searches.js`, `sirens.js`, `resets.js`, `phones.js`                      | Searches, sirens and resets lying in the database, and what a member's devices say of themselves                                |
| 21 | `public/lib/language.js`, `theme.js`, `kept.js`                                      | The language and the theme the viewer chose, and what the browser keeps for the next visit                                      |
| 22 | `public/lib/words.js`, `public/lib/words/*.js`                                       | The words the page speaks, English and Russian, and the join and setup pages' own dictionaries                                  |
| 23 | `public/join.html`                                                                   | The join page an invitation link opens                                                                                          |
| 24 | `public/join-app.js`                                                                 | Reading the invitation and handing it to the application                                                                        |
| 25 | `public/lib/join.js`, `join-words.js`                                                | What an invitation link carries; the join page's words                                                                          |
| 26 | `public/setup.html`                                                                  | The setup page for a group of one's own                                                                                         |
| 27 | `public/setup-app.js`                                                                | The setup page's steps                                                                                                          |
| 28 | `public/setup-privacy.html`                                                          | The privacy policy the setup page links to                                                                                      |
| 29 | `public/lib/setup-*.js`, `config-check.js`                                           | Creating the project and its Hosting site; refusing a half-written firebase-config.js                                           |
| 30 | `public/firebase-config.example.js`, `setup-config.example.json`                     | Templates; `firebase-config.js` itself is written by `tools/web-config.sh` and kept out of git                                  |
| 31 | `public/geoshare.json`, `manifest.webmanifest`, icons                                | The project file the application reads, the web app manifest, the favicons and touch icons                                      |
| 32 | `public/styles/*.css`                                                                | One file per part; the map takes what the panel leaves                                                                          |
| 33 | `public/lib/vendor/`                                                                 | Firebase, MapLibre GL JS, the QR encoder: one directory each                                                                    |
| 34 | `public/lib/qr-svg.js`                                                               | The QR code of a link, drawn as SVG                                                                                             |
| 35 | `public/lib/name-failure.js`                                                         | Why a name was not saved, told in words instead of the database's code                                                          |
| 35 | `public/lib/unexpected.js`                                                           | The banner every page shows for a failure nothing else caught                                                                   |
| 36 | `public/lib/fault.js`, `page-log.js`, `report.js`                                    | What such a failure said, with everything private taken out; the page's log; the problem report that opens GitHub's form        |
| 37 | `public/lib/version.js`, `text.js`                                                   | The version the page belongs to; small things done to text                                                                      |
| 38 | `tests/*.test.mjs`                                                                   | The tests above; `coverage-reach.mjs`, `stylesheets.mjs` and `unreached.txt` are their helpers                                  |
