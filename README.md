# geoshare

Private location sharing between a small group of Android phones, with a map that
also opens in a desktop browser.

No backend of our own: phones write one document each to Cloud Firestore and
subscribe to the documents of the other members. Freshness is demand driven — a
phone only samples location often while somebody is actually looking at the map.

## Status

Under construction. The application builds and the collection modes are
decided and tested; nothing sends a position yet.

## Layout

| Directory    | Contents                                                     |
|--------------|--------------------------------------------------------------|
| `android/`   | Kotlin and Compose application, Gradle build                 |
| `web/`       | Map page deployed to Firebase Hosting                        |
| `extension/` | Browser extension, built on top of the web map (later)       |
| `firebase/`  | Security rules, indexes, document model, rules tests          |
| `tools/`     | Build and test entry points                                  |

Directories appear as the corresponding part is implemented. Setting the
Firebase project up is described in [firebase/README.md](firebase/README.md).

## How it works

1. Each phone runs a foreground service holding an open Firestore subscription.
2. A viewer (phone or laptop) writes a short-lived marker to `watch` and refreshes
   it every minute while the map stays open.
3. The transmitter sees the marker over the already open connection and switches to
   high accuracy at a 15 second interval.
4. When the marker expires, the transmitter falls back to its standby mode.

This removes any need for Cloud Functions, which are not available on the free
Firebase plan.

## Battery

Standby uses balanced priority with batched updates and a geofence around the
current position, so no location is requested at all while the phone stays put.
High accuracy is reserved for the minutes somebody is watching.

## Privacy

The group is a fixed list of account identifiers. Security rules let a member read
everyone's position but write only their own, so positions cannot be forged.

## License

Private project, not for distribution.
