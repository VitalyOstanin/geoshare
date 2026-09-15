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
# Kinds of row, told apart by the first field. What each one carries is
# written at the head of the file itself, which is where to read it:
#   Fix    the position that came out of the epoch
#   NoFix  the same head row for an epoch that produced no position at all
#   Clock  the receiver's own clock as it reported it
#   Agc    the gain of one band the receiver is running
#   Slant  how the loudness of one system stood against the height of the
#          satellites it came from, worked out from the Sat rows below it
#   Drift  how far the satellites of one system moved apart from one another
#          since the block written some fifteen to sixty seconds earlier
#   Sat    one satellite the receiver named
#   Raw    one measurement the receiver made off one signal
#   Nav    one message a satellite broadcast, written when it says something
#          new rather than on every repeat
set -euo pipefail

readonly PACKAGE=io.github.vitalyostanin.geoshare

serial_args=()
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    serial_args+=(-s "${ANDROID_SERIAL}")
fi

exec adb "${serial_args[@]}" shell \
    "run-as ${PACKAGE} sh -c 'cat files/logs/sky-previous.csv 2>/dev/null; cat files/logs/sky.csv 2>/dev/null'"
