# Famiglie: colazioni, spuntini, dessert, frullati.
from gen_core import *
from gen_b import side_carb_step, SIDE_LAB, SIDE_G

# frutta: id -> (nome, g porzione, frase di preparazione)
FRUIT = {
 "banana": ("banana", 110, "Taglia {k} a rondelle."), "mela": ("mela", 130, "Taglia {k} a cubetti."), "pera": ("pera", 130, "Taglia {k} a fettine."),
 "arancia": ("arancia", 150, "Pela al vivo {k} e tagliala a spicchi."), "mandarini": ("mandarini", 150, "Sbuccia {k} e dividili a spicchi."),
 "kiwi": ("kiwi", 140, "Sbuccia {k} e tagliali a fette."), "fragole": ("fragole", 130, "Lava {k}, eliminane il picciolo e tagliale a metà."),
 "mirtilli": ("mirtilli", 90, "Sciacqua {k}."), "lamponi": ("lamponi", 90, "Sciacqua delicatamente {k}."), "frutti_bosco_surg": ("frutti di bosco", 110, "Scongela {k} a temperatura ambiente per 10 minuti."),
 "uva": ("uva", 100, "Lava {k} e staccala dal grappolo."), "pesca": ("pesca", 140, "Taglia {k} a spicchi."), "albicocche": ("albicocche", 120, "Dividi {k} a metà ed elimina il nocciolo."),
 "ciliegie": ("ciliegie", 100, "Lava {k} ed elimina il nocciolo."), "mango": ("mango", 100, "Sbuccia {k} e taglialo a cubetti."), "ananas": ("ananas", 120, "Taglia {k} a pezzetti."),
 "fichi": ("fichi", 100, "Taglia {k} a metà."), "cachi": ("cachi", 120, "Taglia {k} a spicchi con il cucchiaio."), "melograno": ("melograno", 80, "Sgrana {k}."),
 "papaya": ("papaya", 130, "Sbuccia {k}, elimina i semi e taglia a cubetti."), "anguria": ("anguria", 220, "Taglia {k} a cubetti ed elimina i semi."), "melone": ("melone", 200, "Taglia {k} a cubetti."),
 "pompelmo": ("pompelmo", 160, "Pela al vivo {k} e dividilo a spicchi."), "prugne": ("prugne", 130, "Dividi {k} a metà ed elimina il nocciolo."),
}
FRUIT_BR = ["banana", "mela", "pera", "arancia", "kiwi", "fragole", "mirtilli", "lamponi", "frutti_bosco_surg", "pesca", "albicocche", "ciliegie", "mango", "ananas", "fichi", "cachi", "melograno", "papaya", "uva", "prugne"]
NUTS = [("noci", "noci", 12), ("mandorle", "mandorle", 15), ("nocciole", "nocciole", 15), ("anacardi", "anacardi", 15), ("pistacchi", "pistacchi", 12), ("pecan", "noci pecan", 12), ("arachidi", "burro di arachidi", 12)]
def fr(k, g=None):
    n, gg, prep = FRUIT[k]
    return (k, g or gg, "v"), prep.replace("{k}", "{" + k + "}")
def fruit_name(k): return FRUIT[k][0]
LIQ = [("latte_ps", "latte parzialmente scremato"), ("mandorla_drink", "bevanda di mandorla"), ("soia_drink", "bevanda di soia"), ("latte_sl", "latte senza lattosio")]
SPICE = [("cannella", "cannella"), ("vaniglia", "vaniglia"), ("cardamomo", "cardamomo"), ("zenzero_polvere", "zenzero"), ("cacao", "cacao")]

def gen_porridge():
    for fi, k in enumerate(FRUIT_BR):
        (fi_ing, prep) = fr(k)
        n = fruit_name(k)
        for j in range(2):
            nut = NUTS[(fi + j * 3) % len(NUTS)]
            sp = SPICE[(fi + j * 2) % len(SPICE)]
            liq = LIQ[(fi + j) % len(LIQ)]
            spname = sp[1]
            sp_amt = 1 if sp[0] != "cacao" else 8
            ing = [("avena", 55, "c"), (liq[0], 250, "c", liq[1]), fi_ing, (nut[0], nut[2], "f", nut[1] if nut[0] != "arachidi" else "burro di arachidi"), (sp[0], sp_amt, "x")]
            st = [f"Metti {{avena}} e {{{liq[0]}}} in un pentolino e mescola.", "Cuoci a fuoco medio-basso per 5-7 minuti, mescolando, finché diventa cremoso.", prep, f"Versa il porridge in una ciotola e completa con la frutta, {{{nut[0]}}} e {{{sp[0]}}}."]
            if j == 1:
                add(f"Porridge di avena con {n}, {nut[1] if nut[0]!='arachidi' else 'burro di arachidi'} e {spname}", "colazione", 10, ing, st, "Senza zucchero: la dolcezza viene dalla frutta.", "americana", ["padella"], 1, fam="colazioni")
            else:
                add(f"Porridge {n} e {nut[1] if nut[0]!='arachidi' else 'burro di arachidi'} ({spname})", "colazione", 10, ing, st, "", "americana", ["padella"], 1, fam="colazioni")
        # overnight oats
        yo = [("greco0", "yogurt greco 0%"), ("yogurt", "yogurt bianco"), ("kefir", "kefir")][fi % 3]
        seed = [("chia", "chia", 10), ("lino", "semi di lino", 10), ("zucca_semi", "semi di zucca", 10), ("sesamo", "sesamo", 8)][fi % 4]
        add(f"Overnight oats con {n} e {seed[1]}", "colazione", 5, [("avena", 50, "c"), (yo[0], 130, "p", yo[1]), ("latte_ps", 100, "c", "latte"), (seed[0], seed[2], "f", seed[1]), fr(k)[0]],
            [f"La sera prima mescola in un barattolo {{avena}}, {{{yo[0]}}}, {{latte_ps}} e {{{seed[0]}}}.", "Chiudi e lascia in frigorifero per almeno 6 ore.", fr(k)[1], "Al mattino aggiungi la frutta e mescola."], "Si prepara in 5 minuti la sera prima.", "americana", ["senza_cottura", "anticipo"], 1, fam="colazioni")

CRUNCH = [("granola", "granola", 35, "c"), ("cornflakes", "corn flakes", 30, "c"), ("avena", "fiocchi di avena", 30, "c"), ("noci", "noci", 12, "f"), ("mandorle", "mandorle", 12, "f"), ("cocco_rapè", "cocco rapè", 10, "f"), ("zucca_semi", "semi di zucca", 10, "f"), ("chia", "semi di chia", 10, "f"), ("pistacchi", "pistacchi", 12, "f"), ("nocciole", "nocciole", 12, "f")]
DRIZ = [("miele", "miele", 8), ("sciroppo_acero", "sciroppo d'acero", 8), ("marmellata", "marmellata", 15), ("cacao", "cacao amaro", 5), ("burro_mandorle", "burro di mandorle", 10)]
YOG = [("greco0", "yogurt greco 0%", 200, "p"), ("greco_intero", "yogurt greco intero", 170, "p"), ("yogurt_magro", "yogurt magro", 200, "p"), ("kefir", "kefir", 200, "p"), ("yogurt", "yogurt bianco", 200, "p")]
def gen_yogurt():
    for fi, k in enumerate(FRUIT_BR):
        n = fruit_name(k)
        for j in range(3):
            y = YOG[(fi + j) % len(YOG)]
            cr = CRUNCH[(fi * 2 + j * 3) % len(CRUNCH)]
            dz = DRIZ[(fi + j * 2) % len(DRIZ)]
            if dz[0] == "cacao" and k in ("arancia", "kiwi", "ananas", "papaya", "melograno"): dz = DRIZ[0]
            ing = [(y[0], y[2], "p", y[1]), fr(k)[0], (cr[0], cr[2], cr[3], cr[1] if cr[0] not in ("granola",) else "granola"), (dz[0], dz[2], "x" if dz[0] != "marmellata" else "c", dz[1])]
            st = [f"Versa {{{y[0]}}} in una ciotola.", fr(k)[1], f"Aggiungi la frutta e {{{cr[0]}}}.", f"Completa con {{{dz[0]}}}."]
            add(f"{['Ciotola di', 'Coppa di', 'Bowl di'][j]} {y[1]} con {n} e {cr[1]}", "colazione" if j != 2 else "spuntino", 5, ing, st, "", "americana", ["senza_cottura"], 1, fam="colazioni")

TOASTS = [
 ("ricotta, miele e fichi", [("ricotta", 60, "p"), ("miele", 8, "x"), ("fichi", 80, "v")], ["Spalma {ricotta} sul pane tostato.", "Taglia {fichi} a spicchi e adagiali sopra; completa con {miele}."]),
 ("avocado e uova", [("avocado", 60, "f"), ("uova", 100, "p"), ("pepe", 0.5, "x")], ["Cuoci {uova} in camicia o al tegamino 3 minuti.", "Schiaccia {avocado} sul pane tostato, aggiungi le uova e pepe."]),
 ("burro di arachidi e banana", [("arachidi", 15, "f", "burro di arachidi"), ("banana", 100, "v")], ["Spalma {arachidi} sul pane tostato.", "Taglia {banana} a rondelle e adagiala sopra."]),
 ("burro di mandorle e fragole", [("burro_mandorle", 15, "f"), ("fragole", 100, "v")], ["Spalma {burro_mandorle} sul pane tostato.", "Taglia {fragole} a fettine e adagiale sopra."]),
 ("formaggio spalmabile e salmone affumicato", [("spalmabile", 30, "f", "formaggio spalmabile"), ("salmone_aff", 40, "p"), ("cetrioli", 40, "v"), ("aneto", 2, "x")], ["Spalma {spalmabile} sul pane tostato.", "Aggiungi {salmone_aff}, {cetrioli} a rondelle e {aneto}."]),
 ("hummus e pomodori", [("hummus", 50, "f"), ("pomodori", 100, "v"), ("rucola", 20, "v")], ["Spalma {hummus} sul pane tostato.", "Aggiungi {pomodori} a fette e {rucola}."]),
 ("ricotta, pomodorini e basilico", [("ricotta", 60, "p"), ("pomodori", 100, "v", "pomodorini"), ("basilico", 3, "x"), ("olio", 4, "f")], ["Spalma {ricotta} sul pane tostato.", "Aggiungi {pomodori} a metà, {basilico} e un filo di {olio}."]),
 ("ricotta e marmellata", [("ricotta", 60, "p"), ("marmellata", 20, "c")], ["Spalma {ricotta} sul pane tostato e completa con {marmellata}."]),
 ("crema di nocciole e banana", [("crema_nocciole", 15, "f"), ("banana", 100, "v")], ["Spalma {crema_nocciole} sul pane tostato.", "Taglia {banana} a rondelle e adagiala sopra."]),
 ("prosciutto cotto e mozzarella", [("prosciutto_cotto", 40, "p"), ("mozzarella", 50, "p"), ("pomodori", 60, "v")], ["Farcisci il pane con {prosciutto_cotto}, {mozzarella} e {pomodori} a fette, poi tosta in padella 2 minuti per lato."]),
 ("tonno e avocado", [("tonno_nat", 60, "p"), ("avocado", 50, "f"), ("limone", 10, "x")], ["Schiaccia {avocado} con {limone} e spalmalo sul pane tostato.", "Completa con {tonno_nat} sgocciolato."]),
 ("uova strapazzate e spinaci", [("uova", 100, "p"), ("spinaci", 60, "v"), ("olio", 4, "f")], ["Salta {spinaci} 2 minuti con {olio}.", "Aggiungi {uova} sbattute e cuoci mescolando 2 minuti.", "Servi sul pane tostato."]),
 ("fiocchi di latte e cetrioli", [("fiocchi_latte", 100, "p", "fiocchi di latte"), ("cetrioli", 60, "v"), ("erba_cipollina", 2, "x")], ["Spalma {fiocchi_latte} sul pane tostato.", "Aggiungi {cetrioli} a rondelle ed {erba_cipollina}."]),
 ("caprino, pere e noci", [("caprino", 40, "p"), ("pera", 80, "v"), ("noci", 8, "f")], ["Spalma {caprino} sul pane tostato.", "Aggiungi {pera} a fettine e {noci} tritate."]),
 ("ricotta, pesche e pistacchi", [("ricotta", 60, "p"), ("pesca", 100, "v"), ("pistacchi", 8, "f")], ["Spalma {ricotta} sul pane tostato.", "Aggiungi {pesca} a fette e {pistacchi} tritati."]),
 ("mozzarella, pomodoro e origano", [("mozzarella", 60, "p"), ("pomodori", 100, "v"), ("origano", 1, "x"), ("olio", 4, "f")], ["Taglia {mozzarella} e {pomodori} a fette e adagiale sul pane.", "Cuoci in forno a 200 °C per 6 minuti, poi completa con {origano} e {olio}."]),
 ("bresaola e rucola", [("bresaola", 40, "p"), ("rucola", 20, "v"), ("parmigiano", 10, "f"), ("limone", 8, "x")], ["Disponi {bresaola} sul pane tostato.", "Aggiungi {rucola}, scaglie di {parmigiano} e {limone}."]),
 ("uova sode e avocado", [("uova", 100, "p"), ("avocado", 50, "f"), ("paprika", 1, "x")], ["Cuoci {uova} sode 9 minuti e sgusciale.", "Schiaccia {avocado} sul pane, aggiungi le uova a fette e {paprika}."]),
 ("yogurt greco, miele e noci", [("greco0", 100, "p", "yogurt greco 0%"), ("miele", 8, "x"), ("noci", 10, "f")], ["Spalma {greco0} sul pane tostato.", "Completa con {miele} e {noci} tritate."]),
 ("formaggio spalmabile e frutti di bosco", [("spalmabile", 30, "f", "formaggio spalmabile"), ("frutti_bosco_surg", 80, "v", "frutti di bosco"), ("miele", 6, "x")], ["Spalma {spalmabile} sul pane tostato.", "Aggiungi {frutti_bosco_surg} e {miele}."]),
 ("tahina e banana", [("tahina", 12, "f"), ("banana", 100, "v"), ("cannella", 1, "x")], ["Spalma {tahina} sul pane tostato.", "Aggiungi {banana} a rondelle e {cannella}."]),
 ("mele, cannella e ricotta", [("ricotta", 60, "p"), ("mela", 100, "v"), ("cannella", 1, "x"), ("miele", 6, "x")], ["Spalma {ricotta} sul pane tostato, aggiungi {mela} a fettine sottili e {cannella}.", "Completa con {miele}."]),
 ("pomodori secchi, caprino e rucola", [("caprino", 40, "p"), ("pomodori_secchi", 12, "v"), ("rucola", 20, "v")], ["Spalma {caprino} sul pane tostato.", "Aggiungi {pomodori_secchi} a pezzetti e {rucola}."]),
 ("feta, cetrioli e menta", [("feta", 40, "f"), ("cetrioli", 60, "v"), ("menta", 2, "x"), ("olio", 4, "f")], ["Sbriciola {feta} sul pane.", "Aggiungi {cetrioli} a rondelle, {menta} e {olio}."]),
]
BREADS = [("pane_int", "pane integrale", 70), ("pane", "pane", 70), ("fette_bisc", "fette biscottate", 35), ("piadina", "piadina", 80)]
def gen_toast():
    for ti, (nm, ings, st) in enumerate(TOASTS):
        for bi, (b, bn, bg) in enumerate(pick(BREADS, 3, ti)):
            mealk = "colazione" if bi != 1 else ("spuntino" if ti % 2 else "colazione")
            name = f"{bn.capitalize()} con {nm}" if b != "piadina" else f"Piadina con {nm}"
            steps = [f"Tosta {{{b}}} nel tostapane o in padella per 2 minuti per lato."] + list(st)
            add(name, mealk, 10, [(b, bg, "c")] + list(ings), steps, "", "italiana" if "avocado" not in nm else "americana", ["padella"] if b == "piadina" else ["senza_cottura"], 1, fam="colazioni")

PANC = [
 ("di avena e banana", [("avena", 50, "c"), ("uova", 50, "p"), ("albume", 60, "p"), ("banana", 100, "v")], ["Frulla {avena}, {uova}, {albume} e {banana} fino a una pastella liscia."]),
 ("alla ricotta", [("farina", 40, "c"), ("ricotta", 80, "p"), ("uova", 50, "p"), ("lievito", 3, "x")], ["Mescola {ricotta}, {uova}, {farina} e {lievito} fino a una pastella densa."]),
 ("proteici", [("avena", 40, "c"), ("whey", 30, "p", "proteine in polvere"), ("albume", 100, "p"), ("lievito", 3, "x")], ["Frulla {avena}, {whey}, {albume} e {lievito} con poca acqua fino a una pastella liscia."]),
 ("di grano saraceno", [("farina_int", 40, "c", "farina di grano saraceno o integrale"), ("latte_ps", 100, "c", "latte"), ("uova", 50, "p"), ("lievito", 3, "x")], ["Mescola {farina_int}, {latte_ps}, {uova} e {lievito} fino a una pastella senza grumi."]),
 ("al cocco", [("farina_riso", 40, "c"), ("cocco_rapè", 10, "f"), ("uova", 50, "p"), ("latte_ps", 100, "c", "latte"), ("lievito", 3, "x")], ["Mescola {farina_riso}, {cocco_rapè}, {uova}, {latte_ps} e {lievito}."]),
 ("alla zucca", [("farina_avena", 40, "c"), ("zucca", 80, "v"), ("uova", 50, "p"), ("cannella", 1, "x"), ("lievito", 3, "x")], ["Cuoci {zucca} a pezzi al vapore, schiacciala e mescola con {farina_avena}, {uova}, {cannella} e {lievito}."]),
]
PTOP = [("fragole", "fragole"), ("mirtilli", "mirtilli"), ("banana", "banana"), ("mela", "mela e cannella"), ("pesca", "pesche"), ("lamponi", "lamponi")]
SYR = [("miele", "miele", 10), ("sciroppo_acero", "sciroppo d'acero", 10), ("burro_mandorle", "burro di mandorle", 12), ("greco0", "yogurt greco", 60)]
def gen_pancake():
    for pi, (nm, ings, mix) in enumerate(PANC):
        for ti, (tk, tn) in enumerate(pick(PTOP, 4, pi)):
            if tk in [i[0] for i in ings]: continue
            sy = SYR[(pi + ti) % len(SYR)]
            ing = list(ings) + [fr(tk, 90)[0], (sy[0], sy[2], "x" if sy[0] != "greco0" else "p", sy[1])]
            st = mix + ["Scalda una padella antiaderente e versa la pastella a cucchiaiate.", "Cuoci 2 minuti per lato finché i pancake sono dorati.", fr(tk)[1], f"Servi con la frutta e {{{sy[0]}}}."]
            add(f"Pancake {nm} con {tn} e {sy[1]}", "colazione", 20, ing, st, "", "americana", ["padella"], 1, fam="colazioni")

SMO = [  # (nome, ingredienti, passaggi)
 ("banana e burro di arachidi", [("banana", 120, "v"), ("arachidi", 12, "f", "burro di arachidi"), ("latte_ps", 250, "c", "latte"), ("avena", 30, "c")]),
 ("fragole e yogurt", [("fragole", 150, "v"), ("greco0", 150, "p", "yogurt greco 0%"), ("latte_ps", 120, "c", "latte"), ("miele", 6, "x")]),
 ("mango e yogurt", [("mango", 130, "v"), ("yogurt", 150, "p", "yogurt bianco"), ("latte_ps", 100, "c", "latte")]),
 ("frutti di bosco e kefir", [("frutti_bosco_surg", 150, "v", "frutti di bosco"), ("kefir", 200, "p"), ("avena", 25, "c")]),
 ("spinaci, banana e mela", [("spinaci", 40, "v"), ("banana", 100, "v"), ("mela", 100, "v"), ("mandorla_drink", 200, "c", "bevanda di mandorla"), ("chia", 8, "f")]),
 ("cacao e banana", [("banana", 120, "v"), ("cacao", 8, "x"), ("latte_ps", 250, "c", "latte"), ("avena", 30, "c")]),
 ("ananas e zenzero", [("ananas", 150, "v"), ("zenzero", 5, "x"), ("yogurt", 120, "p", "yogurt bianco"), ("succo_arancia", 100, "c", "succo d'arancia")]),
 ("pesca e vaniglia", [("pesca", 150, "v"), ("greco0", 120, "p", "yogurt greco 0%"), ("vaniglia", 2, "x"), ("latte_ps", 100, "c", "latte")]),
 ("kiwi e spinaci", [("kiwi", 140, "v"), ("spinaci", 40, "v"), ("yogurt", 120, "p", "yogurt bianco"), ("mela", 80, "v")]),
 ("avocado e cacao", [("avocado", 60, "f"), ("banana", 80, "v"), ("cacao", 8, "x"), ("mandorla_drink", 250, "c", "bevanda di mandorla")]),
 ("lamponi e ricotta", [("lamponi", 120, "v"), ("ricotta_light", 80, "p", "ricotta light"), ("latte_ps", 150, "c", "latte")]),
 ("papaya e cocco", [("papaya", 150, "v"), ("cocco_rapè", 10, "f"), ("yogurt", 120, "p", "yogurt bianco"), ("latte_ps", 100, "c", "latte")]),
 ("carota, arancia e zenzero", [("carote", 100, "v"), ("arancia", 150, "v"), ("zenzero", 4, "x"), ("yogurt", 100, "p", "yogurt bianco")]),
 ("proteico banana e cacao", [("banana", 100, "v"), ("whey", 30, "p", "proteine in polvere"), ("cacao", 6, "x"), ("latte_ps", 250, "c", "latte"), ("avena", 30, "c")]),
 ("proteico frutti di bosco", [("frutti_bosco_surg", 120, "v", "frutti di bosco"), ("whey", 30, "p", "proteine in polvere"), ("greco0", 120, "p", "yogurt greco 0%"), ("latte_ps", 150, "c", "latte")]),
 ("proteico mango e cocco", [("mango", 120, "v"), ("whey", 30, "p", "proteine in polvere"), ("latte_cocco", 30, "f", "latte di cocco"), ("mandorla_drink", 200, "c", "bevanda di mandorla")]),
 ("da massa avena, banana e arachidi", [("avena", 60, "c"), ("banana", 130, "v"), ("arachidi", 25, "f", "burro di arachidi"), ("latte_intero", 300, "c", "latte intero"), ("whey", 30, "p", "proteine in polvere")]),
 ("melone e menta", [("melone", 200, "v"), ("menta", 3, "x"), ("greco0", 100, "p", "yogurt greco 0%")]),
 ("anguria e lime", [("anguria", 250, "v"), ("lime", 15, "x"), ("menta", 3, "x")]),
 ("barbabietola e mela", [("barbabietole", 80, "v"), ("mela", 130, "v"), ("zenzero", 4, "x"), ("yogurt", 100, "p", "yogurt bianco")]),
 ("fichi e datteri", [("fichi", 100, "v"), ("datteri", 25, "c"), ("latte_ps", 200, "c", "latte"), ("cannella", 1, "x")]),
 ("pera e spinaci", [("pera", 130, "v"), ("spinaci", 40, "v"), ("limone", 10, "x"), ("mandorla_drink", 200, "c", "bevanda di mandorla")]),
 ("cachi e yogurt", [("cachi", 130, "v"), ("yogurt", 150, "p", "yogurt bianco"), ("cannella", 1, "x")]),
 ("ciliegie e yogurt", [("ciliegie", 130, "v"), ("greco0", 130, "p", "yogurt greco 0%"), ("latte_ps", 100, "c", "latte")]),
 ("albicocche e mandorle", [("albicocche", 150, "v"), ("mandorla_drink", 200, "c", "bevanda di mandorla"), ("mandorle", 12, "f"), ("yogurt", 100, "p", "yogurt bianco")]),
 ("cavolo nero e ananas", [("cavolo_nero", 40, "v"), ("ananas", 130, "v"), ("banana", 80, "v"), ("mandorla_drink", 200, "c", "bevanda di mandorla")]),
 ("cetriolo, kiwi e menta", [("cetrioli", 100, "v"), ("kiwi", 120, "v"), ("menta", 3, "x"), ("yogurt", 100, "p", "yogurt bianco")]),
 ("uva e yogurt", [("uva", 130, "v"), ("yogurt", 150, "p", "yogurt bianco"), ("limone", 8, "x")]),
 ("pompelmo e lamponi", [("pompelmo", 150, "v"), ("lamponi", 80, "v"), ("greco0", 100, "p", "yogurt greco 0%")]),
]
def gen_smoothie():
    for si, (nm, ings) in enumerate(SMO):
        names = [i[0] for i in ings]
        st = ["Prepara la frutta e le verdure: sbuccia, lava e taglia a pezzi quello che serve.", "Metti tutti gli ingredienti nel frullatore: " + ", ".join("{" + n + "}" for n in names[:-1]) + f" e {{{names[-1]}}}." if len(names) > 1 else "Metti gli ingredienti nel frullatore.", "Frulla per 1 minuto fino a ottenere una consistenza liscia; aggiungi qualche cubetto di ghiaccio se lo vuoi più fresco.", "Versa in un bicchiere alto e bevi subito."]
        kc = kcal_of(ings)
        meal = "colazione" if kc >= 180 else "spuntino"
        name = ("Frullato " + nm) if not nm.startswith("proteico") and not nm.startswith("da massa") else ("Frullato " + nm)
        add(name, meal, 5, ings, st, "Per un frullato più denso usa frutta congelata.", "americana", ["senza_cottura", "veloce"], 1, fam="frullati")
        # versione "bowl" (frullato denso con topping)
        if si % 2 == 0 and meal == "colazione":
            top = CRUNCH[si % len(CRUNCH)]
            ing2 = [(i[0], i[1] * (0.7 if i[0] in ("latte_ps", "mandorla_drink", "latte_intero", "succo_arancia") else 1), *i[2:]) for i in ings] + [(top[0], top[2], top[3], top[1])]
            if top[0] in names: continue
            st2 = ["Frulla gli ingredienti con poco liquido fino a ottenere una crema densa.", "Versa in una ciotola e completa con {" + top[0] + "}."]
            add(f"Smoothie bowl {nm} con {top[1]}", "colazione", 7, ing2, st2, "", "americana", ["senza_cottura"], 1, fam="frullati")

def gen_chia_uova():
    for fi, k in enumerate(pick(FRUIT_BR, 12, 1)):
        n = fruit_name(k)
        lq = LIQ[fi % 4]
        add(f"Chia pudding con {n}", "colazione", 5, [("chia", 25, "f"), (lq[0], 200, "c", lq[1]), fr(k)[0], ("vaniglia", 1, "x"), ("miele", 6, "x")],
            [f"Mescola {{chia}}, {{{lq[0]}}}, {{vaniglia}} e {{miele}} in un barattolo.", "Lascia in frigorifero almeno 4 ore (meglio tutta la notte), mescolando dopo 10 minuti per evitare i grumi.", fr(k)[1], "Completa con la frutta."], "", "americana", ["senza_cottura", "anticipo"], 1, fam="colazioni")
    EG = [("spinaci", 60), ("pomodori", 100), ("funghi", 80), ("zucchine", 100), ("peperoni", 80), ("cipollotti", 50), ("asparagi", 100), ("broccoli", 80)]
    for vi, (v, g) in enumerate(EG):
        for si, (b, bn, bg) in enumerate([("pane_int", "pane integrale", 60), ("patate", "patate", 150)]):
            add(f"Uova strapazzate con {VEG_NAME_C(v)} e {bn}", "colazione", 15, [("uova", 120, "p"), (v, g, "v"), ("olio", 5, "f", "olio EVO"), (b, bg, "c")], [veg_cook(v, "padella"), "Sbatti {uova} con sale e pepe e versale nella padella a fuoco basso, mescolando piano finché sono cremose (2-3 minuti).", side_carb_step(b) if b != "pane_int" else "Tosta {pane_int}.", "Servi le uova con la verdura e {olio} a crudo."], "", "americana", ["padella"], 1, fam="colazioni")

def VEG_NAME_C(v):
    return {"cipollotti": "cipollotti", "pomodori": "pomodori"}.get(v, v)

# ======================= SPUNTINI =======================
def gen_spuntini():
    # frutta singola (anche < 100 kcal)
    for k in FRUIT:
        n, g, prep = FRUIT[k]
        extra = {"mela": ("cannella", 1, "Cospargi con {cannella}."), "pera": ("cannella", 1, "Cospargi con {cannella}."), "banana": ("cannella", 1, "Cospargi con {cannella}."), "arancia": None, "pompelmo": None, "ananas": ("menta", 2, "Completa con {menta} tritata."),
                 "anguria": ("menta", 2, "Completa con {menta} tritata."), "melone": ("menta", 2, "Completa con {menta} tritata."), "fragole": ("limone", 8, "Spruzza con succo di {limone}."), "mango": ("lime", 8, "Spruzza con succo di {lime}."), "papaya": ("lime", 8, "Spruzza con succo di {lime}.")}.get(k)
        ing = [(k, g, "v")]
        st = [prep.replace("{k}", "{" + k + "}")]
        nm = f"{n.capitalize()} fresc{'a' if n.endswith('a') and k not in ('papaya',) else 'o' if n.endswith('o') else 'i' if n.endswith('i') or n.endswith('e') else 'o'}"
        if extra:
            ing.append((extra[0], extra[1], "x")); st.append(extra[2])
            nm = f"{n.capitalize()} con {'cannella' if extra[0]=='cannella' else 'menta' if extra[0]=='menta' else 'limone' if extra[0]=='limone' else 'lime'}"
        else:
            st.append("Gustala fresca o fredda di frigorifero.")
            nm = f"{n.capitalize()} a spicchi" if k not in ("uva", "ciliegie", "melograno", "mirtilli", "lamponi") else f"{n.capitalize()} fresche" if n.endswith("e") else f"{n.capitalize()} fresca"
        add(nm, "spuntino", 3, ing, st, "", "mediterranea", ["senza_cottura", "veloce"], 1, fam="spuntini")
    # frutta + frutta secca / yogurt
    for fi, k in enumerate(FRUIT_BR):
        n = fruit_name(k)
        for j in range(2):
            nut = NUTS[(fi * 2 + j * 3) % len(NUTS)]
            add(f"{n.capitalize()} e {nut[1] if nut[0]!='arachidi' else 'burro di arachidi'}", "spuntino", 3, [fr(k, 100)[0], (nut[0], nut[2] - 2, "f", nut[1] if nut[0] != "arachidi" else "burro di arachidi")], [fr(k)[1], f"Servi la frutta con {{{nut[0]}}}."], "", "mediterranea", ["senza_cottura", "veloce"], 1, fam="spuntini")
        y = YOG[(fi + 1) % len(YOG)]
        add(f"{y[1].capitalize()} con {n}", "spuntino", 3, [(y[0], 125, "p", y[1]), fr(k, 90)[0]], [fr(k)[1], f"Versa {{{y[0]}}} in una coppetta e aggiungi la frutta."], "", "mediterranea", ["senza_cottura", "veloce"], 1, fam="spuntini")
    # verdure e dip
    VR = [("carote", 100, "Pela {k} e tagliale a bastoncini."), ("cetrioli", 120, "Taglia {k} a bastoncini."), ("sedano", 80, "Pulisci {k} e taglialo a bastoncini."), ("finocchi", 100, "Taglia {k} a spicchi sottili."),
          ("peperoni", 100, "Taglia {k} a striscioline."), ("ravanelli", 80, "Lava {k} e dividili a metà."), ("pomodori", 120, "Taglia {k} a spicchi.")]
    DIP = [("hummus", "hummus", 40, "Servi con {hummus} per intingere."), ("greco0", "salsa allo yogurt", 70, "Mescola {greco0} con un pizzico di sale, erba cipollina e limone per ottenere una salsa; servi per intingere."),
           ("avocado", "guacamole", 50, "Schiaccia {avocado} con limone e sale per ottenere un guacamole e servilo per intingere."), ("ricotta_light", "ricotta alle erbe", 50, "Mescola {ricotta_light} con erbe e sale e servi per intingere."),
           ("tahina", "salsa tahina", 12, "Stempera {tahina} con limone e acqua per ottenere una salsa e servila per intingere."), ("fiocchi_latte", "fiocchi di latte", 80, "Servi con {fiocchi_latte} per intingere.")]
    for vi, (v, g, prep) in enumerate(VR):
        for di, (d, dn, dg, dst) in enumerate(DIP):
            if d == "hummus" and False: continue
            add(f"{VEG_NAME_C2(v)} con {dn}", "spuntino", 5, [(v, g, "v"), (d, dg, "f" if d in ("avocado", "tahina", "hummus") else "p", dn if d not in ("hummus", "fiocchi_latte") else None)] if False else [tuple(x for x in (v, g, "v") if x is not None), tuple(x for x in (d, dg, "f" if d in ("avocado", "tahina", "hummus") else "p", dn if d not in ("hummus", "fiocchi_latte", "tahina", "avocado") else None) if x is not None)], [prep.replace("{k}", "{" + v + "}"), dst], "", "mediterranea", ["senza_cottura", "veloce"], 1, fam="spuntini")
    # gallette, crackers, pane con topping leggeri
    BASE = [("gallette", "Gallette", 20), ("crackers", "Crackers integrali", 25), ("fette_bisc", "Fette biscottate", 25), ("pane_int", "Pane integrale", 40)]
    TOP = [("hummus", 35, "f", "hummus", "Spalma {hummus}."), ("ricotta_light", 50, "p", "ricotta light", "Spalma {ricotta_light}."), ("tonno_nat", 50, "p", "tonno", "Distribuisci {tonno_nat} sgocciolato."), ("fiocchi_latte", 60, "p", "fiocchi di latte", "Spalma {fiocchi_latte}."),
           ("avocado", 40, "f", "avocado", "Schiaccia {avocado} con sale e limone e spalmalo."), ("prosciutto_cotto", 30, "p", "prosciutto cotto", "Adagia {prosciutto_cotto}."), ("bresaola", 30, "p", "bresaola", "Adagia {bresaola}."), ("spalmabile", 25, "f", "formaggio spalmabile", "Spalma {spalmabile}."),
           ("arachidi", 12, "f", "burro di arachidi", "Spalma {arachidi}."), ("marmellata", 15, "c", "marmellata", "Spalma {marmellata}."), ("salmone_aff", 25, "p", "salmone affumicato", "Adagia {salmone_aff}."), ("caprino", 30, "p", "caprino", "Spalma {caprino}.")]
    EXT = [("pomodori", 60, "pomodoro", "Aggiungi {pomodori} a fette."), ("cetrioli", 50, "cetrioli", "Aggiungi {cetrioli} a rondelle."), ("rucola", 15, "rucola", "Completa con {rucola}."), ("carote", 50, "carote", "Completa con {carote} grattugiate.")]
    ti = 0
    for bi, (b, bn, bg) in enumerate(BASE):
        for tj, (t, tg, tr, tl, tst) in enumerate(TOP):
            ext = EXT[(bi + tj) % len(EXT)] if t not in ("marmellata", "arachidi") else None
            ing = [(b, bg, "c"), (t, tg, tr, tl)]
            st = [f"Disponi {{{b}}} su un piatto.", tst]
            nm = f"{bn} con {tl}"
            if ext:
                ing.append((ext[0], ext[1], "v")); st.append(ext[3]); nm = f"{bn} con {tl} e {ext[2]}"
            add(nm, "spuntino", 5, ing, st, "", "italiana", ["senza_cottura", "veloce"], 1, fam="spuntini")
    # uova sode, formaggi, frutta secca
    for i, (nm, ing, st) in enumerate([
        ("Uova sode e pomodorini", [("uova", 100, "p"), ("pomodori", 120, "v", "pomodorini")], ["Cuoci {uova} in acqua bollente per 9 minuti, raffreddale e sgusciale.", "Servile con {pomodori}."]),
        ("Uova sode e cetrioli", [("uova", 100, "p"), ("cetrioli", 120, "v")], ["Cuoci {uova} sode per 9 minuti e sgusciale.", "Servile con {cetrioli} a rondelle e un pizzico di sale."]),
        ("Uova sode e carote", [("uova", 100, "p"), ("carote", 100, "v")], ["Cuoci {uova} sode per 9 minuti e sgusciale.", "Servile con {carote} a bastoncini."]),
        ("Parmigiano e pera", [("parmigiano", 30, "p"), ("pera", 130, "v")], ["Taglia {pera} a fette.", "Servi con scaglie di {parmigiano}."]),
        ("Parmigiano e mela", [("parmigiano", 30, "p"), ("mela", 130, "v")], ["Taglia {mela} a fette.", "Servi con scaglie di {parmigiano}."]),
        ("Mozzarella e pomodorini", [("mozzarella", 60, "p"), ("pomodori", 100, "v", "pomodorini"), ("basilico", 3, "x")], ["Taglia {mozzarella} e {pomodori} a pezzi.", "Condisci con {basilico} spezzettato."]),
        ("Feta e anguria", [("feta", 40, "f"), ("anguria", 200, "v"), ("menta", 2, "x")], ["Taglia {anguria} a cubetti e {feta} a dadini.", "Completa con {menta}."]),
        ("Ricotta e miele", [("ricotta", 80, "p"), ("miele", 8, "x")], ["Versa {ricotta} in una coppetta e completa con {miele}."]),
        ("Ricotta e frutti di bosco", [("ricotta", 80, "p"), ("frutti_bosco_surg", 80, "v", "frutti di bosco")], ["Versa {ricotta} in una coppetta e completa con {frutti_bosco_surg}."]),
        ("Cottage e ananas", [("fiocchi_latte", 100, "p", "fiocchi di latte"), ("ananas", 100, "v")], ["Taglia {ananas} a pezzetti.", "Servi con {fiocchi_latte}."]),
        ("Mix di mandorle e uvetta", [("mandorle", 20, "f"), ("uvetta", 15, "v")], ["Mescola {mandorle} e {uvetta} in una ciotolina."]),
        ("Noci e datteri", [("noci", 15, "f"), ("datteri", 20, "c")], ["Servi {noci} con {datteri} snocciolati."]),
        ("Pistacchi e albicocche secche", [("pistacchi", 15, "f"), ("albicocche_secche", 20, "c", "albicocche secche")], ["Servi {pistacchi} con {albicocche_secche}."]),
        ("Anacardi e fichi secchi", [("anacardi", 15, "f"), ("fichi_secchi", 20, "c")], ["Servi {anacardi} con {fichi_secchi}."]),
        ("Nocciole e cioccolato fondente", [("nocciole", 15, "f"), ("fondente", 15, "f", "cioccolato fondente")], ["Servi {nocciole} con {fondente} a quadretti."]),
        ("Mandorle e prugne secche", [("mandorle", 15, "f"), ("prugne_secche", 20, "c")], ["Servi {mandorle} con {prugne_secche}."]),
        ("Noci pecan e mirtilli rossi secchi", [("pecan", 15, "f"), ("mirtilli_rossi", 15, "c")], ["Mescola {pecan} e {mirtilli_rossi}."]),
        ("Edamame al vapore", [("edamame", 150, "p"), ("sale", 1, "x")], ["Cuoci {edamame} a vapore o in acqua bollente per 5 minuti.", "Scola e condisci con un pizzico di {sale}."]),
        ("Ceci croccanti al forno", [("ceci", 120, "p", "ceci lessati"), ("olio", 4, "f"), ("paprika", 2, "x"), ("cumino", 1, "x")], ["Scola e asciuga bene {ceci}.", "Condiscili con {olio}, {paprika} e {cumino} e cuoci in forno a 200 °C per 25-30 minuti, mescolando a metà, finché sono croccanti."]),
        ("Popcorn fatti in casa", [("popcorn", 25, "c", "mais da popcorn"), ("olio", 3, "f")], ["Scalda {olio} in una pentola con coperchio, aggiungi {popcorn} e copri.", "Scuoti la pentola finché gli scoppi si fermano, poi condisci con un pizzico di sale."]),
        ("Tonno e fagioli su gallette", [("tonno_nat", 60, "p"), ("fagioli", 60, "p", "fagioli lessati"), ("gallette", 20, "c"), ("cipolla", 15, "v")], ["Mescola {tonno_nat}, {fagioli} e {cipolla} a fettine.", "Servi sulle {gallette}."]),
        ("Uovo, avocado e pomodoro", [("uova", 50, "p"), ("avocado", 50, "f"), ("pomodori", 60, "v")], ["Cuoci {uova} sode 9 minuti.", "Servi a fettine con {avocado} e {pomodori}."]),
        ("Olive e cubetti di formaggio", [("olive", 30, "f"), ("emmental", 30, "p")], ["Servi {olive} con {emmental} a cubetti."]),
        ("Fichi e prosciutto crudo", [("fichi", 100, "v"), ("prosciutto_crudo", 30, "p")], ["Taglia {fichi} a metà e avvolgili con {prosciutto_crudo}."]),
        ("Melone e prosciutto crudo", [("melone", 150, "v"), ("prosciutto_crudo", 30, "p")], ["Taglia {melone} a spicchi e servi con {prosciutto_crudo}."]),
        ("Spiedini di frutta e yogurt", [("fragole", 80, "v"), ("kiwi", 80, "v"), ("greco0", 70, "p", "yogurt greco 0%")], ["Taglia {fragole} e {kiwi} a pezzi e infilzali negli stecchi.", "Servi con {greco0} per intingere."]),
        ("Bastoncini di sedano e burro di arachidi", [("sedano", 100, "v"), ("arachidi", 15, "f", "burro di arachidi")], ["Taglia {sedano} a bastoncini.", "Servi con {arachidi}."]),
        ("Carote e hummus", [("carote", 100, "v"), ("hummus", 40, "f")], ["Taglia {carote} a bastoncini.", "Servi con {hummus}."]),
    ]):
        add(nm, "spuntino", 5, ing, st, "", "mediterranea", ["senza_cottura", "veloce"] if "forno" not in " ".join(st) else ["forno"], 1, fam="spuntini")

def VEG_NAME_C2(v):
    return {"carote": "Carote", "cetrioli": "Cetrioli", "sedano": "Sedano", "finocchi": "Finocchi", "peperoni": "Peperoni", "ravanelli": "Ravanelli", "pomodori": "Pomodori"}[v]

# ======================= DESSERT =======================
TREAT = [
 ("al forno con cannella e noci", [("cannella", 1, "x"), ("noci", 12, "f")], ["Accendi il forno a 180 °C.", "{P}", "Disponi la frutta in una pirofila, cospargi con {cannella} e {noci} tritate e cuoci per 20 minuti.", "Servi tiepida."], ["forno"], ("mela", "pera", "pesca", "albicocche", "prugne", "fichi", "banana", "ananas")),
 ("con ricotta e miele", [("ricotta_light", 80, "p", "ricotta light"), ("miele", 8, "x")], ["{P}", "Versa {ricotta_light} in una coppa, aggiungi la frutta e completa con {miele}."], ["senza_cottura"], ("fragole", "pesca", "albicocche", "fichi", "pera", "ciliegie", "mirtilli", "kiwi", "arancia", "prugne", "lamponi", "mango")),
 ("alla piastra con yogurt e pistacchi", [("greco0", 100, "p", "yogurt greco 0%"), ("pistacchi", 10, "f"), ("miele", 6, "x")], ["{P}", "Scalda una piastra o una padella antiaderente e cuoci la frutta 2-3 minuti per lato.", "Servi con {greco0}, {pistacchi} tritati e {miele}."], ["padella"], ("pesca", "ananas", "banana", "albicocche", "pera", "mela", "fichi", "mango", "papaya")),
 ("con cioccolato fondente", [("fondente", 20, "f", "cioccolato fondente")], ["{P}", "Sciogli {fondente} a bagnomaria.", "Servi la frutta con il cioccolato fuso."], ["senza_cottura"], ("fragole", "banana", "pera", "arancia", "lamponi", "ciliegie", "kiwi", "mandarini", "ananas")),
 ("in coppa con crema di yogurt e cacao", [("greco0", 120, "p", "yogurt greco 0%"), ("cacao", 5, "x"), ("miele", 6, "x")], ["{P}", "Mescola {greco0}, {cacao} e {miele}.", "Alterna crema e frutta in una coppa."], ["senza_cottura"], ("fragole", "banana", "pera", "lamponi", "ciliegie", "pesca", "arancia", "mirtilli")),
]
def gen_dessert():
    for ti, (tn, extras, steps, methods, fruits) in enumerate(TREAT):
        for fi, k in enumerate(fruits):
            n = fruit_name(k)
            fing, prep = fr(k)
            ing = [fing] + list(extras)
            st = [s.replace("{P}", prep) for s in steps]
            add(f"{n.capitalize()} {tn}" if not n.endswith("e") or True else f"{n.capitalize()} {tn}", "dessert", 15 if "forno" in methods else 8, ing, st, "", "italiana", methods, 1, fam="dessert")
    # nice cream, mousse, budini, torte, biscotti, crumble, crepes
    for fi, k in enumerate(["fragole", "mango", "mirtilli", "pesca", "ananas", "lamponi", "kiwi", "papaya", "ciliegie", "albicocche", "frutti_bosco_surg", "cachi"]):
        n = fruit_name(k)
        add(f"Gelato di banana e {n}", "dessert", 8, [("banana", 120, "v"), (k, 80, "v"), ("greco0", 50, "p", "yogurt greco 0%")], ["Taglia {banana} a rondelle e congela con la frutta per almeno 4 ore.", "Frulla {banana} congelata, {" + k + "} e {greco0} fino a una crema soffice.", "Servi subito o riponi in freezer 20 minuti per una consistenza più soda."], "Congela la banana già a rondelle: si frulla meglio.", "americana", ["senza_cottura", "anticipo"], 1, fam="dessert")
    for i, (nm, ing, st, meth, cu) in enumerate([
        ("Mousse di yogurt greco e cacao", [("greco0", 200, "p", "yogurt greco 0%"), ("cacao", 10, "x"), ("miele", 10, "x")], ["Mescola {greco0}, {cacao} e {miele} fino a una crema omogenea.", "Riponi in frigorifero 30 minuti e servi."], ["senza_cottura"], "italiana"),
        ("Mousse di ricotta e cacao", [("ricotta", 120, "p"), ("cacao", 10, "x"), ("miele", 10, "x")], ["Sbatti {ricotta} con {cacao} e {miele} fino a una crema liscia.", "Lascia riposare in frigorifero 30 minuti."], ["senza_cottura"], "italiana"),
        ("Mousse di avocado e cacao", [("avocado", 100, "f"), ("cacao", 12, "x"), ("sciroppo_acero", 15, "x"), ("mandorla_drink", 50, "c", "bevanda di mandorla")], ["Frulla {avocado}, {cacao}, {sciroppo_acero} e {mandorla_drink} fino a una crema liscia.", "Riponi in frigorifero 30 minuti."], ["senza_cottura"], "americana"),
        ("Budino di chia e cacao", [("chia", 25, "f"), ("mandorla_drink", 200, "c", "bevanda di mandorla"), ("cacao", 8, "x"), ("sciroppo_acero", 10, "x")], ["Mescola {chia}, {mandorla_drink}, {cacao} e {sciroppo_acero}.", "Lascia in frigorifero 4 ore, mescolando dopo 10 minuti."], ["senza_cottura", "anticipo"], "americana"),
        ("Budino di chia e cocco con mango", [("chia", 25, "f"), ("latte_cocco", 80, "f", "latte di cocco"), ("mango", 80, "v"), ("sciroppo_acero", 8, "x")], ["Mescola {chia}, {latte_cocco} e {sciroppo_acero}.", "Lascia in frigorifero 4 ore.", "Servi con {mango} a cubetti."], ["senza_cottura", "anticipo"], "asiatica"),
        ("Panna cotta light allo yogurt", [("greco0", 150, "p", "yogurt greco 0%"), ("latte_scremato", 100, "x", "latte scremato"), ("miele", 12, "x"), ("vaniglia", 2, "x"), ("fragole", 80, "v")], ["Scalda {latte_scremato} con {miele} e {vaniglia}, lascia intiepidire e unisci {greco0}.", "Versa in coppette e raffredda 3 ore.", "Servi con {fragole} a pezzetti."], ["anticipo"], "italiana"),
        ("Tiramisù light in coppa", [("savoiardi", 0, "x")] if False else [("biscotti", 30, "c", "biscotti secchi"), ("ricotta_light", 100, "p", "ricotta light"), ("caffe", 60, "x", "caffè"), ("cacao", 5, "x"), ("miele", 8, "x")], ["Mescola {ricotta_light} e {miele}.", "Bagna {biscotti} nel {caffe} freddo e disponili in una coppa.", "Alterna con la crema e spolvera con {cacao}."], ["senza_cottura"], "italiana"),
        ("Cheesecake light al vasetto", [("biscotti", 25, "c", "biscotti secchi"), ("ricotta_light", 100, "p", "ricotta light"), ("greco0", 80, "p", "yogurt greco 0%"), ("frutti_bosco_surg", 80, "v", "frutti di bosco"), ("miele", 8, "x")], ["Sbriciola {biscotti} sul fondo di un vasetto.", "Mescola {ricotta_light}, {greco0} e {miele} e versa sopra.", "Completa con {frutti_bosco_surg} e raffredda 1 ora."], ["senza_cottura"], "americana"),
        ("Torta in tazza al cacao", [("farina", 30, "c"), ("cacao", 8, "x"), ("uova", 50, "p"), ("latte_ps", 40, "c", "latte"), ("lievito", 2, "x"), ("miele", 10, "x")], ["Mescola {farina}, {cacao}, {lievito} in una tazza.", "Aggiungi {uova}, {latte_ps} e {miele} e mescola bene.", "Cuoci al microonde 90 secondi a 800 W e lascia riposare 1 minuto."], ["senza_cottura"], "americana"),
        ("Torta in tazza alla mela e cannella", [("farina_avena", 30, "c"), ("mela", 80, "v"), ("uova", 50, "p"), ("cannella", 1, "x"), ("lievito", 2, "x")], ["Grattugia {mela}.", "Mescola {farina_avena}, {mela}, {uova}, {cannella} e {lievito} in una tazza.", "Cuoci al microonde 2 minuti a 800 W."], ["senza_cottura"], "americana"),
        ("Crumble di mele e avena", [("mela", 200, "v"), ("avena", 30, "c"), ("noci", 10, "f"), ("cannella", 1, "x"), ("miele", 10, "x")], ["Accendi il forno a 180 °C.", "Taglia {mela} a cubetti e disponila in una pirofila con {cannella}.", "Mescola {avena}, {noci} tritate e {miele} e distribuisci sopra.", "Cuoci per 25 minuti, finché la superficie è dorata."], ["forno"], "americana"),
        ("Crumble di pere e mandorle", [("pera", 200, "v"), ("avena", 30, "c"), ("mandorle", 12, "f"), ("cannella", 1, "x"), ("sciroppo_acero", 10, "x")], ["Accendi il forno a 180 °C.", "Taglia {pera} a cubetti e disponila in una pirofila con {cannella}.", "Mescola {avena}, {mandorle} tritate e {sciroppo_acero} e distribuisci sopra.", "Cuoci 25 minuti."], ["forno"], "americana"),
        ("Crumble di frutti di bosco e avena", [("frutti_bosco_surg", 150, "v", "frutti di bosco"), ("avena", 30, "c"), ("nocciole", 12, "f"), ("sciroppo_acero", 10, "x")], ["Accendi il forno a 180 °C.", "Distribuisci {frutti_bosco_surg} in una pirofila.", "Mescola {avena}, {nocciole} tritate e {sciroppo_acero} e distribuisci sopra.", "Cuoci 25 minuti."], ["forno"], "americana"),
        ("Biscotti di avena e banana", [("avena", 50, "c"), ("banana", 100, "v"), ("fondente", 12, "f", "gocce di cioccolato fondente")], ["Accendi il forno a 180 °C.", "Schiaccia {banana} e mescola con {avena} e {fondente} tritato.", "Forma 5-6 biscotti su una teglia con carta forno e cuoci 15 minuti."], ["forno"], "americana"),
        ("Biscotti di avena e mirtilli rossi", [("avena", 50, "c"), ("banana", 90, "v"), ("mirtilli_rossi", 15, "c"), ("noci", 10, "f")], ["Accendi il forno a 180 °C.", "Schiaccia {banana} e mescola con {avena}, {mirtilli_rossi} e {noci} tritate.", "Forma 5-6 biscotti e cuoci 15 minuti."], ["forno"], "americana"),
        ("Biscotti di avena e cocco", [("avena", 45, "c"), ("banana", 90, "v"), ("cocco_rapè", 12, "f"), ("cannella", 1, "x")], ["Accendi il forno a 180 °C.", "Mescola {banana} schiacciata, {avena}, {cocco_rapè} e {cannella}.", "Forma 5-6 biscotti e cuoci 15 minuti."], ["forno"], "americana"),
        ("Palline energetiche ai datteri e cacao", [("datteri", 40, "c"), ("avena", 20, "c"), ("mandorle", 15, "f"), ("cacao", 6, "x")], ["Frulla {datteri} snocciolati, {avena}, {mandorle} e {cacao} fino a un impasto appiccicoso.", "Forma 4-5 palline con le mani umide e riponi in frigorifero 20 minuti."], ["senza_cottura"], "americana"),
        ("Palline energetiche al cocco e albicocche", [("albicocche_secche", 40, "c", "albicocche secche"), ("cocco_rapè", 12, "f"), ("mandorle", 15, "f"), ("avena", 15, "c")], ["Frulla {albicocche_secche}, {mandorle} e {avena}.", "Forma 4-5 palline e rotolale nel {cocco_rapè}."], ["senza_cottura"], "americana"),
        ("Barrette di avena e burro di arachidi", [("avena", 50, "c"), ("arachidi", 30, "f", "burro di arachidi"), ("miele", 20, "x"), ("uvetta", 15, "v")], ["Scalda {arachidi} e {miele} in un pentolino finché si fondono.", "Mescola con {avena} e {uvetta}, pressa in uno stampo foderato e raffredda 1 ora.", "Taglia a barrette."], ["anticipo"], "americana"),
        ("Crêpes alla ricotta e fragole", [("farina", 40, "c"), ("uova", 50, "p"), ("latte_ps", 100, "c", "latte"), ("ricotta_light", 60, "p", "ricotta light"), ("fragole", 80, "v")], ["Sbatti {farina}, {uova} e {latte_ps} fino a una pastella fluida.", "Cuoci 2 crêpes in una padella antiaderente, 1 minuto per lato.", "Farciscile con {ricotta_light} e {fragole} a pezzetti e piegale."], ["padella"], "italiana"),
        ("Crêpes alla banana e cacao", [("farina", 40, "c"), ("uova", 50, "p"), ("latte_ps", 100, "c", "latte"), ("banana", 100, "v"), ("cacao", 6, "x")], ["Sbatti {farina}, {uova} e {latte_ps}.", "Cuoci 2 crêpes in padella antiaderente, 1 minuto per lato.", "Farciscile con {banana} a rondelle e spolvera con {cacao}."], ["padella"], "italiana"),
        ("Pesche ripiene di amaretti e ricotta", [("pesca", 150, "v"), ("ricotta_light", 60, "p", "ricotta light"), ("biscotti", 20, "c", "amaretti o biscotti secchi"), ("miele", 6, "x")], ["Accendi il forno a 180 °C.", "Dividi {pesca} a metà ed elimina il nocciolo.", "Mescola {ricotta_light}, {biscotti} sbriciolati e {miele} e riempi le pesche.", "Cuoci 20 minuti."], ["forno"], "italiana"),
        ("Coppa di yogurt, granola e frutti di bosco", [("greco0", 150, "p", "yogurt greco 0%"), ("granola", 25, "c"), ("frutti_bosco_surg", 80, "v", "frutti di bosco"), ("miele", 6, "x")], ["Alterna {greco0}, {granola} e {frutti_bosco_surg} in una coppa.", "Completa con {miele}."], ["senza_cottura"], "americana"),
        ("Semifreddo di yogurt e frutti di bosco", [("greco0", 150, "p", "yogurt greco 0%"), ("frutti_bosco_surg", 100, "v", "frutti di bosco"), ("miele", 12, "x"), ("biscotti", 15, "c")], ["Frulla {greco0}, {frutti_bosco_surg} e {miele}.", "Versa in uno stampo sopra {biscotti} sbriciolati e congela 4 ore.", "Togli dal freezer 10 minuti prima di servire."], ["anticipo"], "italiana"),
        ("Cioccolata calda proteica", [("latte_ps", 250, "c", "latte"), ("cacao", 10, "x"), ("whey", 20, "p", "proteine in polvere"), ("miele", 8, "x")], ["Scalda {latte_ps} senza farlo bollire.", "Sciogli {cacao} e {miele}, spegni e frulla con {whey} per evitare i grumi."], ["padella"], "americana"),
        ("Budino di riso al latte e vaniglia", [("riso", 40, "c"), ("latte_ps", 250, "c", "latte"), ("vaniglia", 2, "x"), ("miele", 10, "x"), ("cannella", 1, "x")], ["Cuoci {riso} nel {latte_ps} con {vaniglia} a fuoco basso per 25 minuti, mescolando spesso.", "Dolcifica con {miele}, versa in coppette e spolvera con {cannella}."], ["padella"], "americana"),
        ("Zabaione light", [("uova", 50, "p"), ("miele", 15, "x"), ("vino_bianco", 40, "x", "vino dolce o bianco"), ("fragole", 80, "v")], ["Monta {uova} con {miele} a bagnomaria.", "Aggiungi {vino_bianco} a filo continuando a montare 6-8 minuti finché è gonfio.", "Servi tiepido con {fragole}."], ["padella"], "italiana"),
        ("Mela caramellata alla cannella", [("mela", 180, "v"), ("cannella", 2, "x"), ("sciroppo_acero", 10, "x"), ("noci", 8, "f")], ["Taglia {mela} a spicchi.", "Cuoci in padella antiaderente con {sciroppo_acero} e {cannella} per 6-8 minuti.", "Servi con {noci} tritate."], ["padella"], "americana"),
        ("Banana alla piastra con cocco", [("banana", 130, "v"), ("cocco_rapè", 8, "f"), ("cannella", 1, "x"), ("sciroppo_acero", 8, "x")], ["Taglia {banana} a metà nel senso della lunghezza.", "Cuoci in padella calda 2 minuti per lato.", "Servi con {cocco_rapè}, {cannella} e {sciroppo_acero}."], ["padella"], "americana"),
    ]):
        add(nm, "dessert", 20 if "forno" in meth or "anticipo" in meth else 12, ing, st, "", cu, meth, 1, fam="dessert")

def generate():
    gen_porridge(); gen_yogurt(); gen_toast(); gen_pancake(); gen_smoothie(); gen_chia_uova(); gen_spuntini(); gen_dessert()
