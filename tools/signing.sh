#!/usr/bin/env bash
# The fingerprints of the keys the application is signed with, as lines of
# "SHA1: …" and "SHA-256: …".
#
# Google sign-in checks the calling application against an OAuth client
# registered for a package name and a certificate fingerprint, so the debug key
# has to be listed in the Firebase project settings before the first sign-in.
#
# The debug key is generated inside the build container and kept in a named
# volume, so what this prints is the key that actually signs the builds — not
# whichever one happens to sit in ~/.android next to the source tree. Once the
# key is there it is read with keytool, which needs neither Gradle nor the
# network; only a machine that has never built asks Gradle, whose
# signingReport makes the key first.
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly KEYSTORE="/root/.android/debug.keystore"

ensure_sdk_image

if podman volume exists "${KEY_VOLUME}" &&
    podman run --rm -v "${KEY_VOLUME}:/root/.android:ro" "${SDK_IMAGE}" test -f "${KEYSTORE}"; then
    # The debug key's store and key passwords are the fixed "android" every
    # Android build uses; nothing secret is passed here.
    podman run --rm -v "${KEY_VOLUME}:/root/.android:ro" "${SDK_IMAGE}" \
        keytool -list -v -keystore "${KEYSTORE}" -storepass android -alias androiddebugkey |
        awk '$1 == "SHA1:" { print "SHA1: " $2 } $1 == "SHA256:" { print "SHA-256: " $2 }'
    exit 0
fi

exec "$(dirname "${BASH_SOURCE[0]}")/gradle.sh" signingReport "$@"
