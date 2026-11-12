# Jarvis Food — preparazione Play Store

Stato verificato su ramo `restyling-playstore` (base: build 1.0.233, tag `backup-1.0.233-source`).

## Bloccanti
1. **Chiave di firma**: `app/build.gradle.kts` contiene la password del keystore in chiaro e il repository è pubblico. Va considerata compromessa: creare una *upload key* nuova per Play App Signing e togliere la password dal repo (usare variabili d'ambiente / secret CI).
2. **Formato**: Play richiede un AAB (`./gradlew bundleRelease`), non l'APK. L'APK attuale pesa ~223 MB (include 3 ABI e modelli): l'AAB lo divide per dispositivo e rientra nei limiti.
3. **targetSdk**: oggi 35. Per nuove app Play richiede il livello più recente (36): alzare `targetSdk` e ripassare le schermate su Android 16.
4. **Informativa privacy** pubblica (URL) + modulo "Sicurezza dei dati": l'app tratta dati salute (peso, passi, frequenza cardiaca da Health Connect).
5. **Health Connect**: compilare la dichiarazione dei permessi; `READ_HEALTH_DATA_IN_BACKGROUND` richiede una giustificazione specifica (o rimuoverlo).
6. **SCHEDULE_EXACT_ALARM**: limitato su Android 14+ per app non-sveglia; passare ad allarmi inesatti per i promemoria o giustificarlo.

## Scheda store
- Screenshot telefono (1008×2244 va bene), icona 512×512, immagine in evidenza 1024×500.
- Categoria: Salute e fitness. Testi breve/lungo in italiano.
- Classificazione contenuti e dichiarazione "non è un dispositivo medico / non fornisce diagnosi".

## Già fatto in questo ramo
- Chat: risposte istantanee senza modello per comandi chiari, header compatto con tastiera, precaricamento modello, modello E2B di default (`QuickIntents.kt`, test inclusi).
- Build di prova locale installabile accanto all'app: `./gradlew assembleDebug -PdevSuffix` (id `com.jarvis.food.dev`).

## Blocchi ancora aperti (aggiunti dopo il restyling)
- **16 KB page size**: le librerie native dell'app non sono allineate a 16 KB; dal 1 novembre 2025 Google Play lo richiede per i nuovi upload con targetSdk 35+. Il telefono lo segnala all'avvio della build di debug; librerie coinvolte: libmlkit_google_ocr_pipeline.so, liblitertlm_jni.so, libtask_vision_jni.so, libandroidx.graphics.path.so. Da risolvere aggiornando le dipendenze (ML Kit OCR, LiteRT-LM, Tasks Vision, AndroidX graphics-path) a versioni allineate prima della pubblicazione.
- **Foto ricette**: solo 170 ricette su 2.533 hanno foto complete (restano 2.363); le altre mostrano un segnaposto. Vedi tools/photography/README.md.
