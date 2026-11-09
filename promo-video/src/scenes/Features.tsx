import React from "react";
import { AbsoluteFill, Easing, Interactive, interpolate, Sequence, useCurrentFrame } from "remotion";
import { AppScreen } from "../components/AppScreen";
import { C } from "../theme";

/** Tagli del montaggio: uno ogni 2 beat (60 frame), sul colpo. */
const CUTS = [
  { from: 0, clip: "piano", label: "Piano pasti", color: C.orange },
  { from: 72, clip: "cucina", label: "Ricettario", color: C.green },
  { from: 132, clip: "spesa", label: "Lista della spesa", color: C.carbs },
  { from: 192, clip: "progressi", label: "Progressi", color: C.fasting },
];

/**
 * Scena 6 (22–26 s) · Montaggio rapido a tempo di musica sulle altre sezioni dell'app.
 */
export const Features: React.FC = () => {
  const frame = useCurrentFrame();
  const idx = CUTS.reduce((acc, c, i) => (frame >= c.from ? i : acc), 0);
  const since = frame - CUTS[idx].from;
  // "Punch" sul taglio: il telefono rimbalza e lo schermo lampeggia
  const punch = idx === 0 ? 0 : Math.exp(-since / 6);

  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      <Interactive.Div
        name="Titolo funzioni"
        style={{
          position: "absolute",
          top: 130,
          width: 960,
          textAlign: "center",
          fontFamily: "Bricolage",
          fontWeight: 800,
          fontSize: 104,
          lineHeight: 1.02,
          color: "#FFFFFF",
          opacity: interpolate(frame, [8, 24], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          scale: interpolate(frame, [8, 36], [1.2, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        Tutto in un&apos;app.
      </Interactive.Div>

      <div
        style={{
          position: "absolute",
          top: 330,
          scale: `${0.86 + punch * 0.035}`,
          rotate: `${(idx % 2 === 0 ? -1 : 1) * punch * 1.2}deg`,
          transformOrigin: "50% 0%",
        }}
      >
        {CUTS.map((c, i) => (
          <Sequence
            key={c.clip}
            from={c.from}
            durationInFrames={(CUTS[i + 1]?.from ?? 264) - c.from}
            layout="none"
          >
            <AppScreen clip={c.clip}>
              <div
                style={{
                  position: "absolute",
                  inset: 0,
                  background: "#FFFFFF",
                  opacity: i === 0 ? 0 : Math.max(0, 0.7 - (frame - c.from) / 10),
                }}
              />
            </AppScreen>
          </Sequence>
        ))}
      </div>

      {/* Etichetta che cambia a ogni taglio */}
      <div
        style={{
          position: "absolute",
          top: 1540,
          display: "flex",
          alignItems: "center",
          gap: 22,
          padding: "26px 48px",
          borderRadius: 999,
          background: CUTS[idx].color,
          boxShadow: `0 20px 60px ${CUTS[idx].color}AA`,
          fontFamily: "Bricolage",
          fontWeight: 800,
          fontSize: 64,
          color: "#FFFFFF",
          translate: `0px ${(1 - Math.min(1, since / 10)) * 40}px`,
          scale: `${1 + punch * 0.08}`,
          opacity: interpolate(frame, [12, 22], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        <span
          style={{
            fontFamily: "DM Sans",
            fontWeight: 600,
            fontSize: 40,
            background: "rgba(255,255,255,0.25)",
            borderRadius: 999,
            padding: "4px 18px",
          }}
        >
          {idx + 1}/4
        </span>
        {CUTS[idx].label}
      </div>

      {/* Indicatori di avanzamento */}
      <div style={{ position: "absolute", top: 1700, display: "flex", gap: 16 }}>
        {CUTS.map((c, i) => (
          <div
            key={c.clip}
            style={{
              width: i === idx ? 80 : 22,
              height: 22,
              borderRadius: 11,
              background: i <= idx ? "#FFFFFF" : "rgba(255,255,255,0.3)",
            }}
          />
        ))}
      </div>
    </AbsoluteFill>
  );
};
