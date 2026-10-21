"""Genera app/src/main/assets/foods.json dai dati USDA FoodData Central SR Legacy (2018-04, dominio pubblico).
Uso: python3 tools/build_foods.py <cartella_csv_sr_legacy>
Tutti i valori sono per 100 g di parte edibile, copiati dal dataset senza modifiche (solo arrotondati)."""
import csv, json, sys, os
sys.path.insert(0, os.path.dirname(__file__))
from foods_spec import F

SR = sys.argv[1]
# numero nutriente USDA -> chiave app
NUTR = {
    "208": "kcal", "203": "protein", "205": "carbs", "204": "fat", "291": "fiber", "269": "sugar", "606": "satFat",
    "307": "sodium", "301": "calcium", "303": "iron", "304": "magnesium", "305": "phosphorus", "306": "potassium",
    "309": "zinc", "317": "selenium", "401": "vitC", "404": "thiamin", "405": "riboflavin", "406": "niacin",
    "415": "vitB6", "435": "folate", "418": "vitB12", "320": "vitA", "323": "vitE", "328": "vitD", "430": "vitK",
    "851": "ala", "629": "epa", "621": "dha", "601": "cholesterol",
}
nid2key = {}
for r in csv.DictReader(open(os.path.join(SR, "nutrient.csv"))):
    nbr = (r.get("nutrient_nbr") or "").split(".")[0]
    if nbr in NUTR: nid2key[r["id"]] = NUTR[nbr]
desc = {r["fdc_id"]: r["description"] for r in csv.DictReader(open(os.path.join(SR, "food.csv")))}
want = {str(x["fdc"]) for x in F}
vals = {k: {} for k in want}
for r in csv.DictReader(open(os.path.join(SR, "food_nutrient.csv"))):
    if r["fdc_id"] in want and r["nutrient_id"] in nid2key:
        vals[r["fdc_id"]][nid2key[r["nutrient_id"]]] = float(r["amount"])

out = []
for x in F:
    fid = str(x["fdc"]); v = vals[fid]
    if "kcal" not in v: raise SystemExit(f"Energia mancante per {x['key']} ({fid})")
    o = dict(id=x["key"], name=x["name"], shop=x["shop"], group=x["group"], tags=x["tags"], fdc=int(fid), usda=desc[fid],
             liquid=bool(x.get("liquid", False)))
    for k in ("kcal", "protein", "carbs", "fat"): o[k] = round(v.get(k, 0.0), 2)
    # USDA calcola i carboidrati "per differenza" (fibra inclusa). In Italia/UE si indicano i carboidrati
    # disponibili, senza fibra: li allineo così sono confrontabili con le etichette e con Open Food Facts.
    o["carbs"] = round(max(0.0, v.get("carbs", 0.0) - v.get("fiber", 0.0)), 2)
    o["micro"] = {k: round(val, 3) for k, val in sorted(v.items()) if k not in ("kcal", "protein", "carbs", "fat")}
    if "piece" in x: o["pieceName"], o["pieceGrams"] = x["piece"]
    if "pack" in x: o["pack"] = x["pack"]
    if "syn" in x: o["syn"] = [s.strip() for s in x["syn"].split(",") if s.strip()]
    if "note" in x: o["note"] = x["note"]
    out.append(o)
dst = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "foods.json")
os.makedirs(os.path.dirname(dst), exist_ok=True)
json.dump({"source": "USDA FoodData Central, SR Legacy (aprile 2018), dominio pubblico", "foods": out},
          open(dst, "w"), ensure_ascii=False, separators=(",", ":"))
print(f"{len(out)} alimenti -> {dst}")
if "-v" in sys.argv:
    for o in out:
        print(f"{o['id']:18} {o['kcal']:6.0f} P{o['protein']:5.1f} C{o['carbs']:5.1f} G{o['fat']:5.1f} micro={len(o['micro']):2} | {o['usda'][:70]}")
