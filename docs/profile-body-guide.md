# Schede profilo e guida misure corporee

Palette invariata: blu #09194D, arancione #EA7138, fondi chiari #F6F5F1, #ECF2FE e #FFF2EA. Componenti dedicati alle impostazioni con intestazioni tonali, sezioni, opzioni selezionabili e controlli con etichette accessibili. Obiettivi generali distinti da quelli ON/OFF; nessun cambiamento alle formule nutrizionali. Sostituzione alimento conserva ricerca, barcode/manuale e foto AI, con anteprima e conferma atomica preesistenti.

Il modello è una vera mesh 3D procedurale WebGL offline, ruotabile e selezionabile tramite lettura del buffer colore della superficie. Fascia tridimensionale intorno alla sede selezionata. Nessuna foto personale, rete o nuova dipendenza. WebView confinata al documento inline locale, senza accesso file/contenuti o navigazione esterna. In caso di WebGL indisponibile viene dichiarato e restano utilizzabili i pulsanti e la guida testuale.

Il manichino è un riferimento illustrativo: non identifica i punti ossei individuali né misura il corpo. La guida testuale indica i riferimenti da trovare sulla persona. Vita e fianchi seguono WHO STEPS, braccio destro il punto medio descritto da PhenX. Petto, coscia e collo sono convenzioni pratiche esplicite dell'app per ripetere il controllo nelle stesse condizioni. Massa grassa usa percentuale da valutazione separata e non mostra una fascia metrica.

Fonti primarie verificate il 27 ottobre 2026:
- WHO STEPS, Part 3 Section 5, pp. 10–13: https://cdn.who.int/media/docs/default-source/ncds/ncd-surveillance/steps/part3-section5.pdf
- PhenX Mid-Upper Arm Circumference: https://www.phenxtoolkit.org/protocols/view/21102

Storico compatibile con il formato `Measurement(date,type,value)` già presente nei backup. Salvare una misura aggiorna soltanto la stessa data/tipologia; tutte le altre sono conservate. Data modificabile, grafico, variazione e lista cronologica; eliminazione con conferma. Controlli numerici scartano valori non finiti, negativi e fuori dai limiti di inserimento dell'app (non soglie diagnostiche).

Verifica prevista: test JVM di storico/validazione/backup; test Android di rendering WebGL effettivo, selezione nativa e tocco sulla mesh, rotazione, registrazione e aggiornamento storico, modifica degli obiettivi e conservazione dei controlli. Esecuzione Android 15 con caratteri normali e 1,3× e smoke test 3D Android 10, oltre alla regressione esistente.
