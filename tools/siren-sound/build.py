#!/usr/bin/env python3
"""Renders the SOS siren's instrumental and vocal layers, mixes them and
writes the 22 s WAV that tools/siren-sound.sh then encodes to Ogg Vorbis.

    build.py <work-dir>

Everything -- the song, the vocal and every intermediate flite/rubberband
file the vocal needs -- is written under work-dir, never into the
repository; the caller owns that directory's lifetime.
"""

import os
import sys

import mix
import song
import vocal


def main():
    if len(sys.argv) != 2:
        print(f"usage: {sys.argv[0]} <work-dir>", file=sys.stderr)
        return 2

    work_dir = sys.argv[1]
    song_path = os.path.join(work_dir, "song-sos.wav")
    vocal_path = os.path.join(work_dir, "vocal-layer.wav")
    words_dir = os.path.join(work_dir, "vocal-words")
    out_path = os.path.join(work_dir, "siren-sos-22s.wav")

    song.write_wav(song_path, song.render())
    vocal.write_wav_int16(vocal_path, vocal.render(words_dir))
    mix.write_wav(out_path, mix.build(song_path, vocal_path))

    print(f"wrote {out_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
