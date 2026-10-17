#!/usr/bin/env bash
# What the scripts that take pictures on the headless emulator share: bringing
# the emulator and the stand-in services up, finding a node on the screen by
# what it says, tapping it, taking the screen, the status bar's demo mode and
# the showcase household. Sourced after lib.sh, never run on its own.
#
# The caller sets OUT, the directory the pictures go to.

readonly PACKAGE="io.github.vitalyostanin.geoshare"
readonly APK="${REPO_ROOT}/android/app/build/outputs/apk/debug/app-debug.apk"
readonly RES="${REPO_ROOT}/android/app/src/main/res"
readonly WAIT_TIMEOUT="${WAIT_TIMEOUT:-30}"
require_whole_number WAIT_TIMEOUT

# Starts what is not running yet and, on exit, stops what it started and gives
# the screen back its own size.
bring_up_stand() {
    if [[ ! -f "${APK}" ]]; then
        echo "error: no debug APK at ${APK}; build it first (tools/build.sh)" >&2
        exit 1
    fi

    started=()
    trap stand_down EXIT

    if ! podman container exists "${SERVICES_NAME}"; then
        "${REPO_ROOT}/tools/run-services.sh"
        started+=(services)
    fi
    if ! podman container exists "${EMULATOR_NAME}"; then
        "${REPO_ROOT}/tools/run-emulator.sh"
        started+=(emulator)
    fi
}

stand_down() {
    restore_screen || true
    local what
    for what in "${started[@]}"; do
        "${REPO_ROOT}/tools/run-${what}.sh" --stop > /dev/null 2>&1 || true
    done
}

# The debug build afresh, so that nothing of an earlier run is left in it.
install_apk() {
    podman cp "${APK}" "${EMULATOR_NAME}:/tmp/geoshare-debug.apk"
    adb_e uninstall "${PACKAGE}" > /dev/null 2>&1 || true
    adb_e install /tmp/geoshare-debug.apk > /dev/null
}

adb_e() {
    adb_cmd -e "$@" < /dev/null
}

say() {
    echo "==> $*" >&2
}

# The text a string resource reads in one language: the buttons are found by
# what they say, and the application carries no test tags to find them by.
words() {
    local language="$1" key="$2" file="${RES}/values/strings.xml"

    [[ "${language}" == "en" ]] || file="${RES}/values-${language}/strings.xml"

    python3 - "${file}" "${key}" <<'EOF'
import sys
import xml.etree.ElementTree as ET

for item in ET.parse(sys.argv[1]).getroot().iter('string'):
    if item.get('name') == sys.argv[2]:
        text = ''.join(item.itertext())
        print(text.replace("\\'", "'").replace('\\"', '"'))
        sys.exit(0)
sys.exit(f'no string {sys.argv[2]} in {sys.argv[1]}')
EOF
}

screen_xml() {
    adb_e shell uiautomator dump /sdcard/ui.xml > /dev/null
    adb_e exec-out cat /sdcard/ui.xml
}

# Looks a node up in what is on the screen and prints its centre, or its
# bounds with --bounds. A node matches by its text or its content description,
# whole, by the beginning with --prefix or anywhere in it with --contains;
# with --near, the one of several alike that sits on the same row as the node
# whose text begins with ANCHOR.
FIND_NODE_PY="$(cat <<'EOF'
import re
import sys
import xml.etree.ElementTree as ET

args = sys.argv[1:]
want = args[-1]
prefix = '--prefix' in args
contains = '--contains' in args
bounds = '--bounds' in args
anchor = args[args.index('--near') + 1] if '--near' in args else None

nodes = []
for node in ET.fromstring(sys.stdin.read()).iter('node'):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    nodes.append((node.get('text', ''), node.get('content-desc', ''), x1, y1, x2, y2))

def matches(text, desc):
    if prefix:
        return text.startswith(want) or desc.startswith(want)
    if contains:
        return want in text or want in desc
    return want in (text, desc)

found = [n for n in nodes if matches(n[0], n[1])]
if anchor is not None:
    rows = [n for n in nodes if n[0].startswith(anchor)]
    if not rows:
        sys.exit(1)
    middle = (rows[0][3] + rows[0][5]) / 2
    found = [n for n in found if abs((n[3] + n[5]) / 2 - middle) < 120]
if not found:
    sys.exit(1)
_, _, x1, y1, x2, y2 = found[0]
print(f'{x1} {y1} {x2} {y2}' if bounds else f'{(x1 + x2) // 2} {(y1 + y2) // 2}')
EOF
)"
readonly FIND_NODE_PY

find_node() {
    screen_xml | python3 -c "${FIND_NODE_PY}" "$@"
}

# Waits for a node to be on the screen and prints what find_node prints.
wait_for() {
    local deadline=$((SECONDS + WAIT_TIMEOUT)) found
    while ((SECONDS < deadline)); do
        if found="$(find_node "$@")"; then
            echo "${found}"
            return 0
        fi
        sleep 1
    done
    echo "error: \"${*: -1}\" did not appear in ${WAIT_TIMEOUT}s" >&2
    # What was on the screen instead, beside the pictures, to see why.
    mkdir -p "${OUT}"
    screen_xml > "${OUT}/not-found.xml" || true
    adb_e exec-out screencap -p > "${OUT}/not-found.png" || true
    echo "  the screen is in ${OUT}/not-found.xml and not-found.png" >&2
    return 1
}

tap() {
    local point
    point="$(wait_for "$@")"
    # shellcheck disable=SC2086 # two numbers, split on purpose
    adb_e shell input tap ${point}
    sleep 1
}

# Taps a node further down a list, scrolling until it shows.
tap_below() {
    local _
    for _ in 1 2 3 4; do
        if find_node "$@" > /dev/null; then
            tap "$@"
            return 0
        fi
        adb_e shell input swipe 720 2600 720 1400 400
        sleep 1
    done
    tap "$@"
}

scroll_to_end() {
    local _
    for _ in 1 2 3; do
        adb_e shell input swipe 720 2600 720 600 400
    done
    sleep 1
}

# The panel's heading, with its Close, scrolls away with the rest.
scroll_to_top() {
    local _
    for _ in 1 2 3; do
        adb_e shell input swipe 720 600 720 2600 300
    done
    sleep 1
}

shoot() {
    local name="$1"
    sleep "${2:-2}"
    adb_e exec-out screencap -p > "${OUT}/${name}.png"
    say "${name}"
}


# The screen at SIZE and DENSITY, the clock in Tallinn and the status bar in
# its demo mode, the same on every picture.
set_screen() {
    local size="$1" density="$2"

    adb_e shell wm size "${size}"
    adb_e shell wm density "${density}"
    # Dates on the pictures read in Tallinn, where the household is.
    adb_e root > /dev/null
    sleep 3
    adb_e shell setprop persist.sys.timezone Europe/Tallinn
    # System UI lays the status bar out for the size and density it started
    # with; after a few changes in one run of the emulator it draws the clock
    # and the icons cut in half. Started again, it lays them out afresh.
    restart_system_ui
    adb_e shell settings put global sysui_demo_allowed 1
    local command
    for command in \
        "enter" \
        "clock -e hhmm 0941" \
        "battery -e level 100 -e plugged false" \
        "network -e wifi show -e level 4 -e mobile show -e datatype 3g -e level 4" \
        "notifications -e visible false"; do
        # shellcheck disable=SC2086 # the demo command and its extras, split on purpose
        adb_e shell am broadcast -a com.android.systemui.demo -e command ${command} > /dev/null
    done
}

restart_system_ui() {
    adb_e shell pkill -f com.android.systemui || true
    local deadline=$((SECONDS + WAIT_TIMEOUT))
    until adb_e shell pidof com.android.systemui > /dev/null 2>&1; do
        if ((SECONDS >= deadline)); then
            echo "error: System UI did not come back in ${WAIT_TIMEOUT}s" >&2
            return 1
        fi
        sleep 1
    done
    # Its first frames still come from the old layout.
    sleep 5
}

restore_screen() {
    adb_e shell am broadcast -a com.android.systemui.demo -e command exit > /dev/null
    adb_e shell wm size reset
    adb_e shell wm density reset
    adb_e shell cmd uimode night no > /dev/null
    restart_system_ui
}

# The showcase household afresh; with --first-share, Anna shares with nobody
# yet, so that the first-share question comes up. The application is stopped
# first: the emulator will not clear a database a client is listening to.
seed() {
    local first_share=""
    [[ "${1:-}" == "--first-share" ]] && first_share=1
    adb_e shell am force-stop "${PACKAGE}"
    # A stopped application's listener lingers for a few seconds, and the
    # emulator refuses the clearing with 499 until it is gone.
    local attempt
    for attempt in 1 2 3; do
        sleep 5
        if podman run --rm --pod "${POD_NAME}" --http-proxy=false \
            -v "${REPO_ROOT}:/src:z" -w /src \
            -e "FIRESTORE_EMULATOR_HOST=127.0.0.1:8080" \
            -e "AUTH_EMULATOR_HOST=127.0.0.1:9099" \
            -e "SHOWCASE_FIRST_SHARE=${first_share}" \
            "${FIREBASE_IMAGE}" \
            node firebase/emulator/showcase-seed.mjs > /dev/null; then
            return 0
        fi
        say "the seed was refused, attempt ${attempt} of 3"
    done
    return 1
}
