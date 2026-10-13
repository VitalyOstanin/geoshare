#!/usr/bin/env bash
# The page from this working copy, served the way Hosting serves it: the
# rewrites and the headers in firebase.json, the Content-Security-Policy among
# them, so that what the policy refuses is refused here before a deploy rather
# than on the site after one.
#
#   tools/serve-web.sh              # http://127.0.0.1:5000/ until Ctrl-C
#   WEB_PORT=5050 tools/serve-web.sh
#
# The page has no build step: a change shows on the next reload. It talks to
# the project web/public/firebase-config.js names, which tools/web-config.sh
# writes; without that file the page has no project to sign in to. The setup
# page asks for setup-config.json, which only tools/setup-site.sh writes, for
# the setup site: here that request fails, as it would on the group's own site.
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly WEB_PORT="${WEB_PORT:-5000}"
require_whole_number WEB_PORT

ensure_firebase_image

if [[ ! -f "${REPO_ROOT}/web/public/firebase-config.js" ]]; then
    echo "serve-web.sh: no web/public/firebase-config.js; tools/web-config.sh writes it" >&2
fi

echo "serve-web.sh: http://127.0.0.1:${WEB_PORT}/" >&2

# The emulator reads a copy of firebase.json with two things changed. The
# hosting target goes: it is resolved through .firebaserc, which names a real
# project and is not in the repository. And the emulator listens on every
# address inside the container, since the container's own loopback is not what
# is published. The copy is written beside a read-only mount of the page, in
# the container, and goes with it.
#
# Published on the loopback alone: the page is for this machine, not for the
# network it is on. The proxy is left out for the reason js-lint.sh gives.
# shellcheck disable=SC2016 # expanded by the container's shell
exec podman run --rm -it --http-proxy=false "${limit_args[@]}" \
    -p "127.0.0.1:${WEB_PORT}:5000" \
    -v "${REPO_ROOT}/firebase.json:/src/firebase.json:z,ro" \
    -v "${REPO_ROOT}/web/public:/site/web/public:z,ro" -w /site \
    "${FIREBASE_IMAGE}" \
    bash -c 'node -e "
        const fs = require(\"fs\");
        const config = JSON.parse(fs.readFileSync(\"/src/firebase.json\", \"utf8\"));
        delete config.hosting.target;
        config.emulators = { hosting: { host: \"0.0.0.0\", port: 5000 } };
        fs.writeFileSync(\"firebase.json\", JSON.stringify(config));
    " && exec firebase emulators:start --only hosting --project demo-geoshare'
