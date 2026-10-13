#!/usr/bin/env bash
# How much of the code the tests reach: the Android unit tests through the
# JaCoCo agent the Android build carries, the page's tests through Node's own
# counter. Each side has a floor just under what it reaches now, so a change
# that leaves code untested fails the run; raise it as the tests grow. The
# page's floors and its list of scripts no test loads are in web/package.json
# and web/tests/unreached.txt.
#
#   tools/coverage.sh
#
# The Android report is android/app/build/reports/coverage/test/debug/
# (index.html, and report.xml for tools that read JaCoCo); the page's table is
# printed at the end of its run.
set -euo pipefail

here="$(dirname "${BASH_SOURCE[0]}")"

# Per cent of the Android lines the unit tests reach, at the least.
readonly ANDROID_LINE_FLOOR=26

"${here}/gradle.sh" -Pcoverage createDebugUnitTestCoverageReport

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

"${here}/web-test.sh" --coverage
