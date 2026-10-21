# Ricette di Jarvis Food. Pesi a crudo, per 1 persona.
# Ingrediente: (chiave alimento, grammi base, ruolo, etichetta opzionale)
# Ruoli per il calcolo delle grammature:
#   c = fonte di carboidrati, scalabile    p = fonte di proteine, scalabile
#   f = grassi da condimento, scalabile    v = verdura/frutta, varia poco (±25-50%)
#   x = fisso (spezie, aglio, aromi)
# I passaggi usano {chiave} per inserire la grammatura calcolata (es. "Cuoci {pasta}" -> "Cuoci 95 g di pasta").
# img = parole per cercare una foto con licenza libera.

R = []
def r(id, name, meal, minutes, ings, steps, img, tip=""):
    R.append(dict(id=id, name=name, meal=meal, minutes=minutes, ings=ings, steps=steps, img=img, tip=tip))

# ---------------- COLAZIONE ----------------
r("porridge_banana", "Porridge banana e noci", "colazione", 10,
  [("avena", 70, "c"), ("latte_sl", 250, "c", "latte senza lattosio"), ("banana", 110, "v"), ("noci", 15, "f"),
   ("greco0", 100, "p", "yogurt greco 0%"), ("cannella", 1, "x")],
  ["Metti {avena} e {latte_sl} in un pentolino antiaderente e mescola a freddo.",
   "Scalda a fuoco medio-basso per 5-7 minuti, mescolando spesso, finché diventa cremoso. Se è troppo denso aggiungi poca acqua.",
   "Taglia {banana} a rondelle e spezzetta {noci}.",
   "Versa il porridge in una ciotola, completa con banana, noci, {greco0} e un pizzico di cannella."],
  "porridge banana walnuts bowl", "La banana dà già dolcezza: non serve zucchero.")
r("yogurt_granola", "Yogurt greco, granola e mirtilli", "colazione", 3,
  [("greco0", 200, "p", "yogurt greco 0%"), ("granola", 40, "c"), ("mirtilli", 100, "v"), ("miele", 7, "x")],
  ["Versa {greco0} in una ciotola.", "Aggiungi {granola} e {mirtilli}.", "Completa con {miele}."],
  "greek yogurt granola blueberries bowl")
r("toast_uova_avocado", "Toast integrale, uova e avocado", "colazione", 10,
  [("pane_int", 80, "c", "pane integrale"), ("uova", 100, "p"), ("avocado", 50, "f"), ("pomodori", 80, "v"), ("pepe", 0.5, "x")],
  ["Tosta {pane_int} in padella o nel tostapane.",
   "Cuoci {uova} in padella antiaderente a fuoco medio, strapazzandole per 2-3 minuti.",
   "Schiaccia {avocado} con una forchetta e spalmalo sul pane.",
   "Aggiungi le uova e {pomodori} a fette, completa con una macinata di pepe."],
  "avocado toast scrambled eggs")
r("pancake_avena", "Pancake di avena e banana", "colazione", 15,
  [("avena", 50, "c"), ("uova", 50, "p"), ("albume", 60, "p"), ("banana", 100, "v"), ("miele", 10, "c")],
  ["Frulla {avena}, {uova}, {albume} e {banana} fino a ottenere una pastella liscia.",
   "Scalda una padella antiaderente e versa la pastella a cucchiaiate.",
   "Cuoci 2 minuti per lato finché i pancake sono dorati.", "Servi con {miele}."],
  "oat banana pancakes stack")
r("overnight_oats", "Overnight oats con chia e kiwi", "colazione", 5,
  [("avena", 50, "c"), ("greco0", 150, "p", "yogurt greco 0%"), ("latte_ps", 100, "c", "latte"), ("chia", 10, "f"), ("kiwi", 150, "v")],
  ["La sera prima mescola in un barattolo {avena}, {greco0}, {latte_ps} e {chia}.",
   "Chiudi e lascia in frigorifero per almeno 6 ore.", "Al mattino sbuccia e taglia {kiwi} e aggiungilo sopra."],
  "overnight oats chia kiwi jar")
r("fette_arachidi", "Fette biscottate, burro di arachidi e banana", "colazione", 5,
  [("fette_bisc", 30, "c"), ("arachidi", 15, "f"), ("banana", 120, "v"), ("latte_ps", 200, "p", "latte"),
   ("fiocchi_latte", 100, "p", "fiocchi di latte")],
  ["Spalma {arachidi} su {fette_bisc}.", "Aggiungi {banana} a rondelle sopra le fette.", "Accompagna con {latte_ps} e {fiocchi_latte}."],
  "peanut butter banana toast breakfast")
r("greco_mela_nocciole", "Yogurt greco, mela, cannella e nocciole", "colazione", 5,
  [("greco0", 200, "p", "yogurt greco 0%"), ("mela", 150, "v"), ("nocciole", 15, "f"), ("avena", 30, "c"), ("cannella", 1, "x")],
  ["Taglia {mela} a cubetti.", "In una ciotola unisci {greco0}, la mela e {avena}.",
   "Completa con {nocciole} tritate grossolanamente e una spolverata di cannella."],
  "greek yogurt apple cinnamon hazelnuts")
r("latte_cereali_fragole", "Latte, corn flakes e fragole", "colazione", 3,
  [("latte_ps", 250, "p", "latte"), ("cornflakes", 40, "c"), ("fragole", 150, "v"), ("mandorle", 10, "f"),
   ("greco0", 120, "p", "yogurt greco 0%")],
  ["Versa {latte_ps} in una tazza grande.", "Aggiungi {cornflakes}.", "Accompagna con {fragole}, {greco0} e {mandorle}."],
  "cornflakes milk strawberries bowl")
r("fiocchi_pane_miele", "Fiocchi di latte, pane integrale, miele e pera", "colazione", 5,
  [("fiocchi_latte", 150, "p"), ("pane_int", 60, "c", "pane integrale"), ("miele", 10, "c"), ("pera", 150, "v"), ("noci", 10, "f")],
  ["Tosta {pane_int}.", "Spalma {fiocchi_latte} sul pane e completa con {miele}.", "Accompagna con {pera} a spicchi e {noci}."],
  "cottage cheese toast honey pear")
r("smoothie_bowl", "Smoothie bowl frutti di bosco", "colazione", 5,
  [("frutti_bosco_surg", 150, "v", "frutti di bosco surgelati"), ("banana", 80, "v"), ("greco0", 150, "p", "yogurt greco 0%"),
   ("avena", 30, "c"), ("chia", 10, "f")],
  ["Frulla {frutti_bosco_surg}, {banana} e {greco0} fino a ottenere una crema densa.",
   "Versa in una ciotola.", "Completa con {avena} e {chia}."],
  "berry smoothie bowl")

# ---------------- PRANZO ----------------
r("pasta_pollo_zucchine", "Pasta pollo e zucchine", "pranzo", 20,
  [("pasta", 90, "c"), ("pollo", 130, "p", "petto di pollo"), ("zucchine", 200, "v"), ("olio", 10, "f", "olio EVO"),
   ("parmigiano", 10, "f"), ("aglio", 3, "x")],
  ["Metti a bollire l'acqua per la pasta e salala.",
   "Taglia {pollo} a straccetti e {zucchine} a rondelle sottili.",
   "In padella scalda {olio} con l'aglio, aggiungi le zucchine e cuoci 6-7 minuti.",
   "Unisci il pollo e cuoci 5-6 minuti, finché è cotto all'interno.",
   "Cuoci {pasta} al dente, scolala e saltala in padella. Completa con {parmigiano}."],
  "penne chicken zucchini pasta")
r("pasta_tonno", "Pasta tonno, pomodorini e rucola", "pranzo", 15,
  [("pasta", 90, "c"), ("tonno_nat", 80, "p", "tonno al naturale"), ("pomodori", 150, "v", "pomodorini"), ("rucola", 30, "v"),
   ("olio", 10, "f", "olio EVO"), ("capperi", 5, "x")],
  ["Cuoci {pasta} in acqua bollente salata.",
   "In padella scalda {olio}, aggiungi {pomodori} tagliati a metà e {capperi}, cuoci 3 minuti.",
   "Unisci {tonno_nat} sgocciolato e la pasta scolata al dente, salta 1 minuto.",
   "Fuori dal fuoco aggiungi {rucola}."],
  "pasta tuna cherry tomatoes arugula")
r("pasta_ceci", "Pasta e ceci", "pranzo", 25,
  [("pasta", 70, "c"), ("ceci", 150, "p", "ceci lessati"), ("passata", 80, "v", "passata"), ("cipolla", 30, "x"),
   ("olio", 10, "f", "olio EVO"), ("parmigiano", 5, "f")],
  ["In una pentola soffriggi la cipolla tritata con {olio} per 3 minuti.",
   "Aggiungi {ceci} sciacquati e {passata}, poi 400 ml di acqua calda e un pizzico di sale.",
   "Dopo 10 minuti frulla metà dei ceci per rendere cremoso il fondo.",
   "Versa {pasta} (meglio corta) e cuocila nel brodo, mescolando, per il tempo indicato.",
   "Servi con {parmigiano} e pepe."],
  "pasta e ceci chickpea pasta soup")
r("pasta_lenticchie", "Pasta e lenticchie", "pranzo", 25,
  [("pasta", 70, "c"), ("lenticchie", 150, "p", "lenticchie lessate"), ("carote", 50, "v"), ("sedano", 30, "x"),
   ("cipolla", 30, "x"), ("passata", 50, "v"), ("olio", 10, "f", "olio EVO")],
  ["Trita cipolla, sedano e {carote} e soffriggili con {olio} per 5 minuti.",
   "Aggiungi {lenticchie}, {passata} e 400 ml di acqua calda; cuoci 10 minuti.",
   "Versa {pasta} e cuocila nel brodo mescolando spesso.", "Lascia riposare 2 minuti e servi."],
  "pasta lentils soup italian")
r("poke_salmone", "Poke di riso, salmone e avocado", "pranzo", 20,
  [("basmati", 80, "c", "riso basmati"), ("salmone", 110, "p"), ("avocado", 40, "f"), ("cetrioli", 100, "v"),
   ("edamame", 50, "p"), ("salsa_soia", 10, "x"), ("sesamo", 5, "f")],
  ["Cuoci {basmati} in acqua per 12 minuti, scolalo e lascialo intiepidire.",
   "Taglia {salmone} a cubetti (se crudo, usa salmone abbattuto) oppure cuocilo in padella 3 minuti per lato.",
   "Taglia {avocado} e {cetrioli} a fettine; scotta {edamame} 3 minuti in acqua bollente.",
   "Componi la ciotola con riso, salmone e verdure; condisci con {salsa_soia} e {sesamo}."],
  "salmon poke bowl avocado")
r("farro_feta", "Insalata di farro, feta e verdure", "pranzo", 25,
  [("farro", 80, "c"), ("feta", 50, "p"), ("pomodori", 150, "v"), ("cetrioli", 100, "v"), ("olive", 20, "f"),
   ("olio", 8, "f", "olio EVO"), ("ceci", 60, "p", "ceci lessati")],
  ["Cuoci {farro} in acqua salata per 20 minuti, scolalo e raffreddalo sotto l'acqua.",
   "Taglia {pomodori}, {cetrioli} e {feta} a cubetti.",
   "Unisci farro, verdure, feta, {ceci} e {olive}.", "Condisci con {olio} e mescola."],
  "farro salad feta tomatoes cucumber")
r("risotto_zucca", "Risotto alla zucca", "pranzo", 25,
  [("riso", 85, "c", "riso"), ("zucca", 200, "v"), ("cipolla", 30, "x"), ("parmigiano", 15, "p"),
   ("olio", 8, "f", "olio EVO"), ("fiocchi_latte", 80, "p", "fiocchi di latte")],
  ["Taglia {zucca} a cubetti e la cipolla finemente.",
   "Soffriggi la cipolla con {olio}, aggiungi la zucca e cuoci 5 minuti.",
   "Aggiungi {riso}, tostalo 1 minuto e cuoci aggiungendo acqua calda salata un mestolo alla volta per 16-18 minuti.",
   "A fuoco spento manteca con {parmigiano} e {fiocchi_latte}."],
  "pumpkin risotto")
r("quinoa_ceci", "Bowl di quinoa, ceci e verdure", "pranzo", 20,
  [("quinoa", 70, "c"), ("ceci", 120, "p", "ceci lessati"), ("peperoni", 120, "v"), ("zucchine", 100, "v"),
   ("olio", 10, "f", "olio EVO"), ("limone", 10, "x", "succo di limone")],
  ["Sciacqua {quinoa} e cuocila in acqua salata per 12-14 minuti.",
   "Taglia {peperoni} e {zucchine} a cubetti e saltali in padella con metà dell'olio per 8 minuti.",
   "Unisci quinoa, verdure e {ceci}.", "Condisci con l'olio rimasto e {limone}."],
  "quinoa chickpea vegetable bowl")
r("pasta_pesto", "Pasta al pesto, patate e fagiolini", "pranzo", 20,
  [("pasta_int", 80, "c", "pasta semi-integrale"), ("patate", 100, "c"), ("fagiolini", 100, "v"), ("pesto", 25, "f"),
   ("parmigiano", 10, "p")],
  ["Porta a bollore l'acqua salata e cuoci {patate} a cubetti per 5 minuti.",
   "Aggiungi {pasta_int} e {fagiolini} spuntati e cuoci per il tempo indicato sulla confezione.",
   "Scola e condisci con {pesto} e {parmigiano}."],
  "trofie pesto potatoes green beans")
r("couscous_pollo", "Cous cous con pollo e verdure", "pranzo", 20,
  [("couscous", 80, "c"), ("pollo", 120, "p", "petto di pollo"), ("zucchine", 100, "v"), ("peperoni", 100, "v"),
   ("cipolla", 30, "x"), ("olio", 10, "f", "olio EVO")],
  ["Versa {couscous} in una ciotola con la stessa quantità di acqua bollente salata, copri per 5 minuti e sgrana.",
   "Taglia {pollo}, {zucchine} e {peperoni} a cubetti.",
   "In padella scalda {olio} con la cipolla, aggiungi le verdure per 6 minuti e poi il pollo per altri 6.",
   "Unisci il cous cous e mescola."],
  "couscous chicken vegetables")
r("riso_tacchino_broccoli", "Riso integrale, tacchino e broccoli", "pranzo", 30,
  [("riso_int", 80, "c", "riso integrale"), ("tacchino", 130, "p", "petto di tacchino"), ("broccoli", 200, "v"),
   ("olio", 10, "f", "olio EVO"), ("salsa_soia", 10, "x")],
  ["Cuoci {riso_int} in acqua salata per il tempo indicato (25-30 minuti).",
   "Lessa o cuoci al vapore {broccoli} divisi in cimette per 6 minuti.",
   "Taglia {tacchino} a cubetti e cuocilo in padella con {olio} per 6-7 minuti.",
   "Unisci riso, tacchino e broccoli e condisci con {salsa_soia}."],
  "brown rice turkey broccoli bowl")
r("piadina_tacchino", "Piadina con tacchino, mozzarella e rucola", "pranzo", 8,
  [("piadina", 110, "c", "piadina"), ("fesa_tacchino", 60, "p", "fesa di tacchino"), ("mozzarella_light", 60, "p", "mozzarella light"),
   ("rucola", 30, "v"), ("pomodori", 100, "v")],
  ["Scalda {piadina} in padella 1 minuto per lato.",
   "Farcisci con {fesa_tacchino}, {mozzarella_light} a fette, {pomodori} e {rucola}.",
   "Chiudi a metà e scalda ancora 1 minuto perché la mozzarella si ammorbidisca."],
  "piadina wrap turkey mozzarella arugula")
r("spaghetti_pomodoro", "Spaghetti al pomodoro con mozzarella", "pranzo", 15,
  [("pasta", 90, "c", "spaghetti"), ("passata", 150, "v", "passata"), ("mozzarella_light", 80, "p", "mozzarella light"),
   ("olio", 8, "f", "olio EVO"), ("basilico", 3, "x"), ("aglio", 3, "x")],
  ["Scalda {olio} con l'aglio, aggiungi {passata} e cuoci 10 minuti con un pizzico di sale.",
   "Cuoci {pasta} al dente e saltala nel sugo.",
   "Fuori dal fuoco aggiungi {mozzarella_light} a cubetti e il basilico."],
  "spaghetti tomato sauce mozzarella basil")
r("insalata_pollo_patate", "Insalatona di pollo, patate e fagiolini", "pranzo", 25,
  [("pollo", 130, "p", "petto di pollo"), ("patate", 250, "c"), ("fagiolini", 120, "v"), ("lattuga", 60, "v"),
   ("olio", 10, "f", "olio EVO"), ("mais", 40, "c")],
  ["Lessa {patate} a cubetti e {fagiolini} per 12 minuti, poi raffreddali.",
   "Cuoci {pollo} alla piastra 4-5 minuti per lato e taglialo a striscioline.",
   "Unisci {lattuga}, patate, fagiolini, pollo e {mais}.", "Condisci con {olio}, sale e pepe."],
  "chicken potato green bean salad")

# ---------------- CENA ----------------
r("salmone_patate", "Salmone al forno con patate", "cena", 35,
  [("salmone", 140, "p"), ("patate", 220, "c"), ("olio", 8, "f", "olio EVO"), ("lattuga", 80, "v"), ("limone", 10, "x", "succo di limone")],
  ["Taglia {patate} a spicchi, condiscile con metà dell'olio, sale e rosmarino.",
   "Cuoci le patate in forno a 200 °C per 20 minuti.",
   "Aggiungi {salmone} nella teglia e cuoci altri 12-15 minuti.",
   "Servi con {lattuga} condita con l'olio rimasto e {limone}."],
  "baked salmon roasted potatoes")
r("merluzzo_pomodorini", "Merluzzo con pomodorini e olive, pane", "cena", 20,
  [("merluzzo", 200, "p"), ("pomodori", 200, "v", "pomodorini"), ("olive", 20, "f"), ("olio", 8, "f", "olio EVO"),
   ("pane", 80, "c", "pane"), ("prezzemolo", 3, "x")],
  ["In padella scalda {olio}, aggiungi {pomodori} tagliati a metà e {olive}; cuoci 3 minuti.",
   "Adagia {merluzzo} sopra, copri e cuoci 8-10 minuti a fuoco medio.",
   "Completa con il prezzemolo e servi con {pane}."],
  "cod cherry tomatoes olives pan")
r("frittata_spinaci", "Frittata di spinaci con pane integrale", "cena", 20,
  [("uova", 150, "p"), ("spinaci", 200, "v"), ("parmigiano", 10, "p"), ("olio", 5, "f", "olio EVO"),
   ("pane_int", 70, "c", "pane integrale")],
  ["Appassisci {spinaci} in padella per 3 minuti e strizzali.",
   "Sbatti {uova} con {parmigiano}, sale e pepe; unisci gli spinaci.",
   "Scalda {olio} in una padella antiaderente, versa il composto e cuoci coperto a fuoco basso 6-8 minuti, girando a metà.",
   "Servi con {pane_int}."],
  "spinach frittata")
r("riso_uova_spinaci", "Riso, uova e spinaci", "cena", 20,
  [("basmati", 90, "c", "riso basmati"), ("uova", 100, "p"), ("spinaci", 200, "v"), ("olio", 12, "f", "olio EVO")],
  ["Cuoci {basmati} in acqua salata per 12 minuti.",
   "Salta {spinaci} in padella con metà dell'olio per 3 minuti.",
   "Strapazza {uova} nella stessa padella per 2-3 minuti.",
   "Servi riso, uova e spinaci condendo con l'olio rimasto."],
  "rice scrambled eggs spinach plate")
r("tagliata_rucola", "Straccetti di manzo, rucola e grana con patate", "cena", 30,
  [("manzo_magro", 150, "p", "manzo magro"), ("rucola", 50, "v"), ("parmigiano", 10, "f"), ("patate", 200, "c"),
   ("olio", 10, "f", "olio EVO")],
  ["Cuoci {patate} a spicchi in forno a 200 °C per 25 minuti con metà dell'olio.",
   "Scotta {manzo_magro} a straccetti in padella ben calda per 2-3 minuti.",
   "Servi la carne su {rucola}, completa con {parmigiano} a scaglie e l'olio rimasto."],
  "beef strips arugula parmesan")
r("pollo_limone_verdure", "Pollo al limone con verdure grigliate", "cena", 25,
  [("pollo", 150, "p", "petto di pollo"), ("zucchine", 150, "v"), ("melanzane", 150, "v"), ("olio", 10, "f", "olio EVO"),
   ("limone", 20, "x", "succo di limone"), ("pane", 70, "c", "pane")],
  ["Taglia {zucchine} e {melanzane} a fette e grigliale 3 minuti per lato.",
   "Cuoci {pollo} a fette in padella con metà dell'olio per 4 minuti per lato; sfuma con {limone}.",
   "Condisci le verdure con l'olio rimasto.", "Servi con {pane}."],
  "lemon chicken grilled vegetables")
r("tacchino_funghi", "Tacchino ai funghi con riso", "cena", 25,
  [("tacchino", 140, "p", "petto di tacchino"), ("funghi", 200, "v"), ("riso", 70, "c", "riso"), ("olio", 10, "f", "olio EVO"),
   ("prezzemolo", 3, "x"), ("aglio", 3, "x")],
  ["Cuoci {riso} in acqua salata per 15 minuti.",
   "Trifola {funghi} a fette in padella con {olio} e aglio per 8 minuti.",
   "Aggiungi {tacchino} a bocconcini e cuoci 6-7 minuti.", "Completa con prezzemolo e servi con il riso."],
  "turkey mushrooms rice")
r("burger_patate", "Burger di manzo, insalata e patate al forno", "cena", 30,
  [("macinato", 150, "p", "macinato magro"), ("patate", 200, "c"), ("lattuga", 80, "v"), ("pomodori", 100, "v"),
   ("olio", 8, "f", "olio EVO")],
  ["Taglia {patate} a spicchi e cuocile in forno a 200 °C per 25 minuti con metà dell'olio.",
   "Forma un burger con {macinato}, sale e pepe.",
   "Cuocilo su piastra ben calda 4-5 minuti per lato.",
   "Servi con {lattuga} e {pomodori} conditi con l'olio rimasto."],
  "beef burger patty salad roasted potatoes")
r("lenticchie_umido", "Lenticchie in umido con pane", "cena", 25,
  [("lenticchie", 250, "p", "lenticchie lessate"), ("passata", 100, "v", "passata"), ("carote", 50, "v"), ("cipolla", 30, "x"),
   ("olio", 10, "f", "olio EVO"), ("pane", 70, "c", "pane")],
  ["Soffriggi cipolla e {carote} tritate con {olio} per 4 minuti.",
   "Aggiungi {lenticchie} e {passata}, copri e cuoci 15 minuti a fuoco basso.",
   "Regola di sale e servi con {pane} tostato."],
  "lentil stew bread")
r("tofu_verdure", "Tofu saltato con verdure e riso", "cena", 20,
  [("tofu", 150, "p"), ("peperoni", 100, "v"), ("broccoli", 150, "v"), ("riso", 70, "c", "riso"),
   ("salsa_soia", 10, "x"), ("olio", 10, "f", "olio EVO")],
  ["Cuoci {riso} in acqua per 14 minuti.",
   "Taglia {tofu} a cubetti e rosolalo in padella con {olio} per 5 minuti.",
   "Aggiungi {peperoni} e {broccoli} a pezzetti e salta 6-7 minuti.",
   "Sfuma con {salsa_soia} e servi con il riso."],
  "tofu stir fry vegetables rice")
r("branzino_cartoccio", "Branzino al cartoccio con patate e fagiolini", "cena", 35,
  [("branzino", 200, "p", "filetti di branzino"), ("patate", 200, "c"), ("fagiolini", 150, "v"), ("olio", 10, "f", "olio EVO"),
   ("limone", 10, "x", "succo di limone")],
  ["Lessa {patate} a cubetti e {fagiolini} per 12 minuti.",
   "Metti {branzino} su carta forno con limone, sale e un filo d'olio; chiudi il cartoccio.",
   "Cuoci in forno a 200 °C per 15 minuti.", "Servi con patate e fagiolini conditi con l'olio rimasto."],
  "sea bass fillet baked parchment")
r("gamberi_couscous", "Gamberi, zucchine e cous cous", "cena", 20,
  [("gamberi", 150, "p"), ("zucchine", 200, "v"), ("couscous", 70, "c"), ("olio", 10, "f", "olio EVO"),
   ("aglio", 3, "x"), ("prezzemolo", 3, "x")],
  ["Prepara {couscous} con la stessa quantità di acqua bollente salata, copri 5 minuti e sgrana.",
   "Salta {zucchine} a cubetti con {olio} e aglio per 6 minuti.",
   "Aggiungi {gamberi} sgusciati e cuoci 2-3 minuti.", "Unisci il cous cous e il prezzemolo."],
  "shrimp zucchini couscous")
r("omelette_cotto", "Omelette prosciutto e mozzarella con insalata", "cena", 15,
  [("uova", 100, "x"), ("albume", 100, "p"), ("prosciutto_cotto", 40, "p", "prosciutto cotto"),
   ("mozzarella_light", 40, "f", "mozzarella light"), ("pane", 60, "c", "pane"), ("lattuga", 80, "v")],
  ["Sbatti {uova} e {albume} con un pizzico di sale.",
   "Versa in padella antiaderente calda e cuoci 2-3 minuti.",
   "Farcisci metà con {prosciutto_cotto} e {mozzarella_light}, richiudi e cuoci un altro minuto.",
   "Servi con {lattuga} e {pane}."],
  "omelette ham cheese salad")
r("trota_patate_dolci", "Trota al forno con patate dolci e spinaci", "cena", 35,
  [("trota", 180, "p", "filetti di trota"), ("patate_dolci", 200, "c", "patate dolci"), ("spinaci", 150, "v"),
   ("olio", 8, "f", "olio EVO")],
  ["Cuoci {patate_dolci} a cubetti in forno a 200 °C per 25 minuti con metà dell'olio.",
   "Negli ultimi 12 minuti aggiungi {trota} nella teglia.",
   "Salta {spinaci} in padella con l'olio rimasto per 3 minuti e servi."],
  "baked trout sweet potato")
r("pasta_salmone_zucchine", "Pasta salmone e zucchine", "cena", 20,
  [("pasta", 80, "c"), ("salmone", 100, "p"), ("zucchine", 150, "v"), ("olio", 5, "f", "olio EVO"), ("aglio", 3, "x")],
  ["Cuoci {pasta} in acqua salata.",
   "Taglia {salmone} e {zucchine} a cubetti.",
   "Salta le zucchine con {olio} e aglio per 5 minuti, aggiungi il salmone per 3 minuti.",
   "Unisci la pasta scolata al dente e mescola."],
  "pasta salmon zucchini")

# ---------------- SPUNTINO ----------------
r("pane_ricotta_marmellata", "Pane, ricotta e marmellata", "spuntino", 3,
  [("pane", 50, "c", "pane"), ("ricotta_light", 80, "p", "ricotta light"), ("marmellata", 15, "c"), ("fragole", 80, "v")],
  ["Tosta {pane}.", "Spalma {ricotta_light} e completa con {marmellata}.", "Accompagna con {fragole}."],
  "ricotta jam toast strawberries")
r("greco_mela", "Yogurt greco e mela", "spuntino", 2,
  [("greco0", 170, "p", "yogurt greco 0%"), ("mela", 150, "c"), ("noci", 5, "f")],
  ["Taglia {mela} a cubetti.", "Unisci a {greco0} e completa con {noci}."],
  "greek yogurt apple")
r("mandorle_mandarini", "Mandorle e mandarini", "spuntino", 1,
  [("mandorle", 20, "f"), ("mandarini", 160, "c")],
  ["Sbuccia {mandarini} e accompagnali con {mandorle}."],
  "almonds tangerines snack")
r("gallette_hummus", "Gallette con hummus e carote", "spuntino", 5,
  [("gallette", 24, "c", "gallette di riso"), ("hummus", 40, "f"), ("carote", 100, "v"), ("lupini", 50, "p")],
  ["Taglia {carote} a bastoncini.", "Spalma {hummus} sulle {gallette}.", "Accompagna con carote e {lupini}."],
  "rice cakes hummus carrot sticks")
r("fiocchi_crackers", "Fiocchi di latte, crackers e pomodorini", "spuntino", 3,
  [("fiocchi_latte", 150, "p"), ("crackers", 25, "c", "crackers integrali"), ("pomodori", 100, "v", "pomodorini")],
  ["Condisci {fiocchi_latte} con un pizzico di pepe.", "Servi con {crackers} e {pomodori}."],
  "cottage cheese crackers tomatoes")
r("toast_cotto", "Toast prosciutto cotto e provola", "spuntino", 6,
  [("pane_int", 60, "c", "pane integrale"), ("prosciutto_cotto", 30, "p", "prosciutto cotto"), ("provola", 20, "f")],
  ["Farcisci {pane_int} con {prosciutto_cotto} e {provola}.", "Tosta in piastra 3-4 minuti, finché il formaggio fila."],
  "ham cheese toasted sandwich")
r("banana_arachidi", "Banana e burro di arachidi", "spuntino", 2,
  [("banana", 120, "c"), ("arachidi", 15, "f"), ("greco0", 100, "p", "yogurt greco 0%")],
  ["Taglia {banana} a rondelle.", "Servi con {arachidi} e {greco0}."],
  "banana peanut butter")
r("smoothie_proteico", "Smoothie proteico banana e avena", "spuntino", 3,
  [("latte_ps", 250, "c", "latte"), ("whey", 25, "p", "proteine whey"), ("banana", 100, "c"), ("avena", 20, "c")],
  ["Frulla {latte_ps}, {whey}, {banana} e {avena} per 30 secondi.", "Bevi subito."],
  "banana protein smoothie glass")
r("kefir_bosco", "Kefir, frutti di bosco e chia", "spuntino", 2,
  [("kefir", 200, "p"), ("frutti_bosco_surg", 100, "c", "frutti di bosco"), ("chia", 10, "f")],
  ["Versa {kefir} in una ciotola.", "Aggiungi {frutti_bosco_surg} e {chia}."],
  "kefir berries chia")
r("fondente_kiwi", "Cioccolato fondente, noci e kiwi", "spuntino", 2,
  [("fondente", 15, "f", "cioccolato fondente"), ("noci", 10, "f"), ("kiwi", 150, "c"), ("greco0", 100, "p", "yogurt greco 0%")],
  ["Sbuccia e taglia {kiwi}.", "Servi con {greco0}, {fondente} e {noci}."],
  "dark chocolate walnuts kiwi")
