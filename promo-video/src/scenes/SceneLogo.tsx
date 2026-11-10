import React from "react";
import { AbsoluteFill } from "remotion";
import { EZ, ip, kf, pulse, sp } from "../anim";
import { Logo } from "../components/Logo";
import { Trail, ellipsePath } from "../components/Trail";
import { Caption, words } from "../components/Caption";
import { PHONE_BASE } from "../theme";

/** posizione/dimensione del logo nello splash del telefono (deve combaciare con PhoneJourney) */
export const SPLASH_LOGO = { cx: 540, cy: 770 + (700 - 836) * PHONE_BASE, width: 620 * PHONE_BASE };

const INTRO = "M -90 1560 C 260 1500, 520 1240, 360 960 C 220 780, 420 480, 560 550";
const ORBIT = ellipsePath(540, 800, 490, 440, -12, 3);

/** SCENA 1 (0-4 s): il logo nasce da una scintilla, si assembla a tempo e riparte in rotazione verso il telefono. */
export const SceneLogo: React.FC<{ f: number }> = ({ f }) => {
  if (f > 256) return null;
  const hat = sp(f, 30, { damping: 10, stiffness: 150 });
  const jar = sp(f, 60, { damping: 9, stiffness: 170 });
  const food = sp(f, 75, { damping: 8, stiffness: 190 });
  const smile = ip(f, [90, 110], [0, 1], EZ.out);
  const sparks = sp(f, 100, { damping: 7, stiffness: 220 });
  const hit = kf(f, [[117, 1], [122, 1.14, EZ.out], [152, 1, EZ.out]]);
  const antic = kf(f, [[198, 1], [212, 0.93, EZ.inOut], [250, 1, EZ.out]]);
  const travel = ip(f, [212, 250], [0, 1], EZ.expoInOut);
  const t = f / 60;
  const idle = f > 120 && f < 212 ? 1 : 0;
  const width = (880 + (SPLASH_LOGO.width - 880) * travel) * hit * antic;
  const cy = 800 + (SPLASH_LOGO.cy - 800) * travel + Math.sin(t * 2.2) * 8 * idle;
  const shock = ip(f, [120, 160], [0, 1], EZ.out);
  const morph = ip(f, [214, 246], [0, 1], EZ.inOut);
  return (
    <AbsoluteFill>
      {/* bagliore del pavimento */}
      <div style={{ position: "absolute", left: 90, top: 1180, width: 900, height: 200, borderRadius: "50%", background: "radial-gradient(closest-side, rgba(255,190,90,0.95), rgba(255,120,10,0.45) 45%, rgba(255,120,10,0))", transform: `scaleX(${ip(f, [116, 140], [0.1, 1], EZ.out)}) scaleY(${0.32 + pulse(f, 118, 4, 30) * 0.3})`, opacity: ip(f, [116, 126], [0, 0.9]) * ip(f, [200, 222], [1, 0]) }} />
      {/* onda d'urto */}
      {shock > 0 && shock < 1 ? <div style={{ position: "absolute", left: 540 - 520, top: 800 - 520, width: 1040, height: 1040, borderRadius: "50%", border: `${10 * (1 - shock)}px solid rgba(255,170,60,${1 - shock})`, transform: `scale(${0.25 + shock * 0.95})`, boxShadow: `0 0 60px rgba(255,130,20,${0.8 * (1 - shock)})` }} /> : null}
      {/* anello che si trasforma nel profilo del telefono */}
      {f >= 214 && f < 258 ? (
        <div
          style={{
            position: "absolute",
            left: 540 - (980 - (980 - 788) * morph) / 2,
            top: 800 - 30 * morph - (880 + (1373 - 880) * morph) / 2,
            width: 980 - (980 - 788) * morph,
            height: 880 + (1373 - 880) * morph,
            borderRadius: 440 - (440 - 86) * morph,
            border: "5px solid rgba(255,190,110,0.95)",
            boxShadow: "0 0 40px rgba(255,120,10,0.9), inset 0 0 40px rgba(255,120,10,0.5)",
            boxSizing: "border-box",
            opacity: ip(f, [214, 224], [0, 1]) * ip(f, [244, 256], [1, 0]),
            transform: `rotate(${(1 - morph) * -12}deg)`,
          }}
        />
      ) : null}
      <Trail d={ORBIT} head={ip(f, [118, 250], [0.02, 1])} len={0.09} width={17} opacity={ip(f, [118, 130], [0, 1]) * ip(f, [206, 226], [1, 0])} clip={{ x: 0, y: 0, w: 1080, h: 780 }} />
      <Logo
        cx={540}
        cy={cy}
        width={width}
        ry={Math.sin(t * 1.5) * 9 * idle * ip(f, [120, 150], [0, 1]) + travel * 720}
        rx={Math.cos(t * 1.2) * 4 * idle}
        opacity={f < 250 ? 1 : 0}
        glow={0.25 + 0.75 * ip(f, [100, 124], [0, 1]) - travel * 0.5}
        pieces={{
          hat: { scale: hat, rotY: (1 - Math.min(1, hat)) * 540, y: (1 - Math.min(1, hat)) * 160, opacity: f < 28 ? 0 : 1 },
          jarvis: { y: (1 - jar) * -620, rotX: (1 - Math.min(1, jar)) * -80, opacity: f < 58 ? 0 : 1 },
          food: { y: (1 - food) * 380, scale: 0.4 + 0.6 * food, rot: (1 - food) * 14, opacity: f < 74 ? 0 : 1 },
          smile: { wipe: smile, opacity: f < 90 ? 0 : 1 },
          sparks: { scale: sparks, rot: (1 - sparks) * -40, opacity: f < 100 ? 0 : 1 },
        }}
      />
      <Trail d={ORBIT} head={ip(f, [118, 250], [0.02, 1])} len={0.09} width={17} opacity={ip(f, [118, 130], [0, 1]) * ip(f, [206, 226], [1, 0])} clip={{ x: 0, y: 780, w: 1080, h: 1140 }} />
      <Trail d={ORBIT} head={ip(f, [118, 250], [0.19, 1.17])} len={0.05} width={8} tone="blue" opacity={ip(f, [124, 140], [0, 0.9]) * ip(f, [206, 226], [1, 0])} />
      {/* scintilla iniziale */}
      <Trail d={INTRO} head={ip(f, [0, 34], [0, 1], EZ.inOut) + ip(f, [34, 52], [0, 0.5])} len={0.42} width={18} />
      {/* payoff */}
      <Caption frame={f} inAt={132} outAt={200} y={1420} size={60} weight={500} dark lines={[words("La tua nutrizione intelligente")]} stagger={9} />
      <svg width={1080} height={1920} style={{ position: "absolute", opacity: ip(f, [200, 212], [1, 0]) }}>
        <path d="M 380 1540 Q 540 1506 700 1524" fill="none" stroke="#FF6B00" strokeWidth={9} strokeLinecap="round" pathLength={1} strokeDasharray={`${ip(f, [160, 184], [0, 1], EZ.out)} 2`} opacity={f < 160 ? 0 : 1} />
      </svg>
    </AbsoluteFill>
  );
};
