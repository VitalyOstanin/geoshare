#!/usr/bin/env bash
# The checks of the Kotlin half: the formatting and the Android lint.
#
#   tools/lint.sh            # check
#   tools/lint.sh --format   # rewrite what the formatter can fix
#
# The formatting pass rewrites files in the tree, so it only makes sense
# where the tree lives. The queue passes no arguments through, which is why
# it is a flag here rather than an action of its own.
#
# Report: android/app/build/reports/lint-results-debug.html
set -euo pipefail

if [[ "${1:-}" == "--format" ]]; then
    shift
    set -- ktlintFormat "$@"
else
    set -- ktlintCheck lintDebug "$@"
fi

exec "$(dirname "${BASH_SOURCE[0]}")/gradle.sh" "$@"
