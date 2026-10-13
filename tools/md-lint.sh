#!/usr/bin/env bash
# The check of the documents: the README, docs/, the decisions and the stories,
# held to the rules in .markdownlint-cli2.jsonc.
#
#   tools/md-lint.sh            # check
#   tools/md-lint.sh --format   # rewrite what the linter can fix
#
# Fetched by npx inside the container, like the linter of the JavaScript.
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

ensure_firebase_image

args=()

if [[ "${1:-}" == "--format" ]]; then
    shift
    args+=(--fix)
fi

# The proxy is left out for the reason js-lint.sh gives.
exec podman run --rm --http-proxy=false "${limit_args[@]}" \
    -v "${REPO_ROOT}:/src:z" -v "${NPM_VOLUME:-geoshare-npm}:/npm" -w /src \
    "${FIREBASE_IMAGE}" \
    npx --yes "markdownlint-cli2@${MARKDOWNLINT_CLI2_VERSION}" "${args[@]}" "$@"
