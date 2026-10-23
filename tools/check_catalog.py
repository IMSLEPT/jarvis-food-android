"""Controlli di coerenza del catalogo ricette (stesse regole di EngineTest): porzioni massime, nomi unici, segnaposto."""
import json, os, re, sys
from collections import Counter
D = os.path.dirname(os.path.abspath(__file__))
foods = {f["id"]: f for f in json.load(open(os.path.join(D, "../app/src/main/assets/foods.json")))["foods"]}
recs = json.load(open(os.path.join(D, "../app/src/main/assets/recipes.json")))["recipes"]

def max_serving(f):
    g = f["group"]
    if f["id"] == "uova": return 200
    if f["id"] == "albume": return 250
    if f["id"] in ("fette_bisc", "crackers", "gallette"): return 60
    if f["id"] == "piadina": return 220
    if f["id"] == "whey": return 40
    if f["id"] == "brodo_veg": return 600
    if f["id"] == "latte_cocco": return 150
    if g == "olio": return 20
    if g in ("frutta_secca", "semi"): return 40
    if g == "dolci": return 40
    if g in ("condimenti", "grassi"): return 30 if f["kcal"] > 300 else 40
    if g in ("erbe", "spezie"): return 10
    if g == "integratori": return 40
    if f.get("liquid"): return 300
    if g == "latticini": return 40 if f["kcal"] > 300 else 250
    if g in ("cereali", "cereali_integrali"): return 150 if f["kcal"] > 300 else 200
    if g == "tuberi": return 400
    if g == "legumi": return 100 if f["kcal"] > 250 else 300
    if g == "salumi": return 80
    if g.startswith("carne") or g.startswith("pesce"): return 250
    if g == "frutta": return 50 if f["kcal"] > 200 else 300
    if g.startswith("verdura"): return 400
    return 300

bad = Counter(); ex = {}
for r in recs:
    for i in r["ings"]:
        f = foods[i["food"]]
        lim = {"x": i["g"], "v": i["g"] * 0.7, "f": 0}.get(i["role"], i["g"] * 0.4)
        if lim > max_serving(f) + 0.01:
            bad[i["food"]] += 1; ex.setdefault(i["food"], (r["name"], i["g"], max_serving(f)))
for k, v in bad.most_common(): print("OLTRE PORZIONE", k, v, ex[k])
names = Counter(re.sub(r"[^a-z0-9]+", " ", r["name"].lower()) for r in recs)
print("nomi duplicati:", [k for k, v in names.items() if v > 1][:5])
print("totale", len(recs), "foto", sum(1 for r in recs if r.get("image")))
