import React from "react";
import { AbsoluteFill, Easing, Img, interpolate, staticFile, useCurrentFrame } from "remotion";
import { AppScreen } from "../../components/AppScreen";
import { Callout } from "../../components/Callout";
import { C, FONT_BODY, FONT_TITLE } from "../../theme";
import { Words } from "../Kinetic";
import { useWords } from "../tl";

const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

// ─────────────────────────────────────────────────────────────────────────────
/** "Jarvis Food è nata così." — rivelazione del logo. */
export const Reveal: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      <div
        style={{
          position: "absolute",
          top: 300,
          width: 900,
          height: 900,
          borderRadius: "50%",
          background: `radial-gradient(circle, ${C.orange}99 0%, ${C.orange}00 65%)`,
          scale: `${interpolate(frame, [0, 30], [0.4, 1.1], { ...clamp, easing: Easing.bezier(0.16, 1, 0.3, 1) })}`,
        }}
      />
      <Img
        src={staticFile("jarvis_logo.png")}
        style={{
          position: "absolute",
          top: 420,
          width: 560,
          height: 560,
          borderRadius: "50%",
          boxShadow: "0 30px 90px rgba(0,0,0,0.6), 0 0 140px rgba(242,94,0,0.6)",
          scale: `${interpolate(frame, [0, 26], [0.2, 1], { ...clamp, easing: Easing.spring({ damping: 11 }) })}`,
          rotate: `${interpolate(frame, [0, 26], [-40, 0], { ...clamp, easing: Easing.spring({ damping: 13 }) })}deg`,
        }}
      />
      <div style={{ position: "absolute", top: 1110 }}>
        <Words line="L06" size={124} accent={{ 1: C.orangeLight }} variant="slam" />
      </div>
    </AbsoluteFill>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Un diario alimentare che ti capisce: calorie, macro, ogni pasto. In un tocco." */
export const Diary: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L07B");
  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      <div style={{ position: "absolute", top: 120 }}>
        <Words line="L07" size={88} accent={{ 4: C.orangeLight, 5: C.orangeLight }} />
      </div>
      <div style={{ position: "absolute", top: 420, perspective: 2000 }}>
        <div
          style={{
            translate: `0px ${interpolate(frame, [0, 34], [700, 0], { ...clamp, easing: Easing.spring({ damping: 15 }) }) + Math.sin(frame / 40) * 8}px`,
            transform: `rotateY(${interpolate(frame, [0, 40, 360], [-16, -5, 6], clamp)}deg) rotateX(${interpolate(frame, [0, 40], [12, 3], clamp)}deg)`,
            scale: "0.9",
            transformOrigin: "50% 0%",
          }}
        >
          <AppScreen clip="diary" />
        </div>
      </div>
      <Callout at={w[0].start} x={30} y={760} side="left" accent={C.orange} title="Calorie" subtitle="sempre sotto controllo" />
      <Callout at={w[1].start} x={540} y={980} side="right" accent={C.protein} title="Macro" subtitle="proteine · carbo · grassi" />
      <Callout at={w[2].start} x={30} y={1220} side="left" accent={C.green} title="Ogni pasto" subtitle="colazione → cena" />
      <Callout at={w[4].start} x={480} y={1470} side="right" accent={C.carbs} title="In un tocco" subtitle="barcode, foto o ricerca" />
    </AbsoluteFill>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Scatta una foto: Jarvis riconosce il piatto." */
export const AiPhoto: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L08");
  const shutter = w[2].start;
  const recog = w[4].start;
  const scan = interpolate(frame, [shutter + 4, recog + 6], [0, 1], { ...clamp, easing: Easing.bezier(0.45, 0, 0.55, 1) });
  const boxes = [
    { at: recog + 4, label: "Lenticchie · 180 kcal", x: 60, y: 300, w: 380, h: 290, color: C.orange },
    { at: recog + 10, label: "Avocado · 120 kcal", x: 300, y: 120, w: 320, h: 250, color: C.green },
    { at: recog + 16, label: "Zucca · 45 kcal", x: 420, y: 440, w: 280, h: 230, color: C.gold },
  ];
  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      <div style={{ position: "absolute", top: 120 }}>
        <Words line="L08" size={92} accent={{ 3: C.orangeLight }} width={960} />
      </div>
      <div
        style={{
          position: "absolute",
          top: 470,
          width: 800,
          height: 800,
          borderRadius: 60,
          overflow: "hidden",
          boxShadow: "0 50px 120px rgba(0,0,0,0.55)",
          scale: `${interpolate(frame, [0, 24], [0.85, 1], { ...clamp, easing: Easing.spring({ damping: 14 }) })}`,
        }}
      >
        <Img src={staticFile("food/quinoa_ceci.webp")} style={{ width: "100%", height: "100%", objectFit: "cover", scale: `${1.1 - scan * 0.06}` }} />
        <div style={{ position: "absolute", left: 0, right: 0, top: `${scan * 100}%`, bottom: 0, background: "rgba(10,18,60,0.45)" }} />
        <div
          style={{
            position: "absolute",
            left: 0,
            right: 0,
            top: `calc(${scan * 100}% - 3px)`,
            height: 6,
            background: `linear-gradient(90deg, transparent, ${C.orangeLight}, #FFFFFF, ${C.orangeLight}, transparent)`,
            boxShadow: `0 0 40px 12px ${C.orange}AA`,
            opacity: scan > 0 && scan < 1 ? 1 : 0,
          }}
        />
        {boxes.map((b) => {
          const p = interpolate(frame, [b.at, b.at + 12], [0, 1], { ...clamp, easing: Easing.spring({ damping: 12 }) });
          return (
            <div
              key={b.label}
              style={{
                position: "absolute",
                left: b.x,
                top: b.y,
                width: b.w,
                height: b.h,
                border: `5px solid ${b.color}`,
                borderRadius: 26,
                boxShadow: `0 0 30px ${b.color}99`,
                opacity: Math.min(1, p * 1.5),
                scale: `${1.25 - 0.25 * p}`,
              }}
            >
              <div
                style={{
                  position: "absolute",
                  left: -5,
                  top: -62,
                  padding: "8px 18px",
                  borderRadius: 16,
                  background: b.color,
                  color: "#FFFFFF",
                  fontFamily: FONT_BODY,
                  fontWeight: 600,
                  fontSize: 32,
                  whiteSpace: "nowrap",
                }}
              >
                {b.label}
              </div>
            </div>
          );
        })}
        {/* flash dell'otturatore */}
        <AbsoluteFill style={{ background: "#FFFFFF", opacity: interpolate(frame, [shutter, shutter + 2, shutter + 10], [0, 0.9, 0], clamp) }} />
      </div>
      <div
        style={{
          position: "absolute",
          top: 1360,
          padding: "26px 44px",
          borderRadius: 40,
          background: "rgba(255,255,255,0.96)",
          boxShadow: "0 30px 80px rgba(0,0,0,0.45)",
          fontFamily: FONT_TITLE,
          fontWeight: 800,
          fontSize: 60,
          color: C.text,
          opacity: interpolate(frame, [recog + 24, recog + 30], [0, 1], clamp),
          translate: `0px ${interpolate(frame, [recog + 24, recog + 40], [60, 0], { ...clamp, easing: Easing.spring({ damping: 12 }) })}px`,
        }}
      >
        <span style={{ color: C.green }}>✓</span> 345 kcal
      </div>
    </AbsoluteFill>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Il digiuno? Ti guida, fase dopo fase." */
export const Fasting: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L09");
  const R = 440;
  const circ = 2 * Math.PI * R;
  const ring = interpolate(frame, [0, 180], [0.08, 0.85], clamp);
  const phases = [
    { at: w[4].start, t: "Digestione", c: C.fasting, x: 30, y: 820 },
    { at: w[5].start, t: "Glicogeno", c: "#C084FC", x: 600, y: 1080 },
    { at: w[6].start, t: "Grassi come energia", c: "#E879F9", x: 60, y: 1360 },
  ];
  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      <div style={{ position: "absolute", top: 120 }}>
        <Words line="L09" size={92} accent={{ 1: "#B79CFF" }} />
      </div>
      <svg width={1000} height={1000} viewBox="0 0 1000 1000" style={{ position: "absolute", top: 560, rotate: `${-90 + frame * 0.4}deg` }}>
        <defs>
          <linearGradient id="fg45" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor={C.fasting} />
            <stop offset="100%" stopColor="#E879F9" />
          </linearGradient>
        </defs>
        <circle cx={500} cy={500} r={R} stroke="rgba(255,255,255,0.08)" strokeWidth={30} fill="none" />
        <circle cx={500} cy={500} r={R} stroke="url(#fg45)" strokeWidth={30} strokeLinecap="round" fill="none"
          strokeDasharray={circ} strokeDashoffset={circ * (1 - ring)} style={{ filter: `drop-shadow(0 0 22px ${C.fasting})` }} />
      </svg>
      <div style={{ position: "absolute", top: 440, scale: "0.82", transformOrigin: "50% 0%",
        translate: `0px ${interpolate(frame, [0, 26], [500, 0], { ...clamp, easing: Easing.spring({ damping: 15 }) })}px` }}>
        <AppScreen clip="fasting" />
      </div>
      {phases.map((p) => (
        <Callout key={p.t} at={p.at} x={p.x} y={p.y} side={p.x < 300 ? "left" : "right"} accent={p.c} title={p.t} />
      ))}
    </AbsoluteFill>
  );
};
