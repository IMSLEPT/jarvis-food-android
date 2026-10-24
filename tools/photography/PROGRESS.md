# Punto di ripresa fotografia ricette — 24 ottobre 2026

Ramo: `feat/recipe-editorial-photos`. Conteggi basati sui file approvati effettivamente presenti.

## Stato verificato

- 2533 ricette, 10.239 passaggi nel database.
- 82 ricette complete di foto principale e di tutti i passaggi.
- 217 WebP distinti (1 fotografie online gratuite), 434 abbinamenti verificati.
- 238 passaggi delle ricette complete coperti; altri passaggi sono condivisi con varianti compatibili.
- 2451 ricette da completare.
- Seconda ripresa: 15 nuove immagini, 8 ricette ulteriori completate rispetto alle 74 iniziali.
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

## Prossime ricette da completare

- Fette biscottate con avocado e uova (`fette_biscottate_con_avocado_e_uova`): 2 nuovi soggetti mancanti.
- Pane integrale con ricotta, pomodorini e basilico (`pane_integrale_con_ricotta_pomodorini_e_basilico`): 2 nuovi soggetti mancanti.
- Pane con ricotta, pomodorini e basilico (`pane_con_ricotta_pomodorini_e_basilico`): 2 nuovi soggetti mancanti.
- Fette biscottate con ricotta e marmellata (`fette_biscottate_con_ricotta_e_marmellata`): 2 nuovi soggetti mancanti.
- Pane integrale con prosciutto cotto e mozzarella (`pane_integrale_con_prosciutto_cotto_e_mozzarella`): 2 nuovi soggetti mancanti.
- Pane con prosciutto cotto e mozzarella (`pane_con_prosciutto_cotto_e_mozzarella`): 2 nuovi soggetti mancanti.
- Pane integrale con tonno e avocado (`pane_integrale_con_tonno_e_avocado`): 2 nuovi soggetti mancanti.
- Pane con tonno e avocado (`pane_con_tonno_e_avocado`): 2 nuovi soggetti mancanti.
- Pane integrale con fiocchi di latte e cetrioli (`pane_integrale_con_fiocchi_di_latte_e_cetrioli`): 2 nuovi soggetti mancanti.
- Pane con fiocchi di latte e cetrioli (`pane_con_fiocchi_di_latte_e_cetrioli`): 2 nuovi soggetti mancanti.
- Pane integrale con caprino, pere e noci (`pane_integrale_con_caprino_pere_e_noci`): 2 nuovi soggetti mancanti.
- Pane con caprino, pere e noci (`pane_con_caprino_pere_e_noci`): 2 nuovi soggetti mancanti.

## Metodo e verifiche

Usare `plan.py --out /tmp/pending.jsonl` per aggiornare i brief; non genera fotografie. Produrre solo i soggetti mancanti, controllare ingredienti, cottura, forma del pane e fase della preparazione. Integrare con prepare.py, poi share.py. Per le foto online usare `kind: licensed_photo` e il blocco `source` completo; prepare.py ne conserva la provenienza anche nelle condivisioni. Verificare con verify.py e scripts/verify-aurora-boundary.py. Salvare ogni gruppo sul ramo GitHub prima di continuare. I prompt delle immagini AI e le licenze delle fotografie reali sono in approved.json.

La dichiarazione storica di 66 ricette includeva immagini allora non recuperabili: sono state ricostruite nella prima ripresa. Usare sempre i conteggi attuali, senza considerarli il completamento dell’intero catalogo. Database nutrizionali e funzionalità precedenti restano conservati.
