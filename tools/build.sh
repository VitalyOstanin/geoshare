#!/usr/bin/env bash
# Assembles the application.
#
#   tools/build.sh              debug APK
#   VARIANT=release tools/build.sh
#
# The variant comes from the environment rather than from an argument because
# the queue that runs this passes no arguments through.
set -euo pipefail

readonly VARIANT="${VARIANT:-debug}"

case "${VARIANT}" in
    debug) task=assembleDebug ;;
    release) task=assembleRelease ;;
    *) echo "error: unknown VARIANT '${VARIANT}', expected debug or release" >&2; exit 1 ;;
esac

tools_dir="$(dirname "${BASH_SOURCE[0]}")"
args=("${task}")
if [[ "${VARIANT}" == release ]]; then
    # The code the release workflow publishes with, not the default of 1.
    args+=("-PappVersionCode=$("${tools_dir}/version-code.sh")")
fi

exec "${tools_dir}/gradle.sh" "${args[@]}"
