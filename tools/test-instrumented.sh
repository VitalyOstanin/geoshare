#!/usr/bin/env bash
# Runs the instrumented tests on the headless emulator: the application
# starts, and its screens, from the map and the settings to the administrator's
# panel, draw and answer. CI runs the same set weekly and on demand
# (.github/workflows/instrumented.yml).
#
# Starts the emulator when it is not running and stops it afterwards; one left
# running for a try-out is used as it is and left running.
#
#   tools/test-instrumented.sh
#
# Report: android/app/build/reports/androidTests/connected/
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

if ! podman container exists "${EMULATOR_NAME}"; then
    stop_emulator() {
        "${REPO_ROOT}/tools/run-emulator.sh" --stop > /dev/null 2>&1 || true
    }
    trap stop_emulator EXIT
    "${REPO_ROOT}/tools/run-emulator.sh"
fi

# Pinned to the emulator: the Gradle task installs on every device adb can
# reach, and a phone attached for something else would get the test APK too.
ANDROID_SERIAL="$(adb_cmd devices | awk '/^emulator-[0-9]+\tdevice$/ { print $1; exit }')"
if [[ -z "${ANDROID_SERIAL}" ]]; then
    echo "error: the emulator is not ready" >&2
    adb_cmd devices >&2
    exit 1
fi
export ANDROID_SERIAL

# Built first on the host's network, where the dependencies and the test
# platform the run drives can be fetched; the run itself is in the emulator's
# pod, which can fetch nothing.
echo "==> building the application and its tests"
"${REPO_ROOT}/tools/gradle.sh" assembleDebug assembleDebugAndroidTest fetchTestPlatform

# A report left by an earlier run would otherwise be read as this one's.
report="${REPO_ROOT}/android/app/build/reports/androidTests/connected/debug/index.html"
rm -f "${report}"

echo "==> connectedDebugAndroidTest on ${ANDROID_SERIAL}"
# The runner bounds each test; this bounds the install and an emulator that
# stops answering under it.
GRADLE_IN_POD=yes timeout "${TIMEOUT:-30m}" \
    "${REPO_ROOT}/tools/gradle.sh" --offline connectedDebugAndroidTest "$@"

# The task succeeds when its test engine cannot reach the device at all, with
# no test run, so the report is read rather than the exit code trusted.
counter() {
    grep -s -A1 "id=\"$1\"" "${report}" | sed -n 's/.*class="counter">\([0-9]*\)<.*/\1/p'
}

tests="$(counter tests)"
failures="$(counter failures)"
echo "==> ${tests:-no} tests, ${failures:-no} failures"
if [[ "${tests:-0}" -eq 0 || "${failures:-1}" -ne 0 ]]; then
    echo "error: the run did not pass: see ${report}" >&2
    exit 1
fi
