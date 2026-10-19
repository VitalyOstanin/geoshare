#!/usr/bin/env bash
# The check of the JavaScript half: one style for the page and for the scripts
# that seed and test the emulators.
#
#   tools/js-lint.sh            # check
#   tools/js-lint.sh --format   # rewrite what the linter can fix
#
# Fetched by npx inside the container rather than installed into the tree: the
# page has no build step, and a node_modules beside it would be the only one in
# the repository.
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

ensure_firebase_image

args=()

if [[ "${1:-}" == "--format" ]]; then
    shift
    args+=(--fix)
fi

# The proxy is left out on purpose: podman hands the host's to every container
# unless told not to, and a proxy on the host's loopback is nothing at all from
# inside a container on its own network.
exec podman run --rm --http-proxy=false "${limit_args[@]}" \
    -v "${REPO_ROOT}:/src:z" -v "${NPM_VOLUME:-geoshare-npm}:/npm" -w /src \
    "${FIREBASE_IMAGE}" \
    npx --yes -p "eslint@${ESLINT_VERSION}" -p "@eslint/js@${ESLINT_JS_VERSION}" \
    -p "@stylistic/eslint-plugin@${STYLISTIC_VERSION}" \
    -p "eslint-plugin-import-x@${IMPORT_X_VERSION}" \
    eslint web firebase "${args[@]}" "$@"
