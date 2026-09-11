# Ideas not scheduled yet

## Home screen shortcuts to one member

A shortcut on the phone's home screen that opens the map straight on one
member's position, one shortcut per member, so that answering "where is X"
takes a single tap instead of opening the application and finding them on a
map of everyone.

Worth checking when the map screen exists: the shortcut carries the member id,
and the map opens with that member in the middle. Dynamic shortcuts fit the
list of members better than static ones, since the members are known only
after a sign-in.

## A choice of map background

The map draws one background and that is all there is. Where more than one is
available for free, the member should be able to pick: OpenStreetMap tiles as
they are now, and Yandex Maps through the free Lite tier, which is the better
one over Russia.

Worth doing once a second background is confirmed to be usable without a
payment method — the choice itself is a setting and a tile source behind it,
but every candidate carries its own terms, key and attribution.

## A way to exchange positions other than Firebase

Everything rests on one provider: the sign-in, the database, the hosting. The
phones are Huawei, and their own mobile services offer the same three pieces,
so a second backing worth trying is that one; a small self-hosted endpoint is
the other candidate.

Worth doing once the Firebase path is settled, and worth designing towards
before then: the parts that talk to the database are already behind their own
sources and writers, and keeping them that way is what makes a second backing
an addition rather than a rewrite.

## Which members the panel keeps in reach

The panel shows the first two members by id and hides the rest behind "more".
That is an arbitrary choice dressed up as an order. The member should decide
who stays in reach: pinning someone out of the full list onto the panel is the
straightforward form of it, and the panel would then be pinned members first,
everyone else behind the button.

Other rules worth weighing against pinning: most recently looked at, closest
first, or whoever is moving. The pinned set is per phone, so it is a local
setting rather than something the group shares.

## Russian, alongside English

Every string on screen is English. Both phones are used in Russian, so the
interface should be too: a Russian resource file beside the default one, with
the plural forms Russian needs — the age of a position is counted in minutes,
hours and days, and Russian distinguishes more cases than one and many.

## Light and dark, and a say in it

The theme follows whatever the material defaults give. It should follow the
phone by default and be overridable in the settings: light, dark, or as the
phone says. The map background matters here too — a bright map at night is
worse than no map, and osmdroid can be asked to draw its tiles inverted.

## Zooming with one thumb

Pinch zoom is all there is, and it takes two hands when the other one is
holding something. Google Maps zooms on a double tap, and on a double tap held
and dragged: up for closer, down for further. The library zooms on a double tap
already; the held-and-dragged part is a gesture of our own over the map view,
and it is the half that matters, since it is the one that works one-handed.

## The battery level on the member buttons

The full list shows how much battery a member has left; the two buttons in the
panel do not, and they are what is on screen most of the time. A phone about to
go quiet is worth seeing there — the position is going to stop being fresh, and
knowing why beats wondering. The button is small, so this is a matter of what
it can carry alongside the name and the age.

## Keeping the tiles

The library caches tiles in the application's own directory, with whatever size
and expiry it defaults to. Neither has been looked at. Worth setting
deliberately: how much space the cache may take, how long a tile stays good,
and whether the area around home is worth downloading once so that the map
draws without the network at all. The tile server's terms have something to say
about bulk downloading, so the last part is a question of how much, not only of
how.

## Maps that work without a connection

Beyond caching what has been looked at, a map that works with no network at
all: a prepared archive of an area on the phone, drawn from a file rather than
from a server. The library reads such archives, and the formats worth weighing
are the ones it supports against what can be produced for a chosen area and how
large the result is. The point is the case where the connection is gone but the
last known positions are not — knowing where somebody was is of little use
without a map to place it on.

## A say in where the position comes from

The phone takes whatever the fused provider hands it. There is no way to tell
it to distrust a source, and no way to correct one that is consistently off:
a satellite fix indoors can be hundreds of metres out while the network fix
beside it is right, and both arrive looking equally certain.

Worth having: a setting that says which sources count, a rule that drops a fix
whose accuracy is worse than a threshold, and a correction applied to a source
known to be biased. The raw provider can be asked for one source at a time,
which is what makes any of this possible; the fused one cannot.

## Members that merge into one dot when zoomed out

Two members a few hundred metres apart become one dot on a city-wide view, and
their labels land on top of each other. The map draws every marker at its own
point and nothing groups them.

What map applications do instead: group the markers that overlap into one that
carries the count, and split them again on the way in. The rules worth settling
are when a group forms, what it says, and what tapping it does — zoom to the
group, or list its members. Until then the labels could at least be hidden at
low zoom, which stops the pile-up without hiding anyone.

## Taking a position somewhere else

The card shows a member's coordinates and that is all that can be done with
them. Reading the pair off the screen and typing it into another application
is the only way to get a route to it, and typing a coordinate by hand is the
step where it becomes a different place.

Worth having on both clients: copying the pair to the clipboard, opening it in
whichever map application the reader would rather use, and sending it on to
somebody who is not in the group at all. Google Maps, Yandex Maps and the rest
are reached by a link with the coordinates in it; the phone can hand the same
point to whatever is installed, a messenger included, and the page has the
browser's own share for that.

The pieces to settle are which destinations are offered, what the copied and
sent text looks like — a bare pair, a link, or both — and whether the phone
offers the applications it has rather than a fixed list. Sending a position
outside the group is also the one place where a member passes on where somebody
else is, so it is worth deciding whether that is offered for everybody or only
for oneself.
