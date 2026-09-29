"""The instrumental layer of the SOS siren: a mechanical-siren piece built on
Morse SOS (··· --- ···) as its rhythmic and melodic cell.

One Morse unit is 0.16 s; one SOS cycle (dot-dot-dot dash-dash-dash dot-dot-dot
plus the trailing word gap) is 34 units, and the piece runs four cycles, one
per chord: Dm, Bb, F, A7 (dominant, resolving back to Dm on the loop). Layers:
bass (saw+sub, plays every Morse element), lead siren (band-limited saw,
vibrato, glides on dashes, a quiet sweep in the trailing word gap where the
vocal sings), pad (detuned triad/A7, one-pole low-pass, crossfaded between
chords), drums (kick/clap/hihat plus a tom fill in the last word gap).

Mono 44100 Hz / 16-bit. Fixed seed (numpy default_rng(124)) so a rebuild is
byte-identical to the shipped asset.
"""

import numpy as np

SR = 44100
U = 0.16                          # Morse time unit, seconds
SPU = int(round(U * SR))          # samples per unit = 7056 (exact)
CYCLE_UNITS = 34
CYCLE_SAMPLES = CYCLE_UNITS * SPU
N_CYCLES = 4
TOTAL_SAMPLES = N_CYCLES * CYCLE_SAMPLES
DT = 1.0 / SR

rng = np.random.default_rng(124)

# ---------------------------------------------------------------- pitches --
PC = {
    "C": 0, "C#": 1, "Db": 1, "D": 2, "D#": 3, "Eb": 3, "E": 4, "F": 5,
    "F#": 6, "Gb": 6, "G": 7, "G#": 8, "Ab": 8, "A": 9, "A#": 10, "Bb": 10,
    "B": 11,
}


def midi(name, octave):
    return 12 * (octave + 1) + PC[name]


def mtof(m):
    return 440.0 * 2.0 ** ((m - 69) / 12.0)


# chord per cycle: bass root (oct2), lead root (oct4), lead fifth (oct5),
# pad voicing (oct3-4, ascending)
CHORDS = [
    dict(name="Dm", bass=midi("D", 2), lroot=midi("D", 4), lfifth=midi("A", 5),
         pad=[midi("D", 3), midi("F", 3), midi("A", 3)]),
    dict(name="Bb", bass=midi("Bb", 2), lroot=midi("Bb", 4), lfifth=midi("F", 5),
         pad=[midi("Bb", 3), midi("D", 4), midi("F", 4)]),
    dict(name="F", bass=midi("F", 2), lroot=midi("F", 4), lfifth=midi("C", 5),
         pad=[midi("F", 3), midi("A", 3), midi("C", 4)]),
    dict(name="A7", bass=midi("A", 2), lroot=midi("A", 4), lfifth=midi("E", 5),
         pad=[midi("A", 3), midi("C#", 4), midi("E", 4), midi("G", 4)]),
]

# Morse SOS layout inside one 34-unit cycle:
# S: dots 0,2,4 | O: dashes 8,12,16 | S: dots 22,24,26 | word gap 27..34
DOT_STARTS = [0, 2, 4, 22, 24, 26]
DASH_STARTS = [8, 12, 16]


# ----------------------------------------------------------------- DSP -----
def onepole_lowpass(x, fc, sr=SR, taps=300):
    """One-pole low-pass approximated as a truncated FIR (numpy-vectorised)."""
    if fc <= 0:
        return x.copy()
    rc = 1.0 / (2 * np.pi * fc)
    alpha = DT / (rc + DT)
    m = np.arange(taps)
    h = alpha * (1.0 - alpha) ** m
    return np.convolve(x, h, mode="full")[: len(x)]


def onepole_highpass(x, fc, sr=SR, taps=300):
    return x - onepole_lowpass(x, fc, sr, taps)


def exp_decay_env(n, decay_time, sr=SR):
    """Exponential decay reaching -60 dB at t = decay_time."""
    t = np.arange(n) / sr
    k = np.log(1000.0) / decay_time
    return np.exp(-k * t)


def ar_env(n, attack_s, release_s, sr=SR):
    env = np.ones(n)
    a = max(0, min(int(round(attack_s * sr)), n))
    r = max(0, min(int(round(release_s * sr)), n - a))
    if a > 0:
        env[:a] = np.linspace(0.0, 1.0, a, endpoint=False)
    if r > 0:
        env[n - r:] = np.linspace(1.0, 0.0, r)
    return env


def saw_wave(freq_inst, n_harmonics=20, max_freq=None):
    """Additive sawtooth: sum_{k=1..n} sin(k*phase)/k, phase = 2*pi*cumsum(f)*dt.

    freq_inst: array of instantaneous frequency (Hz). Supports FM glides/
    vibrato by integrating frequency into phase (not just plugging f(t)
    straight into sin). max_freq band-limits per-sample (mutes harmonics
    whose instantaneous frequency exceeds it) -> "band-limited saw".
    """
    phase0 = 2 * np.pi * np.cumsum(freq_inst) * DT
    out = np.zeros_like(freq_inst)
    for k in range(1, n_harmonics + 1):
        if max_freq is not None:
            # fade a harmonic out over the last 2 kHz below the limit: a hard
            # cut steps the signal every time vibrato carries it across
            amp = np.clip((max_freq - k * freq_inst) / 2000.0, 0.0, 1.0) / k
        else:
            amp = 1.0 / k
        out += amp * np.sin(k * phase0)
    return out * (2.0 / np.pi)


# --------------------------------------------------------------- layers ----
def render_bass():
    buf = np.zeros(TOTAL_SAMPLES)
    dot_len = int(round(0.8 * U * SR))
    dash_len = int(round(3 * U * SR))
    for c in range(N_CYCLES):
        chord = CHORDS[c]
        f0 = mtof(chord["bass"])
        cyc_off = c * CYCLE_SAMPLES

        for su in DOT_STARTS:
            start = cyc_off + su * SPU
            n = dot_len
            freq = np.full(n, f0)
            saw = saw_wave(freq, n_harmonics=20)
            sub = np.sin(2 * np.pi * np.cumsum(freq / 2.0) * DT)
            note = 0.75 * saw + 0.35 * sub
            buf[start:start + n] += note * ar_env(n, 0.005, 0.030)

        for su in DASH_STARTS:
            start = cyc_off + su * SPU
            n = dash_len
            # 30-cent pitch drop over the final quarter of the dash
            drop_from = int(0.75 * n)
            cents = np.zeros(n)
            cents[drop_from:] = np.linspace(0.0, -30.0, n - drop_from)
            freq = f0 * 2.0 ** (cents / 1200.0)
            saw = saw_wave(freq, n_harmonics=20)
            sub = np.sin(2 * np.pi * np.cumsum(freq / 2.0) * DT)
            note = 0.75 * saw + 0.35 * sub
            buf[start:start + n] += note * ar_env(n, 0.005, 0.030)
    return buf


def render_lead():
    buf = np.zeros(TOTAL_SAMPLES)
    dot_len = int(round(0.8 * U * SR))
    dash_len = int(round(3 * U * SR))
    vib_rate, vib_cents = 5.5, 15.0
    n_harm = 60  # enough to reach 14 kHz from the lowest lead root (~294 Hz)

    for c in range(N_CYCLES):
        chord = CHORDS[c]
        cyc_off = c * CYCLE_SAMPLES
        f_fifth = mtof(chord["lfifth"])
        f_root4 = mtof(chord["lroot"])
        f_root5 = f_root4 * 2.0
        f_fourth_down = f_root5 * 2.0 ** (-5.0 / 12.0)

        # dots: chord fifth, octave 5, staccato
        for su in DOT_STARTS:
            start = cyc_off + su * SPU
            n = dot_len
            t = np.arange(n) / SR
            vib = 2.0 ** ((vib_cents * np.sin(2 * np.pi * vib_rate * t)) / 1200.0)
            freq = f_fifth * vib
            saw = saw_wave(freq, n_harmonics=n_harm, max_freq=14000.0)
            buf[start:start + n] += saw * ar_env(n, 0.003, 0.025)

        # dashes: siren glide root(oct4) -> root(oct5), hold, fall a 4th
        for su in DASH_STARTS:
            start = cyc_off + su * SPU
            n = dash_len
            t = np.arange(n) / SR
            frac = t / (n / SR)
            f_base = np.empty(n)
            seg1 = frac < 0.4
            seg2 = (frac >= 0.4) & (frac < 0.8)
            seg3 = frac >= 0.8
            x1 = np.clip(frac[seg1] / 0.4, 0.0, 1.0)
            ease = 1.0 - (1.0 - x1) ** 2  # ease-out
            f_base[seg1] = f_root4 + (f_root5 - f_root4) * ease
            f_base[seg2] = f_root5
            x3 = np.clip((frac[seg3] - 0.8) / 0.2, 0.0, 1.0)
            f_base[seg3] = f_root5 + (f_fourth_down - f_root5) * x3
            vib = 2.0 ** ((vib_cents * np.sin(2 * np.pi * vib_rate * t)) / 1200.0)
            freq = f_base * vib
            saw = saw_wave(freq, n_harmonics=n_harm, max_freq=14000.0)
            buf[start:start + n] += saw * ar_env(n, 0.005, 0.030)

        # trailing word gap (units 27-33): quiet falling sweep, not silence --
        # this is where the vocal phrase for the same cycle sits, so the lead
        # steps back rather than dropping out under it
        gap_start_u = 27
        fade_u = 33.5 - 27.0  # 6.5 units -> silent by unit 33.5
        start = cyc_off + gap_start_u * SPU
        n = int(round(fade_u * U * SR))
        t = np.arange(n) / SR
        dur = fade_u * U
        frac = np.clip(t / dur, 0.0, 1.0)
        f_sweep = f_root5 + (f_root4 - f_root5) * frac  # octave -> root
        vib = 2.0 ** ((vib_cents * np.sin(2 * np.pi * vib_rate * t)) / 1200.0)
        freq = f_sweep * vib
        saw = saw_wave(freq, n_harmonics=n_harm, max_freq=14000.0)
        amp = 1.0 - frac
        atk = int(0.01 * SR)
        amp[:atk] *= np.linspace(0.0, 1.0, atk)
        level = 10 ** (-12.0 / 20.0)
        buf[start:start + n] += saw * amp * level
    return buf


def render_pad():
    buf = np.zeros(TOTAL_SAMPLES)
    fade_samples = int(round(0.3 * SR))
    detunes = [-7, 0, 7]

    # Each chord runs fade_samples past its own cycle and fades out there
    # while the next one fades in, so the chords overlap instead of dipping
    # to silence between them; the last one wraps onto the start of the piece.
    for c in range(N_CYCLES):
        chord = CHORDS[c]
        n = CYCLE_SAMPLES + fade_samples
        cyc_off = c * CYCLE_SAMPLES
        cyc_buf = np.zeros(n)
        n_notes = len(chord["pad"])
        for note_midi in chord["pad"]:
            f = mtof(note_midi)
            for cents in detunes:
                fd = f * 2.0 ** (cents / 1200.0)
                freq = np.full(n, fd)
                cyc_buf += saw_wave(freq, n_harmonics=20) / (3 * n_notes)
        cyc_buf = onepole_lowpass(cyc_buf, 1800.0)

        env = np.ones(n)
        env[:fade_samples] = np.sqrt(np.linspace(0.0, 1.0, fade_samples))
        env[-fade_samples:] = np.sqrt(np.linspace(1.0, 0.0, fade_samples))
        idx = (cyc_off + np.arange(n)) % TOTAL_SAMPLES
        np.add.at(buf, idx, cyc_buf * env)
    return buf


def render_drums():
    buf = np.zeros(TOTAL_SAMPLES)

    for c in range(N_CYCLES):
        cyc_off = c * CYCLE_SAMPLES

        # kick: on the first unit of every dash
        for su in DASH_STARTS:
            start = cyc_off + su * SPU
            n = int(round(0.30 * SR))
            t = np.arange(n) / SR
            freq = 45.0 + (110.0 - 45.0) * np.exp(-t / 0.024)  # ~120 ms drop
            phase = 2 * np.pi * np.cumsum(freq) * DT
            env = exp_decay_env(n, 0.25)
            buf[start:start + n] += np.sin(phase) * env

        # clap/snare: middle dash of O (unit 12)
        start = cyc_off + 12 * SPU
        n = int(round(0.20 * SR))
        noise = rng.standard_normal(n)
        band = onepole_lowpass(onepole_highpass(noise, 1000.0), 5000.0)
        buf[start:start + n] += band * exp_decay_env(n, 0.14) * 1.2

        # closed hihat: every dot
        for su in DOT_STARTS:
            start = cyc_off + su * SPU
            n = int(round(0.06 * SR))
            noise = rng.standard_normal(n)
            hp = onepole_highpass(noise, 7000.0)
            buf[start:start + n] += hp * exp_decay_env(n, 0.035) * 0.9

    # tom fill: word gap of the last cycle only, leading back to the top
    c = N_CYCLES - 1
    cyc_off = c * CYCLE_SAMPLES
    for su, f_start in ((28, 180.0), (29.5, 140.0), (31, 100.0)):
        start = cyc_off + int(round(su * SPU))
        n = int(round(0.30 * SR))
        t = np.arange(n) / SR
        f_end = f_start * 0.409  # same drop ratio as the kick, over ~60 ms
        freq = f_end + (f_start - f_end) * np.exp(-t / 0.012)
        phase = 2 * np.pi * np.cumsum(freq) * DT
        env = exp_decay_env(n, 0.18)
        buf[start:start + n] += np.sin(phase) * env * 0.9

    return buf


# ----------------------------------------------------------------- I/O -----
def write_wav(path, pcm_i16, sr=SR):
    import wave

    with wave.open(path, "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(sr)
        f.writeframes(pcm_i16.tobytes())


def render():
    """Render the four layers and mix them down to 16-bit PCM.

    The lead sets the loudness ceiling; bass and drums sit a little under it,
    and the pad is levelled by ear against the lead's peak (-14 dB) rather
    than mixed at a fixed gain, since its own energy varies with the chord
    voicing. A soft tanh compressor tames the drum transients before the
    final peak normalisation to -1 dBFS.
    """
    bass = render_bass()
    lead = render_lead()
    pad = render_pad()
    drums = render_drums()

    lead_g, bass_g, drums_g = 1.0, 0.85, 0.9
    lead_peak = np.max(np.abs(lead))
    pad_peak = np.max(np.abs(pad))
    pad_target = lead_peak * 10 ** (-14.0 / 20.0)
    pad_g = pad_target / pad_peak

    mix = lead * lead_g + bass * bass_g + pad * pad_g + drums * drums_g

    peak = np.max(np.abs(mix))
    norm = mix / peak
    comp = np.tanh(1.6 * norm) / np.tanh(1.6)

    target_peak = 10 ** (-1.0 / 20.0)
    cpeak = np.max(np.abs(comp))
    out = comp * (target_peak / cpeak)
    out = np.clip(out, -1.0, 1.0)

    return np.round(out * 32767.0).astype(np.int16)
