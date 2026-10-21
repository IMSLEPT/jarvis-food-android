"""Genera una foto illustrativa per ogni ricetta con Pollinations.ai (modello open-weight FLUX), gratis e senza chiave.
Eseguito in GitHub Actions. Le immagini sono illustrative e vengono etichettate come generate con AI nell'app."""
import os, sys, time, json, urllib.request, urllib.parse, io
sys.path.insert(0, os.path.dirname(__file__))
from recipes_spec import R
from PIL import Image
OUT = sys.argv[1] if len(sys.argv) > 1 else "gen_out"
os.makedirs(OUT, exist_ok=True)
STYLE = ("professional food photography, appetizing, natural soft daylight, shallow depth of field, served on a ceramic plate "
         "on a light wooden table, Italian home cooking, realistic, high detail, no text, no people, no hands")
meta = {}
for i, r in enumerate(R):
    prompt = f"{r['img']}, {r['name']} (Italian dish), {STYLE}"
    url = "https://image.pollinations.ai/prompt/" + urllib.parse.quote(prompt) + "?" + urllib.parse.urlencode(
        {"width": 1024, "height": 768, "model": "flux", "seed": 1000 + i, "nologo": "true", "private": "true", "enhance": "false"})
    ok = False
    for attempt in range(4):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "JarvisFood/1.0"})
            with urllib.request.urlopen(req, timeout=120) as resp:
                b = resp.read()
            im = Image.open(io.BytesIO(b)).convert("RGB")
            im.save(os.path.join(OUT, f"{r['id']}.jpg"), quality=90)
            meta[r["id"]] = dict(prompt=prompt, w=im.width, h=im.height)
            ok = True
            break
        except Exception as e:
            print("retry", r["id"], e, flush=True); time.sleep(20 * (attempt + 1))
    print(r["id"], "ok" if ok else "FAIL", flush=True)
    time.sleep(16)
json.dump(meta, open(os.path.join(OUT, "gen_meta.json"), "w"), indent=1)
# foglio provini
from PIL import ImageDraw
files = [f for f in sorted(os.listdir(OUT)) if f.endswith(".jpg")]
for k in range(0, len(files), 12):
    chunk = files[k:k + 12]
    sheet = Image.new("RGB", (4 * 330, 3 * 270), "white"); d = ImageDraw.Draw(sheet)
    for j, f in enumerate(chunk):
        t = Image.open(os.path.join(OUT, f)); t.thumbnail((320, 240))
        x, y = (j % 4) * 330, (j // 4) * 270
        sheet.paste(t, (x + 5, y + 22)); d.text((x + 6, y + 4), f[:-4], fill="black")
    sheet.save(os.path.join(OUT, f"SHEET_{k // 12}.jpg"), quality=82)
