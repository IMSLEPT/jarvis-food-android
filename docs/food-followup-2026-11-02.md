# Feedback video 1000196317 e 1000196318

Base: APK 1.0.185 / 82e2a768. I due video sono una registrazione continua di circa 6 minuti e mezzo.

| Richiesta | Implementazione |
| --- | --- |
| Le schede di Cosa mangio adesso passano sopra i comandi | Spazio per navigazione Android, clipping dello scorrimento e testate opache con priorità di disegno |
| Consiglio per ogni alimento con colore, dosi e frequenza | Scheda etichetta con zuccheri/saturi/sale noti, indicatore più testo, porzioni di riferimento, giorni registrati, alternative e fonti |
| Alternative anche di marca | Confronto Open Food Facts su richiesta con valori per la stessa unità; nessuna marca viene dichiarata migliore senza confronto, né viene dedotta la lavorazione dal nome |
| Consigli subito sotto vitamine e minerali | Guida espandibile indipendente dallo storico, separata dalla valutazione dei dati registrati |
| Cercare una ricetta fuori dalle alternative automatiche | Ricerca per nome e ingredienti in tutti i tipi di pasto; restano i vincoli di dieta ed esclusioni; la ricerca esplicita include anche tempi lunghi |
| Inserire alimenti propri nel pasto pianificato | Compositore con catalogo, preferiti, alimenti personalizzati e prodotti online; ricette personali importabili per porzione |
| Aiuto a raggiungere calorie e macro | Confronto pasto/giornata e proposta delle quantità limitata tra 50% e 200%, da accettare prima di salvare |

Le quantità manuali sono memorizzate nel singolo slot con serializzazione compatibile con i vecchi dati. Il calcolo del piano conserva i grammi scelti. Salvare il piano non registra il pasto come mangiato. Gli alimenti manuali partecipano anche alla lista della spesa.

I colori descrivono specifici nutrienti dell'etichetta secondo i criteri NHS, non un voto generale all'alimento. Gli zuccheri totali restano distinti da quelli aggiunti/liberi. Il consiglio per una crema 100% frutta a guscio evita di inventare un divieto giornaliero o un limite settimanale; tostare e macinare non bastano a classificarla come alimento poco salutare. Gli indicatori mancanti restano sconosciuti. I consigli generali non diagnosticano carenze e non affermano che un solo alimento copra B12 o vitamina D.

Fonti consultate il 02/11/2026: NHS Food labels; CREA Linee guida per una sana alimentazione, revisione 2018; riferimenti già presenti in NutritionEvidence. Collegamenti disponibili nell'app.

Verifiche aggiunte: dodici casi JVM per soglie, dati mancanti, crema di mandorle, storico, persistenza e quantità; quattro flussi Android eseguiti a testo normale e 130% per consigli alimento, guida senza storico, modifica/salvataggio manuale e margine dei suggerimenti rispetto ai comandi Android. Restano tutti i controlli della versione precedente. La pubblicazione resta vincolata al successo dell'intera pipeline.

Corretto il bilancio residuo della giornata: lo scarto di un pasto manuale viene ridistribuito sui successivi senza ricontare la quota già assegnata. Caso riproducibile: obiettivo 1.200 kcal, primo pasto 200 kcal invece di 400, due pasti adattabili da 500 kcal: totale 1.200, anziché 1.300.
