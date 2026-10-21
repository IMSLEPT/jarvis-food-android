# Jarvis Food

App Android personale per il diario alimentare (Kotlin + Jetpack Compose), ispirata nelle funzioni a YAZIO, con grafica propria.

- Diario pasti con calorie, macro, fibre, zuccheri, sale
- Database base offline + Open Food Facts + scanner barcode
- AI foto: offline (TFLite on-device) e online (API compatibile OpenAI, predefinito OpenRouter modelli gratuiti)
- Digiuno intermittente con fasi, notifiche e storico
- Acqua, passi, attività (MET), peso, misure, grafici
- Ricette (11 incluse + editor), alimenti personali, preferiti
- Promemoria locali, backup JSON, export CSV

L'APK viene compilato da GitHub Actions e pubblicato nelle Release.
I colori dell'interfaccia sono tutti in `app/src/main/java/com/jarvis/food/ui/Theme.kt`.
