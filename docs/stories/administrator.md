# Administrator

A member listed in the group's `admins`. The founder is the first one and can
make others. Everything a member does (see [member.md](member.md)) an
administrator does too; the stories here are what only an administrator does.

## Contents

- [ADM-01 Found the group](#adm-01-found-the-group)
- [ADM-02 Rename the group](#adm-02-rename-the-group)
- [ADM-03 Invite a person by their Google address](#adm-03-invite-a-person-by-their-google-address)
- [ADM-04 Take a member off the roster](#adm-04-take-a-member-off-the-roster)
- [ADM-05 Make a member an administrator, or stop being one](#adm-05-make-a-member-an-administrator-or-stop-being-one)
- [ADM-06 See who is in the group and whether their phones are alive](#adm-06-see-who-is-in-the-group-and-whether-their-phones-are-alive)
- [ADM-07 Open a search for a lost device](#adm-07-open-a-search-for-a-lost-device)
- [ADM-08 Make a lost phone sound](#adm-08-make-a-lost-phone-sound)
- [ADM-09 Reset the place of a stuck phone](#adm-09-reset-the-place-of-a-stuck-phone)
- [ADM-10 Record the places where receivers are lied to](#adm-10-record-the-places-where-receivers-are-lied-to)
- [ADM-11 Start an update of the application on a member's phone](#adm-11-start-an-update-of-the-application-on-a-members-phone)
- [ADM-12 Not see a member's place without their permission](#adm-12-not-see-a-members-place-without-their-permission)

## ADM-01 Found the group

- Status: done
- ADR: 0009, 0016, 0019

As the first person to sign in to a new project I found the group, giving it a
name if I like, so that there is a group to invite the others to.

Acceptance:
- the group's id is the founder's uid; the founder is alone on the roster and
  among the administrators;
- without a name the group is shown as "Группа · <founder's name>" in Russian
  and "<founder's name>'s group" in English;
- both the phone and the page can found it.

## ADM-02 Rename the group

- Status: done
- ADR: 0016, 0019

As an administrator I rename the group from the group panel, so that the
members tell it apart from their other groups.

Acceptance:
- only an administrator may change the name;
- every phone and page of the group shows the new name without restarting.

## ADM-03 Invite a person by their Google address

- Status: done
- ADR: 0015, 0017, 0018, 0019, 0025

As an administrator I make an invitation for one Google address, so that only
that person can join on the link and only once.

Acceptance:
- the invitation is a connection link, sent through the system's share menu,
  copied, or shown as a QR code;
- the link carries the group's name, the inviting member's name and the
  address shortened as `ivan…@example.com`; the full address is known to the
  rules alone;
- the link admits the named account with a verified address and nobody else;
- joining deletes the invitation in the same write, so it admits once;
- an invitation lasts a day, and the rules refuse one set to last longer than
  25 hours;
- on the phone the panel says when the invitation has been used or has run
  out, and offers to make a new one.

## ADM-04 Take a member off the roster

- Status: done
- ADR: 0026

As an administrator I take a member off the roster, so that they no longer see
the group nor are seen in it.

Acceptance:
- an ordinary member can be taken off; an administrator must first stop being
  one (ADM-05);
- nobody can take off the last administrator;
- the device of the member taken off says so instead of showing the map
  (DEV-08).

## ADM-05 Make a member an administrator, or stop being one

- Status: done

As an administrator I make another member an administrator, or step down
myself, so that the group is never left to one person.

Acceptance:
- any administrator can make any member an administrator;
- an administrator can stop running the group while another one stays.

## ADM-06 See who is in the group and whether their phones are alive

- Status: done

As an administrator I see the roster with each member's phones: their names,
when each last wrote, in which mode and whether it is charging, so that I
notice a phone that has gone quiet.

Acceptance:
- the panel lists every member, the administrators marked, me marked as me;
- a phone is described by the time of its last write, its mode and its
  charge, or as "not in touch yet";
- a member who shares with nobody is said to share with nobody, and the
  number of readers is said otherwise; who exactly they share with is not
  shown;
- the same panel is on the phone and on the page.

## ADM-07 Open a search for a lost device

- Status: done
- ADR: 0011

As an administrator I open a search for one device for a time I choose, so
that everybody in the group sees where it is while we look, whatever its owner
had shared.

Acceptance:
- while the search stands, every member can read that one device's position,
  and nothing else of its owner;
- the device is marked on every map with a red ring, "searching";
- any administrator, or the owner from any of their devices, ends it and the
  place closes at once;
- a search can be renewed before it ends; several can run at once.

## ADM-08 Make a lost phone sound

- Status: partly — a phone that is asleep hears the call only when it next
  wakes on its own; waking it needs a push
- ADR: —

As an administrator, or as the phone's owner, I make the phone sound like an
alarm, so that it is found under a cushion or in a bag.

Acceptance:
- the phone sounds loudly for 22 seconds, even with its sound off, and says
  on its screen who called; with the owner's leave it sounds through total
  silence too;
- the caller sees whether the phone is waiting, sounding or stopped;
- the owner can stop it on the phone, and the caller from outside.

## ADM-09 Reset the place of a stuck phone

- Status: partly — as ADM-08, the phone acts when it next wakes
- ADR: —

As an administrator, or as the phone's owner, I reset the place of a phone
whose marker is stuck, so that it takes the next place however far it is from
the old one.

Acceptance:
- the phone forgets its last place, takes the next one and says in a
  notification who reset it;
- the one who reset sees "waiting for the device", "the device took it", or
  that it did not answer.

## ADM-10 Record the places where receivers are lied to

- Status: planned — the rules allow it, no screen writes it yet

As an administrator I record a place where receivers are known to be told a
lie, with a centre, a radius and a name, and set the group's threshold for a
track going round in circles, so that every phone of the group leaves out
answers from there.

Acceptance:
- only an administrator writes false places and the threshold; every member
  reads them;
- the phones and the page mark positions taken in a false place.

## ADM-11 Start an update of the application on a member's phone

- Status: planned

As an administrator I start, one phone at a time, the installation of the
already published build, so that a household keeps its phones on one version
without everybody doing it by hand.

Acceptance:
- only a build published on the releases page is offered;
- the application starts on its own after the update and works.

## ADM-12 Not see a member's place without their permission

- Status: done
- ADR: 0008, 0011

As an administrator I have no more right to a member's position than any other
member, so that running the group is not watching it.

Acceptance:
- the rules give an administrator a member's position only through that
  member's grant, or through a standing search (ADM-07);
- the administrator does not see whom a member shares with.
