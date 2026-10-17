#!/usr/bin/env bash
# Prints the section of one version from CHANGELOG.md and, under it, the same
# section from CHANGELOG.ru.md: what the release says on the releases page.
# The Russian one sits in a <details lang="ru"> block, which GitHub shows
# folded and the application reads to show the notes in its own language.
#
# Refuses a version without a section in either file, one whose heading
# carries no date yet (a release whose notes still say "Unreleased" was cut
# before they were finished), and one whose section has no "### Project
# owners" ("### Владельцам проектов"): the project owner of a group reads
# there whether the release needs `tools/project.sh update`, and in which
# order with the devices.
#
#   tools/release-notes.sh 0.1.0 > notes.md
set -euo pipefail

version="${1:?usage: tools/release-notes.sh <version>}"
root="$(dirname "${BASH_SOURCE[0]}")/.."

# section FILE OWNERS-HEADING: the section of ${version} in FILE, checked.
section() {
    local changelog="$1" owners="$2" name heading notes
    name=$(basename "${changelog}")

    heading=$(grep -m1 -F "## [${version}]" "${changelog}" || true)

    if [[ -z "${heading}" ]]; then
        echo "release-notes: ${name} has no section for ${version}" >&2
        exit 1
    fi

    if ! [[ "${heading}" =~ ^"## [${version}] - "[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
        echo "release-notes: the section of ${version} in ${name} has no release date: ${heading}" >&2
        exit 1
    fi

    notes=$(awk -v heading="${heading}" '
        $0 == heading { inside = 1; next }
        inside && /^## / { exit }
        # The link definitions at the end of the file belong to no section.
        inside && /^\[[^]]+\]: / { exit }
        inside { print }
    ' "${changelog}")

    if ! grep -qxF "${owners}" <<<"${notes}"; then
        echo "release-notes: the section of ${version} in ${name} has no '${owners}'" \
            "subsection saying whether the release needs tools/project.sh update" >&2
        exit 1
    fi

    # Without the blank lines around it, so that the block below is laid out
    # the same whatever the section begins and ends with.
    awk '
        NF { started = 1; while (blank > 0) { print ""; blank-- }; print; next }
        started { blank++ }
    ' <<<"${notes}"
}

english=$(section "${root}/CHANGELOG.md" '### Project owners')
russian=$(section "${root}/CHANGELOG.ru.md" '### Владельцам проектов')

printf '%s\n\n<details lang="ru">\n<summary>По-русски</summary>\n\n%s\n\n</details>\n' \
    "${english}" "${russian}"
