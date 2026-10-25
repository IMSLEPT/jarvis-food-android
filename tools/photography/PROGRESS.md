# Punto di ripresa fotografia ricette — 25 ottobre 2026

Ramo: `feat/recipe-editorial-photos`. Conteggi verificati sui file approvati.

## Stato verificato

- 2533 ricette, 10239 passaggi nel database.
- 128 ricette complete di foto principale e di tutti i passaggi.
- 311 WebP distinti, 593 abbinamenti verificati.
- 373 passaggi delle ricette complete coperti.
- 2405 ricette da completare.
- Ripresa del 25 ottobre: 14 nuove ricette e 31 nuove immagini rispetto al checkpoint da 114 ricette e 280 immagini.
- Ripristinati i tre abbinamenti s2 yogurt/miele/noci mancanti nel commit 6098540: immagini esistenti, ora tutte collegate.
- Ultimo APK consegnato noto: 1.0.87 con 49 ricette complete. Queste aggiunte sono nei sorgenti, non ancora in un nuovo APK verificato.

## Ricette completate in questa ripresa

- Porridge vegano alla mandorla e mirtilli (`porridge_mandorla_mirtilli`)
- Uova strapazzate, feta e pomodorini (`uova_feta_pomodorini`)
- Pane integrale con formaggio spalmabile e frutti di bosco (`pane_integrale_con_formaggio_spalmabile_e_frutti_di_bosco`)
- Pane con formaggio spalmabile e frutti di bosco (`pane_con_formaggio_spalmabile_e_frutti_di_bosco`)
- Fette biscottate con formaggio spalmabile e frutti di bosco (`fette_biscottate_con_formaggio_spalmabile_e_frutti_di_bosco`)
- Pane integrale con tahina e banana (`pane_integrale_con_tahina_e_banana`)
- Pane con tahina e banana (`pane_con_tahina_e_banana`)
- Fette biscottate con tahina e banana (`fette_biscottate_con_tahina_e_banana`)
- Pane integrale con mele, cannella e ricotta (`pane_integrale_con_mele_cannella_e_ricotta`)
- Pane con mele, cannella e ricotta (`pane_con_mele_cannella_e_ricotta`)
- Fette biscottate con mele, cannella e ricotta (`fette_biscottate_con_mele_cannella_e_ricotta`)
- Pane integrale con pomodori secchi, caprino e rucola (`pane_integrale_con_pomodori_secchi_caprino_e_rucola`)
- Pane con pomodori secchi, caprino e rucola (`pane_con_pomodori_secchi_caprino_e_rucola`)
- Fette biscottate con pomodori secchi, caprino e rucola (`fette_biscottate_con_pomodori_secchi_caprino_e_rucola`)

## Prossime ricette da completare

- Tortillas con uova, fagioli neri e salsa (`tortilla_uova_fagioli`): 5 abbinamenti mancanti.
- Pancake alla ricotta e fragole (`pancake_ricotta_fragole`): 5 abbinamenti mancanti.
- Chia pudding al mango (`chia_pudding_mango`): 5 abbinamenti mancanti.
- Colazione giapponese: riso, uova e zuppa di miso (`colazione_giapponese`): 5 abbinamenti mancanti.
- Edamame al vapore (`edamame_sale`): 4 abbinamenti mancanti.
- Zuppa di miso con tofu e alga (`miso_tofu_alga`): 5 abbinamenti mancanti.
- Insalata di arance e finocchi (`arance_finocchi`): 4 abbinamenti mancanti.
- Uova sode e pomodorini (`uova_sode_pomodorini`): 4 abbinamenti mancanti.
- Gallette con tonno e pomodoro (`tonno_gallette`): 4 abbinamenti mancanti.
- Tzatziki con bastoncini di verdure (`tzatziki_verdure`): 4 abbinamenti mancanti.
- Burrito bowl con pollo e fagioli neri (`burrito_bowl_pollo`): 5 abbinamenti mancanti.
- Chili vegano di fagioli neri e riso (`chili_fagioli_neri`): 5 abbinamenti mancanti.

## Metodo e verifiche

Usare plan.py per i brief, generare un soggetto per immagine, controllare ingredienti/fase/forma del pane. Integrare con prepare.py, poi share.py; eseguire verify.py e scripts/verify-aurora-boundary.py. Salvare ogni gruppo sul ramo GitHub prima del successivo. Le fotografie sono locali/offline. Prompt e metadati sono in approved.json; conservare fonte/autore/licenza per immagini online gratuite. Il catalogo intero non è ancora completato. Grammature, database e funzionalità precedenti sono preservati.
