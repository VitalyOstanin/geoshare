#!/usr/bin/env bash
# Makes the landing page's pictures from the full-size screenshots that
# tools/landing-shots.sh and tools/landing-steps.sh take.
#
# Each picture comes in three widths, so that a phone screen of high density
# gets one sharp enough and a narrow one does not download the largest; the
# page names them in srcset. Metadata is stripped: a screenshot carries
# nothing worth publishing beside the pixels.
#
# Needs ImageMagick 7 with WebP.
#
#   tools/landing-images.sh                    # the newest shots/<date>/
#   tools/landing-images.sh shots/2026-10-16
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

readonly QUALITY="${QUALITY:-80}"
require_whole_number QUALITY

shots="${1:-}"
if [[ -z "${shots}" ]]; then
    shots="$(find "${REPO_ROOT}/shots" -mindepth 1 -maxdepth 1 -type d | sort | tail -n 1)"
fi
if [[ ! -d "${shots}" ]]; then
    echo "error: no screenshots at '${shots}'; take them with tools/landing-shots.sh" >&2
    exit 1
fi
readonly SITE="${REPO_ROOT}/site/images"

# The application's screens are 1440 wide; the setup page's steps 2280.
readonly SCREEN_WIDTHS=(720 1080 1440)
readonly STEP_WIDTHS=(960 1440 1920)
readonly SCREENS=(map sharing search siren held-back invitation)

# Writes NAME-WIDTH.webp for every width from SOURCE, cropped first when a
# crop geometry is given.
make() {
    local source="$1" name="$2" crop="$3"
    shift 3
    local width
    for width in "$@"; do
        magick "${source}" ${crop:+-crop "${crop}" +repage} -resize "${width}x" \
            -strip -quality "${QUALITY}" "${name}-${width}.webp"
    done
}

for language in en ru; do
    mkdir -p "${SITE}/${language}"
    for theme in light dark; do
        from="${shots}/${language}"
        to="${SITE}/${language}"

        for screen in "${SCREENS[@]}"; do
            make "${from}/${screen}-${theme}.png" "${to}/${screen}-${theme}" "" "${SCREEN_WIDTHS[@]}"
        done

        # The third step shows the invitation alone, cut from its screen.
        read -r x y w h < "${from}/step-link-${theme}.crop"
        make "${from}/invitation-${theme}.png" "${to}/step-link-${theme}" "${w}x${h}+${x}+${y}" \
            "${SCREEN_WIDTHS[@]}"

        for step in step-project step-setup; do
            make "${from}/${step}-${theme}.png" "${to}/${step}-${theme}" "" "${STEP_WIDTHS[@]}"
        done

        echo "==> ${language}, ${theme}"
    done
done
