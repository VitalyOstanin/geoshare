#!/usr/bin/env bash
# What the reads of the clients cost, measured against the Firestore emulator.
#
#   tools/read-cost.sh
#
# No project of our own is involved: a project id starting with "demo-" keeps
# both the emulator and the client library away from the real service, so this
# runs before the project in the console exists and without credentials.
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly NPM_VOLUME="${NPM_VOLUME:-geoshare-npm}"

ensure_firebase_image

# On the container's own network on purpose, unlike the Android build: the
# emulator and the tests talk over the container's loopback, and the port it
# listens on is a popular one — sharing the host's network means a run fails
# whenever anything else on the machine holds 8080.
# The proxy is left out on purpose, and has to be said out loud: podman hands
# the host's HTTP_PROXY and HTTPS_PROXY to every container unless told not to,
# and a proxy listening on the host's loopback is nothing at all from inside a
# container on its own network. npm then fails to reach the registry with a
# refused connection to an address it was never meant to use.
exec podman run --rm --http-proxy=false "${limit_args[@]}" \
    -e COST_SCRIPT="${COST_SCRIPT:-read-cost}" \
    -v "${REPO_ROOT}:/src:z" -v "${NPM_VOLUME}:/npm" -w /src \
    "${FIREBASE_IMAGE}" \
    bash -c 'npm --prefix firebase/tests ci \
        && firebase emulators:exec --only firestore --project demo-geoshare \
            "node firebase/tests/cost/${COST_SCRIPT:-read-cost}.mjs"'
