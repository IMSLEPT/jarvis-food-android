# Jarvis Food 1.0.93 — pulsanti Salva

Jarvis Food 1.0.93 corregge i pulsanti Salva sovrapposti ai tasti di navigazione Android nelle schermate Obiettivi, Preferenze della dieta e Profilo e obiettivi (Salva nome). Il componente condiviso riserva lo spazio effettivo della barra di sistema e conserva la gestione della tastiera. Grafica e comportamento dei salvataggi restano invariati.

Controlli dedicati su sette schermate: obiettivi, dieta, modifica profilo, nuovo alimento, editor ricette, impostazioni AI e aggiunta attività. Configurazioni: Android 15 con tre tasti, tre tasti e testo ingrandito, gesti; Android 10 con tre tasti. I controlli verificano posizione e intera area del pulsante, leggibilità e separazione dal contenuto.

Snapshot del commit ca86f55a86dce687612af35940bdee84f149b3af. L’APK contiene le stesse 435 immagini e 763 abbinamenti della 1.0.90, con 158 ricette complete e modello Foto AI offline invariato. Il gruppo fotografico successivo del salmone è salvato separatamente nel repository (d0af543, totale 159 ricette complete/439 immagini/767 abbinamenti); non viene dichiarato incluso in questa build.

Installare come aggiornamento senza disinstallare la versione precedente.

Verifiche effettivamente superate: **70 test unitari e 15 esecuzioni di test strumentati Android**. Prodotti 141 screenshot; 21 sono dedicati ai sette footer su Android 15. Il test geometrico degli stessi sette footer è superato anche su Android 10; gli screenshot dedicati di quel test sono stati rimossi dal successivo ScreenshotTest, che ha prodotto e conservato le normali schermate del profilo, obiettivi e dieta. Verificati visivamente i tre Salva anche in queste immagini Android 10. Nessuna ulteriore sovrapposizione nei footer esaminati.

Firma crittografica V2 verificata; certificato SHA-256 `a039be1f4e694272f35ac964d46d28185535ce28b2dcfb5bbbedda264b4d80ca`, identico alle versioni 1.0.89/1.0.90.

Dimensione: 211897197 byte. SHA-256 APK: `3591fa99251b0735df6ac66b8a0de691550b974df1e7fdab8c67816b425c257e`.

Build e prove: https://github.com/IMSLEPT/jarvis-food-android/actions/runs/40220576079

[Scarica APK 1.0.93](https://github.com/IMSLEPT/jarvis-food-android/releases/download/v1.0.93/JarvisFood-v1.0.93.apk). [Rapporto verifiche](APK-1.0.93-verification.json).
