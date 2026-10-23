# Famiglie: pasta, risotti/orzotti/farrotti, bowl di cereali, insalate.
from gen_core import *

# ======================= PASTA =======================
# (nome dopo la forma di pasta, forma lunga/corta, ingredienti, passaggi del condimento, minuti, difficolta, consiglio)
PASTA = [
 ("al pomodoro e basilico", "long", [("passata", 160, "v"), ("olio", 8, "f", "olio EVO"), ("aglio", 3, "x"), ("basilico", 4, "x"), ("parmigiano", 10, "f")],
  ["In una padella scalda {olio} con {aglio} per 1 minuto, poi versa {passata} e cuoci 10 minuti a fuoco medio con un pizzico di sale.", "Spegni e aggiungi {basilico} spezzettato."], 20, 1, "Per un sugo più dolce aggiungi una carota grattugiata."),
 ("alla norma", "short", [("melanzane", 150, "v"), ("passata", 120, "v"), ("olio", 8, "f"), ("ricotta_light", 40, "p", "ricotta light"), ("aglio", 3, "x"), ("basilico", 4, "x")],
  ["Taglia {melanzane} a cubetti e rosolale in padella con {olio} per 10 minuti, finché sono dorate.", "Aggiungi {aglio}, {passata} e cuoci 10 minuti.", "Servi con {ricotta_light} sbriciolata e {basilico}."], 30, 2, ""),
 ("zucchine e parmigiano", "short", [("zucchine", 200, "v"), ("olio", 8, "f"), ("aglio", 3, "x"), ("parmigiano", 15, "f"), ("basilico", 3, "x")],
  ["Taglia {zucchine} a rondelle e saltale in padella con {olio} e {aglio} per 8-10 minuti, schiacciandone alcune per fare una crema.", "Fuori dal fuoco unisci {parmigiano} e {basilico}."], 20, 1, ""),
 ("zucchine e gamberi", "long", [("zucchine", 150, "v"), ("gamberi", 120, "p"), ("olio", 8, "f"), ("aglio", 3, "x"), ("prezzemolo", 4, "x")],
  ["Taglia {zucchine} a rondelle e saltale in padella con {olio} e {aglio} per 6 minuti.", "Aggiungi {gamberi} sgusciati e cuoci 3-4 minuti.", "Completa con {prezzemolo} tritato."], 25, 1, ""),
 ("alle vongole", "long", [("vongole", 200, "p"), ("olio", 10, "f"), ("aglio", 3, "x"), ("prezzemolo", 5, "x"), ("vino_bianco", 30, "x"), ("peperoncino", 1, "x")],
  ["Lascia {vongole} in acqua salata 30 minuti per eliminare la sabbia, poi sciacquale.", "In una padella grande scalda {olio} con {aglio} e {peperoncino}, aggiungi le vongole e {vino_bianco}, copri e cuoci finché si aprono (4-5 minuti); elimina quelle rimaste chiuse.", "Aggiungi {prezzemolo} tritato."], 35, 2, "Non salare il sugo: le vongole sono già sapide."),
 ("alle cozze e pomodorini", "long", [("cozze", 200, "p"), ("pomodori", 120, "v", "pomodorini"), ("olio", 8, "f"), ("aglio", 3, "x"), ("prezzemolo", 4, "x")],
  ["Pulisci {cozze} sotto l'acqua corrente eliminando il bisso; scartane le aperte.", "In una padella scalda {olio} con {aglio}, aggiungi {pomodori} a metà e cuoci 4 minuti.", "Unisci le cozze, copri e cuoci finché si aprono (4-5 minuti), poi aggiungi {prezzemolo}."], 30, 2, ""),
 ("al polpo e pomodorini", "short", [("polpo", 130, "p"), ("pomodori", 130, "v", "pomodorini"), ("olio", 8, "f"), ("aglio", 3, "x"), ("prezzemolo", 4, "x"), ("olive", 15, "f", "olive nere")],
  ["Lessa {polpo} (peso a crudo) in acqua bollente per 40-45 minuti, finché una forchetta entra facilmente; lascialo intiepidire e taglialo a pezzetti.", "In padella scalda {olio} con {aglio}, aggiungi {pomodori} a metà e cuoci 6 minuti.", "Unisci il polpo, {olive} e {prezzemolo}."], 65, 2, "Il polpo si può lessare il giorno prima."),
 ("al tonno e pomodorini", "short", [("tonno_nat", 80, "p"), ("pomodori", 150, "v", "pomodorini"), ("olio", 8, "f"), ("capperi", 5, "x"), ("aglio", 3, "x"), ("origano", 1, "x")],
  ["Scalda {olio} con {aglio} in padella, aggiungi {pomodori} a metà e {capperi} e cuoci 8 minuti.", "Spegni e unisci {tonno_nat} sgocciolato e {origano}."], 20, 1, ""),
 ("alla puttanesca", "long", [("passata", 130, "v"), ("olive", 20, "f", "olive nere"), ("capperi", 6, "x"), ("acciughe", 10, "p"), ("olio", 5, "f"), ("aglio", 3, "x"), ("prezzemolo", 3, "x"), ("peperoncino", 1, "x")],
  ["In padella sciogli {acciughe} con {olio}, {aglio} e {peperoncino} a fuoco basso.", "Aggiungi {passata}, {olive} e {capperi} e cuoci 10 minuti.", "Completa con {prezzemolo} tritato."], 20, 1, ""),
 ("broccoli e acciughe", "short", [("broccoli", 200, "v"), ("acciughe", 15, "p"), ("olio", 8, "f"), ("aglio", 3, "x"), ("peperoncino", 1, "x"), ("pangrattato", 10, "c")],
  ["Cuoci {broccoli} a cimette nell'acqua della pasta per gli ultimi 6 minuti.", "In padella sciogli {acciughe} con {olio}, {aglio} e {peperoncino}.", "Tosta {pangrattato} in una padellina asciutta per 2 minuti.", "Salta pasta e broccoli nel condimento, schiacciando un po' i broccoli, e completa con il pangrattato."], 25, 1, ""),
 ("cime di rapa e salsiccia", "short", [("cime_rapa", 200, "v"), ("salsiccia", 70, "p"), ("olio", 5, "f"), ("aglio", 3, "x"), ("peperoncino", 1, "x")],
  ["Pulisci {cime_rapa} e cuocile nell'acqua della pasta per 5 minuti prima di buttare la pasta.", "Sgrana {salsiccia} in padella con {olio}, {aglio} e {peperoncino} e rosolala 8 minuti.", "Salta pasta e cime nel condimento per 1-2 minuti."], 30, 2, ""),
 ("ai funghi e salsiccia", "short", [("funghi", 150, "v"), ("salsiccia", 70, "p"), ("scalogno", 25, "v"), ("olio", 5, "f"), ("prezzemolo", 4, "x"), ("vino_bianco", 30, "x")],
  ["Sgrana {salsiccia} in padella con {olio} e {scalogno} tritato per 6 minuti.", "Aggiungi {funghi} a fettine e {vino_bianco} e cuoci 8 minuti.", "Completa con {prezzemolo}."], 25, 1, ""),
 ("ai funghi e prezzemolo", "short", [("funghi", 200, "v"), ("olio", 8, "f"), ("aglio", 3, "x"), ("prezzemolo", 5, "x"), ("parmigiano", 10, "f")],
  ["Affetta {funghi} e cuocili in padella a fuoco vivo con {olio} e {aglio} per 8-10 minuti.", "Unisci {prezzemolo} tritato e {parmigiano}."], 20, 1, ""),
 ("al ragù di manzo", "short", [("macinato", 90, "p", "macinato di manzo magro"), ("passata", 120, "v"), ("carote", 40, "v"), ("sedano", 30, "v"), ("cipolla", 30, "v"), ("olio", 6, "f"), ("vino_rosso", 30, "x"), ("parmigiano", 8, "f")],
  ["Tritaf finemente {cipolla}, {carote} e {sedano} e soffriggili in {olio} per 5 minuti.", "Aggiungi {macinato} e rosolalo sgranandolo; sfuma con {vino_rosso}.", "Versa {passata}, copri e cuoci a fuoco dolce per 30 minuti.", "Servi con {parmigiano}."], 50, 2, "Il ragù migliora il giorno dopo: puoi preparare una dose doppia e congelarla."),
 ("al ragù di tacchino", "short", [("tacchino_macinato", 100, "p"), ("passata", 120, "v"), ("carote", 40, "v"), ("sedano", 30, "v"), ("cipolla", 30, "v"), ("olio", 6, "f"), ("rosmarino", 2, "x"), ("parmigiano", 8, "f")],
  ["Tritaf finemente {cipolla}, {carote} e {sedano} e soffriggili in {olio} con {rosmarino} per 5 minuti.", "Aggiungi {tacchino_macinato} e rosolalo sgranandolo per 6 minuti.", "Versa {passata}, copri e cuoci per 25 minuti.", "Servi con {parmigiano}."], 45, 2, ""),
 ("al ragù bianco di vitello", "short", [("vitello_macinato", 100, "p"), ("carote", 40, "v"), ("sedano", 30, "v"), ("cipolla", 30, "v"), ("olio", 6, "f"), ("vino_bianco", 40, "x"), ("salvia", 2, "x"), ("parmigiano", 8, "f")],
  ["Tritaf finemente {cipolla}, {carote} e {sedano} e soffriggili in {olio} con {salvia} per 5 minuti.", "Aggiungi {vitello_macinato}, rosolalo sgranandolo e sfuma con {vino_bianco}.", "Cuoci a fuoco dolce 25 minuti con un mestolo di acqua calda.", "Servi con {parmigiano}."], 45, 2, ""),
 ("al ragù di agnello", "short", [("agnello_macinato", 90, "p"), ("passata", 100, "v"), ("cipolla", 30, "v"), ("carote", 30, "v"), ("olio", 6, "f"), ("rosmarino", 2, "x"), ("pecorino", 10, "f")],
  ["Soffriggi {cipolla} e {carote} tritate in {olio} con {rosmarino} per 5 minuti.", "Aggiungi {agnello_macinato} e rosolalo 8 minuti.", "Versa {passata} e cuoci 25 minuti a fuoco dolce.", "Servi con {pecorino} grattugiato."], 45, 2, ""),
 ("al ragù di lenticchie", "short", [("lenticchie", 120, "p", "lenticchie lessate"), ("passata", 100, "v"), ("carote", 35, "v"), ("sedano", 25, "v"), ("cipolla", 30, "v"), ("olio", 6, "f"), ("rosmarino", 2, "x"), ("parmigiano", 8, "f")],
  ["Tritaf {cipolla}, {carote} e {sedano} e soffriggili in {olio} con {rosmarino} per 5 minuti.", "Aggiungi {lenticchie} scolate e {passata} e cuoci 15 minuti, schiacciando metà delle lenticchie.", "Servi con {parmigiano}."], 35, 1, ""),
 ("cacio e pepe", "long", [("pecorino", 30, "f"), ("pepe", 2, "x"), ("olio", 4, "f")],
  ["Tosta {pepe} in padella asciutta per 1 minuto, aggiungi un mestolo di acqua di cottura.", "Grattugia {pecorino} e crea una crema in una ciotola con poca acqua di cottura tiepida.", "Manteca la pasta nella padella con il pepe, poi fuori dal fuoco unisci la crema di pecorino e {olio}."], 15, 2, "La crema deve essere tiepida: se è troppo calda il formaggio fa i grumi."),
 ("al pesto e fagiolini", "short", [("pesto", 30, "f"), ("fagiolini", 100, "v"), ("patate", 80, "c")],
  ["Pela {patate} e tagliale a cubetti, spunta {fagiolini}: cuocili insieme alla pasta negli ultimi 8 minuti.", "Stempera {pesto} con un mestolo di acqua di cottura in una ciotola.", "Condisci la pasta con le verdure e il pesto, fuori dal fuoco."], 25, 1, "Il pesto non va cotto: aggiungilo sempre a fiamma spenta."),
 ("ricotta e spinaci", "short", [("ricotta", 80, "p"), ("spinaci", 150, "v"), ("olio", 5, "f"), ("parmigiano", 10, "f"), ("noce_moscata", 0.5, "x"), ("aglio", 3, "x")],
  ["Salta {spinaci} in padella con {olio} e {aglio} per 4 minuti.", "Mescola {ricotta} con {noce_moscata} e un mestolino di acqua di cottura.", "Condisci la pasta con ricotta e spinaci e completa con {parmigiano}."], 20, 1, ""),
 ("limone e gamberi", "long", [("gamberi", 120, "p"), ("limone", 20, "x"), ("prezzemolo", 4, "x"), ("olio", 8, "f"), ("aglio", 3, "x")],
  ["Sguscia {gamberi} e saltali in padella con {olio} e {aglio} per 3 minuti.", "Spremi {limone}, grattugia un po' di scorza e aggiungila al sugo con {prezzemolo}.", "Manteca la pasta nel sugo per 1 minuto."], 20, 1, ""),
 ("al salmone affumicato e ricotta", "long", [("salmone_aff", 60, "p"), ("ricotta", 60, "p"), ("erba_cipollina", 3, "x"), ("limone", 15, "x"), ("pepe", 0.5, "x")],
  ["Taglia {salmone_aff} a striscioline.", "Mescola {ricotta} con {limone}, {erba_cipollina} tritata, pepe e un mestolino di acqua di cottura.", "Condisci la pasta con la crema e completa con il salmone."], 15, 1, ""),
 ("agli asparagi e prosciutto crudo", "short", [("asparagi", 150, "v"), ("prosciutto_crudo", 40, "p"), ("olio", 6, "f"), ("parmigiano", 10, "f"), ("pepe", 0.5, "x")],
  ["Taglia {asparagi} a rondelle (tieni le punte intere) e cuocili in padella con {olio} per 8 minuti.", "Taglia {prosciutto_crudo} a listarelle e uniscilo a fine cottura.", "Manteca la pasta con gli asparagi e completa con {parmigiano} e pepe."], 25, 1, ""),
 ("alla zucca e salsiccia", "short", [("zucca", 200, "v"), ("salsiccia", 70, "p"), ("cipolla", 30, "v"), ("olio", 5, "f"), ("rosmarino", 2, "x")],
  ["Soffriggi {cipolla} in {olio} con {rosmarino}, aggiungi {zucca} a cubetti e un mestolo di acqua, cuoci 15 minuti finché si sfalda.", "Sgrana {salsiccia} in un'altra padella e rosolala 8 minuti, poi uniscila alla zucca."], 35, 2, ""),
 ("al gorgonzola e noci", "short", [("gorgonzola", 40, "f"), ("noci", 15, "f"), ("latte_scremato", 40, "x", "latte scremato"), ("pepe", 0.5, "x")],
  ["Sciogli {gorgonzola} con {latte_scremato} a fuoco dolcissimo in un pentolino finché diventa una crema.", "Trita grossolanamente {noci}.", "Condisci la pasta con la crema, noci e una macinata di pepe."], 20, 1, ""),
 ("al radicchio e gorgonzola", "short", [("radicchio", 120, "v"), ("gorgonzola", 40, "f"), ("noci", 10, "f"), ("scalogno", 20, "v"), ("olio", 4, "f")],
  ["Soffriggi {scalogno} in {olio}, aggiungi {radicchio} a listarelle e cuoci 6 minuti.", "Fuori dal fuoco unisci {gorgonzola} a pezzetti, mescolando finché si scioglie.", "Condisci la pasta e completa con {noci} tritate."], 25, 1, ""),
 ("pomodori secchi e rucola", "short", [("pomodori_secchi", 25, "v"), ("rucola", 40, "v"), ("pinoli", 10, "f"), ("olio", 6, "f"), ("aglio", 3, "x"), ("parmigiano", 10, "f")],
  ["Tosta {pinoli} 2 minuti in padella asciutta.", "Trita {pomodori_secchi} e scaldali con {olio} e {aglio} per 2 minuti.", "Condisci la pasta, aggiungi {rucola}, {pinoli} e {parmigiano}."], 20, 1, ""),
 ("ai carciofi e pecorino", "short", [("carciofi", 200, "v"), ("pecorino", 20, "f"), ("olio", 6, "f"), ("aglio", 3, "x"), ("prezzemolo", 4, "x"), ("limone", 10, "x")],
  ["Pulisci {carciofi}, tagliali a spicchi e tienili in acqua e {limone}.", "Cuocili in padella con {olio} e {aglio} per 15 minuti, aggiungendo acqua se serve.", "Manteca la pasta, aggiungi {prezzemolo} e {pecorino}."], 35, 2, ""),
 ("ai peperoni e ricotta", "short", [("peperoni", 200, "v"), ("ricotta", 60, "p"), ("olio", 6, "f"), ("basilico", 3, "x"), ("aglio", 3, "x")],
  ["Taglia {peperoni} a dadini e cuocili in padella con {olio} e {aglio} per 15 minuti con un goccio d'acqua.", "Frulla metà dei peperoni con {ricotta} per una crema.", "Condisci la pasta con la crema e i peperoni rimasti, completa con {basilico}."], 30, 1, ""),
 ("alla bresaola e rucola", "short", [("bresaola", 50, "p"), ("rucola", 40, "v"), ("pomodori", 100, "v", "pomodorini"), ("parmigiano", 15, "f"), ("olio", 6, "f"), ("limone", 10, "x")],
  ["Taglia {pomodori} a metà e scaldali in padella con {olio} per 3 minuti.", "Condisci la pasta con i pomodorini, aggiungi {bresaola} a striscioline, {rucola}, {parmigiano} e {limone}."], 20, 1, ""),
 ("ai calamari e pomodorini", "long", [("calamari", 150, "p"), ("pomodori", 120, "v", "pomodorini"), ("olio", 8, "f"), ("aglio", 3, "x"), ("prezzemolo", 4, "x"), ("peperoncino", 1, "x")],
  ["In padella scalda {olio} con {aglio} e {peperoncino}, aggiungi {pomodori} a metà e cuoci 5 minuti.", "Taglia {calamari} ad anelli, uniscili e cuoci 4 minuti.", "Completa con {prezzemolo}."], 25, 1, ""),
 ("al pesce spada e melanzane", "short", [("pesce_spada", 100, "p"), ("melanzane", 120, "v"), ("pomodori", 100, "v", "pomodorini"), ("olio", 8, "f"), ("basilico", 3, "x"), ("capperi", 5, "x"), ("aglio", 3, "x")],
  ["Cuoci {melanzane} a cubetti in padella con {olio} per 8 minuti.", "Aggiungi {aglio}, {pomodori}, {capperi} e cuoci 5 minuti.", "Unisci {pesce_spada} a cubetti e cuoci 3-4 minuti. Completa con {basilico}."], 30, 2, ""),
 ("alle verdure grigliate", "short", [("zucchine", 100, "v"), ("peperoni", 100, "v"), ("melanzane", 80, "v"), ("pomodori", 80, "v", "pomodorini"), ("olio", 8, "f"), ("basilico", 4, "x")],
  ["Taglia {zucchine}, {peperoni} e {melanzane} a pezzi e grigliali o saltali in padella con {olio} per 12 minuti.", "Aggiungi {pomodori} a metà negli ultimi 3 minuti.", "Condisci la pasta e completa con {basilico}."], 30, 1, ""),
 ("al pollo e peperoni", "short", [("pollo", 100, "p"), ("peperoni", 150, "v"), ("passata", 60, "v"), ("olio", 6, "f"), ("cipolla", 30, "v"), ("basilico", 3, "x")],
  ["Taglia {pollo} a straccetti e rosolalo in padella con {olio} per 6 minuti, poi mettilo da parte.", "Nella stessa padella cuoci {cipolla} e {peperoni} a pezzi per 10 minuti, aggiungi {passata} e il pollo e cuoci altri 5 minuti.", "Completa con {basilico}."], 35, 1, ""),
 ("al pollo e funghi", "short", [("pollo", 100, "p"), ("funghi", 120, "v"), ("olio", 6, "f"), ("prezzemolo", 4, "x"), ("vino_bianco", 30, "x")],
  ["Taglia {pollo} a straccetti e rosolalo in padella con {olio} per 6 minuti.", "Aggiungi {funghi} a fettine e {vino_bianco} e cuoci 8 minuti.", "Completa con {prezzemolo}."], 30, 1, ""),
 ("alla carbonara di zucchine", "long", [("zucchine", 150, "v"), ("uova", 50, "p"), ("parmigiano", 20, "f"), ("pepe", 1, "x"), ("olio", 5, "f")],
  ["Taglia {zucchine} a rondelle e dorale in padella con {olio} per 8 minuti.", "Sbatti {uova} con {parmigiano} e il pepe.", "Scola la pasta e versala nella padella fuori dal fuoco, aggiungi la crema d'uovo e mescola in fretta con un po' di acqua di cottura."], 25, 2, "Fuori dal fuoco: l'uovo deve diventare una crema, non una frittata."),
 ("al cavolfiore e acciughe", "short", [("cavolfiore", 200, "v"), ("acciughe", 10, "p"), ("olio", 8, "f"), ("aglio", 3, "x"), ("peperoncino", 1, "x"), ("pangrattato", 10, "c")],
  ["Lessa {cavolfiore} a cimette nell'acqua della pasta per 8 minuti e raccoglilo.", "Sciogli {acciughe} in {olio} con {aglio} e {peperoncino}, aggiungi il cavolfiore e schiaccialo con la forchetta.", "Tosta {pangrattato} in padella asciutta, manteca la pasta nel sugo e completa con il pangrattato."], 30, 1, ""),
 ("alla barbabietola e caprino", "short", [("barbabietole", 150, "v"), ("caprino", 60, "p"), ("noci", 10, "f"), ("erba_cipollina", 3, "x"), ("olio", 4, "f")],
  ["Frulla {barbabietole} cotte con {olio} e un cucchiaio di acqua di cottura fino a una crema rosa.", "Condisci la pasta con la crema, completa con {caprino} a pezzetti, {noci} e {erba_cipollina}."], 20, 1, "Usa barbabietole già cotte al vapore (non sott'aceto)."),
 ("al cavolo nero e fagioli", "short", [("cavolo_nero", 100, "v"), ("fagioli", 100, "p", "fagioli cannellini lessati"), ("olio", 6, "f"), ("aglio", 3, "x"), ("parmigiano", 10, "f")],
  ["Cuoci {cavolo_nero} a pezzi in padella con {olio} e {aglio} per 10 minuti con un goccio d'acqua.", "Aggiungi {fagioli} scolati e schiaccia metà, cuoci 5 minuti.", "Manteca la pasta con il condimento e completa con {parmigiano}."], 30, 1, ""),
 ("ai porri e salmone", "short", [("porri", 100, "v"), ("salmone", 100, "p"), ("olio", 5, "f"), ("erba_cipollina", 3, "x"), ("pepe", 0.5, "x")],
  ["Cuoci {porri} ad anelli in padella con {olio} per 8 minuti a fuoco dolce.", "Aggiungi {salmone} a cubetti e cuoci 4-5 minuti.", "Manteca la pasta e completa con {erba_cipollina} e pepe."], 25, 1, ""),
 ("alla ricotta e limone", "short", [("ricotta", 90, "p"), ("limone", 15, "x"), ("basilico", 3, "x"), ("pepe", 0.5, "x"), ("parmigiano", 10, "f")],
  ["Mescola {ricotta} con la scorza e il succo di {limone}, {basilico} spezzettato e pepe, aggiungendo un mestolino di acqua di cottura.", "Condisci la pasta fuori dal fuoco e completa con {parmigiano}."], 15, 1, ""),
]
PASTA = [(n, s, [(i if len(i) < 2 else i) for i in ing], [x.replace("Tritaf ", "Trita ") for x in st], m, d, t) for (n, s, ing, st, m, d, t) in PASTA]
_LONG = {"pasta": ["Spaghetti", "Linguine", "Spaghetti"], "pasta_int": ["Spaghetti integrali"], "pasta_fresca": ["Tagliatelle fresche"]}
_SHORT = {"pasta": ["Penne", "Fusilli", "Rigatoni", "Mezze maniche", "Caserecce"], "pasta_int": ["Penne integrali", "Fusilli integrali"], "pasta_fresca": ["Garganelli freschi"]}

def gen_pasta():
    for idx, (rest, shape, ings, steps, mins, diff, tip) in enumerate(PASTA):
        for bi, base in enumerate(["pasta", "pasta_int", "pasta_fresca"]):
            forms = (_LONG if shape == "long" else _SHORT)[base]
            form = forms[idx % len(forms)]
            name = f"{form} {rest}"
            gr = CARB_G[base]
            ing_list = [(base, gr, "c")] + list(ings)
            POST = ("Scola", "Condisci la pasta", "Manteca la pasta", "Salta pasta", "Servi con", "Condisci la ")
            pre = [x for x in steps if not x.startswith(POST)]
            post = [x for x in steps if x.startswith(POST)]
            generic = not any(x.startswith(("Scola", "Condisci la", "Manteca la", "Salta pasta")) for x in post)
            st = ["Porta a bollore abbondante acqua salata e prepara il condimento."] + pre + [cook_carb(base)]
            if generic: st.append("Scola e manteca la pasta nel condimento per un minuto a fuoco vivace, aggiungendo un po' di acqua di cottura se serve.")
            st += post
            st = [x.replace("{pasta}", "{" + base + "}") for x in st]
            mt = ["padella"]
            m = mins + (5 if base == "pasta_int" else (-8 if base == "pasta_fresca" else 0))
            add(name, "pranzo" if (idx + bi) % 3 else "cena", max(m, 12), ing_list, st, tip, "italiana", mt, diff, fam="pasta")

# ======================= RISOTTI, ORZOTTI, FARROTTI =======================
# (nome ingredienti, ingredienti, passaggi di aggiunta, minuti, difficolta)
RISO = [
 ("alla zucca", [("zucca", 200, "v"), ("cipolla", 30, "v"), ("parmigiano", 15, "f"), ("olio", 6, "f"), ("rosmarino", 2, "x")], ["Taglia {zucca} a cubetti e cuocila nel brodo caldo insieme al soffritto di {cipolla} e {olio} con {rosmarino}."]),
 ("ai funghi", [("funghi", 200, "v"), ("scalogno", 25, "v"), ("parmigiano", 15, "f"), ("olio", 6, "f"), ("prezzemolo", 4, "x")], ["Cuoci {funghi} a fettine in padella con {olio} e {scalogno} per 8 minuti e uniscili a metà cottura."]),
 ("agli asparagi", [("asparagi", 200, "v"), ("cipolla", 25, "v"), ("parmigiano", 15, "f"), ("olio", 6, "f"), ("limone", 10, "x")], ["Taglia {asparagi} a rondelle (punte intere) e aggiungili a metà cottura, le punte negli ultimi 5 minuti."]),
 ("zucchine e gamberi", [("zucchine", 150, "v"), ("gamberi", 120, "p"), ("cipolla", 25, "v"), ("olio", 8, "f"), ("prezzemolo", 4, "x")], ["Rosola {gamberi} sgusciati 2 minuti in padella con {olio}, tienili da parte e aggiungili a fine cottura insieme a {zucchine} a dadini."]),
 ("al radicchio e gorgonzola", [("radicchio", 120, "v"), ("gorgonzola", 40, "f"), ("scalogno", 25, "v"), ("olio", 5, "f"), ("noci", 10, "f")], ["Aggiungi {radicchio} a listarelle a metà cottura e manteca con {gorgonzola} a fuoco spento."]),
 ("ai piselli e prosciutto cotto", [("piselli", 120, "v"), ("prosciutto_cotto", 50, "p"), ("cipolla", 25, "v"), ("olio", 6, "f"), ("parmigiano", 15, "f")], ["Unisci {piselli} a metà cottura e {prosciutto_cotto} a dadini a fine cottura."]),
 ("al limone e parmigiano", [("limone", 25, "x"), ("parmigiano", 25, "f"), ("cipolla", 25, "v"), ("olio", 6, "f"), ("pepe", 0.5, "x")], ["Aggiungi scorza e succo di {limone} a fine cottura e manteca con {parmigiano}."]),
 ("al pomodoro e basilico", [("passata", 100, "v"), ("basilico", 4, "x"), ("cipolla", 25, "v"), ("olio", 6, "f"), ("parmigiano", 15, "f")], ["Versa {passata} dopo la tostatura e cuoci aggiungendo brodo; completa con {basilico}."]),
 ("alla salsiccia e radicchio", [("salsiccia", 80, "p"), ("radicchio", 100, "v"), ("cipolla", 25, "v"), ("olio", 4, "f"), ("vino_rosso", 30, "x")], ["Sgrana {salsiccia} in padella con {olio} e {cipolla}, sfuma con {vino_rosso} e aggiungi {radicchio} a listarelle."]),
 ("ai carciofi", [("carciofi", 200, "v"), ("cipolla", 25, "v"), ("parmigiano", 15, "f"), ("olio", 6, "f"), ("prezzemolo", 4, "x")], ["Cuoci {carciofi} a spicchi in padella con {olio} per 10 minuti e uniscili a metà cottura."]),
 ("ai peperoni", [("peperoni", 200, "v"), ("cipolla", 25, "v"), ("parmigiano", 15, "f"), ("olio", 6, "f"), ("basilico", 3, "x")], ["Cuoci {peperoni} a dadini con {olio} e {cipolla} 10 minuti e unisci a metà cottura."]),
 ("alla barbabietola e caprino", [("barbabietole", 150, "v"), ("caprino", 50, "p"), ("cipolla", 25, "v"), ("olio", 5, "f"), ("erba_cipollina", 3, "x")], ["Frulla metà di {barbabietole} cotte con un mestolo di brodo e uniscile a metà cottura; manteca con {caprino}."]),
 ("al cavolfiore", [("cavolfiore", 200, "v"), ("cipolla", 25, "v"), ("parmigiano", 15, "f"), ("olio", 6, "f"), ("noce_moscata", 0.5, "x")], ["Lessa {cavolfiore} a cimette 8 minuti e uniscilo a metà cottura, schiacciandolo con il mestolo."]),
 ("ai broccoli e acciughe", [("broccoli", 200, "v"), ("acciughe", 10, "p"), ("cipolla", 25, "v"), ("olio", 6, "f"), ("peperoncino", 1, "x")], ["Sciogli {acciughe} nel soffritto con {peperoncino}, aggiungi {broccoli} a cimette a metà cottura."]),
 ("al salmone e zucchine", [("salmone", 110, "p"), ("zucchine", 120, "v"), ("cipolla", 25, "v"), ("olio", 6, "f"), ("erba_cipollina", 3, "x")], ["Aggiungi {zucchine} a dadini a metà cottura e {salmone} a cubetti negli ultimi 4 minuti."]),
 ("alle seppie", [("seppie", 150, "p"), ("piselli", 80, "v"), ("cipolla", 25, "v"), ("olio", 8, "f"), ("prezzemolo", 4, "x"), ("vino_bianco", 30, "x")], ["Rosola {seppie} a listarelle con {olio} e {cipolla}, sfuma con {vino_bianco} e cuoci 10 minuti; unisci {piselli}."]),
 ("alle vongole", [("vongole", 200, "p"), ("olio", 8, "f"), ("aglio", 3, "x"), ("prezzemolo", 5, "x"), ("vino_bianco", 30, "x")], ["Apri {vongole} in padella con {olio}, {aglio} e {vino_bianco}, sgusciane metà e tieni il liquido filtrato per cuocere il riso."]),
 ("al pesce spada", [("pesce_spada", 110, "p"), ("zucchine", 100, "v"), ("cipolla", 25, "v"), ("olio", 6, "f"), ("limone", 10, "x")], ["Aggiungi {pesce_spada} a cubetti e {zucchine} a dadini negli ultimi 5 minuti di cottura."]),
 ("alla zucca e salsiccia", [("zucca", 150, "v"), ("salsiccia", 70, "p"), ("cipolla", 25, "v"), ("olio", 4, "f"), ("rosmarino", 2, "x")], ["Sgrana {salsiccia} nel soffritto e aggiungi {zucca} a cubetti, che si scioglierà nel brodo."]),
 ("agli spinaci e ricotta", [("spinaci", 150, "v"), ("ricotta", 60, "p"), ("cipolla", 25, "v"), ("olio", 6, "f"), ("noce_moscata", 0.5, "x")], ["Aggiungi {spinaci} negli ultimi 5 minuti e manteca con {ricotta} fuori dal fuoco."]),
 ("ai funghi e salsiccia", [("funghi", 150, "v"), ("salsiccia", 70, "p"), ("scalogno", 25, "v"), ("olio", 4, "f"), ("prezzemolo", 4, "x")], ["Sgrana {salsiccia} nel soffritto di {scalogno} e {olio}, aggiungi {funghi} a fettine."]),
 ("ai porri e pollo", [("porri", 120, "v"), ("pollo", 100, "p"), ("olio", 6, "f"), ("parmigiano", 15, "f"), ("pepe", 0.5, "x")], ["Cuoci {porri} ad anelli e {pollo} a dadini nel soffritto per 8 minuti prima di tostare il riso."]),
 ("al cavolo nero e fagioli", [("cavolo_nero", 100, "v"), ("fagioli", 100, "p"), ("cipolla", 25, "v"), ("olio", 6, "f"), ("parmigiano", 10, "f")], ["Aggiungi {cavolo_nero} a pezzi a metà cottura e {fagioli} scolati negli ultimi 5 minuti."]),
 ("ai frutti di mare", [("cozze", 120, "p"), ("calamari", 80, "p"), ("gamberi", 80, "p"), ("olio", 8, "f"), ("aglio", 3, "x"), ("prezzemolo", 4, "x")], ["Apri {cozze} in padella con {aglio} e {olio}, aggiungi {calamari} ad anelli e {gamberi} sgusciati per 3 minuti e usa il liquido per cuocere il riso."]),
 ("al pomodoro e mozzarella", [("passata", 100, "v"), ("mozzarella", 50, "p"), ("basilico", 4, "x"), ("cipolla", 25, "v"), ("olio", 6, "f")], ["Versa {passata} dopo la tostatura e cuoci; manteca con {mozzarella} a dadini e {basilico}."]),
 ("alle verdure primavera", [("piselli", 60, "v"), ("asparagi", 100, "v"), ("zucchine", 80, "v"), ("cipolla", 25, "v"), ("olio", 6, "f"), ("parmigiano", 15, "f")], ["Unisci {asparagi}, {zucchine} e {piselli} a pezzetti a metà cottura."]),
 ("al gorgonzola e pere", [("gorgonzola", 40, "f"), ("pera", 100, "v"), ("cipolla", 25, "v"), ("olio", 5, "f"), ("noci", 10, "f")], ["Aggiungi {pera} a dadini a metà cottura e manteca con {gorgonzola}; completa con {noci} tritate."]),
 ("ai ceci e rosmarino", [("ceci", 120, "p", "ceci lessati"), ("cipolla", 25, "v"), ("rosmarino", 3, "x"), ("olio", 6, "f"), ("parmigiano", 10, "f")], ["Aggiungi {ceci} scolati a metà cottura e schiaccia una parte per rendere il risotto cremoso."]),
]
GR = [("riso", "Risotto", "il riso"), ("riso_int", "Risotto integrale", "il riso"), ("orzo", "Orzotto", "l'orzo"), ("farro", "Farrotto", "il farro")]

def gen_risi():
    for idx, (rest, ings, add_steps, *_) in enumerate(RISO):
        for gi, (g, lab, art) in enumerate(GR):
            # l'orzo e il farro stanno meglio con alcuni abbinamenti: evitiamo solo gli accostamenti pesce+farro integrale poco tradizionali
            if g == "riso_int" and any(k in rest for k in ("vongole", "frutti di mare")): continue
            name = f"{lab} {rest}"
            mins = {"riso": 25, "riso_int": 45, "orzo": 40, "farro": 35}[g]
            cook = {"riso": "Aggiungi un mestolo di brodo caldo alla volta e cuoci per 16-18 minuti, mescolando spesso.",
                    "riso_int": "Aggiungi un mestolo di brodo caldo alla volta e cuoci per 35-40 minuti, mescolando ogni tanto.",
                    "orzo": "Aggiungi un mestolo di brodo caldo alla volta e cuoci per 25-30 minuti, finché l'orzo è tenero ma sodo.",
                    "farro": "Aggiungi un mestolo di brodo caldo alla volta e cuoci per 20-25 minuti, finché il farro è tenero ma sodo."}[g]
            ing_list = [(g, 80, "c"), ("brodo_veg", 350, "x", "brodo vegetale caldo")] + list(ings)
            cond = [i for i in ings if i[0] in ("olio",)]
            soff = "Scalda il brodo vegetale. In una casseruola scalda l'olio e fai appassire cipolla o scalogno a fuoco dolce per 3-4 minuti, poi tosta {" + g + "} per 2 minuti."
            soff = soff.replace("l'olio", "{olio}") if any(i[0] == "olio" for i in ings) else soff.replace("l'olio", "un filo d'olio")
            if not any(i[0] in ("cipolla", "scalogno") for i in ings): soff = soff.replace("fai appassire cipolla o scalogno a fuoco dolce per 3-4 minuti, poi tosta", "tosta")
            else:
                soff = soff.replace("cipolla o scalogno", "{cipolla}" if any(i[0] == "cipolla" for i in ings) else "{scalogno}")
            steps = [soff] + add_steps + [cook, "A fine cottura togli dal fuoco, manteca con parmigiano o un filo d'olio se previsto e lascia riposare 2 minuti prima di servire."]
            pa = [i for i in ings if i[0] == "parmigiano"]
            steps[-1] = "A fine cottura togli dal fuoco" + (", manteca con {parmigiano}" if pa else "") + " e lascia riposare 2 minuti prima di servire."
            tip = "Il brodo va sempre aggiunto caldo, così la cottura non si ferma."
            add(name, "pranzo" if (idx + gi) % 3 else "cena", mins, ing_list, steps, tip, "italiana", ["padella"], 2, fam="risi")

# ======================= BOWL DI CEREALI =======================
# (nome topping, ingredienti, passaggi, cucina, minuti, basi consigliate)
BASI_BOWL = ["riso", "riso_int", "quinoa", "couscous", "bulgur", "farro", "miglio", "grano_saraceno", "basmati", "orzo"]
BOWL = [
 ("pollo, avocado e mais", [("pollo", 110, "p"), ("avocado", 50, "f"), ("mais", 60, "v"), ("pomodori", 80, "v"), ("lime", 15, "x"), ("coriandolo", 3, "x")],
  ["Cuoci {pollo} a straccetti in padella con un filo d'olio per 6-8 minuti.", "Taglia {avocado} a fette e {pomodori} a dadini.", "Componi la bowl con il cereale, il pollo, l'avocado, {mais} e {pomodori}; condisci con succo di {lime} e {coriandolo}."], "messicana", 25),
 ("salmone, edamame e avocado", [("salmone", 110, "p"), ("edamame", 60, "v"), ("avocado", 40, "f"), ("cetrioli", 60, "v"), ("salsa_soia", 10, "x"), ("sesamo", 5, "f")],
  ["Cuoci {salmone} in padella con un filo d'olio per 3-4 minuti per lato e sfaldalo.", "Taglia {cetrioli} a rondelle e {avocado} a fette.", "Componi la bowl con cereale, salmone, {edamame}, {cetrioli} e {avocado}; condisci con {salsa_soia} e {sesamo}."], "giapponese", 25),
 ("tonno, ceci e pomodorini", [("tonno_nat", 80, "p"), ("ceci", 80, "p", "ceci lessati"), ("pomodori", 100, "v", "pomodorini"), ("cetrioli", 60, "v"), ("olio", 6, "f"), ("limone", 15, "x")],
  ["Taglia {pomodori} a metà e {cetrioli} a cubetti.", "Componi la bowl con cereale, {tonno_nat} sgocciolato, {ceci} scolati e le verdure.", "Condisci con {olio} e {limone}."], "mediterranea", 15),
 ("tofu, verdure saltate e sesamo", [("tofu", 130, "p"), ("broccoli", 100, "v"), ("carote", 60, "v"), ("salsa_soia", 12, "x"), ("sesamo", 5, "f"), ("olio_sesamo", 4, "f")],
  ["Taglia {tofu} a cubetti e rosolalo in padella con {olio_sesamo} per 8 minuti.", "Aggiungi {broccoli} a cimette e {carote} a rondelle e salta 6 minuti.", "Sfuma con {salsa_soia}, versa sul cereale e completa con {sesamo}."], "asiatica", 25),
 ("ceci, spinaci e feta", [("ceci", 100, "p", "ceci lessati"), ("spinaci", 100, "v"), ("feta", 40, "f"), ("pomodori", 80, "v"), ("olio", 6, "f"), ("cumino", 1, "x")],
  ["Salta {spinaci} in padella con {olio} per 3 minuti.", "Scalda {ceci} con {cumino} per 4 minuti.", "Componi la bowl con cereale, ceci, spinaci e {pomodori}; completa con {feta} sbriciolata."], "mediterranea", 20),
 ("gamberi, zucchine e limone", [("gamberi", 130, "p"), ("zucchine", 150, "v"), ("limone", 20, "x"), ("olio", 6, "f"), ("aglio", 3, "x"), ("prezzemolo", 3, "x")],
  ["Salta {zucchine} a rondelle con {olio} e {aglio} per 6 minuti.", "Aggiungi {gamberi} sgusciati e cuoci 3-4 minuti.", "Servi sul cereale con {limone} e {prezzemolo}."], "mediterranea", 25),
 ("tacchino, peperoni e fagioli neri", [("tacchino", 110, "p"), ("peperoni", 100, "v"), ("fagioli_neri", 80, "p", "fagioli neri lessati"), ("salsa_messicana", 40, "x", "salsa messicana"), ("cumino", 1, "x"), ("lime", 15, "x")],
  ["Taglia {tacchino} a straccetti e cuocilo in padella con {cumino} per 7 minuti.", "Salta {peperoni} a listarelle per 8 minuti e scalda {fagioli_neri}.", "Componi la bowl con cereale, tacchino, peperoni e fagioli; completa con {salsa_messicana} e {lime}."], "messicana", 25),
 ("hummus, falafel e verdure", [("falafel", 100, "p"), ("hummus", 40, "f"), ("cetrioli", 80, "v"), ("pomodori", 80, "v"), ("lattuga", 40, "v"), ("limone", 15, "x")],
  ["Scalda {falafel} in forno a 200 °C per 12 minuti.", "Taglia {cetrioli} e {pomodori} a cubetti e {lattuga} a striscioline.", "Componi la bowl con cereale, falafel, verdure e {hummus}; condisci con {limone}."], "altre", 20),
 ("uova, spinaci e avocado", [("uova", 100, "p"), ("spinaci", 80, "v"), ("avocado", 50, "f"), ("pomodori", 80, "v"), ("olio", 4, "f"), ("pepe", 0.5, "x")],
  ["Cuoci {uova} sode in acqua bollente per 8 minuti, raffreddale e sgusciale.", "Salta {spinaci} con {olio} per 3 minuti.", "Componi la bowl con cereale, spinaci, {avocado} a fette, {pomodori} e le uova a metà."], "americana", 20),
 ("manzo, carote e cavolo rosso", [("manzo_magro", 110, "p"), ("carote", 80, "v"), ("cavolo_rosso", 60, "v"), ("salsa_soia", 10, "x"), ("zenzero", 5, "x"), ("sesamo", 5, "f")],
  ["Taglia {manzo_magro} a strisce e rosolalo in padella calda per 3-4 minuti con {zenzero} grattugiato.", "Taglia {carote} a julienne e {cavolo_rosso} a striscioline sottili.", "Componi la bowl con cereale, manzo, verdure e {salsa_soia}; completa con {sesamo}."], "asiatica", 25),
 ("pollo, curry e uvetta", [("pollo", 110, "p"), ("yogurt", 60, "f", "yogurt bianco"), ("curry", 2, "x"), ("carote", 60, "v"), ("uvetta", 15, "v"), ("coriandolo", 3, "x")],
  ["Marina {pollo} a cubetti con metà dello {yogurt} e {curry} per 10 minuti, poi cuocilo in padella 8 minuti.", "Grattugia {carote}.", "Componi la bowl con cereale, pollo, carote e {uvetta}; completa con lo yogurt rimasto e {coriandolo}."], "indiana", 30),
 ("lenticchie, zucca e feta", [("lenticchie", 100, "p", "lenticchie lessate"), ("zucca", 150, "v"), ("feta", 40, "f"), ("olio", 6, "f"), ("rosmarino", 2, "x")],
  ["Taglia {zucca} a cubetti, condiscila con {olio} e {rosmarino} e cuocila in forno a 200 °C per 25 minuti.", "Scalda {lenticchie} 3 minuti.", "Componi la bowl con cereale, zucca e lenticchie; completa con {feta} a cubetti."], "mediterranea", 35),
 ("salmone affumicato, cetrioli e yogurt", [("salmone_aff", 60, "p"), ("cetrioli", 80, "v"), ("greco0", 80, "p", "yogurt greco 0%"), ("aneto", 3, "x"), ("limone", 15, "x"), ("capperi", 5, "x")],
  ["Taglia {cetrioli} a rondelle.", "Mescola {greco0} con {aneto}, {limone} e un pizzico di sale.", "Componi la bowl con cereale, cetrioli e {salmone_aff}; completa con la salsa allo yogurt e {capperi}."], "altre", 15),
 ("pollo, mango e avocado", [("pollo", 110, "p"), ("mango", 80, "v"), ("avocado", 40, "f"), ("lime", 15, "x"), ("coriandolo", 3, "x"), ("peperoncino", 1, "x")],
  ["Cuoci {pollo} a cubetti in padella con un filo d'olio per 7 minuti.", "Taglia {mango} e {avocado} a cubetti.", "Componi la bowl con cereale, pollo, mango e avocado; condisci con {lime}, {coriandolo} e {peperoncino}."], "messicana", 25),
 ("verdure arrosto e hummus", [("zucchine", 100, "v"), ("peperoni", 100, "v"), ("melanzane", 80, "v"), ("hummus", 50, "f"), ("olio", 5, "f"), ("origano", 1, "x")],
  ["Taglia {zucchine}, {peperoni} e {melanzane} a pezzi, condiscili con {olio} e {origano} e cuocili in forno a 200 °C per 25 minuti.", "Componi la bowl con cereale, verdure arrosto e {hummus}."], "mediterranea", 35),
 ("tempeh, cavolo e carote", [("tempeh", 100, "p"), ("cavolo_rosso", 60, "v"), ("carote", 60, "v"), ("salsa_soia", 10, "x"), ("sesamo", 5, "f"), ("olio_sesamo", 4, "f")],
  ["Taglia {tempeh} a cubetti e rosolalo con {olio_sesamo} per 8 minuti.", "Taglia {cavolo_rosso} e {carote} a julienne.", "Componi la bowl con cereale, tempeh e verdure crude; condisci con {salsa_soia} e {sesamo}."], "asiatica", 20),
 ("fagioli, mais e pomodori", [("fagioli", 100, "p", "fagioli lessati"), ("mais", 60, "v"), ("pomodori", 100, "v"), ("cipolla", 20, "v"), ("olio", 6, "f"), ("origano", 1, "x")],
  ["Scola {fagioli} e {mais}.", "Taglia {pomodori} a dadini e {cipolla} a fettine sottilissime.", "Componi la bowl con cereale, fagioli, mais e verdure; condisci con {olio} e {origano}."], "mediterranea", 15),
 ("merluzzo, pomodorini e olive", [("merluzzo", 140, "p"), ("pomodori", 120, "v", "pomodorini"), ("olive", 15, "f"), ("olio", 5, "f"), ("prezzemolo", 3, "x")],
  ["Cuoci {merluzzo} in padella con {olio} per 4 minuti per lato, poi sfaldalo.", "Aggiungi {pomodori} a metà e {olive} e scalda 3 minuti.", "Servi sul cereale con {prezzemolo}."], "mediterranea", 25),
 ("pollo, spinaci e pomodori secchi", [("pollo", 110, "p"), ("spinaci", 100, "v"), ("pomodori_secchi", 15, "v"), ("olio", 6, "f"), ("pinoli", 8, "f")],
  ["Cuoci {pollo} a striscioline in padella con {olio} per 7 minuti.", "Aggiungi {spinaci} e {pomodori_secchi} a pezzetti e cuoci 3 minuti.", "Servi sul cereale con {pinoli}."], "italiana", 25),
 ("uova, fagioli e salsa", [("uova", 100, "p"), ("fagioli_neri", 80, "p", "fagioli neri lessati"), ("avocado", 40, "f"), ("salsa_messicana", 40, "x", "salsa messicana"), ("mais", 40, "v")],
  ["Scalda {fagioli_neri} e {mais} per 4 minuti.", "Cuoci {uova} al tegamino in padella antiaderente per 3 minuti.", "Componi la bowl con cereale, fagioli, mais, {avocado}, uova e {salsa_messicana}."], "messicana", 20),
 ("salmone e broccoli al vapore", [("salmone", 110, "p"), ("broccoli", 150, "v"), ("salsa_soia", 8, "x"), ("zenzero", 4, "x"), ("sesamo", 5, "f")],
  ["Cuoci {broccoli} a vapore per 8 minuti.", "Cuoci {salmone} a vapore o in padella per 8 minuti, poi sfaldalo.", "Componi la bowl con cereale, salmone e broccoli; condisci con {salsa_soia}, {zenzero} grattugiato e {sesamo}."], "giapponese", 25),
 ("gamberi, mango e cetrioli", [("gamberi", 120, "p"), ("mango", 80, "v"), ("cetrioli", 80, "v"), ("lime", 15, "x"), ("menta", 3, "x"), ("peperoncino", 1, "x")],
  ["Salta {gamberi} sgusciati in padella con un filo d'olio per 3-4 minuti.", "Taglia {mango} e {cetrioli} a cubetti.", "Componi la bowl con cereale, gamberi, mango e cetrioli; condisci con {lime}, {menta} e {peperoncino}."], "asiatica", 20),
 ("bresaola, rucola e grana", [("bresaola", 60, "p"), ("rucola", 40, "v"), ("parmigiano", 20, "f"), ("pomodori", 100, "v"), ("olio", 6, "f"), ("limone", 10, "x")],
  ["Taglia {pomodori} a pezzi.", "Componi la bowl con cereale tiepido, {rucola}, {pomodori}, {bresaola} e scaglie di {parmigiano}; condisci con {olio} e {limone}."], "italiana", 15),
]
def gen_bowl():
    for ti, (topping, ings, steps, cu, mins) in enumerate(BOWL):
        # poke/giapponese e asiatico: riso e basmati, quinoa; il resto: tutte le basi con rotazione
        pool = BASI_BOWL if cu not in ("giapponese",) else ["riso", "riso_int", "quinoa", "basmati", "miglio"]
        basi = pick(pool, 6, ti)
        for bi, base in enumerate(basi):
            name = f"Bowl di {CARB_LABEL[base]} con {topping}"
            g = CARB_G[base]
            ing_list = [(base, g, "c")] + list(ings)
            st = [cook_carb(base)] + steps
            add(name, "pranzo" if (ti + bi) % 3 else "cena", mins + GRAIN_MIN[base] // 2, ing_list, st, "Per il pranzo in ufficio prepara tutto la sera prima e condisci al momento.", cu, ["pranzo_ufficio", "meal_prep"] if bi % 2 else ["pranzo_ufficio"], 1, fam="bowl")

# ======================= INSALATE DI CEREALI E PASTA FREDDA =======================
INS_BASI = ["farro", "orzo", "riso", "couscous", "quinoa", "pasta", "bulgur", "miglio", "pasta_int", "riso_int"]
INS = [
 ("pomodorini, mozzarella e basilico", [("pomodori", 120, "v", "pomodorini"), ("mozzarella", 60, "p"), ("basilico", 4, "x"), ("olio", 8, "f"), ("origano", 0.5, "x")], ["Taglia {pomodori} a metà e {mozzarella} a dadini.", "Unisci al cereale freddo con {basilico}, {olio} e {origano}."], "italiana"),
 ("tonno, mais e olive", [("tonno_nat", 80, "p"), ("mais", 50, "v"), ("olive", 15, "f"), ("pomodori", 80, "v"), ("olio", 6, "f")], ["Taglia {pomodori} a pezzi.", "Mescola al cereale freddo {tonno_nat} sgocciolato, {mais}, {olive} e {olio}."], "italiana"),
 ("feta, cetrioli e pomodori", [("feta", 50, "f"), ("cetrioli", 80, "v"), ("pomodori", 100, "v"), ("olive", 15, "f"), ("olio", 6, "f"), ("origano", 1, "x")], ["Taglia {cetrioli}, {pomodori} e {feta} a cubetti.", "Unisci al cereale con {olive}, {olio} e {origano}."], "mediterranea"),
 ("pollo, zucchine e limone", [("pollo", 100, "p"), ("zucchine", 120, "v"), ("limone", 20, "x"), ("olio", 6, "f"), ("menta", 3, "x")], ["Cuoci {pollo} a cubetti in padella con un filo d'olio per 7 minuti.", "Grigliate o saltate, {zucchine} per 6 minuti.", "Unisci tutto al cereale freddo con {limone}, {olio} e {menta}."], "mediterranea"),
 ("gamberi, rucola e pomodorini", [("gamberi", 110, "p"), ("rucola", 30, "v"), ("pomodori", 100, "v", "pomodorini"), ("olio", 6, "f"), ("limone", 15, "x")], ["Salta {gamberi} sgusciati 3 minuti in padella.", "Unisci al cereale freddo con {rucola}, {pomodori} a metà, {olio} e {limone}."], "mediterranea"),
 ("ceci, peperoni e prezzemolo", [("ceci", 100, "p", "ceci lessati"), ("peperoni", 100, "v"), ("prezzemolo", 5, "x"), ("cipolla", 20, "v"), ("olio", 6, "f")], ["Taglia {peperoni} a dadini e {cipolla} sottilissima.", "Unisci al cereale freddo con {ceci}, {prezzemolo} e {olio}."], "mediterranea"),
 ("salmone affumicato e cetrioli", [("salmone_aff", 60, "p"), ("cetrioli", 80, "v"), ("aneto", 3, "x"), ("olio", 5, "f"), ("limone", 15, "x")], ["Taglia {cetrioli} a rondelle e {salmone_aff} a strisce.", "Unisci al cereale freddo con {aneto}, {olio} e {limone}."], "altre"),
 ("uova sode, fagiolini e pomodorini", [("uova", 100, "p"), ("fagiolini", 100, "v"), ("pomodori", 100, "v", "pomodorini"), ("olive", 10, "f"), ("olio", 6, "f")], ["Cuoci {uova} sode per 8 minuti e {fagiolini} per 10; raffredda tutto.", "Unisci al cereale freddo con {pomodori} a metà, {olive} e {olio}."], "italiana"),
 ("prosciutto crudo, melone e rucola", [("prosciutto_crudo", 40, "p"), ("melone", 120, "v"), ("rucola", 30, "v"), ("olio", 5, "f"), ("pepe", 0.5, "x")], ["Taglia {melone} a cubetti.", "Unisci al cereale freddo {prosciutto_crudo} a strisce, {rucola}, {olio} e pepe."], "italiana"),
 ("tonno, fagioli e cipolla", [("tonno_nat", 80, "p"), ("fagioli", 80, "p", "fagioli cannellini lessati"), ("cipolla", 25, "v"), ("prezzemolo", 4, "x"), ("olio", 6, "f")], ["Affetta {cipolla} sottilissima.", "Unisci al cereale freddo {tonno_nat}, {fagioli}, cipolla, {prezzemolo} e {olio}."], "italiana"),
 ("pomodori secchi, olive e capperi", [("pomodori_secchi", 20, "v"), ("olive", 20, "f"), ("capperi", 6, "x"), ("rucola", 30, "v"), ("olio", 6, "f"), ("mozzarella_light", 50, "p", "mozzarella light")], ["Trita {pomodori_secchi}.", "Unisci al cereale freddo {olive}, {capperi}, {rucola}, {mozzarella_light} a dadini e {olio}."], "italiana"),
 ("tacchino, mela e noci", [("fesa_tacchino", 90, "p", "fesa di tacchino"), ("mela", 100, "v"), ("noci", 12, "f"), ("lattuga", 40, "v"), ("limone", 10, "x")], ["Taglia {fesa_tacchino} e {mela} a cubetti, bagna la mela con {limone}.", "Unisci al cereale freddo con {lattuga} spezzettata e {noci}."], "americana"),
 ("barbabietola, caprino e noci", [("barbabietole", 120, "v"), ("caprino", 50, "p"), ("noci", 10, "f"), ("rucola", 30, "v"), ("olio", 5, "f")], ["Taglia {barbabietole} cotte a cubetti.", "Unisci al cereale freddo {caprino} a pezzetti, {noci}, {rucola} e {olio}."], "altre"),
 ("zucchine, menta e ricotta", [("zucchine", 150, "v"), ("ricotta", 60, "p"), ("menta", 3, "x"), ("limone", 15, "x"), ("olio", 6, "f")], ["Grattugia {zucchine} a crudo o grigliale e tagliale a dadini.", "Unisci al cereale freddo con {ricotta}, {menta}, {limone} e {olio}."], "mediterranea"),
 ("edamame, carote e soia", [("edamame", 80, "v"), ("carote", 70, "v"), ("salsa_soia", 10, "x"), ("sesamo", 5, "f"), ("zenzero", 4, "x"), ("olio_sesamo", 4, "f")], ["Cuoci {edamame} 4 minuti e grattugia {carote}.", "Unisci al cereale freddo con {salsa_soia}, {zenzero} grattugiato, {olio_sesamo} e {sesamo}."], "asiatica"),
]
def gen_ins():
    for ti, (topping, ings, steps, cu) in enumerate(INS):
        for bi, base in enumerate(pick(INS_BASI, 7, ti)):
            if base in ("couscous",) and any(k in topping for k in ("salmone",)) and False: continue
            pasta = base.startswith("pasta")
            name = (f"Pasta fredda con {topping}" if base == "pasta" else f"Pasta integrale fredda con {topping}" if base == "pasta_int" else f"Insalata di {CARB_LABEL[base]} con {topping}")
            ing_list = [(base, CARB_G[base] - 5, "c")] + list(ings)
            st = [cook_carb(base) + (" Raffreddala sotto l'acqua corrente e condiscila con un filo d'olio." if pasta or base in ("farro", "orzo", "riso", "riso_int") else " Lascia raffreddare completamente.")] + steps
            add(name, "pranzo", 20 + GRAIN_MIN[base] // 2, ing_list, st, "Si conserva 2 giorni in frigorifero: perfetta per il pranzo da portare.", cu, ["freddo", "pranzo_ufficio", "meal_prep"], 1, fam="insalate_cereali")

def generate():
    gen_pasta(); gen_risi(); gen_bowl(); gen_ins()
