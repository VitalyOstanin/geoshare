#!/usr/bin/env bash
# Takes the header picture of the README and the landing page: one group on
# three screens, the page in a browser window at the back and the tablet and
# the phone laid over its corners, light and dark, with a transparent
# background.
#
# The phone (1080x2400) and the tablet (2560x1600) are the headless emulator
# signed in as Anna, following everyone, the phone with the card of her old
# phone open; the page (1440x900) is tools/page-stand.sh signed in as Mark.
# Both hold the showcase household, whose accounts have fixed ids, so each
# member wears one colour on all three. tools/hero/hero.html lays the frames
# out and tools/hero/shoot.mjs draws it in the Playwright image.
#
# The frames and the pictures land in shots/<date>/hero/ (see
# tools/landing-shots.sh for why not a temporary directory), and
# tools/landing-images.sh makes the published copies of them.
#
# Needs the debug APK (tools/build.sh); starts the emulator, the services and
# the page's stand when they are not running and stops what it started.
#
#   tools/landing-hero.sh
#   SHOTS_DIR=shots/retake tools/landing-hero.sh
#   tools/landing-hero.sh page    # the page again, over the device frames taken
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly OUT="${SHOTS_DIR:-${REPO_ROOT}/shots/$(date +%F)}/hero"
# "page" leaves the device frames already in OUT as they are.
readonly ONLY="${1:-}"
readonly STAND_URL="http://127.0.0.1:${STAND_WEB_PORT:-5000}/"

# shellcheck source=screens.sh
source "${REPO_ROOT}/tools/screens.sh"

started=()
if [[ "${ONLY}" != "page" ]]; then
    bring_up_stand
fi

page_stand_started=""
hero_down() {
    if [[ "${ONLY}" != "page" ]]; then
        stand_down
    fi
    if [[ -n "${page_stand_started}" ]]; then
        "${REPO_ROOT}/tools/page-stand.sh" --stop > /dev/null 2>&1 || true
    fi
}
trap hero_down EXIT

if ! podman container exists "${STAND_NAME:-geoshare-page-stand}"; then
    "${REPO_ROOT}/tools/page-stand.sh"
    page_stand_started=1
fi
"${REPO_ROOT}/tools/page-stand.sh" --showcase > /dev/null

# One device frame in one theme: Anna's map, everybody followed.
take() {
    local name="$1" theme="$2" size="$3" density="$4"
    local -A says
    local key
    for key in map_follow_none map_follow_menu_all_title map_more; do
        says[${key}]="$(words en "${key}")"
    done

    set_screen "${size}" "${density}"
    seed
    adb_e shell pm clear "${PACKAGE}" > /dev/null
    adb_e shell cmd locale set-app-locales "${PACKAGE}" --locales en-GB
    if [[ "${theme}" == "dark" ]]; then
        adb_e shell cmd uimode night yes > /dev/null
    else
        adb_e shell cmd uimode night no > /dev/null
    fi
    adb_e shell am start -n "${PACKAGE}/.MainActivity" > /dev/null
    sleep 3

    # The stand-in sign-in buttons are named after the seed's accounts.
    tap "parent"
    # The strip filled in is the sign the positions have been read.
    wait_for "Leo" > /dev/null
    tap "${says[map_follow_none]}"
    tap "${says[map_follow_menu_all_title]}"
    # Her own two devices are in the strip's overflow, the markers being drawn
    # on the map where the screen reader cannot see them.
    if [[ "${name}" == "phone" ]]; then
        tap "${says[map_more]}"
        tap --contains "Old phone"
    fi
    shoot "${name}-${theme}" 4
    adb_e shell am force-stop "${PACKAGE}"
}

mkdir -p "${OUT}"
if [[ "${ONLY}" != "page" ]]; then
    install_apk
    for theme in light dark; do
        take phone "${theme}" 1080x2400 420
        take tablet "${theme}" 2560x1600 320
    done
fi

cp "${REPO_ROOT}/tools/hero/hero.html" "${OUT}/"
# The library of the image's own version is fetched into the container, which
# is thrown away afterwards. The host's network, where the page's stand and a
# proxy on the loopback both are.
podman run --rm --network host --ipc host \
    -v "${OUT}:/out:z" -v "${REPO_ROOT}/tools/hero:/hero:z,ro" \
    -e "STAND_URL=${STAND_URL}" \
    "mcr.microsoft.com/playwright:v${PLAYWRIGHT_VERSION}-noble" \
    bash -c "cd /tmp && npm install --silent --no-audit --no-fund playwright@${PLAYWRIGHT_VERSION} > /dev/null \
        && cp /hero/shoot.mjs /tmp/ && node /tmp/shoot.mjs /out"
rm -f "${OUT}/hero.html"

say "pictures in ${OUT}"
