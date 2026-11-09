# Jarvis Food · Promo video

Video promozionale verticale di 30 secondi, costruito con **Remotion + React + TypeScript**.

| | |
|---|---|
| Formato | MP4 H.264 + AAC, 1080×1920, 60 fps, 30 s (1800 frame) |
| Composizione | `JarvisPromo` (scene singole nella cartella *Scene* dello Studio) |
| Musica | originale, sintetizzata da `scripts/make_music.py` (120 BPM) |

## Struttura e sincronizzazione

A 60 fps e 120 BPM: 1 beat = 30 frame, 1 battuta = 120 frame. Ogni transizione dura
24 frame ed è centrata su un downbeat dove la musica ha un impatto o un whoosh.

| Tempo | Scena | Contenuto | Transizione in uscita |
|---|---|---|---|
| 0–4 s | `Intro` | logo e nome sul riser | push-cut con flash sul drop |
| 4–8 s | `Hook` | "Ogni pasto conta." parola per parola sui beat, parete di piatti | slide dal basso |
| 8–14 s | `Diary` | registrazione reale: Home, calorie, diario + etichette sui beat | wipe |
| 14–18 s | `AiPhoto` | scansione AI della foto, rilevamenti, totale kcal | slide da destra |
| 18–22 s | `Fasting` | registrazione reale del digiuno + fasi (dal codice dell'app) | wipe diagonale |
| 22–26 s | `Features` | montaggio sui beat: Piano, Ricettario, Spesa, Progressi | push-cut con flash |
| 26–30 s | `Outro` | impatto, logo, claim, call to action, dissolvenza | — |

`src/theme.ts` contiene palette (tema Aurora dell'app) e durate.

## Comandi

```bash
npm install
npm run dev          # Remotion Studio (anteprima interattiva)
npm run lint         # ESLint + TypeScript
npm run stills       # fotogrammi di controllo in out/stills
npm run render       # → out/jarvis-food-promo.mp4
npm run music        # rigenera public/music.wav (richiede python3 + numpy)
```

Se Chrome Headless Shell non può essere scaricato (rete chiusa), indicare un Chromium
headless già installato:

```bash
REMOTION_BROWSER=/percorso/chrome-headless-shell npm run render
```

## Export automatico (GitHub Actions)

Il workflow `.github/workflows/promo-video.yml` installa le dipendenze, rigenera la musica,
esegue i controlli, renderizza l'MP4 e lo carica come artifact `jarvis-food-promo`.
Parte da solo a ogni push che modifica `promo-video/`, oppure a mano da
*Actions → Render promo video → Run workflow*.

## Filmati dell'app

`public/footage/*.mp4` sono spezzoni della registrazione schermo
"Screenshot Jarvis Food.mp4" (Google Drive), convertiti a 60 fps costanti, ritagliati
(barra di stato con il timer di registrazione e tasti di sistema) e ridotti a 720×1432:

```bash
ffmpeg -ss <inizio> -i rec.mp4 -t <durata> -an \
  -vf "fps=60,crop=1008:2004:0:130,scale=720:1432:flags=lanczos,format=yuv420p" \
  -c:v libx264 -preset slow -crf 16 public/footage/<nome>.mp4
```

| Clip | Inizio (s) | Durata (s) |
|---|---|---|
| diary | 29.95 | 6.6 |
| fasting | 25.9 | 3.3 (riprodotta a 0,75×) |
| piano | 104.2 | 1.3 |
| cucina | 124.9 | 1.3 |
| spesa | 142.5 | 1.3 |
| progressi | 160.4 | 1.3 |

La registrazione originale (`footage-src/`, 270 MB) è esclusa da Git.

## Crediti e licenze

Vedi `public/CREDITS.md` (foto Pixabay Content License, font e logo dell'app, musica originale).
Remotion è gratuito per privati e team fino a 3 persone; per aziende più grandi serve
una licenza aziendale (https://www.remotion.pro/license).
