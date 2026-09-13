#!/usr/bin/env bash
# Starts the headless emulator and waits until it has finished booting.
#
# The container joins the pod named in lib.sh, so the emulator and the stand-in
# services share one loopback: what the guest calls 10.0.2.2 is the pod, not
# the machine. Nothing of the emulator is reachable from outside the pod, so
# the container also runs an adb server bound to every address, and the pod
# publishes it — install, logcat and screencap then work from the host through
#
#   export ADB_SERVER_SOCKET=tcp:127.0.0.1:5038
#
#   tools/run-emulator.sh            # start and wait for boot
#   tools/run-emulator.sh --stop     # stop it
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly BOOT_TIMEOUT="${BOOT_TIMEOUT:-300}"

if [[ "${1:-}" == "--stop" ]]; then
    podman rm -f "${EMULATOR_NAME}" > /dev/null 2>&1 || true
    remove_pod_if_idle
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
ensure_pod

echo "==> starting ${EMULATOR_NAME}"
# /dev/kvm is what makes this usable: without it qemu refuses to emulate x86_64
# at all and the container exits.
#
# keep-groups matters where /dev/kvm is reachable through membership in the kvm
# group rather than through an ACL on the current session: rootless podman
# builds the user namespace without the host's supplementary groups, so the
# device stays out of reach inside the container.
#
# The proxy is left out and has to be said out loud: podman hands the host's
# HTTPS_PROXY to every container unless told not to, and a proxy on the host
# loopback is nothing at all from inside a pod.
podman run -d --rm --name "${EMULATOR_NAME}" --pod "${POD_NAME}" --http-proxy=false \
    --group-add keep-groups --device /dev/kvm "${EMULATOR_IMAGE}" > /dev/null

# Started before anything asks adb a question: a client that finds no server
# forks one of its own, and that one listens on the loopback alone, leaving the
# published port answering nothing.
podman exec -d "${EMULATOR_NAME}" adb -a nodaemon server > /dev/null 2>&1 || true
sleep 2

echo "==> waiting for boot (up to ${BOOT_TIMEOUT}s)"
deadline=$((SECONDS + BOOT_TIMEOUT))
# -e addresses the emulator explicitly. Plain `adb shell` picks whatever is
# ready, and while the emulator is still offline that is a phone plugged in.
while (( SECONDS < deadline )); do
    if [[ "$(adb_cmd -e shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
        echo "==> booted after $((SECONDS))s"
        adb_cmd devices
        echo "==> adb from the host: export ADB_SERVER_SOCKET=${ADB_SERVER_SOCKET}"
        exit 0
    fi
    sleep 5
done

echo "error: the emulator did not finish booting in ${BOOT_TIMEOUT}s" >&2
podman logs --tail 20 "${EMULATOR_NAME}" >&2 || true
exit 1
