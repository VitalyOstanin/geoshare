#!/usr/bin/env bash
# The tests of the page's own logic: whatever touches neither the document nor
# the map, such as positions, members, the group's roster, searches, the join
# link and the words of both languages. No browser and no network — the parts
# that talk to Firebase are covered by the rules tests instead.
#
#   tools/web-test.sh
#   tools/web-test.sh --coverage    the same run, with a coverage table at the end
#                                   and the check of web/tests/unreached.txt
set -euo pipefail

script="test"
if [[ "${1:-}" == "--coverage" ]]; then
    script="coverage"
fi

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

ensure_firebase_image

# The image is the one the rules tests use: a Node runtime is all this needs,
# and a second image for one command would be a second thing to keep current.
# Each test has a limit of its own; this one is for the process that outlives
# its tests, held up by a timer or a handle left open.
exec timeout 10m podman run --rm "${limit_args[@]}" \
    -v "${REPO_ROOT}:/src:z" -w /src/web \
    "${FIREBASE_IMAGE}" \
    npm run "${script}"
