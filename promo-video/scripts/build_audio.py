#!/usr/bin/env python3
"""
Regia audio del promo da 45 s: tempi, musica originale, voce e mix finale.

Unica fonte dei tempi: le sezioni musicali (BPM crescenti) e la posizione di ogni frase
sono definite qui. Lo script:
  1. legge le durate reali della voce (voiceover/words.json, prodotto da generate_vo.py)
  2. costruisce la mappa dei beat con accelerazione progressiva
  3. genera la musica (sintetizzata con numpy, nessun campione esterno)
  4. tratta la voce (filtro, compressione), la posiziona e abbassa la musica sotto il parlato
  5. scrive public/soundtrack.wav (mix finale), public/music45.wav (solo musica)
     e src/timeline.json (frame di beat, scene, frasi e singole parole per Remotion)

Uso:  python3 scripts/build_audio.py      (richiede numpy e ffmpeg)
"""
from __future__ import annotations

import json
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parent.parent
SR = 48_000
FPS = 60
TOTAL_S = 45.0
N = int(SR * TOTAL_S)
rng = np.random.default_rng(11)
VO_TARGET_DB = -19.0  # RMS del parlato per ogni frase

# ─────────────────────────────── REGIA ────────────────────────────────────────
# Ogni sezione: BPM, battute (4/4), frasi (id, battuta, beat) e scene visive (id, battuta, beat).
SECTIONS = [
    dict(name="ieri", bpm=104, bars=3,
         lines=[("L01", 0, 0.5)],
         scenes=[("Yesterday", 0, 0)]),
    dict(name="oggi", bpm=116, bars=4,
         lines=[("L02", 0, 0), ("L03", 1, 0), ("L04", 2, 0), ("L05", 3, 0)],
         scenes=[("Today", 0, 0), ("OnePerson", 1, 0), ("OneWeek", 2, 0), ("ZeroCost", 3, 0)]),
    dict(name="costruzione", bpm=120, bars=8,
         lines=[("L06", 0, 0), ("L07", 1, 1), ("L07B", 2, 2), ("L08", 4, 0), ("L09", 5, 3)],
         scenes=[("Reveal", 0, 0), ("Diary", 1, 0), ("AiPhoto", 4, 0), ("Fasting", 5, 2)]),
    dict(name="corsa", bpm=128, bars=3,
         lines=[("L10", 0, 0), ("L11", 1, 3.5)],
         scenes=[("Montage", 0, 0), ("AllInOne", 1, 3.5)]),
    dict(name="finale", bpm=128, bars=4,
         lines=[("L12", 0, 0), ("L13", 2, 1)],
         scenes=[("Future", 0, 0), ("Outro", 2, 0)]),
]
CHORDS = [[57, 60, 64, 69], [53, 57, 60, 65], [55, 60, 64, 67], [55, 59, 62, 67]]  # Am F C G
BASS = [45, 41, 48, 43]


def midi(m: float) -> float:
    return 440 * 2 ** ((m - 69) / 12)


# ─────────────────────────── MAPPA DEI TEMPI ───────────────────────────────────
def build_grid():
    beats = []  # dict(t, section, bar, beat, gbar)
    t = 0.0
    gbar = 0
    for si, s in enumerate(SECTIONS):
        s["start"] = t
        spb = 60 / s["bpm"]
        for b in range(s["bars"]):
            for k in range(4):
                beats.append(dict(t=t, section=si, bar=b, beat=k, gbar=gbar))
                t += spb
            gbar += 1
        s["end"] = t
    return beats, t


def at(si: int, bar: int, beat: float) -> float:
    s = SECTIONS[si]
    return s["start"] + (bar * 4 + beat) * 60 / s["bpm"]


# ─────────────────────────────── SINTESI ───────────────────────────────────────
L = np.zeros(N)
R = np.zeros(N)


def t_axis(n):
    return np.arange(n) / SR


def add(sig, start, gain=1.0, pan=0.0, bus=None):
    i = int(round(start * SR))
    if i >= N or i + len(sig) <= 0:
        return
    if i < 0:
        sig = sig[-i:]
        i = 0
    sig = sig[: N - i]
    a = (pan + 1) * np.pi / 4
    tl, tr = bus if bus is not None else (L, R)
    tl[i:i + len(sig)] += sig * gain * np.cos(a)
    tr[i:i + len(sig)] += sig * gain * np.sin(a)


def lp(x, cutoff):
    """Passa-basso a un polo (cutoff scalare o array), vettorizzato a blocchi."""
    cutoff = np.broadcast_to(np.asarray(cutoff, float), x.shape)
    alpha = 1 - np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for k in range(len(x)):
        acc += alpha[k] * (x[k] - acc)
        y[k] = acc
    return y


def env(n, a=0.005, d=0.1, s=0.6, r=0.2):
    e = np.full(n, float(s))
    na = min(int(a * SR), n)
    e[:na] = np.linspace(0, 1, na)
    nd = min(int(d * SR), n - na)
    e[na:na + nd] = np.linspace(1, s, nd)
    nr = min(int(r * SR), n)
    e[n - nr:] *= np.linspace(1, 0, nr)
    return e


def saw(f, n, det=(0.0,)):
    t = t_axis(n)
    return sum(2 * ((t * f * 2 ** (c / 1200) + rng.random()) % 1) - 1 for c in det) / len(det)


def keys(f, n):
    """Pianoforte elettrico morbido: armoniche sinusoidali con decadimento."""
    t = t_axis(n)
    s = (np.sin(2 * np.pi * f * t) + 0.35 * np.sin(4 * np.pi * f * t) * np.exp(-t * 3)
         + 0.12 * np.sin(6 * np.pi * f * t) * np.exp(-t * 6))
    return s * np.exp(-t * 1.6) * env(n, a=0.004, d=0.05, s=1, r=0.1)


def kick():
    n = int(0.45 * SR)
    t = t_axis(n)
    f = 45 + 115 * np.exp(-t * 28)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 7)
    return np.tanh((body + rng.standard_normal(n) * np.exp(-t * 300) * 0.3) * 1.6)


def clap():
    n = int(0.3 * SR)
    t = t_axis(n)
    x = rng.standard_normal(n)
    x = x - lp(x, 900)
    e = sum(np.exp(-np.clip(t - o, 0, None) * 45) * (t >= o) for o in (0, 0.011, 0.022))
    return x * e * 0.5


def hat(open_=False):
    n = int((0.25 if open_ else 0.05) * SR)
    t = t_axis(n)
    x = rng.standard_normal(n)
    x = x - lp(x, 7000)
    return x * np.exp(-t * (12 if open_ else 90))


def tick():
    n = int(0.03 * SR)
    t = t_axis(n)
    return np.sin(2 * np.pi * 2600 * t) * np.exp(-t * 220)


def riser(length):
    n = int(length * SR)
    t = t_axis(n)
    x = lp(rng.standard_normal(n), np.geomspace(300, 9000, n))
    tone = np.sin(2 * np.pi * np.cumsum(np.geomspace(180, 1500, n)) / SR) * 0.25
    return (x + tone) * (t / length) ** 2


def whoosh(length=0.45):
    n = int(length * SR)
    sweep = np.concatenate([np.geomspace(400, 6500, n // 2), np.geomspace(6500, 500, n - n // 2)])
    x = lp(rng.standard_normal(n), sweep)
    x = x - lp(x, 250)
    return x * np.sin(np.linspace(0, np.pi, n)) ** 2


def impact(length=1.8):
    n = int(length * SR)
    t = t_axis(n)
    boom = np.sin(2 * np.pi * np.cumsum(38 + 85 * np.exp(-t * 10)) / SR) * np.exp(-t * 2.2)
    noise = lp(rng.standard_normal(n), 2500) * np.exp(-t * 6) * 0.6
    return np.tanh((boom + noise) * 1.4)


def build_music(beats, end_t):
    K, C, H, HO, TK = kick(), clap(), hat(), hat(True), tick()
    pad_l = np.zeros(N)
    pad_r = np.zeros(N)
    duck = np.ones(N)  # pompaggio sul kick

    bar_starts = [b for b in beats if b["beat"] == 0]
    for bi, b in enumerate(bar_starts):
        s = SECTIONS[b["section"]]
        name = s["name"]
        spb = 60 / s["bpm"]
        bar_len = 4 * spb
        ch = CHORDS[b["gbar"] % 4]
        t0 = b["t"]
        # Pad: chiuso e scuro in "ieri", poi si apre
        n = int((bar_len + 0.4) * SR)
        sig = sum(saw(midi(m), n, (-12, 0, 11)) for m in ch) / 4
        cut = {"ieri": 700, "oggi": 2000, "costruzione": 2600, "corsa": 3200, "finale": 3400}[name]
        if name == "ieri":
            cut = np.linspace(500 + bi * 150, 900 + bi * 250, n)
        sig = lp(sig, cut) * env(n, a=0.12 if name == "ieri" else 0.05, d=0.3, s=0.85, r=0.4)
        gain = 0.16 if name == "ieri" else 0.2
        i = int(t0 * SR)
        seg = sig[: N - i]
        pad_l[i:i + len(seg)] += seg * gain
        pad_r[i:i + len(seg)] += np.roll(seg, int(0.011 * SR)) * gain

        if name == "ieri":
            # pianoforte malinconico su 1 e 3, ticchettio d'orologio su ogni beat
            for k, m in ((0, ch[0] + 12), (2, ch[2] + 12)):
                add(keys(midi(m), int(2.2 * SR)), t0 + k * spb, 0.16, -0.15)
            add(keys(midi(ch[1] + 24), int(1.5 * SR)), t0 + 3 * spb, 0.07, 0.3)
            for k in range(4):
                add(TK, t0 + k * spb, 0.10 if k else 0.14, 0.4)
            continue

        # da qui in poi: groove
        last_bar_of_song = name == "finale" and b["bar"] >= 2
        for k in range(4):
            tb = t0 + k * spb
            if not last_bar_of_song:
                add(K, tb, 0.85)
                j0 = int(tb * SR)
                j1 = min(N, j0 + int(spb * SR))
                ph = np.arange(j1 - j0) / (spb * SR)
                duck[j0:j1] = np.minimum(duck[j0:j1], 0.35 + 0.65 * np.clip(ph / 0.45, 0, 1) ** 0.6)
                if k % 2 == 1:
                    add(C, tb, 0.42, 0.1)
                add(H, tb + spb / 2, 0.15, 0.35)
                if name in ("corsa", "finale"):
                    add(H, tb + spb / 4, 0.08, -0.35)
                    add(H, tb + 3 * spb / 4, 0.08, -0.35)
                if name != "oggi" and k == 3:
                    add(HO, tb + spb / 2, 0.10, -0.3)
            # basso in crome
            if not last_bar_of_song:
                f = midi(BASS[b["gbar"] % 4])
                for e in range(2):
                    n2 = int(spb / 2 * SR)
                    fr = f * (2 if e else 1)
                    sig2 = lp(saw(fr, n2, (-6, 6)), np.linspace(1400, 300, n2)) * env(n2, 0.003, 0.12, 0.5, 0.04)
                    sig2 += 0.6 * np.sin(2 * np.pi * f * t_axis(n2)) * env(n2, 0.003, 0.2, 0.4, 0.04)
                    add(sig2, tb + e * spb / 2, 0.32)
        # arpeggio pluck dalla "costruzione", in semicrome
        if name in ("costruzione", "corsa") or (name == "finale" and b["bar"] < 2):
            pat = [0, 2, 1, 3, 2, 1, 3, 2]
            for st in range(16):
                n3 = int(0.2 * SR)
                m = ch[pat[st % 8]] + 12
                sig3 = lp(saw(midi(m), n3, (-4, 4)), np.linspace(5200, 600, n3)) * np.exp(-t_axis(n3) * 18)
                add(sig3, t0 + st * spb / 4, 0.10 if name == "costruzione" else 0.12, -0.4 if st % 2 else 0.4)

    # pompaggio applicato al pad (non nella sezione "ieri")
    L[:] += pad_l * duck
    R[:] += pad_r * duck

    # ── transizioni sonore tra sezioni ──
    oggi_t = SECTIONS[1]["start"]
    add(riser(2.4), oggi_t - 2.4 - 0.18, 0.30)
    # mezzo beat di silenzio prima del "Oggi, no." (taglio a vuoto)
    gap0, gap1 = int((oggi_t - 0.18) * SR), int(oggi_t * SR)
    fade = np.linspace(1, 0, 600)
    L[gap0:gap0 + 600] *= fade
    R[gap0:gap0 + 600] *= fade
    L[gap0 + 600:gap1] = 0
    R[gap0 + 600:gap1] = 0
    add(impact(), oggi_t, 0.6)
    add(impact(1.2), SECTIONS[2]["start"], 0.35)
    # rullante che accelera prima della "corsa"
    corsa = SECTIONS[3]["start"]
    spb = 60 / SECTIONS[2]["bpm"]
    for k in range(16):
        add(C, corsa - 4 * spb + k * spb / 4, 0.08 + k * 0.02, 0.05)
    add(riser(4 * spb), corsa - 4 * spb, 0.18)
    # finale: impatto sulla firma, coda con accordo lungo e sub
    firma = at(4, 2, 0)
    add(riser(2.0), firma - 2.0, 0.22)
    add(impact(2.4), firma, 0.65)
    n = int((TOTAL_S - firma) * SR)
    fin = sum(saw(midi(m), n, (-14, -5, 6, 15)) for m in [57, 60, 64, 69, 76]) / 5
    fin = lp(fin, np.linspace(3200, 700, n)) * env(n, a=0.01, d=1.5, s=0.55, r=2.0)
    add(fin, firma, 0.30, -0.1)
    add(np.sin(2 * np.pi * midi(33) * t_axis(n)) * env(n, 0.005, 3.0, 0.0, 0.5), firma, 0.42)


def add_cut_whooshes(scene_times, skip):
    for t in scene_times:
        if any(abs(t - s) < 0.05 for s in skip):
            continue
        w = whoosh(0.4)
        add(w, t - 0.22, 0.20, -0.6)
        add(np.roll(w, 240), t - 0.22, 0.20, 0.6)



# ─────────────────────── ALLINEAMENTO PAROLE ───────────────────────────────────
def _norm_pieces(s: str) -> list[str]:
    import re
    import unicodedata
    s = unicodedata.normalize("NFKD", s.lower())
    s = "".join(c for c in s if not unicodedata.combining(c)).replace("'", " ")
    return re.findall(r"[a-z0-9]+", s)


def align_words(text: str, asr_words: list[dict], dur: float) -> list[dict]:
    """Tempi di Whisper applicati alle parole del copione (il testo a schermo resta quello scritto)."""
    import difflib

    tokens = text.split()
    sp, sp_tok = [], []
    for i, tok in enumerate(tokens):
        for piece in _norm_pieces(tok):
            sp.append(piece)
            sp_tok.append(i)
    hp, hp_t = [], []
    for w in asr_words:
        for piece in _norm_pieces(w["w"]):
            hp.append(piece)
            hp_t.append((w["s"], w["e"]))
    times: list[list[float] | None] = [None] * len(tokens)
    sm = difflib.SequenceMatcher(a=sp, b=hp, autojunk=False)
    for a0, b0, size in sm.get_matching_blocks():
        for k in range(size):
            ti = sp_tok[a0 + k]
            s0, e0 = hp_t[b0 + k]
            if times[ti] is None:
                times[ti] = [s0, e0]
            else:
                times[ti] = [min(times[ti][0], s0), max(times[ti][1], e0)]
    # interpola le parole non riconosciute tra i vicini
    for i in range(len(tokens)):
        if times[i] is None:
            prev = next((times[j][1] for j in range(i - 1, -1, -1) if times[j]), 0.0)
            nxt_i = next((j for j in range(i + 1, len(tokens)) if times[j]), None)
            nxt = times[nxt_i][0] if nxt_i is not None else dur
            gap_n = (nxt_i if nxt_i is not None else len(tokens)) - i
            step = max(0.05, (nxt - prev) / (gap_n + 0))
            times[i] = [prev, min(nxt, prev + step)]
    # monotonia
    for i in range(1, len(times)):
        times[i][0] = max(times[i][0], times[i - 1][0] + 0.01)
    return [dict(w=tok, s=round(t[0], 3), e=round(max(t[1], t[0] + 0.05), 3)) for tok, t in zip(tokens, times)]

# ───────────────────────────────── VOCE ────────────────────────────────────────
def load_wav(path: Path) -> np.ndarray:
    """Legge un wav qualsiasi, lo converte a 48 kHz mono float con ffmpeg."""
    with tempfile.NamedTemporaryFile(suffix=".f32") as tmp:
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(path), "-ac", "1", "-ar", str(SR),
                        "-f", "f32le", tmp.name], check=True)
        return np.fromfile(tmp.name, dtype=np.float32).astype(np.float64)


def process_vo(path: Path) -> np.ndarray:
    """Catena voce da spot: passa-alto, presenza, compressione, de-esser leggero, normalizzazione."""
    with tempfile.NamedTemporaryFile(suffix=".f32") as tmp:
        af = ("highpass=f=85,equalizer=f=220:t=q:w=1.2:g=1.5,equalizer=f=3200:t=q:w=1.0:g=2.5,"
              "deesser=i=0.35,acompressor=threshold=-22dB:ratio=3.5:attack=4:release=90:makeup=4,"
              "alimiter=limit=0.89")
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(path), "-af", af, "-ac", "1", "-ar", str(SR),
                        "-f", "f32le", tmp.name], check=True)
        sig = np.fromfile(tmp.name, dtype=np.float32).astype(np.float64)
    # Livella ogni frase allo stesso volume percepito (RMS delle sole parti parlate),
    # altrimenti le battute brevi risultano più deboli delle lunghe.
    win = int(0.02 * SR)
    fr = sig[: len(sig) // win * win].reshape(-1, win)
    rms = np.sqrt(np.mean(fr ** 2, axis=1) + 1e-12)
    active = rms[rms > 10 ** (-40 / 20)]
    if len(active):
        level = np.sqrt(np.mean(active ** 2))
        sig = sig * (10 ** (VO_TARGET_DB / 20) / level)
    return np.clip(sig, -0.98, 0.98)


def write_wav(path: Path, l: np.ndarray, r: np.ndarray):
    pcm = (np.stack([l, r]).T * 32767).clip(-32768, 32767).astype("<i2")
    with wave.open(str(path), "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def loudnorm(path: Path, target=-14.0, tp=-1.5):
    """Normalizzazione EBU R128 in due passaggi (lineare) con ffmpeg."""
    r = subprocess.run(["ffmpeg", "-hide_banner", "-i", str(path), "-af",
                        f"loudnorm=I={target}:TP={tp}:LRA=11:print_format=json", "-f", "null", "-"],
                       capture_output=True, text=True, check=True)
    js = json.loads(r.stderr[r.stderr.rindex("{"):r.stderr.rindex("}") + 1])
    tmp = path.with_suffix(".tmp.wav")
    af = (f"loudnorm=I={target}:TP={tp}:LRA=11:measured_I={js['input_i']}:measured_TP={js['input_tp']}:"
          f"measured_LRA={js['input_lra']}:measured_thresh={js['input_thresh']}:offset={js['target_offset']}:linear=true")
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(path), "-af", af, "-ar", str(SR), "-c:a", "pcm_s16le", str(tmp)],
                   check=True)
    tmp.replace(path)


def main():
    words = json.loads((ROOT / "voiceover" / "words.json").read_text())
    beats, music_end = build_grid()
    assert music_end <= TOTAL_S, f"le sezioni durano {music_end:.2f}s, oltre {TOTAL_S}s"

    # posizionamento frasi + controllo sovrapposizioni
    placed = []
    for si, s in enumerate(SECTIONS):
        for lid, bar, beat in s["lines"]:
            t = at(si, bar, beat)
            placed.append(dict(id=lid, t=t, dur=words[lid]["dur"]))
    placed.sort(key=lambda x: x["t"])
    for a, b in zip(placed, placed[1:]):
        gap = b["t"] - (a["t"] + a["dur"])
        flag = "  ⚠ SOVRAPPOSIZIONE" if gap < 0.08 else ""
        print(f"{a['id']} {a['t']:6.2f}→{a['t'] + a['dur']:6.2f}s  pausa prima di {b['id']}: {gap:5.2f}s{flag}")
    last = placed[-1]
    print(f"{last['id']} {last['t']:6.2f}→{last['t'] + last['dur']:6.2f}s  (fine video {TOTAL_S}s)")

    scenes = []
    for si, s in enumerate(SECTIONS):
        for sid, bar, beat in s["scenes"]:
            scenes.append(dict(id=sid, t=at(si, bar, beat), section=s["name"]))
    scenes.sort(key=lambda x: x["t"])

    # musica
    build_music(beats, music_end)
    add_cut_whooshes([s["t"] for s in scenes[1:]], skip=[SECTIONS[1]["start"], SECTIONS[2]["start"], at(4, 2, 0)])
    music_l, music_r = L.copy(), R.copy()
    t_all = np.arange(N) / SR
    master_fade = np.clip(t_all / 0.2, 0, 1) * np.clip((TOTAL_S - t_all) / 1.6, 0, 1)
    peak = np.max(np.abs(np.stack([music_l, music_r]))) + 1e-9
    music_l, music_r = music_l / peak * master_fade, music_r / peak * master_fade

    # voce
    vo = np.zeros(N)
    for p in placed:
        sig = process_vo(ROOT / "public" / "vo" / f"{p['id']}.wav")
        i = int(round(p["t"] * SR))
        seg = sig[: N - i]
        vo[i:i + len(seg)] += seg

    # ducking: musica −10 dB dove c'è voce (attacco 60 ms, rilascio 350 ms)
    active = np.zeros(N)
    for p in placed:
        a = int((p["t"] - 0.05) * SR)
        b = int((p["t"] + p["dur"] + 0.05) * SR)
        active[max(0, a):min(N, b)] = 1
    target = 1 - active * (1 - 10 ** (-10 / 20))
    g = np.empty(N)
    acc = 1.0
    att = 1 - np.exp(-1 / (0.06 * SR))
    rel = 1 - np.exp(-1 / (0.35 * SR))
    for k in range(N):
        coef = att if target[k] < acc else rel
        acc += coef * (target[k] - acc)
        g[k] = acc

    mix_l = music_l * g * 0.62 + vo * 0.95
    mix_r = music_r * g * 0.62 + vo * 0.95
    pk = np.max(np.abs(np.stack([mix_l, mix_r])))
    mix_l, mix_r = np.tanh(mix_l / pk * 1.2) / np.tanh(1.2) * 0.85, np.tanh(mix_r / pk * 1.2) / np.tanh(1.2) * 0.85
    write_wav(ROOT / "public" / "music45.wav", music_l * 0.85, music_r * 0.85)
    write_wav(ROOT / "public" / "soundtrack.wav", mix_l, mix_r)
    loudnorm(ROOT / "public" / "soundtrack.wav")

    # timeline per Remotion (frame assoluti a 60 fps)
    f = lambda t: int(round(t * FPS))  # noqa: E731
    timeline = dict(
        fps=FPS,
        durationInFrames=int(TOTAL_S * FPS),
        sections=[dict(name=s["name"], bpm=s["bpm"], start=f(s["start"]), end=f(s["end"])) for s in SECTIONS],
        beats=[dict(f=f(b["t"]), section=SECTIONS[b["section"]]["name"], bar=b["bar"], beat=b["beat"]) for b in beats],
        scenes=[dict(id=s["id"], start=f(s["t"]), end=f(scenes[i + 1]["t"]) if i + 1 < len(scenes) else int(TOTAL_S * FPS),
                     section=s["section"]) for i, s in enumerate(scenes)],
        lines=[dict(id=p["id"], text=words[p["id"]]["text"], start=f(p["t"]), end=f(p["t"] + p["dur"]),
                    words=[dict(w=w["w"], start=f(p["t"] + w["s"]), end=f(p["t"] + w["e"]))
                           for w in align_words(words[p["id"]]["text"], words[p["id"]]["words"], p["dur"])])
               for p in placed],
    )
    (ROOT / "src" / "timeline.json").write_text(json.dumps(timeline, indent=1, ensure_ascii=False))
    print(f"Sezioni musicali: {music_end:.2f}s · scene: {len(scenes)} · frasi: {len(placed)}")
    print("Scritti public/soundtrack.wav, public/music45.wav, src/timeline.json")


if __name__ == "__main__":
    main()
