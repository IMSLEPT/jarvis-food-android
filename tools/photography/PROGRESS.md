# Punto di ripresa fotografia ricette — 24 ottobre 2026

Ramo: `feat/recipe-editorial-photos`. Conteggi basati sui file approvati effettivamente presenti.

## Stato verificato

- 2533 ricette, 10.239 passaggi nel database.
- 86 ricette complete di foto principale e di tutti i passaggi.
- 225 WebP distinti (1 fotografie online gratuite), 467 abbinamenti verificati.
- 248 passaggi delle ricette complete coperti; altri passaggi sono condivisi con varianti compatibili.
- 2447 ricette da completare.
- Seconda ripresa: 23 nuove immagini, 12 ricette ulteriori completate rispetto alle 74 iniziali.
- Checkpoint precedente c662c008: 74 ricette, 202 immagini, 214 passaggi delle ricette complete. Tutti conservati.
- Ultimo APK consegnato al momento del checkpoint: 1.0.87, con 49 ricette fotografiche complete. Le nuove immagini necessitano di una compilazione aggiornata; non dichiararle presenti nell’APK consegnato senza verifica.

## Ricette completate nella seconda ripresa

- Pane integrale con ricotta, miele e fichi (`pane_integrale_con_ricotta_miele_e_fichi`)
- Pane con ricotta, miele e fichi (`pane_con_ricotta_miele_e_fichi`)
- Pane integrale con avocado e uova (`pane_integrale_con_avocado_e_uova`)
- Pane con avocado e uova (`pane_con_avocado_e_uova`)
- Pane integrale con formaggio spalmabile e salmone affumicato (`pane_integrale_con_formaggio_spalmabile_e_salmone_affumicato`)
- Pane con formaggio spalmabile e salmone affumicato (`pane_con_formaggio_spalmabile_e_salmone_affumicato`)
- Pane integrale con hummus e pomodori (`pane_integrale_con_hummus_e_pomodori`)
- Pane con hummus e pomodori (`pane_con_hummus_e_pomodori`)
- Pane integrale con ricotta, pomodorini e basilico (`pane_integrale_con_ricotta_pomodorini_e_basilico`)
- Pane con ricotta, pomodorini e basilico (`pane_con_ricotta_pomodorini_e_basilico`)
- Fette biscottate con ricotta e marmellata (`fette_biscottate_con_ricotta_e_marmellata`)
- Pane integrale con prosciutto cotto e mozzarella (`pane_integrale_con_prosciutto_cotto_e_mozzarella`)

## Prossime ricette da completare

- Fette biscottate con avocado e uova (`fette_biscottate_con_avocado_e_uova`): 1 nuovi soggetti mancanti.
- Fette biscottate con ricotta, miele e fichi (`fette_biscottate_con_ricotta_miele_e_fichi`): 2 nuovi soggetti mancanti.
- Fette biscottate con formaggio spalmabile e salmone affumicato (`fette_biscottate_con_formaggio_spalmabile_e_salmone_affumica`): 2 nuovi soggetti mancanti.
- Fette biscottate con hummus e pomodori (`fette_biscottate_con_hummus_e_pomodori`): 2 nuovi soggetti mancanti.
- Fette biscottate con ricotta, pomodorini e basilico (`fette_biscottate_con_ricotta_pomodorini_e_basilico`): 2 nuovi soggetti mancanti.
- Pane con prosciutto cotto e mozzarella (`pane_con_prosciutto_cotto_e_mozzarella`): 2 nuovi soggetti mancanti.
- Fette biscottate con prosciutto cotto e mozzarella (`fette_biscottate_con_prosciutto_cotto_e_mozzarella`): 2 nuovi soggetti mancanti.
- Pane integrale con tonno e avocado (`pane_integrale_con_tonno_e_avocado`): 2 nuovi soggetti mancanti.
- Pane con tonno e avocado (`pane_con_tonno_e_avocado`): 2 nuovi soggetti mancanti.
- Fette biscottate con tonno e avocado (`fette_biscottate_con_tonno_e_avocado`): 2 nuovi soggetti mancanti.
- Pane integrale con fiocchi di latte e cetrioli (`pane_integrale_con_fiocchi_di_latte_e_cetrioli`): 2 nuovi soggetti mancanti.
- Pane con fiocchi di latte e cetrioli (`pane_con_fiocchi_di_latte_e_cetrioli`): 2 nuovi soggetti mancanti.

## Metodo e verifiche

Usare `plan.py --out /tmp/pending.jsonl` per aggiornare i brief; non genera fotografie. Produrre solo i soggetti mancanti, controllare ingredienti, cottura, forma del pane e fase della preparazione. Integrare con prepare.py, poi share.py. Per le foto online usare `kind: licensed_photo` e il blocco `source` completo; prepare.py ne conserva la provenienza anche nelle condivisioni. Verificare con verify.py e scripts/verify-aurora-boundary.py. Salvare ogni gruppo sul ramo GitHub prima di continuare. I prompt delle immagini AI e le licenze delle fotografie reali sono in approved.json.

La dichiarazione storica di 66 ricette includeva immagini allora non recuperabili: sono state ricostruite nella prima ripresa. Usare sempre i conteggi attuali, senza considerarli il completamento dell’intero catalogo. Database nutrizionali e funzionalità precedenti restano conservati.

## Verifiche di questa ripresa

- Compilazione release/debug e test JVM superati sul commit b41ce405 (nuova gestione foto reali e 76 ricette).
- Suite UI su emulatore e screenshot completati con successo nello stesso run: https://github.com/IMSLEPT/jarvis-food-android/actions/runs/40109634224
- I gruppi successivi modificano solo immagini, indice generato e documentazione. Sull’ultimo gruppo: verificati 86 procedimenti completi, 467 abbinamenti, decodifica di tutti i 225 WebP, conservazione dei 409 abbinamenti del checkpoint iniziale e protezione dei database/funzioni originali.
- Controllo negativo: un’importazione classificata come foto reale senza metadati di licenza viene respinta senza cambiare approved.json.
- Non è stato consegnato un APK contenente tutte le 86 ricette di questo checkpoint.
