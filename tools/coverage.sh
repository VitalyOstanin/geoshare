#!/usr/bin/env bash
# How much of the code the tests reach: the Android unit tests through the
# JaCoCo agent the Android build carries, the page's tests through Node's own
# counter. Nothing is enforced yet; the numbers are for reading.
#
#   tools/coverage.sh
#
# The Android report is android/app/build/reports/coverage/test/debug/
# (index.html, and report.xml for tools that read JaCoCo); the page's table is
# printed at the end of its run.
set -euo pipefail

here="$(dirname "${BASH_SOURCE[0]}")"

"${here}/gradle.sh" -Pcoverage createDebugUnitTestCoverageReport
"${here}/web-test.sh" --coverage
