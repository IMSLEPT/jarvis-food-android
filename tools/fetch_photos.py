"""Cerca foto con licenza libera per piatti e passaggi: Pexels (se è presente PEXELS_KEY) e Openverse.
Scarica fino a 6 candidati per immagine e crea fogli di provini numerati per la scelta. Eseguito in GitHub Actions."""
import json, os, sys, time, io, urllib.request, urllib.parse
from concurrent.futures import ThreadPoolExecutor
sys.path.insert(0, os.path.dirname(__file__))
from recipes_spec import R
from step_queries import S
from PIL import Image, ImageDraw
OUT = sys.argv[1]; os.makedirs(OUT, exist_ok=True)
KEY = os.environ.get("PEXELS_KEY", "").strip()
PKEY = os.environ.get("PIXABAY_KEY", "").strip()
UA = {"User-Agent": "JarvisFood/1.0 (personal app)"}
log = open(os.path.join(OUT, "log.txt"), "w")

def get(url, headers=None, timeout=20):
    req = urllib.request.Request(url, headers={**UA, **(headers or {})})
    with urllib.request.urlopen(req, timeout=timeout) as r: return r.read()

def pexels(q, n=6):
    u = "https://api.pexels.com/v1/search?" + urllib.parse.urlencode({"query": q, "per_page": n, "orientation": "landscape"})
    d = json.loads(get(u, {"Authorization": KEY}))
    return [dict(src="pexels", thumb=p["src"]["large"], author=p.get("photographer", ""), license="Pexels License",
                 page=p.get("url", ""), title=p.get("alt", "")) for p in d.get("photos", [])]

def pixabay(q, n=8):
    u = "https://pixabay.com/api/?" + urllib.parse.urlencode({"key": PKEY, "q": q, "image_type": "photo", "category": "food",
        "per_page": max(3, n), "safesearch": "true", "orientation": "horizontal", "min_width": 800})
    d = json.loads(get(u))
    return [dict(src="pixabay", thumb=h.get("largeImageURL") or h.get("webformatURL"), author=h.get("user", ""),
                 license="Pixabay Content License", page=h.get("pageURL", ""), title=h.get("tags", "")) for h in d.get("hits", [])]

def openverse(q, n=6):
    u = "https://api.openverse.org/v1/images/?" + urllib.parse.urlencode({"q": q, "page_size": n, "license_type": "commercial,modification"})
    d = json.loads(get(u))
    return [dict(src="openverse", thumb=it.get("url"), author=it.get("creator") or "", license=f"CC {(it.get('license') or '').upper()} {it.get('license_version') or ''}".strip(),
                 page=it.get("foreign_landing_url") or "", title=it.get("title") or "") for it in d.get("results", [])]

def search(q):
    res = []
    if PKEY:
        try: res += pixabay(q)
        except Exception as e: print("pixabay", q, e, file=log, flush=True)
        time.sleep(0.7)
    if KEY and len(res) < 4:
        try: res += pexels(q)
        except Exception as e: print("pexels", q, e, file=log, flush=True)
    if len(res) < 4:
        try: res += openverse(q)
        except Exception as e: print("openverse", q, e, file=log, flush=True)
        time.sleep(1.2)
    return res[:8]

def dl(c):
    try:
        im = Image.open(io.BytesIO(get(c["thumb"]))).convert("RGB")
        if min(im.size) < 300: return None
        im.thumbnail((1200, 1200)); return im
    except Exception as e:
        print("dl", c["thumb"], e, file=log, flush=True); return None

tasks = []
done = set(json.load(open(os.path.join(os.path.dirname(__file__), "images_chosen.json"))).keys()) if os.path.exists(os.path.join(os.path.dirname(__file__), "images_chosen.json")) else set()
for r in R:
    if r["id"] in done: continue   # ricette che hanno già la foto scelta
    tasks.append((r["id"], r["img"]))
    short = " ".join(r["img"].split()[:3])
    if short != r["img"]: tasks.append((f"{r['id']}__alt", short))
    for j, q in enumerate(S.get(r["id"], [])):
        if q: tasks.append((f"{r['id']}__s{j}", q))
meta = {}
def run(t):
    name, q = t
    cands = search(q)
    with ThreadPoolExecutor(6) as ex: ims = list(ex.map(dl, cands))
    kept = []
    for c, im in zip(cands, ims):
        if im is None: continue
        c["file"] = f"{name}__{len(kept)}.jpg"; im.save(os.path.join(OUT, c["file"]), quality=88); kept.append((c, im))
    return name, q, kept
with ThreadPoolExecutor(3 if KEY else 2) as ex:
    for name, q, kept in ex.map(run, tasks):
        meta[name] = dict(query=q, cands=[c for c, _ in kept])
        print(name, len(kept), flush=True)
json.dump(meta, open(os.path.join(OUT, "meta.json"), "w"), ensure_ascii=False, indent=1)
# fogli provini: 6 righe (immagini) per foglio, 6 candidati per riga
names = list(meta.keys())
for k in range(0, len(names), 6):
    chunk = names[k:k + 6]
    sheet = Image.new("RGB", (6 * 210 + 200, len(chunk) * 170), "white"); d = ImageDraw.Draw(sheet)
    for row, nm in enumerate(chunk):
        d.text((4, row * 170 + 4), nm[:30], fill="black"); d.text((4, row * 170 + 20), meta[nm]["query"][:30], fill="gray")
        for i, c in enumerate(meta[nm]["cands"]):
            t = Image.open(os.path.join(OUT, c["file"])); t.thumbnail((200, 150))
            sheet.paste(t, (200 + i * 210, row * 170 + 14)); d.text((204 + i * 210, row * 170 + 2), str(i), fill="red")
    sheet.save(os.path.join(OUT, f"SHEET_{k // 6:03d}.jpg"), quality=80)
