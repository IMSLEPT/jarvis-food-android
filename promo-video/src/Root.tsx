import React from "react";
import { Composition } from "remotion";
import { Spot } from "./Spot";
import { DURATION, FPS, H, W } from "./theme";

export const RemotionRoot: React.FC = () => (
  <>
    {/* Versione principale: finale neutro (nessuna dichiarazione di disponibilita' sugli store). */}
    <Composition id="JarvisFoodSpot" component={Spot} durationInFrames={DURATION} fps={FPS} width={W} height={H} defaultProps={{ cta: "neutral" as const }} />
    {/* Variante con "Scarica ora" + badge Google Play: da usare SOLO quando l'app e' pubblicata. */}
    <Composition id="JarvisFoodSpot-GooglePlay" component={Spot} durationInFrames={DURATION} fps={FPS} width={W} height={H} defaultProps={{ cta: "googlePlay" as const }} />
  </>
);
