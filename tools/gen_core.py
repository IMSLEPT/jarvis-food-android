# Nucleo del generatore di ricette: controlli anti-duplicato, controlli nutrizionali e frasi di preparazione
# per ingrediente, cosi' i passaggi di ogni ricetta generata sono corretti (tempi e metodi reali).
import json, os, re, unicodedata
from recipes_spec import r as _r, R

D = os.path.dirname(os.path.abspath(__file__))
FOODS = {f["id"]: f for f in json.load(open(os.path.join(D, "../app/src/main/assets/foods.json")))["foods"]}

def norm(s):
    s = unicodedata.normalize("NFD", s.lower())
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9]+", " ", s).strip()

_NAMES, _SIGS, _IDS = set(), set(), set()
BAD = []   # ricette scartate e motivo
STATS = {}

def _sig(ings, methods, meal):
    return (frozenset(i[0] for i in ings if i[2] != "x"), tuple(sorted(methods)), meal)

def _init():
    if _NAMES: return
    for x in R:
        _NAMES.add(norm(x["name"])); _IDS.add(x["id"]); _SIGS.add(_sig(x["ings"], x["methods"], x["meal"]))

def kcal_of(ings):
    return sum(FOODS[i[0]]["kcal"] * i[1] / 100 for i in ings)

def add(name, meal, minutes, ings, steps, tip="", cuisine="italiana", methods=(), diff=1, img=None, fam=""):
    """Aggiunge una ricetta se il nome e la combinazione ingredienti/metodo/pasto sono nuovi e se le calorie sono plausibili."""
    _init()
    key = norm(name)
    if key in _NAMES: BAD.append((name, "nome duplicato")); return False
    ings = [tuple([i[0], round(i[1], 1) if i[1] < 10 else int(round(i[1]))] + list(i[2:])) for i in ings]
    keys = [i[0] for i in ings]
    if len(set(keys)) != len(keys): BAD.append((name, "ingrediente ripetuto")); return False
    for k in keys:
        if k not in FOODS: BAD.append((name, "alimento sconosciuto " + k)); return False
    sig = _sig(ings, methods, meal)
    if sig in _SIGS: BAD.append((name, "stessi ingredienti e metodo")); return False
    k = kcal_of(ings)
    lim = {"colazione": (120, 900), "pranzo": (250, 1050), "cena": (250, 1050), "spuntino": (15, 700), "dessert": (60, 700)}[meal]
    if not lim[0] <= k <= lim[1]: BAD.append((name, f"kcal {k:.0f} fuori da {lim}")); return False
    for st in steps:
        for ph in re.findall(r"\{([a-z0-9_]+)\}", st):
            if ph not in keys: BAD.append((name, "segnaposto senza ingrediente " + ph)); return False
    rid = re.sub(r"\s+", "_", key)[:60]
    n = 2; base = rid
    while rid in _IDS: rid = f"{base}_{n}"; n += 1
    _NAMES.add(key); _IDS.add(rid); _SIGS.add(sig)
    STATS[fam] = STATS.get(fam, 0) + 1
    _r(rid, name, meal, minutes, ings, steps, img or name, tip, cuisine, tuple(methods), diff)
    return True

def pick(seq, n, seed=0):
    """Sceglie n elementi in modo deterministico e ben distribuito (senza casualita': stesso risultato a ogni build)."""
    seq = list(seq)
    if n >= len(seq): return seq
    step = len(seq) / n
    off = (seed * 7) % max(1, int(step)) if step >= 1 else 0
    return [seq[min(len(seq) - 1, int(i * step + off))] for i in range(n)]

def alt(i, a, b):
    return a if i % 2 == 0 else b

# ---------------- carboidrati: come si cuociono (pesi a crudo) ----------------
# chiave: (frase di cottura, minuti, prefisso nome "di")
def cook_carb(k):
    T = {
     "pasta": "Cuoci {pasta} in abbondante acqua salata per il tempo indicato sulla confezione e scolala al dente, tenendo da parte un mestolino di acqua di cottura.",
     "pasta_int": "Cuoci {pasta_int} in abbondante acqua salata per il tempo indicato sulla confezione (di solito 10-12 minuti) e scolala al dente, conservando un mestolino di acqua di cottura.",
     "pasta_fresca": "Cuoci {pasta_fresca} in acqua bollente salata per 2-4 minuti (segui la confezione) e scolala.",
     "riso": "Cuoci {riso} in acqua bollente salata per 12-14 minuti, scolalo e lascialo intiepidire.",
     "riso_int": "Cuoci {riso_int} in acqua bollente salata per 30-35 minuti, scolalo e lascialo intiepidire.",
     "basmati": "Sciacqua {basmati} finché l'acqua è limpida, poi cuocilo in acqua salata per 10-12 minuti e scolalo.",
     "orzo": "Cuoci {orzo} in acqua bollente salata per 25-30 minuti (segui la confezione) e scolalo.",
     "farro": "Cuoci {farro} in acqua bollente salata per 20-25 minuti (se è perlato bastano 15) e scolalo.",
     "quinoa": "Sciacqua {quinoa} sotto l'acqua corrente, poi cuocila in acqua salata per 12-15 minuti finché i germi si aprono; scolala e sgranala con una forchetta.",
     "couscous": "Versa {couscous} in una ciotola, copri con acqua bollente salata a filo (stesso volume), chiudi con un piatto e lascia gonfiare 5 minuti; sgrana con una forchetta.",
     "bulgur": "Cuoci {bulgur} in acqua bollente salata per 10-12 minuti, scolalo e sgranalo con una forchetta.",
     "miglio": "Sciacqua {miglio}, poi cuocilo in acqua salata per 18-20 minuti, scolalo e sgranalo con una forchetta.",
     "grano_saraceno": "Sciacqua {grano_saraceno} e cuocilo in acqua salata per 12-15 minuti, poi scolalo e sgranalo.",
     "patate": "Lessa {patate} con la buccia in acqua salata per 20-25 minuti (o finché una forchetta entra facilmente), poi sbuccialo e taglialo a pezzi.",
     "patate_dolci": "Cuoci {patate_dolci} a pezzi in forno a 200 °C per 25-30 minuti, finché sono morbide dentro.",
     "noodles_riso": "Metti {noodles_riso} in ammollo in acqua molto calda per 5-8 minuti (o cuoci come indicato sulla confezione) e scolali.",
     "soba": "Cuoci {soba} in acqua bollente per 4-5 minuti, scolali, sciacquali sotto acqua fredda e scolali bene.",
     "noodles_uova": "Cuoci {noodles_uova} in acqua bollente salata per 3-4 minuti e scolali.",
     "polenta": "Versa {polenta} a pioggia in acqua bollente salata mescolando, cuoci come da confezione e tienila morbida.",
    }
    return T[k]

CARB_LABEL = {"pasta": "pasta", "pasta_int": "pasta integrale", "pasta_fresca": "pasta fresca", "riso": "riso", "riso_int": "riso integrale",
              "basmati": "riso basmati", "orzo": "orzo", "farro": "farro", "quinoa": "quinoa", "couscous": "cous cous", "bulgur": "bulgur",
              "miglio": "miglio", "grano_saraceno": "grano saraceno", "patate": "patate", "patate_dolci": "patate dolci",
              "noodles_riso": "noodles di riso", "soba": "soba", "noodles_uova": "noodles all'uovo", "polenta": "polenta"}
CARB_G = {"pasta": 85, "pasta_int": 85, "pasta_fresca": 120, "riso": 80, "riso_int": 80, "basmati": 80, "orzo": 80, "farro": 80, "quinoa": 70,
          "couscous": 70, "bulgur": 70, "miglio": 70, "grano_saraceno": 70, "patate": 250, "patate_dolci": 250, "noodles_riso": 70, "soba": 80,
          "noodles_uova": 80, "polenta": 70}
GRAIN_MIN = {"riso": 14, "riso_int": 35, "basmati": 12, "orzo": 30, "farro": 25, "quinoa": 15, "couscous": 6, "bulgur": 12, "miglio": 20,
             "grano_saraceno": 15, "pasta": 12, "pasta_int": 12, "pasta_fresca": 4, "patate": 25, "patate_dolci": 30, "noodles_riso": 8, "soba": 5,
             "noodles_uova": 4, "polenta": 10}

# ---------------- verdure: (frase di taglio, pronome, min padella, min forno, min vapore) ----------------
VEG = {
 "zucchine": ("Taglia {k} a rondelle", "le", 7, 20, 8), "melanzane": ("Taglia {k} a cubetti", "le", 10, 25, 12),
 "peperoni": ("Pulisci {k} e tagliali a falde", "li", 12, 25, 15), "funghi": ("Pulisci {k} con un panno umido e affettali", "li", 8, 18, 8),
 "broccoli": ("Dividi {k} in cimette", "li", 10, 22, 8), "cavolfiore": ("Dividi {k} in cimette", "lo", 12, 25, 10),
 "carote": ("Pela {k} e tagliale a rondelle", "le", 12, 28, 12), "zucca": ("Elimina buccia e semi di {k} e tagliala a cubetti", "la", 15, 28, 15),
 "fagiolini": ("Pulisci {k} e spuntali", "li", 12, 22, 10), "asparagi": ("Elimina la parte dura di {k} e tagliali a metà", "li", 8, 15, 7),
 "carciofi": ("Pulisci {k}, eliminando le foglie esterne, e tagliali a spicchi", "li", 15, 28, 15), "finocchi": ("Taglia {k} a spicchi sottili", "li", 12, 28, 12),
 "porri": ("Lava {k} e tagliali ad anelli", "li", 10, 20, 8), "cavoletti": ("Dimezza {k}", "li", 12, 25, 10),
 "taccole": ("Pulisci {k}", "le", 6, 12, 5), "spinaci": ("Lava bene {k}", "li", 5, 8, 4), "bietole": ("Lava {k} e taglia le coste a pezzetti", "le", 8, 15, 8),
 "cavolo_nero": ("Elimina la costa centrale di {k} e spezzettalo", "lo", 12, 15, 10), "cime_rapa": ("Pulisci {k}, tenendo cimette e foglie tenere", "le", 10, 18, 8),
 "indivia": ("Lava {k} e tagliala a strisce", "la", 6, 12, 5), "pak_choi": ("Dividi {k} in quarti nel senso della lunghezza", "lo", 4, 10, 4),
 "cavolo_cinese": ("Taglia {k} a strisce", "lo", 5, 12, 5), "verza": ("Taglia {k} a listarelle", "la", 12, 25, 12),
 "cavolo_rosso": ("Taglia {k} a listarelle", "lo", 12, 25, 12), "radicchio": ("Taglia {k} a spicchi", "lo", 5, 12, 4),
 "rape": ("Pela {k} e tagliale a cubetti", "le", 15, 25, 12), "cipolla": ("Affetta {k}", "la", 10, 25, 8), "cipollotti": ("Affetta {k}", "li", 6, 15, 5),
 "pomodori": ("Taglia {k} a spicchi", "li", 6, 18, 4), "barbabietole": ("Pela {k} e tagliale a spicchi", "le", 15, 35, 20),
 "portobello": ("Pulisci {k} e tagliali a fette spesse", "li", 8, 20, 8), "shiitake": ("Pulisci {k} e affettali", "li", 6, 15, 6),
 "cavolo": ("Taglia {k} a listarelle", "lo", 12, 25, 12), "sedano": ("Taglia {k} a tocchetti", "lo", 10, 20, 8), "mais": ("Scola {k}", "lo", 4, 10, 4),
 "piselli": ("Sciacqua {k}", "li", 6, 12, 5), "edamame": ("Sciacqua {k}", "li", 4, 10, 5), "germogli_soia": ("Sciacqua {k}", "li", 2, 8, 2),
}
VEG_RAW = {  # verdure/frutta da mangiare crude: frase di taglio
 "pomodori": "Taglia {k} a pezzi", "cetrioli": "Taglia {k} a rondelle", "carote": "Pela {k} e tagliale a julienne", "finocchi": "Affetta finemente {k}",
 "peperoni": "Taglia {k} a striscioline", "ravanelli": "Affetta {k}", "lattuga": "Lava e spezzetta {k}", "rucola": "Lava e asciuga {k}",
 "romana": "Lava e taglia {k} a striscioline", "radicchio": "Lava e taglia {k} a striscioline", "spinaci": "Lava e asciuga {k}", "indivia": "Lava e taglia {k}",
 "cavolo_cinese": "Taglia {k} a striscioline sottili", "cavolo_rosso": "Taglia {k} a striscioline sottilissime", "cavolo": "Taglia {k} a striscioline sottilissime",
 "zucchine": "Affetta {k} sottilmente con un pelapatate", "sedano": "Taglia {k} a bastoncini", "cipolla": "Affetta {k} finemente", "cipollotti": "Affetta {k}",
 "germogli": "Sciacqua {k}", "mais": "Scola {k}", "avocado": "Sbuccia {k} e taglialo a fette", "mela": "Taglia {k} a cubetti", "arancia": "Pela {k} al vivo e tagliala a spicchi",
 "pera": "Taglia {k} a fette", "melograno": "Sgrana {k}", "uva": "Lava {k}", "barbabietole": "Taglia {k} a cubetti", "rape": "Taglia {k} a bastoncini",
}

def veg_cook(k, method):
    cut, pr, tp, tf, tv = VEG[k]
    c = cut.replace("{k}", "{" + k + "}")
    if method == "padella": return f"{c}, poi salta{pr} in padella con un filo d'olio e un pizzico di sale per {tp} minuti, mescolando ogni tanto."
    if method == "forno": return f"{c}, condisci{pr} con olio e sale e cuoci{pr} in forno a 200 °C per {tf} minuti, girando a metà cottura."
    if method == "vapore": return f"{c} e cuoci{pr} a vapore per {tv} minuti."
    raise KeyError(method)

def veg_raw(k):
    return VEG_RAW[k].replace("{k}", "{" + k + "}") + "."

# ---------------- proteine: come si cuociono ----------------
CLS = {"pollo": "petto", "tacchino": "petto", "fesa_tacchino": "petto", "pollo_cosce": "cosce", "manzo_magro": "straccetti", "bistecca": "bistecca",
       "vitello": "scaloppine", "maiale": "filetto", "agnello": "agnello", "salsiccia": "salsiccia", "macinato": "macinato", "pollo_macinato": "macinato",
       "tacchino_macinato": "macinato", "vitello_macinato": "macinato", "agnello_macinato": "macinato", "salmone": "pesce", "merluzzo": "pesce",
       "branzino": "pesce", "trota": "pesce", "tilapia": "pesce", "eglefino": "pesce", "merluzzo_nordico": "pesce", "pesce_spada": "pesce_spesso",
       "tonno_fresco": "pesce_spesso", "sgombro": "pesce", "gamberi": "gamberi", "calamari": "calamari", "seppie": "calamari", "capesante": "capesante",
       "tofu": "tofu", "tempeh": "tofu"}
PT = {
 "petto": {"padella": "Taglia {k} a fettine sottili, salale e cuocile in padella antiaderente con un filo d'olio per 6-8 minuti, girandole, finché sono dorate e bianche al centro.",
           "forno": "Disponi {k} in una teglia con carta forno, condisci con olio, sale e pepe e cuoci a 200 °C per 18-22 minuti, finché non è bianco al centro.",
           "griglia": "Scalda bene la piastra e cuoci {k} a fette per 5-6 minuti per lato, finché non è cotto e bianco al centro.",
           "friggitrice": "Taglia {k} a pezzi, condiscili con olio, sale e spezie e cuocili in friggitrice ad aria a 180 °C per 12-14 minuti, mescolando a metà.",
           "vapore": "Cuoci {k} a pezzi a vapore per 12-15 minuti, finché non è bianco al centro, poi condiscilo con limone e olio."},
 "cosce": {"padella": "Rosola {k} in padella con un filo d'olio per 5 minuti per lato, poi copri e cuoci a fuoco dolce per 20-25 minuti.",
           "forno": "Disponi {k} in teglia con olio, sale e pepe e cuoci a 200 °C per 35-40 minuti, girando a metà.",
           "friggitrice": "Condisci {k} con olio, sale e spezie e cuoci in friggitrice ad aria a 190 °C per 25-28 minuti, girando a metà."},
 "straccetti": {"padella": "Taglia {k} a straccetti e rosolali in padella molto calda con un filo d'olio per 3-4 minuti: devono restare morbidi.",
                "griglia": "Cuoci {k} a fette sulla piastra rovente per 2-3 minuti per lato.", "forno": "Cuoci {k} in forno a 200 °C per 12-15 minuti."},
 "bistecca": {"padella": "Scalda bene una padella, cuoci {k} 3-4 minuti per lato (media cottura) con un filo d'olio e lasciala riposare 2 minuti.",
              "griglia": "Cuoci {k} sulla piastra rovente per 3-4 minuti per lato e lasciala riposare 2 minuti.",
              "forno": "Rosola {k} in padella 2 minuti per lato, poi finisci in forno a 200 °C per 6-8 minuti."},
 "scaloppine": {"padella": "Infarina leggermente {k}, cuocilo in padella con un filo d'olio per 2-3 minuti per lato e sala a fine cottura.",
                "griglia": "Cuoci {k} sulla piastra per 2-3 minuti per lato.", "forno": "Cuoci {k} in forno a 190 °C per 12-15 minuti con un filo d'olio."},
 "filetto": {"padella": "Rosola {k} a medaglioni in padella con un filo d'olio per 4-5 minuti per lato, finché non è cotto al centro.",
             "forno": "Rosola {k} in padella 2 minuti per lato e cuoci in forno a 180 °C per 20-25 minuti, finché non è cotto al centro.",
             "griglia": "Cuoci {k} a fette sulla piastra per 5-6 minuti per lato, finché non è cotto al centro."},
 "agnello": {"padella": "Rosola {k} a pezzi in padella con un filo d'olio per 5-6 minuti, poi abbassa il fuoco e cuoci altri 10 minuti.",
             "forno": "Condisci {k} con olio, rosmarino e sale e cuoci in forno a 200 °C per 25-30 minuti, girando a metà.",
             "griglia": "Cuoci {k} sulla piastra per 4-5 minuti per lato, finché non è cotto al centro."},
 "salsiccia": {"padella": "Buca {k} con una forchetta e cuocila in padella antiaderente a fuoco medio per 12-15 minuti, girandola spesso, finché non è cotta al centro.",
               "forno": "Disponi {k} in teglia e cuoci in forno a 200 °C per 22-25 minuti, girandola a metà.",
               "friggitrice": "Cuoci {k} in friggitrice ad aria a 180 °C per 14-16 minuti, girandola a metà.",
               "griglia": "Cuoci {k} sulla piastra per 12-15 minuti, girandola spesso."},
 "macinato": {"padella": "Rosola {k} in padella antiaderente, sgranandolo con un cucchiaio di legno, per 6-8 minuti finché perde il colore rosato.",
              "forno": "Forma {k} in polpettine e cuocile in forno a 200 °C per 15-18 minuti."},
 "pesce": {"padella": "Asciuga {k} e cuocilo in padella antiaderente con un filo d'olio per 3-4 minuti per lato, finché si sfalda facilmente.",
           "forno": "Metti {k} su un foglio di carta forno con un filo d'olio, sale e limone, chiudi a cartoccio e cuoci a 180 °C per 15-18 minuti.",
           "griglia": "Scalda la piastra e cuoci {k} 3-4 minuti per lato, finché si sfalda facilmente.",
           "friggitrice": "Condisci {k} con olio e sale e cuoci in friggitrice ad aria a 180 °C per 10-12 minuti.",
           "vapore": "Cuoci {k} a vapore per 8-10 minuti, finché la polpa si sfalda facilmente."},
 "pesce_spesso": {"padella": "Scalda bene una padella con un filo d'olio e cuoci {k} 2-3 minuti per lato: l'interno deve restare tenero.",
                  "forno": "Cuoci {k} in forno a 200 °C con un filo d'olio per 10-12 minuti.", "griglia": "Cuoci {k} sulla piastra rovente per 3 minuti per lato."},
 "gamberi": {"padella": "Sguscia {k} e saltali in padella con un filo d'olio e uno spicchio d'aglio per 3-4 minuti, finché diventano rosa.",
             "forno": "Condisci {k} con olio e aglio e cuocili in forno a 200 °C per 8-10 minuti.",
             "friggitrice": "Condisci {k} con olio e spezie e cuocili in friggitrice ad aria a 180 °C per 6-8 minuti.", "griglia": "Cuoci {k} sulla piastra per 2-3 minuti per lato."},
 "calamari": {"padella": "Taglia {k} ad anelli e saltali in padella molto calda per 3-4 minuti: più a lungo diventano gommosi.",
              "forno": "Cuoci {k} in forno a 200 °C per 10-12 minuti con olio e prezzemolo.", "griglia": "Cuoci {k} sulla piastra per 2-3 minuti per lato."},
 "capesante": {"padella": "Asciuga {k} e rosolale in padella molto calda con un filo d'olio per 1-2 minuti per lato.", "forno": "Cuoci {k} in forno a 220 °C per 6-8 minuti."},
 "tofu": {"padella": "Scola {k}, pressalo 10 minuti in un panno, taglialo a cubetti e rosolalo in padella antiaderente con un filo d'olio per 8-10 minuti, finché è dorato.",
          "forno": "Taglia {k} a cubetti, condiscilo con olio e salsa di soia e cuocilo in forno a 200 °C per 20-25 minuti, girando a metà.",
          "friggitrice": "Taglia {k} a cubetti, condiscilo con olio e salsa di soia e cuocilo in friggitrice ad aria a 190 °C per 12-14 minuti.",
          "griglia": "Taglia {k} a fette spesse e cuocile sulla piastra per 4-5 minuti per lato."},
}
PROT_LABEL = {"pollo": "pollo", "tacchino": "tacchino", "fesa_tacchino": "fesa di tacchino", "pollo_cosce": "cosce di pollo", "manzo_magro": "manzo",
              "bistecca": "bistecca di manzo", "vitello": "vitello", "maiale": "maiale", "agnello": "agnello", "salsiccia": "salsiccia", "macinato": "macinato di manzo",
              "pollo_macinato": "macinato di pollo", "tacchino_macinato": "macinato di tacchino", "vitello_macinato": "macinato di vitello", "agnello_macinato": "macinato di agnello",
              "salmone": "salmone", "merluzzo": "merluzzo", "branzino": "branzino", "trota": "trota", "tilapia": "pesce bianco", "eglefino": "eglefino",
              "merluzzo_nordico": "merluzzo carbonaro", "pesce_spada": "pesce spada", "tonno_fresco": "tonno fresco", "sgombro": "sgombro", "gamberi": "gamberi",
              "calamari": "calamari", "seppie": "seppie", "capesante": "capesante", "tofu": "tofu", "tempeh": "tempeh"}
PROT_G = {"pollo": 130, "tacchino": 130, "fesa_tacchino": 130, "pollo_cosce": 220, "manzo_magro": 120, "bistecca": 150, "vitello": 120, "maiale": 130, "agnello": 130,
          "salsiccia": 120, "macinato": 110, "pollo_macinato": 110, "tacchino_macinato": 110, "vitello_macinato": 110, "agnello_macinato": 110, "salmone": 130,
          "merluzzo": 160, "branzino": 160, "trota": 160, "tilapia": 160, "eglefino": 160, "merluzzo_nordico": 160, "pesce_spada": 130, "tonno_fresco": 130,
          "sgombro": 130, "gamberi": 140, "calamari": 150, "seppie": 150, "capesante": 120, "tofu": 150, "tempeh": 110}

def prot_cook(k, method):
    return PT[CLS[k]][method].replace("{k}", "{" + k + "}")

def prot_methods(k):
    return list(PT[CLS[k]].keys())

METHOD_TAG = {"padella": "padella", "forno": "forno", "griglia": "padella", "friggitrice": "friggitrice", "vapore": ""}
