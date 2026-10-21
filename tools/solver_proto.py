import json, sys, os, math
sys.path.insert(0, os.path.dirname(__file__))
from recipes_spec import R
foods = {f["id"]: f for f in json.load(open(os.path.join(os.path.dirname(__file__), "../app/src/main/assets/foods.json")))["foods"]}
BOUNDS = {"c": (0.4, 2.2), "p": (0.4, 2.2), "f": (0.0, 2.5), "v": (0.7, 1.6), "x": (1, 1)}
W = {"kcal": 3.0, "p": 2.0, "c": 1.0, "f": 1.2}
LAM = 0.15

def nut(key, g):
    f = foods[key]; k = g / 100
    return (f["kcal"]*k, f["protein"]*k, f["carbs"]*k, f["fat"]*k)

def step_of(key, role):
    f = foods[key]
    if key == "uova": return 50  # uovo intero
    if role == "x": return 1
    if f.get("liquid"): return 10
    return 5

def objective(g, ings, t, s):
    tot = [0,0,0,0]
    for (key, d, role, *_), gi in zip(ings, g):
        n = nut(key, gi); tot = [a+b for a,b in zip(tot,n)]
    sc = [max(t[0],1), max(t[1],5), max(t[2],5), max(t[3],3)]
    e = W["kcal"]*((tot[0]-t[0])/sc[0])**2 + W["p"]*((tot[1]-t[1])/sc[1])**2 + W["c"]*((tot[2]-t[2])/sc[2])**2 + W["f"]*((tot[3]-t[3])/sc[3])**2
    reg = sum(((gi - s*d)/(s*d))**2 for (key,d,role,*_), gi in zip(ings,g) if role != "x" and d > 0)
    return e + LAM*reg, tot

def solve(ings, t):
    base = [d for (_, d, *_ ) in ings]
    k0 = sum(nut(k, d)[0] for (k, d, *_) in ings)
    s = max(0.5, min(2.0, t[0]/k0)) if k0 > 0 else 1
    lo = [d*BOUNDS[r][0]*(s if r in "cpf" else 1) if r!="x" else d for (k,d,r,*_) in ings]
    hi = [d*BOUNDS[r][1]*(s if r in "cpf" else 1) if r!="x" else d for (k,d,r,*_) in ings]
    lo = [d*BOUNDS[r][0] if r != "x" else d for (k,d,r,*_) in ings]
    hi = [d*BOUNDS[r][1] if r != "x" else d for (k,d,r,*_) in ings]
    g = [min(max(d*s if r in "cpf" else d, l), h) for (k,d,r,*_), l, h in zip(ings, lo, hi)]
    # discesa coordinata su griglia: passi decrescenti
    for stepmul in (8, 4, 2, 1):
        improved = True; it = 0
        while improved and it < 200:
            improved = False; it += 1
            for i, (k, d, r, *_) in enumerate(ings):
                if r == "x": continue
                st = step_of(k, r) * (stepmul if k != "uova" else 1)
                best, _ = objective(g, ings, t, s)
                for delta in (st, -st):
                    ng = g[:]; ng[i] = min(max(g[i] + delta, lo[i]), hi[i])
                    if k == "uova": ng[i] = max(50, round(ng[i]/50)*50)
                    o, _ = objective(ng, ings, t, s)
                    if o < best - 1e-9: g, best, improved = ng, o, True
    # arrotonda alla griglia finale
    g = [ (max(50, round(gi/50)*50) if k=="uova" else (round(gi/step_of(k,r))*step_of(k,r) if r!="x" else gi)) for (k,d,r,*_), gi in zip(ings,g)]
    return g, objective(g, ings, t, s)[1]

if __name__ == "__main__":
    day = dict(kcal=2600, p=150, f=78)
    day["c"] = (day["kcal"] - day["p"]*4 - day["f"]*9)/4
    split = {"colazione": .25, "pranzo": .35, "spuntino": .10, "cena": .30}
    worst = []
    for rec in R:
        sh = split[rec["meal"]]
        t = (day["kcal"]*sh, day["p"]*sh, day["c"]*sh, day["f"]*sh)
        g, tot = solve(rec["ings"], t)
        err = [(a-b)/b*100 for a,b in zip(tot, t)]
        worst.append((abs(err[0]), rec["id"]))
        print(f"{rec['id']:24} kcal {tot[0]:5.0f}/{t[0]:4.0f} ({err[0]:+5.1f}%) P {tot[1]:4.0f}/{t[1]:3.0f} ({err[1]:+4.0f}%) C {tot[2]:4.0f}/{t[2]:3.0f} ({err[2]:+4.0f}%) F {tot[3]:3.0f}/{t[3]:3.0f} ({err[3]:+4.0f}%)  " +
              " ".join(f"{k}:{gi:.0f}" for (k,*_), gi in zip(rec['ings'], g)))
