# The Firebase side

Everything here is either a file the project deploys or a note about a setting
that only exists in the console. There is no server code: the phones and the map
page talk to Firestore directly, and the rules below are what stands between
them.

## Files

| File                    | What it is                                          |
|-------------------------|-----------------------------------------------------|
| `firestore.rules`       | Who may read and write what                          |
| `firestore.indexes.json`| Composite indexes; empty, every query is by document |
| `data-model.md`         | The documents and their fields                       |
| `tests/`                | The rules, checked against the emulator              |

`firebase.json` in the repository root points at the first two and configures
the emulator.

## Setting the project up

Steps 1 to 4 happen in the console and are done once.

1. Create a project. It is also a Google Cloud project, and the account that
   creates it owns both.
2. Create the Firestore database in production mode. **The location cannot be
   changed afterwards**, so pick the one closest to where the phones are;
   `europe-north1` (Finland) is the nearest to the city. One database
   per project is free.
3. Under Authentication, enable Google as a sign-in provider.
4. Enable Hosting. It hands out `<project-id>.web.app`, which is on the list of
   authorised sign-in domains from the start — a domain of our own would have to
   be added there by hand.
5. Register the Android application and put the `google-services.json` it hands
   out into `android/app/`. The file is deliberately not in the repository.
6. Once both phones have signed in, create `groups/family` with a `members`
   array holding their account identifiers. The rules read that array, so until
   it exists nobody can read anything.

## Deploying the rules

Either paste `firestore.rules` into the rules editor in the console, or use the
command line from a checkout:

```
firebase login
firebase deploy --only firestore:rules --project <project-id>
```

The project identifier is not committed: `.firebaserc` is ignored, so the
`--project` argument carries it.

## Checking the rules

```
tools/rules.sh
```

Starts the Firestore emulator and runs `tests/` against it. Nothing reaches the
real service: the run uses the project id `demo-geoshare`, and the `demo-`
prefix is what keeps both the emulator and the client library local. That also
means the check works before the console project exists.
