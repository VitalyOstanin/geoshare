#!/usr/bin/env bash
# Starts the headless emulator and waits until it has finished booting.
#
# The container shares the host network, so the emulator's adb port is the
# host's 5555 and the host adb talks to it directly — install, logcat and
# screencap all run from outside the container.
#
#   tools/run-emulator.sh            # start and wait for boot
#   tools/run-emulator.sh --stop     # stop it
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly BOOT_TIMEOUT="${BOOT_TIMEOUT:-300}"

if [[ "${1:-}" == "--stop" ]]; then
    podman rm -f "${EMULATOR_NAME}" > /dev/null 2>&1 || true
    echo "==> stopped"
    exit 0
fi

if podman container exists "${EMULATOR_NAME}"; then
    podman rm -f "${EMULATOR_NAME}" > /dev/null
fi

# Two images on first use: the emulator one extends the SDK one. Together they
# are several gigabytes and tens of minutes, so the build says what it is doing
# rather than appearing to hang.
ensure_emulator_image

echo "==> starting ${EMULATOR_NAME}"
# /dev/kvm is what makes this usable: without it qemu refuses to emulate x86_64
# at all and the container exits.
#
# keep-groups matters where /dev/kvm is reachable through membership in the kvm
# group rather than through an ACL on the current session: rootless podman
# builds the user namespace without the host's supplementary groups, so the
# device stays out of reach inside the container.
podman run -d --rm --name "${EMULATOR_NAME}" --network host \
    --group-add keep-groups --device /dev/kvm "${EMULATOR_IMAGE}" > /dev/null

echo "==> waiting for boot (up to ${BOOT_TIMEOUT}s)"
deadline=$((SECONDS + BOOT_TIMEOUT))
adb_cmd start-server > /dev/null 2>&1 || true
# -e addresses the emulator explicitly. Plain `adb shell` picks whatever is
# ready, and while the emulator is still offline that is a phone plugged in.
while (( SECONDS < deadline )); do
    if [[ "$(adb_cmd -e shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
        echo "==> booted after $((SECONDS))s"
        adb_cmd devices
        exit 0
    fi
    sleep 5
done

echo "error: the emulator did not finish booting in ${BOOT_TIMEOUT}s" >&2
podman logs --tail 20 "${EMULATOR_NAME}" >&2 || true
exit 1
