#!/usr/bin/env bash
# Prints the application's own log off a connected device.
#
# The system log is unreadable on some devices — MagicOS keeps its buffers to
# itself and `adb logcat` returns nothing — so the application writes its own
# file, and a debuggable build lets adb read it.
#
#   tools/device-log.sh                 # the whole log, oldest line first
#   tools/device-log.sh --kept          # only the lines kept apart for longer
#   ANDROID_SERIAL=<device> tools/device-log.sh
set -euo pipefail

readonly PACKAGE=io.github.vitalyostanin.geoshare

serial_args=()
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    serial_args+=(-s "${ANDROID_SERIAL}")
fi

# One shell on the device, so the two files arrive in order and a missing
# previous.log — nothing has rolled over yet — is not an error.
current=current.log
previous=previous.log

if [[ "${1:-}" == --kept ]]; then
    current=kept.log
    previous=kept-previous.log
fi

exec adb "${serial_args[@]}" shell \
    "run-as ${PACKAGE} sh -c 'cat files/logs/${previous} 2>/dev/null; cat files/logs/${current} 2>/dev/null'"
