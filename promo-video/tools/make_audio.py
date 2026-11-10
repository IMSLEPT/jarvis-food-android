#!/usr/bin/env python3
"""Colonna sonora ORIGINALE e procedurale per lo spot Jarvis Food (nessun campione esterno).
Genera public/audio/music.wav e public/audio/sfx.wav (48 kHz stereo, 30 s).

Griglia: 120 BPM -> 1 beat = 0,5 s = 30 frame a 60 fps, 1 battuta = 2 s = 120 frame.
Ogni scena dura 2 battute: la prima "piena" (transizione + costruzione), la seconda "leggera"
(presentazione) con rullata/riser sull'ultimo beat.  I cue SFX sono espressi in FRAME video.

Uso: python3 tools/make_audio.py
"""
import os, wave
import numpy as np
from scipy import signal

SR = 48000
DUR = 30.0
N = int(SR * DUR)
BEAT = 0.5
BAR = 2.0
rng = np.random.default_rng(7)
OUT = os.path.join(os.path.dirname(__file__), '..', 'public', 'audio')
os.makedirs(OUT, exist_ok=True)


# ---------------------------------------------------------------- utilita'
def buf():
    return np.zeros((N + SR * 4, 2))


def add(dst, x, t, gain=1.0, pan=0.0):
    """Somma x (mono o stereo) in dst al tempo t (s). pan -1..1"""
    i = int(round(t * SR))
    if i < 0:
        x = x[-i:]; i = 0
    if x.ndim == 1:
        l = np.cos((pan + 1) * np.pi / 4); r = np.sin((pan + 1) * np.pi / 4)
        x = np.stack([x * l, x * r], axis=1) * 1.414
    n = min(len(x), len(dst) - i)
    if n > 0:
        dst[i:i + n] += x[:n] * gain


def tt(d):
    return np.arange(int(d * SR)) / SR


def adsr(d, a=0.005, dec=0.1, s=0.0, r=0.05):
    n = int(d * SR); t = np.arange(n) / SR
    e = np.where(t < a, t / max(a, 1e-6), s + (1 - s) * np.exp(-(t - a) / max(dec, 1e-6)))
    rn = max(1, int(r * SR))
    e[-rn:] *= np.linspace(1, 0, min(rn, n))[-min(rn, n):] if rn <= n else 1
    return e


def filt(x, kind, fc, order=2):
    fc = np.clip(np.atleast_1d(fc), 20, SR / 2 - 200)
    sos = signal.butter(order, fc if len(fc) > 1 else fc[0], kind, fs=SR, output='sos')
    return signal.sosfilt(sos, x, axis=0)


def sweep_filt(x, kind, f0, f1, q_order=2, curve=1.0, chunk=384):
    """Filtro con frequenza di taglio che scorre da f0 a f1 (esponenziale)."""
    out = np.zeros_like(x); zi = None; n = len(x)
    for s in range(0, n, chunk):
        p = (s / n) ** curve
        fc = f0 * (f1 / f0) ** p
        if kind == 'bandpass':
            sos = signal.butter(q_order, [fc * 0.7, min(fc * 1.4, SR / 2 - 300)], 'bandpass', fs=SR, output='sos')
        else:
            sos = signal.butter(q_order, min(fc, SR / 2 - 300), kind, fs=SR, output='sos')
        if zi is None:
            zi = np.zeros((sos.shape[0], 2))
        out[s:s + chunk], zi = signal.sosfilt(sos, x[s:s + chunk], zi=zi)
    return out


def saw(f, d, detune=0.0, phase=None):
    t = tt(d)
    ph = (rng.random() if phase is None else phase)
    return 2 * ((f * (1 + detune) * t + ph) % 1) - 1


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def reverb(x, decay=1.6, mix=1.0, pre=0.012, bright=6000):
    n = int(decay * SR * 1.3)
    t = np.arange(n) / SR
    ir = rng.standard_normal((n, 2)) * np.exp(-t / (decay / 5))[:, None]
    ir = filt(ir, 'lowpass', bright); ir = filt(ir, 'highpass', 250)
    ir[: int(pre * SR)] = 0
    ir /= np.sqrt((ir ** 2).sum(axis=0)).max()
    y = np.stack([signal.fftconvolve(x[:, c], ir[:, c])[: len(x)] for c in range(2)], axis=1)
    return y * mix


def delay(x, time, fb=0.35, mix=0.3, pingpong=True):
    d = int(time * SR); y = np.zeros_like(x); cur = x.copy()
    for k in range(1, 6):
        cur = np.roll(cur, d, axis=0) * fb; cur[:d] = 0
        if pingpong:
            cur = cur[:, ::-1].copy()
        cur = filt(cur, 'lowpass', 4500)
        y += cur
    return y * mix


def mixl(*xs):
    n = max(len(x) for x in xs); o = np.zeros(n)
    for x in xs:
        o[:len(x)] += x
    return o


# ---------------------------------------------------------------- strumenti
def kick(amp=1.0, long=False):
    d = 0.42 if long else 0.3
    t = tt(d)
    f = 46 + 130 * np.exp(-t / 0.028) + 60 * np.exp(-t / 0.006)
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = np.sin(ph) * np.exp(-t / (0.16 if long else 0.11))
    click = filt(rng.standard_normal(len(t)), 'highpass', 3000) * np.exp(-t / 0.004) * 0.25
    return np.tanh((x + click) * 1.8) * 0.9 * amp


def clap():
    d = 0.3; t = tt(d); x = np.zeros(len(t))
    for off in (0, 0.011, 0.022, 0.03):
        i = int(off * SR)
        x[i:] += rng.standard_normal(len(t) - i) * np.exp(-(t[: len(t) - i]) / (0.012 if off < 0.03 else 0.09))
    x = filt(x, 'bandpass', [900, 4200])
    return x * 0.5


def snare(amp=1.0):
    d = 0.2; t = tt(d)
    n = filt(rng.standard_normal(len(t)), 'highpass', 1800) * np.exp(-t / 0.055)
    tone = np.sin(2 * np.pi * (190 + 60 * np.exp(-t / 0.02)) * t) * np.exp(-t / 0.05)
    return (n * 0.6 + tone * 0.5) * amp


def hat(open_=False, amp=1.0):
    d = 0.22 if open_ else 0.05; t = tt(d)
    x = filt(rng.standard_normal(len(t)), 'highpass', 7500, 4) * np.exp(-t / (0.07 if open_ else 0.013))
    return x * 0.35 * amp


def bass_note(note, d, cutoff=700, drive=1.6):
    f = midi(note)
    x = saw(f, d, 0, 0) * 0.6 + saw(f, d, 0.006, 0.3) * 0.4 + np.sign(np.sin(2 * np.pi * f * tt(d))) * 0.25
    x = sweep_filt(x, 'lowpass', cutoff * 2.6, cutoff * 0.7, 2, 0.5)
    sub = np.sin(2 * np.pi * f * tt(d)) * 0.7
    return np.tanh((x + sub) * drive) * adsr(d, 0.004, 0.25, 0.55, 0.03) * 0.5


def supersaw(notes, d, cutoff=4200, a=0.004, dec=0.16, s=0.0):
    n = int(d * SR); L = np.zeros(n); R = np.zeros(n)
    for note in notes:
        f = midi(note)
        for k, det in enumerate((-0.011, -0.007, -0.003, 0, 0.004, 0.008, 0.012)):
            v = saw(f, d, det)
            p = (k / 6) * 2 - 1
            L += v * (1 - p) * 0.5; R += v * (1 + p) * 0.5
    x = np.stack([L, R], axis=1) / (len(notes) * 3.2)
    x = filt(x, 'lowpass', cutoff, 2); x = filt(x, 'highpass', 180)
    return x * adsr(d, a, dec, s, 0.04)[:, None]


def pluck(note, d=0.32, bright=1.0):
    f = midi(note); t = tt(d)
    x = saw(f, d, 0, 0) * 0.5 + np.sin(2 * np.pi * f * t) * 0.6 + np.sin(2 * np.pi * 2 * f * t) * 0.2
    x = sweep_filt(x, 'lowpass', min(9000, 5200 * bright), 500, 2, 0.4)
    return x * np.exp(-t / 0.085) * adsr(d, 0.002, 1, 1, 0.02) * 0.5


def bell(note, d=1.2, amp=1.0):
    f = midi(note); t = tt(d)
    x = sum(np.sin(2 * np.pi * f * r * t) * a * np.exp(-t / (0.35 / (1 + i * 0.6)))
            for i, (r, a) in enumerate(((1, 1), (2.01, 0.45), (3.02, 0.22), (4.2, 0.12), (5.4, 0.06))))
    return x * 0.3 * amp * adsr(d, 0.002, 1, 1, 0.05)


def noise_sweep(d, f0, f1, shape='bell', curve=1.0, order=2):
    x = sweep_filt(rng.standard_normal(int(d * SR)), 'bandpass', f0, f1, order, curve)
    t = np.linspace(0, 1, len(x))
    if shape == 'bell':
        e = np.sin(np.pi * t) ** 1.6
    elif shape == 'up':
        e = t ** 2.2
    else:
        e = (1 - t) ** 2
    return x * e


def riser(d, amp=1.0):
    n = noise_sweep(d, 500, 11000, 'up', 1.3)
    t = tt(d); p = np.linspace(0, 1, len(t))
    f = midi(57) * 2 ** (p * 2.0)
    tone = np.sin(2 * np.pi * np.cumsum(f) / SR) * p ** 2 * 0.25
    tone += (2 * ((np.cumsum(f * 1.005) / SR) % 1) - 1) * p ** 3 * 0.12
    return (n * 0.55 + tone) * amp


def impact(amp=1.0, tail=1.6):
    t = tt(tail)
    f = 34 + 70 * np.exp(-t / 0.06)
    boom = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.4)
    n = filt(rng.standard_normal(len(t)), 'lowpass', 2500) * np.exp(-t / 0.12) * 0.5
    crash = filt(rng.standard_normal(len(t)), 'highpass', 5000) * np.exp(-t / 0.45) * 0.22
    return np.tanh((boom * 1.3 + n) * 1.5) * 0.8 * amp + crash * amp


def whoosh(d=0.5, f0=400, f1=5000, amp=1.0):
    return noise_sweep(d, f0, f1, 'bell', 1.0) * amp * 1.3


def pop(f=520, amp=1.0):
    d = 0.11; t = tt(d)
    fr = f * (1 + 0.9 * np.exp(-t / 0.012))
    return np.sin(2 * np.pi * np.cumsum(fr) / SR) * np.exp(-t / 0.03) * 0.5 * amp


def tick(amp=1.0, f=3200):
    d = 0.03; t = tt(d)
    return (np.sin(2 * np.pi * f * t) * 0.6 + rng.standard_normal(len(t)) * 0.2) * np.exp(-t / 0.004) * 0.4 * amp


def tap(amp=1.0):
    d = 0.09; t = tt(d)
    return (np.sin(2 * np.pi * (240 + 300 * np.exp(-t / 0.008)) * t) * np.exp(-t / 0.02) * 0.6
            + filt(rng.standard_normal(len(t)), 'highpass', 4000) * np.exp(-t / 0.003) * 0.3) * amp


def shimmer(d=0.7, amp=1.0):
    t = tt(d); x = np.zeros(len(t))
    for k in range(9):
        f = midi(84 + [0, 4, 7, 12, 16, 19, 24, 12, 7][k]) * (1 + rng.normal(0, 0.002))
        st = k * d / 14
        e = np.clip((t - st) / 0.004, 0, 1) * np.exp(-np.clip(t - st, 0, None) / 0.16)
        x += np.sin(2 * np.pi * f * t) * e
    return x * 0.09 * amp


def boing(amp=1.0):
    d = 0.32; t = tt(d)
    f = 260 * (1 + 1.2 * t / d) * (1 + 0.12 * np.sin(2 * np.pi * 26 * t) * np.exp(-t / 0.12))
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * adsr(d, 0.004, 0.14, 0.2, 0.08) * 0.4 * amp


def zap(amp=1.0, up=True):
    d = 0.22; t = tt(d); p = t / d
    f = (300 + 2600 * p ** 2) if up else (2900 - 2500 * p ** 0.5)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.4 * (2 * ((np.cumsum(f * 0.5) / SR) % 1) - 1)
    return x * np.sin(np.pi * p) ** 1.2 * 0.22 * amp


# ---------------------------------------------------------------- MUSICA
music = buf(); send = buf(); pl = buf()
duck = np.ones(len(music))          # inviluppo sidechain

# progressione (una battuta per accordo): F  G  Am  C ...  tonalita' La minore / Do maggiore
CH = {'F': ([53, 57, 60, 64], 29), 'G': ([55, 59, 62, 65], 31), 'Am': ([57, 60, 64, 67], 33),
      'C': ([55, 60, 64, 67], 36), 'Em': ([52, 55, 59, 64], 28), 'Dm': ([53, 57, 62, 65], 26)}
BARS = ['F', 'G', 'F', 'G', 'Am', 'C', 'F', 'G', 'Am', 'Em', 'F', 'G', 'Am', 'G', 'C']
FULL = {2, 4, 6, 8, 10, 12}          # battute "piene" (inizio scena, dopo il drop)
LIGHT = {3, 5, 7, 9, 11, 13}         # battute "leggere" (presentazione)

def bt(bar, beat=0.0):
    return bar * BAR + beat * BEAT

# --- intro (battute 0-1): pad, arpeggio che sale, colpo sul logo a 2,0 s, rullata verso il drop
pad = supersaw(CH['F'][0], 2.2, 1400, a=0.9, dec=3, s=0.8) * 0.5
add(music, pad, 0.0, 0.55); add(send, pad, 0.0, 0.5)
pad2 = supersaw(CH['G'][0], 2.1, 2600, a=0.02, dec=1.2, s=0.5) * 0.55
add(music, pad2, 2.0, 0.6); add(send, pad2, 2.0, 0.5)
intro_arp = [69, 72, 76, 79, 81, 84, 88, 91]
for k, n in enumerate(intro_arp):                      # 8 note da 1/8 nella prima battuta
    add(pl, pluck(n, 0.3, 0.6 + k * 0.08), k * 0.25, 0.42 + k * 0.03, pan=(-1) ** k * 0.4)
for k in range(8):                                     # seconda battuta: arpeggio in sedicesimi leggero
    add(pl, pluck([79, 83, 86, 91][k % 4], 0.25, 1.0), 2.0 + k * 0.25, 0.3, pan=(-1) ** k * 0.5)
add(music, kick(0.8, True), 1.0, 0.75); add(music, kick(0.8, True), 1.5, 0.8); add(music, kick(0.8, True), 0.5, 0.6)
add(music, impact(0.95, 1.8), 2.0, 0.9); add(send, impact(0.5, 1.0), 2.0, 0.35)
add(music, kick(1, True), 2.5, 0.7); add(music, kick(1, True), 3.0, 0.8)
# rullata 3,0 -> 4,0 (sedicesimi poi trentaduesimi) + riser
steps = [3.0 + i * 0.125 for i in range(4)] + [3.5 + i * 0.0625 for i in range(8)]
for i, s in enumerate(steps):
    add(music, snare(0.35 + 0.5 * i / len(steps)), s, 0.6)
add(music, riser(2.0, 0.6), 2.0, 0.75)

# --- corpo (battute 2-13)
STAB = [0, 0.75, 1.5, 2.5, 3.25]                    # ritmo sincopato degli accordi (in beat)
HOOK = {  # motivo principale: (beat, nota MIDI) per coppie di battute
    0: [(0, 76), (0.75, 79), (1.5, 81), (2.5, 79), (3, 76), (3.5, 72)],
    1: [(0, 74), (0.75, 79), (1.5, 83), (2.5, 81), (3, 79), (3.5, 74)],
}
for bar in range(2, 14):
    notes, root = CH[BARS[bar]]
    full = bar in FULL
    level = 1.0 + 0.05 * ((bar - 2) // 2)             # leggera crescita d'intensita' lungo lo spot
    last_light = bar in LIGHT
    for b in range(4):
        tk = bt(bar, b)
        fill_beat = last_light and b == 3              # ultimo beat della scena: niente cassa, rullata
        if not fill_beat:
            add(music, kick(1.0 if full else 0.8), tk, 0.95 if full else 0.7)
            i0 = int(tk * SR); n = int(0.3 * SR)
            duck[i0:i0 + n] = np.minimum(duck[i0:i0 + n], 0.22 + 0.78 * np.linspace(0, 1, n) ** 1.6)
        if b in (1, 3) and not fill_beat:
            add(music, clap(), tk, 0.8 if full else 0.42, pan=0.05); add(send, clap(), tk, 0.22 if full else 0.12)
        add(music, hat(True), tk + 0.25, 0.75 if full else 0.4, pan=0.2)         # charleston aperto in levare
        for s in range(4):                                                       # sedicesimi
            if s != 2:
                add(music, hat(False, 0.9 if s == 0 else 0.6), tk + s * 0.125, 0.6 if full else 0.38, pan=-0.25 + 0.15 * s)
    # basso
    if full:
        for e in range(8):                                # ottavi, con salto d'ottava in levare
            n_ = root + 12 if e % 2 == 0 else root + (24 if e in (3, 7) else 12)
            add(music, bass_note(n_ - 12 + (12 if e % 2 else 0), 0.23, 900), bt(bar) + e * 0.25, 0.8)
    else:
        for e in (0, 1.5, 2.5):                           # basso piu' rado e scuro
            add(music, bass_note(root, 0.42, 320, 1.2), bt(bar, e), 0.62)
    # accordi
    if full:
        for sb in STAB:
            x = supersaw([n + 12 for n in notes], 0.22, 5200 * level, dec=0.13)
            add(music, x, bt(bar, sb), 0.62); add(send, x, bt(bar, sb), 0.3)
        add(music, filt(rng.standard_normal((int(1.6 * SR), 2)), 'highpass', 6000) * np.exp(-tt(1.6) / 0.5)[:, None] * 0.16, bt(bar), 1.0)  # crash
        add(music, impact(0.55, 0.9), bt(bar), 0.5)
    else:
        x = supersaw([n + 12 for n in notes], 1.9, 1500, a=0.25, dec=2, s=0.7) * 0.42
        add(music, x, bt(bar), 0.6); add(send, x, bt(bar), 0.4)
    # arpeggio (piu' presente nelle battute leggere)
    arp = [notes[0] + 24, notes[2] + 24, notes[1] + 24, notes[3] + 24, notes[2] + 24, notes[0] + 36, notes[3] + 24, notes[1] + 36]
    for s in range(16):
        if full and s % 2 == 1:
            continue
        if last_light and s >= 12:
            continue
        add(pl, pluck(arp[s % 8], 0.24, 0.9 if full else 1.15), bt(bar) + s * 0.125, 0.26 if full else 0.42, pan=0.6 * (-1) ** s)
    # motivo
    for (hb, hn) in HOOK[(bar - 2) % 2]:
        if last_light and hb >= 3:
            continue
        v = mixl(pluck(hn + 12, 0.42, 1.4) * 0.8, bell(hn + 12, 0.5, 0.5))
        add(pl, v, bt(bar, hb), 0.5 if full else 0.34, pan=-0.1)
        if full and bar >= 6:
            ld = supersaw([hn], 0.3, 6000, dec=0.2) * 0.9
            add(music, ld, bt(bar, hb), 0.36); add(send, ld, bt(bar, hb), 0.25)
    # chiusura scena: riser + rullata sull'ultimo beat (su tutta la mezza battuta prima del finale)
    if last_light:
        long_ = bar == 13
        rd = 2.0 if long_ else 0.75
        add(music, riser(rd, 0.75 if long_ else 0.5), bt(bar + 1) - rd, 0.8)
        st = [bt(bar, 3) + i * 0.0625 for i in range(8)]
        if long_:
            st = [bt(bar, 2) + i * 0.125 for i in range(4)] + [bt(bar, 2.5) + i * 0.125 for i in range(4)] + st
        for i, s in enumerate(st):
            add(music, snare(0.3 + 0.6 * i / len(st)), s, 0.62)
        rv = filt(rng.standard_normal((int(0.5 * SR), 2)), 'highpass', 5000) * (np.linspace(0, 1, int(0.5 * SR)) ** 3)[:, None] * 0.2
        add(music, rv, bt(bar + 1) - 0.5, 1.0)                                    # piatto rovesciato

# --- finale (battuta 14, 28-30 s): colpo, accordo di Do tenuto, risposta melodica, coda
add(music, impact(1.25, 2.2), 28.0, 1.0); add(send, impact(0.6, 1.2), 28.0, 0.5)
add(music, kick(1.1, True), 28.0, 1.0)
fin = supersaw([48, 55, 60, 64, 67, 72, 76], 2.0, 4800, a=0.003, dec=0.9, s=0.25)
add(music, fin, 28.0, 0.95); add(send, fin, 28.0, 0.6)
add(music, bass_note(24, 1.6, 420, 1.3), 28.0, 0.95)
for k, n in enumerate([84, 88, 91, 96]):
    add(pl, mixl(pluck(n, 0.4, 1.4), bell(n, 0.9, 0.7)), 28.0 + k * 0.125, 0.5, pan=0.4 * (-1) ** k)
add(music, kick(0.9, True), 28.75, 0.7); add(music, clap(), 28.75, 0.5)
add(music, kick(1.0, True), 29.0, 0.85)
x = supersaw([60, 64, 67, 72, 79], 0.9, 5200, dec=0.5, s=0.1); add(music, x, 29.0, 0.7); add(send, x, 29.0, 0.6)
add(pl, bell(96, 1.2, 1.0), 29.0, 0.6)

# sidechain su tutto tranne batteria (qui applicato a plucks e riverbero; il basso/accordi sono gia' in "music")
pl_wet = pl + delay(pl, 0.375, 0.42, 0.34)
music += pl_wet * duck[:, None] ** 0.5
music += reverb(send + pl * 0.35, 1.5, 0.3) * (0.4 + 0.6 * duck[:, None])
music = filt(music, 'highpass', 28)

# ---------------------------------------------------------------- EFFETTI (tempi in frame @60fps)
sfx = buf(); sfx_send = buf()
def F(fr):
    return fr / 60.0
def fx(x, frame, gain=1.0, pan=0.0, rev=0.0, lead=0.0):
    add(sfx, x, F(frame) - lead, gain, pan)
    if rev:
        add(sfx_send, x, F(frame) - lead, gain * rev, pan)

# S1 - nascita del logo
fx(whoosh(0.9, 300, 3800), 2, 0.5, -0.5, 0.4); fx(shimmer(0.9), 4, 0.9, -0.3, 0.6)
fx(zap(0.9), 30, 0.7, 0.0, 0.4, lead=0.1); fx(pop(420), 30, 0.9, 0, 0.3)              # cappello
fx(whoosh(0.3, 500, 2500), 60, 0.5, -0.2, 0.2, lead=0.2); fx(tap(1.2), 60, 0.9, -0.2, 0.3)   # "Jarvis"
fx(boing(0.9), 75, 0.6, 0.2, 0.3); fx(pop(620), 75, 0.7, 0.2, 0.3)                 # "Food"
fx(whoosh(0.3, 900, 5000), 90, 0.35, 0.0, 0.3); fx(pop(820), 100, 0.6, 0.5, 0.4); fx(tick(1), 104, 0.6, 0.55); fx(tick(1), 108, 0.6, 0.6)
fx(shimmer(1.2), 122, 1.0, 0.0, 0.8)                                                # impatto (boom nella musica)
for k in range(4):
    fx(tick(0.6, 2400 + k * 300), 136 + k * 9, 0.45, -0.3 + k * 0.2)               # payoff, parola per parola
fx(whoosh(0.75, 250, 6500), 240, 0.95, 0.0, 0.4, lead=0.55)                          # T1: il logo ruota e parte
fx(zap(1.0), 226, 0.7, 0.2, 0.4)
# S2 - un'unica app
fx(tap(1.3), 242, 0.8, 0, 0.3)
for k, fr in enumerate((255, 270, 285, 300, 315)):
    fx(pop(480 + k * 70), fr, 0.75, (-1) ** k * 0.55, 0.35)
fx(whoosh(1.4, 500, 2600), 330, 0.22, 0.4, 0.5); fx(whoosh(1.4, 500, 2600), 390, 0.2, -0.4, 0.5)   # orbita della scia
for k, fr in enumerate((444, 451, 458, 465, 472)):
    fx(zap(0.7, False), fr, 0.45, (-1) ** k * 0.5, 0.3)                              # le icone rientrano
fx(whoosh(0.8, 200, 7000), 480, 1.0, 0.0, 0.4, lead=0.6)                             # T2: tuffo nello schermo
# S3 - Oggi
for k in range(14):
    fx(tick(0.5, 2600 + k * 60), 486 + k * 4, 0.3, -0.2)                            # contatore calorie
fx(bell(88, 0.8), 540, 0.6, -0.1, 0.6)                                                # anello completo
fx(whoosh(0.5, 2500, 500), 545, 0.4, 0, 0.3)
for k, fr in enumerate((555, 570, 585, 600)):
    fx(pop(540 + k * 60), fr, 0.6, -0.5 + k * 0.33, 0.3)                             # azioni rapide
fx(tap(1.2), 640, 0.9, -0.4, 0.3); fx(bell(91, 0.5), 642, 0.3, -0.4, 0.5)            # tap su "Aggiungi"
fx(whoosh(0.7, 300, 6000), 720, 1.0, 0.0, 0.4, lead=0.45)                            # T3: giro del telefono
fx(whoosh(0.4, 4000, 600), 722, 0.5, 0.0, 0.3)
# S4 - Piano
fx(tap(1.2), 750, 0.8, 0, 0.3)
for k, fr in enumerate((752, 765, 778)):
    fx(tick(1.0, 2200 + k * 500), fr, 0.7, -0.5 + k * 0.25)                          # selettore giorni
for k, fr in enumerate((780, 795, 810, 825)):
    fx(whoosh(0.16, 1500, 4000), fr, 0.3, 0, 0.2); fx(pop(400 + k * 50), fr + 5, 0.5, 0.1, 0.3)   # schede pasto
for k, fr in enumerate((876, 891, 906)):
    fx(tick(1.1, 3400), fr, 0.7, -0.3 + k * 0.3); fx(bell(84 + k * 4, 0.4), fr, 0.3, -0.3 + k * 0.3, 0.5)  # spunte lista spesa
fx(tap(1.0), 934, 0.8, -0.3, 0.3)
fx(whoosh(0.7, 250, 7000), 960, 1.0, 0.0, 0.4, lead=0.5)                             # T4: la foto esce dallo schermo
# S5 - Cucina
fx(tap(1.3), 978, 0.8, 0, 0.3)
for k, fr in enumerate((968, 974, 980, 986)):
    fx(pop(700 + k * 90, 0.7), fr, 0.4, (-1) ** k * 0.6, 0.4)                        # ingredienti
for k, fr in enumerate((990, 998, 1006, 1014, 1022)):
    fx(tick(0.8, 2800), fr, 0.5, -0.5 + k * 0.25)                                    # filtri
fx(pop(900, 1.2), 1050, 0.9, 0.5, 0.5); fx(shimmer(0.6), 1052, 0.8, 0.5, 0.7)        # cuore
fx(whoosh(0.35, 800, 3000), 1085, 0.4, 0, 0.3)                                        # scorrimento lista
fx(pop(820, 1.0), 1140, 0.7, 0.4, 0.5)
fx(whoosh(0.75, 6000, 250), 1200, 1.0, 0.0, 0.4, lead=0.5)                           # T5: frustata verticale
# S6 - Progressi
fx(tap(1.3), 1222, 0.8, 0, 0.3)
for k, fr in enumerate((1226, 1235, 1244, 1253, 1262, 1271, 1280, 1289)):
    fx(pop(500 + k * 55, 0.8), fr, 0.5, -0.6 + k * 0.17, 0.35)                       # punti del grafico
fx(tap(1.5), 1290, 1.0, 0.4, 0.4); fx(bell(91, 1.0), 1290, 0.7, 0.4, 0.7)            # badge -3,8 kg
fx(whoosh(0.3, 1000, 4500), 1300, 0.35, 0.3, 0.3)
for k in range(12):
    fx(tick(0.45, 2400 + k * 70), 1322 + k * 4, 0.3, -0.3 + k * 0.05)                # contatori
for k in range(7):
    fx(pop(380 + k * 45, 0.7), 1340 + k * 8, 0.42, -0.6 + k * 0.2, 0.3)              # barre aderenza
fx(tap(1.2), 1420, 0.8, 0.5, 0.3)
fx(whoosh(0.7, 300, 6500), 1440, 1.0, 0.3, 0.4, lead=0.5)                            # T6: portale dal tab Jarvis
# S7 - Jarvis
fx(boing(1.2), 1446, 0.9, -0.2, 0.4); fx(shimmer(0.8), 1450, 0.8, 0, 0.7)
for k in range(3):
    fx(tick(0.9, 1800 + k * 300), 1474 + k * 6, 0.6, 0.2)                            # puntini "sta scrivendo"
fx(pop(560, 1.2), 1500, 0.85, 0.3, 0.4); fx(zap(0.6), 1498, 0.4, 0.3, 0.3)           # fumetto
for k, fr in enumerate((1530, 1545, 1560, 1575)):
    fx(pop(520 + k * 60, 0.9), fr, 0.55, 0.4, 0.3); fx(bell(79 + [0, 4, 7, 12][k], 0.7), fr + 4, 0.5, 0.4, 0.6)   # spunte
fx(whoosh(0.9, 250, 7000), 1680, 1.0, 0.0, 0.5, lead=0.7)                            # T7: verso il logo
fx(zap(1.1), 1660, 0.6, 0.0, 0.4)
# S8 - finale
fx(shimmer(1.6, 1.2), 1682, 1.0, 0.0, 0.9)
for k, fr in enumerate((1684, 1688, 1692, 1696)):
    fx(pop(600 + k * 110, 0.7), fr, 0.35, (-1) ** k * 0.7, 0.5)                      # ingredienti che esplodono
fx(tap(1.2), 1725, 0.7, 0, 0.4); fx(bell(96, 1.4), 1740, 0.5, 0, 0.8)                # call to action
sfx += reverb(sfx_send, 1.3, 0.5, bright=8000)
sfx = filt(sfx, 'highpass', 60)

# ---------------------------------------------------------------- mix e scrittura
music = music[:N]; sfx = sfx[:N]
music *= 0.9; sfx *= 0.62
fade = np.ones(N); k = int(0.35 * SR); fade[-k:] = np.linspace(1, 0, k) ** 1.5
music *= fade[:, None]; sfx *= fade[:, None]
# master: guadagno + limiter "linked" (lo stesso inviluppo su entrambi gli stem, cosi' la somma resta sotto 0 dBFS)
from scipy.ndimage import maximum_filter1d, uniform_filter1d
PRE = 0.8
music *= PRE; sfx *= PRE
mix = music + sfx
env = maximum_filter1d(np.abs(mix).max(axis=1), int(0.006 * SR))
THR = 0.88
gr = np.minimum(1.0, THR / np.maximum(env, 1e-6))
gr = -maximum_filter1d(-gr, int(0.004 * SR))                       # lookahead
rel = np.empty_like(gr); c = np.exp(-1 / (0.09 * SR)); cur = 1.0
for i in range(len(gr)):                                           # rilascio 90 ms
    cur = min(gr[i], 1 - (1 - cur) * c)
    rel[i] = cur
rel = uniform_filter1d(rel, int(0.002 * SR))
music *= rel[:, None]; sfx *= rel[:, None]
mix = music + sfx; g = 1.0
print('picco finale %.3f, riduzione massima %.1f dB' % (np.abs(mix).max(), -20 * np.log10(rel.min())))

def write(name, x):
    x = np.clip(x, -1, 1)
    with wave.open(os.path.join(OUT, name), 'wb') as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype('<i2').tobytes())

write('music.wav', music); write('sfx.wav', sfx); write('_mix_preview.wav', mix * g)
print('ok')
