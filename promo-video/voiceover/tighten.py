#!/usr/bin/env python3
"""
Ripristina in public/vo il take scelto per ogni frase (da voiceover/takes) e rimuove solo
le code lunghe dopo l'ultima parola. Whisper tende a chiudere presto le vocali finali,
quindi il margine è ampio (0,3 s) e si taglia solo se la coda supera 0,5 s.
Tagli mirati (CUTS) per artefatti brevi individuati a orecchio/trascrizione.
"""
import json
import subprocess
from pathlib import Path

import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parent.parent
CUTS = {"L07": 1.98, "L11": 1.72}  # secondi: rumore residuo dopo l'ultima parola

words = json.loads((ROOT / "voiceover" / "words.json").read_text())
takes = json.loads((ROOT / "voiceover" / "takes.json").read_text())
for lid, e in words.items():
    base = ROOT / "voiceover" / "takes" / f"{lid}_{takes[lid]['best']}"
    src = base.with_name(base.name + "_cut.wav")
    if not src.exists():
        src = base.with_suffix(".wav")
    a, sr = sf.read(src)
    last = e["words"][-1]["e"]
    dur = len(a) / sr
    end = CUTS.get(lid) or (last + 0.3 if dur - last > 0.5 else dur)
    a = a[: int(end * sr)].copy()
    n = int(0.03 * sr)
    a[-n:] *= np.linspace(1, 0, n)
    tmp = ROOT / "voiceover" / "takes" / f"_{lid}.wav"
    sf.write(tmp, a, sr)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(tmp), "-ar", "48000", str(ROOT / "public" / "vo" / f"{lid}.wav")], check=True)
    tmp.unlink()
    e["dur"] = round(len(a) / sr, 2)
    print(f"{lid}: {src.name} {dur:.2f}s → {e['dur']:.2f}s")
(ROOT / "voiceover" / "words.json").write_text(json.dumps(words, indent=1, ensure_ascii=False))
