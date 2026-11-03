# Jarvis Food Images — test isolato

Base: release 1.0.205, commit `974ae5c83c34553ef3ffce35652386b04150949e`.

Sito pubblico: https://jarvis-food-images.justale-c.chatgpt.site
Catalogo: https://jarvis-food-images.justale-c.chatgpt.site/catalog-v1.json

- `applicationId` separato: `com.jarvis.food.imagestest`. APK debug, installabile accanto all'app originale. Non è un aggiornamento della versione principale.
- Non sono inclusi nuovi file immagine nell'APK. Tutti i file già inclusi e i dati delle ricette sono preservati byte per byte.
- Priorità invariata: foto dell'utente → risorsa locale risolta da EditorialPhotos → online soltanto in assenza di entrambe.
- JSON remoto aggiornato ogni 6 ore con copia atomica dell'ultimo catalogo valido. Fallimenti con attesa di un minuto prima di nuovi tentativi.
- Cache delle immagini su disco: 64 MiB, LRU. Cache bitmap remota: 16 MiB; cache bitmap già esistente: 32 MiB. Richieste simultanee: massimo 3. Prefetch: 4 ricette successive nell'elenco.
- SHA-256 verificato prima di utilizzare un'immagine. Solo HTTPS, stesso dominio del sito, dimensioni e contenuti limitati, nessun redirect automatico.
- Senza rete si usano i file in cache. Una ricetta mai scaricata mostra il segnaposto esistente.
- Gli URL dei file sono immutabili e includono un hash. Lo script del sito conserva i vecchi file durante gli aggiornamenti; nessuna promessa di conservazione se il sito viene cancellato o cambia dominio.

## Aggiornare l'archivio

Il sorgente di Sites include `tools/add-image.py`. La procedura aggiunge WebP 960 px e miniature 384 px, aggiorna il catalogo e conserva gli URL precedenti. La pubblicazione del sito rende visibili le aggiunte senza ricompilare Android. Il caricamento dal browser non è implementato: per il test si usa il sorgente e la ripubblicazione. Per migliaia di foto e upload frequenti, il passo successivo è un bucket R2 con gestione autenticata, senza migrare adesso alcuna immagine locale.

`tools/online_images/discover.py` raccoglie candidati Wikimedia con metadati di licenza, non seleziona né pubblica foto automaticamente. Occorre controllare gli ingredienti visibili e le condizioni della singola licenza. Per l'archivio completo privilegiare foto online autorizzate; generare soltanto le eccezioni.

## Test

Il workflow `online-images-test.yml` compila senza firma di produzione, confronta APK debug dalla stessa base, controlla risorse protette e avvia test su Android 15. Non crea release, non unisce branch e non pubblica aggiornamenti. Consultare gli esiti effettivi del workflow: la presenza di questo documento non dimostra che i test siano passati.

Nessuna migrazione delle immagini esistenti è autorizzata o eseguita.
