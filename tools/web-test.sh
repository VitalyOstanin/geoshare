#!/usr/bin/env bash
# The tests of the page's own logic: whatever touches neither the document nor
# the map, such as positions, members, the group's roster, searches, the join
# link and the words of both languages. No browser and no network — the parts
# that talk to Firebase are covered by the rules tests instead.
#
#   tools/web-test.sh
#   tools/web-test.sh --coverage    the same run, with a coverage table at the end
#                                   and the check of web/tests/unreached.txt
#   tools/web-test.sh --watch       reruns the tests that a saved file affects, until
#                                   interrupted; not together with --coverage
set -euo pipefail

script="test"
watch=false
for arg in "$@"; do
    case "${arg}" in
        --coverage) script="coverage" ;;
        --watch) watch=true ;;
        *) echo "unknown argument: ${arg}" >&2; exit 2 ;;
    esac
done
if [[ "${watch}" == true && "${script}" == "coverage" ]]; then
    echo "--watch and --coverage cannot be combined: a coverage table needs a finished run" >&2
    exit 2
fi

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

ensure_firebase_image

# The image is the one the rules tests use: a Node runtime is all this needs,
# and a second image for one command would be a second thing to keep current.
# Each test has a limit of its own; this one is for the process that outlives
# its tests, held up by a timer or a handle left open.
if [[ "${watch}" == true ]]; then
    # Same flags as `npm test`, plus --watch. A terminal is needed for Ctrl-C to
    # reach node, and a run that is meant to wait for edits has no time limit.
    # The glob is left to node, which expands it itself.
    exec podman run --rm -it "${limit_args[@]}" \
        -v "${REPO_ROOT}:/src:z" -w /src/web \
        "${FIREBASE_IMAGE}" \
        node --test --watch --test-concurrency=4 --test-timeout=30000 'tests/*.test.mjs'
fi

exec timeout 10m podman run --rm "${limit_args[@]}" \
    -v "${REPO_ROOT}:/src:z" -w /src/web \
    "${FIREBASE_IMAGE}" \
    npm run "${script}"
