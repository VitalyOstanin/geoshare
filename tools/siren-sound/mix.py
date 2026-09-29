"""Sums the instrumental (song.py) and vocal (vocal.py) layers into the final
22 s siren clip: fades the last 0.15 s so the loop doesn't end on a hard
edge, pads with silence to 22 s -- the siren's own length, SIREN_SECONDS in
the app and on the page, so one pass of the clip is exactly one siren -- then
normalises to -1 dBFS.

The layers are summed as the 16-bit samples song.py and vocal.py wrote rather
than as their float arrays, so a rebuild reproduces the shipped clip bit for
bit.
"""

import wave

import numpy as np

SR = 44100
TOTAL_SAMPLES = 959616   # 4 Morse-SOS cycles * 34 units * 7056 samples/unit
DURATION_S = 22


def read_wav_int16_as_float(path):
    with wave.open(path) as w:
        return np.frombuffer(w.readframes(w.getnframes()), np.int16) / 32768.0


def build(song_path, vocal_path):
    """Mix the two rendered layers down to the padded, faded, normalised
    22 s clip, as 16-bit PCM ready to write out."""
    song = read_wav_int16_as_float(song_path)
    vocal = read_wav_int16_as_float(vocal_path)
    assert len(song) == len(vocal) == TOTAL_SAMPLES

    mix = song + vocal
    fade = int(0.15 * SR)
    mix[-fade:] *= np.linspace(1.0, 0.0, fade)
    mix = np.concatenate([mix, np.zeros(DURATION_S * SR - len(mix))])
    mix = mix * (10 ** (-1 / 20) / np.max(np.abs(mix)))

    return np.round(mix * 32767).astype(np.int16)


def write_wav(path, pcm_i16, sr=SR):
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(pcm_i16.tobytes())
