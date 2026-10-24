# Punto di ripresa fotografia ricette — 24 ottobre 2026

Ramo: `feat/recipe-editorial-photos`. Questo file descrive asset effettivamente presenti e verificati, non generazioni pianificate.

## Stato verificato

- 2533 ricette e 10.239 passaggi nel database.
- 74 ricette complete di foto principale e di tutti i passaggi.
- 202 fotografie WebP distinte, 409 abbinamenti controllati.
- 214 passaggi delle ricette complete coperti; altri passaggi sono condivisi con varianti compatibili.
- 2459 ricette rimangono da completare.
- In questa ripresa: 29 nuove foto, 21 ricette ulteriori completate.
- Le 53 ricette del checkpoint c409d728 sono state conservate.
- La precedente dichiarazione di 66 ricette comprendeva file allora non recuperabili. Ora queste ricette sono state ricostruite, integrate e superate: usare i conteggi di approved.json.
- Ultimo APK consegnato: 1.0.87, contiene 49 ricette fotografiche complete. Le nuove fotografie richiedono una compilazione aggiornata; nessun nuovo APK è stato prodotto in questa ripresa.

## Ricette completate in questa ripresa

- Cetrioli e carote con hummus (`cetrioli_hummus`)
- Anguria e feta (`anguria_feta`)
- Ricotta, miele e noci (`ricotta_miele_noci`)
- Albumi sodi e pomodorini (`albume_sodo_pomodori`)
- Ciotola di yogurt greco 0% con banana e granola (`ciotola_di_yogurt_greco_0_con_banana_e_granola`)
- Coppa di yogurt greco intero con banana e noci (`coppa_di_yogurt_greco_intero_con_banana_e_noci`)
- Ciotola di yogurt greco intero con mela e fiocchi di avena (`ciotola_di_yogurt_greco_intero_con_mela_e_fiocchi_di_avena`)
- Bowl di kefir con mela e pistacchi (`bowl_di_kefir_con_mela_e_pistacchi`)
- Pane integrale con ricotta e marmellata (`pane_integrale_con_ricotta_e_marmellata`)
- Pane con ricotta e marmellata (`pane_con_ricotta_e_marmellata`)
- Yogurt greco intero con banana (`yogurt_greco_intero_con_banana`)
- Mela e nocciole (`mela_e_nocciole`)
- Mela e noci pecan (`mela_e_noci_pecan`)
- Yogurt magro con mela (`yogurt_magro_con_mela`)
- Ricotta e miele (`ricotta_e_miele`)
- Ricotta e frutti di bosco (`ricotta_e_frutti_di_bosco`)
- Noci e datteri (`noci_e_datteri`)
- Pistacchi e albicocche secche (`pistacchi_e_albicocche_secche`)
- Nocciole e cioccolato fondente (`nocciole_e_cioccolato_fondente`)
- Olive e cubetti di formaggio (`olive_e_cubetti_di_formaggio`)
- Melone e prosciutto crudo (`melone_e_prosciutto_crudo`)

## Prossime ricette da completare

- Pane integrale con ricotta, miele e fichi (`pane_integrale_con_ricotta_miele_e_fichi`): 2 nuovi soggetti fotografici mancanti.
- Pane con ricotta, miele e fichi (`pane_con_ricotta_miele_e_fichi`): 2 nuovi soggetti fotografici mancanti.
- Pane integrale con avocado e uova (`pane_integrale_con_avocado_e_uova`): 2 nuovi soggetti fotografici mancanti.
- Pane con avocado e uova (`pane_con_avocado_e_uova`): 2 nuovi soggetti fotografici mancanti.
- Pane integrale con formaggio spalmabile e salmone affumicato (`pane_integrale_con_formaggio_spalmabile_e_salmone_affumicato`): 2 nuovi soggetti fotografici mancanti.
- Pane con formaggio spalmabile e salmone affumicato (`pane_con_formaggio_spalmabile_e_salmone_affumicato`): 2 nuovi soggetti fotografici mancanti.
- Pane integrale con hummus e pomodori (`pane_integrale_con_hummus_e_pomodori`): 2 nuovi soggetti fotografici mancanti.
- Pane con hummus e pomodori (`pane_con_hummus_e_pomodori`): 2 nuovi soggetti fotografici mancanti.
- Pane integrale con ricotta, pomodorini e basilico (`pane_integrale_con_ricotta_pomodorini_e_basilico`): 2 nuovi soggetti fotografici mancanti.
- Pane con ricotta, pomodorini e basilico (`pane_con_ricotta_pomodorini_e_basilico`): 2 nuovi soggetti fotografici mancanti.
- Fette biscottate con ricotta e marmellata (`fette_biscottate_con_ricotta_e_marmellata`): 2 nuovi soggetti fotografici mancanti.
- Pane integrale con prosciutto cotto e mozzarella (`pane_integrale_con_prosciutto_cotto_e_mozzarella`): 2 nuovi soggetti fotografici mancanti.

## Riprendere senza duplicati

1. Leggere questo file e `approved.json` dal ramo fotografico corrente.
2. Eseguire `python tools/photography/plan.py --out /tmp/pending.jsonl`.
3. Generare ciascun soggetto mancante con image_gen, controllare ingredienti, azione e ritaglio, poi integrare con prepare.py.
4. Eseguire share.py per i soli abbinamenti compatibili.
5. Eseguire verify.py e scripts/verify-aurora-boundary.py; aggiornare i conteggi del README.
6. Salvare ogni gruppo sul ramo GitHub prima di passare al successivo. Tutti i WebP approvati e i prompt sono nel repository.

Non dichiarare completo il catalogo intero. Lavorare su fotografie locali, identificabili nell'app come illustrazioni AI. I database nutrizionali e le correzioni funzionali precedenti non devono essere riscritti per aggiungere le immagini.
