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

Paid providers belong in the same choice rather than outside it. Free tiers are
what this household happens to use, not a property of the design: somebody with
a key to a paid map should be able to use it, and the better maps over a given
country are often the paid ones. What that adds beyond a second free source:

- **A key the member supplies.** The key is theirs, not the build's, so it is
  entered in a setting and kept on the phone rather than compiled in. That also
  keeps a paid key out of the repository, which a free one can afford to be in.
- **Terms per provider.** Attribution text, whether tiles may be cached at all
  and for how long, and whether the key may be used from a page as well as from
  a phone — each provider answers those differently, and the answers decide
  what the client may do, not a preference.
- **A shape that admits more than two.** The tile source is already behind its
  own object in both clients; a provider then becomes a name, an address
  template, an attribution line, a caching rule and an optional key, so adding
  one is data rather than code. That is the part worth designing before the
  second provider, not after the fifth.
- **What happens when a key stops working.** A paid key runs out, gets revoked
  or exceeds its quota, and the map must say so and fall back to a free source
  rather than drawing an empty grid.

Worth deciding alongside: whether the choice is per client or shared through
the profile — a key entered on a phone is that phone's, but the preference of
which provider to draw could reasonably follow the member to the page.

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

## Sideways, and not only upright

Nothing fixes the orientation, so the screen turns with the phone, and the
layout is a column: the map above, the panel of members below. Turned
sideways that panel takes a third of what is left of the height, the detail
card for one member has nowhere to open, and the map is reduced to a strip.

Worth settling as a layout rather than a lock: the panel alongside the map
rather than under it when the window is wider than it is tall, with the same
parts in it. The page has the same question on a laptop, where the window is
always wider than tall, so whatever shape is chosen is worth choosing for both
clients at once.

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

## Naming the phones, and who does the naming

A phone calls itself whatever the manufacturer wrote in `Build.MODEL`, and that
string is written into the position document as `deviceName`. Two phones of one
model are then indistinguishable — the card says "BVL-N49" twice — and the
model itself reaches every reader who may see the position, which is a fact
about somebody's hardware that sharing a position did not have to include.

Wanted: names people choose. Two readings of that, and they are not exclusive:

**The owner names their own phones.** A setting listing this member's devices
with a name for each, written into the profile beside the device id and read by
everybody. One name per phone for the whole group, chosen by the person the
phone belongs to. It is the smaller change and it is what lets `deviceName`
leave the position document altogether: the model stops being published, and a
phone is called "the old one" or "work" because somebody said so.

**Each reader names the phones they watch.** The same list, but kept on the
reading phone and never written to the database: a reader labels the devices of
the members they watch, and those labels are theirs alone. It costs nothing in
the data model — it is a local preference keyed by device id — and it answers
the case the first reading does not: a member who never opens the setting still
shows two identical names, and the reader can fix that for themselves.

Worth building the first as the published answer and the second as the local
override, with the position document carrying no model at all once the first
exists. Until then the card falls back to what it has: the model, and the
device id when even that is missing.

Pieces to settle: where the name lives in the grant (a map of device id to
name, beside the `devices` list, with a length limit the rules enforce), what
the page and the application show when a member has one phone (nothing — a
single phone needs no name), and whether a name is asked for when a second
device of the same member first appears rather than being buried in a setting.

## What a stranger would have to do to use this

Everything is one household's: the group is the constant `family` in both
clients, the project keys arrive as files kept out of the repository, and the
roster is an array in the group document that no client may write, so it is
edited by hand in the Firebase console. Somebody who installs a published build
has no way to say where their positions go, who they are shared with, or how a
second phone joins them.

Three separate questions sit behind that.

**Whose storage.** Either each household runs its own Firebase project, or
everybody writes into one project the author pays for. Their own project keeps
the positions and the bill with the person they belong to, and it is what a
free tier can carry at all; it costs a setup the application has to walk
somebody through — creating the project, deploying the rules, handing the
configuration over — and it moves the start of Firebase out of the build and
into a screen, because the configuration is no longer known when the build is
made. One shared project needs no code at all and is the worse of the two:
other people's positions land in an account that is mine, the free tier runs
out, and the bill that follows has a card behind it.

Settled: each household runs its own project. The application therefore may
not assume it knows its project when it is built.

**Making that first run bearable.** Creating a project, turning on sign-in and
the database, deploying the rules and copying the configuration out is a dozen
steps in a console that fights back. A script the person runs once on a
computer can do all of it through the Firebase command line — the project, the
rules, registering the application and reading its configuration back are all
commands, as this project's own setup showed — and finish by printing the
configuration as a code the phone reads. It costs a script to write and keep
working, and it asks for a computer and a terminal, which not everybody who
would install this has. Worth having as the offered path, with the manual steps
written down as the fallback.

**Who is above whom.** The person who creates the project owns it: they read
the whole database from the console without the rules applying, edit the rules,
see every account that signed in, and can delete the whole thing. Nobody else
can, and the only way to level that is inviting a second owner into the project
itself, which is the console work this is meant to avoid. Inside the
application the imbalance is removable: a list of the group's administrators in
the group document, and a rule letting those on it change the roster, puts
inviting and removing in more than one pair of hands. Worth deciding alongside
the invitation code, since both write to the same document.

Standing above the users is not itself unusual — the large map applications
have the same shape, and their server sees everything their users hand it. What
differs is who stands there. Theirs is a company, a stranger to everybody and
equally far from each of them; ours is one of the members, who is also being
looked at by the rest. In a household that is the arrangement people want: the
project belongs to the parent. Among people who met on a trip it means one of
them reads the others' history from a console. So the thing to build is not an
attempt to level the two, it is telling members whose storage they are in:
the application knows which account owns the project, and saying so is what
lets somebody join on terms they understand.

**The signing fingerprint, which the setup cannot skip.** Signing in with a
Google account only works if the project has been told the fingerprint of the
build doing the asking; ours was told the debug one when sign-in was built. A
household running its own project therefore has to register the application in
it with the fingerprint of the published build before anybody can sign in at
all. The fingerprint is the same for every installation of a given release, so
it can simply be printed in the instructions, but the step remains, and it
belongs in whatever script or walkthrough does the first run.

**How a group is joined.** Settled: an invitation code carried in the
application. The first member creates the group and hands out a code; the
person invited enters it and adds themselves to the roster. It is the form that
works for somebody who will never open a console, and it is what makes removing
somebody possible at all. Whether the rules alone can allow the joining write —
one that may only append the caller's own id, against an invitation document
that has to exist and not have expired — is the piece to establish against the
emulator before anything is designed on top of it, because the alternative is a
cloud function, and those are not free.

What joins is a member, meaning an account, not a phone: the code is spent by
the person who enters it, and whatever devices they sign in on come with them.
That raises a question the single-household build never had to answer. One
account signed in on two phones writes into one position document, and the last
write wins, so the two take turns overwriting each other. Either that is
declared acceptable — the phone in somebody's pocket is one phone, and the
spare in a drawer writes nothing — or a position is kept per device under its
member, which changes the document model, the subscriptions and what a marker
means when one person is in two places.

What two phones on one account do today, in full:

- The position document is overwritten by whichever phone wrote last, and the
  marker jumps between two places while looking freshly written both times, so
  nothing on screen explains the jump.
- Neither phone knows what the other wrote: the write filter compares a fix
  against the last one this phone wrote, not against what the database holds,
  so both write on their own schedule and the group pays twice the writes.
- The viewing marker is one document as well. Closing the map on one phone
  deletes it for both, and the other members drop out of their fast mode until
  the phone still looking pushes the marker forward again, up to a minute later.
- The profile is written once per phone, since each remembers separately what
  it has had accepted. The content is identical, so this one is harmless.

Wanted instead: every device of a member drawn on the map. The case is an
ordinary one — a parent hands their old phone to a child for the afternoon and
goes out with the new one, and both are worth seeing at once. That means a
position per device rather than per member: a device identity that survives a
restart, a document under the member for each one, markers that carry the
device as well as the person, and a viewing marker that one device cannot
withdraw on another's behalf. The card and the panel then have to say which
device they are showing, and a member with one phone must not end up reading
its name anywhere. Worth weighing against the simpler answer for that same
case: give the child their own account on the old phone, which the group
already supports.

When: straight after the beta, before anything else is built on top. This is a
change to the shape of the documents, and the longer the clients, the rules and
the page keep assuming one position per member, the more there is to unpick —
every subscription, every marker and every card reads that assumption today.

**More than one group at a time.** A person is in a family and also in, say, a
group of people on a walking trip. Those are different groups with different
members, and both want the same position. With a project per household the
groups do not merely sit side by side — they sit in different projects
entirely, so the phone would hold several configurations, sign in against each
of them, and write the same fix once per group. The battery cost multiplies by
the number of groups, and so does the number of places a position is stored.
Several groups inside one project is the cheaper shape, but then somebody's
project holds people who have nothing to do with them.

**Why any of this is hard: the free tier.** The awkwardness above is not in the
idea of sharing a position, it is in having no storage of our own. A service
with a global store, the way the large map applications have, would know each
person once: a position is written once, and permission is granted person to
person — this one may see me, that one may not — with a group being no more
than a name for a list. Because storage has to be somebody's free Firebase
project instead, identity is scoped to the project, and permission has to be
granted per group and per member both, once in every project a person belongs
to. That is the constraint to keep in view when weighing the item above about
exchanging positions without Firebase: what a store of our own would buy is
exactly this.

**Who may see a position.** Settled: a member names the readers one by one, and
each permission may carry an expiry. Everyone in the group is not the same as
everyone who may watch — a group of people on a trip is not a household — and a
permission that runs out on its own is what lets somebody be visible for the
walk and not for the week after it.

That decision costs more than the switch it replaces, and the costs are worth
stating before it is built:

- The permission has to live where a rule can read it without paying for a
  second document. Inside the position document is that place: a field mapping
  a reader to when their permission ends, with the absence of an end meaning
  indefinite. A separate permissions document would be a `get` on every read,
  and reads are the quota that runs out first.
- Subscribing to the whole positions collection stops working. A collection
  listener is refused outright when a single document in it is out of reach,
  so both clients have to subscribe per member instead, walking the roster.
  That is one listener per member rather than one for the group.
- An expiry that passes turns a working listener into a permission error rather
  than into an empty result, and both clients have to read that as "no longer
  shared" rather than as a failure to report.
- Both clients need the screen where the permissions are given, which is a list
  of members, a switch each and a choice of how long.

None of this is worth building before the two phones work. It is worth deciding
before the storage is designed any further, since the first question decides
whether a client may assume it knows its project at build time.
