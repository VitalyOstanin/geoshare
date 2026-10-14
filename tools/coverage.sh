#!/usr/bin/env bash
# How much of the code the tests reach: the Android unit tests through the
# JaCoCo agent the Android build carries, the page's tests through Node's own
# counter. Each side has a floor just under what it reaches now, so a change
# that leaves code untested fails the run; raise it as the tests grow. The
# Android floor is in tools/coverage-floor.sh; the page's floors and its list
# of scripts no test loads are in web/package.json and web/tests/unreached.txt.
# CI checks the same floors.
#
#   tools/coverage.sh
#
# The Android report is android/app/build/reports/coverage/test/debug/
# (index.html, and report.xml for tools that read JaCoCo); the page's table is
# printed at the end of its run.
set -euo pipefail

here="$(dirname "${BASH_SOURCE[0]}")"

"${here}/gradle.sh" -Pcoverage createDebugUnitTestCoverageReport
"${here}/coverage-floor.sh"

"${here}/web-test.sh" --coverage
