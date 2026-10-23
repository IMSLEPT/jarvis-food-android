# Verifica del redesign Aurora — 23 ottobre 2026

Base: `549d63739ee25cc7e8a9784d627b04828f51fde7`.
Prototipo approvato invariato: 232 file confrontati con SHA-256.

## Test

58 test JVM del progetto: 15 motore/pianificazione, 13 famiglia, 1 sincronizzazione
famiglia reale, 9 protocollo chat, 7 adattamento pratico, 6 categorie, 4 barcode,
3 importazione ricette. Nelle esecuzioni verificate: zero errori e zero test saltati.
Il test di rete ha verificato dati cifrati, somma delle quantità e convergenza
tra due client dopo modifiche simultanee, con dati di prova.

Tre test Compose della nuova interfaccia:

1. Navigazione principale, profilo e famiglia raggiungibili.
2. Categorie originali (55 + catalogo completo), filtro Vegana e risultati nativi.
3. Piano compatto: registra e annulla un pasto, apre alternative e ricetta,
   genera la lista della spesa.

La suite di screenshot copre 35 viste/stati con 88 acquisizioni, incluse le
posizioni di scorrimento. Comprende Home, piano, ricette base e personali,
editor, alternative, dispensa, spesa, progressi, micronutrienti, profilo,
smartwatch, digiuno, preferenze, ricerca, chat, obiettivi, questionario e famiglia.
Non è una prova esaustiva di ogni combinazione di input e permessi.

Correzioni emerse: compilazione della tessera ricetta personale; riepilogo Piano
troppo grande; etichetta macro tagliata; intestazioni durante lo scorrimento.

## Conservazione e ripristino

218 file funzionali/dati originali verificati con SHA-256 e invariati. Nessuna
modifica a modelli dati, database, asset, motori, AI, rete, notifiche, Health
Connect, firma, application ID e versioni delle dipendenze. Stessa firma della
release precedente, verificata con apksigner sui pacchetti prodotti.

Ripristino da GitHub tramite Revert della pull request o ramo di backup:
`backup/aurora-pre-redesign-2026-10-23`. Nell’app non c’è alcun selettore grafico.
Vedi [istruzioni](AURORA-REDESIGN.md).

## Limiti

Build e prove strumentate eseguite in GitHub Actions, non sul telefono dell’utente.
Amazfit/Health Connect, permessi del dispositivo, fotocamera, notifiche effettive
e AI cloud con chiavi personali richiedono anche una prova sul telefono.
Il protocollo chat è testato; il LLM da scaricare non viene eseguito dalla pipeline.
Classificatore alimentare TFLite e 159 asset fotografici originali inclusi nell’APK.
