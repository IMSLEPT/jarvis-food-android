# Punto di ripresa fotografia ricette — 25 ottobre 2026

Ramo: `feat/recipe-editorial-photos`. Conteggi verificati sui file approvati.

## Stato verificato

- 2533 ricette, 10239 passaggi nel database.
- 151 ricette complete di foto principale e di tutti i passaggi.
- 404 WebP distinti, 701 abbinamenti verificati.
- 459 passaggi delle ricette complete coperti.
- 2382 ricette da completare.
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

Ripartenza dal commit `4711c04` controllato su GitHub: nessun aggiornamento successivo al checkpoint APK 1.0.89 al momento della clonazione. Aggiunte **12 ricette complete, 46 immagini distinte e 55 abbinamenti** in questa sessione.

Generatore integrato `image_gen` effettivamente utilizzato; nessuna API esterna a pagamento, acquisto di crediti o attivazione di abbonamenti. Nessun generatore locale disponibile (nessuna GPU NVIDIA esposta, Torch/Diffusers assenti). La prova iniziale dei pancake è stata controllata visivamente prima di completarne i passaggi.

Ricette completate, ciascuna con foto principale e tutti i passaggi:

- Pancake alla ricotta e fragole (`pancake_ricotta_fragole`)
- Chia pudding al mango (`chia_pudding_mango`)
- Colazione giapponese: riso, uova e zuppa di miso (`colazione_giapponese`)
- Edamame al vapore (`edamame_sale`)
- Zuppa di miso con tofu e alga (`miso_tofu_alga`)
- Insalata di arance e finocchi (`arance_finocchi`)
- Uova sode e pomodorini (`uova_sode_pomodorini`)
- Gallette con tonno e pomodoro (`tonno_gallette`)
- Tzatziki con bastoncini di verdure (`tzatziki_verdure`)
- Burrito bowl con pollo e fagioli neri (`burrito_bowl_pollo`)
- Chili vegano di fagioli neri e riso (`chili_fagioli_neri`)
- Curry di ceci e spinaci al latte di cocco (`curry_ceci_spinaci`)

Verifiche: tutte le nuove immagini esaminate visivamente per ingredienti, fase, utensili, luce e inquadratura; conversione con `prepare.py`, abbinamenti con `share.py`, controllo `verify.py` e `scripts/verify-aurora-boundary.py` superati. I 316 WebP iniziali e tutti i 598 record preesistenti sono stati confrontati con il commit di ripartenza e sono identici. Database, grammature, dati nutrizionali e funzionalità preservati. Nessun nuovo APK compilato; i test Android e i 70 test unitari restano quelli della consegna 1.0.89, non sono stati rieseguiti per questi gruppi fotografici.

Sono state generate 52 prove singole: 46 approvate e 6 scartate e rigenerate prima dell’integrazione (miscela chia già troppo densa, scolapasta edamame senza appoggio, finocchio simile a cipolla, porzione uova incompleta, pollo con puntini simili a pepe, curry con pezzi di pomodoro invece di passata). Le prove scartate non sono nel repository né negli abbinamenti. Ricontrollati tutti i 46 WebP convertiti anche nel ritaglio quadrato; dimensioni 1280×853, decodifica valida. Il controllo del chili ha rilevato il passaggio finale mancante: aggiunta una quinta foto dedicata al servizio del riso, poi verifica ripetuta con esito positivo. Nessuna regola di condivisione modificata.

Punto di ripresa dopo i primi 12 gruppi (superato dalla prosecuzione seguente): **Bowl di riso, salmone ed edamame (`riso_salmone_edamame`)**. Rigenerare l’inventario con `python tools/photography/plan.py --out /tmp/pending.jsonl`; restano 2392 ricette e 12119 abbinamenti mancanti. Nessuna produzione in background. Salvare ogni gruppo con `[skip ci]` prima del successivo.

## Prosecuzione con ricerca di foto gratuite — 25 ottobre 2026

Ripresa dal commit `bb13fb2`, controllato contro GitHub senza aggiornamenti successivi. Aggiunte **6 ricette complete, 21 nuove immagini distinte e 26 abbinamenti**. Di queste nuove immagini, **1 fotografia online** e **20 illustrazioni fotografiche con il generatore integrato**. Nessuna API a pagamento, acquisto di crediti o abbonamento attivato.

Ricette completate in questa prosecuzione, con tutti i passaggi:

- Bowl di riso, salmone ed edamame (`riso_salmone_edamame`).
- Bowl con falafel, hummus e cous cous (`falafel_bowl`).
- Insalata greca con pollo e pane pita (`insalata_greca_pollo`).
- Insalatona di riso, tonno e mais (`riso_tonno_mais`).
- Pasta fredda alla caprese (`pasta_fredda_caprese`).
- Banana con cioccolato fondente (`banana_con_cioccolato_fondente`).

Ricerca online tramite API pubblica gratuita di Wikimedia Commons, con verifica di autore, pagina del file e licenza ufficiale, seguita da esame delle fotografie. Scartate immagini con ingredienti diversi, preparazioni diverse o qualità insufficiente. Fotografie importate con metadati completi in `approved.json`, attribuzioni in `LICENSED-PHOTOS.md` e copia dei crediti inclusa negli asset offline. I crediti all’autore sono conservati dall’indice esistente; nessuna fotografia reale etichettata AI. Le foto mancanti sono prodotte con `image_gen`, un soggetto per fotografia.

Controlli superati: `verify.py`, `scripts/verify-aurora-boundary.py`, corrispondenza completa indice Kotlin/manifest, decodifica WebP e controllo visivo dei nuovi soggetti. Tutti i **362 WebP e 653 record del checkpoint iniziale sono identici**; nessuna modifica a database, ingredienti, grammature, nutrizione, funzionalità o regole di distribuzione/condivisione. Ogni gruppo salvato separatamente con `[skip ci]` prima di produrre il successivo. Nessun APK compilato; test unitari e Android non rieseguiti per questi aggiornamenti fotografici.

Verifica visiva finale anche sui ritagli quadrati dei 21 nuovi WebP. Prodotti 24 output del generatore integrato: 20 immagini AI nella raccolta attuale e 4 versioni intermedie scartate o sostituite (consistenza della mozzarella, ingrediente estraneo sullo sfondo della caprese, mais escluso dal ritaglio della preparazione del riso). La foto di preparazione del riso è stata corretta con una composizione quadrata che conserva visibili carota, pomodoro, tonno e mais. La fotografia reale di Lenore Edman è stata verificata nell’originale e dopo conversione, senza ritocchi al soggetto; credito e licenza conservati anche offline.

Punto di ripresa precedente (superato dalla terza prosecuzione): **Buddha bowl di quinoa, tofu e verdure arrosto (`buddha_bowl_tofu`)**. Rigenerare `python tools/photography/plan.py --out /tmp/pending.jsonl`. Restano **2386 ricette** e **12093 abbinamenti** da completare. Nessuna produzione in background.

## Terza prosecuzione fotografica — 25 ottobre 2026

Ripresa dal commit `2986464`, verificato su GitHub senza aggiornamenti successivi. Aggiunte **4 ricette complete, 21 immagini distinte e 22 abbinamenti** in questa prosecuzione. Nessun acquisto, credito acquistabile, API esterna a pagamento o abbonamento aggiuntivo. Generatore integrato `image_gen` disponibile ed effettivamente utilizzato.

Ricette completate con immagine principale e tutti i passaggi:

- Buddha bowl di quinoa, tofu e verdure arrosto (`buddha_bowl_tofu`).
- Noodles di riso con tofu e verdure (`noodles_tofu_verdure`).
- Tacos di pollo, avocado e salsa (`tacos_pollo_avocado`).
- Pollo al curry e yogurt con riso basmati (`pollo_curry_basmati`).

Le immagini sono state controllate visivamente per ingredienti, fase, cottura, utensili e presentazione, quindi integrate con `prepare.py` e `share.py`. Verifiche superate: `verify.py`, `scripts/verify-aurora-boundary.py`, indice Kotlin coerente col manifest, decodifica WebP, assenza di file duplicati con contenuto identico. Ricerca gratuita su Wikimedia Commons per la quinoa: nessun risultato adatto; nessuna nuova fotografia online importata. Le fotografie reali già presenti e le attribuzioni restano intatte.

Tutti i **383 WebP e 679 record** del checkpoint iniziale, insieme ai crediti offline, sono identici. Database, ingredienti, grammature, nutrizione, grafica, funzionalità e distribuzione/condivisione delle immagini preservati. Ogni gruppo salvato su GitHub con `[skip ci]` prima del successivo. Nessun nuovo APK; test unitari e Android non rieseguiti per questi soli aggiornamenti fotografici.

Punto preciso da cui riprendere: **Dal di lenticchie rosse con riso (`dal_lenticchie_rosse`)**. Rigenerare `python tools/photography/plan.py --out /tmp/pending.jsonl`. Restano **2382 ricette** e **12071 abbinamenti** mancanti. Nessuna produzione in background.
