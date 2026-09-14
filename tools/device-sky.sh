#!/usr/bin/env bash
# Prints the sky the receiver reported, off a connected device.
#
# One block per fix: the position, the gain of every band the receiver is
# running, and every satellite it named. The prose log says what the
# application decided; this is what it decided it from.
#
#   tools/device-sky.sh                 # the whole file, oldest row first
#   ANDROID_SERIAL=<device> tools/device-sky.sh
#
# Columns:
#   Fix,<millis>,<latitude>,<longitude>,<accuracy m>,<provider>
#   Agc,<millis>,<system>,<carrier Hz>,<level dB>
#   Sat,<millis>,<system>,<id>,<C/N0>,<baseband C/N0>,<elevation>,<azimuth>,
#       <carrier Hz>,<in the fix>,<has ephemeris>,<has almanac>
set -euo pipefail

readonly PACKAGE=io.github.vitalyostanin.geoshare

serial_args=()
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    serial_args+=(-s "${ANDROID_SERIAL}")
fi

exec adb "${serial_args[@]}" shell \
    "run-as ${PACKAGE} sh -c 'cat files/logs/sky-previous.csv 2>/dev/null; cat files/logs/sky.csv 2>/dev/null'"
