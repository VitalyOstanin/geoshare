"""The vocal layer of the SOS siren: a channel-vocoded "robot" voice singing
over the trailing word gap of each Morse cycle in song.py.

Modulator: each lyric phrase ("I lost my phone", "come back to me", ...) is
spoken whole by flite (slt, US female) rather than word by word, so flite's
own coarticulation between words carries into the modulator, then the whole
phrase is time-stretched (ffmpeg's rubberband filter) to fill its gap.

Carrier: a legato band-limited sawtooth whose pitch steps through the
phrase's melody notes; each note's start is placed where that word's own
(separately measured) spoken length would put it, so the sung syllables land
close to where a spoken performance stresses them. The melody is kept at or
below A4 so it sits under the lead siren instead of masking it. Vibrato (5 Hz,
+-15 cents, ramping in over the first 150-200 ms of the phrase) and a 30 ms
glide between notes keep the pitch changes from sounding stepped. White noise
mixed into the carrier, weighted by how unvoiced the modulator sounds at each
moment, gives consonants their hiss.

Vocoder: STFT channel vocoder (frame 1024, hop 256, Hann window), the
modulator's magnitude spectrum reduced to 48 log-spaced bands (100 Hz-10 kHz)
-- finer than a single spoken word needs, but a whole phrase carries more
formant detail -- and imposed on the carrier's per-frame unit-magnitude
spectrum.

55 % of the original speech is mixed straight into the vocoded result, along
with some of its own high end, so the words stay intelligible under the sung
character rather than dissolving into pure tone.
"""

import os
import subprocess
import wave

import numpy as np

SR = 44100
U = 7056            # samples per unit (0.16 s)
CYCLE = 34 * U       # samples per cycle (5.44 s)
N_CYCLES = 4
TOTAL_SAMPLES = N_CYCLES * CYCLE
assert TOTAL_SAMPLES == 959616

FRAME = 1024
HOP = 256

SILENCE_GAP_S = 0.015          # gap left at the tail of the sung phrase
FADE_IN_S = 0.008              # 8 ms
FADE_OUT_S = 0.060             # 60 ms

VIBRATO_RATE_HZ = 5.0
VIBRATO_START_S = 0.15         # vibrato kicks in after the note has settled
VIBRATO_RAMP_S = 0.05          # ramp-in length for the vibrato depth
MELODY_VIBRATO_CENTS = 15.0
NOTE_GLIDE_S = 0.03            # portamento between two adjacent melody notes

NOISE_BASE = 0.0                # the sung carrier is otherwise a clean tone
NOISE_MAX = 0.15                # noise ceiling in fully unvoiced (fricative) frames
UNVOICED_THRESH = 0.40          # high-freq-energy ratio above which a frame counts
                                 # as unvoiced

# A whole phrase's modulator thins out sooner than a single word's would, so
# the carrier's top harmonic sits lower than the per-word vocal did.
HARMONIC_FADE_START_HZ = 8000.0
HARMONIC_LIMIT_HZ = 10000.0

SLOPE = 1.3                     # 1/k**SLOPE rather than 1/k: a softer roll-off
                                 # than a plain sawtooth, so the harmonics fade
                                 # in gently instead of buzzing in all at once

DRY = 0.55                      # fraction of the raw speech kept under the
                                 # vocoded/sung result, for intelligibility
START_UNIT, END_UNIT = 27, 34   # the trailing word gap of every cycle

PHRASES = [
    ("I lost my phone", ["I", "lost", "my", "phone"], [69, 67, 65, 62]),  # Dm: A4 G4 F4 D4
    ("come back to me", ["come", "back", "to", "me"], [65, 62, 60, 58]),  # Bb: F4 D4 C4 Bb3
    ("I lost my phone", ["I", "lost", "my", "phone"], [69, 67, 65, 60]),  # F:  A4 G4 F4 C4
    ("please come home", ["please", "come", "home"], [69, 64, 61]),      # A7: A4 E4 C#4
]


def midi_to_freq(m: float) -> float:
    return 440.0 * (2.0 ** ((m - 69) / 12.0))


# ------------------------------------------------------------- wav i/o ----

def read_wav_float(path: str) -> tuple[np.ndarray, int]:
    with wave.open(path, "rb") as wf:
        assert wf.getsampwidth() == 2, path
        n = wf.getnframes()
        raw = wf.readframes(n)
        sr = wf.getframerate()
    data = np.frombuffer(raw, dtype="<i2").astype(np.float64) / 32768.0
    return data, sr


def write_wav_int16(path: str, data: np.ndarray, sr: int = SR) -> None:
    data = np.clip(data, -1.0, 1.0)
    ints = (data * 32767.0).astype("<i2")
    with wave.open(path, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sr)
        wf.writeframes(ints.tobytes())


def run(cmd: list[str]) -> None:
    subprocess.run(["timeout", "120"] + cmd, check=True,
                    stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)


# ------------------------------------------------------- 1. modulator -----

def synth_flite(text: str, out_path: str) -> None:
    run([
        "ffmpeg", "-y", "-loglevel", "error",
        "-f", "lavfi", "-i", f"flite=text='{text}':voice=slt",
        "-ar", str(SR), "-ac", "1", "-c:a", "pcm_s16le", out_path,
    ])


def trim_silence(data: np.ndarray, thresh_db: float = -45.0, win: int = 221) -> np.ndarray:
    thresh = 10.0 ** (thresh_db / 20.0)
    n = len(data)
    if n == 0:
        return data
    # short-window moving RMS envelope
    pad = win // 2
    padded = np.concatenate([np.zeros(pad), data, np.zeros(pad)])
    csum = np.cumsum(padded ** 2)
    csum = np.concatenate([[0.0], csum])
    energy = csum[win:] - csum[:-win]
    rms = np.sqrt(energy / win)
    above = np.where(rms > thresh)[0]
    if len(above) == 0:
        return data
    start = max(0, above[0])
    end = min(n, above[-1] + 1)
    return data[start:end]


def apply_rubberband_chain(src: str, dst: str, tempo: float) -> list[float]:
    """Apply rubberband tempo change, chaining stages if tempo falls
    outside librubberband's usable [0.25, 4] range."""
    stages = []
    t = tempo
    while t < 0.25 or t > 4.0:
        stage = 4.0 if t > 4.0 else 0.25
        stages.append(stage)
        t /= stage
    stages.append(t)

    cur = src
    tmp_files = []
    for i, st in enumerate(stages):
        is_last = (i == len(stages) - 1)
        out = dst if is_last else f"{dst}.stage{i}.wav"
        run([
            "ffmpeg", "-y", "-loglevel", "error", "-i", cur,
            "-af", f"rubberband=tempo={st:.6f}",
            "-ar", str(SR), "-ac", "1", "-c:a", "pcm_s16le", out,
        ])
        if cur != src:
            tmp_files.append(cur)
        cur = out
    for f in tmp_files:
        try:
            os.remove(f)
        except OSError:
            pass
    return stages


def fit_length(data: np.ndarray, target_len: int) -> np.ndarray:
    n = len(data)
    if n == target_len:
        return data
    if n > target_len:
        return data[:target_len]
    return np.concatenate([data, np.zeros(target_len - n)])


def speech(text: str, name: str, words_dir: str) -> np.ndarray:
    """Synthesise and trim one phrase (or, for melody timing, one word)."""
    raw = os.path.join(words_dir, f"vocal-phrase-raw-{name}.wav")
    synth_flite(text, raw)
    data, _sr = read_wav_float(raw)
    return trim_silence(data)


def stretched(data: np.ndarray, target: int, name: str, words_dir: str) -> tuple[np.ndarray, float]:
    trim = os.path.join(words_dir, f"vocal-phrase-trim-{name}.wav")
    out = os.path.join(words_dir, f"vocal-phrase-stretch-{name}.wav")
    write_wav_int16(trim, data)
    apply_rubberband_chain(trim, out, len(data) / target)
    result, _sr = read_wav_float(out)
    return fit_length(result, target), len(data) / target


# --------------------------------------------------------- 2. carrier -----

def hann(n: int) -> np.ndarray:
    return 0.5 - 0.5 * np.cos(2.0 * np.pi * np.arange(n) / n)


def harmonic_gain(inst_freq_k: np.ndarray) -> np.ndarray:
    """Linear fade over the last 2 kHz before the hard limit."""
    g = np.ones_like(inst_freq_k)
    fade = (inst_freq_k > HARMONIC_FADE_START_HZ)
    g = np.where(
        fade,
        np.clip(1.0 - (inst_freq_k - HARMONIC_FADE_START_HZ)
                / (HARMONIC_LIMIT_HZ - HARMONIC_FADE_START_HZ), 0.0, 1.0),
        g,
    )
    g = np.where(inst_freq_k >= HARMONIC_LIMIT_HZ, 0.0, g)
    return g


def noise_weight_from_modulator(modulator: np.ndarray, length: int) -> np.ndarray:
    """Per-frame high-frequency-energy ratio of the modulator, expanded
    to a per-sample noise-amplitude envelope."""
    n = len(modulator)
    if n < FRAME:
        pad = np.concatenate([modulator, np.zeros(FRAME - n)])
        frames = pad[None, :]
        centers = np.array([FRAME // 2])
    else:
        pad_len = FRAME // 2
        xp = np.concatenate([np.zeros(pad_len), modulator, np.zeros(FRAME)])
        n_frames = 1 + (len(xp) - FRAME) // HOP
        idx = np.arange(FRAME)[None, :] + HOP * np.arange(n_frames)[:, None]
        frames = xp[idx]
        centers = HOP * np.arange(n_frames) + FRAME // 2 - pad_len

    win = hann(FRAME)
    spec = np.fft.rfft(frames * win, axis=1)
    freqs = np.fft.rfftfreq(FRAME, 1.0 / SR)
    mag2 = np.abs(spec) ** 2
    total = mag2.sum(axis=1)
    high = mag2[:, freqs >= 4000.0].sum(axis=1)
    ratio = np.where(total > 1e-12, high / np.maximum(total, 1e-12), 0.0)

    centers = np.clip(centers, 0, length - 1)
    ratio_t = np.interp(np.arange(length), centers, ratio,
                         left=ratio[0], right=ratio[-1])
    boost = np.clip((ratio_t - UNVOICED_THRESH) / (1.0 - UNVOICED_THRESH), 0.0, 1.0)
    weight = NOISE_BASE + (NOISE_MAX - NOISE_BASE) * boost
    return weight


def pitch_contour(
    length: int, words: list[str], notes: list[int], words_dir: str
) -> tuple[np.ndarray, np.ndarray]:
    """Place each melody note where its word would start in a spoken
    performance: synthesise the words on their own just to measure their
    relative lengths, then split the (already phrase-stretched) modulator
    length in that proportion."""
    spans = np.array(
        [len(speech(w, f"w-{w}", words_dir)) for w in words], dtype=float)
    bounds = np.concatenate([[0], np.cumsum(spans / spans.sum() * length)]).astype(int)
    f0 = np.empty(length)
    for i, m in enumerate(notes):
        f0[bounds[i]:bounds[i + 1]] = midi_to_freq(m)

    glide = int(NOTE_GLIDE_S * SR)
    for b in bounds[1:-1]:
        f_before, f_after = f0[b - 1], f0[b]
        f0[b:b + glide] = f_before * (f_after / f_before) ** (np.arange(glide) / glide)

    t = np.arange(length) / SR
    ramp = np.clip((t - VIBRATO_START_S) / VIBRATO_RAMP_S, 0.0, 1.0)
    vibrato = MELODY_VIBRATO_CENTS * np.sin(2 * np.pi * VIBRATO_RATE_HZ * t) * ramp
    return f0 * 2.0 ** (vibrato / 1200.0), bounds


def carrier(f0: np.ndarray) -> np.ndarray:
    phase = np.cumsum(2.0 * np.pi * f0 / SR)
    out = np.zeros(len(f0))
    for k in range(1, int(HARMONIC_LIMIT_HZ / f0.min()) + 2):
        gain = harmonic_gain(k * f0)
        if np.any(gain > 0.0):
            out += gain * np.sin(k * phase) / k ** SLOPE
    return out


def highpass(x: np.ndarray, fc: float) -> np.ndarray:
    """A true one-pole recursive IIR high-pass (unlike song.py's FIR
    approximation): applied twice, it strips the low end from the dry
    speech kept under the vocoded voice without smearing its transients."""
    rc = 1.0 / (2 * np.pi * fc)
    a = rc / (rc + 1.0 / SR)
    y = np.zeros_like(x)
    for i in range(1, len(x)):
        y[i] = a * (y[i - 1] + x[i] - x[i - 1])
    return y


# --------------------------------------------------------- 3. vocoder -----

def stft_frames(x: np.ndarray) -> tuple[np.ndarray, int, int]:
    pad_len = FRAME // 2
    xp = np.concatenate([np.zeros(pad_len), x, np.zeros(FRAME)])
    n_frames = 1 + (len(xp) - FRAME) // HOP
    idx = np.arange(FRAME)[None, :] + HOP * np.arange(n_frames)[:, None]
    return xp[idx], len(xp), pad_len


def envelope_from_magnitude(
    mag: np.ndarray,
    freqs: np.ndarray,
    n_bands: int = 48,
    f_lo: float = 100.0,
    f_hi: float = 10000.0,
) -> np.ndarray:
    edges = np.geomspace(f_lo, f_hi, n_bands + 1)
    centers = np.sqrt(edges[:-1] * edges[1:])
    n_frames = mag.shape[0]
    env = np.zeros_like(mag)
    band_vals = np.zeros((n_frames, n_bands))
    for j in range(n_bands):
        m = (freqs >= edges[j]) & (freqs < edges[j + 1])
        if not np.any(m):
            band_vals[:, j] = 0.0
        else:
            band_vals[:, j] = mag[:, m].mean(axis=1)
    for i in range(n_frames):
        env[i] = np.interp(freqs, centers, band_vals[i],
                            left=band_vals[i, 0], right=band_vals[i, -1])
    return env


def vocode(modulator: np.ndarray, carrier_signal: np.ndarray) -> np.ndarray:
    length = max(len(modulator), len(carrier_signal))
    modulator = fit_length(modulator, length)
    carrier_signal = fit_length(carrier_signal, length)

    win = hann(FRAME)
    mod_frames, padded_len, pad_len = stft_frames(modulator)
    car_frames, _, _ = stft_frames(carrier_signal)

    MOD = np.fft.rfft(mod_frames * win, axis=1)
    CAR = np.fft.rfft(car_frames * win, axis=1)
    freqs = np.fft.rfftfreq(FRAME, 1.0 / SR)

    mod_mag = np.abs(MOD)
    envelope = envelope_from_magnitude(mod_mag, freqs)

    car_mag = np.abs(CAR)
    car_mag_safe = np.maximum(car_mag, 1e-9)
    car_unit = CAR / car_mag_safe

    OUT = envelope * car_unit
    out_frames = np.fft.irfft(OUT, n=FRAME, axis=1) * win[None, :]

    out = np.zeros(padded_len)
    wsum = np.zeros(padded_len)
    n_frames = out_frames.shape[0]
    for i in range(n_frames):
        s = i * HOP
        out[s:s + FRAME] += out_frames[i]
        wsum[s:s + FRAME] += win ** 2
    wsum = np.maximum(wsum, 1e-9)
    out = out / wsum
    out = out[pad_len:pad_len + length]
    return out


# ---------------------------------------------------------- 4. polish -----

def apply_fades(data: np.ndarray, sr: int = SR) -> np.ndarray:
    n = len(data)
    fi = min(int(round(FADE_IN_S * sr)), n)
    fo = min(int(round(FADE_OUT_S * sr)), n)
    out = data.copy()
    if fi > 0:
        out[:fi] *= np.linspace(0.0, 1.0, fi)
    if fo > 0:
        out[n - fo:] *= np.linspace(1.0, 0.0, fo)
    return out


def comb_filter(x: np.ndarray, delay_samples: int, feedback: float) -> np.ndarray:
    x = list(x)
    n = len(x)
    y = [0.0] * n
    buf = [0.0] * delay_samples
    bi = 0
    for i in range(n):
        d = buf[bi]
        v = x[i] + feedback * d
        y[i] = v
        buf[bi] = v
        bi += 1
        if bi >= delay_samples:
            bi = 0
    return np.array(y)


def allpass_filter(x: np.ndarray, delay_samples: int, g: float) -> np.ndarray:
    x = list(x)
    n = len(x)
    y = [0.0] * n
    buf = [0.0] * delay_samples
    bi = 0
    for i in range(n):
        d = buf[bi]
        xi = x[i]
        v = -g * xi + d
        buf[bi] = xi + g * v
        y[i] = v
        bi += 1
        if bi >= delay_samples:
            bi = 0
    return np.array(y)


def reverb(data: np.ndarray, sr: int = SR, wet: float = 0.20) -> np.ndarray:
    delays_ms = [29.7, 37.1, 41.1, 43.7]
    combs = [comb_filter(data, max(1, int(round(d * sr / 1000.0))), 0.7)
             for d in delays_ms]
    wet_sig = sum(combs) / len(combs)
    wet_sig = allpass_filter(wet_sig, max(1, int(round(5.0 * sr / 1000.0))), 0.5)
    return (1.0 - wet) * data + wet * wet_sig


def peak_normalize(data: np.ndarray, target_dbfs: float = -3.0) -> np.ndarray:
    peak = np.max(np.abs(data))
    if peak < 1e-9:
        return data
    target = 10.0 ** (target_dbfs / 20.0)
    return data * (target / peak)


# --------------------------------------------------------------- render ---

def render(words_dir: str) -> np.ndarray:
    """Sing the four lyric phrases over the trailing word gap of each cycle,
    returning the mixed, reverbed, peak-normalised vocal layer.

    words_dir holds every intermediate flite/rubberband WAV; the caller owns
    its lifetime (a scratch directory, not the repository).
    """
    os.makedirs(words_dir, exist_ok=True)
    layer = np.zeros(TOTAL_SAMPLES)
    target = (END_UNIT - START_UNIT) * U - int(SILENCE_GAP_S * SR)

    for c, (text, words, notes) in enumerate(PHRASES):
        mod, _tempo = stretched(speech(text, f"c{c}", words_dir), target, f"c{c}", words_dir)
        f0, _bounds = pitch_contour(len(mod), words, notes, words_dir)
        noise = noise_weight_from_modulator(mod, len(mod)) * \
            np.random.default_rng(c).uniform(-1.0, 1.0, len(mod))
        car = carrier(f0) + noise
        voiced = vocode(mod, car)
        scale = np.max(np.abs(voiced)) / max(np.max(np.abs(mod)), 1e-9)
        hiss = highpass(highpass(mod, 3500.0), 3500.0)
        sung = voiced + scale * (0.6 * hiss + DRY * mod)
        sung = apply_fades(sung)
        start = c * CYCLE + START_UNIT * U
        layer[start:start + len(sung)] += sung

    return peak_normalize(reverb(layer, SR, wet=0.05), -3.0)
