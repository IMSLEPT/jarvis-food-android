import React from "react";
import { Easing, interpolate, useCurrentFrame } from "remotion";
import { FONT_BODY, FONT_TITLE } from "../theme";

/**
 * Etichetta "vetro" che esce dal telefono sul beat (`at` = frame locale del colpo).
 * `side` indica da quale lato entra; `x`, `y` sono la posizione finale nel canvas.
 */
export const Callout: React.FC<{
  at: number;
  x: number;
  y: number;
  side: "left" | "right";
  accent: string;
  title: string;
  subtitle?: string;
  out?: number;
}> = ({ at, x, y, side, accent, title, subtitle, out }) => {
  const frame = useCurrentFrame();
  const p = interpolate(frame, [at - 3, at + 22], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.spring({ damping: 11 }),
  });
  const o = interpolate(frame, [at - 3, at + 6], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const exit =
    out === undefined
      ? 1
      : interpolate(frame, [out, out + 12], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const dir = side === "left" ? -1 : 1;
  // leggero galleggiamento dopo l'ingresso
  const float = Math.sin((frame - at) / 22) * 6;

  return (
    <div
      style={{
        position: "absolute",
        left: x,
        top: y,
        opacity: o * exit,
        translate: `${(1 - p) * -dir * 160}px ${float}px`,
        scale: `${0.6 + 0.4 * p}`,
        display: "flex",
        alignItems: "center",
        gap: 20,
        padding: "22px 34px 22px 22px",
        borderRadius: 36,
        background: "rgba(255,255,255,0.94)",
        boxShadow: `0 24px 60px rgba(0,0,0,0.35), 0 0 0 3px ${accent}33`,
      }}
    >
      <div
        style={{
          width: 64,
          height: 64,
          borderRadius: 20,
          background: `linear-gradient(140deg, ${accent}, ${accent}BB)`,
          boxShadow: `0 8px 24px ${accent}88`,
          flexShrink: 0,
        }}
      />
      <div style={{ display: "flex", flexDirection: "column" }}>
        <div style={{ fontFamily: FONT_TITLE, fontWeight: 800, fontSize: 46, color: "#1D1B3A", lineHeight: 1.05 }}>
          {title}
        </div>
        {subtitle ? (
          <div style={{ fontFamily: FONT_BODY, fontWeight: 500, fontSize: 30, color: "#5E5B7A", marginTop: 4 }}>
            {subtitle}
          </div>
        ) : null}
      </div>
    </div>
  );
};
