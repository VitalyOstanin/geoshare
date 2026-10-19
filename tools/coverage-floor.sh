#!/usr/bin/env bash
# Fails when the Android unit tests reach fewer lines or branches than the
# floors. Reads the JaCoCo report the build has already made, so it needs no
# container and runs the same way in CI as after tools/coverage.sh.
#
#   tools/coverage-floor.sh
set -euo pipefail

here="$(dirname "${BASH_SOURCE[0]}")"

# Per cent of the Android lines and branches the unit tests reach, at the
# least. Just under what they reach now; raise them as the tests grow.
readonly ANDROID_LINE_FLOOR=27
readonly ANDROID_BRANCH_FLOOR=36

# The packages that hold the decisions, each held to a line floor of its own:
# the whole could otherwise grow on the screens while one of these shrinks.
readonly PACKAGE_LINE_FLOORS=(
    "service 21"
    "project 29"
    "collection 95"
    "map 9"
)

report="${here}/../android/app/build/reports/coverage/test/debug/report.xml"
if [[ ! -f "$report" ]]; then
    echo "no coverage report at ${report}" >&2
    exit 1
fi

# One line per package and one for the whole application ("all"): name, then
# the covered and the total of its LINE and BRANCH counters. A package's own
# counters come after its last source file, and the application's after the
# last package; the earlier ones are per class and method.
counted="$(
    grep -oE '<package name="[^"]*">|</sourcefile>|</package>|<counter type="(LINE|BRANCH)" missed="[0-9]+" covered="[0-9]+"/>' "$report" |
        awk '
            function emit(name) {
                print name, lc + 0, lc + lm, bc + 0, bc + bm
                lm = lc = bm = bc = 0
            }
            /^<package / { name = $0; sub(/.*\//, "", name); sub(/".*/, "", name); lm = lc = bm = bc = 0; next }
            /^<\/sourcefile>/ { lm = lc = bm = bc = 0; next }
            /^<\/package>/ { emit(name); next }
            /type="LINE"/ { split($0, n, "\""); lm = n[4]; lc = n[6]; next }
            /type="BRANCH"/ { split($0, n, "\""); bm = n[4]; bc = n[6]; next }
            END { emit("all") }
        '
)"

failed=0

# Prints the per cent and fails the check when it is under the floor.
check() {
    local what="$1" covered="$2" total="$3" floor="$4"
    local percent=0

    if ((total > 0)); then
        percent=$((covered * 100 / total))
    fi
    echo "${what}: ${covered} of ${total}, ${percent}%, floor ${floor}%"
    if ((percent < floor)); then
        echo "${what} coverage ${percent}% is under the floor of ${floor}%" >&2
        failed=1
    fi
}

read -r _ lines_covered lines_total branches_covered branches_total < <(grep '^all ' <<<"$counted")
if ((lines_total == 0)); then
    echo "no line counter in ${report}" >&2
    exit 1
fi
check "Android lines" "$lines_covered" "$lines_total" "$ANDROID_LINE_FLOOR"
check "Android branches" "$branches_covered" "$branches_total" "$ANDROID_BRANCH_FLOOR"

for entry in "${PACKAGE_LINE_FLOORS[@]}"; do
    read -r package floor <<<"$entry"
    row="$(grep "^${package} " <<<"$counted" || true)"
    if [[ -z "$row" ]]; then
        echo "no package ${package} in ${report}" >&2
        failed=1
        continue
    fi
    read -r _ covered total _ _ <<<"$row"
    check "Android ${package} lines" "$covered" "$total" "$floor"
done

exit "$failed"
