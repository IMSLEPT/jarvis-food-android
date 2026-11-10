# JARVIS Food — spot motion graphics (Remotion)

Spot verticale di 30 secondi, 1080 × 1920, 60 fps, con musica ed effetti sonori originali.

## Comandi

```bash
npm install
npm run dev                 # anteprima in Remotion Studio
npm run render              # out/jarvis-food-spot.mp4 (finale neutro)
npm run render:googleplay   # out/jarvis-food-spot-googleplay.mp4 (variante con badge Google Play)
npm run audio               # rigenera musica ed effetti (Python 3 + numpy + scipy)
node tools/stills.mjs 0,120,480 nome   # provino di fotogrammi in /tmp/stills
```

Se Remotion non riesce a scaricare il proprio browser: `CHROME_BIN=/percorso/di/chrome npm run render`.

## Due composizioni

| ID | Finale | Quando usarla |
| --- | --- | --- |
| `JarvisFoodSpot` | Logo + «La tua nutrizione intelligente» | Sempre: non dichiara disponibilità sugli store |
| `JarvisFoodSpot-GooglePlay` | «Scarica ora Jarvis Food» + badge Google Play | Solo quando l'app è effettivamente pubblicata su Google Play |

## Struttura

| Percorso | Contenuto |
| --- | --- |
| `src/theme.ts` | Colori, griglia ritmica (120 BPM: 1 beat = 30 frame), inizio delle scene |
| `src/anim.ts` | Curve, keyframe, molle, formati numerici italiani |
| `src/Spot.tsx` | Regia generale: fondo, scene, titoli, audio |
| `src/scenes/SceneLogo.tsx` | 0–4 s: nascita del logo |
| `src/scenes/PhoneJourney.tsx` | 4–24 s: il telefono attraversa Oggi, Piano, Cucina, Progressi (funzione `phonePose`) |
| `src/scenes/SceneJarvis.tsx` | 24–28 s: assistente Jarvis |
| `src/scenes/SceneFinale.tsx` | 28–30 s: logo finale e invito all'azione |
| `src/ui/*.tsx` | Schermate dell'app ricostruite e animate (ogni file espone i propri tempi, es. `OGGI_T`) |
| `src/components/*` | Telefono 3D, logo a pezzi, scia luminosa, fondo, titoli, sfocatura di movimento |
| `tools/extract_assets.py` | Ritaglia loghi, robot, foto e icone dalle immagini di riferimento in `public/img` |
| `tools/make_audio.py` | Sintetizza `public/audio/music.wav` e `sfx.wav` |

## Come modificare

- **Testi dei titoli**: `src/Spot.tsx` (componenti `Caption`).
- **Tempi di una schermata**: costanti `*_T` in testa a ogni file di `src/ui`.
- **Movimenti di camera**: blocchi per scena in `phonePose` (`src/scenes/PhoneJourney.tsx`).
- **Effetti sonori**: i cue in `tools/make_audio.py` sono espressi in frame video; se sposti un'animazione, sposta lo stesso numero lì e riesegui `npm run audio`.

## Origine dei materiali

- Logo, robot, icone, foto dei piatti e schermate derivano dalle immagini di riferimento fornite dal committente (storyboard e schermate dell'app).
- Musica ed effetti sonori sono generati proceduralmente da `tools/make_audio.py`: nessun campione o brano di terzi.
- Carattere: Poppins (SIL Open Font License), dal pacchetto `@fontsource/poppins`.
- «Google Play» e il relativo logo sono marchi di Google LLC: la variante con badge va usata secondo le linee guida di Google e solo ad app pubblicata.
