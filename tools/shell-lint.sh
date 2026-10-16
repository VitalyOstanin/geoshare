#!/usr/bin/env bash
# ShellCheck over every shell script in the repository, from its own image so
# that the version is the one in versions.env rather than whatever the
# machine has.
#
#   tools/shell-lint.sh
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

cd "${REPO_ROOT}"
mapfile -t scripts < <(git ls-files '*.sh')

# -x follows the `source` lines, so lib.sh is read with every script that
# uses it.
exec podman run --rm --network none -v "${REPO_ROOT}:/mnt:z,ro" -w /mnt \
    "docker.io/koalaman/shellcheck:v${SHELLCHECK_VERSION}@${SHELLCHECK_DIGEST}" -x "${scripts[@]}"
