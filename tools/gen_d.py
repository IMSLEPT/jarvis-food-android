# Famiglie: wrap/panini, cucine del mondo, piatti abbondanti, insalatone.
from gen_core import *
from gen_b import side_carb_step, SIDE_LAB, SIDE_G, VEG_NAME, LESS

# ======================= WRAP, PIADINE, PANINI, PITA =======================
BREAD = {"piadina": ("Piadina", 80, "Scalda {piadina} in padella 1 minuto per lato."), "tortilla_mais": ("Tacos di mais", 60, "Scalda {tortilla_mais} in padella asciutta 30 secondi per lato."),
         "pane_pita": ("Pita", 70, "Scalda {pane_pita} nel tostapane o in padella per 1 minuto."), "pane_int": ("Panino integrale", 80, "Taglia {pane_int} a metà e tostalo 2 minuti."),
         "pane": ("Panino", 80, "Taglia {pane} a metà e tostalo 2 minuti.")}
WR = [  # (nome, ingredienti, passaggi, cucina, minuti)
 ("pollo grigliato, lattuga e yogurt", [("pollo", 90, "p"), ("lattuga", 30, "v"), ("pomodori", 60, "v"), ("greco0", 40, "p", "yogurt greco 0%"), ("limone", 8, "x")], ["Taglia {pollo} a fettine e cuocilo sulla piastra 4-5 minuti per lato; affettalo.", "Mescola {greco0} con {limone} e un pizzico di sale.", "Farcisci con {lattuga}, {pomodori} a fette, il pollo e la salsa allo yogurt."], "americana", 20),
 ("tacchino, mozzarella e rucola", [("fesa_tacchino", 60, "p", "fesa di tacchino a fette"), ("mozzarella", 50, "p"), ("rucola", 20, "v"), ("pomodori", 50, "v")], ["Farcisci con {fesa_tacchino}, {mozzarella} a fette, {pomodori} e {rucola}."], "italiana", 10),
 ("prosciutto crudo, mozzarella e rucola", [("prosciutto_crudo", 40, "p"), ("mozzarella", 50, "p"), ("rucola", 20, "v")], ["Farcisci con {prosciutto_crudo}, {mozzarella} a fette e {rucola}."], "italiana", 10),
 ("bresaola, rucola e grana", [("bresaola", 50, "p"), ("rucola", 20, "v"), ("parmigiano", 15, "f"), ("limone", 6, "x")], ["Farcisci con {bresaola}, {rucola} e scaglie di {parmigiano}; completa con {limone}."], "italiana", 10),
 ("tonno, pomodoro e lattuga", [("tonno_nat", 70, "p"), ("pomodori", 70, "v"), ("lattuga", 30, "v"), ("olio", 4, "f"), ("origano", 0.5, "x")], ["Farcisci con {tonno_nat} sgocciolato, {pomodori} a fette e {lattuga}; condisci con {olio} e {origano}."], "italiana", 10),
 ("uova, spinaci e pomodoro", [("uova", 100, "p"), ("spinaci", 40, "v"), ("pomodori", 60, "v"), ("olio", 4, "f")], ["Salta {spinaci} 2 minuti con {olio}.", "Cuoci {uova} in padella come una piccola frittata, 3 minuti per lato.", "Farcisci con la frittata, gli spinaci e {pomodori} a fette."], "italiana", 15),
 ("hummus e verdure grigliate", [("hummus", 50, "f"), ("zucchine", 80, "v"), ("peperoni", 60, "v"), ("rucola", 15, "v")], ["Griglia {zucchine} e {peperoni} a fette 3-4 minuti per lato.", "Spalma {hummus} e farcisci con le verdure e {rucola}."], "mediterranea", 20),
 ("falafel, hummus e cetrioli", [("falafel", 100, "p"), ("hummus", 30, "f"), ("cetrioli", 60, "v"), ("pomodori", 50, "v"), ("lattuga", 20, "v")], ["Scalda {falafel} in forno a 200 °C per 10 minuti.", "Spalma {hummus} e farcisci con {falafel}, {cetrioli}, {pomodori} e {lattuga}."], "altre", 20),
 ("salmone affumicato e formaggio spalmabile", [("salmone_aff", 50, "p"), ("spalmabile", 30, "f", "formaggio spalmabile"), ("cetrioli", 50, "v"), ("aneto", 2, "x")], ["Spalma {spalmabile} e farcisci con {salmone_aff}, {cetrioli} a rondelle e {aneto}."], "altre", 10),
 ("feta, pomodori e cetrioli", [("feta", 50, "f"), ("pomodori", 70, "v"), ("cetrioli", 60, "v"), ("olive", 10, "f"), ("origano", 0.5, "x")], ["Farcisci con {feta} a cubetti, {pomodori}, {cetrioli} e {olive}; completa con {origano}."], "mediterranea", 10),
 ("tofu saltato e verdure croccanti", [("tofu", 110, "p"), ("carote", 50, "v"), ("cavolo_rosso", 40, "v"), ("salsa_soia", 8, "x"), ("sesamo", 4, "f")], ["Rosola {tofu} a cubetti con {salsa_soia} per 8 minuti.", "Taglia {carote} e {cavolo_rosso} a julienne.", "Farcisci con tofu e verdure crude, completa con {sesamo}."], "asiatica", 20),
 ("straccetti di manzo, cipolla e rucola", [("manzo_magro", 100, "p"), ("cipolla", 40, "v"), ("rucola", 20, "v"), ("parmigiano", 10, "f"), ("olio", 4, "f")], ["Soffriggi {cipolla} in {olio} per 6 minuti.", "Aggiungi {manzo_magro} a straccetti e salta 3 minuti.", "Farcisci con la carne, {rucola} e scaglie di {parmigiano}."], "italiana", 20),
 ("prosciutto cotto ed emmental", [("prosciutto_cotto", 50, "p"), ("emmental", 30, "p"), ("lattuga", 20, "v"), ("senape", 6, "x")], ["Spalma {senape}, farcisci con {prosciutto_cotto}, {emmental} e {lattuga}."], "americana", 10),
 ("avocado e uova", [("avocado", 60, "f"), ("uova", 100, "p"), ("pomodori", 50, "v"), ("pepe", 0.5, "x")], ["Cuoci {uova} sode 9 minuti e sgusciale.", "Schiaccia {avocado}, farcisci con le uova a fette e {pomodori}; completa con pepe."], "americana", 15),
 ("ceci, cetrioli e yogurt", [("ceci", 100, "p", "ceci lessati"), ("cetrioli", 60, "v"), ("greco0", 40, "p", "yogurt greco 0%"), ("menta", 2, "x"), ("limone", 6, "x")], ["Schiaccia grossolanamente {ceci} con {limone}.", "Mescola {greco0} con {menta}.", "Farcisci con ceci, {cetrioli} e la salsa."], "mediterranea", 10),
 ("pollo e avocado", [("pollo", 90, "p"), ("avocado", 50, "f"), ("pomodori", 60, "v"), ("lattuga", 20, "v"), ("lime", 8, "x")], ["Cuoci {pollo} a striscioline in padella con un filo d'olio per 6-7 minuti.", "Farcisci con il pollo, {avocado} a fette, {pomodori}, {lattuga} e {lime}."], "americana", 20),
 ("gamberi, avocado e lime", [("gamberi", 100, "p"), ("avocado", 50, "f"), ("lattuga", 25, "v"), ("lime", 10, "x"), ("peperoncino", 0.5, "x")], ["Salta {gamberi} sgusciati 3 minuti in padella con un filo d'olio.", "Farcisci con i gamberi, {avocado}, {lattuga}, {lime} e {peperoncino}."], "messicana", 15),
 ("melanzane grigliate, mozzarella e pomodoro", [("melanzane", 100, "v"), ("mozzarella", 50, "p"), ("pomodori", 60, "v"), ("basilico", 3, "x"), ("olio", 4, "f")], ["Griglia {melanzane} a fette 4 minuti per lato con {olio}.", "Farcisci con melanzane, {mozzarella} a fette, {pomodori} e {basilico}."], "italiana", 20),
 ("tacchino, hummus e carote", [("fesa_tacchino", 70, "p", "fesa di tacchino a fette"), ("hummus", 35, "f"), ("carote", 50, "v"), ("lattuga", 20, "v")], ["Spalma {hummus}, aggiungi {fesa_tacchino}, {carote} grattugiate e {lattuga}."], "altre", 10),
 ("caprino, rucola e pomodori secchi", [("caprino", 50, "p"), ("rucola", 20, "v"), ("pomodori_secchi", 12, "v"), ("olio", 3, "f")], ["Spalma {caprino}, aggiungi {pomodori_secchi} a pezzetti, {rucola} e {olio}."], "italiana", 10),
 ("tonno, uova sode e insalata", [("tonno_nat", 60, "p"), ("uova", 50, "p"), ("lattuga", 30, "v"), ("pomodori", 60, "v"), ("olive", 10, "f")], ["Cuoci {uova} sode 9 minuti e sgusciale.", "Farcisci con {tonno_nat}, le uova a fette, {lattuga}, {pomodori} e {olive}."], "mediterranea", 15),
 ("manzo, cheddar e cipolla caramellata", [("manzo_magro", 90, "p"), ("cheddar", 25, "f"), ("cipolla", 50, "v"), ("senape", 6, "x"), ("olio", 4, "f")], ["Cuoci {cipolla} a fettine in {olio} a fuoco dolce per 12 minuti finché è dorata.", "Rosola {manzo_magro} a strisce 3 minuti.", "Farcisci con carne, cipolla, {cheddar} e {senape}."], "americana", 25),
 ("pollo, curry e yogurt", [("pollo", 90, "p"), ("greco0", 40, "p", "yogurt greco 0%"), ("curry", 2, "x"), ("carote", 40, "v"), ("lattuga", 20, "v")], ["Marina {pollo} a striscioline con {curry} e metà dello {greco0}, poi cuocilo in padella 6-7 minuti.", "Farcisci con pollo, {carote} grattugiate, {lattuga} e lo yogurt rimasto."], "indiana", 25),
]
def gen_wrap():
    breads = list(BREAD.keys())
    for wi, (nm, ings, steps, cu, mins) in enumerate(WR):
        for bi, b in enumerate(pick(breads, 4, wi)):
            bn, bg, bst = BREAD[b]
            if b == "tortilla_mais" and "tacos" not in nm and cu not in ("messicana", "americana", "altre"): pass
            name = f"{bn} con {nm}"
            st = [bst] + steps
            add(name, "pranzo" if (wi + bi) % 4 else "cena", mins, [(b, bg, "c")] + list(ings), st, "Avvolto nella carta forno si porta in ufficio.", cu, ["pranzo_ufficio"], 1, fam="wrap")

# ======================= CUCINA ASIATICA (WOK) =======================
WOK_P = {"pollo": ("pollo", "Taglia {k} a striscioline e saltalo nel wok con un filo d'olio per 5-6 minuti, finché non è bianco al centro; toglilo e tienilo da parte."),
         "manzo_magro": ("manzo", "Taglia {k} a striscioline sottili e saltalo nel wok molto caldo per 2-3 minuti; toglilo e tienilo da parte."),
         "maiale": ("maiale", "Taglia {k} a striscioline e saltalo nel wok caldo per 4-5 minuti, finché non è cotto al centro; toglilo e tienilo da parte."),
         "gamberi": ("gamberi", "Sguscia {k} e saltali nel wok con un filo d'olio per 2-3 minuti, finché diventano rosa; toglili e tienili da parte."),
         "tofu": ("tofu", "Scola {k}, pressalo in un panno, taglialo a cubetti e rosolalo nel wok con un filo d'olio per 8 minuti, finché è dorato; toglilo e tienilo da parte."),
         "tempeh": ("tempeh", "Taglia {k} a cubetti e rosolalo nel wok con un filo d'olio per 6-8 minuti; toglilo e tienilo da parte."),
         "salmone": ("salmone", "Taglia {k} a cubetti e rosolalo nel wok con un filo d'olio per 3-4 minuti; toglilo con delicatezza e tienilo da parte."),
         "uova": ("uova", "Sbatti {k} e cuocile in un velo d'olio nel wok come una sottile omelette, poi tagliala a striscioline e tienila da parte.")}
WOK_SAUCE = [
 ("alla soia e zenzero", [("salsa_soia", 15, "x"), ("zenzero", 5, "x"), ("aglio", 3, "x"), ("olio_sesamo", 6, "f", "olio di sesamo")], "Sfuma con {salsa_soia}, aggiungi {zenzero} grattugiato, {aglio} e {olio_sesamo}."),
 ("alla teriyaki", [("salsa_soia", 15, "x"), ("miele", 8, "x"), ("zenzero", 4, "x"), ("sesamo", 5, "f"), ("olio_sesamo", 5, "f", "olio di sesamo")], "Mescola {salsa_soia}, {miele} e {zenzero} grattugiato e versa nel wok per 1-2 minuti finché si addensa; completa con {sesamo} e {olio_sesamo}."),
 ("al curry e cocco", [("curry", 3, "x"), ("latte_cocco", 45, "f", "latte di cocco"), ("aglio", 3, "x"), ("olio", 4, "f")], "Aggiungi {curry}, {aglio} e {latte_cocco} e cuoci 3 minuti; regola di sale con un goccio di salsa di soia se serve."),
 ("alle arachidi", [("arachidi", 15, "f", "burro di arachidi"), ("salsa_soia", 10, "x"), ("peperoncino", 1, "x"), ("lime", 10, "x")], "Stempera {arachidi} con {salsa_soia}, {lime} e {peperoncino} e un goccio d'acqua calda, versa nel wok e mescola 1 minuto."),
 ("all'agrodolce", [("aceto_mele", 12, "x"), ("miele", 10, "x"), ("salsa_soia", 10, "x"), ("ketchup", 15, "x"), ("zenzero", 4, "x")], "Mescola {aceto_mele}, {miele}, {salsa_soia}, {ketchup} e {zenzero} grattugiato e versa nel wok per 2 minuti, finché si addensa."),
]
WOK_VEG = [("broccoli", "carote"), ("pak_choi", "funghi"), ("peperoni", "cipollotti"), ("taccole", "carote"), ("cavolo_cinese", "germogli_soia"), ("zucchine", "peperoni"), ("cavolfiore", "fagiolini"), ("verza", "carote"), ("spinaci", "funghi"), ("asparagi", "carote")]
WOK_BASE = ["noodles_riso", "soba", "riso", "basmati", "noodles_uova", "riso_int", "quinoa"]
WOK_G = {"pollo": 120, "manzo_magro": 110, "maiale": 120, "gamberi": 130, "tofu": 140, "tempeh": 110, "salmone": 120, "uova": 150}
def gen_wok():
    i = 0
    for pk, (pn, pst) in WOK_P.items():
        for si, (sn, sing, sst) in enumerate(WOK_SAUCE):
            if pk == "uova" and sn in ("all'agrodolce",): continue
            for j in range(2):
                v1, v2 = WOK_VEG[(i * 3 + j * 4) % len(WOK_VEG)]
                base = WOK_BASE[(i + j * 2) % len(WOK_BASE)]
                ing = [(pk, WOK_G[pk], "p"), (v1, 120, "v"), (v2, 80, "v")] + list(sing) + [(base, CARB_G[base], "c")]
                if not any(x[0] == "olio" or x[0] == "olio_sesamo" for x in sing): ing.append(("olio", 5, "f", "olio EVO"))
                st = [cook_carb(base), pst.replace("{k}", "{" + pk + "}"), veg_cook(v1, "padella").replace("in padella", "nel wok"), veg_cook(v2, "padella").replace("in padella", "nel wok"),
                      "Rimetti nel wok " + ("la proteina" if pk != "uova" else "l'omelette") + ", aggiungi la base cotta e " + sst[0].lower() + sst[1:], "Servi subito, ben caldo."]
                # il passaggio salsa va dopo: ricostruzione chiara
                st = [cook_carb(base), pst.replace("{k}", "{" + pk + "}"), veg_cook(v1, "padella").replace("in padella", "nel wok"), veg_cook(v2, "padella").replace("in padella", "nel wok"),
                      "Rimetti nel wok " + ("la proteina" if pk != "uova" else "l'omelette") + " e unisci la base cotta.", sst, "Mescola per 1-2 minuti e servi subito, ben caldo."]
                name = f"Wok di {pn} {sn} con {VEG_NAME[v1] if v1 in VEG_NAME else v1.replace('_',' ')}, {VEG_NAME[v2] if v2 in VEG_NAME else v2.replace('_',' ')} e {CARB_LABEL[base]}"
                add(name, "cena" if (i + j) % 3 else "pranzo", 25 + GRAIN_MIN[base] // 3, ing, st, "Il wok va tenuto molto caldo: cuoci poca roba per volta.", "asiatica", ["padella"], 2, fam="asia")
            i += 1

# ======================= GIAPPONESE =======================
GIAP = [
 ("Oyakodon di pollo e uova", [("riso", 80, "c"), ("pollo", 100, "p"), ("uova", 100, "p"), ("cipolla", 50, "v"), ("salsa_soia", 15, "x"), ("miele", 6, "x")], [cook_carb("riso"), "Affetta {cipolla} e cuocila in un pentolino con {salsa_soia}, {miele} e un po' d'acqua per 5 minuti.", "Aggiungi {pollo} a tocchetti e cuoci 6 minuti.", "Versa {uova} sbattute a filo, copri e cuoci 1-2 minuti finché sono appena rapprese; servi sul riso."]),
 ("Gyudon di manzo e cipolla", [("riso", 80, "c"), ("manzo_magro", 110, "p"), ("cipolla", 60, "v"), ("salsa_soia", 15, "x"), ("zenzero", 4, "x"), ("miele", 6, "x")], [cook_carb("riso"), "Affetta {cipolla} e cuocila con {salsa_soia}, {miele}, {zenzero} grattugiato e poca acqua per 8 minuti.", "Aggiungi {manzo_magro} a fettine sottili e cuoci 2-3 minuti; servi sul riso."]),
 ("Sakedon di salmone e avocado", [("riso", 80, "c"), ("salmone", 110, "p"), ("avocado", 40, "f"), ("nori", 1, "x"), ("salsa_soia", 10, "x"), ("sesamo", 4, "f")], [cook_carb("riso"), "Cuoci {salmone} in padella 3-4 minuti per lato e sfaldalo.", "Componi la ciotola con riso, salmone, {avocado} a fette, {nori} a striscioline, {salsa_soia} e {sesamo}."]),
 ("Chirashi con tonno fresco e cetrioli", [("riso", 80, "c"), ("tonno_fresco", 100, "p"), ("cetrioli", 60, "v"), ("nori", 1, "x"), ("salsa_soia", 10, "x"), ("zenzero", 4, "x"), ("sesamo", 4, "f")], [cook_carb("riso"), "Taglia {tonno_fresco} a cubetti (usa pesce fresco da consumo crudo, abbattuto) e {cetrioli} a rondelle.", "Componi la ciotola con riso, tonno, cetrioli, {nori}, {zenzero} grattugiato, {salsa_soia} e {sesamo}."]),
 ("Tofu don con verdure", [("riso", 80, "c"), ("tofu", 130, "p"), ("broccoli", 100, "v"), ("carote", 50, "v"), ("salsa_soia", 12, "x"), ("sesamo", 4, "f")], [cook_carb("riso"), "Rosola {tofu} a cubetti con un filo d'olio 8 minuti.", "Salta {broccoli} a cimette e {carote} a rondelle 8 minuti.", "Servi sul riso con {salsa_soia} e {sesamo}."]),
 ("Yakisoba di pollo e verza", [("soba", 80, "c"), ("pollo", 100, "p"), ("verza", 100, "v"), ("carote", 50, "v"), ("salsa_soia", 15, "x"), ("olio_sesamo", 5, "f", "olio di sesamo")], [cook_carb("soba"), "Salta {pollo} a striscioline con {olio_sesamo} per 6 minuti.", "Aggiungi {verza} a listarelle e {carote} a julienne e salta 5 minuti.", "Unisci {soba} e {salsa_soia} e mescola 2 minuti a fuoco vivo."]),
 ("Yakisoba di gamberi e verdure", [("soba", 80, "c"), ("gamberi", 120, "p"), ("cavolo_cinese", 100, "v"), ("carote", 50, "v"), ("salsa_soia", 15, "x"), ("olio_sesamo", 5, "f", "olio di sesamo")], [cook_carb("soba"), "Salta {gamberi} sgusciati con {olio_sesamo} per 3 minuti e toglili.", "Salta {cavolo_cinese} e {carote} a julienne 5 minuti.", "Unisci {soba}, i gamberi e {salsa_soia}; mescola 2 minuti."]),
 ("Onigiri al tonno con insalata", [("riso", 90, "c"), ("tonno_nat", 60, "p"), ("nori", 2, "x"), ("maionese", 10, "f"), ("cetrioli", 80, "v")], [cook_carb("riso"), "Mescola {tonno_nat} sgocciolato con {maionese}.", "Forma 2 triangoli di riso con il ripieno al centro e avvolgili con {nori}.", "Servi con {cetrioli} a rondelle."]),
 ("Ramen leggero di pollo e uovo", [("noodles_uova", 80, "c"), ("pollo", 100, "p"), ("uova", 50, "p"), ("brodo_veg", 400, "x", "brodo vegetale"), ("miso", 15, "x"), ("pak_choi", 80, "v"), ("zenzero", 4, "x")], [cook_carb("noodles_uova"), "Cuoci {uova} per 6 minuti e mezzo, raffredda e sgusciale.", "Scalda {brodo_veg} con {zenzero} grattugiato e {pollo} a fettine per 8 minuti, poi sciogli {miso} fuori dal fuoco.", "Aggiungi {pak_choi} per 2 minuti, versa su {noodles_uova} e completa con l'uovo a metà."]),
 ("Ramen leggero di tofu e funghi", [("soba", 80, "c"), ("tofu", 120, "p"), ("shiitake", 80, "v"), ("brodo_veg", 400, "x", "brodo vegetale"), ("miso", 15, "x"), ("cipollotti", 30, "v")], [cook_carb("soba"), "Scalda {brodo_veg} con {shiitake} affettati e {tofu} a cubetti per 8 minuti.", "Sciogli {miso} fuori dal fuoco, versa su {soba} e completa con {cipollotti}."]),
 ("Zuppa di miso con soba e gamberi", [("soba", 60, "c"), ("gamberi", 100, "p"), ("miso", 15, "x"), ("brodo_veg", 400, "x", "brodo vegetale"), ("spinaci", 60, "v"), ("cipollotti", 30, "v")], [cook_carb("soba"), "Scalda {brodo_veg}, aggiungi {gamberi} sgusciati e {spinaci} per 3 minuti.", "Sciogli {miso} fuori dal fuoco, versa su {soba} e completa con {cipollotti}."]),
 ("Poke di tonno, edamame e mango", [("riso", 80, "c"), ("tonno_fresco", 100, "p"), ("edamame", 50, "v"), ("mango", 50, "v"), ("avocado", 30, "f"), ("salsa_soia", 10, "x"), ("sesamo", 4, "f")], [cook_carb("riso"), "Taglia {tonno_fresco} a cubetti (pesce da consumo crudo, abbattuto), {mango} e {avocado}.", "Componi la bowl con riso, tonno, {edamame}, mango e avocado; condisci con {salsa_soia} e {sesamo}."]),
 ("Poke di salmone, cetrioli e alga", [("riso", 80, "c"), ("salmone", 100, "p"), ("cetrioli", 60, "v"), ("nori", 1, "x"), ("avocado", 30, "f"), ("salsa_soia", 10, "x"), ("sesamo", 4, "f")], [cook_carb("riso"), "Cuoci {salmone} in padella 4 minuti per lato e sfaldalo (o usa salmone fresco abbattuto da consumo crudo).", "Componi la bowl con riso, salmone, {cetrioli}, {avocado} e {nori}; condisci con {salsa_soia} e {sesamo}."]),
 ("Okonomiyaki di cavolo e uova", [("farina", 40, "c"), ("uova", 100, "p"), ("verza", 150, "v"), ("cipollotti", 30, "v"), ("salsa_soia", 8, "x"), ("olio", 6, "f")], ["Taglia {verza} a striscioline sottili e {cipollotti} ad anelli.", "Mescola {farina}, {uova}, le verdure e {salsa_soia} con poca acqua fino a un impasto denso.", "Cuoci in padella con {olio} 5 minuti per lato, schiacciando con la spatola."]),
 ("Insalata di soba, edamame e sesamo", [("soba", 80, "c"), ("edamame", 80, "v"), ("carote", 50, "v"), ("cetrioli", 60, "v"), ("salsa_soia", 12, "x"), ("sesamo", 6, "f"), ("olio_sesamo", 5, "f", "olio di sesamo")], [cook_carb("soba"), "Cuoci {edamame} 5 minuti; taglia {carote} e {cetrioli} a julienne.", "Condisci tutto con {salsa_soia} e {olio_sesamo} e completa con {sesamo}."]),
]
def gen_giap():
    for gi, (nm, ings, steps) in enumerate(GIAP):
        add(nm, "pranzo" if gi % 2 else "cena", 25, ings, steps, "", "giapponese", ["padella"] if "Zuppa" not in nm else [], 2 if "Ramen" in nm else 1, fam="giappone")

# ======================= INDIA: CURRY =======================
CURRY_P = {"pollo": ("pollo", 120, "Taglia {k} a cubetti e rosolalo con un filo d'olio per 5 minuti prima di aggiungere la salsa."),
           "agnello": ("agnello", 120, "Taglia {k} a cubetti e rosolalo 8 minuti prima di aggiungere la salsa; allunga la cottura finale a 30 minuti."),
           "gamberi": ("gamberi", 140, "Sguscia {k} e aggiungili alla salsa pronta per gli ultimi 4 minuti."),
           "tofu": ("tofu", 150, "Taglia {k} a cubetti e rosolalo 6 minuti in padella antiaderente, poi uniscilo alla salsa."),
           "ceci": ("ceci", 150, "Scola {k} e uniscili alla salsa."),
           "lenticchie": ("lenticchie", 150, "Scola {k} e uniscile alla salsa."),
           "fagioli_rossi": ("fagioli rossi", 150, "Scola {k} e uniscili alla salsa."),
           "uova": ("uova", 150, "Cuoci {k} sode 9 minuti, sgusciale e dividile a metà: aggiungile alla salsa a fine cottura."),
           "merluzzo": ("merluzzo", 160, "Taglia {k} a pezzi e aggiungilo alla salsa per gli ultimi 6-7 minuti, senza mescolare troppo."),
           "tempeh": ("tempeh", 110, "Taglia {k} a cubetti e rosolalo 6 minuti prima di aggiungerlo alla salsa.")}
CURRY_S = [
 ("al pomodoro e yogurt", [("passata", 120, "v"), ("yogurt", 50, "f", "yogurt bianco"), ("curry", 4, "x"), ("cipolla", 40, "v"), ("aglio", 3, "x"), ("zenzero", 4, "x"), ("coriandolo", 3, "x"), ("olio", 8, "f", "olio EVO")], "Soffriggi {cipolla} in {olio} con {aglio}, {zenzero} grattugiato e {curry} per 4 minuti, aggiungi {passata} e cuoci 10 minuti; a fuoco spento unisci {yogurt} e {coriandolo}."),
 ("agli spinaci", [("spinaci", 200, "v"), ("curry", 3, "x"), ("cipolla", 40, "v"), ("aglio", 3, "x"), ("zenzero", 4, "x"), ("yogurt", 40, "f", "yogurt bianco"), ("olio", 8, "f", "olio EVO")], "Soffriggi {cipolla} in {olio} con {aglio}, {zenzero} grattugiato e {curry}, aggiungi {spinaci} e cuoci 6 minuti; frulla grossolanamente e unisci {yogurt}."),
 ("al latte di cocco", [("latte_cocco", 70, "f", "latte di cocco"), ("curcuma", 2, "x"), ("curry", 3, "x"), ("cipolla", 40, "v"), ("zenzero", 4, "x"), ("coriandolo", 3, "x"), ("olio", 5, "f", "olio EVO")], "Soffriggi {cipolla} in {olio} con {zenzero} grattugiato, {curry} e {curcuma}, versa {latte_cocco} e cuoci 8 minuti a fuoco dolce; completa con {coriandolo}."),
 ("alla mandorla (korma)", [("yogurt", 60, "f", "yogurt bianco"), ("mandorle", 15, "f"), ("curry", 3, "x"), ("cipolla", 40, "v"), ("aglio", 3, "x"), ("cardamomo", 1, "x"), ("olio", 6, "f", "olio EVO")], "Frulla {mandorle} con {yogurt} e un goccio d'acqua; soffriggi {cipolla} in {olio} con {aglio}, {curry} e {cardamomo}, aggiungi la crema e cuoci 8 minuti a fuoco dolce."),
 ("piccante (madras)", [("passata", 100, "v"), ("peperoncino", 2, "x"), ("cumino", 1, "x"), ("curry", 4, "x"), ("cipolla", 40, "v"), ("coriandolo", 3, "x"), ("olio", 8, "f", "olio EVO")], "Soffriggi {cipolla} in {olio} con {curry}, {cumino} e {peperoncino}, aggiungi {passata} e cuoci 12 minuti; completa con {coriandolo}."),
 ("al pomodoro e zenzero", [("passata", 120, "v"), ("zenzero", 6, "x"), ("curcuma", 2, "x"), ("cumino", 1, "x"), ("cipolla", 40, "v"), ("aglio", 3, "x"), ("olio", 8, "f", "olio EVO")], "Soffriggi {cipolla} in {olio} con {aglio}, {zenzero} grattugiato, {curcuma} e {cumino}, aggiungi {passata} e cuoci 12 minuti."),
]
def gen_curry():
    i = 0
    for pk, (pn, pg, pst) in CURRY_P.items():
        for si, (sn, sing, sst) in enumerate(CURRY_S):
            for j in range(2):
                base = ["basmati", "riso", "riso_int", "quinoa"][(i + j * 2) % 4]
                if pk in ("ceci", "lenticchie", "fagioli_rossi"): pgi = (pk, pg, "p", LESS[pk])
                else: pgi = (pk, pg, "p")
                ing = [pgi] + list(sing) + [(base, 75, "c")]
                st = [cook_carb(base), sst, pst.replace("{k}", "{" + pk + "}"), "Cuoci ancora 5-10 minuti a fuoco dolce, finché la salsa è cremosa; regola di sale.", "Servi il curry sul riso."]
                name = f"Curry di {pn} {sn} con {CARB_LABEL[base]}"
                add(name, "cena" if (i + j) % 3 else "pranzo", 40 + (20 if pk == "agnello" else 0), ing, st, "Il curry è ancora meglio il giorno dopo.", "indiana", ["padella", "meal_prep"], 2, fam="india")
            i += 1

# ======================= MESSICA E TEX-MEX =======================
def gen_messico():
    FILL = [
     ("pollo", "pollo", 100, [("pollo", 100, "p")], "Taglia {pollo} a striscioline e cuocilo in padella con {paprika} e {cumino} per 7 minuti.", [("paprika", 2, "x"), ("cumino", 1, "x")]),
     ("manzo macinato", "manzo", 100, [("macinato", 100, "p", "macinato di manzo magro")], "Rosola {macinato} in padella con {paprika} e {cumino} per 7 minuti.", [("paprika", 2, "x"), ("cumino", 1, "x")]),
     ("tacchino", "tacchino", 100, [("tacchino_macinato", 100, "p")], "Rosola {tacchino_macinato} in padella con {paprika} e {cumino} per 7 minuti.", [("paprika", 2, "x"), ("cumino", 1, "x")]),
     ("fagioli neri", "fagioli neri", 120, [("fagioli_neri", 120, "p", "fagioli neri lessati")], "Scalda {fagioli_neri} in padella con {cumino} e un goccio d'acqua per 5 minuti, schiacciandone alcuni.", [("cumino", 1, "x")]),
     ("gamberi", "gamberi", 110, [("gamberi", 110, "p")], "Salta {gamberi} sgusciati in padella con {paprika} per 3-4 minuti.", [("paprika", 2, "x")]),
     ("tofu", "tofu", 120, [("tofu", 120, "p")], "Sbriciola {tofu} e rosolalo in padella con {paprika} e {cumino} per 8 minuti.", [("paprika", 2, "x"), ("cumino", 1, "x")]),
     ("merluzzo", "merluzzo", 130, [("merluzzo", 130, "p")], "Cuoci {merluzzo} in padella con un filo d'olio e {paprika} per 3-4 minuti per lato e sfaldalo.", [("paprika", 2, "x")]),
     ("uova e fagioli", "uova", 100, [("uova", 100, "p"), ("fagioli_neri", 60, "p", "fagioli neri lessati")], "Scalda {fagioli_neri} e cuoci {uova} strapazzate in padella per 3 minuti.", []),
    ]
    TOP = [("avocado", 40, "f", "avocado a fette"), ("salsa_messicana", 40, "x", "salsa messicana"), ("lattuga", 30, "v", "lattuga a striscioline"), ("cavolo_rosso", 30, "v", "cavolo rosso a striscioline"), ("pomodori", 60, "v", "pomodori a cubetti"), ("lime", 10, "x", "lime")]
    for fi, (fn, fshort, g, ings, st0, ex) in enumerate(FILL):
        for ti in range(3):
            t1 = TOP[(fi + ti) % 6]; t2 = TOP[(fi + ti + 2) % 6]
            if t1[0] == t2[0]: continue
            name = f"Tacos di {fn} con {t1[0].replace('_', ' ') if t1[0] not in ('avocado', 'cavolo_rosso', 'salsa_messicana') else {'avocado': 'avocado', 'cavolo_rosso': 'cavolo rosso', 'salsa_messicana': 'salsa messicana'}[t1[0]]} e {t2[0].replace('_', ' ') if t2[0] not in ('cavolo_rosso', 'salsa_messicana') else {'cavolo_rosso': 'cavolo rosso', 'salsa_messicana': 'salsa messicana'}[t2[0]]}"
            ing = list(ings) + list(ex) + [("tortilla_mais", 80, "c"), (t1[0], t1[1], t1[2]), (t2[0], t2[1], t2[2])]
            ing = list(dict((i[0], i) for i in ing).values())
            st = [st0, "Scalda {tortilla_mais} in padella asciutta 30 secondi per lato.", f"Farcisci le tortillas con il ripieno e completa con {{{t1[0]}}} e {{{t2[0]}}}."]
            add(name, "cena" if (fi + ti) % 2 else "pranzo", 25, ing, st, "", "messicana", ["padella"], 1, fam="messico")
    # quesadillas, fajitas, burrito, chili
    Q = [("pollo e peperoni", [("pollo", 80, "p"), ("peperoni", 80, "v")], "Cuoci {pollo} a striscioline e {peperoni} a listarelle in padella per 8 minuti."),
         ("fagioli neri e mais", [("fagioli_neri", 80, "p", "fagioli neri lessati"), ("mais", 50, "v")], "Scalda {fagioli_neri} e {mais} con un po' di cumino per 4 minuti."),
         ("funghi e spinaci", [("funghi", 100, "v"), ("spinaci", 60, "v")], "Salta {funghi} e {spinaci} in padella per 6 minuti."),
         ("tacchino e cipolla", [("tacchino", 90, "p"), ("cipolla", 40, "v")], "Cuoci {tacchino} a striscioline con {cipolla} per 8 minuti."),
         ("tonno e mais", [("tonno_nat", 70, "p"), ("mais", 50, "v")], "Mescola {tonno_nat} sgocciolato e {mais}."),
         ("zucchine e ricotta", [("zucchine", 120, "v"), ("ricotta_light", 50, "p", "ricotta light")], "Salta {zucchine} a rondelle 6 minuti e mescola con {ricotta_light}.")]
    for qi, (qn, qing, qst) in enumerate(Q):
        for ci, (ch, cg, cl) in enumerate([("cheddar", 30, "cheddar"), ("mozzarella_light", 50, "mozzarella light")]):
            add(f"Quesadilla {qn} e {cl}", "pranzo" if (qi + ci) % 2 else "cena", 20, [("piadina", 80, "c", "tortilla di grano")] + list(qing) + [(ch, cg, "f")], [qst, f"Spargi {{{ch}}} e il ripieno su metà di {{piadina}} e chiudila a mezzaluna.", "Cuoci in padella antiaderente 2-3 minuti per lato, finché è dorata e il formaggio fuso.", "Taglia a spicchi e servi."], "", "messicana", ["padella"], 1, fam="messico")
    FJ = [("pollo", "pollo", 120), ("tacchino", "tacchino", 120), ("manzo_magro", "manzo", 110), ("gamberi", "gamberi", 130), ("tofu", "tofu", 130)]
    for fi, (pk, pn, pg) in enumerate(FJ):
        for ti, tp in enumerate([("avocado", 40, "f", "avocado"), ("greco0", 50, "p", "yogurt greco 0%")]):
            ing = [(pk, pg, "p"), ("peperoni", 150, "v"), ("cipolla", 50, "v"), ("piadina", 80, "c", "tortilla di grano"), ("paprika", 3, "x"), ("cumino", 1, "x"), ("olio", 6, "f", "olio EVO"), (tp[0], tp[1], tp[2], tp[3]), ("lime", 10, "x")]
            st = ["Taglia " + "{" + pk + "} e le verdure a strisce.", "Scalda {olio} in padella e cuoci {" + pk + "} con {paprika} e {cumino} per 6 minuti.", "Aggiungi {peperoni} e {cipolla} e cuoci altri 8 minuti a fuoco vivo.", "Scalda {piadina} e farciscila con il composto, {" + tp[0] + "} e {lime}."]
            add(f"Fajitas di {pn} con {tp[3]}", "cena" if (fi + ti) % 2 else "pranzo", 30, ing, st, "", "messicana", ["padella"], 1, fam="messico")
    CH = [("macinato", "manzo", 100, "macinato di manzo magro"), ("tacchino_macinato", "tacchino", 100, None), ("pollo_macinato", "pollo", 100, None), ("lenticchie", "lenticchie", 120, "lenticchie lessate"), ("ceci", "ceci", 120, "ceci lessati"), ("tofu", "tofu", 130, None)]
    for ci, (pk, pn, pg, lab) in enumerate(CH):
        for fi, (bk, bn) in enumerate([("fagioli_rossi", "fagioli rossi"), ("fagioli_neri", "fagioli neri")]):
            for si, sd in enumerate(["riso", "pane_int"]):
                if pk == "lenticchie" and bk == "fagioli_neri" and si == 1: continue
                ing = [(pk, pg, "p", lab) if lab else (pk, pg, "p"), (bk, 80, "p", f"{bn} lessati"), ("passata", 150, "v"), ("cipolla", 40, "v"), ("peperoni", 60, "v"), ("cumino", 2, "x"), ("paprika", 2, "x"), ("peperoncino", 1, "x"), ("olio", 6, "f", "olio EVO"), (sd, SIDE_G[sd], "c")]
                st = ["Soffriggi {cipolla} e {peperoni} a dadini in {olio} per 6 minuti.", "Aggiungi {" + pk + "} e rosolalo con {cumino}, {paprika} e {peperoncino} per 5 minuti.", "Unisci {passata} e {" + bk + "} e cuoci a fuoco dolce per 25 minuti.", side_carb_step(sd)]
                add(f"Chili di {pn} e {bn} con {SIDE_LAB[sd]}", "cena" if (ci + fi + si) % 2 else "pranzo", 45, ing, st, "Ottimo per il meal prep: si conserva 4 giorni.", "messicana", ["padella", "meal_prep"], 1, fam="messico")

# ======================= MEDITERRANEO / GRECIA / MEDIO ORIENTE =======================
def gen_med():
    for pi, (pk, pn, pg, ps) in enumerate([("pollo", "pollo", 120, "Taglia {pollo} a cubetti, marinalo con {limone}, {origano} e un filo d'olio per 15 minuti e cuocilo in padella o sulla piastra per 8-10 minuti."),
                                          ("tacchino", "tacchino", 120, "Taglia {tacchino} a cubetti, marinalo con {limone}, {origano} e un filo d'olio per 15 minuti e cuocilo per 8-10 minuti."),
                                          ("maiale", "maiale", 120, "Taglia {maiale} a cubetti, marinalo con {limone}, {origano} e un filo d'olio per 15 minuti e cuocilo per 10-12 minuti."),
                                          ("agnello", "agnello", 120, "Taglia {agnello} a cubetti, marinalo con {limone}, {origano} e un filo d'olio per 20 minuti e cuocilo per 10-12 minuti.")]):
        for bi, (b, bn) in enumerate([("pane_pita", "pita"), ("riso", "riso"), ("couscous", "cous cous")]):
            ing = [(pk, pg, "p"), (b, SIDE_G[b] if b in SIDE_G else 70, "c"), ("greco0", 70, "p", "yogurt greco 0%"), ("cetrioli", 60, "v"), ("pomodori", 80, "v"), ("limone", 15, "x"), ("origano", 1, "x"), ("olio", 5, "f", "olio EVO"), ("cipolla", 20, "v")]
            st = [ps, "Mescola {greco0} con {cetrioli} grattugiati (strizzati) e un pizzico di sale per una salsa tzatziki.", side_carb_step(b) if b != "pane_pita" else "Scalda {pane_pita} 1 minuto per lato.", "Servi gli spiedini con {pomodori}, {cipolla} a fettine e la salsa."]
            add(f"Souvlaki di {pn} con tzatziki e {bn}", "cena" if (pi + bi) % 2 else "pranzo", 35, ing, st, "", "mediterranea", ["padella"], 1, fam="med")
    GR = [("feta e olive", [("feta", 60, "f"), ("olive", 20, "f")]), ("pollo", [("pollo", 100, "p"), ("feta", 40, "f")]), ("ceci", [("ceci", 100, "p", "ceci lessati"), ("feta", 40, "f")]),
          ("tonno", [("tonno_nat", 80, "p"), ("feta", 40, "f")]), ("uova", [("uova", 100, "p"), ("feta", 40, "f")]), ("gamberi", [("gamberi", 100, "p"), ("feta", 40, "f")])]
    for gi, (gn, gings) in enumerate(GR):
        for bi, (b, bn) in enumerate([("pane_pita", "pita"), ("pane_int", "pane integrale"), ("bulgur", "bulgur")]):
            ing = list(gings) + [("pomodori", 120, "v"), ("cetrioli", 100, "v"), ("cipolla", 25, "v"), ("olive", 10, "f") if gn != "feta e olive" else ("origano", 1, "x"), ("olio", 8, "f", "olio EVO"), ("origano", 1, "x"), (b, SIDE_G[b], "c")]
            ing = list(dict((i[0], i) for i in ing).values())
            st = [("Cuoci {pollo} a cubetti in padella 7 minuti." if gn == "pollo" else "Cuoci {uova} sode 9 minuti e sgusciale." if gn == "uova" else "Salta {gamberi} sgusciati 3 minuti." if gn == "gamberi" else "Prepara gli ingredienti."), "Taglia {pomodori} e {cetrioli} a pezzi e {cipolla} a fettine sottili.", "Componi l'insalata con tutti gli ingredienti e condisci con {olio} e {origano}.", side_carb_step(b)]
            add(f"Insalata greca con {gn} e {bn}", "pranzo", 20, ing, st, "", "mediterranea", ["pranzo_ufficio"], 1, fam="med")
    SH = [("peperoni e feta", [("peperoni", 150, "v"), ("feta", 40, "f")]), ("spinaci e ricotta", [("spinaci", 120, "v"), ("ricotta", 50, "p")]), ("zucchine e caprino", [("zucchine", 150, "v"), ("caprino", 40, "p")]), ("melanzane e mozzarella", [("melanzane", 120, "v"), ("mozzarella_light", 50, "p")])]
    for si_, (sn, sings) in enumerate(SH):
        for bi, b in enumerate(["pane_int", "pane_pita", "couscous"]):
            ing = [("uova", 150, "p"), ("passata", 150, "v"), ("cipolla", 40, "v"), ("cumino", 1, "x"), ("paprika", 2, "x"), ("olio", 6, "f", "olio EVO"), (b, SIDE_G[b] if b in SIDE_G else 60, "c")] + list(sings)
            vk = sings[0][0]
            st = ["Soffriggi {cipolla} in {olio} con {cumino} e {paprika} per 3 minuti.", veg_cook(vk, "padella"), "Aggiungi {passata} e cuoci 8 minuti finché si addensa.", f"Aggiungi {{{sings[1][0]}}}, forma delle conche e rompi {{uova}}; copri e cuoci 5-6 minuti.", side_carb_step(b) if b != "pane_pita" else "Scalda {pane_pita}."]
            ing = list(dict((i[0], i) for i in ing).values())
            if b == "pane_pita": ing = [(i[0], i[1], i[2], *i[3:]) for i in ing]
            add(f"Shakshuka con {sn} e {SIDE_LAB.get(b, 'pita')}", "pranzo" if (si_ + bi) % 2 else "cena", 30, ing, st, "", "mediterranea", ["padella"], 1, fam="med")

# ======================= AMERICANA: BURGER E SANDWICH =======================
def gen_us():
    PR = [("macinato", "manzo", 110, "macinato di manzo magro"), ("pollo_macinato", "pollo", 110, None), ("tacchino_macinato", "tacchino", 110, None), ("tofu", "tofu e ceci", 110, None), ("ceci", "ceci", 120, "ceci lessati")]
    CH = [("cheddar", 25, "cheddar"), ("mozzarella_light", 40, "mozzarella light"), ("emmental", 25, "emmental"), ("feta", 30, "feta")]
    SIDE = [("patate", "patate al forno"), ("patate", "patate in friggitrice"), ("patate_dolci", "patate dolci al forno")]
    for pi, (pk, pn, pg, lab) in enumerate(PR):
        for ci, (ch, cg, cn) in enumerate(CH):
            sd, sdn = SIDE[(pi + ci) % 3]
            ing = [(pk, pg, "p", lab) if lab else (pk, pg, "p"), ("panino_hamb", 70, "c", "panino da hamburger"), (ch, cg, "f"), ("lattuga", 30, "v"), ("pomodori", 60, "v"), ("senape", 6, "x"), (sd, 180, "c"), ("olio", 6, "f", "olio EVO")]
            meth = "friggitrice" if "friggitrice" in sdn else "forno"
            if pk in ("tofu", "ceci"):
                extra = ("ceci", 70, "p", "ceci lessati") if pk == "tofu" else None
                if extra: ing.append(extra)
                prep = "Schiaccia {tofu} con {ceci}, sale e paprika e forma un burger." if pk == "tofu" else "Schiaccia {ceci} con sale e paprika, aggiungi un cucchiaio di farina e forma un burger."
            else:
                prep = f"Forma un burger con {{{pk}}} e un pizzico di sale e pepe."
            if "farina" in prep: ing.append(("farina", 10, "c"))
            ing = list(dict((i[0], i) for i in ing).values())
            st = [prep, ("Taglia {" + sd + "} a bastoncini, condisci con {olio} e cuoci in forno a 220 °C per 30 minuti, girando a metà." if meth == "forno" else "Taglia {" + sd + "} a bastoncini, condisci con {olio} e cuoci in friggitrice ad aria a 200 °C per 22-25 minuti, scuotendo a metà."), "Cuoci il burger in padella antiaderente 4-5 minuti per lato, finché è cotto al centro; sul finale aggiungi " + "{" + ch + "} per farlo fondere.", "Tosta {panino_hamb} 1 minuto.", "Componi il panino con {senape}, {lattuga}, {pomodori} e il burger e servi con le patate."]
            add(f"Burger di {pn} con {cn} e {sdn}", "cena" if (pi + ci) % 2 else "pranzo", 40, ing, st, "", "americana", [meth, "padella"], 1, fam="usa")
    CL = [("pollo", "pollo", 90, "Cuoci {pollo} a fette in padella 6 minuti per lato."), ("tacchino", "tacchino", 90, "Cuoci {tacchino} a fette in padella 6 minuti per lato."), ("tonno_nat", "tonno", 70, "Scola {tonno_nat}.")]
    for ci, (ck, cn, cg, cst) in enumerate(CL):
        for bi, b in enumerate(["pane_int", "pane"]):
            for xi, (x, xg, xn) in enumerate([("uova", 50, "uovo"), ("avocado", 50, "avocado")]):
                ing = [(ck, cg, "p"), (b, 90, "c"), (x, xg, "p" if x == "uova" else "f"), ("pomodori", 60, "v"), ("lattuga", 25, "v"), ("maionese", 8, "f", "maionese")]
                st = [cst, "Cuoci {uova} sode 9 minuti e sgusciale." if x == "uova" else "Schiaccia {avocado} con un pizzico di sale.", "Tosta {" + b + "} 2 minuti.", "Farcisci con " + ("il " + cn if cn != "tonno" else "il tonno") + ", " + ("l'uovo a fette" if x == "uova" else "l'avocado") + ", {pomodori}, {lattuga} e {maionese}."]
                add(f"Club sandwich di {cn} con {xn}" + ("" if b == "pane_int" else " su pane bianco"), "pranzo", 25, ing, st, "", "americana", ["padella"], 1, fam="usa")
    for mi, (pk, pn) in enumerate([("pollo", "pollo"), ("tonno_nat", "tonno"), ("fesa_tacchino", "tacchino"), ("prosciutto_cotto", "prosciutto")]):
        ing = [("pasta", 85, "c"), ("cheddar", 40, "f"), ("latte_ps", 100, "c", "latte"), ("broccoli", 100, "v"), (pk, 80 if pk != "pollo" else 100, "p"), ("pepe", 0.5, "x"), ("noce_moscata", 0.3, "x")]
        st = [cook_carb("pasta"), "Cuoci {broccoli} a cimette a vapore 8 minuti.", "Scalda {latte_ps} con {cheddar} grattugiato, pepe e {noce_moscata} finché è cremoso.", "Unisci pasta, broccoli e " + ("{" + pk + "} a cubetti" if pk != "pollo" else "{pollo} cotto a cubetti in padella 7 minuti") + "; mescola e servi."]
        add(f"Mac and cheese light con {pn} e broccoli", "cena", 30, ing, st, "", "americana", ["padella"], 1, fam="usa")

# ======================= PIATTI ABBONDANTI (ipertrofia) =======================
def gen_big():
    PR = [("pollo", "pollo", 200), ("manzo_magro", "manzo", 180), ("salmone", "salmone", 180), ("tacchino", "tacchino", 200), ("tonno_nat", "tonno", 150), ("uova", "uova", 200), ("vitello", "vitello", 180), ("maiale", "maiale", 180)]
    CB = [("riso", 120), ("pasta", 130), ("patate", 350), ("couscous", 110), ("quinoa", 100), ("farro", 120), ("riso_int", 120), ("pane_int", 120)]
    FT = [("avocado", 80, "avocado"), ("olio", 15, "olio EVO"), ("noci", 30, "noci"), ("parmigiano", 30, "parmigiano"), ("burro_mandorle", 25, "burro di mandorle")]
    VG = ["broccoli", "zucchine", "spinaci", "fagiolini", "peperoni", "carote", "asparagi", "cavolfiore"]
    i = 0
    for pk, pn, pg in PR:
        for bi in range(3):
            c, cg = CB[(i + bi * 3) % len(CB)]
            f, fg, fn = FT[(i + bi) % len(FT)]
            v = VG[(i * 2 + bi) % len(VG)]
            if pk == "tonno_nat" and c in ("patate", "pane_int"): c, cg = "riso", 120
            ing = [(pk, pg, "p"), (c, cg, "c"), (v, 150, "v"), (f, fg, "f", fn if f in ("olio", "avocado", "noci", "parmigiano") else None)]
            ing = [tuple(x for x in it if x is not None) for it in ing]
            if pk == "uova": cook = "Cuoci {uova} in padella con un filo d'olio come omelette, 4 minuti per lato."
            elif pk == "tonno_nat": cook = "Scola {tonno_nat} e scaldalo 2 minuti in padella."
            else:
                m = "padella" if "padella" in prot_methods(pk) else prot_methods(pk)[0]
                cook = prot_cook(pk, m)
            fstep = "Completa il piatto con {" + f + "}."
            if f == "burro_mandorle": fstep = "Condisci con {burro_mandorle} stemperato con poca acqua calda."
            st = [cook, veg_cook(v, "padella"), side_carb_step(c), fstep]
            add(f"Piatto abbondante: {pn}, {CARB_LABEL.get(c, SIDE_LAB.get(c, c))}, {VEG_NAME[v]} e {fn}", "cena" if (i + bi) % 2 else "pranzo", 40, ing, st, "Porzione da fase di massa o dopo un allenamento intenso.", "italiana", ["padella"], 1, fam="abbondanti")
        i += 1
    for pi, (pk, pn, pg) in enumerate([("macinato", "ragù di manzo", 150), ("tacchino_macinato", "ragù di tacchino", 150), ("salsiccia", "salsiccia", 150), ("pollo", "pollo e funghi", 160), ("tonno_nat", "tonno e pomodorini", 120)]):
        for bi, b in enumerate(["pasta", "pasta_int"]):
            ing = [(b, 130, "c"), (pk, pg, "p"), ("passata", 150, "v"), ("olio", 12, "f", "olio EVO"), ("parmigiano", 25, "f"), ("cipolla", 40, "v")]
            if pk == "pollo": ing.insert(3, ("funghi", 120, "v"))
            st = ["Porta a bollore abbondante acqua salata.", "Soffriggi {cipolla} in {olio} per 4 minuti, aggiungi " + ("{" + pk + "} " + ("a straccetti e {funghi}" if pk == "pollo" else "sbriciolato" if pk != "tonno_nat" else "sgocciolato") + " e cuoci 8 minuti."), "Aggiungi {passata} e cuoci altri 12 minuti.", cook_carb(b), "Manteca la pasta nel sugo e completa con {parmigiano}."]
            add(f"Pasta XL {'integrale ' if b == 'pasta_int' else ''}al {pn}" if pn not in ("salsiccia",) else f"Pasta XL {'integrale ' if b == 'pasta_int' else ''}con salsiccia", "pranzo", 35, ing, st, "Porzione abbondante per giorni di allenamento.", "italiana", ["padella"], 1, fam="abbondanti")

# ======================= INSALATONE =======================
INSAL = [
 ("pollo, avocado e pomodorini", [("pollo", 110, "p"), ("avocado", 50, "f"), ("pomodori", 100, "v", "pomodorini"), ("lattuga", 60, "v"), ("olio", 6, "f", "olio EVO"), ("limone", 10, "x")], ["Cuoci {pollo} a fette sulla piastra 5 minuti per lato e taglialo a strisce.", "Componi l'insalata con {lattuga}, {pomodori} a metà, {avocado} a fette e il pollo; condisci con {olio} e {limone}."]),
 ("tonno, uova e fagiolini", [("tonno_nat", 80, "p"), ("uova", 100, "p"), ("fagiolini", 100, "v"), ("pomodori", 80, "v"), ("lattuga", 50, "v"), ("olive", 10, "f"), ("olio", 6, "f", "olio EVO")], ["Cuoci {uova} sode 9 minuti e {fagiolini} 10 minuti; raffredda.", "Componi con {lattuga}, {pomodori}, {fagiolini}, {tonno_nat}, le uova a spicchi e {olive}; condisci con {olio}."]),
 ("salmone affumicato, avocado e cetrioli", [("salmone_aff", 70, "p"), ("avocado", 50, "f"), ("cetrioli", 80, "v"), ("lattuga", 50, "v"), ("limone", 12, "x"), ("olio", 5, "f", "olio EVO")], ["Taglia {cetrioli} a rondelle e {avocado} a fette.", "Componi con {lattuga}, {salmone_aff}, {cetrioli} e {avocado}; condisci con {limone} e {olio}."]),
 ("gamberi, mango e rucola", [("gamberi", 120, "p"), ("mango", 80, "v"), ("rucola", 40, "v"), ("avocado", 30, "f"), ("lime", 12, "x"), ("olio", 5, "f", "olio EVO")], ["Salta {gamberi} sgusciati 3 minuti in padella.", "Componi con {rucola}, {mango} a cubetti, {avocado} e i gamberi; condisci con {lime} e {olio}."]),
 ("mozzarella, pomodori e basilico", [("mozzarella", 100, "p"), ("pomodori", 150, "v"), ("basilico", 4, "x"), ("lattuga", 40, "v"), ("olio", 8, "f", "olio EVO"), ("origano", 1, "x")], ["Taglia {mozzarella} e {pomodori} a fette.", "Componi con {lattuga}, {basilico}, {origano} e {olio}."]),
 ("bresaola, rucola e grana", [("bresaola", 70, "p"), ("rucola", 50, "v"), ("parmigiano", 25, "f"), ("pomodori", 100, "v"), ("olio", 6, "f", "olio EVO"), ("limone", 10, "x")], ["Componi con {rucola}, {pomodori}, {bresaola} e scaglie di {parmigiano}; condisci con {olio} e {limone}."]),
 ("tacchino, mela e noci", [("fesa_tacchino", 100, "p", "fesa di tacchino"), ("mela", 100, "v"), ("noci", 15, "f"), ("lattuga", 60, "v"), ("senape", 6, "x"), ("olio", 5, "f", "olio EVO"), ("limone", 10, "x")], ["Taglia {fesa_tacchino} a strisce e {mela} a cubetti.", "Mescola {senape}, {limone} e {olio} per il condimento.", "Componi con {lattuga}, tacchino, mela e {noci}."]),
 ("ceci, feta e cetrioli", [("ceci", 120, "p", "ceci lessati"), ("feta", 50, "f"), ("cetrioli", 80, "v"), ("pomodori", 80, "v"), ("lattuga", 40, "v"), ("olio", 6, "f", "olio EVO"), ("origano", 1, "x")], ["Componi con {lattuga}, {pomodori}, {cetrioli}, {ceci} e {feta} a cubetti; condisci con {olio} e {origano}."]),
 ("lenticchie, barbabietola e caprino", [("lenticchie", 120, "p", "lenticchie lessate"), ("barbabietole", 100, "v"), ("caprino", 50, "p"), ("rucola", 40, "v"), ("noci", 10, "f"), ("olio", 5, "f", "olio EVO")], ["Taglia {barbabietole} cotte a cubetti.", "Componi con {rucola}, {lenticchie}, {barbabietole}, {caprino} a pezzetti e {noci}; condisci con {olio}."]),
 ("uova sode, spinaci e bacon di tacchino", [("uova", 100, "p"), ("spinaci", 80, "v"), ("fesa_tacchino", 40, "p", "fesa di tacchino"), ("funghi", 60, "v"), ("olio", 6, "f", "olio EVO"), ("aceto_bals", 8, "x")], ["Cuoci {uova} sode 9 minuti e {fesa_tacchino} a striscioline in padella 3 minuti.", "Componi con {spinaci} crudi, {funghi} affettati sottili, tacchino e uova; condisci con {olio} e {aceto_bals}."]),
 ("manzo, rucola e pomodorini", [("manzo_magro", 110, "p"), ("rucola", 50, "v"), ("pomodori", 100, "v", "pomodorini"), ("parmigiano", 20, "f"), ("aceto_bals", 8, "x"), ("olio", 5, "f", "olio EVO")], ["Cuoci {manzo_magro} a fette sulla piastra rovente 2-3 minuti per lato e affettalo.", "Componi con {rucola}, {pomodori} a metà, la carne e scaglie di {parmigiano}; condisci con {aceto_bals} e {olio}."]),
 ("pollo, mais e peperoni", [("pollo", 110, "p"), ("mais", 60, "v"), ("peperoni", 80, "v"), ("lattuga", 60, "v"), ("greco0", 40, "p", "yogurt greco 0%"), ("limone", 10, "x")], ["Cuoci {pollo} a striscioline in padella 7 minuti.", "Mescola {greco0} con {limone} e un pizzico di sale.", "Componi con {lattuga}, {mais}, {peperoni} a listarelle e il pollo; condisci con la salsa."]),
 ("tofu, carote e cavolo rosso", [("tofu", 130, "p"), ("carote", 70, "v"), ("cavolo_rosso", 60, "v"), ("lattuga", 40, "v"), ("salsa_soia", 8, "x"), ("sesamo", 5, "f"), ("olio_sesamo", 4, "f", "olio di sesamo")], ["Rosola {tofu} a cubetti con {salsa_soia} per 8 minuti.", "Componi con {lattuga}, {carote} a julienne, {cavolo_rosso} e tofu; condisci con {olio_sesamo} e {sesamo}."]),
 ("pesce spada, arance e finocchi", [("pesce_spada", 120, "p"), ("arancia", 100, "v"), ("finocchi", 100, "v"), ("rucola", 30, "v"), ("olio", 6, "f", "olio EVO"), ("olive", 10, "f")], ["Cuoci {pesce_spada} sulla piastra 3 minuti per lato e taglialo a cubetti.", "Componi con {rucola}, {finocchi} affettati, {arancia} a spicchi, il pesce e {olive}; condisci con {olio}."]),
 ("merluzzo, fagiolini e patate", [("merluzzo", 140, "p"), ("fagiolini", 100, "v"), ("patate", 150, "c"), ("pomodori", 80, "v", "pomodorini"), ("olive", 10, "f"), ("olio", 6, "f", "olio EVO")], ["Lessa {patate} a pezzi 15 minuti e {fagiolini} 10 minuti.", "Cuoci {merluzzo} al vapore 8 minuti e sfaldalo.", "Componi con patate, fagiolini, {pomodori}, il merluzzo e {olive}; condisci con {olio}."]),
 ("polpo, patate e olive", [("polpo", 130, "p"), ("patate", 150, "c"), ("olive", 15, "f"), ("sedano", 40, "v"), ("prezzemolo", 4, "x"), ("olio", 6, "f", "olio EVO"), ("limone", 10, "x")], ["Lessa {polpo} (peso a crudo) 40-45 minuti e tagliato a pezzi.", "Lessa {patate} a cubetti 15 minuti.", "Mescola polpo, patate, {sedano} a rondelle, {olive}, {prezzemolo}, {olio} e {limone}."]),
 ("pollo, cavolo cinese e arachidi", [("pollo", 110, "p"), ("cavolo_cinese", 100, "v"), ("carote", 60, "v"), ("arachidi", 15, "f"), ("salsa_soia", 8, "x"), ("lime", 10, "x")], ["Cuoci {pollo} a striscioline in padella 7 minuti.", "Taglia {cavolo_cinese} a striscioline e {carote} a julienne.", "Condisci con {salsa_soia} e {lime}, completa con {arachidi} tritate."]),
 ("fagioli, tonno e cipolla rossa", [("tonno_nat", 80, "p"), ("fagioli", 100, "p", "fagioli cannellini lessati"), ("cipolla", 25, "v"), ("pomodori", 80, "v"), ("lattuga", 40, "v"), ("olio", 6, "f", "olio EVO")], ["Affetta {cipolla} sottilissima e {pomodori} a spicchi.", "Componi con {lattuga}, {fagioli}, {tonno_nat}, cipolla e pomodori; condisci con {olio}."]),
 ("pollo, pere e gorgonzola", [("pollo", 100, "p"), ("pera", 100, "v"), ("gorgonzola", 30, "f"), ("noci", 12, "f"), ("rucola", 40, "v"), ("olio", 4, "f", "olio EVO")], ["Cuoci {pollo} a fette in padella 6 minuti per lato e taglialo a strisce.", "Componi con {rucola}, {pera} a fettine, il pollo, {gorgonzola} a pezzetti e {noci}; condisci con {olio}."]),
 ("uova, patate e fagiolini (nizzarda)", [("uova", 100, "p"), ("patate", 150, "c"), ("fagiolini", 100, "v"), ("tonno_nat", 60, "p"), ("pomodori", 80, "v"), ("acciughe", 8, "p"), ("olive", 10, "f"), ("olio", 6, "f", "olio EVO")], ["Lessa {patate} a pezzi 15 minuti, {fagiolini} 10 minuti e {uova} 9 minuti.", "Componi con {pomodori}, patate, fagiolini, {tonno_nat}, le uova, {acciughe} e {olive}; condisci con {olio}."]),
 ("salmone, edamame e avocado", [("salmone", 110, "p"), ("edamame", 60, "v"), ("avocado", 40, "f"), ("lattuga", 50, "v"), ("salsa_soia", 8, "x"), ("sesamo", 4, "f")], ["Cuoci {salmone} in padella 3-4 minuti per lato e sfaldalo; cuoci {edamame} 5 minuti.", "Componi con {lattuga}, salmone, {edamame} e {avocado}; condisci con {salsa_soia} e {sesamo}."]),
 ("prosciutto crudo, fichi e caprino", [("prosciutto_crudo", 50, "p"), ("fichi", 100, "v"), ("caprino", 40, "p"), ("rucola", 40, "v"), ("noci", 10, "f"), ("aceto_bals", 6, "x")], ["Componi con {rucola}, {fichi} a spicchi, {prosciutto_crudo}, {caprino} a pezzetti e {noci}; condisci con {aceto_bals}."]),
 ("salsiccia, cime di rapa e cannellini", [("salsiccia", 80, "p"), ("cime_rapa", 100, "v"), ("fagioli", 100, "p", "fagioli cannellini lessati"), ("olio", 6, "f", "olio EVO"), ("aglio", 3, "x")], ["Lessa {cime_rapa} 5 minuti e scolale.", "Cuoci {salsiccia} a rondelle in padella 8 minuti.", "Mescola cime, {fagioli}, salsiccia, {olio} e {aglio} tritato."]),
 ("pollo, quinoa e spinaci", [("pollo", 100, "p"), ("quinoa", 50, "c"), ("spinaci", 60, "v"), ("pomodori", 80, "v"), ("olio", 6, "f", "olio EVO"), ("limone", 10, "x")], [cook_carb("quinoa"), "Cuoci {pollo} a cubetti in padella 7 minuti.", "Componi con {spinaci} crudi, {quinoa}, {pomodori} e pollo; condisci con {olio} e {limone}."]),
 ("tonno fresco, avocado e sesamo", [("tonno_fresco", 110, "p"), ("avocado", 50, "f"), ("lattuga", 50, "v"), ("cetrioli", 60, "v"), ("sesamo", 5, "f"), ("salsa_soia", 8, "x")], ["Cuoci {tonno_fresco} 2 minuti per lato in padella molto calda; taglialo a fette.", "Componi con {lattuga}, {cetrioli}, {avocado} e il tonno; condisci con {salsa_soia} e {sesamo}."]),
 ("ricotta, spinaci e pomodori secchi", [("ricotta", 80, "p"), ("spinaci", 80, "v"), ("pomodori_secchi", 15, "v"), ("pinoli", 10, "f"), ("olio", 5, "f", "olio EVO")], ["Tosta {pinoli} 2 minuti in padella asciutta.", "Componi con {spinaci} crudi, {pomodori_secchi} a pezzetti, {ricotta} a cucchiaiate e {pinoli}; condisci con {olio}."]),
 ("uova, avocado e gamberi", [("uova", 100, "p"), ("avocado", 50, "f"), ("gamberi", 90, "p"), ("lattuga", 50, "v"), ("pomodori", 80, "v"), ("limone", 10, "x")], ["Cuoci {uova} sode 9 minuti; salta {gamberi} sgusciati 3 minuti.", "Componi con {lattuga}, {pomodori}, {avocado}, gamberi e uova a spicchi; condisci con {limone}."]),
]
def gen_insalatone():
    for ii, (nm, ings, st0) in enumerate(INSAL):
        for si, (sd, sg) in enumerate([("pane_int", 70), ("patate", 150), ("riso", 55), ("farro", 55)] if ii % 2 == 0 else [("pane", 70), ("couscous", 55), ("quinoa", 50), ("pane_int", 70)]):
            if sd in [i[0] for i in ings]: continue
            if si > 2: continue
            if sd == "patate" and any(i[0] == "patate" for i in ings): continue
            ing = list(ings) + [(sd, sg, "c")]
            st = list(st0) + [side_carb_step(sd) if sd not in ("pane", "pane_int") else "Servi con {" + sd + "} a fette."]
            add(f"Insalatona di {nm} con {SIDE_LAB[sd]}", "pranzo" if si % 2 == 0 else "cena", 25, ing, st, "", "mediterranea", ["pranzo_ufficio"], 1, fam="insalatone")

def generate():
    gen_wrap(); gen_wok(); gen_giap(); gen_curry(); gen_messico(); gen_med(); gen_us(); gen_big(); gen_insalatone()
