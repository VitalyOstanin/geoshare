#!/usr/bin/env bash
# The page from this working copy against stand-in sign-in and a stand-in
# database: the Hosting, Auth and Firestore emulators in one container, under
# a project id beginning with "demo-", so no credentials are involved and
# nothing here can reach a real project.
#
#   tools/page-stand.sh               # start, wait, print the address
#   tools/page-stand.sh --showcase    # fill with the showcase household
#   tools/page-stand.sh --stop        # stop it
#
# Signing in goes through the Auth emulator's own page, where "Add new
# account" makes one up on the spot. After --showcase, typing
# parent@geoshare.test there signs in as one of the household. Nothing is kept
# between runs.
#
# The page is mounted as an overlay: the container writes a firebase-config.js
# naming the emulators over the one in the working copy, the working copy
# keeps its own, and a change to the page still shows on the next reload.
#
# The ports are published on the loopback alone, and the browser calls the
# emulators directly, so each is a port of its own on this machine. They are
# not the emulators' usual ones, which other work here may hold:
#
#   STAND_WEB_PORT=5050 tools/page-stand.sh
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly STAND_NAME="${STAND_NAME:-geoshare-page-stand}"
readonly STAND_WEB_PORT="${STAND_WEB_PORT:-5000}"
readonly STAND_FIRESTORE_PORT="${STAND_FIRESTORE_PORT:-18080}"
readonly STAND_AUTH_PORT="${STAND_AUTH_PORT:-19099}"
readonly READY_TIMEOUT="${READY_TIMEOUT:-120}"
require_whole_number STAND_WEB_PORT
require_whole_number STAND_FIRESTORE_PORT
require_whole_number STAND_AUTH_PORT
require_whole_number READY_TIMEOUT

# Run in the stand's container, where the emulators listen; the proxy is left
# out for the reason run-services.sh gives.
showcase() {
    podman exec \
        -e "FIRESTORE_EMULATOR_HOST=127.0.0.1:${STAND_FIRESTORE_PORT}" \
        -e "AUTH_EMULATOR_HOST=127.0.0.1:${STAND_AUTH_PORT}" \
        -e HTTPS_PROXY= -e HTTP_PROXY= -e https_proxy= -e http_proxy= \
        "${STAND_NAME}" node /src/firebase/emulator/showcase-seed.mjs
}

stand_ready() {
    podman exec "${STAND_NAME}" node -e "
        Promise.all([
            fetch('http://127.0.0.1:${STAND_WEB_PORT}/'),
            fetch('http://127.0.0.1:${STAND_FIRESTORE_PORT}/'),
            fetch('http://127.0.0.1:${STAND_AUTH_PORT}/'),
        ]).then(() => process.exit(0), () => process.exit(1))
    " > /dev/null 2>&1
}

case "${1:-}" in
    --stop)
        podman rm -f "${STAND_NAME}" > /dev/null 2>&1 || true
        echo "==> stopped"
        exit 0
        ;;
    --showcase)
        if ! podman container exists "${STAND_NAME}"; then
            echo "error: no stand is running; start it with tools/page-stand.sh" >&2
            exit 1
        fi
        showcase
        exit 0
        ;;
    "") ;;
    *) echo "error: unknown argument '${1}'" >&2; exit 1 ;;
esac

ensure_firebase_image

if podman container exists "${STAND_NAME}"; then
    podman rm -f "${STAND_NAME}" > /dev/null
fi

echo "==> starting ${STAND_NAME}"

# The emulators read a copy of firebase.json, changed the way serve-web.sh
# changes it, and then some more: every emulator listens on every address in
# the container at the port published for it, and the page's
# Content-Security-Policy lets it reach the two the browser calls. The page's
# own firebase-config.js names the same ports; the emulators refuse to mix it
# with a real project id anyway (config-check.js).
# shellcheck disable=SC2016 # expanded by the container's shell
podman run -d --name "${STAND_NAME}" --http-proxy=false "${limit_args[@]}" \
    -p "127.0.0.1:${STAND_WEB_PORT}:${STAND_WEB_PORT}" \
    -p "127.0.0.1:${STAND_FIRESTORE_PORT}:${STAND_FIRESTORE_PORT}" \
    -p "127.0.0.1:${STAND_AUTH_PORT}:${STAND_AUTH_PORT}" \
    -e "WEB_PORT=${STAND_WEB_PORT}" \
    -e "FIRESTORE_PORT=${STAND_FIRESTORE_PORT}" \
    -e "AUTH_PORT=${STAND_AUTH_PORT}" \
    -v "${REPO_ROOT}/firebase.json:/src/firebase.json:z,ro" \
    -v "${REPO_ROOT}/firebase:/src/firebase:z,ro" \
    -v "${REPO_ROOT}/web/public:/site/web/public:O" -w /site \
    "${FIREBASE_IMAGE}" \
    bash -c 'node -e "
        const fs = require(\"fs\");
        const { WEB_PORT, FIRESTORE_PORT, AUTH_PORT } = process.env;
        const auth = \"http://127.0.0.1:\" + AUTH_PORT;
        const firestore = \"http://127.0.0.1:\" + FIRESTORE_PORT;
        const config = JSON.parse(fs.readFileSync(\"/src/firebase.json\", \"utf8\"));
        delete config.hosting.target;
        for (const rule of config.hosting.headers) {
            for (const header of rule.headers) {
                if (header.key === \"Content-Security-Policy\") {
                    header.value = header.value
                        .replace(\"connect-src \", \"connect-src \" + auth + \" \" + firestore + \" \")
                        .replace(\"frame-src \", \"frame-src \" + auth + \" \");
                }
            }
        }
        config.firestore.rules = \"/src/firebase/firestore.rules\";
        config.firestore.indexes = \"/src/firebase/firestore.indexes.json\";
        config.emulators = {
            hosting: { host: \"0.0.0.0\", port: Number(WEB_PORT) },
            firestore: { host: \"0.0.0.0\", port: Number(FIRESTORE_PORT) },
            auth: { host: \"0.0.0.0\", port: Number(AUTH_PORT) },
            singleProjectMode: true,
            ui: { enabled: false },
        };
        fs.writeFileSync(\"firebase.json\", JSON.stringify(config));
        fs.writeFileSync(\"web/public/firebase-config.js\", [
            \"// Written by tools/page-stand.sh for the emulators; gone with the stand.\",
            \"export const firebaseConfig = \" + JSON.stringify({
                projectId: \"demo-geoshare\",
                appId: \"1:1:web:1\",
                apiKey: \"demo-key\",
                authDomain: \"127.0.0.1\",
                emulators: {
                    auth: auth,
                    firestore: { host: \"127.0.0.1\", port: Number(FIRESTORE_PORT) },
                },
            }, null, 4) + \";\",
            \"\",
        ].join(\"\\n\"));
    " && exec firebase emulators:start --only hosting,firestore,auth --project demo-geoshare' \
    > /dev/null

echo "==> waiting for the emulators (up to ${READY_TIMEOUT}s)"
deadline=$((SECONDS + READY_TIMEOUT))
while (( SECONDS < deadline )); do
    if stand_ready; then
        echo "==> http://127.0.0.1:${STAND_WEB_PORT}/"
        echo "    database on ${STAND_FIRESTORE_PORT}, sign-in on ${STAND_AUTH_PORT}"
        exit 0
    fi
    if ! podman container exists "${STAND_NAME}"; then
        echo "error: the stand stopped while starting" >&2
        exit 1
    fi
    sleep 2
done

echo "error: the emulators did not come up in ${READY_TIMEOUT}s" >&2
podman logs --tail 20 "${STAND_NAME}" >&2 || true
exit 1
