import React from "react";
import { AbsoluteFill, Easing, interpolate, useCurrentFrame } from "remotion";
import { C, FONT_BODY, FONT_TITLE } from "../theme";
import { useWords } from "./tl";

const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

/**
 * Testo cinetico sincronizzato con la voce: ogni parola entra nel frame in cui viene pronunciata
 * (2 frame prima, per compensare la percezione). Le parole indicate in `accent` cambiano colore.
 *
 * - `from`/`to`: sottoinsieme di parole della frase (indici, `to` escluso)
 * - `variant`: "pop" (scala + blur, default), "rise" (sale dal basso), "slam" (arriva grande e si assesta)
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
  variant?: "pop" | "rise" | "slam";
  width?: number;
  align?: "center" | "left";
  lineHeight?: number;
  style?: React.CSSProperties;
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
  variant = "pop",
  width = 920,
  align = "center",
  lineHeight = 1.05,
  style,
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
        columnGap: size * 0.26,
        rowGap: size * (lineHeight - 1) + 4,
        fontFamily: font === "title" ? FONT_TITLE : FONT_BODY,
        fontWeight: weight ?? (font === "title" ? 800 : 600),
        fontSize: size,
        lineHeight,
        color,
        textShadow: "0 10px 40px rgba(0,0,0,0.45)",
        ...style,
      }}
    >
      {words.map((w, i) => {
        const s = w.start - lead;
        const idx = i + from;
        const o = interpolate(frame, [s, s + 4], [0, 1], clamp);
        let translate = "0px 0px";
        let scale = 1;
        let blur = 0;
        if (variant === "pop") {
          scale = interpolate(frame, [s, s + 12], [0.6, 1], { ...clamp, easing: Easing.spring({ damping: 11 }) });
          blur = interpolate(frame, [s, s + 7], [10, 0], clamp);
          translate = `0px ${interpolate(frame, [s, s + 12], [size * 0.35, 0], { ...clamp, easing: Easing.bezier(0.16, 1, 0.3, 1) })}px`;
        } else if (variant === "rise") {
          translate = `0px ${interpolate(frame, [s, s + 14], [size * 0.9, 0], { ...clamp, easing: Easing.bezier(0.16, 1, 0.3, 1) })}px`;
        } else {
          scale = interpolate(frame, [s, s + 10], [1.9, 1], { ...clamp, easing: Easing.bezier(0.16, 1, 0.3, 1) });
          blur = interpolate(frame, [s, s + 8], [16, 0], clamp);
        }
        return (
          <span
            key={idx}
            style={{
              display: "inline-block",
              opacity: o,
              translate,
              scale: `${scale}`,
              filter: blur > 0.05 ? `blur(${blur}px)` : undefined,
              color: accent[idx] ?? color,
              overflow: variant === "rise" ? "hidden" : undefined,
            }}
          >
            {w.w}
          </span>
        );
      })}
    </div>
  );
};

/** Ingresso di scena "zoom-in" su taglio netto: più rapido quando la musica accelera. */
export const CutIn: React.FC<{ children: React.ReactNode; frames?: number; zoom?: number }> = ({
  children,
  frames = 10,
  zoom = 1.08,
}) => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill
      style={{
        scale: `${interpolate(frame, [0, frames], [zoom, 1], { ...clamp, easing: Easing.bezier(0.16, 1, 0.3, 1) })}`,
        filter: frame < frames ? `blur(${interpolate(frame, [0, frames], [8, 0], clamp)}px)` : undefined,
        opacity: interpolate(frame, [0, 3], [0, 1], clamp),
      }}
    >
      {children}
    </AbsoluteFill>
  );
};

/** Lampo sul taglio (si posiziona come sequenza nel frame del cambio scena). */
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

/** Etichetta grande "pillola" usata nel montaggio. */
export const Pill: React.FC<{ children: React.ReactNode; color?: string; size?: number; style?: React.CSSProperties }> = ({
  children,
  color = C.orange,
  size = 60,
  style,
}) => (
  <div
    style={{
      padding: `${size * 0.36}px ${size * 0.8}px`,
      borderRadius: 999,
      background: color,
      boxShadow: `0 20px 60px ${color}AA`,
      fontFamily: FONT_TITLE,
      fontWeight: 800,
      fontSize: size,
      color: "#FFFFFF",
      whiteSpace: "nowrap",
      ...style,
    }}
  >
    {children}
  </div>
);
