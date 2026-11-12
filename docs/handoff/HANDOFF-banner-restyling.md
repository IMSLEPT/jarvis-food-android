# Handoff — restyling banner blu (12 novembre 2026)

Progetto: Jarvis Food Android, branch `restyling-playstore`, cartella `/Users/alessandrocalamai/Desktop/jarvis-food-android`.
Stile: Apple, schermate chiare, banner blu navy con gradiente, arancione come colore primario. Nessun tema scuro, niente viola come colore di marca. Tutte le funzioni, i dati, i calcoli e le impostazioni restano invariati.

## Richiesta in corso (ultimi messaggi dell'utente)

1. Rifare la parte **chat di Jarvis** usando l'immagine allegata della mascotte che saluta (`docs/handoff/mascot-wave-source.png`, PNG con trasparenza già pronta). Il banner blu della chat deve avere la stessa altezza del banner di Oggi e la mascotte deve stare in alto a destra, un po' più grande, e più bella.
2. Rifare anche i **banner blu delle altre schede** (Piano, Cucina, Progressi) nello stesso stile di Oggi e della chat: scritte più curate e icone coerenti.
3. **La mascotte NON va reinserita in Cucina, Progressi e Piano.** Solo Oggi (pollice in su) e Chat (che saluta) hanno la mascotte.

## Cosa è stato fatto (tutto compila, nessun errore `^e:`)

- `app/src/main/res/drawable-nodpi/mascot_chat.webp`: sostituito con la nuova mascotte che saluta (580x640, ritagliata dal PNG dell'utente, margine 4 px). Sorgente in `docs/handoff/mascot-wave-source.png`.
- `ui/NavyBanner.kt` riscritto. Nuova firma: `NavyBanner(title, labels, selected, onSelect, query, onQuery, hint, icons, descriptions, accounts, onSubmit, onBack, labelTags, searchContent, actions, supportingText, count, tileIcon, subtitle, mascot)`.
  - Sono stati tolti i parametri `logo` e `logoSize`.
  - Con `mascot != null` (e non font enorme): layout stile Oggi, con riga di tessere a sinistra (back + profilo 42dp), titolo 28sp con `GlowIcon` arancione 32dp (`tileIcon`), sottotitolo 13sp color B9C8EC, mascotte a destra.
  - Senza mascotte: riga compatta con back, GlowIcon + titolo + sottotitolo e tessera profilo a destra.
  - L'area di ricerca sotto le linguette compare solo se c'è `searchContent`, `supportingText`, `hint` o `query`.
  - Nuovo `BannerBackTile` (tessera vetro 42dp).
- `ui/RefDesign.kt` (`RefBannerTabs`): le linguette ora stanno in un contenitore "vetro" (bianco 10% + bordo 15%, raggio 18dp), la voce attiva è una pillola arancione con alone (`orangeGlow`), niente più separatori verticali, testo inattivo DCE5F5.
- `ui/HeaderLayout.kt`: `HeaderProfileTile` ora `internal`. In `CalendarHeaderContent` c'è un nuovo ramo per `greeting == null && !large` (usato dalla scheda **Piano**): GlowIcon calendario + titolo "Piano" 28sp + data, poi tessera calendario e tessera profilo a destra, **senza mascotte**. Il ramo di Oggi (con mascotte) è invariato.
- `ui/KitchenScreen.kt`: `tileIcon = RIc.Fork`, `subtitle = "Ricette, dispensa e lista della spesa"` (niente più `logo`).
- `ui/ProgressScreen.kt`: `tileIcon = RIc.Bars`, `subtitle = "Dati, grafici e obiettivi"`.
- `ui/ChatScreen.kt`: `ChatHero()` eliminata. La `NavyBanner` della chat ora usa `tileIcon = RIc.ChatPlus`, `subtitle` (con hero: "Pasti, ricette, piano e spesa: scrivimi qui sotto."; altrimenti "Il tuo assistente alimentare") e `mascot = if (hero && !setup) { MascotChat(140.dp, Modifier.offset(x = 8.dp, y = 6.dp)) } else null`. Il banner compatto con tastiera aperta è rimasto com'era (usa ancora `JarvisHeadLogo`).

## Verificato sullo schermo (display virtuale scrcpy)

- **Cucina**: banner ok (icona forchetta arancione, titolo, sottotitolo, linguette in vetro con pillola arancione, ricerca bianca). Vedi `verified-cucina-progressi-banners.png`.
- **Progressi**: banner ok, stesso stile. Stessa immagine.
- **Oggi**: invariato.

## NON verificato (da fare subito)

- **Chat Jarvis** con la nuova mascotte che saluta: dimensione (140dp), posizione (offset), altezza del banner rispetto a Oggi (il banner di Oggi è circa 230dp con la barra di stato), leggibilità del sottotitolo a 3 righe accanto alla mascotte, anello arancione `MascotRing` dietro (tilt -10) che potrebbe non piacere con la nuova immagine.
- **Piano**: nuovo ramo compatto, da controllare (titolo, data, tessere calendario/profilo, striscia settimana sotto).
- Chat in stato attivo (con messaggi), con impostazioni "Modello" aperte, e con tastiera aperta.
- Font grande (`fontScale > 1.5`): `large` usa il layout compatto senza mascotte; controllare che non tagli niente.

## Rimasti da rivedere nell'app (richiesta precedente)

Schermate Profilo (già viste, coerenti, vedi `profile-screens-review.png`), questionario/onboarding (`Screen.Questionnaire`, `Onboarding.kt`, `Questionnaire.kt`), sotto-pagine Digiuno, grafici/dettaglio andamento, dialoghi, segnaposto foto dei pasti in Piano.

## Come verificare sul telefono (trucchi imparati)

- Build: `cd /Users/alessandrocalamai/Desktop/jarvis-food-android && JAVA_HOME=~/android-dev/jdk/Contents/Home ANDROID_HOME=~/android-dev/sdk ./gradlew assembleDebug -PdevSuffix -q 2>&1 | grep -E "^e:|FAILED"`
- Install: `/Users/alessandrocalamai/Desktop/scrcpy/adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Cattura sul display virtuale: `docs/handoff/cap3.sh` (copiare in `/tmp/cap3.sh`; usa `--start-app=+com.jarvis.food.dev`). Azioni: `t x y` tap, `s` swipe su, `b` indietro, `w sec` attesa. Frame a 1 fps in `/tmp/jf-shots/f/`. Display 1008x2244; barra tab a y≈2030; x delle tab: Oggi 116, Piano 310, Cucina 504, Progressi 698, Jarvis 890.
- **Problema noto**: a volte l'app salta sul display 0 (il telefono vero) e la cattura mostra solo lo sfondo del launcher. Rimedio: `pkill -f scrcpy; adb shell am force-stop com.jarvis.food.dev; sleep 6` e rilanciare; riprovare più volte; non toccare il telefono vero. Il tap sulla scheda Jarvis (x 890) è quello che più spesso coincide con lo spostamento: provare a catturare la chat per ultima, oppure dopo aver aperto Cucina.
- Se la chat resta introvabile: temporaneamente forzare `hero = true` in `ChatScreen.kt` (chiamata `ChatHeader(... hero = !active)`) e poi ripristinare `hero = !active`.
- Dopo le modifiche: `python3 scripts/verify-aurora-boundary.py`; se segnala file funzionali modificati su richiesta dell'utente, registrarli in `scripts/authorized-functional-changes.json`.
- Push: `GIT_SSH_COMMAND="ssh -i ~/.ssh/jarvis_food_deploy -o IdentitiesOnly=yes" git push -q git@github.com:IMSLEPT/jarvis-food-android.git restyling-playstore`. Trailer commit: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

## Stato git

Le modifiche sopra sono in un commit locale WIP sul branch `restyling-playstore`, **non ancora pushato** e non ancora installato sul telefono dell'utente con la chat verificata. Ultimo commit pushato: `08b5e32` (icona finale).

## Ancora aperti (invariati)

Foto ricette mancanti (scelta dell'utente in `docs/PHOTOS-REMOTE-PLAN.md`); blocchi Play Store: librerie native 16 KB (il telefono mostra il dialogo "Compatibilità delle app"), AAB, targetSdk 36, privacy policy, upload key. Icona dell'app sulla home del telefono non ancora controllata (può servire riavviare il launcher).

## Prompt suggerito per la nuova chat

"Leggi `docs/handoff/HANDOFF-banner-restyling.md` nel repo jarvis-food-android e prosegui da lì: verifica sullo schermo la chat Jarvis e la scheda Piano, correggi quello che non torna (mascotte solo in Oggi e Chat), poi controlla boundary, installa sul telefono, fai commit e push."
