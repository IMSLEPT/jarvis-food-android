"""Genera (in parallelo su più runner) le foto dei piatti e dei passaggi con Pollinations.ai / FLUX.
Uso: gen_all.py <cartella> <shard> <numero_shard>"""
import os, sys, time, re, urllib.request, urllib.parse, io
sys.path.insert(0, os.path.dirname(__file__))
from recipes_spec import R
from PIL import Image
OUT, SH, N = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
os.makedirs(OUT, exist_ok=True)
HERO = ("professional food photography, appetizing, natural soft daylight, shallow depth of field, served on a ceramic plate "
        "on a light wooden table, Italian home cooking, realistic, high detail, no text, no people, no hands")
STEP = ("close-up food photography of a home cooking step, bright natural daylight, light kitchen counter, "
        "realistic, appetizing, shallow depth of field, no text, no faces")
jobs = []
for i, r in enumerate(R):
    jobs.append((f"{r['id']}.jpg", f"{r['img']}, {r['name']} (Italian dish), {HERO}", 1024, 768, 1000 + i))
    labels = {x[0]: (x[3] if len(x) > 3 else x[0].replace("_", " ")) for x in r["ings"]}
    for j, st in enumerate(r["steps"]):
        text = re.sub(r"\{([a-z0-9_]+)\}", lambda m: labels.get(m.group(1), m.group(1)), st)
        jobs.append((f"{r['id']}__s{j}.jpg", f"{r['img']} recipe, step {j+1}: {text} (Italian recipe '{r['name']}'), {STEP}", 640, 640, 5000 + i * 10 + j))
mine = jobs[SH::N]
print(len(jobs), "totali,", len(mine), "in questo shard", flush=True)
for name, prompt, w, h, seed in mine:
    url = "https://image.pollinations.ai/prompt/" + urllib.parse.quote(prompt[:900]) + "?" + urllib.parse.urlencode(
        {"width": w, "height": h, "model": "flux", "seed": seed, "nologo": "true", "private": "true"})
    t0 = time.time()
    for attempt in range(3):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "JarvisFood/1.0"}), timeout=90) as resp:
                im = Image.open(io.BytesIO(resp.read())).convert("RGB")
            im.save(os.path.join(OUT, name), quality=88); break
        except Exception as e:
            print("retry", name, e, flush=True); time.sleep(15)
    print(name, round(time.time() - t0), "s", flush=True)
    time.sleep(max(0, 16 - (time.time() - t0)))
