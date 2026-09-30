# Two steps in the Google console

The setup script cannot do these two; the project owner does them once, signed
in with the same Google account that holds the project. The script prints both
links with the project filled in. [Russian version](console-steps.ru.md).

**1. Turn on sign-in with Google.**
Open `https://console.firebase.google.com/project/<project-id>/authentication/providers`.
Press Get started if it is there, then Google, Enable. Pick your address as the
support address. Save.

**2. Let the page sign in from its own address.**
Open `https://console.cloud.google.com/auth/clients?project=<project-id>`.
Open "Web client (auto created by Google Service)". Under Authorized redirect
URIs press Add URI and paste `https://<site>.web.app/__/auth/handler`. Save.

**Then** tell whoever runs the script that both steps are done.

**Leave alone:** the Blaze plan, billing, and Firebase AI Logic. The project
works on the free plan, and with AI Logic on, anybody could spend its quota.
