# Punto di ripresa fotografia ricette — 24 ottobre 2026

Ramo: `feat/recipe-editorial-photos`. Conteggi basati sui file approvati effettivamente presenti.

## Stato verificato

- 2533 ricette, 10.239 passaggi nel database.
- 108 ricette complete di foto principale e di tutti i passaggi.
- 268 WebP distinti (1 fotografie online gratuite), 530 abbinamenti verificati.
- 312 passaggi delle ricette complete coperti; altri passaggi sono condivisi con varianti compatibili.
- 2425 ricette da completare.
- Sessione continuativa: 18 nuove immagini, 9 ricette ulteriori completate rispetto alle 99 iniziali.
- Checkpoint precedente 6af589f0: 99 ricette, 250 immagini, 285 passaggi delle ricette complete. Tutti conservati.
- Ultimo APK consegnato al momento del checkpoint: 1.0.87, con 49 ricette fotografiche complete. Le nuove immagini necessitano di una compilazione aggiornata; non dichiararle presenti nell’APK consegnato senza verifica.

## Ricette completate nella sessione continuativa

- Pane integrale con caprino, pere e noci (`pane_integrale_con_caprino_pere_e_noci`)
- Pane con caprino, pere e noci (`pane_con_caprino_pere_e_noci`)
- Fette biscottate con caprino, pere e noci (`fette_biscottate_con_caprino_pere_e_noci`)
- Pane integrale con ricotta, pesche e pistacchi (`pane_integrale_con_ricotta_pesche_e_pistacchi`)
- Pane con ricotta, pesche e pistacchi (`pane_con_ricotta_pesche_e_pistacchi`)
- Fette biscottate con ricotta, pesche e pistacchi (`fette_biscottate_con_ricotta_pesche_e_pistacchi`)
- Pane integrale con mozzarella, pomodoro e origano (`pane_integrale_con_mozzarella_pomodoro_e_origano`)
- Pane con mozzarella, pomodoro e origano (`pane_con_mozzarella_pomodoro_e_origano`)
- Fette biscottate con mozzarella, pomodoro e origano (`fette_biscottate_con_mozzarella_pomodoro_e_origano`)

## Prossime ricette da completare

- Pane integrale con bresaola e rucola (`pane_integrale_con_bresaola_e_rucola`): 2 nuovi soggetti mancanti.
- Pane con bresaola e rucola (`pane_con_bresaola_e_rucola`): 2 nuovi soggetti mancanti.
- Fette biscottate con bresaola e rucola (`fette_biscottate_con_bresaola_e_rucola`): 2 nuovi soggetti mancanti.
- Pane integrale con yogurt greco, miele e noci (`pane_integrale_con_yogurt_greco_miele_e_noci`): 2 nuovi soggetti mancanti.
- Pane con yogurt greco, miele e noci (`pane_con_yogurt_greco_miele_e_noci`): 2 nuovi soggetti mancanti.
- Fette biscottate con yogurt greco, miele e noci (`fette_biscottate_con_yogurt_greco_miele_e_noci`): 2 nuovi soggetti mancanti.
- Pane integrale con formaggio spalmabile e frutti di bosco (`pane_integrale_con_formaggio_spalmabile_e_frutti_di_bosco`): 2 nuovi soggetti mancanti.
- Pane con formaggio spalmabile e frutti di bosco (`pane_con_formaggio_spalmabile_e_frutti_di_bosco`): 2 nuovi soggetti mancanti.
- Fette biscottate con formaggio spalmabile e frutti di bosco (`fette_biscottate_con_formaggio_spalmabile_e_frutti_di_bosco`): 2 nuovi soggetti mancanti.
- Pane integrale con tahina e banana (`pane_integrale_con_tahina_e_banana`): 2 nuovi soggetti mancanti.
- Pane con tahina e banana (`pane_con_tahina_e_banana`): 2 nuovi soggetti mancanti.
- Fette biscottate con tahina e banana (`fette_biscottate_con_tahina_e_banana`): 2 nuovi soggetti mancanti.

## Metodo e verifiche

Usare `plan.py --out /tmp/pending.jsonl` per aggiornare i brief; non genera fotografie. Produrre solo i soggetti mancanti, controllare ingredienti, cottura, forma del pane e fase della preparazione. Integrare con prepare.py, poi share.py. Per le foto online usare `kind: licensed_photo` e il blocco `source` completo; prepare.py ne conserva la provenienza anche nelle condivisioni. Verificare con verify.py e scripts/verify-aurora-boundary.py. Salvare ogni gruppo sul ramo GitHub prima di continuare. I prompt delle immagini AI e le licenze delle fotografie reali sono in approved.json.

La dichiarazione storica di 66 ricette includeva immagini allora non recuperabili: sono state ricostruite nella prima ripresa. Usare sempre i conteggi attuali, senza considerarli il completamento dell’intero catalogo. Database nutrizionali e funzionalità precedenti restano conservati.
