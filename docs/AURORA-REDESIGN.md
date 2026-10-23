# Aurora: grafica approvata e ripristino

Base originale: `549d63739ee25cc7e8a9784d627b04828f51fde7`.
Ramo di ripristino: `backup/aurora-pre-redesign-2026-10-23`.

## Tornare alla grafica precedente

Apri il profilo (avatar in alto a destra), **Aspetto → Aurora precedente → Fatto**.
Per riattivare la nuova grafica: **Profilo → Aspetto → Aurora → Fatto**.
La scelta persiste agli avvii successivi. Non modifica diario, piano, ricette,
preferenze alimentari, backup, collegamento famiglia o smartwatch.
Non serve disinstallare l’app e non occorre installare un APK più vecchio.

## Confine della modifica

- Interfaccia Android nativa: nessuna WebView e nessun dato dimostrativo del prototipo.
- Cinque destinazioni etichettate, profilo e famiglia nell’intestazione.
- Home con riepilogo compatto, macro colorati e diario prima delle proposte.
- Piano con schede compatte: ricetta completa, alternative e registrazione originali.
- Ricette: 55 categorie originali, emoji e palette originali in sette gruppi,
  con vista compatta / completa e filtri nativi.
- Componenti e gerarchia visiva condivisi dalle schermate secondarie.
- Testo bianco sui pulsanti arancioni.
- Il prototipo approvato resta separato e invariato.

La modalità precedente è una fotografia del solo livello UI nel package
`classicui`. I componenti e il router sono isolati, ma Store, database,
calcoli, rete, AI, sync famiglia, notifiche e Health Connect sono condivisi.
Le sole modifiche alla fotografia sono il namespace e l’accesso ad Aspetto.
`scripts/classic-ui-manifest.json` registra i digest SHA-256 dei sorgenti originali.
Non mantenere due versioni dei motori o dell’archivio dati.

## Verifiche prima della release

La pipeline compila release/debug/test Android ed esegue tutti i test unitari.
Prima di pubblicare l’APK esegue in emulatore i test di regressione della
navigazione, delle categorie e del ripristino con conservazione dati,
poi la suite di screenshot delle schermate native. I crash bloccano la release.
Un APK di un ramo di lavoro è disponibile negli artifact, senza pubblicazione
come release. I servizi esterni con credenziali o hardware reale richiedono
anche una verifica sul dispositivo con i collegamenti dell’utente.
