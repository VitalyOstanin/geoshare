# Guest

A person holding a link who is not on the group's roster yet. A guest has not
signed in, and nothing they do touches the database until they do.

## Contents

- [GST-01 See what a link invites to, in the browser](#gst-01-see-what-a-link-invites-to-in-the-browser)
- [GST-02 Get the application](#gst-02-get-the-application)
- [GST-03 Open the link in the application](#gst-03-open-the-link-in-the-application)
- [GST-04 Be told that an invitation cannot be used](#gst-04-be-told-that-an-invitation-cannot-be-used)

## GST-01 See what a link invites to, in the browser

- Status: done
- ADR: 0017, 0018

As a person sent a link I open it in any browser and see which group invites
me, who invited and for which account, so that I know it is genuine before I
install anything.

Acceptance:
- the join page reads only the link, not the database and not
  `geoshare.json`;
- it shows the group's name, the inviting member's name and the shortened
  address;
- it does not name the project owner nor say when the invitation expires.

## GST-02 Get the application

- Status: partly — the button is there, a published build is not yet
  (OWN-10)
- ADR: 0018

As a person without the application I tap "Download the app" on the
join page, so that I get it from the repository's releases.

Acceptance:
- the button leads to the repository's GitHub releases page;
- on a browser that is not Android's, "Open in the app" copies the link and
  says to open it on an Android device.

## GST-03 Open the link in the application

- Status: done
- ADR: 0001, 0019

As a person with the application installed I open the link in it from the join
page, so that the application gets the project and the invitation in one step
and goes on to sign-in (MEM-01).

Acceptance:
- "Open in the app" is a real link that Chrome hands to the
  application;
- with the application already in a group, the link adds a group beside it.

## GST-04 Be told that an invitation cannot be used

- Status: partly — another account is told apart; a used or run-out
  invitation gets one message for both, which also covers a group that
  already exists
- ADR: 0015, 0019, 0025

As a person whose invitation has been used, has run out or was made for
another account I am told which of these it is, so that I ask for a new one
rather than retrying.

Acceptance:
- another account: both addresses are named and switching accounts is
  offered (MEM-02);
- used or run out: the screen says which, and to ask an administrator for a
  new link.
