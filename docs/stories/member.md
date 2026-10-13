# Member

A person on the group's roster, on a device with the application or on the
page in a browser. Joining comes first; leaving and signing out come last.

## Contents

- [Getting in and out](#getting-in-and-out)
  - [MEM-01 Join a group on an invitation link](#mem-01-join-a-group-on-an-invitation-link)
  - [MEM-02 Join on a link made out to another of my accounts](#mem-02-join-on-a-link-made-out-to-another-of-my-accounts)
  - [MEM-03 Be in several groups on one device](#mem-03-be-in-several-groups-on-one-device)
  - [MEM-04 Leave a group](#mem-04-leave-a-group)
  - [MEM-05 Sign out of one group or of all of them](#mem-05-sign-out-of-one-group-or-of-all-of-them)
  - [MEM-06 Sign in again after a sign-in that did not finish](#mem-06-sign-in-again-after-a-sign-in-that-did-not-finish)
  - [MEM-07 Look at the group from the page](#mem-07-look-at-the-group-from-the-page)
- [My devices](#my-devices)
  - [MEM-08 Add another device of mine](#mem-08-add-another-device-of-mine)
  - [MEM-09 See and forget my devices](#mem-09-see-and-forget-my-devices)
- [Sharing my place](#sharing-my-place)
  - [MEM-10 Show my place to chosen members for a time](#mem-10-show-my-place-to-chosen-members-for-a-time)
  - [MEM-11 Stop sharing my place](#mem-11-stop-sharing-my-place)
  - [MEM-12 Show my place back to somebody who cannot see it](#mem-12-show-my-place-back-to-somebody-who-cannot-see-it)
  - [MEM-13 Know who has the map open](#mem-13-know-who-has-the-map-open)
- [Seeing the others](#seeing-the-others)
  - [MEM-14 See where the members are](#mem-14-see-where-the-members-are)
  - [MEM-15 Follow one member on the map](#mem-15-follow-one-member-on-the-map)
  - [MEM-16 Name people and devices for myself](#mem-16-name-people-and-devices-for-myself)
  - [MEM-17 Know where a place came from and why some were left out](#mem-17-know-where-a-place-came-from-and-why-some-were-left-out)
  - [MEM-18 Hear when a search, a siren or a reset concerns me](#mem-18-hear-when-a-search-a-siren-or-a-reset-concerns-me)
- [The application itself](#the-application-itself)
  - [MEM-19 Choose the language and the theme](#mem-19-choose-the-language-and-the-theme)
  - [MEM-20 Choose which networks the application downloads over](#mem-20-choose-which-networks-the-application-downloads-over)
  - [MEM-21 Join by scanning a QR code](#mem-21-join-by-scanning-a-qr-code)
  - [MEM-22 Keep a shared list, reminders and notes on the map](#mem-22-keep-a-shared-list-reminders-and-notes-on-the-map)

## Getting in and out

### MEM-01 Join a group on an invitation link

- Status: done
- ADR: 0001, 0002, 0015, 0019, 0025
- Tests: rules `membership.test.mjs` (joining a group; a code is spent by joining); Android `ConnectFlowTest`, `ConnectLinkTest`, `SettingsFetchTest`, `ProjectSettingsTest`, `PendingInvitationTest`, `WayInPasteTest`; page `join.test.mjs`
- Not covered: sign-in in the browser, by hand

As a person given an invitation link I open it on my device, sign in with the
Google account it was made for and land on the group's map, so that I join
without typing anything.

Acceptance:

- the link opens the application, which fetches the project's settings and
  shows the group, who invited and for which account (ADR-0018 says what is
  not shown);
- sign-in happens in the browser with Google;
- the join writes the claim, the roster entry and the deletion of the
  invitation in one batch;
- a pasted link works as well as an opened one.

### MEM-02 Join on a link made out to another of my accounts

- Status: done
- ADR: 0019
- Tests: rules `membership.test.mjs` (another account holding the same code is refused); Android `PendingInvitationTest`
- Not covered: the screen naming both addresses

As a person signed in with another Google account than the one invited I am
told both addresses and offered to switch accounts, so that I do not stay
outside wondering why.

Acceptance:

- the refusal names the signed-in address and the shortened invited one;
- the invitation is kept for the next sign-in.

### MEM-03 Be in several groups on one device

- Status: done
- ADR: 0026
- Tests: Android `ConnectionListTest`, `ConnectFlowTest` (a link to a project the device is not in yet joins it beside the others), `GroupSwitcherTest`
- Not covered: writing the position into every group; notifications naming the group

As a member of a family and of a trip I am in both groups on one device with
one Google account, so that I do not need a second device or a second
application.

Acceptance:

- a link to another project adds a group beside the others, without a
  restart;
- the map shows one group at a time, chosen from a chip, drawn only while
  there are two groups or more;
- my position is written into every group I am signed in to, with each
  group's own list of readers;
- a search, a siren or a reset is heard from every group and its notification
  names the group;
- my own devices are listed per group, and the screen says so.

### MEM-04 Leave a group

- Status: done
- ADR: 0026
- Tests: rules `leaving.test.mjs`; Android `LeavingTest`

As a member I leave a group, so that I am off its roster and what I wrote
there is gone.

Acceptance:

- one batch takes me off the roster and deletes my position, grant and
  claims, then the device signs out of that project and forgets it;
- a member whose going would leave nobody to run the group only takes the
  group off the device;
- the rules let a member take themselves, and nobody else, off the roster.

### MEM-05 Sign out of one group or of all of them

- Status: done
- ADR: 0026
- Tests: —
- Not covered: signing out of one group or of all of them; taking the watch claim back first

As a member I sign this device out of one group, or of all of them, so that a
device I give away stops showing my groups and sending my place.

Acceptance:

- "here" signs this device out of the group on screen; the other groups stay
  and keep getting my place;
- "everywhere" signs this device out of every group and turns sharing off;
- the device is forgotten in each group it signs out of;
- the "looking at the map" mark is taken back before the sign-out, not after.

### MEM-06 Sign in again after a sign-in that did not finish

- Status: done
- ADR: —
- Tests: Android `SignInFailureTest`
- Not covered: the message on the map after a cancelled sign-in

As a member whose sign-in page showed an error or was closed I am told that
sign-in did not finish and given a button to try again, so that I am not left
on a map of another group wondering.

Acceptance:

- the sign-in screen explains in muted text, not red, and suggests Chrome as
  the default browser if it repeats;
- after a cancelled sign-in to a further group the map of a group already
  signed in says so for a while, with "Sign in".

### MEM-07 Look at the group from the page

- Status: done
- ADR: 0004
- Tests: page tests as a whole: `members.test.mjs`, `searches.test.mjs`, `sirens.test.mjs`, `group-lines.test.mjs`

As a member at a computer I sign in on the group's page and see the same map,
so that I need no device to look.

Acceptance:

- the page shows the members, their places, searches and sirens as the device
  does;
- the page does not publish a position, and grants are given from the device.

## My devices

### MEM-08 Add another device of mine

- Status: done
- ADR: 0007
- Tests: rules `positions.test.mjs` (a member writes a position for each of their devices); Android `ShownNamesTest`; page `members.test.mjs`

As a member with a second device or a tablet I sign in on it with the same
account, so that it shows up as another device of mine rather than replacing
the first.

Acceptance:

- every device writes its own position, named after its model;
- the map labels the device's name only when I have more than one.

### MEM-09 See and forget my devices

- Status: done
- ADR: 0007, 0026
- Tests: rules `access.test.mjs` (devices; grants)
- Not covered: forgetting a device, end to end

As a member I see the list of my devices in the group and forget one I no
longer have, so that the group stops seeing a device I sold.

Acceptance:

- "My devices" lists this group's devices, this one marked;
- forgetting a device, from the device or the page, removes its position and
  watch claim and takes it out of my grant and device list in that group.

## Sharing my place

### MEM-10 Show my place to chosen members for a time

- Status: done
- ADR: 0008, 0031
- Tests: rules `access.test.mjs` (grants), `positions.test.mjs` (permission run out, taken back); Android `ShareSpanTest`, `RefusalsTest`

As a member I choose whom I show my place to, and for how long: an hour, eight
hours, a day or always, so that nobody watches me unless I let them.

Acceptance:

- without a grant nobody reads my position;
- a permission runs out by itself at the time chosen, and the rules refuse
  the read from then;
- a new reader sees my place at once, not on my device's next write.

### MEM-11 Stop sharing my place

- Status: done
- ADR: —
- Tests: Android `StartConditionsTest` (the switch outranks the permissions)
- Not covered: the switch kept over a restart and a new sign-in

As a member I switch sharing off on the device or from its notification, so
that the device stops writing my place.

Acceptance:

- the service stops and its notification goes;
- the switch stays as I left it after a restart and after signing in again.

### MEM-12 Show my place back to somebody who cannot see it

- Status: done
- ADR: —
- Tests: Android `ShareBackTest`

As a member looking at somebody who cannot see me I am told so on their card
and offered to show my place to them, so that sharing is mutual when I want
it.

Acceptance:

- the card says the member does not see where I am;
- one button opens the grant for that member.

### MEM-13 Know who has the map open

- Status: done
- ADR: 0010
- Tests: rules `access.test.mjs` (watch); Android `WatchingTest`; page `watchers.test.mjs`
- Not covered: the badge on the icon

As a member I see who is looking at the map with me in it, so that I know when
I am being watched.

Acceptance:

- "Looking at the map: &lt;names&gt;" is shown while somebody holds a live watch
  claim;
- the application icon carries a badge while my place is being shared.

## Seeing the others

### MEM-14 See where the members are

- Status: done
- ADR: 0006, 0007, 0010
- Tests: Android `PositionAgeTest`, `AccuracyRingTest`, `MemberClustersTest`, `MemberOrderTest`, `GroupLineTest`; page `age.test.mjs`, `clusters.test.mjs`, `members.test.mjs`

As a member I see on a map where the members who share with me are, how old
each place is and how accurate, so that I know whether to trust it.

Acceptance:

- a marker per device, with an accuracy ring;
- a marker goes grey when its place is older than half an hour;
- opening the map makes the watched devices collect faster;
- a member who does not share with me is counted in a line under the map;
- a member without a profile is shown as "Unnamed member · <4 characters>".

### MEM-15 Follow one member on the map

- Status: done
- ADR: —
- Tests: Android `FollowingTest`; page `following.test.mjs`

As a member I keep the map centred on one member as they move, so that I do
not have to pan by hand.

Acceptance:

- dragging the map holds the camera for 20 seconds, then following resumes.

### MEM-16 Name people and devices for myself

- Status: done
- ADR: 0030
- Tests: rules `access.test.mjs` (labels); Android `LabelsTest`; page `labels.test.mjs`

As a member I call a device "new device" rather than its model, and a person
"grandma" rather than the name on her account, for myself alone, so that the
map speaks my words.

Acceptance:

- the names are kept in my own documents and nobody else sees them;
- the device and the page show them alike, cut to 40 characters.

### MEM-17 Know where a place came from and why some were left out

- Status: done
- ADR: 0014, 0020, 0023, 0024
- Tests: rules `positions.test.mjs` (answers left out; how a position was arrived at); Android `FoundByTest`, `ModeExplainedTest`, `ReasonExplainedTest`, `LeftOutWordsTest`, `FalseMarksTest`; page `found.test.mjs`, `modes.test.mjs`, `reason-help.test.mjs`, `false-places.test.mjs`

As a member I see on a card whether a place came from the satellites, the
towers or Wi-Fi, and why the device left some answers out, so that I can tell a
true place from a spoofed one.

Acceptance:

- the card names the source and the collection mode;
- a place taken in a recorded false place is marked;
- the device counts the answers it left out and says why.

### MEM-18 Hear when a search, a siren or a reset concerns me

- Status: done
- ADR: 0011, 0028, 0029
- Tests: rules `searches.test.mjs` (the member whose device it is calls it off), `sirens.test.mjs`, `resets.test.mjs`
- Not covered: the notifications themselves

As a member I am told on the device when somebody opens a search for one of my
devices, makes it sound or resets its place, so that nothing is done to my
device without my knowing.

Acceptance:

- each comes as a notification naming who did it;
- a search for my device can be ended by me from any of my devices.

## The application itself

### MEM-19 Choose the language and the theme

- Status: done
- ADR: 0013
- Tests: Android `LanguageSettingTest`, `ThemeSettingTest`, `TranslationTest`; page `language.test.mjs`, `theme.test.mjs`, `words.test.mjs`

As a member I choose Russian or English and light or dark, so that the
application reads as I want.

Acceptance:

- the language is the system's per-application choice on Android 13 and
  later;
- the page follows the same choices.

### MEM-20 Choose which networks the application downloads over

- Status: done
- ADR: 0022
- Tests: Android `DownloadPolicyTest`

As a member I choose whether the application downloads its own files over
Wi-Fi, the mobile network at home and in roaming, so that a trip abroad does
not cost me.

Acceptance:

- out of the box Wi-Fi and the home network are on, roaming off;
- in roaming I am asked each time, "Download" or "Not now";
- the map and the positions go over any network.

### MEM-21 Join by scanning a QR code

- Status: done
- ADR: 0032
- Tests: Android `QrScanTest`, `ManifestTest`
- Not covered: the camera, the vibration and the permission screens, by hand

As a person shown an invitation QR code on another device I scan it from the
connection screen, so that I do not need the link sent to me.

Acceptance:

- the camera scanner sits beside pasting a link, and in the settings'
  invitation field;
- the camera is asked for only when the scanner is opened;
- a read link opens the same confirmation a pasted one does; the device
  vibrates when it is read;
- a code that is not a connection link is said to be one, and scanning goes
  on.

### MEM-22 Keep a shared list, reminders and notes on the map

- Status: planned
- ADR: —
- Tests: —
- Not covered: planned

As a member I keep a shopping list with the group, reminders, and notes pinned
to a place, so that the group's everyday things live with its map.

Acceptance:

- to be agreed; whether this belongs in geoshare at all is still open.
