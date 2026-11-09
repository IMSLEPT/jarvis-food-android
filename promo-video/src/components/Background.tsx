import React from "react";
import { AbsoluteFill, useCurrentFrame, useVideoConfig } from "remotion";
import { BEAT, C } from "../theme";

/**
 * Sfondo continuo sotto tutte le scene: gradiente navy, "aurora" di luci morbide in movimento,
 * griglia prospettica e un respiro di luce sincronizzato con la cassa (dal drop a 4 s).
 */
export const Background: React.FC<{ pulse?: number; saturation?: number }> = ({ pulse: pulseIn, saturation = 1 }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const t = frame / fps;

  // Impulso sul beat: picco sul colpo, decadimento in ~0,4 s. Attivo tra 4 s e 26 s.
  const beatPhase = (frame % BEAT) / BEAT;
  const groove = frame >= 4 * fps && frame < 26 * fps ? 1 : 0;
  const pulse = pulseIn ?? groove * Math.pow(1 - beatPhase, 3);

  const blob = (
    color: string,
    size: number,
    x: number,
    y: number,
    speed: number,
    phase: number,
    opacity: number,
  ) => (
    <div
      style={{
        position: "absolute",
        width: size,
        height: size,
        left: x + Math.sin(t * speed + phase) * 140 - size / 2,
        top: y + Math.cos(t * speed * 0.8 + phase) * 180 - size / 2,
        borderRadius: "50%",
        background: `radial-gradient(circle, ${color} 0%, ${color}00 68%)`,
        opacity,
        filter: "blur(30px)",
      }}
    />
  );

  return (
    <AbsoluteFill
      style={{
        background: `radial-gradient(120% 80% at 50% 30%, ${C.navyBright} 0%, ${C.navyDeep} 45%, ${C.bgDeep} 100%)`,
        overflow: "hidden",
        filter: saturation < 1 ? `saturate(${saturation}) brightness(${0.7 + 0.3 * saturation})` : undefined,
      }}
    >
      {blob(C.orange, 1300, 900, 300, 0.35, 0, 0.42 + pulse * 0.18)}
      {blob(C.fasting, 1100, 150, 1250, 0.28, 2, 0.32)}
      {blob(C.carbs, 1000, 950, 1700, 0.31, 4, 0.30)}
      {blob(C.gold, 700, 200, 450, 0.4, 1, 0.18 + pulse * 0.1)}

      {/* Griglia prospettica che scorre verso lo spettatore */}
      <AbsoluteFill style={{ perspective: 900, opacity: 0.22 }}>
        <div
          style={{
            position: "absolute",
            left: -1000,
            right: -1000,
            top: 1150,
            height: 1800,
            transform: "rotateX(72deg)",
            transformOrigin: "50% 0%",
            backgroundImage:
              "linear-gradient(rgba(255,255,255,0.55) 2px, transparent 2px), linear-gradient(90deg, rgba(255,255,255,0.55) 2px, transparent 2px)",
            backgroundSize: "120px 120px",
            backgroundPosition: `0px ${(frame * 2) % 120}px`,
            maskImage: "linear-gradient(to bottom, black 0%, transparent 70%)",
            WebkitMaskImage: "linear-gradient(to bottom, black 0%, transparent 70%)",
          }}
        />
      </AbsoluteFill>

      {/* Particelle luminose che salgono */}
      {new Array(28).fill(0).map((_, i) => {
        const seed = (i * 9301 + 49297) % 233280;
        const x = (seed / 233280) * 1080;
        const speed = 40 + (i % 7) * 18;
        const y = 1920 - ((t * speed + i * 137) % 2100);
        const s = 4 + (i % 4) * 3;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: x,
              top: y,
              width: s,
              height: s,
              borderRadius: "50%",
              background: i % 3 === 0 ? C.orangeLight : "#FFFFFF",
              opacity: 0.25 + 0.35 * Math.abs(Math.sin(t * 1.3 + i)),
              boxShadow: `0 0 ${s * 3}px ${i % 3 === 0 ? C.orange : "#9fb4ff"}`,
            }}
          />
        );
      })}

      {/* Vignettatura */}
      <AbsoluteFill
        style={{
          background: "radial-gradient(90% 70% at 50% 45%, transparent 55%, rgba(2,4,18,0.75) 100%)",
        }}
      />
    </AbsoluteFill>
  );
};
