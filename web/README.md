# The map page

The same positions the phones write, in a browser. Signing in with the Google
account the family shares under is what makes the page a viewer: it subscribes
to the positions, and it writes the marker that puts the phones into their
fast collection mode while it is open.

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

Publishing goes through the Firebase command line:

```sh
npx firebase-tools deploy --only hosting
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

| File | What it holds |
|--------------------------|------------------------------------------------|
| `public/index.html`      | The two screens: signing in, and the map        |
| `public/app.js`          | What happens when: sign-in, subscription, panel |
| `public/lib/store.js`    | Firebase: the account, the positions, the marker |
| `public/lib/map.js`      | MapLibre: the background, the markers and their bubbles |
| `public/lib/age.js`      | How old a position is, and when it goes grey     |
| `public/lib/members.js`  | The order of the members and their colours       |
| `public/styles.css`      | One column: the map takes what the panel leaves  |
