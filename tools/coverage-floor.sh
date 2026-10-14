#!/usr/bin/env bash
# Fails when the Android unit tests reach fewer lines than the floor. Reads
# the JaCoCo report the build has already made, so it needs no container and
# runs the same way in CI as after tools/coverage.sh.
#
#   tools/coverage-floor.sh
set -euo pipefail

here="$(dirname "${BASH_SOURCE[0]}")"

# Per cent of the Android lines the unit tests reach, at the least. Just under
# what they reach now; raise it as the tests grow.
readonly ANDROID_LINE_FLOOR=26

# The last LINE counter in the report is the one for the whole application;
# the earlier ones are per package, class and method.
report="${here}/../android/app/build/reports/coverage/test/debug/report.xml"
counter="$(grep -o '<counter type="LINE" missed="[0-9]*" covered="[0-9]*"/>' "$report" | tail -n 1 || true)"
if ! [[ "$counter" =~ missed=\"([0-9]+)\"\ covered=\"([0-9]+)\" ]]; then
    echo "no line counter in ${report}" >&2
    exit 1
fi
missed="${BASH_REMATCH[1]}"
covered="${BASH_REMATCH[2]}"
percent=$((covered * 100 / (covered + missed)))
echo "Android: ${covered} of $((covered + missed)) lines, ${percent}%, floor ${ANDROID_LINE_FLOOR}%"
if ((percent < ANDROID_LINE_FLOOR)); then
    echo "Android line coverage ${percent}% is under the floor of ${ANDROID_LINE_FLOOR}%" >&2
    exit 1
fi
