#!/usr/bin/env python3
"""
Voce fuori campo del promo con Chatterbox Multilingual (Resemble AI, MIT, open source).
https://github.com/resemble-ai/chatterbox

Per ogni riga di script.json genera più take, li trascrive con faster-whisper
e sceglie quello con meno errori rispetto al testo (a parità, il più naturale per durata).
Poi salva:
  public/vo/<ID>.wav            take scelto (48 kHz mono, silenzi ai bordi rimossi)
  voiceover/takes.json          tutte le take con punteggio
  voiceover/words.json          parole con tempi (secondi, relativi alla riga)

Uso (dentro un venv con chatterbox-tts e faster-whisper):
  python voiceover/generate_vo.py --ref voiceover/ref/it_m1.flac --takes 3
"""
from __future__ import annotations

import argparse
import json
import re
import unicodedata
from pathlib import Path

import numpy as np
import torch
import torchaudio as ta

ROOT = Path(__file__).resolve().parent.parent
SCRIPT = ROOT / "voiceover" / "script.json"
OUT_DIR = ROOT / "public" / "vo"
TAKES_DIR = ROOT / "voiceover" / "takes"

# Parametri per tono: exaggeration = espressività, cfg_weight più basso = eloquio più lento e posato
TONES = {
    "calmo": dict(exaggeration=0.45, cfg_weight=0.45, temperature=0.75),
    "deciso": dict(exaggeration=0.60, cfg_weight=0.40, temperature=0.75),
    "caldo": dict(exaggeration=0.55, cfg_weight=0.45, temperature=0.75),
    "energico": dict(exaggeration=0.60, cfg_weight=0.50, temperature=0.80),
    "incalzante": dict(exaggeration=0.65, cfg_weight=0.50, temperature=0.80),
    "ispirato": dict(exaggeration=0.55, cfg_weight=0.40, temperature=0.75),
    "firma": dict(exaggeration=0.50, cfg_weight=0.35, temperature=0.70),
}


def norm(s: str) -> list[str]:
    s = unicodedata.normalize("NFKD", s.lower())
    s = "".join(c for c in s if not unicodedata.combining(c))
    s = s.replace("'", " ")
    return re.findall(r"[a-z0-9]+", s)


def wer(ref: list[str], hyp: list[str]) -> float:
    d = np.zeros((len(ref) + 1, len(hyp) + 1), dtype=int)
    d[:, 0] = np.arange(len(ref) + 1)
    d[0, :] = np.arange(len(hyp) + 1)
    for i in range(1, len(ref) + 1):
        for j in range(1, len(hyp) + 1):
            d[i, j] = min(d[i - 1, j] + 1, d[i, j - 1] + 1, d[i - 1, j - 1] + (ref[i - 1] != hyp[j - 1]))
    return d[-1, -1] / max(1, len(ref))


def trim_silence(wav: np.ndarray, sr: int, thresh_db: float = -55, pad: float = 0.10) -> np.ndarray:
    win = int(0.01 * sr)
    frames = len(wav) // win
    if frames == 0:
        return wav
    rms = np.sqrt(np.mean(wav[: frames * win].reshape(frames, win) ** 2, axis=1) + 1e-12)
    db = 20 * np.log10(rms)
    loud = np.where(db > thresh_db)[0]
    if len(loud) == 0:
        return wav
    a = max(0, loud[0] * win - int(pad * sr))
    b = min(len(wav), (loud[-1] + 1) * win + int(pad * sr))
    return wav[a:b]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--ref", required=True, help="Campione di voce di riferimento (es. prompt italiano ufficiale)")
    ap.add_argument("--takes", type=int, default=3)
    ap.add_argument("--only", nargs="*", help="Rigenera solo questi ID")
    ap.add_argument("--whisper", default="small")
    args = ap.parse_args()

    from chatterbox.mtl_tts import ChatterboxMultilingualTTS
    from faster_whisper import WhisperModel

    torch.set_num_threads(max(1, torch.get_num_threads()))
    tts = ChatterboxMultilingualTTS.from_pretrained(device="cpu")
    asr = WhisperModel(args.whisper, device="cpu", compute_type="int8")

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    TAKES_DIR.mkdir(parents=True, exist_ok=True)
    lines = json.loads(SCRIPT.read_text())["lines"]
    takes_path = ROOT / "voiceover" / "takes.json"
    words_path = ROOT / "voiceover" / "words.json"
    all_takes = json.loads(takes_path.read_text()) if takes_path.exists() else {}
    all_words = json.loads(words_path.read_text()) if words_path.exists() else {}

    for line in lines:
        lid = line["id"]
        if args.only and lid not in args.only:
            continue
        params = TONES[line["tone"]]
        ref_words = norm(line["say"])
        results = []
        for k in range(args.takes):
            torch.manual_seed(1000 + k * 17)
            wav = tts.generate(line["say"], language_id="it", audio_prompt_path=args.ref, **params)
            mono = wav.squeeze(0).cpu().numpy().astype(np.float32)
            mono = trim_silence(mono, tts.sr)
            path = TAKES_DIR / f"{lid}_{k}.wav"
            ta.save(str(path), torch.from_numpy(mono).unsqueeze(0), tts.sr)
            segs, _ = asr.transcribe(str(path), language="it", word_timestamps=True, beam_size=5)
            words = [dict(w=w.word.strip(), s=round(w.start, 3), e=round(w.end, 3)) for s in segs for w in s.words]
            hyp = norm(" ".join(w["w"] for w in words))
            score = wer(ref_words, hyp)
            dur = len(mono) / tts.sr
            results.append(dict(take=k, path=str(path.relative_to(ROOT)), wer=round(score, 3), dur=round(dur, 2),
                                text=" ".join(w["w"] for w in words), words=words))
            print(f"{lid} take {k}: wer={score:.2f} dur={dur:.2f}s → {results[-1]['text']}", flush=True)
        # miglior take: WER minimo; a parità, durata più vicina alla mediana (evita take strascicati o affrettati)
        med = float(np.median([r["dur"] for r in results]))
        best = min(results, key=lambda r: (r["wer"], abs(r["dur"] - med)))
        all_takes[lid] = dict(best=best["take"], takes=[{k: v for k, v in r.items() if k != "words"} for r in results])
        all_words[lid] = dict(dur=best["dur"], words=best["words"], text=line["say"])
        # esporta a 48 kHz
        w, sr = ta.load(str(ROOT / best["path"]))
        w = ta.functional.resample(w, sr, 48000)
        ta.save(str(OUT_DIR / f"{lid}.wav"), w, 48000)
        takes_path.write_text(json.dumps(all_takes, indent=1, ensure_ascii=False))
        words_path.write_text(json.dumps(all_words, indent=1, ensure_ascii=False))
        print(f"{lid}: scelta take {best['take']} (wer={best['wer']}, {best['dur']}s)", flush=True)


if __name__ == "__main__":
    main()
