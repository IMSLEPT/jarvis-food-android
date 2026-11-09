import { Audio } from "@remotion/media";
import { linearTiming, springTiming, TransitionSeries } from "@remotion/transitions";
import { pushCut } from "@remotion/transitions/push-cut";
import { slide } from "@remotion/transitions/slide";
import { wipe } from "@remotion/transitions/wipe";
import React from "react";
import { AbsoluteFill, Easing, interpolate, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { Background } from "./components/Background";
import { AiPhoto } from "./scenes/AiPhoto";
import { Diary } from "./scenes/Diary";
import { Fasting } from "./scenes/Fasting";
import { Features } from "./scenes/Features";
import { Hook } from "./scenes/Hook";
import { Intro } from "./scenes/Intro";
import { Outro } from "./scenes/Outro";

/**
 * Promo verticale Jarvis Food: 1080×1920, 60 fps, 30 s.
 * Ogni transizione dura 24 frame ed è centrata su un downbeat della musica
 * (4, 8, 14, 18, 22, 26 s), dove la traccia ha impatti/whoosh.
 */
export const JarvisPromo: React.FC = () => {
  const frame = useCurrentFrame();
  const { durationInFrames } = useVideoConfig();

  return (
    <AbsoluteFill style={{ backgroundColor: "#060B26" }}>
      <Background />

      <TransitionSeries>
        <TransitionSeries.Sequence name="Intro" durationInFrames={252}>
          <Intro />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={pushCut({ flashColor: "#FFB02E", flashOpacity: 0.85 })}
          timing={linearTiming({ durationInFrames: 24 })}
        />
        <TransitionSeries.Sequence name="Hook" durationInFrames={264}>
          <Hook />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={slide({ direction: "from-bottom" })}
          timing={springTiming({ config: { damping: 200 }, durationInFrames: 24 })}
        />
        <TransitionSeries.Sequence name="Diario" durationInFrames={384}>
          <Diary />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={wipe({ direction: "from-right" })}
          timing={linearTiming({ durationInFrames: 24, easing: Easing.bezier(0.65, 0, 0.35, 1) })}
        />
        <TransitionSeries.Sequence name="Foto AI" durationInFrames={264}>
          <AiPhoto />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={slide({ direction: "from-right" })}
          timing={springTiming({ config: { damping: 200 }, durationInFrames: 24 })}
        />
        <TransitionSeries.Sequence name="Digiuno" durationInFrames={264}>
          <Fasting />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={wipe({ direction: "from-bottom-left" })}
          timing={linearTiming({ durationInFrames: 24, easing: Easing.bezier(0.65, 0, 0.35, 1) })}
        />
        <TransitionSeries.Sequence name="Funzioni" durationInFrames={264}>
          <Features />
        </TransitionSeries.Sequence>
        <TransitionSeries.Transition
          presentation={pushCut({ flashColor: "#FFFFFF", flashOpacity: 0.9 })}
          timing={linearTiming({ durationInFrames: 24 })}
        />
        <TransitionSeries.Sequence name="Outro" durationInFrames={252}>
          <Outro />
        </TransitionSeries.Sequence>
      </TransitionSeries>

      {/* Dissolvenza finale al nero, sincronizzata con il fade della musica */}
      <AbsoluteFill
        style={{
          backgroundColor: "#02040F",
          opacity: interpolate(frame, [durationInFrames - 40, durationInFrames - 1], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      />

      <Audio src={staticFile("music.wav")} />
    </AbsoluteFill>
  );
};
