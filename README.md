# Jarvis Food

App Android personale per il diario alimentare (Kotlin + Jetpack Compose), ispirata nelle funzioni a YAZIO, con grafica propria.

- Diario pasti con calorie, macro, fibre, zuccheri, sale
- Database base offline + Open Food Facts + scanner barcode
- AI foto: offline (TFLite on-device) e online (API compatibile OpenAI, predefinito OpenRouter modelli gratuiti)
- Digiuno intermittente con fasi, notifiche e storico
- Acqua, passi, attività (MET), peso, misure, grafici
- Ricettario offline, editor personale, alimenti personali, preferiti
- Promemoria locali, backup JSON, export CSV

L'APK viene compilato da GitHub Actions e pubblicato nelle Release.
I colori dell'interfaccia sono tutti in `app/src/main/java/com/jarvis/food/ui/Theme.kt`.

## Grafica Aurora

Redesign nativo con logica e dati originali. Il ripristino avviene soltanto
da GitHub: Revert della pull request del redesign oppure ramo
`backup/aurora-pre-redesign-2026-10-23`. Nessun selettore grafico nell’app.

- [Modifiche e ripristino](docs/AURORA-REDESIGN.md)
- [Verifiche e limiti del debug](docs/DEBUG-AURORA.md)

Test unitari e controlli UI in emulatore precedono la pubblicazione dell’APK.
La firma resta la stessa: installare l’APK come aggiornamento, senza disinstallare.
