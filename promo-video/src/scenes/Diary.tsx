import React from "react";
import { AbsoluteFill, Easing, Interactive, interpolate, useCurrentFrame } from "remotion";
import { AppScreen } from "../components/AppScreen";
import { Callout } from "../components/Callout";
import { C } from "../theme";

/**
 * Scena 3 (8–14 s) · Diario reale dell'app: Home → calorie → diario dei pasti.
 * Downbeat locali: 12, 42, 72 … (ogni 30 frame). Le etichette escono sui colpi.
 */
export const Diary: React.FC = () => {
  const frame = useCurrentFrame();

  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      <Interactive.Div
        name="Titolo diario"
        style={{
          position: "absolute",
          top: 130,
          width: 920,
          textAlign: "center",
          fontFamily: "Bricolage",
          fontWeight: 800,
          fontSize: 96,
          lineHeight: 1.02,
          color: "#FFFFFF",
          opacity: interpolate(frame, [10, 28], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          translate: interpolate(frame, [10, 40], ["0px -40px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        Il diario che
      </Interactive.Div>
      <Interactive.Div
        name="Titolo diario 2"
        style={{
          position: "absolute",
          top: 232,
          width: 920,
          textAlign: "center",
          fontFamily: "Bricolage",
          fontWeight: 800,
          fontSize: 96,
          lineHeight: 1.02,
          color: "#FF8A1C",
          opacity: interpolate(frame, [20, 38], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          translate: interpolate(frame, [20, 50], ["0px -40px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        ti capisce.
      </Interactive.Div>

      <div
        style={{
          position: "absolute",
          top: 420,
          perspective: 2000,
        }}
      >
        <div
          style={{
            translate: `0px ${interpolate(frame, [0, 50], [900, 0], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.spring({ damping: 16 }),
            }) + Math.sin(frame / 40) * 8}px`,
            transform: `rotateY(${interpolate(frame, [0, 60, 384], [-18, -6, 6], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            })}deg) rotateX(${interpolate(frame, [0, 60], [14, 3], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            })}deg)`,
            scale: "0.94",
            transformOrigin: "50% 0%",
          }}
        >
          <AppScreen clip="diary" />
        </div>
      </div>

      <Callout at={72} x={34} y={760} side="left" accent={C.orange} title="1.629 kcal" subtitle="ancora disponibili oggi" out={230} />
      <Callout at={132} x={500} y={980} side="right" accent={C.protein} title="Macro al grammo" subtitle="proteine · carbo · grassi" out={290} />
      <Callout at={252} x={30} y={1230} side="left" accent={C.green} title="Ogni pasto" subtitle="colazione → cena, in un tocco" />
      <Callout at={312} x={470} y={1480} side="right" accent={C.carbs} title="Barcode e AI" subtitle="Open Food Facts integrato" />
    </AbsoluteFill>
  );
};
