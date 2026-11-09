import { Audio } from "@remotion/media";
import React from "react";
import { AbsoluteFill, interpolate, Sequence, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { Background } from "../components/Background";
import { CutIn, Flash } from "./Kinetic";
import { AiPhoto, Diary, Fasting, Reveal } from "./scenes/Product";
import { AllInOne, Future, Montage, Outro } from "./scenes/Finale";
import { OnePerson, OneWeek, Today, Yesterday, ZeroCost } from "./scenes/Story";
import { SceneStart, TL, beatPulse } from "./tl";

const SCENES: Record<string, React.FC> = {
  Yesterday, Today, OnePerson, OneWeek, ZeroCost, Reveal, Diary, AiPhoto, Fasting, Montage, AllInOne, Future, Outro,
};

/** Effetto sul taglio in base al momento musicale: lampi forti sui drop, più rapidi quando il ritmo accelera. */
const CUT_FX: Record<string, { color: string; strength: number; frames: number } | null> = {
  Yesterday: null,
  Today: { color: "#FFB02E", strength: 0.95, frames: 16 },
  Reveal: { color: "#FFFFFF", strength: 0.8, frames: 14 },
  Outro: { color: "#FFFFFF", strength: 0.9, frames: 18 },
};

/**
 * Promo Jarvis Food 45 s · 1080×1920 · 60 fps.
 * Tempi da src/timeline.json (scripts/build_audio.py): 90 → 108 → 120 → 128 BPM,
 * scene tagliate sui beat, testo sincronizzato parola per parola con la voce.
 */
export const Promo45: React.FC = () => {
  const frame = useCurrentFrame();
  const { durationInFrames } = useVideoConfig();
  const ieri = TL.sections[0];
  const inIeri = frame < ieri.end;
  const pulse = inIeri ? 0 : beatPulse(frame, 8);

  return (
    <AbsoluteFill style={{ backgroundColor: "#060B26" }}>
      <Background pulse={pulse} saturation={inIeri ? 0.15 : 1} />

      {TL.scenes.map((s) => {
        const Comp = SCENES[s.id];
        const fast = s.section === "corsa" || s.section === "finale";
        return (
          <Sequence key={s.id} name={s.id} from={s.start} durationInFrames={s.end - s.start}>
            <SceneStart.Provider value={s.start}>
              <CutIn frames={s.id === "Yesterday" ? 30 : fast ? 7 : 10} zoom={fast ? 1.12 : 1.07}>
                <Comp />
              </CutIn>
            </SceneStart.Provider>
          </Sequence>
        );
      })}

      {TL.scenes.slice(1).map((s) => {
        const fx = CUT_FX[s.id] ?? { color: "#FFFFFF", strength: 0.28, frames: s.section === "corsa" ? 5 : 7 };
        return (
          <Sequence key={`fx-${s.id}`} from={s.start} durationInFrames={fx.frames + 1} layout="absolute-fill">
            <Flash {...fx} />
          </Sequence>
        );
      })}

      {/* buio prima del drop (mezzo beat di silenzio nella musica) */}
      <AbsoluteFill
        style={{
          backgroundColor: "#000",
          opacity: interpolate(frame, [ieri.end - 14, ieri.end - 2, ieri.end], [0, 0.85, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      />
      {/* dissolvenza finale */}
      <AbsoluteFill
        style={{
          backgroundColor: "#02040F",
          opacity: interpolate(frame, [durationInFrames - 50, durationInFrames - 1], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      />

      <Audio src={staticFile("soundtrack.wav")} />
    </AbsoluteFill>
  );
};
