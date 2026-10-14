# Recording a false place

A false place is somewhere a false satellite signal is known to put the
group's devices. Once it is recorded, every device of the group leaves out
answers that land there, and the card says why. No screen writes false places
yet, so the project owner records them in the Firebase console, signed in with
the Google account that holds the project. [Russian version](false-places.ru.md).

## Contents

- [Where the place comes from](#where-the-place-comes-from)
- [Adding a place](#adding-a-place)
- [Changing or removing a place](#changing-or-removing-a-place)
- [Checking it arrived](#checking-it-arrived)

## Where the place comes from

A device under a false signal shows it on its card: the position it was given
jumps far from where it was, often to the same spot every time. That spot is
the centre. The radius is not the spread of the false answers, which sit within
metres of each other, but how far the receiver wanders away from the spot
while it is being fed; a few kilometres is usual, and fifty is the most a
device accepts.

## Adding a place

1. Open `https://console.firebase.google.com/project/<project-id>/firestore/data`.
2. Open the `groups` collection and the one document in it: that is the group.
3. In the group, open the `falsePlaces` collection. The first time it is not
   there: press Start collection and type `falsePlaces` as its id.
4. Press Add document, leave Document ID on Auto-ID, and add the fields:

   | Field          | Type   | Value                                                   |
   |----------------|--------|---------------------------------------------------------|
   | `latitude`     | number | degrees, for example `59.4370`                          |
   | `longitude`    | number | degrees, for example `24.7536`                          |
   | `radiusMetres` | number | metres, more than 0 and at most `50000`                 |
   | `named`        | string | optional; what the place is, up to 100 characters       |

5. Press Save.

The type matters: the console offers string first, and a latitude typed as a
string is not a number to the devices. Add no other fields. The console writes
past the database rules, so nothing there stops a wrong document; the devices
check the numbers again and leave out a place that is out of range.

## Changing or removing a place

Open the document under `falsePlaces` and edit a field, or delete the document
from its menu. The devices follow the change within seconds while they are
online, and on their next connection otherwise.

## Checking it arrived

Each device keeps the places in a file of its own, so it knows them before it
reaches the network after a restart. Its log says
`the group names <n> false places` whenever the list changes, and a left-out
answer reads `a jump by landing on <named>, left out`.
