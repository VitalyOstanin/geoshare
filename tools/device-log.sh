#!/usr/bin/env bash
# Prints the application's own log off a connected device.
#
# The system log is unreadable on some phones — MagicOS keeps its buffers to
# itself and `adb logcat` returns nothing — so the application writes its own
# file, and a debuggable build lets adb read it.
#
#   tools/device-log.sh                 # the whole log, oldest line first
#   ANDROID_SERIAL=<device> tools/device-log.sh
set -euo pipefail

readonly PACKAGE=io.github.vitalyostanin.geoshare

serial_args=()
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    serial_args+=(-s "${ANDROID_SERIAL}")
fi

# One shell on the device, so the two files arrive in order and a missing
# previous.log — nothing has rolled over yet — is not an error.
exec adb "${serial_args[@]}" shell \
    "run-as ${PACKAGE} sh -c 'cat files/logs/previous.log 2>/dev/null; cat files/logs/current.log 2>/dev/null'"
