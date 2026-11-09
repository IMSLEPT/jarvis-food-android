import React from "react";
import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame } from "remotion";
import { AppScreen } from "../components/AppScreen";
import { Callout } from "../components/Callout";
import { C } from "../theme";

/**
 * Scena 5 (18–22 s) · Digiuno intermittente, filmato reale rallentato (0,75×) per coprire la scena.
 * Fasi del digiuno sui beat 72/132/192.
 */
export const Fasting: React.FC = () => {
  const frame = useCurrentFrame();
  const ring = interpolate(frame, [10, 250], [0.05, 0.78], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.33, 0, 0.2, 1),
  });
  const R = 470;
  const circ = 2 * Math.PI * R;

  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      <Interactive.Div
        name="Titolo digiuno"
        style={{
          position: "absolute",
          top: 130,
          width: 960,
          textAlign: "center",
          fontFamily: "Bricolage",
          fontWeight: 800,
          fontSize: 96,
          lineHeight: 1.02,
          color: "#FFFFFF",
          opacity: interpolate(frame, [10, 26], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          translate: interpolate(frame, [10, 40], ["0px -40px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        Digiuno guidato,
      </Interactive.Div>
      <Interactive.Div
        name="Titolo digiuno 2"
        style={{
          position: "absolute",
          top: 232,
          width: 960,
          textAlign: "center",
          fontFamily: "Bricolage",
          fontWeight: 800,
          fontSize: 96,
          lineHeight: 1.02,
          color: "#B79CFF",
          opacity: interpolate(frame, [20, 36], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          translate: interpolate(frame, [20, 50], ["0px -40px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        fase per fase.
      </Interactive.Div>

      {/* Anello viola di avanzamento dietro il telefono */}
      <svg
        width={1080}
        height={1080}
        viewBox="0 0 1080 1080"
        style={{
          position: "absolute",
          top: 560,
          rotate: `${-90 + frame * 0.15}deg`,
          opacity: interpolate(frame, [0, 30], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        <defs>
          <linearGradient id="fastGrad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor={C.fasting} />
            <stop offset="100%" stopColor="#E879F9" />
          </linearGradient>
        </defs>
        <circle cx={540} cy={540} r={R} stroke="rgba(255,255,255,0.08)" strokeWidth={34} fill="none" />
        <circle
          cx={540}
          cy={540}
          r={R}
          stroke="url(#fastGrad)"
          strokeWidth={34}
          strokeLinecap="round"
          fill="none"
          strokeDasharray={circ}
          strokeDashoffset={circ * (1 - ring)}
          style={{ filter: `drop-shadow(0 0 24px ${C.fasting})` }}
        />
      </svg>

      <div
        style={{
          position: "absolute",
          top: 430,
          scale: `${interpolate(frame, [0, 46], [0.7, 0.86], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.spring({ damping: 14 }),
          })}`,
          opacity: interpolate(frame, [0, 20], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          transformOrigin: "50% 0%",
        }}
      >
        <AppScreen clip="fasting" playbackRate={0.75} />
      </div>

      <Callout at={72} x={30} y={820} side="left" accent={C.fasting} title="Digestione" subtitle="0–4 h" />
      <Callout at={132} x={420} y={1120} side="right" accent="#C084FC" title="Grassi come energia" subtitle="dalle 12 h" />
      <Callout at={192} x={40} y={1420} side="left" accent="#E879F9" title="Promemoria" subtitle="a fine digiuno" />
    </AbsoluteFill>
  );
};
