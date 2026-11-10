import React from "react";
import { Img, staticFile } from "remotion";
import meta from "../logoMeta.json";

export type PieceName = "hat" | "jarvis" | "food" | "smile" | "sparks";
export type PieceState = {
  x?: number;
  y?: number;
  scale?: number;
  rot?: number;
  rotX?: number;
  rotY?: number;
  opacity?: number;
  /** 0..1: rivelazione da sinistra a destra (per il sorriso) */
  wipe?: number;
};
const DEPTH: Record<PieceName, number> = { smile: 10, hat: 40, food: 70, jarvis: 110, sparks: 60 };
const ORDER: PieceName[] = ["smile", "hat", "food", "jarvis", "sparks"];
export const LOGO_W = meta.box[0];
export const LOGO_H = meta.box[1];

/** Logo originale scomposto in 5 pezzi su piani 3D diversi: ruotando il gruppo i pezzi scorrono in parallasse. */
export const Logo: React.FC<{
  cx: number;
  cy: number;
  /** larghezza a schermo in px */
  width: number;
  rx?: number;
  ry?: number;
  rz?: number;
  opacity?: number;
  glow?: number;
  pieces?: Partial<Record<PieceName, PieceState>>;
}> = ({ cx, cy, width, rx = 0, ry = 0, rz = 0, opacity = 1, glow = 0.6, pieces = {} }) => {
  if (opacity <= 0.001) return null;
  const s = width / LOGO_W;
  return (
    <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 1920, perspective: 2200, perspectiveOrigin: `${cx}px ${cy}px`, opacity, pointerEvents: "none" }}>
      <div style={{ position: "absolute", left: cx - 700, top: cy - 700, width: 1400, height: 1400, background: `radial-gradient(closest-side, rgba(255,130,20,${0.42 * glow}), rgba(255,110,0,${0.12 * glow}) 55%, rgba(255,110,0,0))` }} />
      <div
        style={{
          position: "absolute",
          left: cx - LOGO_W / 2,
          top: cy - LOGO_H / 2,
          width: LOGO_W,
          height: LOGO_H,
          transformStyle: "preserve-3d",
          transform: `scale(${s}) rotateZ(${rz}deg) rotateX(${rx}deg) rotateY(${ry}deg)`,
        }}
      >
        {ORDER.map((n) => {
          const [x, y, w, h] = (meta.pieces as Record<string, number[]>)[n];
          const st = pieces[n] ?? {};
          if ((st.opacity ?? 1) <= 0.001 || (st.scale ?? 1) <= 0.001) return null;
          return (
            <div
              key={n}
              style={{
                position: "absolute",
                left: x,
                top: y,
                width: w,
                height: h,
                opacity: st.opacity ?? 1,
                transform: `translate3d(${st.x ?? 0}px, ${st.y ?? 0}px, ${DEPTH[n]}px) rotateX(${st.rotX ?? 0}deg) rotateY(${st.rotY ?? 0}deg) rotate(${st.rot ?? 0}deg) scale(${st.scale ?? 1})`,
                clipPath: st.wipe !== undefined && st.wipe < 1 ? `inset(-20% ${(1 - st.wipe) * 100}% -20% -20%)` : undefined,
                filter: "drop-shadow(0 14px 22px rgba(0,0,20,0.45))",
              }}
            >
              <Img src={staticFile(`img/logo_${n}.png`)} style={{ width: w, height: h }} />
            </div>
          );
        })}
      </div>
    </div>
  );
};
