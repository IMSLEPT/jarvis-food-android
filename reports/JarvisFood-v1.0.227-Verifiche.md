# Jarvis Food 1.0.227 - correzioni e verifiche

Data: 4 novembre 2026.

Applicazione originale aggiornata. Package com.jarvis.food; stessa firma originale.
Installare sopra la versione attuale senza disinstallarla.

## Correzioni applicate

- Banner compatti, gerarchia tipografica uniforme e calendari Oggi/Piano alla stessa altezza anche con nomi lunghi.
- Tipo giornata: icona 40 dp, titolo 16 sp e sottotitolo 13 sp.
- Pulsanti principali 50/52 dp; raggi, bordi, icone, ombre e padding coordinati.
- Macro da 12 sp, anelli live 32 dp e righe compatte sui display stretti con caratteri ingranditi.
- Schede pasto: foto 2:1 e titolo 20 sp; mangiato/annulla, alternative e codice a barre conservati.
- Navigazione con icone 22 dp ed elementi minimi 56 dp.
- Cucina: linguette distribuite, scorrimento sui display stretti, spazi ridotti tra sezioni.
- Profilo organizzato in tre gruppi con separatori e icone allineati.
- Componenti Material coordinati per impostazioni, campi e finestre.
- Comandi delle schermate secondarie sopra l'area dei gesti Android.
- Diagramma smartwatch: misura e resa del testo allineate, larghezze adattate alle parole, limiti delle righe verificati.
- Contrasto delle scritte bianche sulle azioni arancioni: 4,62-5,50:1.

Conservati navy, arancione, sfondi chiari, font Bricolage/DM Sans e immagini esistenti. Riferimenti ridimensionati per l'uso reale sul telefono.

## Test realmente eseguiti

- 164 test unitari, zero fallimenti nella compilazione finale.
- 6 esecuzioni Android sulla v227: 2 test a tre configurazioni, piu 2 sulla base v224. Tutte 8 passate. Un test percorre cinque sezioni e 13 destinazioni del Profilo; l'altro verifica i calendari con nomi lunghi.
- Android 15/API35, emulatore Google APIs pixel_7_pro: 1344x2992/densita480 con testo100% e115%; 945x2048/densita420 con testo130%. Larghezze logiche448 e360 dp, verticale.
- Limiti dei comandi rispetto ai gesti Android; limiti delle righe e parole intere nel diagramma smartwatch.
- Suite estesa: 44 esecuzioni Android passate sulla v226, prima dell'ultima modifica isolata al diagramma. Pasti, mangiato/annulla, alternative, barcode, dialoghi, dieta, ricerca, calendario, tastiera/chat e regressioni. Log distinto: https://github.com/IMSLEPT/jarvis-food-android/actions/runs/41919566657
- Firma originale apksigner confermata; APK release installato e avviato.
- Aggiornamento firmato v220 -> v226 -> v227: profilo, acqua, passi e chat della fixture conservati. Nessun dato del telefono dell'utente letto.
- 671 asset applicativi identici byte per byte, esclusi due profili di compilazione.
- Verifica sorgenti precedente: 187 file funzionali/dati originali invariati; 31 modifiche precedentemente autorizzate. Nessuna nuova modifica a persistenza o motori nutrizionali.
- Catalogo fotografie: 170 ricette complete, 821 mappature, 329 riusi compatibili; controllo superato.

## Immagini e valutazione

139 screenshot nel passaggio finale. PDF: 17 coppie v224/v227 con gli stessi dati e una coppia smartwatch v226/v227 al130%, 18 pagine.
Screenshot da build di test degli stessi sorgenti; installazione/avvio della release firmata verificati separatamente.
La fixture senza foto profilo usa l'icona; la foto salvata continua a essere usata quando presente.
Le immagini delle interazioni della v226 riguardano componenti invariati e sono in una cartella separata nello ZIP.

Valutazione visiva interna: 9.0/10. coerenza: 9/10, proporzioni: 9/10, leggibilita: 9/10, gerarchia: 9/10, fedelta: 9/10. Giudizio sui percorsi e sulle configurazioni documentate, non un test con utenti reali. Nessun problema bloccante o sovrapposizione rilevato nei percorsi verificati.

## Limiti

Nessuna prova su dispositivo fisico, altre versioni Android o integrazioni esterne reali AI, Health Connect e fotocamera/scanner. Sono intenzionali lo scorrimento delle linguette strette e l'abbreviazione dei suggerimenti di ricerca quando non entrano. I moduli sono scorrevoli.

L'ambiente locale si e disconnesso durante la consegna: il codice era gia salvato. File e confronti sono ricostruiti dalle prove della compilazione remota, controllando i digest degli asset pubblicati. Lo stato della verifica visiva finale e indicato sopra.

## Tracciabilita

Codice APK: 6aa999e8d7d4bfda4e03d56f982963b18bd6c710
CI finale: https://github.com/IMSLEPT/jarvis-food-android/actions/runs/41928404591
Release: https://github.com/IMSLEPT/jarvis-food-android/releases/tag/preview-v1.0.227-redesign
SHA256 APK: e012fa2f5f2c03d7f047f30d07ba18c509340cb59e25988804a46512d29b77e3
Certificato SHA256: a039be1f4e694272f35ac964d46d28185535ce28b2dcfb5bbbedda264b4d80ca
