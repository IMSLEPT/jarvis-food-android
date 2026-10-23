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

Il ramo di backup conserva l’intero progetto prima del redesign. Per recuperare
solo i sorgenti della grafica da terminale:

```sh
git switch -c restore/aurora-previous main
git restore --source=backup/aurora-pre-redesign-2026-10-23 -- app/src/main/java/com/jarvis/food/ui
# Poi commit, push e pull request verso main.
```

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
