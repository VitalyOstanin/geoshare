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

readonly OUT="${SHOTS_DIR:-${REPO_ROOT}/shots/$(date +%F)}"
# The language-theme pairs to take, all four when none is named.
readonly ONLY="$*"

# shellcheck source=screens.sh
source "${REPO_ROOT}/tools/screens.sh"

# The address the invitation on the pictures is made for.
readonly INVITEE="sofia@example.com"

bring_up_stand

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
        settings_close map_open_settings; do
        says[${key}]="$(words "${language}" "${key}")"
    done
    # Up to the address, which the screen shortens.
    says[invitation]="$(words "${language}" group_invitation_screen_title)"
    says[invitation]="${says[invitation]%%%1\$s*}"

    say "${language}, ${theme}"
    seed --first-share
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

    # Anna's initial is the way into the settings. The letter itself is hidden
    # from the accessibility tree; the button answers to its description, which
    # gains a tail when an update is available.
    tap --prefix "${says[map_open_settings]}"
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

install_apk
# The same 411 dp across as an ordinary 1080x2400 phone, at a third more pixels.
set_screen 1440x3200 560

for language in en ru; do
    for theme in light dark; do
        if [[ -n "${ONLY}" && " ${ONLY} " != *" ${language}-${theme} "* ]]; then
            continue
        fi
        take "${language}" "${theme}"
    done
done

say "pictures in ${OUT}"
