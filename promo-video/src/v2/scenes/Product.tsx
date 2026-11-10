import React from "react";
import { AbsoluteFill, Easing, Img, interpolate, staticFile, useCurrentFrame } from "remotion";
import { AppScreen } from "../../components/AppScreen";
import { Callout } from "../../components/Callout";
import { FONT_BODY, FONT_TITLE } from "../../theme";
import { P, Stage, Words } from "../Kinetic";
import { useWords } from "../tl";

const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const OUT = Easing.bezier(0.16, 1, 0.3, 1);

// ─────────────────────────────────────────────────────────────────────────────
/** "Jarvis Food è nata così." — rivelazione del logo. */
export const Reveal: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <Stage
      bg={P.ink}
      shapes={[
        { color: "#22368A", size: 1500, x: 540, y: 700 },
        { color: P.orange, size: 760, x: 540, y: 700 },
        { color: P.orangeLight, size: 880, x: 540, y: 700, kind: "ring" },
      ]}
    >
      <AbsoluteFill style={{ alignItems: "center" }}>
        <Img
          src={staticFile("jarvis_logo.png")}
          style={{
            position: "absolute",
            top: 420,
            width: 560,
            height: 560,
            borderRadius: "50%",
            boxShadow: "0 40px 100px rgba(0,0,0,0.45)",
            scale: `${interpolate(frame, [0, 26], [0.2, 1], { ...clamp, easing: Easing.spring({ damping: 11 }) })}`,
            rotate: `${interpolate(frame, [0, 26], [-40, 0], { ...clamp, easing: Easing.spring({ damping: 13 }) })}deg`,
          }}
        />
        <div style={{ position: "absolute", top: 1180 }}>
          <Words line="L06" size={136} accent={{ 1: P.orangeLight }} />
        </div>
      </AbsoluteFill>
    </Stage>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Un diario alimentare che ti capisce. Calorie. Macro. Ogni pasto. In un tocco." */
export const Diary: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L07B");
  return (
    <Stage
      bg={P.paper}
      shapes={[
        { color: P.orange, size: 1050, x: 760, y: 1280 },
        { color: P.softNavy, size: 520, x: 110, y: 760 },
        { color: P.gold, size: 190, x: 960, y: 520, kind: "ring" },
      ]}
    >
      <AbsoluteFill style={{ alignItems: "center" }}>
        <div style={{ position: "absolute", top: 130 }}>
          <Words line="L07" size={96} color={P.ink} accent={{ 4: P.orange, 5: P.orange }} />
        </div>
        <div style={{ position: "absolute", top: 440, perspective: 2000 }}>
          <div
            style={{
              translate: `0px ${interpolate(frame, [4, 40], [1500, 0], { ...clamp, easing: Easing.spring({ damping: 15 }) }) + Math.sin(frame / 40) * 8}px`,
              transform: `rotateY(${interpolate(frame, [0, 50, 376], [-16, -5, 6], clamp)}deg) rotateX(${interpolate(frame, [0, 50], [12, 3], clamp)}deg)`,
              scale: "0.9",
              transformOrigin: "50% 0%",
            }}
          >
            <AppScreen clip="diary" />
          </div>
        </div>
        <Callout at={w[0].start} x={30} y={770} side="left" accent={P.orange} title="Calorie" subtitle="sempre sotto controllo" />
        <Callout at={w[1].start} x={540} y={990} side="right" accent={P.pink} title="Macro" subtitle="proteine · carbo · grassi" />
        <Callout at={w[2].start} x={30} y={1230} side="left" accent={P.green} title="Ogni pasto" subtitle="colazione → cena" />
        <Callout at={w[4].start} x={480} y={1480} side="right" accent={P.blue} title="In un tocco" subtitle="barcode, foto o ricerca" />
      </AbsoluteFill>
    </Stage>
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
    { at: recog + 4, label: "Lenticchie · 180 kcal", x: 70, y: 330, w: 400, h: 300, color: P.orange },
    { at: recog + 11, label: "Avocado · 120 kcal", x: 330, y: 140, w: 330, h: 260, color: P.green },
    { at: recog + 18, label: "Zucca · 45 kcal", x: 460, y: 470, w: 290, h: 240, color: P.gold },
  ];
  const CARD = 860;
  return (
    <Stage
      bg={P.inkDeep}
      shapes={[
        { color: "#16246A", size: 1400, x: 540, y: 900 },
        { color: P.orange, size: 240, x: 950, y: 1640, kind: "ring" },
      ]}
    >
      <AbsoluteFill style={{ alignItems: "center" }}>
        <div style={{ position: "absolute", top: 130 }}>
          <Words line="L08" size={98} accent={{ 3: P.orangeLight }} width={980} />
        </div>
        <div
          style={{
            position: "absolute",
            top: 460,
            width: CARD,
            height: CARD,
            scale: `${interpolate(frame, [0, 26], [0.8, 1], { ...clamp, easing: Easing.spring({ damping: 14 }) })}`,
            rotate: `${interpolate(frame, [0, 26], [-5, 0], { ...clamp, easing: Easing.spring({ damping: 14 }) })}deg`,
          }}
        >
          <div style={{ position: "absolute", inset: 0, borderRadius: 64, overflow: "hidden", boxShadow: "0 50px 120px rgba(0,0,0,0.55)" }}>
            <Img
              src={staticFile("food/quinoa_ceci.webp")}
              style={{ width: "100%", height: "100%", objectFit: "cover", scale: `${1.12 - scan * 0.08}` }}
            />
            <div style={{ position: "absolute", left: 0, right: 0, top: `${scan * 100}%`, bottom: 0, background: "rgba(10,18,60,0.45)" }} />
            <div
              style={{
                position: "absolute",
                left: 0,
                right: 0,
                top: `calc(${scan * 100}% - 3px)`,
                height: 6,
                background: `linear-gradient(90deg, transparent, ${P.orangeLight}, #FFFFFF, ${P.orangeLight}, transparent)`,
                boxShadow: `0 0 40px 12px ${P.orange}AA`,
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
                    borderRadius: 28,
                    opacity: Math.min(1, p * 1.5),
                    scale: `${1.25 - 0.25 * p}`,
                  }}
                >
                  <div
                    style={{
                      position: "absolute",
                      left: -5,
                      top: -66,
                      padding: "8px 20px",
                      borderRadius: 18,
                      background: b.color,
                      color: "#FFFFFF",
                      fontFamily: FONT_BODY,
                      fontWeight: 600,
                      fontSize: 34,
                      whiteSpace: "nowrap",
                    }}
                  >
                    {b.label}
                  </div>
                </div>
              );
            })}
            <AbsoluteFill style={{ background: "#FFFFFF", opacity: interpolate(frame, [shutter, shutter + 2, shutter + 10], [0, 0.9, 0], clamp) }} />
          </div>
          {/* angoli del mirino */}
          {[
            [0, 0],
            [1, 0],
            [0, 1],
            [1, 1],
          ].map(([cx, cy], i) => {
            const d = interpolate(frame, [2, 26], [70, 0], { ...clamp, easing: OUT });
            return (
              <div
                key={i}
                style={{
                  position: "absolute",
                  left: cx ? undefined : -26,
                  right: cx ? -26 : undefined,
                  top: cy ? undefined : -26,
                  bottom: cy ? -26 : undefined,
                  width: 110,
                  height: 110,
                  borderColor: "#FFFFFF",
                  borderStyle: "solid",
                  borderWidth: 0,
                  borderTopWidth: cy ? 0 : 10,
                  borderBottomWidth: cy ? 10 : 0,
                  borderLeftWidth: cx ? 0 : 10,
                  borderRightWidth: cx ? 10 : 0,
                  borderTopLeftRadius: !cx && !cy ? 44 : 0,
                  borderTopRightRadius: cx && !cy ? 44 : 0,
                  borderBottomLeftRadius: !cx && cy ? 44 : 0,
                  borderBottomRightRadius: cx && cy ? 44 : 0,
                  translate: `${(cx ? 1 : -1) * d}px ${(cy ? 1 : -1) * d}px`,
                }}
              />
            );
          })}
        </div>
        <div
          style={{
            position: "absolute",
            top: 1420,
            display: "flex",
            alignItems: "center",
            gap: 24,
            padding: "28px 48px",
            borderRadius: 44,
            background: "#FFFFFF",
            boxShadow: "0 30px 80px rgba(0,0,0,0.45)",
            opacity: interpolate(frame, [recog + 28, recog + 34], [0, 1], clamp),
            translate: `0px ${interpolate(frame, [recog + 28, recog + 46], [70, 0], { ...clamp, easing: Easing.spring({ damping: 12 }) })}px`,
          }}
        >
          <svg width="72" height="72" viewBox="0 0 72 72">
            <circle cx="36" cy="36" r="36" fill={P.green} />
            <path d="M20 37 L31 48 L53 25" stroke="#fff" strokeWidth="8" fill="none" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          <div>
            <div style={{ fontFamily: FONT_TITLE, fontWeight: 800, fontSize: 64, color: P.ink, lineHeight: 1 }}>345 kcal</div>
            <div style={{ fontFamily: FONT_BODY, fontWeight: 500, fontSize: 34, color: "#5E5B7A", marginTop: 6 }}>aggiunte al pranzo</div>
          </div>
        </div>
      </AbsoluteFill>
    </Stage>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Il digiuno? Ti guida, fase dopo fase." */
export const Fasting: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L09");
  const R = 470;
  const circ = 2 * Math.PI * R;
  const ring = interpolate(frame, [10, 300], [0.06, 0.9], { ...clamp, easing: Easing.bezier(0.33, 0, 0.2, 1) });
  return (
    <Stage
      bg="#F1ECFF"
      shapes={[
        { color: "#E2D8FF", size: 1250, x: 540, y: 1080 },
        { color: P.violet, size: 200, x: 130, y: 500, kind: "ring" },
      ]}
    >
      <AbsoluteFill style={{ alignItems: "center" }}>
        <div style={{ position: "absolute", top: 130 }}>
          <Words line="L09" size={100} color={P.ink} accent={{ 1: P.violet }} />
        </div>
        <svg width={1040} height={1040} viewBox="0 0 1040 1040" style={{ position: "absolute", top: 560, rotate: `${-90 + frame * 0.2}deg` }}>
          <defs>
            <linearGradient id="fg45" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stopColor={P.violet} />
              <stop offset="100%" stopColor="#E879F9" />
            </linearGradient>
          </defs>
          <circle cx={520} cy={520} r={R} stroke="rgba(139,92,246,0.15)" strokeWidth={34} fill="none" />
          <circle cx={520} cy={520} r={R} stroke="url(#fg45)" strokeWidth={34} strokeLinecap="round" fill="none"
            strokeDasharray={circ} strokeDashoffset={circ * (1 - ring)} />
        </svg>
        <div
          style={{
            position: "absolute",
            top: 450,
            scale: "0.84",
            transformOrigin: "50% 0%",
            translate: `0px ${interpolate(frame, [4, 36], [1500, 0], { ...clamp, easing: Easing.spring({ damping: 15 }) })}px`,
          }}
        >
          <AppScreen clip="fasting" playbackRate={0.6} />
        </div>
        <Callout at={w[4].start} x={30} y={840} side="left" accent={P.violet} title="Digestione" subtitle="0–4 h" />
        <Callout at={w[5].start} x={520} y={1110} side="right" accent="#C084FC" title="Glicogeno" subtitle="dalle 8 h" />
        <Callout at={w[6].start} x={40} y={1390} side="left" accent="#E879F9" title="Grassi come energia" subtitle="dalle 12 h" />
      </AbsoluteFill>
    </Stage>
  );
};
