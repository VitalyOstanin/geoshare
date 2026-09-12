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

# The SDK image plus a system image, which is what an emulator needs and a
# build does not.
readonly EMULATOR_IMAGE="${EMULATOR_IMAGE:-localhost/geoshare-emulator:${ANDROID_EMULATOR_API}}"

# The containers the two halves of a try-out run in: the emulator itself, and
# the stand-in database and sign-in it talks to.
readonly EMULATOR_NAME="${EMULATOR_NAME:-geoshare-emulator}"
readonly SERVICES_NAME="${SERVICES_NAME:-geoshare-services}"

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
        -t "${EMULATOR_IMAGE}" -f "${REPO_ROOT}/tools/Containerfile.emulator" "${REPO_ROOT}/tools"
}

# adb against the emulator, without requiring the platform tools on the
# machine running these scripts: where the host has none, the call goes into
# the emulator container, which carries its own.
adb_cmd() {
    if command -v adb > /dev/null 2>&1; then
        adb "$@"
    else
        podman exec "${EMULATOR_NAME}" adb "$@"
    fi
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
