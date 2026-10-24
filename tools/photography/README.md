# Fotografia delle ricette — migrazione in corso

Lo stile richiesto è fotografico: luce naturale, cucina luminosa, piatti curati
ma riproducibili, ingredienti riconoscibili e inquadrature ravvicinate delle azioni.
Gli screenshot dell'utente sono riferimenti di stile, non fotografie da ritagliare.
Il primo gruppo è prodotto con lo strumento integrato `image_gen`.

## Stato reale del catalogo

Il database contiene **2.533 ricette e 10.239 passaggi**. Questo primo gruppo
contiene **4 ricette complete, 20 abbinamenti immagine e 16 fotografie distinte**:
porridge banana e noci, pasta pollo e zucchine, pane ricotta e marmellata,
riso uova e spinaci. L'ultimo passaggio di impiattamento usa la foto del piatto.
**La sostituzione dell'intero catalogo non è ancora completata.**

Le immagini sono WebP locali: disponibili offline, senza chiavi API o richieste
di rete dal telefono. L'app le identifica come immagini illustrative create con AI.
Le fotografie caricate dall'utente mantengono la precedenza.

## Dati e ripristino

I 218 file originali protetti da hash, compresi ricette, ingredienti, grammature,
calcoli e foto precedenti, rimangono identici. `EditorialPhotos.kt` sceglie il nuovo
asset nelle viste condivise senza riscrivere il database nutrizionale.
Il ripristino si effettua con Revert della PR su GitHub, senza selettori nell'app.

## Produrre e verificare gli altri gruppi

1. Eseguire `python tools/photography/plan.py --out /tmp/pending.jsonl` per ottenere
   un inventario aggiornato e i brief specifici di tutte le immagini mancanti.
   Questo comando prepara i brief: **non genera né scarica fotografie**.
2. Produrre un'immagine per brief con lo strumento immagini integrato, poi
   controllare visivamente ingredienti, fase di preparazione e resa nel ritaglio.
3. Preparare un JSON di record `id`, `path`, `prompt` con le foto approvate.
   Per l'impiattamento si può usare `id`, `reuse` con l'ID del piatto finito.
4. Eseguire `python tools/photography/prepare.py /percorso/input.json`.
   Lo script converte in WebP, registra i prompt e gli hash in `approved.json` e
   rigenera l'indice Kotlin. Richiede Pillow e non chiama servizi AI.
5. Eseguire il controllo degli originali e i test della CI. I test impediscono
   che un nuovo piatto sia distribuito con un procedimento fotografico incompleto.

Le immagini non ancora prodotte conservano il comportamento originale. Non sono
abbinate automaticamente a piatti simili o a fotografie generiche.
