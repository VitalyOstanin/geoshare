#!/usr/bin/env bash
# Prints the version code of a release, worked out from appVersionName in
# android/gradle.properties.
#
# Android refuses to install a lower code over a higher one, so the code is
# worked out from the version rather than raised by hand: 0.1.0 is 100, 1.2.3
# is 10203. Each part past the first stays below a hundred for that to hold.
# The release workflow and a local release build both read it here, so the
# package checked before tagging carries the code that will be published.
set -euo pipefail

readonly PROPERTIES="$(dirname "${BASH_SOURCE[0]}")/../android/gradle.properties"

version=$(sed -n 's/^appVersionName=//p' "${PROPERTIES}")
if ! [[ "${version}" =~ ^([0-9]+)\.([0-9]{1,2})\.([0-9]{1,2})$ ]]; then
    echo "error: appVersionName ${version} is not major.minor.patch with minor and patch below 100" >&2
    exit 1
fi
# Base ten throughout: a part written "08" is not read as octal.
echo $(( 10#${BASH_REMATCH[1]} * 10000 + 10#${BASH_REMATCH[2]} * 100 + 10#${BASH_REMATCH[3]} ))
