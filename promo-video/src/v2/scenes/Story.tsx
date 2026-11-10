import React from "react";
import { AbsoluteFill, Easing, interpolate, useCurrentFrame } from "remotion";
import { FONT_BODY, FONT_TITLE } from "../../theme";
import { P, Pill, Pop, Stage, Words } from "../Kinetic";
import { useWords } from "../tl";

const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const OUT = Easing.bezier(0.16, 1, 0.3, 1);

export const Person: React.FC<{ size: number; color: string }> = ({ size, color }) => (
  <svg width={size} height={size * 1.15} viewBox="0 0 100 115">
    <circle cx="50" cy="30" r="24" fill={color} />
    <path d="M8 112 C8 74 28 62 50 62 C72 62 92 74 92 112 Z" fill={color} />
  </svg>
);

const MONTHS = ["GEN", "FEB", "MAR", "APR", "MAG", "GIU", "LUG", "AGO", "SET", "OTT", "NOV", "DIC"];

// ─────────────────────────────────────────────────────────────────────────────
/**
 * "Una volta, per creare un'app servivano un team intero, mesi di lavoro... e budget da capogiro."
 * Fondo scuro e spento: le tre voci di costo si accumulano una sotto l'altra, a peso crescente.
 */
export const Yesterday: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L01");
  const tTeam = w[6].start;
  const tMesi = w[9].start;
  const tBudget = w[12].start;
  const tEnd = w[15].end;
  // alla fine tutto il blocco "cede" verso il basso, prima del taglio a vuoto
  const sink = interpolate(frame, [tEnd + 4, tEnd + 30], [0, 1], { ...clamp, easing: Easing.bezier(0.5, 0, 0.9, 0.4) });
  const row = (y: number): React.CSSProperties => ({ position: "absolute", left: 90, top: y, width: 900 });

  return (
    <AbsoluteFill style={{ background: `radial-gradient(120% 80% at 50% 20%, #23252F 0%, ${P.coal} 60%, #0B0C10 100%)` }}>
      <AbsoluteFill
        style={{
          scale: `${interpolate(frame, [0, 415], [1, 1.05])}`,
          translate: `0px ${sink * 90}px`,
          rotate: `${sink * 1.5}deg`,
          opacity: 1 - sink * 0.6,
        }}
      >
        <div style={{ ...row(200) }}>
          <Words line="L01" to={6} size={78} color={P.ash} accent={{ 0: "#FFFFFF", 1: "#FFFFFF" }} align="left" width={900} />
        </div>

        {/* 1 · un team intero */}
        <div style={row(560)}>
          <Words line="L01" from={6} to={9} size={112} align="left" width={900} />
          <div style={{ display: "flex", gap: 13, marginTop: 34 }}>
            {new Array(12).fill(0).map((_, i) => {
              const s = tTeam + 6 + i * 2;
              return (
                <div
                  key={i}
                  style={{
                    opacity: interpolate(frame, [s, s + 4], [0, 1], clamp),
                    scale: `${interpolate(frame, [s, s + 14], [0.2, 1], { ...clamp, easing: Easing.spring({ damping: 10 }) })}`,
                  }}
                >
                  <Person size={62} color={i % 4 === 0 ? "#8C93B8" : "#5B6289"} />
                </div>
              );
            })}
          </div>
        </div>

        {/* 2 · mesi di lavoro */}
        <div style={row(900)}>
          <Words line="L01" from={9} to={12} size={112} align="left" width={900} />
          <div style={{ display: "flex", gap: 8, marginTop: 34, opacity: interpolate(frame, [tMesi - 2, tMesi + 6], [0, 1], clamp) }}>
            {MONTHS.map((m, i) => {
              const s = tMesi + 4 + i * 3;
              const on = interpolate(frame, [s, s + 4], [0, 1], clamp);
              return (
                <div
                  key={m}
                  style={{
                    width: 67,
                    height: 76,
                    borderRadius: 14,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontFamily: FONT_BODY,
                    fontWeight: 600,
                    fontSize: 22,
                    color: on > 0.5 ? "#15161D" : "#6A6F85",
                    background: on > 0.5 ? "#C9CEE3" : "rgba(255,255,255,0.07)",
                    scale: `${1 + Math.exp(-Math.max(0, frame - s) / 3) * 0.18 * on}`,
                  }}
                >
                  {m}
                </div>
              );
            })}
          </div>
        </div>

        {/* 3 · budget da capogiro */}
        <div style={row(1240)}>
          <Words line="L01" from={12} size={112} align="left" width={900} accent={{ 13: "#FF6B6B", 15: "#FF6B6B" }} />
          <div style={{ display: "flex", alignItems: "flex-end", gap: 16, marginTop: 34, height: 190, opacity: interpolate(frame, [tBudget + 6, tBudget + 12], [0, 1], clamp) }}>
            {[0.22, 0.34, 0.48, 0.64, 0.82, 1].map((h, i) => {
              const s = tBudget + 8 + i * 5;
              const p = interpolate(frame, [s, s + 24], [0, h], { ...clamp, easing: OUT });
              return (
                <div
                  key={i}
                  style={{
                    width: 136,
                    height: 190 * p,
                    borderRadius: 16,
                    background: "linear-gradient(180deg, #FF6B6B, #7A2740)",
                    display: "flex",
                    justifyContent: "center",
                    paddingTop: 8,
                    fontFamily: FONT_TITLE,
                    fontWeight: 800,
                    fontSize: 34,
                    color: "rgba(255,255,255,0.9)",
                    overflow: "hidden",
                  }}
                >
                  €
                </div>
              );
            })}
          </div>
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Oggi, no." — il drop: tutto lo schermo diventa arancione. */
export const Today: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L02");
  const shake = Math.exp(-frame / 5) * 16;
  return (
    <Stage
      bg={P.orange}
      shapes={[
        { color: P.orangeLight, size: 1500, x: 540, y: 960 },
        { color: "#FF9D3D", size: 900, x: 540, y: 960 },
      ]}
    >
      <AbsoluteFill
        style={{
          alignItems: "center",
          justifyContent: "center",
          flexDirection: "column",
          translate: `${Math.sin(frame * 2.1) * shake}px ${Math.cos(frame * 1.7) * shake}px`,
        }}
      >
        <Words line="L02" to={1} size={300} variant="slam" width={1000} lineHeight={0.95} />
        <Words line="L02" from={1} size={340} variant="slam" color={P.ink} width={1000} lineHeight={0.95} />
        <Pop at={w[1].end + 4} style={{ marginTop: 70 }}>
          <Pill bg="#FFFFFF" color={P.ink} size={50}>
            Le regole sono cambiate
          </Pill>
        </Pop>
      </AbsoluteFill>
    </Stage>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Oggi basta una persona." — dodici figure si fondono in una sola. */
export const OnePerson: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L03");
  const tP = w[3].start;
  const merge = interpolate(frame, [8, tP], [0, 1], { ...clamp, easing: Easing.bezier(0.65, 0, 0.35, 1) });
  const grid = new Array(12).fill(0).map((_, i) => ({ x: (i % 4) * 190 - 285, y: Math.floor(i / 4) * 200 - 200 }));
  return (
    <Stage
      bg={P.paper}
      shapes={[
        { color: P.softOrange, size: 980, x: 540, y: 1180 },
        { color: P.softNavy, size: 420, x: 120, y: 1700, kind: "ring" },
      ]}
    >
      <AbsoluteFill style={{ alignItems: "center" }}>
        <div style={{ position: "absolute", top: 250 }}>
          <Words line="L03" size={136} color={P.ink} accent={{ 2: P.orange, 3: P.orange }} />
        </div>
        <div style={{ position: "absolute", top: 1180, left: 540 }}>
          {grid.map((p, i) => (
            <div
              key={i}
              style={{
                position: "absolute",
                left: p.x * (1 - merge) - 65,
                top: p.y * (1 - merge) - 75,
                opacity: 1 - merge,
                scale: `${1 - merge * 0.5}`,
              }}
            >
              <Person size={130} color="#B4BDDC" />
            </div>
          ))}
          <div
            style={{
              position: "absolute",
              left: -200,
              top: -260,
              opacity: interpolate(frame, [tP - 8, tP], [0, 1], clamp),
              scale: `${interpolate(frame, [tP - 8, tP + 18], [0.3, 1], { ...clamp, easing: Easing.spring({ damping: 9 }) })}`,
            }}
          >
            <Person size={400} color={P.orange} />
          </div>
        </div>
        <Pop at={tP + 8} style={{ position: "absolute", top: 1500 }}>
          <Pill bg={P.ink} size={58}>
            1 persona
          </Pill>
        </Pop>
      </AbsoluteFill>
    </Stage>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Una settimana." — sette giorni che si accendono in fila. */
export const OneWeek: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L04");
  const t0 = w[1].start;
  const days = ["L", "M", "M", "G", "V", "S", "D"];
  return (
    <Stage
      bg={P.ink}
      shapes={[
        { color: "#22368A", size: 1300, x: 900, y: 500 },
        { color: P.orange, size: 300, x: 150, y: 1650, kind: "ring" },
      ]}
    >
      <AbsoluteFill style={{ alignItems: "center" }}>
        <div style={{ position: "absolute", top: 380 }}>
          <Words line="L04" size={170} variant="slam" accent={{ 1: P.orangeLight }} width={1000} />
        </div>
        <div style={{ position: "absolute", top: 900, display: "flex", gap: 20 }}>
          {days.map((d, i) => {
            const s = t0 + 6 + i * 5;
            const on = interpolate(frame, [s, s + 3], [0, 1], clamp);
            return (
              <div
                key={i}
                style={{
                  width: 120,
                  height: 200,
                  borderRadius: 36,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontFamily: FONT_TITLE,
                  fontWeight: 800,
                  fontSize: 68,
                  color: on > 0.5 ? "#FFFFFF" : "rgba(255,255,255,0.35)",
                  background: on > 0.5 ? `linear-gradient(180deg, ${P.orangeLight}, ${P.orange})` : "rgba(255,255,255,0.08)",
                  scale: `${1 + Math.exp(-Math.max(0, frame - s) / 4) * 0.2 * on}`,
                  translate: `0px ${-Math.exp(-Math.max(0, frame - s) / 5) * 26 * on}px`,
                }}
              >
                {d}
              </div>
            );
          })}
        </div>
        <Pop at={t0 + 44} style={{ position: "absolute", top: 1200 }}>
          <div style={{ fontFamily: FONT_TITLE, fontWeight: 800, fontSize: 260, lineHeight: 1, color: "#FFFFFF" }}>
            7<span style={{ fontSize: 100, color: P.orangeLight }}> giorni</span>
          </div>
        </Pop>
      </AbsoluteFill>
    </Stage>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "E un costo quasi pari a zero." — il contatore precipita fino a zero. */
export const ZeroCost: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L05");
  const tZero = w[6].start;
  const v = interpolate(frame, [6, tZero], [100000, 0], { ...clamp, easing: Easing.bezier(0.6, 0, 0.9, 0.3) });
  const done = frame >= tZero;
  const tags = ["strumenti gratuiti", "open source", "AI"];
  return (
    <Stage
      bg={P.paper}
      shapes={[
        { color: P.softNavy, size: 1100, x: 200, y: 1500 },
        { color: P.softOrange, size: 560, x: 930, y: 420 },
      ]}
    >
      <AbsoluteFill style={{ alignItems: "center" }}>
        <div style={{ position: "absolute", top: 260 }}>
          <Words line="L05" size={118} color={P.ink} accent={{ 4: P.orange, 5: P.orange, 6: P.orange }} />
        </div>
        <div
          style={{
            position: "absolute",
            top: done ? 720 : 840,
            fontFamily: FONT_TITLE,
            fontWeight: 800,
            fontSize: done ? 440 : 170,
            lineHeight: 1,
            letterSpacing: done ? -10 : -2,
            color: done ? P.orange : P.ink,
            fontVariantNumeric: "tabular-nums",
            whiteSpace: "nowrap",
            scale: `${done ? interpolate(frame, [tZero, tZero + 16], [1.5, 1], { ...clamp, easing: Easing.spring({ damping: 9 }) }) : 1}`,
          }}
        >
          {done ? "0 €" : `${Math.round(v).toLocaleString("it-IT")} €`}
        </div>
        <div style={{ position: "absolute", top: 1330, display: "flex", flexDirection: "column", gap: 22, alignItems: "center" }}>
          {tags.map((t, i) => (
            <Pop key={t} at={tZero + 10 + i * 5}>
              <Pill bg={i === 2 ? P.orange : P.ink} size={46}>
                {t}
              </Pill>
            </Pop>
          ))}
        </div>
      </AbsoluteFill>
    </Stage>
  );
};
