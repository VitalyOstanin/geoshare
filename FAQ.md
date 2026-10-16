# Questions and answers

English · [Русский](FAQ.ru.md)

## Contents

- [What is it?](#what-is-it)
- [Can I rely on it in an emergency?](#can-i-rely-on-it-in-an-emergency)
- [Has every feature been thoroughly tested?](#has-every-feature-been-thoroughly-tested)
- [Does it work only on devices with Google services?](#does-it-work-only-on-devices-with-google-services)
- [Who sees where I am?](#who-sees-where-i-am)
- [Does the project owner see everything?](#does-the-project-owner-see-everything)
- [Does the author of geoshare see other groups' projects?](#does-the-author-of-geoshare-see-other-groups-projects)
- [Where is the data kept?](#where-is-the-data-kept)
- [What does it cost?](#what-does-it-cost)
- [What is the setup page for?](#what-is-the-setup-page-for)
- [Why is it not on Google Play?](#why-is-it-not-on-google-play)
- [How do I update the application?](#how-do-i-update-the-application)
- [Does it work on an iPhone?](#does-it-work-on-an-iphone)
- [Can I be in several groups?](#can-i-be-in-several-groups)
- [How do I leave a group, and what happens to my data?](#how-do-i-leave-a-group-and-what-happens-to-my-data)

## What is it?

First of all, a pet project: written by one person for their own family and
friends. Anyone who finds it useful is welcome to use it.

## Can I rely on it in an emergency?

No. It is a non-commercial project and comes with no obligations of any kind.
If it helps somebody find a lost phone, the author will be glad, but there is
no guarantee that it will.

## Has every feature been thoroughly tested?

No. These are the first releases. The author has checked everything with
care, but mistakes are possible.

## Does it work only on devices with Google services?

No. There is no hard dependency on Google Play services: the application has
been tried on a Huawei tablet without them. A Google account is all it needs.
The one thing missing on such a device is the collection mode that saves
battery while the device lies still, since it is built on Play services.

## Who sees where I am?

The members you choose yourself, one by one, each until a moment you pick;
taking a permission back closes your position at once. Being in a group tells
the others that you are in it, not where you are.

An administrator sees whether your devices are in touch, not where they are.
When you cannot be reached, an administrator can open your place to the whole
group for six hours, a day or three days; your device tells you so and lets you
end the search with one tap.

## Does the project owner see everything?

They can if they want to, and nothing can prevent it: the project owner has
direct access to the database from the Firebase console, past the rules, as
Google itself does. In the application they see only what the members have
shared with them, like anybody else. Whoever sets the project up should be
somebody the group trusts with that; the application says so to everybody
before they join.

## Does the author of geoshare see other groups' projects?

No. The author of the application cannot see the data in anybody else's
Firebase project: access to a project is limited to the Google accounts it is
given to.

## Where is the data kept?

In the group's own Firebase project, which the project owner creates and
holds, and nowhere else. geoshare has no server of its own and no analytics.
Each new position replaces the previous one, so no history of places is kept.

## What does it cost?

Nothing. Everything runs on the free Spark plan of Firebase: a family with two
devices uses about 5% of its daily reads and writes. Past a daily limit the
database stops answering until the quota resets, so the project owner should
keep an eye on usage; the [README](README.md#free-tier-limits) has the figures.

## What is the setup page for?

Convenience. The [setup page](https://geoshare-setup.web.app/) makes a new
group's project from a browser, without a terminal. The same can be done from
a terminal alone with [tools/project.sh](tools/project.sh)
([docs/new-project.md](docs/new-project.md)), but either way a couple of steps
have to be taken in the Firebase console
([docs/console-steps.md](docs/console-steps.md)).

## Why is it not on Google Play?

Publishing there takes a developer account and Google's review of every
release, which a pet project does without. The application is downloaded from
the [releases page](https://github.com/VitalyOstanin/geoshare/releases) on
GitHub and installed by hand.

## How do I update the application?

By hand, for now: the application does not check for new versions itself.
Download the new release from the
[releases page](https://github.com/VitalyOstanin/geoshare/releases) and
install it over the old one; the groups and settings stay.

## Does it work on an iPhone?

There is no application for iPhone. The group's web page shows the map in a
browser, but it has not been tried on an iPhone, and the page only shows the
others: it does not report where its own device is, so a member on an iPhone
can see the group without being seen.

## Can I be in several groups?

Yes. A device can be in more than one group at once, each in a Firebase
project of its own. The map shows one group at a time, and the position goes
to every group, each with its own list of who may see it.

## How do I leave a group, and what happens to my data?

Leave it from the application. The members stop seeing you and your devices,
and the application deletes what you wrote into the group: your profile, your
permissions, your list of devices, the names you gave, and every device's
position. Coming back takes a new invitation.

An administrator cannot leave a group that would then have nobody to run it;
in that case the group is removed from the device only, and they stay in it.
