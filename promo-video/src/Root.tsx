import React from "react";
import { Composition, Folder } from "remotion";
import { fontsReady } from "./fonts";
import { JarvisPromo } from "./JarvisPromo";
import { AiPhoto } from "./scenes/AiPhoto";
import { Diary } from "./scenes/Diary";
import { Fasting } from "./scenes/Fasting";
import { Features } from "./scenes/Features";
import { Hook } from "./scenes/Hook";
import { Intro } from "./scenes/Intro";
import { Outro } from "./scenes/Outro";
import { Background } from "./components/Background";
import { Promo45 } from "./v2/Promo45";

// I font vengono caricati prima del primo frame (delayRender gestito da @remotion/fonts).
void fontsReady;

/** Scena isolata con lo sfondo, per l'anteprima nello Studio. */
const withBg = (Scene: React.FC): React.FC => {
  const Wrapped: React.FC = () => (
    <>
      <Background />
      <Scene />
    </>
  );
  return Wrapped;
};

export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Composition
        id="JarvisPromo45"
        component={Promo45}
        durationInFrames={2700}
        fps={60}
        width={1080}
        height={1920}
      />
      <Composition
        id="JarvisPromo"
        component={JarvisPromo}
        durationInFrames={1800}
        fps={60}
        width={1080}
        height={1920}
      />
      <Folder name="Scene">
        <Composition id="S1-Intro" component={withBg(Intro)} durationInFrames={252} fps={60} width={1080} height={1920} />
        <Composition id="S2-Hook" component={withBg(Hook)} durationInFrames={264} fps={60} width={1080} height={1920} />
        <Composition id="S3-Diario" component={withBg(Diary)} durationInFrames={384} fps={60} width={1080} height={1920} />
        <Composition id="S4-FotoAI" component={withBg(AiPhoto)} durationInFrames={264} fps={60} width={1080} height={1920} />
        <Composition id="S5-Digiuno" component={withBg(Fasting)} durationInFrames={264} fps={60} width={1080} height={1920} />
        <Composition id="S6-Funzioni" component={withBg(Features)} durationInFrames={264} fps={60} width={1080} height={1920} />
        <Composition id="S7-Outro" component={withBg(Outro)} durationInFrames={252} fps={60} width={1080} height={1920} />
      </Folder>
    </>
  );
};
