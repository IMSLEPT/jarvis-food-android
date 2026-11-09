import React from "react";
import { AbsoluteFill, Easing, Img, Interactive, interpolate, staticFile, useCurrentFrame } from "remotion";
import { C } from "../theme";

/**
 * Scena 1 (0–4 s) · Intro sul riser: il logo si accende, il nome si compone lettera per lettera.
 * Tempo locale = tempo assoluto. Kick del build-up a 120/150/180/210, drop a 240.
 */
export const Intro: React.FC = () => {
  const frame = useCurrentFrame();

  // Piccolo "colpo" del logo su ogni kick del build-up (2,0 · 2,5 · 3,0 · 3,5 s)
  const kick = [120, 150, 180, 210].reduce((acc, k) => {
    const d = frame - k;
    return d >= 0 && d < 24 ? acc + Math.exp(-d / 5) * 0.05 : acc;
  }, 0);

  const title = "JARVIS FOOD";

  return (
    <AbsoluteFill style={{ justifyContent: "center", alignItems: "center" }}>
      {/* Onde di luce dietro il logo */}
      {[0, 1, 2].map((i) => {
        const p = interpolate(frame, [10 + i * 14, 110 + i * 14], [0, 1], {
          extrapolateLeft: "clamp",
          extrapolateRight: "clamp",
          easing: Easing.bezier(0.16, 1, 0.3, 1),
        });
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              top: 560,
              width: 900,
              height: 900,
              borderRadius: "50%",
              border: `${4 - i}px solid ${i === 1 ? C.gold : C.orange}`,
              scale: `${0.3 + p * 1.1}`,
              opacity: (1 - p) * 0.8,
              boxShadow: `0 0 60px ${C.orange}`,
            }}
          />
        );
      })}

      <AbsoluteFill
        style={{
          justifyContent: "center",
          alignItems: "center",
          flexDirection: "column",
          // anticipo prima del drop
          scale: `${interpolate(frame, [190, 240], [1, 1.08], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.55, 0, 1, 0.45),
          })}`,
        }}
      >
        {/* Alone arancione */}
        <div
          style={{
            position: "absolute",
            top: 470,
            width: 760,
            height: 760,
            borderRadius: "50%",
            background: `radial-gradient(circle, ${C.orange}AA 0%, ${C.orange}00 65%)`,
            opacity: interpolate(frame, [20, 90], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            scale: `${1 + kick * 3}`,
          }}
        />

        <div style={{ scale: `${1 + kick}`, marginTop: -140 }}>
          <Img
            name="Logo"
            src={staticFile("jarvis_logo.png")}
            style={{
              width: 520,
              height: 520,
              borderRadius: "50%",
              boxShadow: "0 30px 90px rgba(0,0,0,0.6), 0 0 120px rgba(242,94,0,0.55)",
              opacity: interpolate(frame, [12, 40], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
              scale: interpolate(frame, [12, 80], [0.2, 1], {
                extrapolateLeft: "clamp",
                extrapolateRight: "clamp",
                easing: Easing.spring({ damping: 12 }),
                output: "perceptual-scale",
              }),
              rotate: interpolate(frame, [12, 80], ["-30deg", "0deg"], {
                extrapolateLeft: "clamp",
                extrapolateRight: "clamp",
                easing: Easing.spring({ damping: 14 }),
              }),
            }}
          />
        </div>

        <Interactive.Div
          name="Titolo"
          style={{
            marginTop: 90,
            fontFamily: "Bricolage",
            fontWeight: 800,
            fontSize: 128,
            letterSpacing: 4,
            color: "#FFFFFF",
            display: "flex",
            textShadow: "0 10px 40px rgba(0,0,0,0.45)",
          }}
        >
          {title.split("").map((ch, i) => {
            const start = 70 + i * 4;
            return (
              <span
                key={i}
                style={{
                  display: "inline-block",
                  whiteSpace: "pre",
                  color: i >= 7 ? C.orangeLight : "#FFFFFF",
                  opacity: interpolate(frame, [start, start + 14], [0, 1], {
                    extrapolateLeft: "clamp",
                    extrapolateRight: "clamp",
                  }),
                  translate: `0px ${interpolate(frame, [start, start + 26], [80, 0], {
                    extrapolateLeft: "clamp",
                    extrapolateRight: "clamp",
                    easing: Easing.spring({ damping: 13 }),
                  })}px`,
                  filter: `blur(${interpolate(frame, [start, start + 16], [12, 0], {
                    extrapolateLeft: "clamp",
                    extrapolateRight: "clamp",
                  })}px)`,
                }}
              >
                {ch}
              </span>
            );
          })}
        </Interactive.Div>

        <Interactive.Div
          name="Sottotitolo"
          style={{
            marginTop: 26,
            fontFamily: "DM Sans",
            fontWeight: 500,
            fontSize: 50,
            color: "rgba(255,255,255,0.82)",
            letterSpacing: 1,
            opacity: interpolate(frame, [125, 160], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            translate: interpolate(frame, [125, 165], ["0px 30px", "0px 0px"], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
          }}
        >
          Il tuo diario alimentare intelligente
        </Interactive.Div>

        {/* Linea luminosa che si allarga sotto il titolo */}
        <div
          style={{
            marginTop: 44,
            height: 6,
            borderRadius: 3,
            width: interpolate(frame, [140, 200], [0, 420], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
            background: `linear-gradient(90deg, transparent, ${C.orange}, ${C.gold}, transparent)`,
            boxShadow: `0 0 24px ${C.orange}`,
          }}
        />
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
