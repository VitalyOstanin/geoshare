#!/usr/bin/env bash
# Runs Gradle tasks inside the container built from Containerfile.sdk.
#
# Every other Gradle script here goes through this one, so the cache volume and
# the signing key volume are stated once.
#
#   tools/gradle.sh assembleDebug
#   tools/gradle.sh testDebugUnitTest --info
set -euo pipefail

# shellcheck source=lib.sh
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

# The network the build runs in. The host's, by default: a proxy on the host
# loopback is not reachable from the container's own network.
#
# GRADLE_IN_POD runs it in the try-out emulator's pod instead, which is how
# an instrumented run reaches the emulator. The Android plugin's test engine
# calls adb on its default port with an environment of its own, so no
# variable pointing elsewhere reaches it; inside the pod that port is the
# emulator's own adb server, and the devices plugged into the machine are
# not in sight at all. The pod has no proxy, so a build there has to find
# everything in the cache already.
network_args=(--network host "${proxy_run_args[@]}")
if [[ -n "${GRADLE_IN_POD:-}" ]]; then
    network_args=(--pod "${POD_NAME}" --http-proxy=false)
fi

ensure_sdk_image

# Bounded like the other scripts that start a container: a build that hangs
# outside the test task, in the compiler, R8 or a download, has nothing else
# to stop it. A first build on a fresh cache takes a good part of this.
exec timeout "${GRADLE_TIMEOUT:-60m}" \
    podman run --rm "${network_args[@]}" "${limit_args[@]}" "${env_args[@]}" \
    -v "${REPO_ROOT}:/src:z" -v "${CACHE_VOLUME}:/gradle" \
    -v "${KEY_VOLUME}:/root/.android" -w /src/android \
    "${SDK_IMAGE}" \
    ./gradlew --no-daemon "$@"
