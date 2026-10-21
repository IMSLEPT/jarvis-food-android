"""Cerca foto con licenza libera per ogni ricetta (Openverse + Wikimedia Commons) e crea fogli di provini.
Eseguito in GitHub Actions (il runner ha accesso a internet)."""
import json, os, sys, time, urllib.request, urllib.parse, io
sys.path.insert(0, os.path.dirname(__file__))
from recipes_spec import R
from PIL import Image, ImageDraw

OUT = sys.argv[1] if len(sys.argv) > 1 else "img_out"
os.makedirs(OUT, exist_ok=True)
UA = {"User-Agent": "JarvisFood/1.0 (personal app; contact ale.calamai97@gmail.com)"}

def get(url, raw=False, timeout=12):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=timeout) as r:
        b = r.read()
    return b if raw else json.loads(b)

def openverse(q):
    u = "https://api.openverse.org/v1/images/?" + urllib.parse.urlencode(
        {"q": q, "page_size": 10, "mature": "false", "license": "cc0,pdm,by,by-sa", "aspect_ratio": "wide,square"})
    res = []
    for it in get(u).get("results", []):
        res.append(dict(src="openverse", url=it.get("url"), thumb=it.get("thumbnail") or it.get("url"), title=it.get("title"),
                        creator=it.get("creator"), license=(it.get("license") or "").upper(), license_version=it.get("license_version"),
                        page=it.get("foreign_landing_url"), w=it.get("width"), h=it.get("height")))
    return res

def commons(q):
    u = "https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode({
        "action": "query", "format": "json", "generator": "search", "gsrsearch": q + " filetype:bitmap", "gsrnamespace": 6,
        "gsrlimit": 8, "prop": "imageinfo", "iiprop": "url|extmetadata|size", "iiurlwidth": 900})
    res = []
    for p in (get(u).get("query", {}).get("pages", {}) or {}).values():
        ii = (p.get("imageinfo") or [{}])[0]; md = ii.get("extmetadata", {})
        val = lambda k: (md.get(k, {}) or {}).get("value", "")
        res.append(dict(src="commons", url=ii.get("thumburl") or ii.get("url"), thumb=ii.get("thumburl"), title=p.get("title"),
                        creator=val("Artist"), license=val("LicenseShortName"), page=ii.get("descriptionurl"),
                        w=ii.get("width"), h=ii.get("height")))
    return res

from concurrent.futures import ThreadPoolExecutor

def download(c):
    for u in [c.get("thumb"), c.get("url")]:
        if not u: continue
        try:
            im = Image.open(io.BytesIO(get(u, raw=True, timeout=10))).convert("RGB")
            if min(im.size) >= 280: return im
        except Exception as e:
            pass
    return None

def process(rec):
    cands = []
    for fn in (openverse, commons):
        try: cands += fn(rec["img"])
        except Exception as e: print("ERR", rec["id"], fn.__name__, e, flush=True)
    cands = cands[:14]
    with ThreadPoolExecutor(8) as ex:
        ims = list(ex.map(download, cands))
    kept, tiles = [], []
    for c, im in zip(cands, ims):
        if im is None: continue
        idx = len(kept)
        im.thumbnail((1000, 1000)); im.save(os.path.join(OUT, f"{rec['id']}__{idx}.jpg"), quality=86)
        c["file"] = f"{rec['id']}__{idx}.jpg"; kept.append(c)
        t = im.copy(); t.thumbnail((300, 300)); tiles.append(t)
    if tiles:
        sheet = Image.new("RGB", (4 * 310, ((len(tiles) + 3) // 4) * 330), "white")
        d = ImageDraw.Draw(sheet)
        for i, t in enumerate(tiles):
            x, y = (i % 4) * 310, (i // 4) * 330
            sheet.paste(t, (x + 5, y + 25)); d.text((x + 8, y + 5), f"{i} {kept[i]['src']} {kept[i]['license']}", fill="black")
        sheet.save(os.path.join(OUT, f"SHEET_{rec['id']}.jpg"), quality=80)
    print(rec["id"], len(kept), flush=True)
    return rec["id"], kept

meta = {}
with ThreadPoolExecutor(4) as ex:
    for rid, kept in ex.map(process, R):
        meta[rid] = kept
json.dump(meta, open(os.path.join(OUT, "meta.json"), "w"), ensure_ascii=False, indent=1)
