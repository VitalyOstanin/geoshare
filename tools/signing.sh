#!/usr/bin/env bash
# The fingerprints of the keys the application is signed with.
#
# Google sign-in checks the calling application against an OAuth client
# registered for a package name and a certificate fingerprint, so the debug key
# has to be listed in the Firebase project settings before the first sign-in.
#
# The debug key is generated inside the build container and kept in a named
# volume, so what this prints is the key that actually signs the builds — not
# whichever one happens to sit in ~/.android next to the source tree.
set -euo pipefail

exec "$(dirname "${BASH_SOURCE[0]}")/gradle.sh" signingReport "$@"
