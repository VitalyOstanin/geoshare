#!/usr/bin/env bash
# Rebuilds the SOS siren sound from tools/siren-sound/*.py: a mechanical-siren
# piece in Morse SOS, with a channel-vocoded voice singing "I lost my
# phone... come back to me" over it. See the docstrings of song.py and
# vocal.py for how each layer works and why.
#
#   tools/siren-sound.sh
#
# Needs python3 with numpy, and an ffmpeg built with the flite, rubberband and
# libvorbis components: flite speaks the words the vocal sings, rubberband
# stretches them to fit the beat, and libvorbis is how the result ships.
# Everything runs on the host rather than in a container -- none of the
# SDK/Firebase images here carry those, and this is a rare, offline rebuild
# rather than something every change needs.
#
# SIREN_SOUND_TARGET writes somewhere other than the shipped asset, for trying
# a change without touching the committed file:
#
#   SIREN_SOUND_TARGET=/tmp/try-siren.ogg tools/siren-sound.sh
set -euo pipefail

# shellcheck source=lib.sh
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

HERE="$(dirname "${BASH_SOURCE[0]}")"
readonly HERE
readonly TARGET="${SIREN_SOUND_TARGET:-${REPO_ROOT}/android/app/src/main/res/raw/siren_sos.ogg}"

# Fails early and by name rather than letting ffmpeg reject an unknown filter
# or encoder halfway through the run, once the (slow) rendering is already
# done.
require_ffmpeg_feature() {
    local kind="$1" name="$2" listing

    case "${kind}" in
        filter) listing="$(ffmpeg -hide_banner -filters 2> /dev/null)" ;;
        encoder) listing="$(ffmpeg -hide_banner -encoders 2> /dev/null)" ;;
    esac

    if ! grep -qw "${name}" <<< "${listing}"; then
        echo "siren-sound.sh: ffmpeg has no '${name}' ${kind};" \
            "rebuild it with the matching --enable flag" >&2
        exit 1
    fi
}

require_ffmpeg_feature filter flite
require_ffmpeg_feature filter rubberband
require_ffmpeg_feature encoder libvorbis

# All the intermediate WAVs -- the song, the vocal layer, every flite/
# rubberband file the vocal needs per word and per phrase -- live here and
# nowhere else; the trap keeps the repository clean even if a step fails.
work_dir="$(mktemp -d)"
trap 'rm -rf "${work_dir}"' EXIT

# No __pycache__ next to the sources: the script is run rarely enough that
# the compiled copies would only ever show up as untracked files.
PYTHONDONTWRITEBYTECODE=1 python3 "${HERE}/siren-sound/build.py" "${work_dir}"

# The exact flags the shipped asset was encoded with, kept fixed rather than
# left at ffmpeg's defaults, since the goal here is reproducing that file
# byte for byte rather than an equally good one.
ffmpeg -y -loglevel error -i "${work_dir}/siren-sos-22s.wav" \
    -c:a libvorbis -q:a 6 -fflags +bitexact -flags:a +bitexact "${TARGET}"

echo "==> wrote ${TARGET#"${REPO_ROOT}"/}"
