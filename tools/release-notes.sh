#!/usr/bin/env bash
# Prints the section of one version from CHANGELOG.md: what the release says
# on the releases page.
#
# Refuses a version without a section, one whose heading carries no date yet
# (a release whose notes still say "Unreleased" was cut before they were
# finished), and one whose section has no "### Project owners": the project
# owner of a group reads there whether the release needs
# `tools/project.sh update`, and in which order with the devices.
#
#   tools/release-notes.sh 0.1.0 > notes.md
set -euo pipefail

version="${1:?usage: tools/release-notes.sh <version>}"
changelog="$(dirname "${BASH_SOURCE[0]}")/../CHANGELOG.md"

heading=$(grep -m1 -F "## [${version}]" "${changelog}" || true)

if [[ -z "${heading}" ]]; then
    echo "release-notes: CHANGELOG.md has no section for ${version}" >&2
    exit 1
fi

if ! [[ "${heading}" =~ ^"## [${version}] - "[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
    echo "release-notes: the section of ${version} has no release date: ${heading}" >&2
    exit 1
fi

notes=$(awk -v heading="${heading}" '
    $0 == heading { inside = 1; next }
    inside && /^## / { exit }
    # The link definitions at the end of the file belong to no section.
    inside && /^\[[^]]+\]: / { exit }
    inside { print }
' "${changelog}")

if ! grep -qx '### Project owners' <<<"${notes}"; then
    echo "release-notes: the section of ${version} has no '### Project owners'" \
        "subsection saying whether the release needs tools/project.sh update" >&2
    exit 1
fi

printf '%s\n' "${notes}"
