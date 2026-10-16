#!/usr/bin/env bash
# Takes the application's screenshots for the landing page, full size, on the
# headless emulator against the stand-in services and the showcase household.
#
# Every picture comes in both languages and both themes. The screen is set to
# 1440x3200 at 560 dpi: the same 411 dp across as an ordinary 1080x2400 phone,
# so the screens are laid out as on one, at a third more pixels than the
# largest copy the page offers. The status bar is the system's demo mode.
#
# The pictures land in shots/<date>/ (kept out of git, never in a temporary
# directory: they are what the page's pictures are made from, and taking them
# again means building the whole household again). tools/landing-images.sh
# turns them into the page's pictures.
#
# Needs the debug APK (tools/build.sh); starts the emulator and the services
# when they are not running and stops what it started.
#
#   tools/landing-shots.sh
#   SHOTS_DIR=shots/retake tools/landing-shots.sh
#   tools/landing-shots.sh en-light ru-dark    # only these, by name
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly PACKAGE="io.github.vitalyostanin.geoshare"
readonly APK="${REPO_ROOT}/android/app/build/outputs/apk/debug/app-debug.apk"
readonly RES="${REPO_ROOT}/android/app/src/main/res"
readonly OUT="${SHOTS_DIR:-${REPO_ROOT}/shots/$(date +%F)}"
readonly WAIT_TIMEOUT="${WAIT_TIMEOUT:-30}"
# The language-theme pairs to take, all four when none is named.
readonly ONLY="$*"
require_whole_number WAIT_TIMEOUT

# The address the invitation on the pictures is made for.
readonly INVITEE="sofia@example.com"

if [[ ! -f "${APK}" ]]; then
    echo "error: no debug APK at ${APK}; build it first (tools/build.sh)" >&2
    exit 1
fi

started=()
cleanup() {
    restore_screen || true
    local what
    for what in "${started[@]}"; do
        "${REPO_ROOT}/tools/run-${what}.sh" --stop > /dev/null 2>&1 || true
    done
}
trap cleanup EXIT

if ! podman container exists "${SERVICES_NAME}"; then
    "${REPO_ROOT}/tools/run-services.sh"
    started+=(services)
fi
if ! podman container exists "${EMULATOR_NAME}"; then
    "${REPO_ROOT}/tools/run-emulator.sh"
    started+=(emulator)
fi

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
# whole or, with --prefix, by the beginning; with --near, the one of several
# alike that sits on the same row as the node whose text begins with ANCHOR.
FIND_NODE_PY="$(cat <<'EOF'
import re
import sys
import xml.etree.ElementTree as ET

args = sys.argv[1:]
want = args[-1]
prefix = '--prefix' in args
bounds = '--bounds' in args
anchor = args[args.index('--near') + 1] if '--near' in args else None

nodes = []
for node in ET.fromstring(sys.stdin.read()).iter('node'):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    nodes.append((node.get('text', ''), node.get('content-desc', ''), x1, y1, x2, y2))

def matches(text, desc):
    if prefix:
        return text.startswith(want) or desc.startswith(want)
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

set_screen() {
    adb_e shell wm size 1440x3200
    adb_e shell wm density 560
    # Dates on the pictures read in Tallinn, where the household is.
    adb_e root > /dev/null
    sleep 3
    adb_e shell setprop persist.sys.timezone Europe/Tallinn
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

restore_screen() {
    adb_e shell am broadcast -a com.android.systemui.demo -e command exit > /dev/null
    adb_e shell wm size reset
    adb_e shell wm density reset
    adb_e shell cmd uimode night no > /dev/null
}

# The showcase household afresh, with Anna sharing with nobody yet so that the
# first-share question comes up. The application is stopped first: the
# emulator will not clear a database a client is listening to.
seed() {
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
            -e "SHOWCASE_FIRST_SHARE=1" \
            "${FIREBASE_IMAGE}" \
            node firebase/emulator/showcase-seed.mjs > /dev/null; then
            return 0
        fi
        say "the seed was refused, attempt ${attempt} of 3"
    done
    return 1
}

# One language in one theme: the six screens and the crop of the invitation
# the third step shows.
take() {
    local language="$1" theme="$2" locale
    local -A says

    case "${language}" in
        # British English for the 24-hour clock the Russian pictures have too.
        en) locale="en-GB" ;;
        ru) locale="ru-RU" ;;
    esac

    local key
    for key in first_share_title first_share_go map_follow_none \
        map_follow_menu_all_title admin_title search_start search_hours_6 \
        search_go siren_hint siren_go siren_stop_short group_make_invitation \
        group_invite_address group_invite_create group_make_another_invitation \
        settings_close; do
        says[${key}]="$(words "${language}" "${key}")"
    done
    # Up to the address, which the screen shortens.
    says[invitation]="$(words "${language}" group_invitation_screen_title)"
    says[invitation]="${says[invitation]%%%1\$s*}"

    say "${language}, ${theme}"
    seed
    adb_e shell pm clear "${PACKAGE}" > /dev/null
    adb_e shell cmd locale set-app-locales "${PACKAGE}" --locales "${locale}"
    if [[ "${theme}" == "dark" ]]; then
        adb_e shell cmd uimode night yes > /dev/null
    else
        adb_e shell cmd uimode night no > /dev/null
    fi
    adb_e shell am start -n "${PACKAGE}/.MainActivity" > /dev/null
    # Asked again once the application is up: on the first start after a
    # clear it has been seen to come up in the device's language instead.
    sleep 3
    if ! adb_e shell cmd locale get-app-locales "${PACKAGE}" | grep -qF "[${locale}]"; then
        say "the language did not hold, setting it again"
        adb_e shell cmd locale set-app-locales "${PACKAGE}" --locales "${locale}"
        sleep 3
    fi

    # The stand-in sign-in buttons are named after the seed's accounts.
    tap "parent"
    wait_for "${says[first_share_title]}" > /dev/null
    # The strip filled in is the sign the positions have been read.
    wait_for "Leo" > /dev/null
    shoot "${language}/sharing-${theme}" 4

    tap "${says[first_share_go]}"
    tap "${says[map_follow_none]}"
    tap "${says[map_follow_menu_all_title]}"
    shoot "${language}/map-${theme}" 4

    tap "Leo"
    shoot "${language}/held-back-${theme}" 3
    adb_e shell input keyevent KEYCODE_BACK
    sleep 1

    # Anna's initial is the way into the settings.
    tap "A"
    tap_below "${says[admin_title]}"
    tap --near "Galaxy S24" "${says[search_start]}"
    tap "${says[search_hours_6]}"
    tap "${says[search_go]}"
    tap --near "Redmi Note 13" "${says[siren_hint]}"
    tap "${says[siren_go]}"
    wait_for "${says[siren_stop_short]}" > /dev/null
    shoot "${language}/siren-${theme}" 1

    tap_below "${says[group_make_invitation]}"
    tap "${says[group_invite_address]}"
    adb_e shell input text "${INVITEE}"
    tap "${says[group_invite_create]}"
    scroll_to_end
    wait_for "${says[group_make_another_invitation]}" > /dev/null
    shoot "${language}/invitation-${theme}" 2

    # Where the third step's picture is cut from: the invitation alone.
    local top bottom
    read -r _ top _ _ < <(find_node --bounds --prefix "${says[invitation]}")
    read -r _ _ _ bottom < <(find_node --bounds "${says[group_make_another_invitation]}")
    echo "0 $((top - 40)) 1440 $((bottom - top + 80))" > "${OUT}/${language}/step-link-${theme}.crop"

    scroll_to_top
    tap "${says[settings_close]}"
    tap "${says[settings_close]}"
    tap "Mark"
    shoot "${language}/search-${theme}" 3

    adb_e shell am force-stop "${PACKAGE}"
}

mkdir -p "${OUT}/en" "${OUT}/ru"

podman cp "${APK}" "${EMULATOR_NAME}:/tmp/geoshare-debug.apk"
adb_e uninstall "${PACKAGE}" > /dev/null 2>&1 || true
adb_e install /tmp/geoshare-debug.apk > /dev/null
set_screen

for language in en ru; do
    for theme in light dark; do
        if [[ -n "${ONLY}" && " ${ONLY} " != *" ${language}-${theme} "* ]]; then
            continue
        fi
        take "${language}" "${theme}"
    done
done

say "pictures in ${OUT}"
