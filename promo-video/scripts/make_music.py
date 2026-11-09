#!/usr/bin/env python3
"""
Colonna sonora originale per il promo Jarvis Food (30 s, 120 BPM, 48 kHz stereo).

Tutto è sintetizzato da zero con numpy: nessun campione esterno, nessun diritto di terzi.
La griglia musicale è allineata al video a 60 fps:
  1 beat = 0,5 s = 30 frame · 1 battuta = 2 s = 120 frame.
Cambi scena (centro delle transizioni) a 4, 8, 14, 18, 22, 26 s → tutti su downbeat.

Uso:  python3 scripts/make_music.py  →  public/music.wav
"""
from __future__ import annotations

import os
import wave

import numpy as np

SR = 48_000
BPM = 120
BEAT = 60 / BPM            # 0.5 s
BAR = 4 * BEAT             # 2 s
DUR = 30.0
N = int(SR * DUR)
SCENE_CUTS = [4, 8, 14, 18, 22, 26]  # secondi
DROP = 4.0                 # entra il groove completo
OUTRO = 26.0               # accordo finale

rng = np.random.default_rng(7)
L = np.zeros(N)
R = np.zeros(N)


def t_axis(n: int) -> np.ndarray:
    return np.arange(n) / SR


def add(sig: np.ndarray, start: float, gain: float = 1.0, pan: float = 0.0) -> None:
    """Mixa un segnale mono a `start` secondi con pan equal-power (-1..1)."""
    i = int(start * SR)
    if i >= N:
        return
    sig = sig[: N - i]
    a = (pan + 1) * np.pi / 4
    L[i : i + len(sig)] += sig * gain * np.cos(a)
    R[i : i + len(sig)] += sig * gain * np.sin(a)


def adsr(n: int, a=0.005, d=0.1, s=0.6, r=0.2) -> np.ndarray:
    env = np.full(n, s)
    na, nd, nr = int(a * SR), int(d * SR), int(r * SR)
    na = min(na, n)
    env[:na] = np.linspace(0, 1, na)
    nd = min(nd, n - na)
    env[na : na + nd] = np.linspace(1, s, nd)
    nr = min(nr, n)
    env[n - nr :] *= np.linspace(1, 0, nr)
    return env


def onepole_lp(x: np.ndarray, cutoff) -> np.ndarray:
    """Passa-basso a un polo; cutoff scalare o array (Hz)."""
    cutoff = np.broadcast_to(np.asarray(cutoff, dtype=float), x.shape)
    alpha = 1 - np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for k in range(len(x)):
        acc += alpha[k] * (x[k] - acc)
        y[k] = acc
    return y


def saw(freq: float, n: int, detune_cents=(0.0,)) -> np.ndarray:
    t = t_axis(n)
    out = np.zeros(n)
    for c in detune_cents:
        f = freq * 2 ** (c / 1200)
        ph = rng.random()
        out += 2 * ((t * f + ph) % 1) - 1
    return out / len(detune_cents)


def midi(m: float) -> float:
    return 440 * 2 ** ((m - 69) / 12)


# ── Armonia: Am – F – C – G (vi–IV–I–V), una battuta per accordo ─────────────
CHORDS = [
    [57, 60, 64, 69],  # Am
    [53, 57, 60, 65],  # F
    [55, 60, 64, 67],  # C
    [55, 59, 62, 67],  # G
]
BASS = [45, 41, 48, 43]
n_bars = int(DUR / BAR)

# ── Sidechain (pompaggio sul kick) ──────────────────────────────────────────
t_all = t_axis(N)
duck = np.ones(N)
beat_phase = (t_all % BEAT) / BEAT
pump = 0.35 + 0.65 * np.clip(beat_phase / 0.45, 0, 1) ** 0.6
groove = (t_all >= DROP) & (t_all < OUTRO)
duck[groove] = pump[groove]

# ── Pad (supersaw filtrato) ─────────────────────────────────────────────────
pad = np.zeros(N)
for b in range(n_bars):
    start = b * BAR
    if start >= OUTRO:
        break
    n = int(BAR * SR) + int(0.3 * SR)
    chord = CHORDS[b % 4]
    sig = sum(saw(midi(m), n, (-12, 0, 11)) for m in chord) / len(chord)
    # nell'intro il filtro si apre lentamente, poi resta brillante
    if start < DROP:
        cut = np.linspace(500 + start * 400, 900 + start * 700, n)
    else:
        cut = np.full(n, 2600.0)
    sig = onepole_lp(sig, cut)
    sig *= adsr(n, a=0.08, d=0.3, s=0.85, r=0.35)
    i = int(start * SR)
    seg = sig[: N - i]
    pad[i : i + len(seg)] += seg
pad *= duck
L += pad * 0.20
R += np.roll(pad, int(0.011 * SR)) * 0.20  # leggero allargamento stereo

# ── Accordo finale (outro) con lunga coda ───────────────────────────────────
n = int((DUR - OUTRO) * SR)
fin = sum(saw(midi(m), n, (-14, -5, 6, 15)) for m in [57, 60, 64, 69, 76]) / 5
fin = onepole_lp(fin, np.linspace(3200, 900, n)) * adsr(n, a=0.01, d=1.2, s=0.55, r=1.6)
add(fin, OUTRO, 0.30, -0.1)
sub = np.sin(2 * np.pi * midi(33) * t_axis(n)) * adsr(n, a=0.005, d=2.5, s=0.0, r=0.5)
add(sub, OUTRO, 0.45)

# ── Basso ottava pulsante (crome) ───────────────────────────────────────────
for b in range(n_bars):
    bar_t = b * BAR
    if bar_t < DROP or bar_t >= OUTRO:
        continue
    f = midi(BASS[b % 4])
    for e in range(8):
        n = int(BEAT / 2 * SR)
        fr = f * (2 if e % 2 else 1)
        sig = saw(fr, n, (-6, 6))
        sig = onepole_lp(sig, np.linspace(1400, 300, n)) * adsr(n, a=0.003, d=0.12, s=0.5, r=0.05)
        sig += 0.6 * np.sin(2 * np.pi * f * t_axis(n)) * adsr(n, a=0.003, d=0.2, s=0.4, r=0.05)
        add(sig * duck[int((bar_t + e * BEAT / 2) * SR)], bar_t + e * BEAT / 2, 0.34)


# ── Batteria ────────────────────────────────────────────────────────────────
def kick() -> np.ndarray:
    n = int(0.45 * SR)
    t = t_axis(n)
    f = 45 + 110 * np.exp(-t * 28)
    ph = 2 * np.pi * np.cumsum(f) / SR
    body = np.sin(ph) * np.exp(-t * 7)
    click = rng.standard_normal(n) * np.exp(-t * 300) * 0.3
    return np.tanh((body + click) * 1.6)


def clap() -> np.ndarray:
    n = int(0.3 * SR)
    t = t_axis(n)
    noise = rng.standard_normal(n)
    noise = noise - onepole_lp(noise, 900)  # passa-alto
    env = np.zeros(n)
    for off in (0, 0.011, 0.022):
        env += np.exp(-np.clip(t - off, 0, None) * 45) * (t >= off)
    return noise * env * 0.5


def hat(open_: bool = False) -> np.ndarray:
    n = int((0.25 if open_ else 0.06) * SR)
    t = t_axis(n)
    noise = rng.standard_normal(n)
    noise = noise - onepole_lp(noise, 7000)
    return noise * np.exp(-t * (12 if open_ else 80))


K, C, H, HO = kick(), clap(), hat(), hat(True)
beats = int(DUR / BEAT)
for i in range(beats):
    t0 = i * BEAT
    if DROP <= t0 < OUTRO:
        add(K, t0, 0.85)
        if i % 2 == 1:
            add(C, t0, 0.42, 0.1)
        add(H, t0 + BEAT / 2, 0.16, 0.35)
        if i % 4 == 3:
            add(HO, t0 + BEAT / 2, 0.10, -0.3)
    elif t0 < DROP and t0 >= 2.0:
        # build-up: kick in quarti negli ultimi 2 s dell'intro
        add(K, t0, 0.45)
# roll di rullante prima del drop
for k in range(8):
    add(C, DROP - BEAT * 2 + k * BEAT / 4, 0.12 + k * 0.03, 0.05)

# ── Pluck arpeggiato (semicrome) sulle sezioni feature ──────────────────────
pattern = [0, 2, 1, 3, 2, 1, 3, 2]
for b in range(n_bars):
    bar_t = b * BAR
    if bar_t < 8 or bar_t >= OUTRO:
        continue
    chord = CHORDS[b % 4]
    for s in range(16):
        n = int(0.22 * SR)
        m = chord[pattern[s % 8]] + 12
        sig = saw(midi(m), n, (-4, 4))
        sig = onepole_lp(sig, np.linspace(5000, 600, n)) * np.exp(-t_axis(n) * 18)
        add(sig, bar_t + s * BEAT / 4, 0.11, -0.4 if s % 2 else 0.4)


# ── Effetti: riser, whoosh sui tagli, impatti ───────────────────────────────
def riser(length: float) -> np.ndarray:
    n = int(length * SR)
    t = t_axis(n)
    noise = rng.standard_normal(n)
    sig = onepole_lp(noise, np.geomspace(300, 9000, n))
    tone = np.sin(2 * np.pi * np.cumsum(np.geomspace(200, 1600, n)) / SR) * 0.25
    return (sig + tone) * (t / length) ** 2


def whoosh(length: float = 0.5) -> np.ndarray:
    n = int(length * SR)
    noise = rng.standard_normal(n)
    sweep = np.concatenate([np.geomspace(400, 6000, n // 2), np.geomspace(6000, 500, n - n // 2)])
    sig = onepole_lp(noise, sweep)
    sig = sig - onepole_lp(sig, 250)
    return sig * np.sin(np.linspace(0, np.pi, n)) ** 2


def impact() -> np.ndarray:
    n = int(1.6 * SR)
    t = t_axis(n)
    boom = np.sin(2 * np.pi * np.cumsum(40 + 80 * np.exp(-t * 10)) / SR) * np.exp(-t * 2.5)
    noise = onepole_lp(rng.standard_normal(n), 2500) * np.exp(-t * 6) * 0.6
    return np.tanh((boom + noise) * 1.4)


add(riser(3.5), DROP - 3.5, 0.32)
add(impact(), DROP, 0.55)
for c in SCENE_CUTS[1:]:
    w = whoosh(0.6)
    add(w, c - 0.3, 0.30, -0.6)
    add(np.roll(w, 300), c - 0.3, 0.30, 0.6)
add(riser(2.0), OUTRO - 2.0, 0.20)
add(impact(), OUTRO, 0.6)

# ── Master: fade-in breve, fade-out finale, limiter morbido ────────────────
mix = np.stack([L, R])
fade_in = np.clip(t_all / 0.15, 0, 1)
fade_out = np.clip((DUR - t_all) / 1.2, 0, 1)
mix *= fade_in * fade_out
mix /= np.max(np.abs(mix)) + 1e-9
mix = np.tanh(mix * 1.25) / np.tanh(1.25) * 0.82  # ≈ -1 dBFS di picco

out = os.path.join(os.path.dirname(__file__), "..", "public", "music.wav")
pcm = (mix.T * 32767).astype("<i2")
with wave.open(out, "wb") as w:
    w.setnchannels(2)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes(pcm.tobytes())
print(f"Scritto {os.path.abspath(out)} ({DUR:.0f} s, {SR} Hz)")
