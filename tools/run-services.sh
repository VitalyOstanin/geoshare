#!/usr/bin/env bash
# Starts the stand-in database and sign-in the emulator talks to.
#
# Both are Firebase emulators under a project id starting with "demo-", so
# nothing here reaches the real project and no credentials are involved. They
# listen on the host, which is what the Android emulator's 10.0.2.2 resolves
# to, and the rules they enforce are the ones in the repository.
#
#   tools/run-services.sh            # start, wait, then fill with stand-in data
#   tools/run-services.sh --stop     # stop them
#   tools/run-services.sh --seed     # fill again, leaving them running
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly READY_TIMEOUT="${READY_TIMEOUT:-120}"
readonly FIRESTORE_PORT="${FIRESTORE_PORT:-8080}"
readonly AUTH_PORT="${AUTH_PORT:-9099}"

seed() {
    podman run --rm --network host \
        -v "${REPO_ROOT}:/src:z" -w /src \
        -e "FIRESTORE_EMULATOR_HOST=127.0.0.1:${FIRESTORE_PORT}" \
        -e "AUTH_EMULATOR_HOST=127.0.0.1:${AUTH_PORT}" \
        "${FIREBASE_IMAGE}" \
        node firebase/emulator/seed.mjs
}

case "${1:-}" in
    --stop)
        podman rm -f "${SERVICES_NAME}" > /dev/null 2>&1 || true
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

if podman container exists "${SERVICES_NAME}"; then
    podman rm -f "${SERVICES_NAME}" > /dev/null
fi

echo "==> starting ${SERVICES_NAME}"
# The host's network on purpose: the Android emulator reaches its host at
# 10.0.2.2, and that is this machine's loopback.
podman run -d --rm --name "${SERVICES_NAME}" --network host \
    -v "${REPO_ROOT}:/src:z" -w /src \
    "${FIREBASE_IMAGE}" \
    firebase emulators:start --only firestore,auth --project demo-geoshare > /dev/null

echo "==> waiting for the emulators (up to ${READY_TIMEOUT}s)"
deadline=$((SECONDS + READY_TIMEOUT))
while (( SECONDS < deadline )); do
    if curl -s -o /dev/null "http://127.0.0.1:${FIRESTORE_PORT}/" \
        && curl -s -o /dev/null "http://127.0.0.1:${AUTH_PORT}/"; then
        echo "==> database on ${FIRESTORE_PORT}, sign-in on ${AUTH_PORT}"
        seed
        exit 0
    fi
    sleep 2
done

echo "error: the emulators did not come up in ${READY_TIMEOUT}s" >&2
podman logs --tail 20 "${SERVICES_NAME}" >&2 || true
exit 1
