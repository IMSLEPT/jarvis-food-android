# Punto di ripresa fotografia ricette — 25 ottobre 2026

Ramo: `feat/recipe-editorial-photos`. Conteggi verificati sui file approvati.

## Stato verificato

- 2533 ricette, 10239 passaggi nel database.
- 162 ricette complete di foto principale e di tutti i passaggi.
- 452 WebP distinti, 781 abbinamenti verificati.
- 498 passaggi delle ricette complete coperti.
- 2371 ricette da completare.
- Ripresa precedente del 25 ottobre: 15 nuove ricette e 36 nuove immagini rispetto al checkpoint da 114 ricette e 280 immagini.
- Ripristinati i tre abbinamenti s2 yogurt/miele/noci mancanti nel commit 6098540: immagini esistenti, ora tutte collegate.
- APK precedente consegnato il 25 ottobre 2026: **JarvisFood-v1.0.89.apk**, 129 ricette complete e 316 immagini effettivamente verificate nel pacchetto (196,0 MB).
- Build precedente GitHub Actions 40168930071, commit 3b91fa8ffaa42036cac65174066fcbd8faf653f8: compilazione, 70 test unitari e test Android 15/10 superati; modello Foto AI offline incluso.
- SHA-256 APK 1.0.89: `04f34ee72bef0dd4b2642d726947bdc38aaa523dde2a11d93ea09da5e405a6f8`.
- File persistente della consegna 1.0.89: `libfile_bb8cca6e0d308191ba6c892ef3563556`.

## APK aggiornato richiesto dall’utente — 25 ottobre 2026

Pubblicato [JarvisFood-v1.0.90.apk](https://github.com/IMSLEPT/jarvis-food-android/releases/download/v1.0.90/JarvisFood-v1.0.90.apk), snapshot del commit `b98706fcaff4f6414482f67742969a06b1e145a2`: **158 ricette complete, 435 immagini distinte e 763 abbinamenti**, su 2533 ricette. Dimensione 211897197 byte; SHA-256 `c735cdd0998bc3ab07596907593d88dcf4684d4f31722c1c5416885d6730c3af`.

Build [40217600483](https://github.com/IMSLEPT/jarvis-food-android/actions/runs/40217600483) completata: **70 test unitari e 11 test strumentati superati su Android 15/10**. Prodotti 120 screenshot; controllati visivamente diario, ricetta con testo standard e ingrandito, conversazione Jarvis. Verificate firma crittografica APK, versione e compatibilità dell’aggiornamento dalla 1.0.89; modello Foto AI offline presente e identico. Tutti i 435 WebP corrispondono agli hash approvati e tutti i 316 WebP della 1.0.89 sono identici. Controlli fotografia e confine funzionale superati. Nessuna modifica a ricette, grammature, nutrizione, grafica o funzionalità.

Pubblicazione del medesimo APK tramite [40219731299](https://github.com/IMSLEPT/jarvis-food-android/actions/runs/40219731299), senza ricompilazione: dimensione e SHA-256 dell’asset GitHub confrontati con il file verificato. Rapporto: [docs/APK-1.0.90.md](../../docs/APK-1.0.90.md). La produzione fotografica resta da riprendere da `salmone_friggitrice_asparagi`; restano 2375 ricette da completare.

## Aggiornamento UI richiesto dopo la consegna 1.0.90 — 25 ottobre 2026

Pubblicato [JarvisFood-v1.0.93.apk](https://github.com/IMSLEPT/jarvis-food-android/releases/download/v1.0.93/JarvisFood-v1.0.93.apk), source `ca86f55`, con la correzione dei Salva di Obiettivi, Preferenze della dieta e modifica del nome. Questo APK conserva lo snapshot fotografico della 1.0.90 (158 ricette complete, 435 immagini, 763 abbinamenti); il gruppo successivo del salmone resta nel repository. Firma di aggiornamento identica, modello AI offline e tutte le 435 immagini del precedente APK preservati byte per byte.

Build `40220576079` riuscita: 70 test unitari e 15 esecuzioni di test Android 15/10 superate, inclusi sette footer con navigazione a tre tasti, gesti e testo ingrandito; controllati visivamente gli screenshot dei Salva e degli altri footer. Pubblicazione senza ricompilazione `40221825447` riuscita. Asset GitHub: 211897197 byte, SHA-256 `3591fa99251b0735df6ac66b8a0de691550b974df1e7fdab8c67816b425c257e`, uguale al file verificato. Rapporti: [APK-1.0.93.md](../../docs/APK-1.0.93.md) e [verifiche JSON](../../docs/APK-1.0.93-verification.json). Nessuna compilazione aggiuntiva per il gruppo fotografico.

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

Ripresa dal commit `2986464`, verificato su GitHub senza aggiornamenti successivi. Aggiunte **6 ricette complete, 31 immagini distinte e 33 abbinamenti** in questa prosecuzione. Nessun acquisto, credito acquistabile, API esterna a pagamento o abbonamento aggiuntivo. Generatore integrato `image_gen` disponibile ed effettivamente utilizzato.

Ricette completate con immagine principale e tutti i passaggi:

- Buddha bowl di quinoa, tofu e verdure arrosto (`buddha_bowl_tofu`).
- Noodles di riso con tofu e verdure (`noodles_tofu_verdure`).
- Tacos di pollo, avocado e salsa (`tacos_pollo_avocado`).
- Pollo al curry e yogurt con riso basmati (`pollo_curry_basmati`).
- Dal di lenticchie rosse con riso (`dal_lenticchie_rosse`).
- Noodles di riso con gamberi, uova e germogli (`pad_thai_gamberi`).

Le immagini sono state controllate visivamente per ingredienti, fase, cottura, utensili e presentazione, quindi integrate con `prepare.py` e `share.py`. Verifiche superate: `verify.py`, `scripts/verify-aurora-boundary.py`, indice Kotlin coerente col manifest, decodifica WebP, assenza di file duplicati con contenuto identico. Ricerca gratuita su Wikimedia Commons per la quinoa: nessun risultato adatto; nessuna nuova fotografia online importata. Le fotografie reali già presenti e le attribuzioni restano intatte.

Tutti i **383 WebP e 679 record** del checkpoint iniziale, insieme ai crediti offline, sono identici. Database, ingredienti, grammature, nutrizione, grafica, funzionalità e distribuzione/condivisione delle immagini preservati. Ogni gruppo salvato su GitHub con `[skip ci]` prima del successivo. Nessun nuovo APK; test unitari e Android non rieseguiti per questi soli aggiornamenti fotografici.

Verifica visiva finale dei 31 nuovi WebP anche nel ritaglio quadrato centrale: ingredienti principali e azioni conservati nell’inquadratura. In questa prosecuzione prodotti 32 output del generatore integrato: 31 approvati e una prima versione del servizio dei tacos sostituita prima dell’integrazione per eliminare ingredienti ambigui nelle ciotole sullo sfondo. Le prove scartate non sono negli asset né negli abbinamenti. Nessuna regola di condivisione modificata.

Punto di ripresa precedente (vedere ultimo gruppo): **Fajitas di pollo e peperoni (`fajitas_pollo_peperoni`)**. Rigenerare `python tools/photography/plan.py --out /tmp/pending.jsonl`. Restano **2380 ricette** e **12060 abbinamenti** mancanti. Nessuna produzione in background.

## Gruppo con pipeline riprendibile — 2026-10-25

Ricette completate: fajitas_pollo_peperoni. Nuove immagini: 5; nuovi abbinamenti: 5.

Aggiunti `batch.py`, `doctor.py` e `AUTOMATION.md`: coda riprendibile, revisione
legata all'hash e rapporto salvato in `tools/photography/reviews/`, blocco dei
gruppi incompleti o delle modifiche successive alla pianificazione, ripristino
dei metadati dopo errori. Otto test della pipeline superati, senza rete o generazione.
Prova reale completata con il generatore incluso e revisione visiva assistita;
nessun generatore locale o processo programmato installato/attivato. L'ambiente
attuale è Linux x86_64, non il Mac dell'utente; per valutare la generazione locale
restano da conoscere chip e RAM del Mac e da verificare la qualità di una prova.

Revisione visiva individuale registrata per ingredienti, fase, realismo, stile e ritaglio. Conversione con prepare.py, riuso con share.py, verify.py e controllo degli originali superati. File fotografici e record precedenti preservati; database e funzionalità invariati. Nessuna API a pagamento, nessun nuovo APK; test unitari/Android non rieseguiti.

Punto di ripresa precedente (vedere ultimo gruppo): **hamburger_pollo_panino**. Restano 2379 ricette e 12055 abbinamenti; 8015 gruppi fotografici ancora da produrre. Commit [skip ci] e push del gruppo prima del successivo. Nessun processo in background avviato.

## Gruppo con pipeline riprendibile — 2026-10-25

Ricette completate: mirtilli_fresca. Nuove immagini: 3; nuovi abbinamenti: 21.

Revisione visiva individuale registrata per ingredienti, fase, realismo, stile e ritaglio. Conversione con prepare.py, riuso con share.py, verify.py e controllo degli originali superati. File fotografici e record precedenti preservati; database e funzionalità invariati. Nessuna API a pagamento, nessun nuovo APK; test unitari/Android non rieseguiti.

Punto di ripresa precedente (vedere ultimo gruppo): **hamburger_pollo_panino**. Restano 2378 ricette e 12034 abbinamenti; 8012 gruppi fotografici ancora da produrre. Commit [skip ci] e push del gruppo prima del successivo. Nessun processo in background avviato.

Ricerca professionale gratuita su Unsplash, Pexels e Pixabay: licenze ufficiali verificate; nessun acquisto, piano premium o API a pagamento. Le fotografie reali importate sono elencate in LICENSED-PHOTOS.md e nei crediti offline, con fonte/autore/licenza conservati nel manifest e nell’app. Scartati candidati macro poco nitidi o scuri, foto con fragole insieme ai lamponi e risultati non scaricabili in qualità sufficiente. I passaggi mancanti vengono prodotti con image_gen e revisionati individualmente. Tutti i 419 WebP e i 717 record del checkpoint iniziale sono identici; i crediti preesistenti sono conservati integralmente.

Hardware comunicato dall’utente: Mac 13 pollici, Apple M3, 8 GB di memoria. Il Mac non è accessibile dall’ambiente Linux della sessione; nessun modello locale installato o attivato e nessuna qualità/velocità locale dichiarata senza prova.

## Richiesta di produzione con PC spento — 25 ottobre 2026

Configurata e abilitata l'attività cloud **Foto Jarvis Food**, con un gruppo
completo per tentativo e cadenza oraria; prima finestra dalle 17:15 Europe/Rome.
La creazione e il calendario sono confermati, ma **non è ancora verificata
un'esecuzione che produca e pubblichi fotografie**. Il comando di avvio immediato
ha restituito `404 Action not found`; nessun risultato viene attribuito a questo
tentativo. Vedere `AUTOMATION.md` per stato e vincoli della prova.

Nessun costo o servizio a pagamento attivato. Il prompt richiede di fermarsi e
disattivarsi se manca una capacità indispensabile o se servono risorse a pagamento;
include revisione visiva, licenze, conservazione dei file, verifiche e commit
`[skip ci]`. Nessun agente locale o nuovo APK. I conteggi restano 155 ricette
complete, 422 immagini e 738 abbinamenti fino a un nuovo gruppo realmente salvato.
La candidata Pixabay dei lamponi di Saramukitza non è ancora integrata.

## Gruppo con pipeline riprendibile — 2026-10-25

Ricette completate: lamponi_fresca. Nuove immagini: 3; nuovi abbinamenti: 15.

Revisione visiva individuale registrata per ingredienti, fase, realismo, stile e ritaglio. Conversione con prepare.py, riuso con share.py, verify.py e controllo degli originali superati. File fotografici e record precedenti preservati; database e funzionalità invariati. Nessuna API a pagamento, nessun nuovo APK; test unitari/Android non rieseguiti.

Punto di ripresa precedente (vedere ultimo gruppo): **hamburger_pollo_panino**. Restano 2377 ricette e 12019 abbinamenti; 8009 gruppi fotografici ancora da produrre. Commit [skip ci] e push del gruppo prima del successivo. Questa esecuzione termina dopo il solo gruppo dei lamponi.

Verifiche aggiuntive del gruppo lamponi: 422 WebP e 738 record del checkpoint `51ec955` identici; crediti precedenti conservati integralmente mediante sola appendice. Nuove immagini: 1 foto reale Pixabay di Saramukitza e 2 immagini con image_gen incluso, tutte viste nell’originale e nel ritaglio dei WebP convertiti. I 12 riusi aggiuntivi rappresentano esclusivamente la stessa azione di risciacquo dei lamponi; nessuna regola di condivisione modificata. Fonte/autore/licenza sincronizzati nel manifest, in LICENSED-PHOTOS.md e nei crediti offline.

In questa esecuzione generazione inclusa, visione, script e accesso al checkout aggiornato sono effettivamente disponibili; un singolo gruppo è stato preparato e verificato. La conferma di pubblicazione è il commit effettivo sul ramo. Questo risultato non dimostra produzione illimitata né disponibilità garantita delle esecuzioni successive. Nessun nuovo APK, nessuna API a consumo e nessun servizio a pagamento attivato.

## Gruppo con pipeline riprendibile — 2026-10-25

Ricette completate: hamburger_pollo_panino. Nuove immagini: 5; nuovi abbinamenti: 5.

Revisione visiva individuale registrata per ingredienti, fase, realismo, stile e ritaglio. Conversione con prepare.py, riuso con share.py, verify.py e controllo degli originali superati. File fotografici e record precedenti preservati; database e funzionalità invariati. Nessuna API a pagamento, nessun nuovo APK; test unitari/Android non rieseguiti.

Punto di ripresa precedente (vedere ultimo gruppo): **pollo_friggitrice_patate**. Restano 2376 ricette e 12014 abbinamenti; 8004 gruppi fotografici ancora da produrre. Commit [skip ci] e push del gruppo prima del successivo. Questa esecuzione termina dopo il gruppo dell’hamburger di pollo.

Ripresa dal checkpoint `969d93d`, verificato contro GitHub senza aggiornamenti successivi. Cinque output del generatore incluso, tutti approvati dopo esame individuale di originali e ritagli, poi ricontrollati nei WebP convertiti: piatto finito, formazione del macinato crudo, cottura in padella, cheddar sotto coperchio e assemblaggio del panino. Nessuna fotografia online aggiunta in questo gruppo. Tutti i **425 WebP e 753 record** precedenti sono identici, insieme alle attribuzioni online e offline. Rapporto con prompt, hash e osservazioni in `tools/photography/reviews/2026-10-25T16-40-46.763490+00-00.json`. Nessun costo aggiuntivo e nessun nuovo APK.

## Gruppo con pipeline riprendibile — 2026-10-25

Ricette completate: pollo_friggitrice_patate. Nuove immagini: 5; nuovi abbinamenti: 5.

Revisione visiva individuale registrata per ingredienti, fase, realismo, stile e ritaglio. Conversione con prepare.py, riuso con share.py, verify.py e controllo degli originali superati. File fotografici e record precedenti preservati; database e funzionalità invariati. Nessuna API a pagamento, nessun nuovo APK; test unitari/Android non rieseguiti.

Punto di ripresa precedente (vedere ultimo gruppo): **salmone_friggitrice_asparagi**. Restano 2375 ricette e 12009 abbinamenti; 7999 gruppi fotografici ancora da produrre. Commit [skip ci] e push del gruppo prima del successivo. Questa esecuzione termina dopo il gruppo del pollo in friggitrice.

Ripresa dal checkpoint `9285c05`, verificato contro GitHub senza aggiornamenti successivi. Prodotti sei output del generatore incluso: cinque immagini approvate e una prima versione della principale sostituita per rimuovere foglioline estranee dallo sfondo. Originali e ritagli esaminati individualmente, poi ricontrollati nei WebP convertiti. Cinque soggetti: piatto finito, patate crude condite, pollo crudo condito, giro in friggitrice e broccoli al vapore (alternativa prevista dalla ricetta). Il taglio di pollo segue il catalogo: sovracosce senza pelle. Nessuna fotografia online aggiunta. Tutti i **430 WebP e 758 record** precedenti sono identici, insieme alle attribuzioni online e offline. Rapporto con prompt, hash, osservazioni e prova scartata in `tools/photography/reviews/2026-10-25T17-19-13.730738+00-00.json`. Nessun costo aggiuntivo e nessun nuovo APK.

## Gruppo con pipeline riprendibile — 2026-10-25

Ricette completate: salmone_friggitrice_asparagi. Nuove immagini: 4; nuovi abbinamenti: 4.

Revisione visiva individuale registrata per ingredienti, fase, realismo, stile e ritaglio. Conversione con prepare.py, riuso con share.py, verify.py e controllo degli originali superati. File fotografici e record precedenti preservati; database e funzionalità invariati. Nessuna API a pagamento, nessun nuovo APK; test unitari/Android non rieseguiti.

Punto di ripresa precedente (vedere ultimo gruppo): **parmigiana_light**. Restano 2374 ricette e 12005 abbinamenti; 7995 gruppi fotografici ancora da produrre. Commit [skip ci] e push del gruppo prima del successivo. Questa esecuzione termina dopo il solo gruppo del salmone.

Ripresa dal checkpoint fotografico `b98706f`, con gli aggiornamenti successivi fino a `ca86f55` conservati e verificati contro GitHub. Generatore incluso `image_gen` realmente disponibile: quattro output singoli, approvati dopo esame individuale di originali e ritagli quadrati e ricontrollati nei WebP convertiti (1280×853). Soggetti: piatto finito, salmone crudo condito con olio/limone/pepe, asparagi crudi conditi con olio e cottura nel cestello della friggitrice ad aria. Nessuna fotografia online aggiunta; nessun costo aggiuntivo. Tutti i **435 WebP e 763 record** precedenti sono identici, insieme alle attribuzioni online e offline. Rapporto con prompt effettivi, hash e osservazioni in `tools/photography/reviews/2026-10-25T18-18-17.547898+00-00.json`.

Separatamente, l’utente ha richiesto la correzione dei pulsanti Salva sovrapposti alla navigazione Android. La modifica del componente condiviso è nel commit `ca86f55`, precedente a questo gruppo, e la relativa build UI `40220576079` usa quel preciso snapshot fotografico (158 ricette, 435 immagini). Nessuna build aggiuntiva richiesta dal gruppo fotografico e nessuna modifica di interfaccia inserita nel suo commit.

## Gruppo con pipeline riprendibile — 2026-10-25

Ricette completate: parmigiana_light. Nuove immagini: 4; nuovi abbinamenti: 5.

Revisione visiva individuale registrata per ingredienti, fase, realismo, stile e ritaglio. Conversione con prepare.py, riuso con share.py, verify.py e controllo degli originali superati. File fotografici e record precedenti preservati; database e funzionalità invariati. Nessuna API a pagamento, nessun nuovo APK; test unitari/Android non rieseguiti.

Punto di ripresa precedente (vedere ultimo gruppo): **pollo_panna_broccoli**. Restano 2373 ricette e 12000 abbinamenti; 7991 gruppi fotografici ancora da produrre. Commit [skip ci] e push del gruppo prima del successivo. Questa esecuzione termina dopo il solo gruppo della parmigiana.


Ripresa da `112c80b`, verificato contro GitHub e conservando le correzioni UI e la consegna APK 1.0.93. Prodotti cinque output del generatore incluso: quattro approvati e una prima principale scartata perché il ritaglio quadrato tagliava parte della porzione e del pane. La versione corretta conserva entrambi interamente nel ritaglio. Originali e ritagli esaminati individualmente, poi ricontrollati nei quattro WebP 1280×853. Soggetti: parmigiana con pane integrale, melanzane al forno, assemblaggio con passata/mozzarella/basilico e gratinatura nella pirofila in forno. Il solo riuso aggiunto da `share.py` è la principale per il passaggio finale di servizio con pane, già previsto dalle regole esistenti. Tutti i **439 WebP e 767 record** precedenti e le attribuzioni online/offline sono identici; verificati indice Kotlin/manifest e decodifica/dimensioni. Rapporto con prompt effettivi, hash, osservazioni e candidata scartata: `tools/photography/reviews/2026-10-25T19-11-38.906108+00-00.json`. Nessuna fotografia online aggiunta, costo aggiuntivo, modifica funzionale o nuovo APK.

## Gruppo con pipeline riprendibile — 2026-10-25

Ricette completate: pollo_panna_broccoli. Nuove immagini: 5; nuovi abbinamenti: 5.

Revisione visiva individuale registrata per ingredienti, fase, realismo, stile e ritaglio. Conversione con prepare.py, riuso con share.py, verify.py e controllo degli originali superati. File fotografici e record precedenti preservati; database e funzionalità invariati. Nessuna API a pagamento, nessun nuovo APK; test unitari/Android non rieseguiti.

Punto di ripresa precedente (vedere ultimo gruppo): **frittata_zucchine_feta**. Restano 2372 ricette e 11995 abbinamenti; 7986 gruppi fotografici ancora da produrre. Commit [skip ci] e push del gruppo prima del successivo. Questa esecuzione termina dopo il solo gruppo del pollo cremoso con broccoli.


Ripresa da `057c75b`, verificato contro GitHub senza aggiornamenti successivi e conservando le correzioni UI e le consegne APK precedenti. Generatore incluso effettivamente disponibile: sei output prodotti, cinque approvati e una prima versione della crema scartata per puntini scuri simili a pepe non previsto e salsa troppo abbondante. La sostitutiva mostra una copertura più contenuta. Tutti gli originali e i ritagli sono stati esaminati individualmente, poi ricontrollati nei cinque WebP 1280×853. Soggetti: piatto finito, broccoli al vapore, rosolatura del petto di pollo con aglio, crema di panna/Parmigiano e aggiunta dei broccoli in padella. Nessun nuovo riuso compatibile trovato da `share.py`, senza cambiarne le regole. Tutti i **443 WebP e 772 record** precedenti e le attribuzioni online/offline sono identici; controllo fotografia, confine funzionale, indice Kotlin/manifest e decodifica/dimensioni superati. Rapporto con prompt effettivi, hash, osservazioni e candidata scartata: `tools/photography/reviews/2026-10-25T20-13-41.293726+00-00.json`. Nessuna fotografia online aggiunta, costo aggiuntivo, modifica a ricette/funzionalità o nuovo APK.

## Gruppo con pipeline riprendibile — 2026-10-25

Ricette completate: frittata_zucchine_feta. Nuove immagini: 4; nuovi abbinamenti: 4.

Revisione visiva individuale registrata per ingredienti, fase, realismo, stile e ritaglio. Conversione con prepare.py, riuso con share.py, verify.py e controllo degli originali superati. File fotografici e record precedenti preservati; database e funzionalità invariati. Nessuna API a pagamento, nessun nuovo APK; test unitari/Android non rieseguiti.

Punto preciso da cui riprendere: **polpette_pollo_polenta**. Restano 2371 ricette e 11991 abbinamenti; 7982 gruppi fotografici ancora da produrre. Commit [skip ci] e push del gruppo prima del successivo. Questa esecuzione termina dopo il solo gruppo della frittata con zucchine e feta.


Ripresa da `06664e1`, verificato contro GitHub senza aggiornamenti successivi; conservate tutte le modifiche precedenti. Generatore incluso realmente disponibile: cinque output prodotti, quattro approvati e una prima versione delle zucchine scartata per granelli bianchi sulla spatola e ciotola di ingredienti sullo sfondo, rimossi nella sostitutiva. Originali e ritagli esaminati individualmente, poi ricontrollati nei WebP convertiti: tre 1280×853 e uno quadrato 1254×1254. Soggetti: frittata servita con fetta tagliata, rondelle di zucchine saltate, versamento di uova e origano senza feta e cottura in forno dopo aggiunta della feta. Nessun riuso compatibile aggiuntivo da `share.py`; regole invariate. Tutti i **448 WebP e 777 record** precedenti e le attribuzioni online/offline sono identici. Verifiche superate: fotografia, confine funzionale, indice Kotlin/manifest e decodifica/dimensioni. Rapporto con prompt effettivi, hash, osservazioni e candidata scartata: `tools/photography/reviews/2026-10-25T21-20-31.680838+00-00.json`. Nessuna fotografia online aggiunta, costo aggiuntivo, modifica a ricette/funzionalità o compilazione APK; test unitari/Android non ripetuti per questo gruppo fotografico.
