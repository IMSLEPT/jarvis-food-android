# Fotografia delle ricette — migrazione in corso

**Compilazione e consegna autorizzate dall’utente dopo le modifiche richieste:**
vedi `docs/FINAL-APK-HOLD.md` per le decisioni aggiornate. La copertura fotografica
dichiarata deve sempre corrispondere ai file effettivamente approvati.

Lo stile richiesto è fotografico: luce naturale, cucina luminosa, piatti curati
ma riproducibili, ingredienti riconoscibili e inquadrature ravvicinate delle azioni.
Gli screenshot dell'utente sono riferimenti di stile, non fotografie da ritagliare.
Il primo gruppo è prodotto con lo strumento integrato `image_gen`.

## Stato reale del catalogo

Il database contiene **2.533 ricette e 10.239 passaggi**. La raccolta attuale
contiene **136 ricette complete, 630 abbinamenti immagine e 342 fotografie distinte**.
Tutte le 49 ricette che avevano immagini nel catalogo originale sono complete.
La raccolta attuale copre 402 passaggi fotografici di 136 ricette.
Le fotografie di azioni compatibili sono condivise anche con altre ricette.
Restano 2397 ricette da completare. L’impiattamento può riutilizzare la foto del piatto finito.
**La sostituzione dell'intero catalogo non è ancora completata.**

Le immagini sono WebP locali: disponibili offline, senza chiavi API o richieste
di rete dal telefono. L'app distingue le illustrazioni AI dalle fotografie reali con credito all'autore.
Le fotografie caricate dall'utente mantengono la precedenza.

## Dati e ripristino

I database di ricette, ingredienti, grammature e fotografie precedenti rimangono
identici. Dei 218 file originali protetti da hash, 217 sono invariati; FoodAi.kt
ha le correzioni richieste dall’utente per Foto AI, registrate separatamente
in scripts/authorized-functional-changes.json senza alterare la baseline originale. `EditorialPhotos.kt` sceglie il nuovo
asset nelle viste condivise senza riscrivere il database nutrizionale.
Il ripristino si effettua con Revert della PR su GitHub, senza selettori nell'app.

## Produrre e verificare gli altri gruppi

1. Eseguire `python tools/photography/plan.py --out /tmp/pending.jsonl` per ottenere
   un inventario aggiornato e i brief specifici di tutte le immagini mancanti.
   Questo comando prepara i brief: **non genera né scarica fotografie**.
   La scelta dell'utente è condividere immagini fra varianti compatibili.
   `shared.py` raggruppa azioni identiche e alimenti dall'aspetto equivalente;
   salse, frutta, verdure, forme di presentazione e metodi diversi restano separati.
   `--legacy-only` dà priorità alle 49 ricette che avevano già foto.
2. Produrre un'immagine per brief con lo strumento immagini integrato, poi
   controllare visivamente ingredienti, fase di preparazione e resa nel ritaglio.
3. Preparare un JSON di record `id`, `path`, `prompt` con le foto approvate.
   Per l'impiattamento si può usare `id`, `reuse` con l'ID del piatto finito.
4. Eseguire `python tools/photography/prepare.py /percorso/input.json`.
   Lo script converte in WebP, registra i prompt e gli hash in `approved.json` e
   rigenera l'indice Kotlin. Richiede Pillow e non chiama servizi AI.
5. Eseguire `python tools/photography/share.py` per condividere automaticamente
   solo le immagini compatibili. Le varianti con passaggi mancanti non ricevono
   la foto principale finché la procedura non è completa.
6. Eseguire `python tools/photography/verify.py`, il controllo degli originali e
   i test della CI. Il controllo ricalcola la compatibilità delle immagini condivise.
   I test impediscono
   che un nuovo piatto sia distribuito con un procedimento fotografico incompleto.

Le immagini non ancora prodotte conservano il comportamento originale. Non sono
abbinate automaticamente a piatti simili o a fotografie generiche.

## Foto online gratuite

Autorizzate da Alessandro il 24 ottobre 2026, purché gratuite, di alta qualità e coerenti con ingredienti e fase della ricetta. La licenza deve consentirne l’uso nell’app. Registrare fonte, autore, licenza, verifica e conversioni in `approved.json`; non qualificare queste fotografie come generate con AI. Il riuso compatibile deve conservare i metadati originali. I crediti delle foto importate sono anche in `LICENSED-PHOTOS.md`.
