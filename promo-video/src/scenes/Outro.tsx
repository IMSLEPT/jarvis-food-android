import React from "react";
import { AbsoluteFill, Easing, Img, Interactive, interpolate, staticFile, useCurrentFrame } from "remotion";
import { C } from "../theme";

/**
 * Scena 7 (26–30 s) · Impatto finale sul downbeat (frame locale 12), logo, claim e call to action.
 */
export const Outro: React.FC = () => {
  const frame = useCurrentFrame();

  return (
    <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", flexDirection: "column" }}>
      {/* Onda d'urto dell'impatto */}
      {[0, 1].map((i) => {
        const p = interpolate(frame, [12 + i * 8, 80 + i * 8], [0, 1], {
          extrapolateLeft: "clamp",
          extrapolateRight: "clamp",
          easing: Easing.bezier(0.16, 1, 0.3, 1),
        });
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              top: 360,
              width: 600,
              height: 600,
              borderRadius: "50%",
              border: `${10 - i * 4}px solid ${i ? C.gold : C.orange}`,
              scale: `${0.6 + p * 2.4}`,
              opacity: (1 - p) * 0.9,
            }}
          />
        );
      })}

      {/* Raggi di luce rotanti */}
      <div
        style={{
          position: "absolute",
          top: 60,
          width: 1200,
          height: 1200,
          borderRadius: "50%",
          background: `repeating-conic-gradient(from 0deg, ${C.orange}33 0deg 6deg, transparent 6deg 24deg)`,
          maskImage: "radial-gradient(circle, black 0%, transparent 62%)",
          WebkitMaskImage: "radial-gradient(circle, black 0%, transparent 62%)",
          rotate: `${frame * 0.25}deg`,
          opacity: interpolate(frame, [12, 40], [0, 0.9], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      />

      <Img
        name="Logo finale"
        src={staticFile("jarvis_logo.png")}
        style={{
          position: "absolute",
          top: 420,
          width: 480,
          height: 480,
          borderRadius: "50%",
          boxShadow: "0 30px 90px rgba(0,0,0,0.6), 0 0 160px rgba(242,94,0,0.7)",
          scale: interpolate(frame, [8, 50], [2.4, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.spring({ damping: 13 }),
            output: "perceptual-scale",
          }),
          opacity: interpolate(frame, [8, 18], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      />

      <Interactive.Div
        name="Nome app"
        style={{
          position: "absolute",
          top: 980,
          fontFamily: "Bricolage",
          fontWeight: 800,
          fontSize: 160,
          letterSpacing: 2,
          color: "#FFFFFF",
          textShadow: "0 12px 50px rgba(0,0,0,0.5)",
          opacity: interpolate(frame, [30, 46], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          translate: interpolate(frame, [30, 60], ["0px 60px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.spring({ damping: 14 }),
          }),
        }}
      >
        Jarvis Food
      </Interactive.Div>

      <Interactive.Div
        name="Claim finale"
        style={{
          position: "absolute",
          top: 1190,
          fontFamily: "DM Sans",
          fontWeight: 500,
          fontSize: 60,
          color: "rgba(255,255,255,0.9)",
          opacity: interpolate(frame, [54, 72], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          translate: interpolate(frame, [54, 84], ["0px 30px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        Mangia meglio, ogni giorno.
      </Interactive.Div>

      {/* Call to action con riflesso che scorre */}
      <div
        style={{
          position: "absolute",
          top: 1380,
          padding: "34px 70px",
          borderRadius: 999,
          overflow: "hidden",
          background: `linear-gradient(180deg, #C94D00, #BA4000)`,
          boxShadow: "0 24px 70px rgba(242,94,0,0.6)",
          fontFamily: "Bricolage",
          fontWeight: 800,
          fontSize: 62,
          color: "#FFFFFF",
          scale: `${interpolate(frame, [78, 100], [0.6, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.spring({ damping: 10 }),
          })}`,
          opacity: interpolate(frame, [78, 88], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        Disponibile per Android
        <div
          style={{
            position: "absolute",
            top: 0,
            bottom: 0,
            width: 160,
            left: interpolate(frame, [110, 160], [-200, 900], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
            background: "linear-gradient(90deg, transparent, rgba(255,255,255,0.55), transparent)",
            rotate: "20deg",
          }}
        />
      </div>

      <Interactive.Div
        name="Riepilogo"
        style={{
          position: "absolute",
          top: 1580,
          fontFamily: "DM Sans",
          fontWeight: 600,
          fontSize: 44,
          letterSpacing: 2,
          color: "rgba(255,255,255,0.7)",
          opacity: interpolate(frame, [100, 120], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        Diario · AI · Digiuno · Ricette
      </Interactive.Div>
    </AbsoluteFill>
  );
};
