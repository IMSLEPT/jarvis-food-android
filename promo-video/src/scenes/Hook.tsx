import React from "react";
import { AbsoluteFill, Easing, Img, Interactive, interpolate, staticFile, useCurrentFrame } from "remotion";
import { C } from "../theme";

const COLUMNS = [
  ["spaghetti_pomodoro", "poke_salmone", "pancake_avena", "salmone_patate"],
  ["toast_uova_avocado", "smoothie_bowl", "pasta_pesto", "risotto_zucca"],
  ["insalata_pollo_patate", "yogurt_granola", "tagliata_rucola", "burger_patate"],
];

/**
 * Scena 2 (4–8 s) · Il drop: parete di piatti che scorre e tipografia cinetica sui beat.
 * Downbeat locali: 12, 42, 72, 102, 132, 162, 192, 222.
 */
export const Hook: React.FC = () => {
  const frame = useCurrentFrame();

  // Parola che "atterra" sul beat: scala, blur e opacità
  const slam = (start: number) => ({
    opacity: interpolate(frame, [start - 4, start + 4], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
    scale: `${interpolate(frame, [start - 4, start + 16], [1.7, 1], {
      extrapolateLeft: "clamp",
      extrapolateRight: "clamp",
      easing: Easing.bezier(0.16, 1, 0.3, 1),
    })}`,
    filter: `blur(${interpolate(frame, [start - 4, start + 10], [18, 0], {
      extrapolateLeft: "clamp",
      extrapolateRight: "clamp",
    })}px)`,
  });

  return (
    <AbsoluteFill>
      {/* Parete di foto inclinata, colonne in direzioni opposte */}
      <AbsoluteFill
        style={{
          rotate: "-10deg",
          scale: "1.35",
          display: "flex",
          flexDirection: "row",
          justifyContent: "center",
          gap: 28,
          opacity: interpolate(frame, [0, 24], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        {COLUMNS.map((col, ci) => (
          <div
            key={ci}
            style={{
              display: "flex",
              flexDirection: "column",
              gap: 28,
              marginTop: -300,
              translate: `0px ${(ci % 2 === 0 ? -1 : 1) * frame * 1.6 + (ci === 1 ? -260 : 0)}px`,
            }}
          >
            {[...col, ...col].map((f, i) => (
              <Img
                key={i}
                src={staticFile(`food/${f}.webp`)}
                style={{ width: 380, height: 480, objectFit: "cover", borderRadius: 44 }}
              />
            ))}
          </div>
        ))}
      </AbsoluteFill>

      {/* Velo scuro per la leggibilità */}
      <AbsoluteFill
        style={{
          background:
            "linear-gradient(180deg, rgba(6,11,38,0.55) 0%, rgba(6,11,38,0.86) 38%, rgba(6,11,38,0.86) 62%, rgba(6,11,38,0.6) 100%)",
        }}
      />

      <AbsoluteFill
        style={{
          justifyContent: "center",
          alignItems: "center",
          flexDirection: "column",
          fontFamily: "Bricolage",
          fontWeight: 800,
          color: "#FFFFFF",
          lineHeight: 1.0,
          textAlign: "center",
          textShadow: "0 12px 50px rgba(0,0,0,0.6)",
        }}
      >
        <div style={{ fontSize: 210, ...slam(12) }}>Ogni</div>
        <div style={{ fontSize: 210, ...slam(42) }}>pasto</div>
        <div style={{ fontSize: 210, color: C.orangeLight, ...slam(72) }}>conta.</div>

        <Interactive.Div
          name="Claim"
          style={{
            marginTop: 90,
            fontFamily: "DM Sans",
            fontWeight: 600,
            fontSize: 64,
            lineHeight: 1.2,
            color: "#FFFFFF",
            opacity: interpolate(frame, [132, 150], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            translate: interpolate(frame, [132, 162], ["0px 50px", "0px 0px"], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.bezier(0.16, 1, 0.3, 1),
            }),
          }}
        >
          Jarvis lo rende
        </Interactive.Div>

        {/* "semplice." con evidenziatore che si stende sul beat */}
        <div style={{ position: "relative", marginTop: 14, fontSize: 104 }}>
          <div
            style={{
              position: "absolute",
              left: -24,
              right: -24,
              top: "30%",
              height: "58%",
              borderRadius: 16,
              background: `linear-gradient(90deg, ${C.orange}, ${C.orangeLight})`,
              transformOrigin: "0% 50%",
              scale: `${interpolate(frame, [164, 190], [0, 1], {
                extrapolateLeft: "clamp",
                extrapolateRight: "clamp",
                easing: Easing.bezier(0.65, 0, 0.35, 1),
              })} 1`,
              rotate: "-2deg",
            }}
          />
          <div style={{ position: "relative", ...slam(162) }}>semplice.</div>
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};
