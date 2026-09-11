#!/usr/bin/env bash
# Runs Gradle tasks inside the container built from Containerfile.sdk.
#
# Every other Gradle script here goes through this one, so the cache volume and
# the signing key volume are stated once.
#
#   tools/gradle.sh assembleDebug
#   tools/gradle.sh testDebugUnitTest --info
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

# A named volume keeps the Gradle cache between runs; without it every build
# re-downloads the whole dependency graph.
readonly CACHE_VOLUME="${CACHE_VOLUME:-geoshare-gradle}"
# The debug signing key lives in ~/.android and is generated on first use. In a
# --rm container that means a new key every build, and the next install fails
# with INSTALL_FAILED_UPDATE_INCOMPATIBLE against the copy already on the
# device. A volume keeps one key.
readonly KEY_VOLUME="${KEY_VOLUME:-geoshare-android-home}"

env_args=()
# Passed through so an instrumented run installs on the device it was aimed at,
# and not on everything adb can reach.
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    env_args+=(-e "ANDROID_SERIAL=${ANDROID_SERIAL}")
fi

ensure_sdk_image

# --network host: a proxy on the host loopback is not reachable from the
# container's own network, and it is also how the container reaches an
# emulator running on the host.
exec podman run --rm --network host "${proxy_run_args[@]}" "${limit_args[@]}" "${env_args[@]}" \
    -v "${REPO_ROOT}:/src:z" -v "${CACHE_VOLUME}:/gradle" \
    -v "${KEY_VOLUME}:/root/.android" -w /src/android \
    "${SDK_IMAGE}" \
    ./gradlew --no-daemon "$@"
