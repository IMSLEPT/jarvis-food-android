import { Audio } from "@remotion/media";
import React from "react";
import { AbsoluteFill, Easing, interpolate, Sequence, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { Flash } from "./Kinetic";
import { AllInOne, Future, Montage, Outro } from "./scenes/Finale";
import { AiPhoto, Diary, Fasting, Reveal } from "./scenes/Product";
import { OnePerson, OneWeek, Today, Yesterday, ZeroCost } from "./scenes/Story";
import { SceneStart, TL } from "./tl";

const SCENES: Record<string, React.FC> = {
  Yesterday, Today, OnePerson, OneWeek, ZeroCost, Reveal, Diary, AiPhoto, Fasting, Montage, AllInOne, Future, Outro,
};

type Wipe = { kind: "circle"; x: number; y: number } | { kind: "up" } | { kind: "left" } | { kind: "none" };

/** Come entra ogni scena sopra la precedente. */
const ENTER: Record<string, Wipe> = {
  Yesterday: { kind: "none" },
  Today: { kind: "circle", x: 540, y: 960 },
  OnePerson: { kind: "up" },
  OneWeek: { kind: "circle", x: 540, y: 1000 },
  ZeroCost: { kind: "left" },
  Reveal: { kind: "circle", x: 540, y: 700 },
  Diary: { kind: "up" },
  AiPhoto: { kind: "circle", x: 540, y: 890 },
  Fasting: { kind: "left" },
  Montage: { kind: "circle", x: 540, y: 1100 },
  AllInOne: { kind: "up" },
  Future: { kind: "circle", x: 540, y: 1080 },
  Outro: { kind: "circle", x: 540, y: 640 },
};

/** Coda in cui la scena resta sotto la successiva mentre questa si apre. */
const TAIL = 18;

const Enter: React.FC<{ wipe: Wipe; frames: number; children: React.ReactNode }> = ({ wipe, frames, children }) => {
  const frame = useCurrentFrame();
  const p = interpolate(frame, [0, frames], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.5, 0, 0.15, 1),
  });
  let clipPath: string | undefined;
  if (p < 1) {
    if (wipe.kind === "circle") clipPath = `circle(${p * 2300}px at ${wipe.x}px ${wipe.y}px)`;
    else if (wipe.kind === "up") clipPath = `inset(${(1 - p) * 100}% 0% 0% 0% round ${(1 - p) * 120}px ${(1 - p) * 120}px 0px 0px)`;
    else if (wipe.kind === "left") clipPath = `inset(0% 0% 0% ${(1 - p) * 100}%)`;
  }
  return <AbsoluteFill style={{ clipPath }}>{children}</AbsoluteFill>;
};

/**
 * Promo Jarvis Food 45 s · 1080×1920 · 60 fps.
 * Tempi da src/timeline.json (scripts/build_audio.py): 104 → 116 → 120 → 128 BPM.
 * Ogni scena è un fondo pieno che si apre sulla precedente (cerchio o tendina) esattamente sul beat;
 * le aperture si accorciano man mano che la musica accelera.
 */
export const Promo45: React.FC = () => {
  const frame = useCurrentFrame();
  const { durationInFrames } = useVideoConfig();
  const ieri = TL.sections[0];

  return (
    <AbsoluteFill style={{ backgroundColor: "#0B0C10" }}>
      {TL.scenes.map((s, i) => {
        const Comp = SCENES[s.id];
        const last = i === TL.scenes.length - 1;
        const frames = s.section === "oggi" ? 16 : s.section === "costruzione" ? 15 : 12;
        return (
          <Sequence key={s.id} name={s.id} from={s.start} durationInFrames={s.end - s.start + (last ? 0 : TAIL)}>
            <SceneStart.Provider value={s.start}>
              <Enter wipe={ENTER[s.id]} frames={s.id === "Today" ? 12 : frames}>
                <Comp />
              </Enter>
            </SceneStart.Provider>
          </Sequence>
        );
      })}

      {/* lampi solo nei tre momenti forti della musica */}
      {[
        { id: "Today", color: "#FFFFFF", strength: 0.9, frames: 14 },
        { id: "Reveal", color: "#FFFFFF", strength: 0.5, frames: 12 },
        { id: "Outro", color: "#FFFFFF", strength: 0.8, frames: 16 },
      ].map((fx) => {
        const s = TL.scenes.find((x) => x.id === fx.id)!;
        return (
          <Sequence key={fx.id} from={s.start} durationInFrames={fx.frames + 1} layout="absolute-fill">
            <Flash color={fx.color} strength={fx.strength} frames={fx.frames} />
          </Sequence>
        );
      })}

      {/* buio prima del drop (mezzo beat di silenzio nella musica) */}
      <AbsoluteFill
        style={{
          backgroundColor: "#000",
          opacity: interpolate(frame, [ieri.end - 16, ieri.end - 3, ieri.end], [0, 1, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      />
      {/* dissolvenza finale */}
      <AbsoluteFill
        style={{
          backgroundColor: "#0C1540",
          opacity: interpolate(frame, [durationInFrames - 45, durationInFrames - 1], [0, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
          }),
        }}
      />

      <Audio src={staticFile("soundtrack.wav")} />
    </AbsoluteFill>
  );
};
