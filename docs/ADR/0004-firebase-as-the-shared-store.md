# ADR-0004: Firestore as the shared store, with applications of our own

- Status: Accepted
- Date: 2026-09-11 (recorded 2026-09-30)

## Context

Two or more phones have to see each other's positions, and a desktop has to
show them too. The whole thing has to cost nothing and keep the battery close
to the minimum. At the time of the decision, hosts with free compute for new
accounts were gone or wanted a payment card, and none of them delivered
changes to subscribers without a server of our own.

Ready-made senders such as OwnTracks and Traccar Client were considered.
Neither writes to Firestore, and neither can listen for "somebody is watching
me now", which is what lets a phone stay slow until it is looked at.

## Decision

- The shared store is Cloud Firestore on the free Spark plan: one document per
  device position, changes pushed to subscribers.
- The phone runs an application of our own: a foreground service with a
  notification that writes positions and holds a subscription to the
  database.
- The desktop view is a web page on the same database, served by Firebase
  Hosting; a browser extension may come later on top of the same map code.
- One repository holds the Android application, the page and the Firebase
  rules.

## Consequences

- No server to run or pay for; the free plan's daily quotas bound how many
  phones and how often they write, and every design choice is weighed against
  them.
- The database's security is entirely in the Firestore rules, which are
  tested against the emulator.
- The phones depend on Google Play services; phones without them are out of
  scope.
- Waking a phone by push (Cloud Messaging) is not in use.
