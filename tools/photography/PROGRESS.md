# Punto di ripresa fotografia ricette — 24 ottobre 2026

Ramo: `feat/recipe-editorial-photos`. Conteggi basati sui file approvati effettivamente presenti.

## Stato verificato

- 2533 ricette, 10.239 passaggi nel database.
- 99 ricette complete di foto principale e di tutti i passaggi.
- 250 WebP distinti (1 fotografie online gratuite), 503 abbinamenti verificati.
- 285 passaggi delle ricette complete coperti; altri passaggi sono condivisi con varianti compatibili.
- 2434 ricette da completare.
- Sessione continuativa: 25 nuove immagini, 13 ricette ulteriori completate rispetto alle 86 iniziali.
- Checkpoint precedente 759c31ad: 86 ricette, 225 immagini, 248 passaggi delle ricette complete. Tutti conservati.
- Ultimo APK consegnato al momento del checkpoint: 1.0.87, con 49 ricette fotografiche complete. Le nuove immagini necessitano di una compilazione aggiornata; non dichiararle presenti nell’APK consegnato senza verifica.

## Ricette completate nella sessione continuativa

- Fette biscottate con ricotta, miele e fichi (`fette_biscottate_con_ricotta_miele_e_fichi`)
- Fette biscottate con avocado e uova (`fette_biscottate_con_avocado_e_uova`)
- Fette biscottate con formaggio spalmabile e salmone affumicato (`fette_biscottate_con_formaggio_spalmabile_e_salmone_affumica`)
- Fette biscottate con hummus e pomodori (`fette_biscottate_con_hummus_e_pomodori`)
- Fette biscottate con ricotta, pomodorini e basilico (`fette_biscottate_con_ricotta_pomodorini_e_basilico`)
- Pane con prosciutto cotto e mozzarella (`pane_con_prosciutto_cotto_e_mozzarella`)
- Fette biscottate con prosciutto cotto e mozzarella (`fette_biscottate_con_prosciutto_cotto_e_mozzarella`)
- Pane integrale con tonno e avocado (`pane_integrale_con_tonno_e_avocado`)
- Pane con tonno e avocado (`pane_con_tonno_e_avocado`)
- Fette biscottate con tonno e avocado (`fette_biscottate_con_tonno_e_avocado`)
- Pane integrale con fiocchi di latte e cetrioli (`pane_integrale_con_fiocchi_di_latte_e_cetrioli`)
- Pane con fiocchi di latte e cetrioli (`pane_con_fiocchi_di_latte_e_cetrioli`)
- Fette biscottate con fiocchi di latte e cetrioli (`fette_biscottate_con_fiocchi_di_latte_e_cetrioli`)

## Prossime ricette da completare

- Pane integrale con caprino, pere e noci (`pane_integrale_con_caprino_pere_e_noci`): 2 nuovi soggetti mancanti.
- Pane con caprino, pere e noci (`pane_con_caprino_pere_e_noci`): 2 nuovi soggetti mancanti.
- Fette biscottate con caprino, pere e noci (`fette_biscottate_con_caprino_pere_e_noci`): 2 nuovi soggetti mancanti.
- Pane integrale con ricotta, pesche e pistacchi (`pane_integrale_con_ricotta_pesche_e_pistacchi`): 2 nuovi soggetti mancanti.
- Pane con ricotta, pesche e pistacchi (`pane_con_ricotta_pesche_e_pistacchi`): 2 nuovi soggetti mancanti.
- Fette biscottate con ricotta, pesche e pistacchi (`fette_biscottate_con_ricotta_pesche_e_pistacchi`): 2 nuovi soggetti mancanti.
- Pane integrale con mozzarella, pomodoro e origano (`pane_integrale_con_mozzarella_pomodoro_e_origano`): 2 nuovi soggetti mancanti.
- Pane con mozzarella, pomodoro e origano (`pane_con_mozzarella_pomodoro_e_origano`): 2 nuovi soggetti mancanti.
- Fette biscottate con mozzarella, pomodoro e origano (`fette_biscottate_con_mozzarella_pomodoro_e_origano`): 2 nuovi soggetti mancanti.
- Pane integrale con bresaola e rucola (`pane_integrale_con_bresaola_e_rucola`): 2 nuovi soggetti mancanti.
- Pane con bresaola e rucola (`pane_con_bresaola_e_rucola`): 2 nuovi soggetti mancanti.
- Fette biscottate con bresaola e rucola (`fette_biscottate_con_bresaola_e_rucola`): 2 nuovi soggetti mancanti.

## Metodo e verifiche

Usare `plan.py --out /tmp/pending.jsonl` per aggiornare i brief; non genera fotografie. Produrre solo i soggetti mancanti, controllare ingredienti, cottura, forma del pane e fase della preparazione. Integrare con prepare.py, poi share.py. Per le foto online usare `kind: licensed_photo` e il blocco `source` completo; prepare.py ne conserva la provenienza anche nelle condivisioni. Verificare con verify.py e scripts/verify-aurora-boundary.py. Salvare ogni gruppo sul ramo GitHub prima di continuare. I prompt delle immagini AI e le licenze delle fotografie reali sono in approved.json.

La dichiarazione storica di 66 ricette includeva immagini allora non recuperabili: sono state ricostruite nella prima ripresa. Usare sempre i conteggi attuali, senza considerarli il completamento dell’intero catalogo. Database nutrizionali e funzionalità precedenti restano conservati.

## Verifiche della ripresa corrente

- Conservati tutti i 475 abbinamenti trovati nella precedente chat (89 ricette).
- Completate altre 10 ricette: 99 in totale, 250 immagini distinte, 285 passaggi delle ricette complete.
- Tutti i WebP decodificati; hash, compatibilità del riuso, completezza delle procedure e corrispondenza dell’indice Kotlin verificati.
- Protezione dei database e delle funzioni originali superata. Nessuna nuova compilazione Android o nuovo APK in questa ripresa.
- File del lavoro corrente: /workspace/scratch/cb91287dd779/jarvis-food-android. I prompt effettivi sono conservati in approved.json.
- Ricerca foto gratuite: i risultati relativi a tonno e avocado contenevano ingredienti aggiuntivi (uova o guarnizioni) e non sono stati importati. La fotografia Unsplash già approvata e i relativi crediti sono conservati.
