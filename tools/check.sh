#!/usr/bin/env bash
# The checks a change actually needs, rather than all of them.
#
#   tools/check.sh              # run what the working tree asks for
#   tools/check.sh --list       # name them without running anything
#   tools/check.sh --all        # every check there is
#   tools/check.sh --since <ref># what changed since that commit or branch
#
# The tree is two halves that share nothing but a repository: Kotlin under
# android/, the page and the rules under web/ and firebase/. A change to one
# of them cannot break the other, and the checks of the other half are pure
# waiting -- a minute and a half of it for the page, about a minute for the
# Kotlin. So the change is read first and only the checks that can have
# anything to say are run.
#
# The names it prints are the names of the scripts beside it: test, style,
# lint, web_test, js_lint, rules. --list exists so that a caller which runs
# them somewhere else can be told which ones to run.
set -euo pipefail

readonly HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

list_only=
everything=
since=

while [[ $# -gt 0 ]]; do
    case "$1" in
        --list) list_only=yes ;;
        --all) everything=yes ;;
        --since)
            shift
            since="${1:?--since needs a commit or a branch}"
            ;;
        *)
            echo "check.sh: unknown argument: $1" >&2
            exit 2
            ;;
    esac
    shift
done

# What the change touched, in paths from the root of the repository.
#
# The working tree first, because that is what a check before a commit is
# about. A clean tree falls back to everything this branch has that the
# branch it follows does not: the other moment checks are run is after a run
# of commits, before they are sent anywhere.
changed_files() {
    if [[ -n "${since}" ]]; then
        git diff --name-only "${since}"
        return
    fi

    local working
    working="$(git diff --name-only HEAD; git ls-files --others --exclude-standard)"

    if [[ -n "${working}" ]]; then
        printf '%s\n' "${working}"
        return
    fi

    local upstream
    upstream="$(git rev-parse --abbrev-ref --symbolic-full-name '@{upstream}' 2>/dev/null || true)"

    if [[ -n "${upstream}" ]]; then
        git diff --name-only "${upstream}..HEAD"
    fi
}

wanted=()

want() {
    local check="$1"

    for already in ${wanted[@]+"${wanted[@]}"}; do
        [[ "${already}" == "${check}" ]] && return
    done

    wanted+=("${check}")
}

if [[ -n "${everything}" ]]; then
    want test
    want lint
    want web_test
    want js_lint
    want rules
else
    kotlin_only=yes

    while IFS= read -r file; do
        [[ -n "${file}" ]] || continue

        case "${file}" in
            # A Kotlin file asks for the tests and for the formatting. Whether
            # the Android lint is asked for as well is decided below, once
            # every file has been seen: it reads the manifest, the resources
            # and the versions, and none of those is a Kotlin file.
            android/*.kt)
                want test
                ;;
            # Anything else under android/ is a manifest, a resource, a version
            # or a Gradle file, and the Android lint has something to say about
            # all of them.
            android/*)
                want test
                kotlin_only=
                ;;
            # The page: its own tests, and the one linter the JavaScript here
            # is held to.
            web/*)
                want web_test
                want js_lint
                ;;
            # The rules and the tests that hold them to it. The linter covers
            # this directory too.
            firebase/*)
                want rules
                want js_lint
                ;;
            # Everything else -- the lists, the notes, the scripts themselves
            # -- is checked by nothing here.
            *) ;;
        esac
    done < <(changed_files)

    # Decided once every file has been seen, because one resource among a
    # hundred Kotlin files is enough to want the whole of the Android lint.
    for check in ${wanted[@]+"${wanted[@]}"}; do
        [[ "${check}" == "test" ]] || continue

        if [[ -n "${kotlin_only}" ]]; then
            want style
        else
            want lint
        fi

        break
    done
fi

if [[ ${#wanted[@]} -eq 0 ]]; then
    echo "check.sh: nothing the checks here can say anything about" >&2
    exit 0
fi

if [[ -n "${list_only}" ]]; then
    printf '%s\n' "${wanted[@]}"
    exit 0
fi

for check in "${wanted[@]}"; do
    echo "check.sh: ${check}"

    case "${check}" in
        test) "${HERE}/test.sh" ;;
        style) "${HERE}/lint.sh" --style-only ;;
        lint) "${HERE}/lint.sh" ;;
        web_test) "${HERE}/web-test.sh" ;;
        js_lint) "${HERE}/js-lint.sh" ;;
        rules) "${HERE}/rules.sh" ;;
    esac
done
