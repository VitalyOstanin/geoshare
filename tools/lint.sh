#!/usr/bin/env bash
# The checks of the Kotlin half: the formatting, the length of functions
# (detekt, held to the 60 lines the page's JavaScript is) and the Android lint.
#
#   tools/lint.sh              # check
#   tools/lint.sh --format     # rewrite what the formatter can fix
#   tools/lint.sh --style-only # the formatting and the length of functions,
#                              # without the Android lint
#
# The formatting pass rewrites files in the tree, so it only makes sense
# where the tree lives. The queue passes no arguments through, which is why
# it is a flag here rather than an action of its own.
#
# --style-only is for a change that touched nothing but Kotlin. The Android
# lint reads the manifest, the resources and the versions, and re-reading all
# of it to be told again what it said about an untouched manifest is most of
# the wait; the formatting and the length of functions are the parts that
# can have anything to say about a line of Kotlin that changed. See tools/check.sh, which picks between
# the two by what the change touched.
#
# Report: android/app/build/reports/lint-results-debug.html
set -euo pipefail

case "${1:-}" in
    --format)
        shift
        set -- ktlintFormat "$@"
        ;;
    --style-only)
        shift
        set -- ktlintCheck detekt "$@"
        ;;
    *)
        set -- ktlintCheck detekt lintDebug "$@"
        ;;
esac

exec "$(dirname "${BASH_SOURCE[0]}")/gradle.sh" "$@"
