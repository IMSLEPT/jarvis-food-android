import React from "react";
import { AbsoluteFill, Easing, interpolate, useCurrentFrame } from "remotion";
import { C, FONT_BODY, FONT_TITLE } from "../../theme";
import { Words } from "../Kinetic";
import { useSceneStart, useWords } from "../tl";

const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const SLATE = "#9AA3C7";

export const Person: React.FC<{ size: number; color: string; glow?: boolean }> = ({ size, color, glow }) => (
  <svg width={size} height={size * 1.15} viewBox="0 0 100 115" style={glow ? { filter: `drop-shadow(0 0 30px ${color})` } : undefined}>
    <circle cx="50" cy="30" r="24" fill={color} />
    <path d="M8 112 C8 74 28 62 50 62 C72 62 92 74 92 112 Z" fill={color} />
  </svg>
);

/** Posizioni della griglia del "team" (4×3), condivise con la scena "una persona". */
export const TEAM = new Array(12).fill(0).map((_, i) => ({ x: (i % 4) * 200 - 300, y: Math.floor(i / 4) * 230 - 230 }));

// ─────────────────────────────────────────────────────────────────────────────
/** "Una volta, per creare un'app servivano un team intero, mesi di lavoro... e budget da capogiro." */
export const Yesterday: React.FC = () => {
  const frame = useCurrentFrame();
  const s0 = useSceneStart();
  const w = useWords("L01");
  const tTeam = w[6].start;
  const tMesi = w[9].start;
  const tBudget = w[12].start;
  const seg = (a: number, b: number) =>
    interpolate(frame, [a - 4, a + 4, b - 4, b + 2], [0, 1, 1, 0], clamp);
  const segLast = (a: number) => interpolate(frame, [a - 4, a + 4], [0, 1], clamp);
  void s0;

  return (
    <AbsoluteFill
      style={{
        alignItems: "center",
        // lenta spinta di camera
        scale: `${interpolate(frame, [0, 480], [1, 1.06])}`,
      }}
    >
      <div style={{ position: "absolute", top: 210 }}>
        <Words line="L01" to={6} size={74} color={SLATE} variant="rise" accent={{ 0: "#FFFFFF", 1: "#FFFFFF" }} />
      </div>

      {/* 1 · un team intero */}
      <AbsoluteFill style={{ alignItems: "center", opacity: seg(tTeam, tMesi) }}>
        <div style={{ position: "absolute", top: 560 }}>
          <Words line="L01" from={6} to={9} size={112} />
        </div>
        <div style={{ position: "absolute", top: 1180, left: 540 }}>
          {TEAM.map((p, i) => {
            const s = tTeam + 4 + i * 2;
            return (
              <div
                key={i}
                style={{
                  position: "absolute",
                  left: p.x - 70,
                  top: p.y - 80,
                  opacity: interpolate(frame, [s, s + 5], [0, 1], clamp),
                  scale: `${interpolate(frame, [s, s + 12], [0.3, 1], { ...clamp, easing: Easing.spring({ damping: 10 }) })}`,
                }}
              >
                <Person size={140} color={i % 3 === 0 ? "#6B74A0" : "#4B5585"} />
              </div>
            );
          })}
        </div>
      </AbsoluteFill>

      {/* 2 · mesi di lavoro: calendario che sfoglia i mesi */}
      <AbsoluteFill style={{ alignItems: "center", opacity: seg(tMesi, tBudget) }}>
        <div style={{ position: "absolute", top: 560 }}>
          <Words line="L01" from={9} to={12} size={112} />
        </div>
        {(() => {
          const months = ["GEN", "FEB", "MAR", "APR", "MAG", "GIU", "LUG", "AGO", "SET", "OTT", "NOV", "DIC"];
          const k = Math.max(0, Math.min(11, Math.floor((frame - tMesi) / 4)));
          const flip = ((frame - tMesi) % 4) / 4;
          return (
            <div
              style={{
                position: "absolute",
                top: 860,
                width: 520,
                height: 560,
                borderRadius: 48,
                background: "#E8EBF5",
                overflow: "hidden",
                boxShadow: "0 40px 100px rgba(0,0,0,0.5)",
                perspective: 900,
              }}
            >
              <div style={{ height: 130, background: "#5B6492", display: "flex", justifyContent: "center", gap: 120, alignItems: "center" }}>
                {[0, 1].map((r) => (
                  <div key={r} style={{ width: 26, height: 70, borderRadius: 13, background: "#C9CEE3", marginTop: -60 }} />
                ))}
              </div>
              <div
                style={{
                  height: 430,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontFamily: FONT_TITLE,
                  fontWeight: 800,
                  fontSize: 190,
                  color: "#2A3160",
                  transform: k < 11 ? `rotateX(${flip * 70}deg)` : undefined,
                  transformOrigin: "50% 0%",
                }}
              >
                {months[k]}
              </div>
            </div>
          );
        })()}
      </AbsoluteFill>

      {/* 3 · budget da capogiro: colonne di € che salgono */}
      <AbsoluteFill style={{ alignItems: "center", opacity: segLast(tBudget) }}>
        <div style={{ position: "absolute", top: 560 }}>
          <Words line="L01" from={12} size={112} accent={{ 13: "#FF6B6B", 15: "#FF6B6B" }} />
        </div>
        <div style={{ position: "absolute", top: 900, height: 620, display: "flex", alignItems: "flex-end", gap: 34 }}>
          {[0.35, 0.55, 0.72, 0.88, 1].map((h, i) => {
            const s = tBudget + 6 + i * 4;
            const p = interpolate(frame, [s, s + 26], [0, h], { ...clamp, easing: Easing.bezier(0.16, 1, 0.3, 1) });
            return (
              <div key={i} style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                <div
                  style={{
                    fontFamily: FONT_TITLE,
                    fontWeight: 800,
                    fontSize: 70,
                    color: "#FF8A8A",
                    opacity: interpolate(frame, [s + 10, s + 16], [0, 1], clamp),
                  }}
                >
                  €
                </div>
                <div
                  style={{
                    width: 130,
                    height: 540 * p,
                    borderRadius: 24,
                    background: "linear-gradient(180deg, #FF6B6B, #8B2E4A)",
                    boxShadow: "0 0 40px rgba(255,90,90,0.35)",
                  }}
                />
              </div>
            );
          })}
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Oggi, no." — il drop. */
export const Today: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L02");
  const shake = Math.exp(-frame / 6) * 18;
  return (
    <AbsoluteFill
      style={{
        alignItems: "center",
        justifyContent: "center",
        translate: `${Math.sin(frame * 2.1) * shake}px ${Math.cos(frame * 1.7) * shake}px`,
      }}
    >
      {[0, 1, 2].map((i) => {
        const p = interpolate(frame, [i * 5, 50 + i * 5], [0, 1], { ...clamp, easing: Easing.bezier(0.16, 1, 0.3, 1) });
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              width: 700,
              height: 700,
              borderRadius: "50%",
              border: `${10 - i * 3}px solid ${i === 1 ? C.gold : C.orange}`,
              scale: `${0.3 + p * 2}`,
              opacity: (1 - p) * 0.9,
            }}
          />
        );
      })}
      <Words line="L02" size={250} variant="slam" accent={{ 1: C.orangeLight }} lineHeight={0.95} width={1000} />
      <div
        style={{
          position: "absolute",
          top: 1330,
          fontFamily: FONT_BODY,
          fontWeight: 600,
          fontSize: 46,
          letterSpacing: 8,
          color: "rgba(255,255,255,0.75)",
          opacity: interpolate(frame, [w[1].end, w[1].end + 10], [0, 1], clamp),
        }}
      >
        LE REGOLE SONO CAMBIATE
      </div>
    </AbsoluteFill>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Oggi basta una persona." — il team collassa in una sola persona. */
export const OnePerson: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L03");
  const tP = w[3].start; // "persona."
  const merge = interpolate(frame, [0, tP], [0, 1], { ...clamp, easing: Easing.bezier(0.65, 0, 0.35, 1) });
  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      <div style={{ position: "absolute", top: 230 }}>
        <Words line="L03" size={118} accent={{ 2: C.orangeLight, 3: C.orangeLight }} />
      </div>
      <div style={{ position: "absolute", top: 1130, left: 540 }}>
        {TEAM.map((p, i) => (
          <div
            key={i}
            style={{
              position: "absolute",
              left: p.x * (1 - merge) - 70,
              top: p.y * (1 - merge) - 80,
              opacity: 1 - merge * 0.95,
              scale: `${1 - merge * 0.4}`,
            }}
          >
            <Person size={140} color="#4B5585" />
          </div>
        ))}
        <div
          style={{
            position: "absolute",
            left: -160,
            top: -230,
            opacity: interpolate(frame, [tP - 6, tP + 2], [0, 1], clamp),
            scale: `${interpolate(frame, [tP - 6, tP + 18], [0.4, 1], { ...clamp, easing: Easing.spring({ damping: 9 }) })}`,
          }}
        >
          <Person size={320} color={C.orange} glow />
        </div>
      </div>
      <div
        style={{
          position: "absolute",
          top: 1560,
          fontFamily: FONT_TITLE,
          fontWeight: 800,
          fontSize: 64,
          color: "#FFFFFF",
          opacity: interpolate(frame, [tP + 6, tP + 14], [0, 1], clamp),
        }}
      >
        1 persona
      </div>
    </AbsoluteFill>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Una settimana." — sette giorni che si accendono uno dopo l'altro. */
export const OneWeek: React.FC = () => {
  const frame = useCurrentFrame();
  const days = ["L", "M", "M", "G", "V", "S", "D"];
  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      <div style={{ position: "absolute", top: 380 }}>
        <Words line="L04" size={150} variant="slam" accent={{ 1: C.orangeLight }} />
      </div>
      <div style={{ position: "absolute", top: 860, display: "flex", gap: 22 }}>
        {days.map((d, i) => {
          const s = 8 + i * 6;
          const on = interpolate(frame, [s, s + 4], [0, 1], clamp);
          return (
            <div
              key={i}
              style={{
                width: 116,
                height: 190,
                borderRadius: 34,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontFamily: FONT_TITLE,
                fontWeight: 800,
                fontSize: 64,
                color: on > 0.5 ? "#FFFFFF" : "rgba(255,255,255,0.4)",
                background: on > 0.5 ? `linear-gradient(180deg, ${C.orangeLight}, ${C.orange})` : "rgba(255,255,255,0.08)",
                boxShadow: on > 0.5 ? `0 0 40px ${C.orange}99` : "none",
                scale: `${1 + (1 - on) * 0 + Math.exp(-Math.max(0, frame - s) / 4) * 0.15 * on}`,
                border: "2px solid rgba(255,255,255,0.15)",
              }}
            >
              {d}
            </div>
          );
        })}
      </div>
      <div
        style={{
          position: "absolute",
          top: 1160,
          fontFamily: FONT_TITLE,
          fontWeight: 800,
          fontSize: 230,
          color: "#FFFFFF",
          lineHeight: 1,
          opacity: interpolate(frame, [50, 58], [0, 1], clamp),
          scale: `${interpolate(frame, [50, 66], [1.5, 1], { ...clamp, easing: Easing.bezier(0.16, 1, 0.3, 1) })}`,
        }}
      >
        7<span style={{ fontSize: 90, color: C.orangeLight }}> giorni</span>
      </div>
    </AbsoluteFill>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "E un costo quasi pari a zero." — contatore che crolla a zero. */
export const ZeroCost: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L05");
  const tZero = w[w.length - 1].start;
  const v = interpolate(frame, [4, tZero + 4], [100000, 0], { ...clamp, easing: Easing.bezier(0.7, 0, 0.84, 0) });
  const done = frame >= tZero + 4;
  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      <div style={{ position: "absolute", top: 280 }}>
        <Words line="L05" size={112} accent={{ 4: C.orangeLight, 5: C.orangeLight, 6: C.orangeLight }} />
      </div>
      <div
        style={{
          position: "absolute",
          top: 820,
          fontFamily: FONT_TITLE,
          fontWeight: 800,
          fontSize: done ? 330 : 190,
          lineHeight: 1,
          color: done ? C.orangeLight : "rgba(255,255,255,0.85)",
          fontVariantNumeric: "tabular-nums",
          textShadow: done ? `0 0 80px ${C.orange}` : "none",
          scale: `${done ? interpolate(frame, [tZero + 4, tZero + 18], [1.4, 1], { ...clamp, easing: Easing.spring({ damping: 9 }) }) : 1}`,
        }}
      >
        {done ? "≈ 0 €" : `${Math.round(v).toLocaleString("it-IT")} €`}
      </div>
      <div
        style={{
          position: "absolute",
          top: 1260,
          fontFamily: FONT_BODY,
          fontWeight: 600,
          fontSize: 48,
          color: "rgba(255,255,255,0.7)",
          opacity: interpolate(frame, [tZero + 10, tZero + 20], [0, 1], clamp),
        }}
      >
        strumenti gratuiti · open source · AI
      </div>
    </AbsoluteFill>
  );
};
