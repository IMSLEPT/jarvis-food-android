import React from "react";
import { C, FONT } from "../theme";
import { EZ, ip } from "../anim";

export type CapWord = { t: string; hi?: boolean };
type Props = {
  frame: number;
  inAt: number;
  outAt: number;
  lines: CapWord[][];
  y: number;
  size?: number;
  dark?: boolean; // testo chiaro su fondo scuro
  weight?: number;
  italic?: boolean;
  stagger?: number;
};

/** Titolo animato parola per parola: ogni parola sale da dietro una maschera, poi tutto esce verso l'alto. */
export const Caption: React.FC<Props> = ({ frame, inAt, outAt, lines, y, size = 62, dark = false, weight = 700, italic = false, stagger = 4 }) => {
  if (frame < inAt - 2 || frame > outAt + 16) return null;
  const out = ip(frame, [outAt, outAt + 14], [0, 1], EZ.in);
  let n = 0;
  return (
    <div
      style={{
        position: "absolute",
        left: 60,
        right: 60,
        top: y,
        textAlign: "center",
        fontFamily: FONT,
        fontWeight: weight,
        fontStyle: italic ? "italic" : "normal",
        fontSize: size,
        lineHeight: 1.16,
        letterSpacing: -0.8,
        color: dark ? "#FFFFFF" : C.navy,
        opacity: 1 - out,
        transform: `translateY(${-out * 40}px)`,
        textShadow: dark ? "0 4px 30px rgba(0,10,60,0.6)" : "none",
      }}
    >
      {lines.map((line, li) => (
        <div key={li} style={{ overflow: "hidden", padding: "0.06em 0 0.14em", marginBottom: "-0.14em" }}>
          {line.map((w, wi) => {
            const k = n++;
            const p = ip(frame, [inAt + k * stagger, inAt + k * stagger + 26], [0, 1], EZ.out);
            return (
              <span
                key={wi}
                style={{
                  display: "inline-block",
                  marginRight: "0.26em",
                  color: w.hi ? C.orange : undefined,
                  fontWeight: w.hi ? 800 : undefined,
                  transform: `translateY(${(1 - p) * 115}%) rotate(${(1 - p) * 6}deg)`,
                  opacity: Math.min(1, p * 3),
                }}
              >
                {w.t}
              </span>
            );
          })}
        </div>
      ))}
    </div>
  );
};

export const words = (s: string, hi: string[] = []): CapWord[] =>
  s.split(" ").map((t) => ({ t, hi: hi.includes(t.replace(/[.,]/g, "")) }));
