"""Foto illustrative dei passaggi di ogni ricetta (Pollinations.ai, modello FLUX open-weight)."""
import os, sys, time, json, re, urllib.request, urllib.parse, io
sys.path.insert(0, os.path.dirname(__file__))
from recipes_spec import R
from PIL import Image
OUT = sys.argv[1] if len(sys.argv) > 1 else "steps_out"
os.makedirs(OUT, exist_ok=True)
STYLE = ("close-up food photography of a home cooking step, bright natural daylight, light kitchen counter, "
         "realistic, appetizing, shallow depth of field, no text, no faces")
k = 0
for r in R:
    labels = {i[0]: (i[3] if len(i) > 3 else i[0].replace("_", " ")) for i in r["ings"]}
    for j, st in enumerate(r["steps"]):
        text = re.sub(r"\{([a-z0-9_]+)\}", lambda m: labels.get(m.group(1), m.group(1)), st)
        prompt = f"{r['img']} recipe, step {j + 1}: {text} (Italian recipe '{r['name']}'), {STYLE}"
        url = "https://image.pollinations.ai/prompt/" + urllib.parse.quote(prompt[:900]) + "?" + urllib.parse.urlencode(
            {"width": 640, "height": 640, "model": "flux", "seed": 5000 + k, "nologo": "true", "private": "true"})
        k += 1
        name = f"{r['id']}__s{j}.jpg"
        for attempt in range(3):
            try:
                with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "JarvisFood/1.0"}), timeout=120) as resp:
                    im = Image.open(io.BytesIO(resp.read())).convert("RGB")
                im.save(os.path.join(OUT, name), quality=88); break
            except Exception as e:
                print("retry", name, e, flush=True); time.sleep(20 * (attempt + 1))
        print(name, flush=True)
        time.sleep(16)
