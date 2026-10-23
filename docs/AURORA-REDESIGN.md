# Aurora: grafica approvata e ripristino da GitHub

Base originale: `549d63739ee25cc7e8a9784d627b04828f51fde7`.
Ramo di backup: `backup/aurora-pre-redesign-2026-10-23`.

## Ripristino solo da GitHub

Nell’app non c’è un selettore di grafica. Il ripristino avviene nel repository.
Il redesign viene integrato con un’unica pull request e squash merge.
Per annullarlo: apri la pull request integrata, premi **Revert**, crea e integra
la pull request di ripristino. La pipeline genererà un nuovo APK con la grafica
precedente e un numero di versione superiore, installabile come aggiornamento.
Non serve installare un APK vecchio né disinstallare l’app.

Il ramo di backup conserva l’intero progetto prima del redesign e si può
consultare dal selettore dei rami nella pagina del repository.
La pull request del redesign è [#1](https://github.com/IMSLEPT/jarvis-food-android/pull/1).
Il suo Revert ripristina insieme interfaccia, test e configurazione di build,
così l’APK della grafica precedente viene ricompilato correttamente.

## Modifiche

- Interfaccia Android nativa, senza WebView e senza dati dimostrativi del prototipo.
- Cinque destinazioni etichettate; profilo e famiglia nell’intestazione.
- Home con riepilogo compatto, macro colorati e diario prima delle proposte.
- Piano compatto con riepilogo espandibile e strumenti nel menu calendario;
  ricetta completa, alternative, registrazione pasti e spesa originali.
- 55 categorie originali, emoji e palette originali in sette gruppi con vista
  compatta o completa e filtri nativi.
- Componenti e gerarchia visiva condivisi dalle schermate secondarie.
- Testo bianco sui pulsanti arancioni.
- Prototipo approvato separato e invariato.

`scripts/functional-source-manifest.json` e `verify-aurora-boundary.py`
verificano 218 file funzionali/dati originali senza modifiche.

## Verifica e pubblicazione

La pipeline compila release/debug/test Android, esegue i test unitari e i test
Compose in emulatore, quindi acquisisce le schermate native. I crash bloccano
la release. I rami di lavoro producono artifact; soltanto main pubblica l’APK.

## Seconda revisione: Navy equilibrato (A)

Il saluto passa da 9 a 16 sp, in navy e senza tracking espanso. Home con sfondo
più chiaro, riepilogo navy più compatto, macro su righe separate e suggerimento
profilo bianco. La barra inferiore ha etichette da 11 sp, aree di tocco da almeno
48 dp, ruolo Tab e stato selezionato, con un solo spazio per i comandi Android.

Pulsanti principali e secondari crescono con il testo; titoli secondari più
equilibrati; ricette senza sovrapposizione della barra azioni; categorie a due
colonne con caratteri ingranditi. I colori originali restano negli accenti, mentre
l'arancione dei pulsanti con testo bianco usa #C94D00–#BA4000 (contrasto minimo
4,62:1). Testo ambra più scuro sui fondi gialli.

Questa revisione è separata dalla prima: per tornare alla grafica della 1.0.68
usa Revert della pull request della seconda revisione. Per tornare alla grafica
precedente a tutto il redesign, annulla prima questa revisione e poi la PR #1.
La release v1.0.68 e il ramo backup originale rimangono disponibili su GitHub.

La pipeline include Android 10 per la regressione esistente e Android 15 per
barre di sistema, sovrapposizioni, testi lunghi e contrasti, a dimensioni normali
e su schermo da 360 dp con caratteri al 130%.
