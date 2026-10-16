# Two steps in the Google console

The setup script cannot do these two; the project owner does them once, signed
in with the same Google account that holds the project. The script prints both
links with the project filled in. [Russian version](console-steps.ru.md).

**Before you start:** from 18 October 2026 Google lets an account into the
Firebase and Google Cloud consoles only with 2-Step Verification turned on.
Turn it on in your Google account's security settings first, or the consoles
will stop at a banner asking for it.

**1. Turn on sign-in with Google.**
Open `https://console.firebase.google.com/project/<project-id>/authentication/providers`.
Press Get started if it is there, then Google, Enable. Type the group's name as
the public-facing name; members see it when they sign in. Pick your address as
the support address. Save.

The console may then say "Can't enable Google sign-in for some apps" with
"An identical OAuth client already exists" next to the Android app. That is
expected and nothing needs fixing: every group's app is the same build with the
same signing key, which only one project in the world can hold as an OAuth
client, and the app signs in through the browser instead
([ADR-0002](ADR/0002-sign-in-through-the-browser.md)). Close the dialog;
sign-in with Google stays on.

**2. Let the page sign in from its own address.**
Open `https://console.cloud.google.com/auth/clients?project=<project-id>`.
Open "Web client (auto created by Google Service)". Under Authorized redirect
URIs press Add URI and paste `https://<site>.web.app/__/auth/handler`. Save.

**Then** tell whoever runs the script that both steps are done.

**On the first sign-in** to the group's page or the app, Google asks whether
`<site>.web.app` (or `<project-id>.firebaseapp.com` from the app) may see your
email address and profile. That address is the group's own, not somebody else's;
allow it. Members see the same screen once, the first time they sign in.

**Leave alone:** the Blaze plan, billing, and Firebase AI Logic. The project
works on the free plan, and with AI Logic on, anybody could spend its quota.
