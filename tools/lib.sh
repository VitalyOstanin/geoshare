# shellcheck shell=bash
# Shared by the scripts in this directory: the repository root, the pinned
# versions, the proxy plumbing and building the image the first time it is
# needed.
#
# Sourced, never executed; each script keeps its own `set -euo pipefail`.

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly REPO_ROOT

# shellcheck source=versions.env
source "${REPO_ROOT}/tools/versions.env"

# The tag carries the version the image was built for, so a bump to
# versions.env asks for a new image instead of silently reusing the old one.
#
# Overridable, because another project on the same machine may already carry a
# compatible image — the same platform and build tools — and building a second
# copy of several gigabytes to run one task is not worth it:
#
#   SDK_IMAGE=localhost/other-sdk:37 tools/lint.sh --format
readonly SDK_IMAGE="${SDK_IMAGE:-localhost/geoshare-sdk:${ANDROID_COMPILE_SDK}-jdk${JDK_VERSION}}"

# The image the rules tests run in. Overridable for the same reason as the one
# above.
readonly FIREBASE_IMAGE="${FIREBASE_IMAGE:-localhost/geoshare-firebase:${FIREBASE_TOOLS_VERSION}-jre${FIREBASE_JRE_VERSION}}"

# Which system image the emulator runs: google_apis carries Play services,
# default does not, and is what shows how the application behaves on a device
# sold without them.
#
#   EMULATOR_VARIANT=default tools/run-emulator.sh
readonly EMULATOR_VARIANT="${EMULATOR_VARIANT:-google_apis}"

# The SDK image plus a system image, which is what an emulator needs and a
# build does not. The image without Play services carries its variant in the
# tag, so the two live side by side and switching does not rebuild either.
if [[ "${EMULATOR_VARIANT}" == "google_apis" ]]; then
    readonly EMULATOR_IMAGE="${EMULATOR_IMAGE:-localhost/geoshare-emulator:${ANDROID_EMULATOR_API}}"
else
    readonly EMULATOR_IMAGE="${EMULATOR_IMAGE:-localhost/geoshare-emulator:${ANDROID_EMULATOR_API}-${EMULATOR_VARIANT}}"
fi

# The containers the two halves of a try-out run in: the emulator itself, and
# the stand-in database and sign-in it talks to.
readonly EMULATOR_NAME="${EMULATOR_NAME:-geoshare-emulator}"
readonly SERVICES_NAME="${SERVICES_NAME:-geoshare-services}"

# Both halves share one pod, which is to say one network namespace: the
# emulator reaches its host at 10.0.2.2, and inside a pod that is the pod's
# loopback, where the stand-in services listen. Nothing of theirs is published,
# so a machine already using 8080 for something else — a popular port — is no
# longer a reason for a run to fail.
readonly POD_NAME="${POD_NAME:-geoshare}"

# The one port the pod does publish: the adb server inside the emulator
# container, for a host adb and for an instrumented run in the SDK container.
# 5038 rather than 5037, because the machine's own adb server usually holds
# 5037 and the devices plugged into it have to keep working.
readonly ADB_HOST_PORT="${ADB_HOST_PORT:-5038}"
readonly ADB_SERVER_SOCKET="${ADB_SERVER_SOCKET:-tcp:127.0.0.1:${ADB_HOST_PORT}}"

ensure_pod() {
    if podman pod exists "${POD_NAME}"; then
        return 0
    fi

    echo "==> creating pod ${POD_NAME}"
    podman pod create --name "${POD_NAME}" \
        -p "127.0.0.1:${ADB_HOST_PORT}:5037" > /dev/null
}

# Called by both --stop paths. A pod outlives the containers in it, and an
# empty one left behind holds the published port against the next run.
remove_pod_if_idle() {
    if ! podman pod exists "${POD_NAME}"; then
        return 0
    fi

    if podman container exists "${EMULATOR_NAME}" \
        || podman container exists "${SERVICES_NAME}"; then
        return 0
    fi

    podman pod rm -f "${POD_NAME}" > /dev/null 2>&1 || true
}

# A proxy listening on the host loopback is unreachable from a container on
# its own network, which is why every run and every build below shares the
# host's. HTTP_PROXY is set from the same value: curl and apt pick the
# variable matching the scheme of the URL, and the image fetches over both.
proxy_run_args=()
proxy_build_args=()
if [[ -n "${HTTPS_PROXY:-}" ]]; then
    proxy_run_args+=(-e "HTTPS_PROXY=${HTTPS_PROXY}" -e "HTTP_PROXY=${HTTPS_PROXY}")
    proxy_build_args+=(--build-arg "HTTPS_PROXY=${HTTPS_PROXY}" --build-arg "HTTP_PROXY=${HTTPS_PROXY}")
fi

# How much of the machine a container may take. Raise deliberately, with JOBS
# and MEMORY, rather than by default: the machine has other work to do.
readonly JOBS="${JOBS:-4}"
readonly MEMORY="${MEMORY:-6g}"
# shellcheck disable=SC2034 # read by the scripts that source this one
limit_args=(--cpus "${JOBS}" --memory "${MEMORY}")

ensure_sdk_image() {
    if podman image exists "${SDK_IMAGE}"; then
        return 0
    fi

    echo "==> building ${SDK_IMAGE}"
    podman build --network host "${proxy_build_args[@]}" \
        --build-arg "JDK_VERSION=${JDK_VERSION}" \
        --build-arg "ANDROID_PLATFORM=${ANDROID_PLATFORM}" \
        --build-arg "ANDROID_BUILD_TOOLS=${ANDROID_BUILD_TOOLS}" \
        --build-arg "CMDLINE_TOOLS_RELEASE=${CMDLINE_TOOLS_RELEASE}" \
        --build-arg "CMDLINE_TOOLS_SHA256=${CMDLINE_TOOLS_SHA256}" \
        --build-arg "GRADLE_VERSION=${GRADLE_VERSION}" \
        --build-arg "GRADLE_SHA256=${GRADLE_SHA256}" \
        -t "${SDK_IMAGE}" -f "${REPO_ROOT}/tools/Containerfile.sdk" "${REPO_ROOT}/tools"
}

# The emulator image is the SDK one plus a system image, so the base has to
# exist before it can be built.
ensure_emulator_image() {
    ensure_sdk_image

    if podman image exists "${EMULATOR_IMAGE}"; then
        return 0
    fi

    echo "==> building ${EMULATOR_IMAGE}"
    podman build --network host "${proxy_build_args[@]}" \
        --build-arg "SDK_IMAGE=${SDK_IMAGE}" \
        --build-arg "ANDROID_EMULATOR_API=${ANDROID_EMULATOR_API}" \
        --build-arg "ANDROID_EMULATOR_VARIANT=${EMULATOR_VARIANT}" \
        -t "${EMULATOR_IMAGE}" -f "${REPO_ROOT}/tools/Containerfile.emulator" "${REPO_ROOT}/tools"
}

# adb against the emulator, run inside its container. The emulator listens on
# the pod's loopback and nothing outside the pod can reach that, so the call
# goes where the device is rather than to whatever adb the host has.
adb_cmd() {
    podman exec "${EMULATOR_NAME}" adb "$@"
}

ensure_firebase_image() {
    if podman image exists "${FIREBASE_IMAGE}"; then
        return 0
    fi

    echo "==> building ${FIREBASE_IMAGE}"
    podman build --network host "${proxy_build_args[@]}" \
        --build-arg "NODE_VERSION=${NODE_VERSION}" \
        --build-arg "FIREBASE_TOOLS_VERSION=${FIREBASE_TOOLS_VERSION}" \
        --build-arg "FIREBASE_JRE_VERSION=${FIREBASE_JRE_VERSION}" \
        -t "${FIREBASE_IMAGE}" -f "${REPO_ROOT}/tools/Containerfile.firebase" "${REPO_ROOT}/tools"
}
