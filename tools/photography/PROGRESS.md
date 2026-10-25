# Punto di ripresa fotografia ricette — 25 ottobre 2026

Ramo: `feat/recipe-editorial-photos`. Conteggi verificati sui file approvati.

## Stato verificato

- 2533 ricette, 10239 passaggi nel database.
- 135 ricette complete di foto principale e di tutti i passaggi.
- 339 WebP distinti, 626 abbinamenti verificati.
- 399 passaggi delle ricette complete coperti.
- 2398 ricette da completare.
- Ripresa precedente del 25 ottobre: 15 nuove ricette e 36 nuove immagini rispetto al checkpoint da 114 ricette e 280 immagini.
- Ripristinati i tre abbinamenti s2 yogurt/miele/noci mancanti nel commit 6098540: immagini esistenti, ora tutte collegate.
- APK aggiornato consegnato il 25 ottobre 2026: **JarvisFood-v1.0.89.apk**, 129 ricette complete e 316 immagini effettivamente verificate nel pacchetto (196,0 MB).
- Build GitHub Actions 40168930071, commit 3b91fa8ffaa42036cac65174066fcbd8faf653f8: compilazione, 70 test unitari e test Android 15/10 superati; modello Foto AI offline incluso.
- SHA-256 APK: `04f34ee72bef0dd4b2642d726947bdc38aaa523dde2a11d93ea09da5e405a6f8`.
- File persistente consegnato: `libfile_bb8cca6e0d308191ba6c892ef3563556`.

## Ricette completate nella ripresa precedente

- Porridge vegano alla mandorla e mirtilli (`porridge_mandorla_mirtilli`)
- Uova strapazzate, feta e pomodorini (`uova_feta_pomodorini`)
- Tortillas con uova, fagioli neri e salsa (`tortilla_uova_fagioli`)
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

## Coda prioritaria originaria (vedere sotto per il punto aggiornato)

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
- Curry di ceci e spinaci al latte di cocco (`curry_ceci_spinaci`): 5 abbinamenti mancanti.

## Metodo e verifiche

Usare plan.py per i brief, generare un soggetto per immagine, controllare ingredienti/fase/forma del pane. Integrare con prepare.py, poi share.py; eseguire verify.py e scripts/verify-aurora-boundary.py. Salvare ogni gruppo sul ramo GitHub prima del successivo. Le fotografie sono locali/offline. Prompt e metadati sono in approved.json; conservare fonte/autore/licenza per immagini online gratuite. Il catalogo intero non è ancora completato. Grammature, database e funzionalità precedenti sono preservati.

## Prosecuzione senza costi aggiuntivi — 25 ottobre 2026

Ripartenza dal commit `4711c04` controllato su GitHub: nessun aggiornamento successivo al checkpoint APK 1.0.89 al momento della clonazione. Aggiunte **6 ricette complete, 23 immagini distinte e 28 abbinamenti** in questa sessione.

Generatore integrato `image_gen` effettivamente utilizzato; nessuna API esterna a pagamento, acquisto di crediti o attivazione di abbonamenti. Nessun generatore locale disponibile (nessuna GPU NVIDIA esposta, Torch/Diffusers assenti). La prova iniziale dei pancake è stata controllata visivamente prima di completarne i passaggi.

Ricette completate, ciascuna con foto principale e tutti i passaggi:

- Pancake alla ricotta e fragole (`pancake_ricotta_fragole`)
- Chia pudding al mango (`chia_pudding_mango`)
- Colazione giapponese: riso, uova e zuppa di miso (`colazione_giapponese`)
- Edamame al vapore (`edamame_sale`)
- Zuppa di miso con tofu e alga (`miso_tofu_alga`)
- Insalata di arance e finocchi (`arance_finocchi`)

Verifiche: tutte le nuove immagini esaminate visivamente per ingredienti, fase, utensili, luce e inquadratura; conversione con `prepare.py`, abbinamenti con `share.py`, controllo `verify.py` e `scripts/verify-aurora-boundary.py` superati. I 316 WebP iniziali e tutti i 598 record preesistenti sono stati confrontati con il commit di ripartenza e sono identici. Database, grammature, dati nutrizionali e funzionalità preservati. Nessun nuovo APK compilato; i test Android e i 70 test unitari restano quelli della consegna 1.0.89, non sono stati rieseguiti per questi gruppi fotografici.

Punto preciso da cui riprendere: **Uova sode e pomodorini (`uova_sode_pomodorini`)**. Rigenerare l’inventario con `python tools/photography/plan.py --out /tmp/pending.jsonl`; restano 2398 ricette e 12146 abbinamenti mancanti. Nessuna produzione in background. Salvare ogni gruppo con `[skip ci]` prima del successivo.
