"""Genera app/src/main/assets/recipes.json da tools/recipes_spec.py e tools/images_chosen.json.
Controlla che ogni ingrediente esista nel database e che ogni segnaposto {chiave} corrisponda a un ingrediente."""
import json, os, re, sys
D = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, D)
from recipes_spec import R
foods = {f["id"] for f in json.load(open(os.path.join(D, "../app/src/main/assets/foods.json")))["foods"]}
imgs = json.load(open(os.path.join(D, "images_chosen.json"))) if os.path.exists(os.path.join(D, "images_chosen.json")) else {}
out, errors = [], []
for r in R:
    keys = [i[0] for i in r["ings"]]
    for k in keys:
        if k not in foods: errors.append(f"{r['id']}: alimento sconosciuto {k}")
    for st in r["steps"]:
        for ph in re.findall(r"\{([a-z0-9_]+)\}", st):
            if ph not in keys: errors.append(f"{r['id']}: segnaposto {{{ph}}} senza ingrediente")
    for k in keys:
        if not any("{" + k + "}" in st for st in r["steps"]) and k not in ("sale", "pepe"):
            pass  # alcuni aromi sono citati senza grammatura (es. "aglio"): ammesso
    o = dict(id=r["id"], name=r["name"], meal=r["meal"], minutes=r["minutes"], steps=r["steps"], tip=r.get("tip", ""), cuisine=r.get("cuisine", "italiana"), methods=r.get("methods", []), difficulty=r.get("diff", 1),
             ings=[dict(food=i[0], g=float(i[1]), role=i[2], **({"label": i[3]} if len(i) > 3 else {})) for i in r["ings"]])
    im = imgs.get(r["id"])
    if im:
        o["image"] = f"recipes/{r['id']}.webp"
        o["credit"] = dict(author=im.get("author", ""), license=im.get("license", ""), source=im.get("source", ""), title=im.get("title", ""))
        st = im.get("steps", [])
        if len(st) != len(r["steps"]): errors.append(f"{r['id']}: foto passaggi {len(st)} != passaggi {len(r['steps'])}")
        o["stepImages"] = [s["path"] if s else None for s in st]
    out.append(o)
if errors: raise SystemExit("\n".join(errors))
json.dump({"recipes": out}, open(os.path.join(D, "../app/src/main/assets/recipes.json"), "w"), ensure_ascii=False, separators=(",", ":"))
print(len(out), "ricette;", sum(1 for o in out if "image" in o), "con foto")
