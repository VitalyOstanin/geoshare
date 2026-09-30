#!/usr/bin/env bash
# Starts the stand-in database and sign-in the emulator talks to.
#
# Both are Firebase emulators started under a project id beginning with
# "demo-", so no credentials are involved and nothing here can reach the real
# project. They listen on the loopback of the pod named in lib.sh, which is
# what the Android emulator's 10.0.2.2 resolves to once it runs in the same
# pod, and the rules they enforce are the ones in the repository.
#
# Nothing is published to the machine: the database's port is a popular one,
# and a run must not fail because something else on the machine already holds
# it.
#
# The database inside them is keyed by the project the phone asks about, which
# is demo-geoshare on both sides: redirecting the library to the emulators
# changes the host it calls, not the project, so the debug build's stand-in
# settings and this project id have to name the same thing, and the seed
# writes under it too.
#
#   tools/run-services.sh            # start, wait, then fill with stand-in data
#   tools/run-services.sh --stop     # stop them
#   tools/run-services.sh --seed     # fill again, leaving them running
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly READY_TIMEOUT="${READY_TIMEOUT:-120}"
readonly FIRESTORE_PORT="${FIRESTORE_PORT:-8080}"
readonly AUTH_PORT="${AUTH_PORT:-9099}"

# In the pod as well, and for the same reason as the services themselves: the
# addresses below are the pod's loopback, and from anywhere else they are
# nothing. The proxy is left out on purpose — podman hands the host's
# HTTPS_PROXY to every container unless told not to, and a proxy on the host
# loopback is unreachable from inside a pod.
seed() {
    podman run --rm --pod "${POD_NAME}" --http-proxy=false \
        -v "${REPO_ROOT}:/src:z" -w /src \
        -e "FIRESTORE_EMULATOR_HOST=127.0.0.1:${FIRESTORE_PORT}" \
        -e "AUTH_EMULATOR_HOST=127.0.0.1:${AUTH_PORT}" \
        "${FIREBASE_IMAGE}" \
        node firebase/emulator/seed.mjs
}

# Asked from inside the container, because that is the only place the ports
# exist. Node answers it: the image carries no curl, and a refused connection
# is a rejected promise either way.
services_ready() {
    podman exec "${SERVICES_NAME}" node -e "
        Promise.all([
            fetch('http://127.0.0.1:${FIRESTORE_PORT}/'),
            fetch('http://127.0.0.1:${AUTH_PORT}/'),
        ]).then(() => process.exit(0), () => process.exit(1))
    " > /dev/null 2>&1
}

case "${1:-}" in
    --stop)
        podman rm -f "${SERVICES_NAME}" > /dev/null 2>&1 || true
        remove_pod_if_idle
        echo "==> stopped"
        exit 0
        ;;
    --seed)
        ensure_firebase_image
        seed
        exit 0
        ;;
    "") ;;
    *) echo "error: unknown argument '${1}'" >&2; exit 1 ;;
esac

ensure_firebase_image
ensure_pod

if podman container exists "${SERVICES_NAME}"; then
    podman rm -f "${SERVICES_NAME}" > /dev/null
fi

echo "==> starting ${SERVICES_NAME}"
podman run -d --rm --name "${SERVICES_NAME}" --pod "${POD_NAME}" --http-proxy=false \
    -v "${REPO_ROOT}:/src:z" -w /src \
    "${FIREBASE_IMAGE}" \
    firebase emulators:start --only firestore,auth --project demo-geoshare > /dev/null

echo "==> waiting for the emulators (up to ${READY_TIMEOUT}s)"
deadline=$((SECONDS + READY_TIMEOUT))
while (( SECONDS < deadline )); do
    if services_ready; then
        echo "==> database on ${FIRESTORE_PORT}, sign-in on ${AUTH_PORT}, inside pod ${POD_NAME}"
        seed
        exit 0
    fi
    sleep 2
done

echo "error: the emulators did not come up in ${READY_TIMEOUT}s" >&2
podman logs --tail 20 "${SERVICES_NAME}" >&2 || true
exit 1
