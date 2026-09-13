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
2. A viewer (phone or laptop) writes a short-lived claim to `watch` and refreshes
   it every minute while the map is in front of somebody.
3. The transmitter sees the claim over the already open connection and switches to
   high accuracy at a 15 second interval, writing what it finds on the same terms.
4. When the claim expires, the transmitter falls back to its standby mode.

This removes any need for Cloud Functions, which are not available on the free
Firebase plan.

## Battery

Standby uses balanced priority with batched updates: a position every fifteen
minutes, with up to ten more before the batch is handed over. High accuracy is
reserved for the minutes somebody is watching, and a claim to be watching is
only made by a screen that is in front of somebody — a tab left open on a desk
stops counting after a quarter of an hour.

Between 23:00 and 07:00, and whenever the battery drops below 15 percent, the
interval stretches to twenty minutes at low power. No mode goes longer than the
half hour after which a position is drawn as old, so a working phone never looks
like a stopped one.

A geofence around the current position — which would let a phone that has not
moved ask for nothing at all — is designed for and not implemented; the modes it
would drive are in the code and unreachable until it is.

## Privacy

The group is a fixed list of account identifiers. Security rules let a member
write only their own position, so positions cannot be forged, and let them read
another member's only while that member's profile says they may: the permission
is read from where its owner writes it, so withdrawing it takes effect at once,
whatever the phone it describes is doing.

## License

Private project, not for distribution.
