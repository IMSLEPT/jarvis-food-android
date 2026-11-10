import React from "react";
import { AbsoluteFill, Easing, interpolate, useCurrentFrame } from "remotion";
import { FONT_BODY, FONT_TITLE } from "../theme";
import { beatPulse, useSceneStart, useWords } from "./tl";

const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const OUT = Easing.bezier(0.16, 1, 0.3, 1);

/** Palette dello spot: carta chiara, inchiostro navy, arancione del marchio. */
export const P = {
  paper: "#F7F9FC",
  ink: "#17286B",
  inkDeep: "#0C1540",
  orange: "#F25E00",
  orangeLight: "#FF8A1C",
  gold: "#FFB02E",
  softNavy: "#E4E9F6",
  softOrange: "#FFE4D2",
  green: "#10B981",
  blue: "#3B82F6",
  violet: "#8B5CF6",
  pink: "#F43F6E",
  coal: "#15161D",
  ash: "#8C90A6",
} as const;

/**
 * Testo cinetico sincronizzato con la voce: ogni parola entra nel frame in cui viene pronunciata
 * (2 frame prima, per compensare la percezione).
 *
 * - `variant` "rise": la parola sale da dietro una maschera (pulito, stile editoriale)
 * - `variant` "slam": arriva grande e sfocata e si assesta (per le parole chiave)
 * - `accent`: indice della parola → colore
 */
export const Words: React.FC<{
  line: string;
  from?: number;
  to?: number;
  size: number;
  color?: string;
  accent?: Record<number, string>;
  font?: "title" | "body";
  weight?: number;
  variant?: "rise" | "slam";
  width?: number;
  align?: "center" | "left";
  lineHeight?: number;
  lead?: number;
}> = ({
  line,
  from = 0,
  to,
  size,
  color = "#FFFFFF",
  accent = {},
  font = "title",
  weight,
  variant = "rise",
  width = 920,
  align = "center",
  lineHeight = 1.04,
  lead = 2,
}) => {
  const frame = useCurrentFrame();
  const words = useWords(line).slice(from, to);

  return (
    <div
      style={{
        width,
        display: "flex",
        flexWrap: "wrap",
        justifyContent: align === "center" ? "center" : "flex-start",
        columnGap: size * 0.24,
        rowGap: 0,
        fontFamily: font === "title" ? FONT_TITLE : FONT_BODY,
        fontWeight: weight ?? (font === "title" ? 800 : 600),
        fontSize: size,
        lineHeight,
        letterSpacing: font === "title" ? -size * 0.012 : 0,
        color,
      }}
    >
      {words.map((w, i) => {
        const s = w.start - lead;
        const idx = i + from;
        const c = accent[idx] ?? color;
        if (variant === "slam") {
          const blur = interpolate(frame, [s, s + 7], [14, 0], clamp);
          return (
            <span
              key={idx}
              style={{
                display: "inline-block",
                color: c,
                opacity: interpolate(frame, [s, s + 3], [0, 1], clamp),
                scale: `${interpolate(frame, [s, s + 11], [1.8, 1], { ...clamp, easing: OUT })}`,
                filter: blur > 0.05 ? `blur(${blur}px)` : undefined,
              }}
            >
              {w.w}
            </span>
          );
        }
        return (
          <span
            key={idx}
            style={{ display: "inline-block", overflow: "hidden", paddingBottom: size * 0.16, marginBottom: -size * 0.16 }}
          >
            <span
              style={{
                display: "inline-block",
                color: c,
                translate: `0px ${interpolate(frame, [s, s + 13], [112, 0], { ...clamp, easing: OUT })}%`,
                rotate: `${interpolate(frame, [s, s + 13], [6, 0], { ...clamp, easing: OUT })}deg`,
                transformOrigin: "0% 100%",
              }}
            >
              {w.w}
            </span>
          </span>
        );
      })}
    </div>
  );
};

/**
 * Fondo pieno della scena con forme geometriche piatte che derivano lentamente
 * e "respirano" sul beat della musica.
 */
export const Stage: React.FC<{
  bg: string;
  shapes?: { color: string; size: number; x: number; y: number; kind?: "circle" | "ring" | "pill" }[];
  children: React.ReactNode;
  pulse?: boolean;
}> = ({ bg, shapes = [], children, pulse = true }) => {
  const frame = useCurrentFrame();
  const s0 = useSceneStart();
  const beat = pulse ? beatPulse(s0 + frame, 9) : 0;
  return (
    <AbsoluteFill style={{ background: bg, overflow: "hidden" }}>
      {shapes.map((sh, i) => {
        const enter = interpolate(frame, [i * 3, 22 + i * 3], [0, 1], { ...clamp, easing: OUT });
        const dx = Math.sin(frame / 55 + i * 2.1) * 26;
        const dy = Math.cos(frame / 47 + i * 1.3) * 30;
        const kind = sh.kind ?? "circle";
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: sh.x - sh.size / 2 + dx,
              top: sh.y - sh.size / 2 + dy,
              width: sh.size,
              height: kind === "pill" ? sh.size * 0.42 : sh.size,
              borderRadius: sh.size,
              background: kind === "ring" ? "transparent" : sh.color,
              border: kind === "ring" ? `${Math.max(10, sh.size * 0.06)}px solid ${sh.color}` : undefined,
              scale: `${enter * (1 + beat * 0.035)}`,
              rotate: kind === "pill" ? `${-24 + Math.sin(frame / 60 + i) * 4}deg` : undefined,
            }}
          />
        );
      })}
      {children}
    </AbsoluteFill>
  );
};

/** Lampo sul taglio (posizionato come sequenza nel frame del cambio scena). */
export const Flash: React.FC<{ color?: string; strength?: number; frames?: number }> = ({
  color = "#FFFFFF",
  strength = 0.45,
  frames = 8,
}) => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill
      style={{
        backgroundColor: color,
        opacity: interpolate(frame, [0, frames], [strength, 0], { ...clamp, easing: Easing.bezier(0.33, 1, 0.68, 1) }),
        pointerEvents: "none",
      }}
    />
  );
};

/** Pillola (etichette e call to action). */
export const Pill: React.FC<{
  children: React.ReactNode;
  bg?: string;
  color?: string;
  size?: number;
  style?: React.CSSProperties;
}> = ({ children, bg = P.orange, color = "#FFFFFF", size = 60, style }) => (
  <div
    style={{
      padding: `${size * 0.36}px ${size * 0.8}px`,
      borderRadius: 999,
      background: bg,
      fontFamily: FONT_TITLE,
      fontWeight: 800,
      fontSize: size,
      color,
      whiteSpace: "nowrap",
      ...style,
    }}
  >
    {children}
  </div>
);

/** Ingresso a molla di un elemento a partire da `at` (frame locale). */
export const Pop: React.FC<{ at: number; children: React.ReactNode; style?: React.CSSProperties; damping?: number }> = ({
  at,
  children,
  style,
  damping = 11,
}) => {
  const frame = useCurrentFrame();
  return (
    <div
      style={{
        opacity: interpolate(frame, [at, at + 4], [0, 1], clamp),
        scale: `${interpolate(frame, [at, at + 22], [0.5, 1], { ...clamp, easing: Easing.spring({ damping }) })}`,
        ...style,
      }}
    >
      {children}
    </div>
  );
};
