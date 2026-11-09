# Jarvis Food · Promo video

Video promozionali verticali costruiti con **Remotion + React + TypeScript**:

- **`JarvisPromo45`** — 45 s con voce fuori campo, testo cinetico parola per parola e ritmo crescente (principale)
- **`JarvisPromo`** — 30 s solo musica (prima versione)

## Versione 45 s con voce

### Testo (voiceover/script.json)

> Una volta, per creare un'app servivano un team intero, mesi di lavoro… e budget da capogiro.
> **Oggi, no.** Oggi basta una persona. Una settimana. E un costo quasi pari a zero.
> Jarvis Food è nata così. Un diario alimentare che ti capisce. Calorie. Macro. Ogni pasto. In un tocco.
> Scatta una foto: Jarvis riconosce il piatto. Il digiuno? Ti guida, fase dopo fase.
> Piano pasti. Ricette. Spesa. Progressi. Tutto, in un'unica app.
> Il futuro delle app è già qui. Ed è nel tuo telefono.
> **Jarvis Food. Mangia meglio, ogni giorno.**

### Ritmo crescente

| Sezione | BPM | Tempo | Scene |
|---|---|---|---|
| ieri | 104 | 0–6,9 s | team, mesi, budget (desaturato, nessuna batteria) |
| oggi | 116 | 6,9–15,2 s | drop "Oggi, no.", 1 persona, 7 giorni, ≈ 0 € |
| costruzione | 120 | 15,2–31,2 s | logo, diario, foto AI, digiuno |
| corsa | 128 | 31,2–36,8 s | montaggio a un taglio per parola, "tutto in un'unica app" |
| finale | 128 | 36,8–45 s | futuro, firma, call to action |

I tagli si fanno più fitti (da ~2,5 s a ~0,8 s), gli ingressi di scena più rapidi e la
batteria si arricchisce (semicrome, rullante che accelera) fino all'impatto sulla firma.

### Pipeline

1. **Voce** — `voiceover/generate_vo.py`: [Chatterbox Multilingual](https://github.com/resemble-ai/chatterbox)
   (Resemble AI, licenza MIT, open source) in italiano, clonando il prompt maschile italiano
   ufficiale della demo (`voiceover/ref/it_m1.flac`). Per ogni frase genera più take,
   li trascrive con faster-whisper e tiene quello senza errori. `voiceover/tighten.py`
   ripristina i take scelti e toglie code e artefatti dopo l'ultima parola.
   L'audio generato contiene il watermark impercettibile Perth di Resemble (marcatura come audio AI).
2. **Regia audio** — `scripts/build_audio.py` (`npm run audio`): sezioni e BPM, posizione di ogni
   frase su un beat, musica sintetizzata, catena voce (EQ, compressione, de-esser), ducking della
   musica sotto il parlato, normalizzazione a −14 LUFS. Scrive `public/soundtrack.wav` e
   `src/timeline.json` (beat, scene, frasi e parole in frame a 60 fps).
3. **Video** — `src/v2/`: le scene leggono `timeline.json`; il componente `Words` fa comparire ogni
   parola nel frame in cui viene pronunciata (testo dal copione, tempi da Whisper).

Rigenerare la voce (opzionale, richiede ~6 GB per PyTorch e ~3 GB di modello):

```bash
uv venv -p 3.13 .venv-tts && source .venv-tts/bin/activate
uv pip install chatterbox-tts faster-whisper "setuptools<81" "av<16"
python voiceover/generate_vo.py --ref voiceover/ref/it_m1.flac --takes 5
python voiceover/tighten.py
npm run audio
```

## Versione 30 s

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
npm run render       # → out/jarvis-food-promo-45s.mp4 (versione con voce)
npm run render:30    # → out/jarvis-food-promo.mp4 (versione 30 s)
npm run audio        # rigenera mix e timeline della versione 45 s (python3 + numpy + ffmpeg)
npm run music        # rigenera public/music.wav della versione 30 s
```

Se Chrome Headless Shell non può essere scaricato (rete chiusa), indicare un Chromium
headless già installato:

```bash
REMOTION_BROWSER=/percorso/chrome-headless-shell npm run render
```

## Export automatico (GitHub Actions)

Il workflow `.github/workflows/promo-video.yml` installa le dipendenze, rigenera la musica,
esegue i controlli, renderizza entrambi gli MP4 e li carica come artifact `jarvis-food-promo`.
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
