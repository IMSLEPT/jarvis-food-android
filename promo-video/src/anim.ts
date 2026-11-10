import { Easing, interpolate, spring } from "remotion";
import { FPS } from "./theme";

export const EZ = {
  out: Easing.bezier(0.16, 1, 0.3, 1),
  outSoft: Easing.bezier(0.25, 1, 0.5, 1),
  inOut: Easing.bezier(0.65, 0, 0.35, 1),
  expoInOut: Easing.bezier(0.87, 0, 0.13, 1),
  in: Easing.bezier(0.7, 0, 0.84, 0),
  inSoft: Easing.bezier(0.5, 0, 0.75, 0),
  back: Easing.bezier(0.34, 1.56, 0.64, 1),
  backSoft: Easing.bezier(0.3, 1.25, 0.5, 1),
  linear: Easing.linear,
};
type Ease = (t: number) => number;

/** interpolate con clamp su entrambi i lati */
export const ip = (f: number, range: number[], out: number[], easing?: Ease) =>
  interpolate(f, range, out, {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing,
  });

/** Keyframe: [[frame, valore, easing per ARRIVARE a questo keyframe], ...] */
export type KF = [number, number, Ease?][];
export const kf = (f: number, keys: KF): number => {
  if (f <= keys[0][0]) return keys[0][1];
  for (let i = 1; i < keys.length; i++) {
    const [f1, v1, e] = keys[i];
    if (f <= f1) {
      const [f0, v0] = keys[i - 1];
      if (f1 === f0) return v1;
      const t = (f - f0) / (f1 - f0);
      return v0 + (v1 - v0) * (e ?? EZ.inOut)(t);
    }
  }
  return keys[keys.length - 1][1];
};

/** Molla 0 -> 1 che parte al frame `start` */
export const sp = (
  f: number,
  start: number,
  cfg: { damping?: number; stiffness?: number; mass?: number } = {},
) =>
  spring({
    frame: f - start,
    fps: FPS,
    config: { damping: 11, stiffness: 180, mass: 0.7, ...cfg },
  });

/** impulso 0 -> 1 -> 0 (per flash, onde d'urto, pressioni) */
export const pulse = (f: number, start: number, up: number, down: number) =>
  f < start ? 0 : f < start + up ? (f - start) / up : Math.max(0, 1 - (f - start - up) / down);

export const lerp = (a: number, b: number, t: number) => a + (b - a) * t;

/** Numeri in formato italiano: 2050 -> "2.050", 71.2 -> "71,2" */
export const itNum = (v: number, dec = 0) => {
  const s = v.toFixed(dec);
  const [a, b] = s.split(".");
  const th = a.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return b ? `${th},${b}` : th;
};
