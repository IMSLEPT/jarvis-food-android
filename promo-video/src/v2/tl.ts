import { createContext, useContext } from "react";
import timeline from "../timeline.json";

/**
 * Accesso alla timeline generata da scripts/build_audio.py:
 * beat (con BPM crescenti), scene, frasi della voce e singole parole, tutto in frame a 60 fps.
 */
export type Word = { w: string; start: number; end: number };
export type Line = { id: string; text: string; start: number; end: number; words: Word[] };
export type Scene = { id: string; start: number; end: number; section: string };

export const TL = timeline as {
  fps: number;
  durationInFrames: number;
  sections: { name: string; bpm: number; start: number; end: number }[];
  beats: { f: number; section: string; bar: number; beat: number }[];
  scenes: Scene[];
  lines: Line[];
};

export const scene = (id: string): Scene => {
  const s = TL.scenes.find((x) => x.id === id);
  if (!s) throw new Error(`Scena ${id} non trovata nella timeline`);
  return s;
};

export const line = (id: string): Line => {
  const l = TL.lines.find((x) => x.id === id);
  if (!l) throw new Error(`Frase ${id} non trovata nella timeline`);
  return l;
};

/** Inizio (frame assoluto) della scena corrente, per convertire i tempi assoluti in locali. */
export const SceneStart = createContext(0);
export const useSceneStart = () => useContext(SceneStart);

/** Parole di una frase con frame relativi alla scena corrente. */
export const useWords = (id: string): Word[] => {
  const s0 = useSceneStart();
  return line(id).words.map((w) => ({ ...w, start: w.start - s0, end: w.end - s0 }));
};

/** Beat (frame assoluti) di tutta la timeline. */
export const BEATS = TL.beats.map((b) => b.f);

/** Impulso 0..1 che decade dopo ogni beat (per pulsazioni grafiche). */
export const beatPulse = (absFrame: number, decay = 7): number => {
  let last = -1e9;
  for (const b of BEATS) {
    if (b <= absFrame) last = b;
    else break;
  }
  return Math.exp(-(absFrame - last) / decay);
};
