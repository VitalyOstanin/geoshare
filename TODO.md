# Ideas not scheduled yet

## Contents

- [Home screen shortcuts to one member](#home-screen-shortcuts-to-one-member)
- [A choice of map background](#a-choice-of-map-background)
- [A way to exchange positions other than Firebase](#a-way-to-exchange-positions-other-than-firebase)
- [Which members the panel keeps in reach](#which-members-the-panel-keeps-in-reach)
- [Russian, alongside English](#russian-alongside-english)
- [Light and dark, and a say in it](#light-and-dark-and-a-say-in-it)
- [Zooming with one thumb](#zooming-with-one-thumb)
- [A control that fits everybody on screen](#a-control-that-fits-everybody-on-screen)
- [The battery level on the member buttons](#the-battery-level-on-the-member-buttons)
- [Keeping the tiles](#keeping-the-tiles)
- [Maps that work without a connection](#maps-that-work-without-a-connection)
- [A say in where the position comes from](#a-say-in-where-the-position-comes-from)
- [Sideways, and not only upright](#sideways-and-not-only-upright)
- [Taking a position somewhere else](#taking-a-position-somewhere-else)
- [Naming the phones: what is left](#naming-the-phones-what-is-left)
- [What a stranger would have to do to use this](#what-a-stranger-would-have-to-do-to-use-this)
- [An administrator's panel for the family](#an-administrators-panel-for-the-family)
- [The phones fetch their own updates](#the-phones-fetch-their-own-updates)
- [The screen says the satellites are being spoofed](#the-screen-says-the-satellites-are-being-spoofed)
- [Turning one member's position on for the rest, in an emergency](#turning-one-members-position-on-for-the-rest-in-an-emergency)
- [The phone being looked for makes a sound](#the-phone-being-looked-for-makes-a-sound)
- [Beyond a location: a shared list, reminders, and notes pinned to a place](#beyond-a-location-a-shared-list-reminders-and-notes-pinned-to-a-place)
- [The service coming back after a restart, checked on the phones](#the-service-coming-back-after-a-restart-checked-on-the-phones)
- [A choice of siren sound on the device](#a-choice-of-siren-sound-on-the-device)
- [Watching a member of another family](#watching-a-member-of-another-family)
- [Short messages between the members of a group](#short-messages-between-the-members-of-a-group)
- [A browser extension over the page's code](#a-browser-extension-over-the-pages-code)
- [Geofence and movement on Huawei devices without Google services](#geofence-and-movement-on-huawei-devices-without-google-services)

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
they are now, and Yandex Maps, which is the better one over Russia.

The first candidate is Yandex's Tiles API on the web page, as a fallback layer.
Its terms, read in September 2026 (<https://yandex.ru/legal/maps_api/>):

- **Free** for commercial and non-commercial use up to 30 requests a second,
  and allowed in closed systems as well as open ones.
- **A key of one's own.** It comes from Yandex's developer cabinet and may not
  be handed to anyone else, so whoever deploys the page brings their own key,
  restricted to the page's domain; without a key the choice is not offered.
- **On the map.** Yandex's logo and copyright, a link to
  <https://yandex.ru/legal/maps_termsofuse> and an "Open in Yandex Maps" button.
- **Caching** no longer than 30 days.
- **Visitors' data.** Yandex receives the visitors' addresses and locations,
  and their consent is the page owner's to obtain.
- **Technically** `https://tiles.api-maps.yandex.ru/v1/tiles/` with
  `projection=web_mercator` (the default projection is elliptical and does not
  line up with MapLibre), `scale=2` on dense screens, and a line in the page's
  `img-src`.

The choice itself is a setting and a tile source behind it, but every
candidate carries its own terms, key and attribution.

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

## A control that fits everybody on screen

Moving the map onto one member is a tap; seeing all of them at once is not.
The buttons frame one position, and the rest is pinching and dragging until
the furthest marker comes into view — which is the thing wanted most often
before leaving the house: is everybody where they should be.

What is needed is a control that frames the positions this member is allowed
to read, all of them, with room around the edges: the readable ones and no
others, since a member whose position is refused has no marker to frame and
zooming out to nothing would be the only honest alternative. One member
readable means the frame is that one marker at a sensible zoom rather than a
point at maximum. Both clients want it, and both libraries can do it — the
work is deciding where the control sits and what it does when nothing is
readable at all.

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

The project owner's take on the shape (October 2026): a context menu on the
position — a long press on the coordinates in the card or on the marker — with
"Share" at its head, and next to it ready-made links that go out at once: one
to Google Maps and one to Yandex Maps, each opening that very point, so that
the person on the other end needs no app of ours and no typing.

## Naming the phones: what is left

Both readings of "names people choose" are built: the owner names their own
phone for the whole group (the devices document), and every reader may name
anybody's phone for themselves (the labels document), which comes first. Two
phones still sharing a name — two of one model nobody named — are numbered in
the order of their ids, "(1)" and "(2)", alike on every screen.

Left: the position document still carries `deviceName`, the model as the
manufacturer wrote it in `Build.MODEL`, and the devices document carries it
too. It goes from the position as a duplicate: the names take the model from
the devices document, and nobody who reads a position is kept from the model
by its going — the devices document is open to every reader of the positions.
Old builds read it, so it goes once every phone runs a build that names phones
from the devices document alone.

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
somebody possible at all.

Established against the emulator: the rules alone can allow all of it, and no
cloud function is needed. A group is created by its founder alone on both
lists; a code is a document named by the code itself, which nobody may read and
the rules read for everybody; the person joining writes the code into a
document of their own, and the rule weighing their addition to the roster reads
it from there, allows the caller's own id and nothing else, and refuses a code
that was never made or has run out. An administrator adds and removes members
and hands the group on, while the administrators stay a non-empty subset of the
members. The document model carries the shapes, and the rules tests cover
twenty-three cases of it.

What the rules cannot do is spend a code. Adding oneself and marking the
invitation used are two writes, and nothing makes the second follow the first,
so a code lets in everybody holding it until it runs out. That is why an
invitation carries an expiry rather than a count, and why removing a member is
part of the same piece of work rather than a later one.

Built on top of it: the screens, in the application and on the page alike.
Somebody with no group is offered both ways in at once, because neither client
can tell a group that has not been made from a group it is not on — both read
as a refusal. A member sees the roster; the one running the group hands out
invitations and takes members off; a group nobody runs says so instead of
offering controls whose every write would be refused.

A group is named after the account that made it, and the rules allow no other
name: a name of the founder's choosing would let one account fill the database
with groups nobody is in, a document apiece and each of them free to make. So
the name is not something a client knows when it is built, and both remember
which group they are in — the phone beside its sharing switch, the page in its
browser storage — falling back to `family`, the household that started before
any of this. An invitation is therefore one string, `group.code`: the rule
weighing somebody's addition to a roster weighs a write to a path, and the path
has to be known before the write is made.

Left on top of that: naming administrators on a screen, which is the section
below, and the first write that fills in the `admins` of a group made before
the list existed.

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

**The whole path, built end to end.** Settled: somebody who does not write
software is to get from an empty phone to a household sharing positions
without a console and without being handed a file. Joining a group by
invitation is built already; what is missing is everything before it, and it
has to work as one path rather than as parts that each work on their own:

- Making the project, turning on sign-in and the database, deploying the rules
  and registering the build's fingerprint. The offered path is the script that
  drives the Firebase command line; the manual steps stay written down for
  somebody who cannot run it.
- Handing the configuration to the phone. The configuration is no longer known
  when the build is made, so the application starts Firebase from what it was
  given rather than from a file compiled into it, and there is a screen where
  that is entered — as a code carried across from the script rather than typed
  field by field.
- The same for the page, which today is configured by a file written at deploy
  time.
- Saying whose storage a member is in. The application knows which account owns
  the project, and somebody joining is told before they agree to be looked at.
- The first member founding the group, then inviting the rest, which is the
  part that works today.

What it costs: Firebase is started later than it is now, so everything reading
the database has to cope with "not configured yet" as an ordinary state rather
than as a failure; and the walkthrough becomes something to keep working as
the Firebase console and its command line change under it.

## An administrator's panel for the family

The member who owns the Firebase project is already unlike the others: the
project is theirs, the quota is theirs, and the rules are written by them. That
position could carry a screen the other members do not have — on the page and
in the application both — showing every device in the group and what the
project knows about it.

The first thing that screen is needed for is a position that has stuck. The jump
filter anchors on the last believed fix, and where the anchor itself is wrong
the phone can refuse every honest answer that follows. On the phone this is
settled by the button that forgets the last place, but the phone is not always
the one to ask: a child's phone is in a pocket somewhere, and the member holding
it should not have to be talked through a settings screen. The administrator
should be able to clear the anchor of any device in the group from where they
are.

What it costs, so that the decision is made with the price in view:

- A command has to reach a phone that is not being used. Today nothing travels
  towards the phone except what the collection service already reads, so the
  command is a document the service watches, the administrator writes and the
  phone clears once it has acted. Waking a phone that is not listening is the
  same unsolved problem as the push item above.
- The rules have to say who may write such a command, and the answer is not "the
  project owner" by default: ownership is a billing fact, not a permission
  Firestore knows about. The administrator has to be named in the data like
  anybody else.
- A command that reaches into another person's phone is a different thing from
  sharing a position with them, and it should look like one: the phone says in
  its log what was asked and by whom, and the screen where the member sees it
  is not hidden behind a developer setting.

Settled: this is to be built rather than weighed further. What it starts from
is the part that needs nothing new travelling towards a phone — the group seen
whole from one screen: every member, every device they have listed, when each
last wrote, and who runs the group. The commands that reach into another
phone come after that, because they wait on waking a phone that is not
listening, which is its own unsolved item above.

### The tier, the limits and what the project has spent

The same screen is where the household should be able to see what the free
tier has left. Everything here is built to stay inside it — a position every
few minutes, three fields a minute while a phone is being lied to, no
functions, no storage — but nothing in the application says how close that
comes to the daily allowance, and the first sign of going over would be phones
failing to write at all.

What such a screen shows: which tier the project is on, the daily allowance of
document reads, writes and deletes against what has been used today, and the
same for the stored size. Whose it is: the member who owns the project, since
the numbers are a billing fact.

What it costs:

- The numbers are not in Firestore, so the client cannot read them the way it
  reads everything else. They come from the Cloud Monitoring API of the
  project, which needs a credential the phones do not have and should not be
  given.
- A credential that reads billing figures is worth more than anything else the
  application holds, so it cannot ride in a build. Either the owner signs in on
  that screen with the account that owns the project, or something outside the
  phones reads the numbers on a schedule and writes them into the group where
  the rules can share them with the household.
- The second way needs something that runs without a phone — a function, a
  scheduled job — which is exactly what the free tier does not include. That
  makes the first way the one to start with, and it means the screen works only
  for the owner.

## The phones fetch their own updates

Every phone in the household is updated by cable: a build is made, `adb
install` is run against each device in turn, and the application is opened by
hand so that the service comes back. That works while all three phones are on
one desk and stops working the moment one of them is not — the third phone
spent an evening on an old build for no better reason than being unplugged.

What is wanted is the phone asking, on its own and rarely, whether a newer
build exists, and offering to install it.

What it costs, so that the decision is made with the price in view:

- **Somewhere to ask.** The application is not in a store, so the answer has to
  come from somewhere the household controls: a small document beside the page
  on Hosting, or a release on the repository, saying the newest version code
  and where its package is. Whatever answers has to be reachable without a
  session, since a phone that cannot sign in is exactly the phone that needs
  fixing.
- **The install is not silent.** A package installed by an ordinary
  application goes through the platform's own confirmation, and the member has
  to allow this application to install packages at all. Silent installation
  belongs to a device owner, which means enrolling the phones into management —
  a great deal of machinery for a household of three.
- **The signature is the check that matters.** The platform refuses a package
  signed by anybody else, which is what keeps this from being a way into the
  phone. Nothing about that is optional or worth working around.
- **The service does not survive the install.** Replacing the package stops
  the collection, and nothing brings it back until the application is opened.
  An update that quietly ends the sharing is worse than an old build, so
  whatever does this has to bring the service back and say in the log that it
  did.
- **Asking costs battery.** The question is worth asking about once a day, on
  a charger and over Wi-Fi, and never in the middle of the modes that matter.
  A phone that checks for updates every hour has been made worse, not better.

Asking once a day is the ordinary case, and it is the wrong one for the case
that matters: a build that fixes something the household is suffering from now
should not wait for tomorrow's question. So the household should be able to
start an update from the administrator's panel, and the phones should hear about
it rather than be asked again.

What that adds, on top of the above:

- **A push, and nothing else reaches a sleeping phone.** The phones already
  cannot be woken from outside — the item about waking a phone without the paid
  tier says so — and an update nobody is present for is exactly the case where
  waiting for the phone to speak first is useless. Whatever carries this is the
  same machinery that carries a request to sound or an emergency grant, and it
  should be built once.
- **The panel says who is on what.** A household cannot decide to update
  anything without seeing which phone runs which build and when each of them
  last asked. That listing is worth having on its own, before any update is
  ever started from it.
- **Starting is not installing.** What the panel starts is a phone fetching and
  offering; the platform's confirmation still stands in front of the install,
  and a phone in a pocket will not answer it for hours. The panel has to show
  the difference between asked, fetched, and running the new build, rather than
  reporting the update as done the moment it was requested.
- **One phone at a time, and reversibly.** An update that breaks the collection
  breaks it on every phone at once if every phone takes it at once. Starting
  with one phone, seeing it come back, and only then going on is the difference
  between a bad build costing an evening and costing the household its map.

## The screen says the satellites are being spoofed

The phone already knows: the log names every answer it leaves out, and the
service knows which radio the position it wrote came from. None of that reaches
the screen, and a member looking at the application sees a position with no
sign that the satellites are saying somewhere the phone has never been and that
what is on the map came from the towers instead.

What the screen should carry:

- **That a spoof is going on right now.** A line the moment answers start being
  left out for landing in a place the phone counts as false, naming the place
  and how long it has been going on. It ends when the satellites are believed
  again, and the line says that too.
- **Where the position on screen came from.** The satellites, the towers and
  the Wi-Fi, or a repeat of what the phone last believed — named on the
  position itself rather than buried in the log. Under a spoof the honest
  position comes from the towers and is hundreds of metres wide, and a member
  reading fifty metres of accuracy off a satellite fix would be reading the
  spoof.
- **What the phone is leaving out.** A count over the last hour, so that a
  member can tell a quiet radio from a radio being answered by somebody else.

What it costs: the service has to hand this out rather than only write it to
its own log, the screen has to keep it while the application is not in front,
and every wording here is one more line to translate.

## Turning one member's position on for the rest, in an emergency

The administrator's panel decides who sees whom. What it cannot do today is
change that in a hurry: an older member who has gone quiet and is being looked
for by the whole family is exactly the case where every relative needs their
position at once, and the person who could grant it is the person nobody can
reach.

What it should carry:

- **A switch per pair, and a switch for the household.** Turning one member
  visible to everybody is one action rather than a grant added for each
  relative in turn, and it is undone the same way.
- **The switch says how long it stands.** An emergency ends, and a grant that
  outlives it is a grant nobody remembers making. A day is the useful length,
  with the panel naming when it lapses and offering to hold it longer.
- **Everybody is told.** A position becoming visible without the member being
  told is the one thing this must not do quietly, even when the member is the
  one being looked for: the phone says it on its own screen, and the panel
  names who opened it and when.
- **Who may do it.** Only an administrator of the household, and the change is
  written down where the others can read it afterwards.

What every screen shows while it stands:

- **The phone of the member being looked for.** Its own screen says so, in
  words rather than in a mark: that the household has opened its position, who
  opened it, and until when. A phone that gives more away than usual and looks
  exactly as it did is the version of this worth refusing to build.
- **Every other phone, and the page.** The member being looked for is marked
  wherever they are named — in the strip of cards, on the pin, in the card
  itself — so that a relative opening the map sees who the search is for
  without being told separately.
- **More than one at a time.** A household can be looking for two members at
  once, so this is a state each member carries rather than one switch for the
  household: the marks stand on each of them, the panel lists them together,
  and ending one leaves the others standing.

What it costs: the rules on the server have to admit a grant with an end to it,
the page has to keep showing a position it is about to lose, and the application
grows a state it has to show on its own screen rather than only obey. None of it
is free, and all of it belongs to the administrator's panel rather than beside
it.

## The phone being looked for makes a sound

A position on a map ends the search from across town and does nothing in the
last ten metres: a phone under a coat on the back seat, or in the hands of
somebody who cannot answer, is found by sound rather than by coordinates. The
same sound is what finds a phone left behind in a flat, which makes this one
piece of machinery serving two cases.

How it behaves:

- **Loud, and on the alarm's terms.** Played the way an alarm is played rather
  than the way a notification is, so that a phone turned down is still heard,
  and at a level set for this rather than at whatever the phone was left at —
  restored to what it was afterwards. Whether a silenced phone and one under Do
  Not Disturb really let this through has to be tried on these phones rather
  than taken from the documentation: the platform's rules and the maker's own
  differ, and the case this exists for is the one where the phone was silenced
  on purpose.
- **It says on screen what it is doing and who asked.** A phone screaming with
  no explanation is a phone somebody turns off. The screen names the member who
  started the search and offers to stop the sound; stopping the sound is not
  stopping the search, and the phone says so and goes on sharing its position.
- **It ends by itself.** A few minutes of sound, then quiet, with the search
  still standing and the sound able to be asked for again. A phone sounding
  until its battery is gone has been made useless at exactly the wrong moment.

Who it reaches, which is the same choice the emergency grant makes above:

- **Everybody in the household.** The ordinary case: an older member has gone
  quiet and the family is looking.
- **A part of it, named.** The two relatives actually out looking rather than
  every phone in the group — a grandchild at school does not need this. The
  named part is remembered, because the same few people do the looking every
  time.
- **Nobody but its owner.** Which is the lost phone: the owner asks their own
  phone to sound, and nothing about it reaches anybody else's screen.

What it costs, beyond the sound itself:

- **The phone hears about it late.** Nothing wakes these phones from outside —
  see the item above about waking a phone without the paid tier — so the
  request waits until the phone next speaks to the server: up to twenty-five
  minutes in the settled mode, and a couple of minutes while somebody has the
  map open. A member pressing this and hearing nothing for twenty minutes has
  to be told that, on the button itself.
- **Somebody must be able to stop it from outside.** The member who started the
  sound ends it, and the end has to reach the phone the same slow way the start
  did.
- **It is a thing one phone does to another.** The rules on the server decide
  who may ask, the phone refuses what does not come from its own household, and
  every request is written down where the household can read who asked for
  what.

## Beyond a location: a shared list, reminders, and notes pinned to a place

The household already shares one thing through this app — where everyone is —
and could share more the same way: what to buy, what needs doing, and notes
that are about a place. Four pieces, loosely related:

- **A shared shopping list.** One list the whole group edits, the way
  positions are already shared — items added, checked off, and removed by
  whoever is at the shop.
- **A reminder.** Something the group or one member is meant to remember,
  read back at the right time rather than buried in a chat.
- **Quick capture.** Adding to the list should not require opening the map
  first and finding the right screen — a home screen shortcut (or a widget)
  that opens straight onto the list, and dictation for entering an item by
  voice, are both about the same thing: getting an item in with as few taps as
  possible, since a shopping list is usually filled in a hurry, one-handed, in
  a shop.
- **Notes tied to a place.** A note pinned to a point on the map, either
  shared with the group (a gate code, where the spare key is) or personal to
  whoever wrote it.

Worth deciding before any of this is built:

- **Whether this belongs in geoshare at all**, or is a separate application
  that happens to share the same family and the same Firebase project. The
  positions are read continuously and pushed by a background service; a
  shopping list is read on demand and has no service of its own. Folding both
  into one application couples their release cycles and permissions for a
  gain that is mostly one icon on the home screen rather than shared code.
- **Whether personal notes belong in the same collection as shared ones**, and
  what "personal" means when a member may sign into the account from more
  than one phone — the group already separates a member from a phone for
  positions, and personal data would need the same care carried over.
- **What the map already gives for free.** The same background and the same
  MapLibre and Compose maps could draw a note's marker, but a note is a fixed
  point rather than a moving position and needs its own marker style, so the
  two are never mistaken for one another on the same screen.
- **Where a reminder is read back.** A time, a place ("remind me at the
  shop"), or both — the second needs the same geofencing a spoof-detection
  screen would also want, which is its own undecided piece of work.

## The service coming back after a restart, checked on the phones

The receiver that starts the service again after the phone restarts and after
the application is replaced exists and is tested, but only against the code:
`BootReceiver` has never been watched through an actual restart of an actual
phone. That is the wrong way round for this particular piece. What it guards
against is silence nobody notices — a phone that stopped writing still has a
marker on everybody's map, freshly placed and slowly ageing — and the failure
would look exactly like a phone doing nothing wrong.

What to check, on each of the phones rather than on one:

- **That it starts at all.** Restart the phone, leave it alone, and look for
  the notification coming back on its own and for a position written after
  the restart. The log says which of the two happened, `boot` or `an update`,
  so the line is what confirms the receiver ran rather than somebody opening
  the application by habit.
- **That a refusal is a refusal and not a crash.** The receiver weighs four
  conditions before starting — signed in, sharing turned on, location granted,
  and background location granted, since a start from a receiver is a start
  from the background — and writes the reason it stopped. A phone whose
  location permission is "while using the app" is meant to stay quiet and say
  so in the log, not to start and be killed by the platform.
- **What the manufacturer does about it.** Several Android shells keep their
  own list of applications allowed to start themselves, and an application
  outside it never receives the broadcast at all, whatever the manifest says.
  Whether these phones are in such a list, and what the member has to switch
  on if they are not, belongs in the instructions rather than in the code —
  but it has to be found out by restarting, because nothing in the build can
  report it.

## A choice of siren sound on the device

The siren plays the application's own SOS sound, the same on every device.
The member whose device it is should be able to pick another one, the way a
ringtone is picked: the application's own sounds, and the device's alarm and
ringtone sounds through the system picker (`RingtoneManager.ACTION_RINGTONE_PICKER`
with `TYPE_ALARM`), with a preview before the choice is kept.

Worth settling before it is built:

- **Whose choice it is.** The siren is started by somebody else to find this
  device, so the sound is a setting of the device that sounds, kept on it,
  not something the member who presses the button chooses.
- **What still has to hold.** Whatever is picked plays on the alarm stream at
  full volume and through "Do Not Disturb", like the stock sound now; a quiet
  or very short sound defeats the purpose, so the picker may need to warn
  about it or keep the SOS as the default it returns to.
- **A sound that disappears.** A chosen system sound can be removed or become
  unreadable later; the siren then falls back to the application's own sound,
  as it already falls back from its own sound to the alarm.

## Watching a member of another family

A member of one family is let watch, for a while, one member of another
family — say, a child staying with friends. They should see their own family
and that one member on the same map at once.

Nothing in the design allows it now:

- A permission to watch names the reader by their account, and an account
  belongs to one Firebase project. A reader from another family's project has
  no account in this one to be named by.
- A position is read only by a member of the group it lies in, so even inside
  one project a permission does not reach across groups.
- The application writes into and reads one group of one project.

The way to build it: a second connection. The application opens a second,
named `FirebaseApp` for the other family's project, and the member signs in
there separately, through the browser the same way as into their own. The map
puts the members from both connections together. The other family has to take
the reader in as a guest: a guest is on their roster only to be named in a
permission, and sees nobody but those who shared with them — not the rest of
the family.

Worth settling before it is built:

- **The guest role.** The rules have to tell a guest from a member: a guest
  reads the positions shared with them and nothing else — not the roster, the
  other positions, the settings or the watch claims.
- **How the guest gets in.** An invitation marked as a guest's, handed over the
  same way as a family's own, carrying the other project's settings.
- **When it ends.** The permission runs out on its own; the guest's place on
  the roster and the second connection should go with it rather than linger.
- **The runtime settings.** The project's settings are read at start-up
  (`FirebaseOptions`); keeping room for a second, named connection is cheaper
  to plan now than to retrofit.

## Short messages between the members of a group

A minimal chat inside a group: a member writes a line, the others in the same
group see it. Meant for what goes with a position rather than instead of a
messenger — "I'm at the entrance", "wait for me at the car", "running late" —
so that the map and the words about it are on one screen.

Minimal means:

- **Text only, one thread per group.** No pictures, files, voice, reactions
  or threads inside threads; no messages between two members alone at first.
- **Members only.** The rules let a member of the group read and write its
  messages and nobody else, the way the roster already is; a member taken off
  the group stops reading them at once.
- **Kept briefly.** Old messages go after a fixed time or past a fixed count,
  so a group's project stays inside the free quota and the database is not an
  archive of the family's talk.

Worth settling before it is built:

- **Being told of a new message.** Without push (see "The phones fetch their
  own updates" and the paid-tier question for waking a phone) a message
  reaches a phone only while the application or its service listens. Whether
  the service listens to the thread, and what that costs the battery, decides
  whether this is a chat or a notice board.
- **Where the messages live.** A collection under the group in its own
  project; each message carries its author and the server's time, and the
  rules hold its shape the way they hold every other document's.
- **The page.** Whether the web page reads and writes the thread too, or the
  phones alone.
- **Relation to the shared list and notes** (see "Beyond a location"): both
  are short text shared within a group, and may be better as one feature than
  two.

## A browser extension over the page's code

The group's map in a browser without opening the page: an icon in the
toolbar, the map in its popup, built from the same code as the page. Whether
it is needed at all is an open question; what has been found so far:

- **Signing in.** Firebase signs an extension in through a page it opens in an
  offscreen document, and the extension's origin has to be an authorised
  domain of the project. Every group has its own project, so that is one more
  step for every project owner, unless the setup page can add it.
- **Where the subscription lives.** A popup subscribes each time it opens and
  starts cold; an offscreen document keeps the subscription warm but then the
  devices of the group would be told someone is watching all the time, and
  spend their battery on it; a side panel stays open beside any tab and keeps
  a clear lifetime.
- **Several groups.** The page already handles more than one group; the
  extension would have to as well.

## Geofence and movement on Huawei devices without Google services

Without Google Play services the `Idle` collection mode is out of reach: the
geofence and the movement recognition both come from Play services, and the
platform's own proximity alert checks the position only every half hour, too
rarely to tell when someone has left. Huawei's Location Kit offers both pieces
on Huawei devices with HMS Core.

Worth doing if a Huawei device without Google services joins a group and its
battery matters. The price: an application registered in AppGallery Connect,
its configuration file and signing fingerprint, and a closed-source library in
the build — which sits poorly with a project anyone is meant to build and
check from source, so it would have to stay optional.
