# Foto di tutte le ricette senza appesantire l'app

Stato: 170 ricette su 2.533 hanno foto (circa 76 MB nell'APK). Per averle tutte (~10.000 immagini tra piatto e passaggi) non si può metterle nell'APK.

## Soluzione proposta
1. **Foto scaricate al bisogno**: l'app mostra la foto da un indirizzo web e la salva in cache sul telefono (solo le ricette viste). Pubblicazione gratuita su GitHub Pages/Releases del repository.
2. **WebP piccoli**: 720 px lato lungo, qualità 80 → circa 35-60 KB l'una (circa 400 MB totali online, 0 MB nell'APK).
3. **Anteprima nell'APK**: solo le 170 foto attuali, ricompresse (da 76 MB a circa 15 MB), e in seguito spostabili online anch'esse.
4. **Generazione a lotti**: pipeline `tools/photography` (plan.py → generazione → prepare.py), priorità alle ricette più usate (suggerimenti di Oggi e piano), prima il piatto finito, poi i passaggi. Serve uno strumento di generazione immagini e i relativi crediti.
5. **Comportamento senza rete**: segnaposto compatto già pronto.

## Da decidere
- Dove ospitare le immagini e quale generatore usare.
- La Scheda dati di Google Play va aggiornata (l'app già usa internet per il modello e il cloud AI).
