# Produzione fotografica riprendibile, senza servizi a pagamento

## Cosa funziona adesso

`batch.py` automatizza inventario, gruppi completi, richieste, anteprime quadrate,
registrazione delle revisioni, conversione WebP, riuso compatibile, verifiche,
aggiornamento dei conteggi e punto di ripresa. Usa gli script esistenti senza
cambiare ricette, grammature, dati nutrizionali o distribuzione delle immagini.
Non chiama API, non acquista crediti e non avvia server, cron o GitHub Actions.

Il generatore `image_gen` incluso è stato usato con successo nella sessione.
È uno strumento della sessione assistita, non un endpoint Python disponibile
al repository. Non abbiamo verificato una disponibilità illimitata o l'accesso
da esecuzioni programmate. Una coda pronta non equivale a immagini prodotte.
Non aggirare quote o limiti creando account o chiamando endpoint non documentati.

Ogni nuova immagine deve essere vista da un revisore, anche assistente con visione,
nell'originale e nel ritaglio quadrato. Il software controlla che la revisione
esista e corrisponda all'hash del file; non può certificare da solo ingredienti
o qualità estetica. Nessuna approvazione automatica in ciclo.

## Un gruppo alla volta

Prima di iniziare: leggere README.md, PROGRESS.md e docs/FINAL-APK-HOLD.md;
fare fetch e conservare eventuali aggiornamenti remoti. Non iniziare un nuovo
gruppo prima del commit e push del precedente. Usare una directory di lavoro
persistente esterna al repository per originali e batch.json.

```bash
python tools/photography/batch.py stats
python tools/photography/batch.py plan --recipes 1 --out /tmp/jarvis-gruppo-001
python tools/photography/batch.py status /tmp/jarvis-gruppo-001
```

`requests.jsonl` contiene una richiesta per soggetto compatibile; `batch.json`
conserva ricette complete, contesto e stato di lavoro. `BRIEF.md` spiega il ciclo.
Per esportare l'intera coda ancora mancante, senza produrre immagini:

```bash
python tools/photography/plan.py --out /tmp/jarvis-inventario.jsonl
```

Il revisore adatta il prompt al soggetto e alla fase, usando solo gli ingredienti
previsti; chiama il generatore incluso e salva il prompt effettivamente usato in
un file UTF-8. Niente foto generiche, collage, guarnizioni estranee o piatto
finito usato per una lavorazione intermedia. Poi registra ogni candidato:

```bash
python tools/photography/batch.py candidate /tmp/jarvis-gruppo-001 ID /percorso/originale.png --prompt-file /percorso/prompt.txt
```

Visualizzare originale e `ID.square.jpg`. Solo dopo l'esame individuale:

```bash
python tools/photography/batch.py review /tmp/jarvis-gruppo-001 ID --decision approve --reviewer 'nome del revisore' --checks ingredients,stage,realism,style,squareCrop --notes 'Osservazioni concrete su ingredienti e azione visibili, difetti assenti e ritaglio.'
```

Se non va bene, registrare `--decision reject`, spiegare il difetto, rigenerare e
registrare il nuovo candidato: la precedente approvazione viene annullata.
Un file cambiato dopo la revisione viene bloccato dall'hash.

```bash
python tools/photography/batch.py integrate /tmp/jarvis-gruppo-001
```

L'integrazione rifiuta gruppi incompleti, immagini non approvate, file già
esistenti e cataloghi o manifest cambiati dopo la pianificazione. Esegue
prepare.py, share.py, verify.py e verify-aurora-boundary.py; confronta tutti
i record e file fotografici preesistenti, aggiorna PROGRESS.md e README.md.
Verifica anche decodifica/dimensioni dei nuovi WebP, assenza di file con contenuto
duplicato e corrispondenza esatta dell'indice Kotlin con il manifest.
Se una verifica fallisce ripristina manifest, indice, documentazione e rimuove
gli asset creati dal tentativo. Le revisioni dei gruppi riusciti vengono salvate
in `tools/photography/reviews/`, senza percorsi locali degli originali.
Ripetere integrate su un gruppo completato non aggiunge nuovamente immagini.
Usare un solo processo di integrazione alla volta; non modificare la raccolta
in parallelo. Il batch è uno stato di lavoro locale fidato, non un file da
eseguire se ricevuto da fonti non attendibili.

Controllare diff e conteggi, quindi aggiungere solo i nuovi WebP, il manifest,
l'indice, i documenti e il rapporto di revisione. Commit con `[skip ci]` e push
normale sul ramo `feat/recipe-editorial-photos`. Nessun force push e nessun APK.
I test unitari/Android dell'APK non sono implicitamente rieseguiti da questa pipeline.

Le fotografie online restano ammesse, con fonte, autore, licenza verificata e
coerenza visiva. Il batch accetta metadati e revisioni dei candidati reali, ma
per integrarli usare prepare.py e mantenere sincronizzati LICENSED-PHOTOS.md,
gli stessi crediti negli asset offline e approved.json. L'integrazione automatica
si ferma esplicitamente per queste foto finché i crediti non sono sincronizzati.

## Produzione locale sul Mac

L'interfaccia sul Mac non dà all'assistente accesso al computer: l'ambiente
verificato della sessione è Linux x86_64, senza GPU NVIDIA/Torch/Diffusers.
Per conoscere il Mac effettivo, eseguire **sul Mac**, dal checkout del repository:

```bash
python3 tools/photography/doctor.py
```

Il comando legge sistema, chip e RAM; non stampa seriali o credenziali, non
installa nulla e non contatta servizi. Sono sufficienti questi dati per scegliere
un modello locale compatibile. Un candidato è ComfyUI con un modello a pesi
scaricabili e licenza che consenta gli usi richiesti; modello, versione e licenza
devono essere scelti dopo aver verificato l'hardware, senza API cloud a pagamento.
Gratuità dei pesi non significa automaticamente licenza commerciale né stessa qualità.

Prima di automatizzare migliaia di generazioni: prova di almeno tre ricette,
comprendendo un piatto finito e azioni intermedie difficili. Valutare fedeltà,
mani/utensili, cottura, stile, ritaglio, tempi effettivi e scarti rispetto alle
immagini già approvate. Nessuna qualità equivalente dichiarata senza questa prova.
Un computer locale deve rimanere acceso; consumi elettrici e tempo di calcolo
non sono gratuiti. Non acquistare hardware o abbonamenti per questo flusso.
Finché la prova non passa, usare il generatore incluso entro i suoi limiti.

Nessun worker locale, modello o pianificazione automatica è stato attivato da
questi script. L'avvio locale richiede un'esecuzione nell'ambiente del Mac;
la produzione autonoma completa non è verificata nell'ambiente attuale.

## Prova di esecuzione cloud — 25 ottobre 2026

Su richiesta dell'utente di controllare il lavoro da smartphone con il PC spento,
è stata creata tramite lo strumento delle attività programmate un'attività cloud
abilitata, denominata **Foto Jarvis Food**. Cadenza: un tentativo ogni ora;
prima finestra impostata dalle 17:15 del 25 ottobre, fuso Europe/Rome. La
pianificazione è flessibile: la creazione non conferma l'orario esatto né il
buon esito di un'esecuzione. Non è stato installato un agente sul Mac/Pixel e
non è stato aggiunto alcun workflow GitHub Actions.

Il prompt limita ogni esecuzione a un gruppo completo: legge lo stato attuale,
usa solo strumenti inclusi o fonti gratuite con licenza verificata, richiede
revisione visiva originale/ritaglio, esegue gli script e salva con `[skip ci]`.
Sono vietati API/runner a pagamento, acquisti, APK ripetuti e modifiche a ricette
o funzionalità. Non deve partire un secondo gruppo mentre uno è in corso.

Se mancano generazione, visione, esecuzione degli script o scrittura GitHub e
le fonti gratuite non consentono di completare fedelmente un gruppo, l'attività
deve riportare il blocco e disattivarsi. Deve disattivarsi anche a catalogo finito.
L'utente può richiederne sospensione e ripresa dalla conversazione.

Verificato l'accesso in lettura al repository mediante il connettore GitHub
(permesso push presente). Creazione, abilitazione e modifica del calendario
confermate dagli strumenti. **Produzione fotografica programmata non ancora
verificata:** al momento della configurazione non risultano esecuzioni o output
registrati. Il comando di avvio immediato ha restituito `404 Action not found`
prima dell'invocazione; non è una prova avviata o completata. Verificare il primo
risultato registrato e il commit effettivo prima di dichiarare la produzione
automatica funzionante. Non inferire avanzamento dalla sola attività abilitata.

## Esecuzione del gruppo lamponi — 25 ottobre 2026

Il prompt di prosecuzione è stato eseguito su un checkout aggiornato al commit `51ec955`. Il generatore incluso è stato effettivamente chiamato per risciacquo e consumo dei lamponi; originale e ritaglio di tutte e tre le immagini sono stati esaminati. Conversione, abbinamenti, verifica fotografica e controllo degli originali superati. Fonte Pixabay, autore e licenza sincronizzati anche offline. Questa prova riguarda un solo gruppo e non garantisce disponibilità o qualità dei gruppi futuri; la pubblicazione va verificata dal commit effettivo sul ramo. Nessun agente open source autonomo installato su GitHub.
