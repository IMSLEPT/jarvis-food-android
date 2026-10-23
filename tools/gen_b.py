# Famiglie: zuppe, secondi di carne/pesce/tofu, uova e frittate, legumi, polpette e burger, verdure ripiene.
from gen_core import *

# ---------- contorni ----------
SIDE_G = {"patate": 200, "riso": 70, "pane_int": 70, "pane": 70, "polenta": 60, "couscous": 60, "quinoa": 60, "farro": 70, "patate_dolci": 200, "bulgur": 60, "orzo": 70, "basmati": 70, "pane_pita": 70}
SIDE_LAB = {"patate": "patate", "riso": "riso", "pane_int": "pane integrale", "pane": "pane", "polenta": "polenta", "couscous": "cous cous", "quinoa": "quinoa", "farro": "farro",
            "patate_dolci": "patate dolci", "bulgur": "bulgur", "orzo": "orzo", "basmati": "riso basmati", "pane_pita": "pita"}
VEG_NAME = {"zucchine": "zucchine", "melanzane": "melanzane", "peperoni": "peperoni", "funghi": "funghi", "broccoli": "broccoli", "cavolfiore": "cavolfiore", "carote": "carote",
            "zucca": "zucca", "fagiolini": "fagiolini", "asparagi": "asparagi", "carciofi": "carciofi", "finocchi": "finocchi", "porri": "porri", "cavoletti": "cavoletti di Bruxelles",
            "spinaci": "spinaci", "bietole": "bietole", "cavolo_nero": "cavolo nero", "cime_rapa": "cime di rapa", "verza": "verza", "pak_choi": "pak choi", "pomodori": "pomodori",
            "barbabietole": "barbabietole", "cipolla": "cipolle", "taccole": "taccole", "rape": "rape"}

def side_carb_step(c):
    if c == "patate": return "Taglia {patate} a spicchi, condisci con olio, sale e rosmarino e cuoci in forno a 200 °C per 35-40 minuti, girando a metà."
    if c == "patate_dolci": return "Taglia {patate_dolci} a spicchi, condisci con olio e paprika e cuoci in forno a 200 °C per 25-30 minuti, girando a metà."
    if c == "pane_pita": return "Scalda {pane_pita} nel tostapane o in padella per 1 minuto."
    if c in ("pane", "pane_int"): return "Servi con {" + c + "} a fette, tostato se preferisci."
    return cook_carb(c)

def side_min(c):
    return {"patate": 40, "patate_dolci": 30, "pane": 0, "pane_int": 0}.get(c, GRAIN_MIN.get(c, 15))

LESS = {"ceci": "ceci lessati", "lenticchie": "lenticchie lessate", "fagioli": "fagioli lessati", "fagioli_rossi": "fagioli rossi lessati", "fave_cotte": "fave lessate", "piselli": "piselli lessati"}

# ======================= ZUPPE E VELLUTATE =======================
SOUP_VEG = ["zucca", "zucchine", "carote", "broccoli", "cavolfiore", "spinaci", "asparagi", "funghi", "finocchi", "porri", "barbabietole", "cavolo_nero", "bietole", "verza", "fagiolini", "carciofi", "peperoni", "rape", "cime_rapa", "pomodori", "sedano", "cavoletti"]
SOUP_G = {"pomodori": 250, "sedano": 180, "spinaci": 250, "cavolo_nero": 150, "verza": 200, "bietole": 200, "cime_rapa": 200}
def gen_zuppe():
    for vi, v in enumerate(SOUP_VEG):
        g = SOUP_G.get(v, 250)
        vn = VEG_NAME.get(v, v.replace("_", " "))
        cut = VEG[v][0].replace("{k}", "{" + v + "}")
        base_st = [f"{cut} e fai appassire in una casseruola con {{olio}} e un pizzico di sale per 5 minuti."]
        # vellutata con crostini
        add(f"Vellutata di {vn} con crostini", "pranzo" if vi % 2 else "cena", 35, [(v, g, "v"), ("patate", 120, "c"), ("brodo_veg", 350, "x", "brodo vegetale"), ("olio", 8, "f", "olio EVO"), ("pane_int", 40, "c", "pane integrale")],
            base_st + ["Aggiungi {patate} sbucciate a cubetti e {brodo_veg} caldo, copri e cuoci per 20-25 minuti.", "Frulla con un frullatore a immersione fino a ottenere una crema liscia.", "Tosta {pane_int} a cubetti in forno o in padella e servi sulla vellutata."], "Prepara una dose doppia: si congela benissimo.", "italiana", ["padella", "meal_prep"], 1, fam="zuppe")
        for li, (leg, lab, lg) in enumerate([("ceci", "ceci", 120), ("lenticchie", "lenticchie", 120), ("fagioli", "fagioli", 120), ("piselli", "piselli", 100)]):
            if leg == "piselli" and v in ("piselli",): continue
            add(f"Zuppa di {vn} e {lab}", "cena" if (vi + li) % 3 else "pranzo", 35, [(v, g - 50, "v"), (leg, lg, "p", LESS[leg]), ("patate", 80, "c"), ("brodo_veg", 350, "x", "brodo vegetale"), ("olio", 8, "f", "olio EVO"), ("rosmarino", 2, "x")],
                base_st + [f"Aggiungi {{patate}} a cubetti, {{{leg}}}, {{rosmarino}} e {{brodo_veg}} caldo.", "Cuoci a fuoco dolce per 20 minuti, poi schiaccia una parte dei legumi con il mestolo per rendere la zuppa cremosa.", "Servi con un filo d'olio a crudo e pepe."], "", "italiana", ["padella", "meal_prep"], 1, fam="zuppe")
        for ci, c in enumerate(["orzo", "farro", "pasta", "riso"]):
            nm = {"orzo": "orzo", "farro": "farro", "pasta": "pasta", "riso": "riso"}[c]
            add(f"Minestra di {vn} e {nm}", "pranzo" if (vi + ci) % 2 else "cena", 35, [(v, g - 30, "v"), (c, 55, "c"), ("brodo_veg", 400, "x", "brodo vegetale"), ("olio", 8, "f", "olio EVO"), ("parmigiano", 15, "f"), ("cipolla", 25, "v")],
                [f"Trita {{cipolla}} e falla appassire con {{olio}} per 4 minuti; {cut[0].lower() + cut[1:]} e aggiungila.", "Versa {brodo_veg} caldo e cuoci per 10 minuti.", f"Unisci {{{c}}} e cuoci per il tempo della confezione ({GRAIN_MIN[c] if c!='pasta' else 10}-{GRAIN_MIN[c] + 3 if c!='pasta' else 12} minuti).", "Servi con {parmigiano} grattugiato."], "", "italiana", ["padella"], 1, fam="zuppe")
    # zuppe leggere (spuntino/primo leggero)
    for vi, v in enumerate(SOUP_VEG):
        vn = VEG_NAME.get(v, v.replace("_", " "))
        cut = VEG[v][0].replace("{k}", "{" + v + "}")
        add(f"Brodo leggero con {vn}", "spuntino", 20, [(v, 200, "v"), ("brodo_veg", 350, "x", "brodo vegetale"), ("olio", 3, "f", "olio EVO"), ("prezzemolo", 3, "x")],
            [f"{cut}.", "Porta a bollore {brodo_veg} e aggiungi la verdura.", f"Cuoci {VEG[v][4] + 6} minuti finché è tenera, aggiungi un filo di {{olio}} e {{prezzemolo}}."], "Ottima come spuntino caldo o per iniziare un pasto leggero.", "italiana", ["padella"], 1, fam="zuppe_leggere")

# ======================= SECONDI (carne, pesce, tofu) =======================
PN = {"pollo": "Petto di pollo", "tacchino": "Petto di tacchino", "fesa_tacchino": "Fettine di tacchino", "pollo_cosce": "Cosce di pollo", "manzo_magro": "Straccetti di manzo", "bistecca": "Bistecca di manzo",
      "vitello": "Scaloppine di vitello", "maiale": "Filetto di maiale", "agnello": "Spezzatino di agnello", "salsiccia": "Salsiccia", "salmone": "Salmone", "merluzzo": "Merluzzo", "branzino": "Branzino",
      "trota": "Trota", "tilapia": "Filetto di pesce bianco", "eglefino": "Eglefino", "merluzzo_nordico": "Merluzzo carbonaro", "pesce_spada": "Pesce spada", "tonno_fresco": "Tonno fresco",
      "sgombro": "Sgombro", "gamberi": "Gamberi", "calamari": "Calamari", "seppie": "Seppie", "capesante": "Capesante", "tofu": "Tofu", "tempeh": "Tempeh"}
# stile: (resto nome, ingredienti extra, passi prima della cottura, passi dopo la cottura, cucina)
S_POLLO = [
 ("al limone", [("limone", 25, "x"), ("prezzemolo", 3, "x")], [], ["Sfuma con il succo di {limone} e completa con {prezzemolo} tritato."], "italiana"),
 ("alle erbe aromatiche", [("rosmarino", 2, "x"), ("timo", 2, "x"), ("aglio", 3, "x")], ["Marina {K} con {rosmarino}, {timo}, {aglio} schiacciato e un filo d'olio per 15 minuti."], [], "italiana"),
 ("al curry e yogurt", [("curry", 3, "x"), ("yogurt", 60, "f", "yogurt bianco")], ["Marina {K} con {curry} e {yogurt} per almeno 20 minuti."], [], "indiana"),
 ("alla paprika", [("paprika", 3, "x"), ("cipolla", 30, "v")], ["Condisci {K} con {paprika} e un pizzico di sale; affetta {cipolla}."], ["Aggiungi {cipolla} negli ultimi minuti di cottura."], "americana"),
 ("al pomodoro e olive", [("passata", 100, "v"), ("olive", 15, "f", "olive taggiasche"), ("origano", 1, "x")], [], ["Aggiungi {passata}, {olive} e {origano} e cuoci 8 minuti finché il sugo si stringe."], "mediterranea"),
 ("ai funghi", [("funghi", 100, "v"), ("vino_bianco", 30, "x"), ("prezzemolo", 3, "x")], [], ["Aggiungi {funghi} a fettine e {vino_bianco}, cuoci 8 minuti e completa con {prezzemolo}."], "italiana"),
 ("alla senape e miele", [("senape", 10, "x"), ("miele", 7, "x")], ["Mescola {senape} e {miele} e spennella {K}."], [], "americana"),
 ("alla pizzaiola", [("passata", 100, "v"), ("mozzarella_light", 40, "p", "mozzarella light"), ("origano", 1, "x")], [], ["Copri con {passata} e {origano} e cuoci 8 minuti, aggiungi {mozzarella_light} a fettine e fai fondere 2 minuti."], "italiana"),
 ("alla cacciatora", [("passata", 80, "v"), ("olive", 12, "f"), ("capperi", 4, "x"), ("vino_bianco", 30, "x"), ("rosmarino", 2, "x")], [], ["Sfuma con {vino_bianco}, aggiungi {passata}, {olive}, {capperi} e {rosmarino} e cuoci 10 minuti."], "italiana"),
 ("al latte di cocco e curcuma", [("latte_cocco", 50, "f", "latte di cocco"), ("curcuma", 2, "x"), ("zenzero", 4, "x")], [], ["Aggiungi {latte_cocco}, {curcuma} e {zenzero} grattugiato e cuoci a fuoco dolce 6 minuti."], "asiatica"),
 ("con salsa allo yogurt e menta", [("greco0", 70, "p", "yogurt greco 0%"), ("menta", 3, "x"), ("limone", 10, "x")], [], ["Mescola {greco0} con {menta} tritata, succo di {limone} e un pizzico di sale e servi accanto."], "mediterranea"),
 ("in crosta di pangrattato", [("pangrattato", 25, "c"), ("albume", 40, "p"), ("prezzemolo", 3, "x")], ["Sbatti {albume} in un piatto e mescola {pangrattato} con {prezzemolo} e sale in un altro; passa {K} prima nell'albume e poi nel pangrattato, premendo bene."], [], "italiana"),
]
S_ROSSA = {
 "manzo_magro": [("al rosmarino", [("rosmarino", 3, "x"), ("aglio", 3, "x")], [], ["Aggiungi {rosmarino} e {aglio} e salta 1 minuto."], "italiana"),
                 ("con rucola e grana", [("rucola", 40, "v"), ("parmigiano", 15, "f"), ("pomodori", 80, "v", "pomodorini")], [], ["Servi su un letto di {rucola} con {pomodori} a metà e scaglie di {parmigiano}."], "italiana"),
                 ("al vino rosso", [("vino_rosso", 50, "x"), ("cipolla", 30, "v"), ("rosmarino", 2, "x")], [], ["Aggiungi {cipolla} affettata e {rosmarino}, sfuma con {vino_rosso} e lascia ridurre 6 minuti."], "italiana"),
                 ("all'aceto balsamico", [("aceto_bals", 12, "x"), ("rosmarino", 2, "x")], [], ["Sfuma con {aceto_bals} e lascia caramellare 1 minuto con {rosmarino}."], "italiana"),
                 ("alla pizzaiola", [("passata", 100, "v"), ("origano", 1, "x"), ("aglio", 3, "x")], [], ["Aggiungi {passata}, {aglio} e {origano} e cuoci 8 minuti."], "italiana"),
                 ("ai peperoni", [("peperoni", 100, "v"), ("cipolla", 30, "v"), ("paprika", 2, "x")], [], ["Aggiungi {peperoni} e {cipolla} a listarelle con {paprika} e cuoci altri 8 minuti."], "americana"),
                 ("alla paprika e yogurt", [("paprika", 3, "x"), ("greco0", 60, "p", "yogurt greco 0%")], [], ["Fuori dal fuoco mescola con {greco0} e {paprika}."], "altre"),
                 ("alla soia e zenzero", [("salsa_soia", 10, "x"), ("zenzero", 5, "x"), ("sesamo", 5, "f")], [], ["Sfuma con {salsa_soia} e {zenzero} grattugiato, completa con {sesamo}."], "asiatica")],
 "bistecca": [("al pepe", [("pepe", 2, "x"), ("rosmarino", 2, "x")], ["Pepa {K} su entrambi i lati e profumala con {rosmarino}."], [], "italiana"),
              ("con rucola e pomodorini", [("rucola", 40, "v"), ("pomodori", 80, "v", "pomodorini"), ("parmigiano", 10, "f")], [], ["Servi con {rucola}, {pomodori} a metà e scaglie di {parmigiano}."], "italiana"),
              ("al limone e prezzemolo", [("limone", 20, "x"), ("prezzemolo", 4, "x")], [], ["Condisci con succo di {limone} e {prezzemolo} tritato."], "italiana")],
 "vitello": [("al limone", [("limone", 25, "x"), ("prezzemolo", 3, "x")], [], ["Sfuma con il succo di {limone} e completa con {prezzemolo}."], "italiana"),
             ("ai funghi", [("funghi", 100, "v"), ("vino_bianco", 30, "x"), ("prezzemolo", 3, "x")], [], ["Aggiungi {funghi} e {vino_bianco}, cuoci 6 minuti, completa con {prezzemolo}."], "italiana"),
             ("alla salvia", [("salvia", 3, "x"), ("vino_bianco", 30, "x")], [], ["Profuma con {salvia} e sfuma con {vino_bianco} per 2 minuti."], "italiana"),
             ("alla pizzaiola", [("passata", 100, "v"), ("origano", 1, "x")], [], ["Aggiungi {passata} e {origano} e cuoci 6 minuti."], "italiana"),
             ("al pomodoro e capperi", [("passata", 80, "v"), ("capperi", 5, "x"), ("olive", 10, "f")], [], ["Aggiungi {passata}, {capperi} e {olive} e cuoci 6 minuti."], "mediterranea"),
             ("con prosciutto crudo e salvia", [("prosciutto_crudo", 30, "p"), ("salvia", 3, "x")], [], ["Copri ogni fetta con {prosciutto_crudo} e {salvia}, scalda 1 minuto."], "italiana")],
 "maiale": [("alla senape", [("senape", 12, "x"), ("rosmarino", 2, "x")], [], ["Spalma con {senape} e {rosmarino} negli ultimi 2 minuti."], "americana"),
            ("alle mele", [("mela", 100, "v"), ("cannella", 1, "x")], [], ["Aggiungi {mela} a spicchi e {cannella} e cuoci 5 minuti finché è morbida."], "altre"),
            ("al latte e salvia", [("latte_scremato", 80, "x", "latte scremato"), ("salvia", 3, "x")], [], ["Aggiungi {latte_scremato} e {salvia} e cuoci a fuoco dolce finché si addensa."], "italiana"),
            ("all'arancia", [("arancia", 100, "v"), ("rosmarino", 2, "x")], [], ["Aggiungi il succo di {arancia} e {rosmarino} e riduci 4 minuti."], "mediterranea"),
            ("alle prugne", [("prugne_secche", 25, "v"), ("vino_bianco", 30, "x")], [], ["Aggiungi {prugne_secche} e {vino_bianco} e cuoci 8 minuti."], "italiana"),
            ("all'aceto balsamico", [("aceto_bals", 12, "x"), ("rosmarino", 2, "x")], [], ["Sfuma con {aceto_bals} e lascia caramellare 1 minuto."], "italiana")],
 "agnello": [("al rosmarino e limone", [("rosmarino", 3, "x"), ("limone", 15, "x"), ("aglio", 3, "x")], ["Marina {K} con {rosmarino}, {aglio} e succo di {limone} per 30 minuti."], [], "mediterranea"),
             ("alla paprika", [("paprika", 3, "x"), ("cumino", 1, "x"), ("cipolla", 30, "v")], ["Condisci {K} con {paprika} e {cumino}."], ["Aggiungi {cipolla} a fettine e cuoci altri 5 minuti."], "altre"),
             ("con yogurt e menta", [("greco0", 70, "p", "yogurt greco 0%"), ("menta", 3, "x")], [], ["Servi con {greco0} mescolato a {menta} tritata."], "mediterranea")],
 "salsiccia": [("con cime di rapa", [("cime_rapa", 150, "v"), ("aglio", 3, "x")], [], ["Cuoci {cime_rapa} a parte con {aglio} e un filo d'olio e servile con la salsiccia."], "italiana"),
               ("con funghi e cipolla", [("funghi", 100, "v"), ("cipolla", 40, "v")], [], ["Aggiungi {funghi} e {cipolla} affettati e cuoci 8 minuti."], "italiana"),
               ("con lenticchie", [("lenticchie", 100, "p", "lenticchie lessate"), ("rosmarino", 2, "x")], [], ["Scalda {lenticchie} con {rosmarino} e servi con la salsiccia."], "italiana"),
               ("con peperoni", [("peperoni", 120, "v"), ("cipolla", 30, "v")], [], ["Aggiungi {peperoni} e {cipolla} a listarelle e cuoci 10 minuti."], "italiana")],
}
S_POLLO_PROT = ["pollo", "tacchino", "fesa_tacchino"]
S_COSCE = [S_POLLO[1], S_POLLO[2], S_POLLO[3], S_POLLO[8], S_POLLO[0], S_POLLO[6], S_POLLO[4]]
S_PESCE = [
 ("al limone e prezzemolo", [("limone", 25, "x"), ("prezzemolo", 4, "x")], [], ["Condisci con succo di {limone} e {prezzemolo} tritato."], "mediterranea"),
 ("ai pomodorini e olive", [("pomodori", 120, "v", "pomodorini"), ("olive", 15, "f"), ("origano", 1, "x")], [], ["Aggiungi {pomodori} a metà, {olive} e {origano} e cuoci 5 minuti."], "mediterranea"),
 ("alla mediterranea", [("pomodori", 100, "v", "pomodorini"), ("capperi", 6, "x"), ("olive", 12, "f"), ("prezzemolo", 3, "x")], [], ["Aggiungi {pomodori}, {capperi} e {olive}, cuoci 5 minuti e completa con {prezzemolo}."], "mediterranea"),
 ("in crosta di pistacchi", [("pistacchi", 15, "f"), ("limone", 10, "x")], ["Trita {pistacchi} e premili sulla superficie di {K}."], ["Servi con una spruzzata di {limone}."], "italiana"),
 ("alla salsa di yogurt e aneto", [("greco0", 70, "p", "yogurt greco 0%"), ("aneto", 3, "x"), ("limone", 10, "x")], [], ["Mescola {greco0} con {aneto} e {limone} e servi accanto."], "altre"),
 ("alla soia e zenzero", [("salsa_soia", 10, "x"), ("zenzero", 5, "x"), ("sesamo", 5, "f")], [], ["Condisci con {salsa_soia}, {zenzero} grattugiato e {sesamo}."], "asiatica"),
 ("all'arancia", [("arancia", 100, "v"), ("finocchi", 60, "v")], [], ["Spremi {arancia} sul pesce e servi con {finocchi} a fettine sottili."], "mediterranea"),
 ("in crosta di pangrattato e erbe", [("pangrattato", 20, "c"), ("prezzemolo", 4, "x"), ("limone", 10, "x")], ["Mescola {pangrattato} con {prezzemolo} tritato e un filo d'olio e premilo sopra {K}."], ["Servi con {limone}."], "italiana"),
 ("al curry e latte di cocco", [("latte_cocco", 50, "f", "latte di cocco"), ("curry", 2, "x"), ("coriandolo", 3, "x")], [], ["Aggiungi {latte_cocco} e {curry} e cuoci 5 minuti; completa con {coriandolo}."], "indiana"),
]
S_SHELL = [S_PESCE[0], S_PESCE[1], S_PESCE[5], S_PESCE[8],
           ("al peperoncino e aglio", [("peperoncino", 1, "x"), ("aglio", 3, "x"), ("prezzemolo", 4, "x")], [], ["Aggiungi {peperoncino}, {aglio} e {prezzemolo} nell'ultimo minuto."], "italiana")]
S_TOFU = [
 ("alla soia e zenzero", [("salsa_soia", 12, "x"), ("zenzero", 5, "x"), ("sesamo", 5, "f")], [], ["Sfuma con {salsa_soia} e {zenzero} grattugiato, completa con {sesamo}."], "asiatica"),
 ("al curry", [("curry", 3, "x"), ("latte_cocco", 40, "f", "latte di cocco"), ("coriandolo", 3, "x")], [], ["Aggiungi {curry} e {latte_cocco} e cuoci 5 minuti; completa con {coriandolo}."], "indiana"),
 ("al limone e erbe", [("limone", 20, "x"), ("prezzemolo", 4, "x"), ("origano", 1, "x")], [], ["Condisci con succo di {limone}, {prezzemolo} e {origano}."], "mediterranea"),
 ("alla paprika affumicata", [("paprika", 3, "x"), ("cipolla", 30, "v")], [], ["Aggiungi {cipolla} a fettine e {paprika} e cuoci 5 minuti."], "americana"),
 ("al pomodoro e basilico", [("passata", 100, "v"), ("basilico", 3, "x"), ("aglio", 3, "x")], [], ["Aggiungi {passata} e {aglio}, cuoci 8 minuti, completa con {basilico}."], "italiana"),
 ("alle arachidi e salsa di soia", [("arachidi", 15, "f"), ("salsa_soia", 10, "x"), ("peperoncino", 1, "x")], [], ["Tosta e trita {arachidi}, aggiungile con {salsa_soia} e {peperoncino}."], "asiatica"),
 ("in crosta di sesamo", [("sesamo", 10, "f"), ("salsa_soia", 8, "x")], ["Spennella {K} con {salsa_soia} e rotolalo nel {sesamo}."], [], "giapponese"),
 ("al miso e zenzero", [("miso", 12, "x"), ("zenzero", 4, "x")], ["Mescola {miso} con {zenzero} grattugiato e un cucchiaio d'acqua e spennella {K}."], [], "giapponese"),
]
S_TEMPEH = [S_TOFU[0], S_TOFU[1], S_TOFU[3], S_TOFU[5], S_TOFU[7]]
FISH_METHODS = {"salmone": ["forno", "padella", "griglia", "friggitrice"], "merluzzo": ["forno", "padella", "vapore"], "branzino": ["forno", "padella", "griglia"], "trota": ["forno", "padella"],
                "tilapia": ["padella", "forno", "friggitrice"], "eglefino": ["forno", "padella", "vapore"], "merluzzo_nordico": ["forno", "padella"], "pesce_spada": ["padella", "griglia", "forno"],
                "tonno_fresco": ["padella", "griglia"], "sgombro": ["forno", "padella", "griglia"]}
VEG_SIDES = ["zucchine", "broccoli", "peperoni", "fagiolini", "spinaci", "carote", "asparagi", "finocchi", "melanzane", "cavolfiore", "bietole", "funghi", "zucca", "cavoletti", "carciofi", "cime_rapa", "cavolo_nero", "pak_choi"]
CARB_SIDES = ["patate", "riso", "pane_int", "polenta", "couscous", "quinoa", "farro", "patate_dolci", "bulgur"]

def _compat_side(prot, veg, carb):
    cls = CLS[prot]
    if cls in ("tofu",) and carb in ("polenta", "patate_dolci") and veg in ("funghi",): return True
    if carb == "polenta" and cls in ("pesce", "pesce_spesso", "gamberi", "calamari", "capesante"): return False
    if carb == "patate_dolci" and cls in ("calamari", "capesante"): return False
    if veg in ("pak_choi",) and prot not in ("tofu", "tempeh", "salmone", "gamberi", "pollo", "maiale", "manzo_magro", "calamari", "pesce_spada"): return False
    return True

def secondo(prot, style, veg, carb, idx):
    rest, extras, pre, post, cu = style
    meths = prot_methods(prot)
    if prot in FISH_METHODS: meths = FISH_METHODS[prot]
    if prot in ("gamberi", "calamari", "seppie", "capesante"): meths = [m for m in meths if m in ("padella", "forno", "friggitrice")]
    if prot in ("pollo", "tacchino", "fesa_tacchino"): meths = [m for m in meths if m != "vapore"]
    if any(k in rest for k in ("pomodoro", "pizzaiola", "cacciatora", "funghi", "limone e prezzemolo", "olive", "latte", "curry", "vino", "mele", "arancia", "yogurt")) and "padella" in meths and not any(k in rest for k in ("crosta", "yogurt")):
        meths = ["padella"]
    if any(k in rest for k in ("crosta", "in crosta")): meths = [m for m in meths if m in ("forno", "friggitrice")] or ["forno"]
    m = meths[idx % len(meths)]
    vm = ["padella", "forno", "vapore"][(idx // 2) % 3]
    if veg in ("spinaci", "bietole", "pak_choi", "cime_rapa", "cavolo_nero") and vm == "forno": vm = "padella"
    if carb in ("patate", "patate_dolci") and m == "forno": vm = "forno"
    pg = PROT_G[prot]
    ings = [(prot, pg, "p")]
    ings += list(extras)
    if not any(e[0] == "olio" for e in extras): ings.append(("olio", 8, "f", "olio EVO"))
    ings += [(veg, 150 if veg not in ("spinaci", "bietole", "cavolo_nero") else 200, "v"), (carb, SIDE_G[carb], "c")]
    Kp = "{" + prot + "}"
    st = [s.replace("{K}", Kp) for s in pre]
    if "crosta" in rest and m in ("forno", "friggitrice"):
        st.append("Cuoci in forno a 200 °C per 15-18 minuti, girando a metà." if m == "forno" else "Cuoci in friggitrice ad aria a 190 °C per 12-14 minuti, girando a metà.")
        if CLS[prot] == "pesce": st[-1] = st[-1].replace("15-18", "12-15").replace("12-14", "10-12")
        if prot == "tofu": st[-1] = st[-1].replace("15-18", "20-25")
    else:
        st.append(prot_cook(prot, m))
    st += [s for s in post]
    st.append(veg_cook(veg, vm))
    st.append(side_carb_step(carb))
    st.append("Impiatta e condisci con {olio} a crudo prima di servire.")
    name = (f"{PN[prot]} {rest}, {VEG_NAME[veg]} e {SIDE_LAB[carb]}" if rest.startswith("con ") else f"{PN[prot]} {rest} con {VEG_NAME[veg]} e {SIDE_LAB[carb]}")
    mins = max(20, 12 + (side_min(carb) if carb in ("patate", "patate_dolci") else 8) + (10 if pre else 0))
    methods = [METHOD_TAG[m]] + ([METHOD_TAG.get(vm, "")] if vm == "forno" else [])
    methods = [x for x in dict.fromkeys(methods) if x]
    tip = ""
    return add(name, "cena" if idx % 5 else "pranzo", mins, ings, st, tip, cu, methods, 1 if not pre else 2, fam="secondi")

def gen_secondi():
    jobs = []
    def sides(prot, i, n):
        res = []
        k = 0
        while len(res) < n and k < 200:
            v = VEG_SIDES[(i * 3 + k * 5) % len(VEG_SIDES)]
            c = CARB_SIDES[(i * 2 + k * 3) % len(CARB_SIDES)]
            if _compat_side(prot, v, c) and (v, c) not in res: res.append((v, c))
            k += 1
        return res
    i = 0
    for prot in S_POLLO_PROT:
        for st in S_POLLO:
            for (v, c) in sides(prot, i, 2): secondo(prot, st, v, c, i)
            i += 1
    for st in S_COSCE:
        for (v, c) in sides("pollo_cosce", i, 2): secondo("pollo_cosce", st, v, c, i)
        i += 1
    for prot, sts in S_ROSSA.items():
        for st in sts:
            for (v, c) in sides(prot, i, 2): secondo(prot, st, v, c, i)
            i += 1
    for prot in FISH_METHODS:
        for st in pick(S_PESCE, 6, len(prot)):
            if prot in ("tonno_fresco", "pesce_spada") and "crosta di pangrattato" in st[0]: continue
            for (v, c) in sides(prot, i, 2): secondo(prot, st, v, c, i)
            i += 1
    for prot in ("gamberi", "calamari", "capesante", "seppie"):
        for st in S_SHELL:
            for (v, c) in sides(prot, i, 2): secondo(prot, st, v, c, i)
            i += 1
    for st in S_TOFU:
        for (v, c) in sides("tofu", i, 3): secondo("tofu", st, v, c, i)
        i += 1
    for st in S_TEMPEH:
        for (v, c) in sides("tempeh", i, 2): secondo("tempeh", st, v, c, i)
        i += 1

# ======================= UOVA E FRITTATE =======================
FR_VEG = [("spinaci", 150), ("zucchine", 150), ("funghi", 130), ("cipolla", 100), ("asparagi", 150), ("peperoni", 130), ("pomodori", 130), ("cipollotti", 100), ("broccoli", 130),
          ("bietole", 150), ("carciofi", 130), ("porri", 120), ("fagiolini", 130), ("melanzane", 130), ("cavolfiore", 130), ("zucca", 130)]
FR_CHEESE = [("parmigiano", 20, "f", "con parmigiano"), ("feta", 40, "f", "e feta"), ("ricotta", 60, "p", "e ricotta"), ("mozzarella", 50, "p", "e mozzarella"), ("caprino", 40, "p", "e caprino"),
             ("pecorino", 20, "f", "e pecorino")]
FR_SIDE = [("pane_int", 70), ("patate", 150), ("pane", 70)]
def gen_uova():
    k = 0
    for vi, (v, g) in enumerate(FR_VEG):
        vn = VEG_NAME.get(v, v)
        for ci, (ch, cg, cr, cn) in enumerate(pick(FR_CHEESE, 4, vi)):
            side, sg = FR_SIDE[(vi + ci) % 3]
            kind = ["Frittata", "Frittata al forno", "Omelette"][(vi + ci) % 3]
            form = {"Frittata": "padella", "Frittata al forno": "forno", "Omelette": "padella"}[kind]
            ings = [("uova", 150, "p"), (v, g, "v"), (ch, cg, cr), ("olio", 6, "f", "olio EVO"), (side, sg, "c")]
            cut = veg_cook(v, "padella") if v not in ("pomodori",) else veg_cook(v, "padella")
            if kind == "Frittata":
                st = [cut, "Sbatti {uova} con un pizzico di sale e unisci " + f"{{{ch}}}" + ".", "Versa le uova sulle verdure, abbassa il fuoco, copri e cuoci 6-7 minuti; gira la frittata con un piatto e cuoci altri 3 minuti."]
            elif kind == "Frittata al forno":
                st = ["Accendi il forno a 190 °C.", cut, "Sbatti {uova} con sale e pepe, unisci " + f"{{{ch}}}" + " e le verdure, versa in una teglia unta con {olio} e cuoci in forno per 15-18 minuti, finché è dorata."]
            else:
                st = [cut, "Sbatti {uova} con un pizzico di sale e versale in padella antiaderente calda; quando iniziano ad addensarsi aggiungi le verdure e " + f"{{{ch}}}" + ".", "Piega l'omelette a metà e cuoci 1-2 minuti."]
            st.append(side_carb_step(side))
            name = f"{kind} di {vn} {cn} con {SIDE_LAB[side]}" if cn.startswith("con") is False else f"{kind} di {vn} {cn} e {SIDE_LAB[side]}".replace(" con parmigiano e ", " con parmigiano e ")
            name = f"{kind} di {vn} {cn} con {SIDE_LAB[side]}" if not cn.startswith("con") else f"{kind} di {vn} con parmigiano e {SIDE_LAB[side]}"
            add(name, "cena" if (vi + ci) % 3 else "pranzo", 25, ings, st, "", "italiana", [form] if form else [], 1, fam="uova")
    # uova al pomodoro / in camicia / al forno con verdure
    for vi, (v, g) in enumerate(pick(FR_VEG, 10, 2)):
        vn = VEG_NAME.get(v, v)
        for si, (side, sg) in enumerate([("pane_int", 70), ("polenta", 60)]):
            add(f"Uova al pomodoro con {vn} e {SIDE_LAB[side]}", "cena", 25, [("uova", 150, "p"), ("passata", 150, "v"), (v, g - 30, "v"), ("olio", 6, "f", "olio EVO"), ("aglio", 3, "x"), ("basilico", 3, "x"), (side, sg, "c")],
                [veg_cook(v, "padella"), "Aggiungi {passata} e {aglio} e cuoci 8 minuti finché il sugo si restringe.", "Fai delle piccole conche e rompi dentro {uova}; copri e cuoci 5-6 minuti, finché l'albume è rappreso.", "Completa con {basilico}.", side_carb_step(side)], "Se ti piace il tuorlo morbido cuoci 4 minuti.", "mediterranea", ["padella"], 1, fam="uova")

# ======================= LEGUMI STUFATI =======================
LEG = [("ceci", "ceci", 150), ("lenticchie", "lenticchie", 150), ("fagioli", "fagioli", 150), ("fagioli_rossi", "fagioli rossi", 150), ("fave_cotte", "fave", 150), ("piselli", "piselli", 130)]
LEG_STYLES = [
 ("in umido con pomodoro", [("passata", 120, "v"), ("cipolla", 30, "v"), ("olio", 8, "f"), ("origano", 1, "x")], ["Soffriggi {cipolla} tritata in {olio} per 4 minuti.", "Aggiungi {passata}, {origano} e i legumi e cuoci 15 minuti a fuoco dolce."]),
 ("con spinaci e cumino", [("spinaci", 150, "v"), ("cumino", 1, "x"), ("aglio", 3, "x"), ("olio", 8, "f")], ["Scalda {olio} con {aglio} e {cumino}.", "Aggiungi {spinaci} e cuoci 4 minuti, poi unisci i legumi e scalda 6 minuti."]),
 ("con verdure miste", [("carote", 60, "v"), ("sedano", 40, "v"), ("zucchine", 80, "v"), ("olio", 8, "f"), ("rosmarino", 2, "x")], ["Taglia {carote}, {sedano} e {zucchine} a dadini e cuocili in {olio} con {rosmarino} per 8 minuti.", "Aggiungi i legumi e un mestolo d'acqua e cuoci 12 minuti."]),
 ("speziati con curry", [("curry", 3, "x"), ("cipolla", 40, "v"), ("passata", 80, "v"), ("olio", 8, "f"), ("coriandolo", 3, "x")], ["Soffriggi {cipolla} in {olio} con {curry} per 3 minuti.", "Aggiungi {passata} e i legumi e cuoci 12 minuti; completa con {coriandolo}."]),
 ("alla toscana con salvia", [("passata", 60, "v"), ("salvia", 3, "x"), ("aglio", 4, "x"), ("olio", 10, "f")], ["Scalda {olio} con {aglio} e {salvia}.", "Aggiungi {passata} e i legumi e cuoci 15 minuti a fuoco dolce."]),
 ("con bietole e aglio", [("bietole", 150, "v"), ("aglio", 4, "x"), ("olio", 8, "f"), ("peperoncino", 1, "x")], ["Cuoci {bietole} a pezzi in {olio} con {aglio} e {peperoncino} per 8 minuti.", "Aggiungi i legumi e scalda 6 minuti."]),
]
LEG_CARBS = ["pane_int", "riso", "farro", "polenta", "orzo", "couscous"]
def gen_legumi():
    k = 0
    for li, (lid, ln, lg) in enumerate(LEG):
        for si, (rest, extras, steps) in enumerate(LEG_STYLES):
            if lid == "piselli" and "spinaci" in rest: pass
            for ci in range(2):
                c = LEG_CARBS[(li + si * 2 + ci * 3) % len(LEG_CARBS)]
                ing = [(lid, lg, "p", LESS[lid])] + list(extras) + [(c, SIDE_G[c], "c")]
                st = [s.replace("i legumi", "{" + lid + "}") for s in steps] + [side_carb_step(c)]
                add(f"{ln.capitalize()} {rest} con {SIDE_LAB[c]}", "pranzo" if (li + si + ci) % 2 else "cena", 30 + side_min(c) // 2, ing, st, "Usa legumi già lessati (al naturale) o in barattolo, sciacquati.", "italiana" if "curry" not in rest else "indiana", ["padella", "meal_prep"], 1, fam="legumi")

# ======================= POLPETTE E BURGER =======================
POLP_PROT = [("macinato", "manzo", 110), ("pollo_macinato", "pollo", 110), ("tacchino_macinato", "tacchino", 110), ("vitello_macinato", "vitello", 110), ("agnello_macinato", "agnello", 100)]
POLP_STYLE = [
 ("al sugo", [("passata", 150, "v"), ("basilico", 3, "x")], ["Cuoci le polpette in padella con un filo d'olio 5 minuti per farle rosolare.", "Aggiungi {passata}, copri e cuoci 15 minuti; completa con {basilico}."], "forno_no", "italiana"),
 ("al forno", [("origano", 1, "x")], ["Disponi le polpette in teglia con carta forno, con un filo d'olio e {origano}, e cuoci a 200 °C per 18-20 minuti, girandole a metà."], "forno", "italiana"),
 ("in friggitrice ad aria", [("paprika", 2, "x")], ["Cuoci le polpette in friggitrice ad aria a 190 °C per 12-14 minuti, scuotendo a metà, con {paprika}."], "friggitrice", "americana"),
 ("al limone", [("limone", 20, "x"), ("prezzemolo", 3, "x")], ["Rosola le polpette in padella con un filo d'olio per 8 minuti.", "Sfuma con succo di {limone} e completa con {prezzemolo}."], "padella", "italiana"),
]
def gen_polpette():
    k = 0
    for pi, (p, pn, pg) in enumerate(POLP_PROT):
        for si, (rest, extras, steps, meth, cu) in enumerate(POLP_STYLE):
            for ci in range(2):
                c = ["patate", "pane_int", "riso", "polenta", "couscous", "farro"][(pi + si * 2 + ci * 3) % 6]
                v = ["zucchine", "spinaci", "fagiolini", "carote", "broccoli", "peperoni"][(pi * 2 + si + ci * 3) % 6]
                ing = [(p, pg, "p"), ("uova", 25, "p"), ("pangrattato", 15, "c"), ("parmigiano", 10, "f"), ("prezzemolo", 3, "x"), ("olio", 8, "f", "olio EVO")] + [e for e in extras if e[0] != "prezzemolo"] + [(v, 150, "v"), (c, SIDE_G[c], "c")]
                st = ["Mescola {" + p + "}, {uova}, {pangrattato}, {parmigiano} e {prezzemolo} con un pizzico di sale; forma polpette di 3-4 cm."] + steps + [veg_cook(v, "padella"), side_carb_step(c), "Condisci con {olio} a crudo e servi."]
                add(f"Polpette di {pn} {rest} con {VEG_NAME[v]} e {SIDE_LAB[c]}", "cena" if (pi + si + ci) % 3 else "pranzo", 40, ing, st, "Prepara la dose doppia e congela le polpette crude, già formate.", cu, [x for x in [meth if meth not in ("forno_no",) else "padella"] if x], 1, fam="polpette")
    # polpette di legumi e vegetali
    VEGP = [("ceci", "ceci", 130), ("lenticchie", "lenticchie", 130), ("fagioli", "fagioli", 130), ("tonno_nat", "tonno e patate", 90)]
    for pi, (p, pn, pg) in enumerate(VEGP):
        for si, c in enumerate(["pane_int", "riso", "couscous", "farro", "quinoa"]):
            for meth, mtxt in (("forno", "in forno a 200 °C per 20 minuti, girandole a metà"), ("friggitrice", "in friggitrice ad aria a 190 °C per 12-14 minuti")):
                if (si + (meth == "forno")) % 2 == 0 and pi >= 3: continue
                v = ["zucchine", "spinaci", "carote", "pomodori", "finocchi"][(pi + si) % 5]
                extra = [("patate", 80, "c")] if p == "tonno_nat" else []
                ing = [(p, pg, "p", f"{pn.split(' ')[0]} lessati" if p != "tonno_nat" else None) if p != "tonno_nat" else (p, pg, "p")] + extra + [("pangrattato", 20, "c"), ("prezzemolo", 4, "x"), ("olio", 8, "f", "olio EVO"), ("cumino", 1, "x"), (v, 120, "v"), (c, SIDE_G[c] - 10, "c")]
                ing = [tuple(x for x in i if x is not None) for i in ing]
                st = ["Schiaccia {" + p + "} con una forchetta" + (" insieme a {patate} lessate" if p == "tonno_nat" else "") + ", aggiungi {pangrattato}, {prezzemolo}, {cumino} e sale e impasta; forma polpette."] + [f"Disponile su carta forno, spennellale con olio e cuocile {mtxt}.", veg_raw(v) if v in VEG_RAW and v in ("pomodori", "carote", "finocchi") else veg_cook(v, "padella"), side_carb_step(c), "Condisci con {olio} e servi."]
                add(f"Polpette di {pn} {'al forno' if meth == 'forno' else 'in friggitrice'} con {VEG_NAME[v]} e {SIDE_LAB[c]}", "cena", 40, ing, st, "", "mediterranea", [meth], 1, fam="polpette")

# ======================= VERDURE RIPIENE =======================
RIPIENE = {
 "zucchine": ("Zucchine", "Dividi {zucchine} a metà nel senso della lunghezza e scava la polpa con un cucchiaino; tritala e tienila da parte.", 150, 220),
 "peperoni": ("Peperoni", "Taglia {peperoni} a metà, elimina semi e filamenti.", 200, 250),
 "melanzane": ("Melanzane", "Dividi {melanzane} a metà, incidi la polpa a quadretti e scavala leggermente; tritala.", 200, 250),
 "pomodori": ("Pomodori", "Taglia la calotta ai {pomodori}, svuotali e tieni da parte la polpa tritata.", 250, 300),
 "portobello": ("Funghi portobello", "Pulisci {portobello} e stacca i gambi; tritali.", 200, 250),
 "zucca": ("Zucca", "Dividi {zucca} in spicchi sottili ed elimina i semi.", 250, 300),
}
FILL = [
 ("tonno e ricotta", [("tonno_nat", 80, "p"), ("ricotta", 60, "p"), ("pangrattato", 15, "c"), ("prezzemolo", 4, "x"), ("olio", 8, "f", "olio EVO")], "Mescola {tonno_nat} sgocciolato, {ricotta}, {pangrattato}, {prezzemolo} e la polpa tritata."),
 ("carne e parmigiano", [("macinato", 100, "p", "macinato di manzo magro"), ("parmigiano", 15, "f"), ("pangrattato", 15, "c"), ("uova", 25, "p"), ("olio", 8, "f", "olio EVO")], "Rosola {macinato} 5 minuti con la polpa tritata, lascia intiepidire e mescola con {parmigiano}, {pangrattato} e {uova}."),
 ("tacchino e ricotta", [("tacchino_macinato", 100, "p"), ("ricotta_light", 50, "p"), ("parmigiano", 10, "f"), ("pangrattato", 15, "c"), ("olio", 8, "f", "olio EVO")], "Rosola {tacchino_macinato} 5 minuti con la polpa tritata e mescola con {ricotta_light}, {parmigiano} e {pangrattato}."),
 ("riso e feta", [("riso", 50, "c"), ("feta", 40, "f"), ("menta", 3, "x"), ("pomodori_secchi", 10, "v"), ("olio", 8, "f", "olio EVO")], "Cuoci {riso} in acqua salata, scolalo e mescolalo con {feta} a pezzetti, {menta} e {pomodori_secchi} tritati e la polpa."),
 ("quinoa e ceci", [("quinoa", 50, "c"), ("ceci", 80, "p", "ceci lessati"), ("cumino", 1, "x"), ("prezzemolo", 4, "x"), ("olio", 8, "f", "olio EVO")], "Cuoci {quinoa} in acqua salata, scolala e mescolala con {ceci} schiacciati grossolanamente, {cumino}, {prezzemolo} e la polpa."),
 ("tofu e verdure", [("tofu", 120, "p"), ("carote", 40, "v"), ("salsa_soia", 8, "x"), ("pangrattato", 15, "c"), ("olio", 8, "f", "olio EVO")], "Sbriciola {tofu} e mescolalo con {carote} grattugiate, {salsa_soia}, {pangrattato} e la polpa."),
 ("lenticchie e pomodoro", [("lenticchie", 100, "p", "lenticchie lessate"), ("passata", 60, "v"), ("pangrattato", 15, "c"), ("rosmarino", 2, "x"), ("olio", 8, "f", "olio EVO")], "Mescola {lenticchie}, {passata}, {pangrattato}, {rosmarino} e la polpa tritata."),
 ("pollo e mozzarella", [("pollo_macinato", 100, "p"), ("mozzarella_light", 40, "p", "mozzarella light"), ("pangrattato", 15, "c"), ("basilico", 3, "x"), ("olio", 8, "f", "olio EVO")], "Rosola {pollo_macinato} 5 minuti con la polpa e mescola con {mozzarella_light} a dadini, {pangrattato} e {basilico}."),
]
def gen_ripiene():
    for ci, (v, (vn, prep, vg, mins)) in enumerate(RIPIENE.items()):
        for fi, (fn, fing, fst) in enumerate(FILL):
            if v == "zucca" and fi not in (0, 1, 3, 4): continue
            if v == "pomodori" and "carne" in fn: continue
            ing = [(v, vg, "v")] + list(fing)
            c = ["pane_int", "riso", "couscous"][(ci + fi) % 3] if "riso" not in fn and "quinoa" not in fn else None
            if c: ing.append((c, 50, "c"))
            st = ["Accendi il forno a 190 °C.", prep, fst, "Farcisci le verdure con il ripieno, disponile in una teglia con carta forno, irrora con {olio} e cuoci per 30-35 minuti, finché sono morbide e gratinate."]
            if c: st.append(side_carb_step(c))
            name = f"{vn} ripiene di {fn}" if v not in ("zucca",) else f"{vn} al forno con ripieno di {fn}"
            add(name, "cena" if (ci + fi) % 3 else "pranzo", 55, ing, st, "Si possono preparare in anticipo e scaldare.", "italiana", ["forno", "meal_prep"], 2, fam="ripiene")

def generate():
    gen_zuppe(); gen_secondi(); gen_uova(); gen_legumi(); gen_polpette(); gen_ripiene()
