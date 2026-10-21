"""Converte le foto scelte (Pixabay, Pixabay Content License) in WebP negli asset dell'app
e scrive tools/images_chosen.json con autore e pagina di ogni foto.
Uso: python3 tools/make_images.py <choices.py> <cartella_candidati_1> <cartella_candidati_2>
choices.py definisce C = {ricetta: (hero, [passaggi...])}; ogni voce è un indice (candidato dello stesso nome),
"nome:idx" (altro nome, cartella 1), "X:nome:idx" (cartella 2) oppure None."""
import json, os, sys, importlib.util
from PIL import Image, ImageOps
D = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, D)
from step_queries import S
spec = importlib.util.spec_from_file_location("choices", sys.argv[1]); ch = importlib.util.module_from_spec(spec); spec.loader.exec_module(ch)
dirs = [sys.argv[2], sys.argv[3]]
metas = [json.load(open(os.path.join(d, "meta.json"))) for d in dirs]
OUT = os.path.join(D, "../app/src/main/assets/recipes"); os.makedirs(OUT, exist_ok=True)

def resolve(own, v):
    if v is None: return None
    k = 0
    if isinstance(v, int): name, idx = own, v
    else:
        p = v.split(":")
        if p[0] == "X": k, p = 1, p[1:]
        name, idx = p[0], int(p[1])
    c = metas[k][name]["cands"][idx]
    return os.path.join(dirs[k], c["file"]), c

def save(src, dst, w, ratio):
    im = ImageOps.exif_transpose(Image.open(src)).convert("RGB")
    im = ImageOps.fit(im, (w, int(w / ratio)), Image.LANCZOS, centering=(0.5, 0.5))
    im.save(dst, "WEBP", quality=80, method=6)

def credit(c): return dict(author=c["author"], license=c["license"], source=c["page"], title=c["title"])

out, total = {}, 0
for rid, (hero, steps) in ch.C.items():
    h = resolve(f"{rid}", hero)
    if not h: raise SystemExit(f"manca la foto principale di {rid}")
    save(h[0], os.path.join(OUT, f"{rid}.webp"), 960, 4 / 3); total += 1
    imgs = []
    for j, q in enumerate(S[rid]):
        if q is None:   # passaggio finale (impiattamento): foto del piatto
            imgs.append(dict(path=f"recipes/{rid}.webp", credit=None)); continue
        r = resolve(f"{rid}__s{j}", steps[j] if j < len(steps) else None)
        if not r: imgs.append(None); continue
        save(r[0], os.path.join(OUT, f"{rid}_s{j}.webp"), 560, 4 / 3); total += 1
        imgs.append(dict(path=f"recipes/{rid}_s{j}.webp", credit=credit(r[1])))
    out[rid] = dict(credit(h[1]), steps=imgs)
json.dump(out, open(os.path.join(D, "images_chosen.json"), "w"), ensure_ascii=False, indent=1)
size = sum(os.path.getsize(os.path.join(OUT, f)) for f in os.listdir(OUT))
print(total, "immagini,", round(size / 1e6, 1), "MB")
