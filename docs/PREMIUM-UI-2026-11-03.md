# Jarvis Food: perfezionamento grafico approvato

Base originale: release 1.0.205, commit `974ae5c83c34553ef3ffce35652386b04150949e`.
Alessandro ha approvato il progetto delle cinque schermate il 3 novembre 2026 e ne ha chiesto l'applicazione all'app originale.

- Conservare `com.jarvis.food`, compatibilità dei dati e firma originale.
- Conservare fotografie, icona, illustrazioni, ricette, database e motori esistenti. Nessuna migrazione fotografica e nessuna importazione dell'archivio sperimentale in questa modifica.
- Telaio delle cinque intestazioni allineato, padding del corpo 16 dp, palette navy/arancione e caratteri Bricolage/DM Sans originali.
- Un solo input nella chat; Nuova chat, Modello e Cronologia restano azioni, con i callback esistenti.
- Titolo della ricetta a tutta larghezza, stato separato, foto 1,75:1, azioni e codice a barre conservati.
- Dati dei Progressi invariati: migliorano etichette, copertura e distinzione visiva dei giorni senza registrazioni.
- Build e prove tramite GitHub Actions sul ramo dedicato; questo ramo non pubblica automaticamente release e non modifica main.

Le verifiche finali e i limiti effettivi sono riportati nel rapporto di consegna. Gli screenshot del concept non sono screenshot dell'APK.
