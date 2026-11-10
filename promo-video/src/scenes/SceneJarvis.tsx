import React from "react";
import { AbsoluteFill, Img, staticFile } from "remotion";
import { EZ, ip, kf, sp } from "../anim";
import { C, FONT } from "../theme";
import { Trail, ellipsePath } from "../components/Trail";
import { CheckIcon } from "../ui/kit";

const PORTAL = { x: 640, y: 1000 };
const SPLIT = 1220;
const ROBOT = { cx: 288, cy: 900, h: 840, w: 840 * (540 / 802) };
const PILLS = ["Consigli nutrizionali", "Piani su misura", "Ricette personalizzate", "Supporto quotidiano"];
const PILL_AT = [1515, 1530, 1545, 1560];
const SWIRL = ellipsePath(308, 1220, 360, 84, -8, 3);

/** SCENA 7 (24-28 s): dal tab "Jarvis" si apre un portale; il robot esce, il fumetto si espande, le spunte arrivano a tempo. */
export const SceneJarvis: React.FC<{ f: number }> = ({ f }) => {
  if (f < 1426 || f > 1688) return null;
  const R = ip(f, [1428, 1464], [0, 1750], EZ.inOut);
  const t = f / 60;
  const pop = sp(f, 1442, { damping: 9, stiffness: 120, mass: 0.9 });
  const bob = Math.sin(t * 2.4) * 12;
  const rcx = PORTAL.x + (ROBOT.cx - PORTAL.x) * pop;
  const rcy = PORTAL.y + (ROBOT.cy - PORTAL.y) * pop + bob;
  // fumetto: prima i puntini, poi si allarga
  const dotB = sp(f, 1462, { damping: 9, stiffness: 200 });
  const big = sp(f, 1485, { damping: 11, stiffness: 150 });
  const bw = 170 + (450 - 170) * big;
  const bh = 110 + (270 - 110) * big;
  const bx = 610 + (590 - 610) * big;
  const by = 400 + (300 - 400) * big;
  // uscita: la camera entra nel cappello sul petto del robot
  const zoom = kf(f, [[1644, 1], [1682, 9, EZ.in]]);
  const emblem = { x: ROBOT.cx + ROBOT.w * 0.13, y: ROBOT.cy + ROBOT.h * 0.26 };
  const swirlHead = ip(f, [1446, 1680], [0.02, 1]);
  return (
    <AbsoluteFill style={{ clipPath: f < 1466 ? `circle(${R}px at ${PORTAL.x}px ${PORTAL.y}px)` : undefined, opacity: ip(f, [1672, 1684], [1, 0]) }}>
      <AbsoluteFill style={{ transform: `scale(${zoom})`, transformOrigin: `${emblem.x}px ${emblem.y}px`, filter: zoom > 1.02 ? `blur(${Math.min(14, (zoom - 1) * 2.2)}px)` : undefined }}>
        <AbsoluteFill style={{ background: "linear-gradient(165deg, #06175A 0%, #0B43B8 42%, #2A8CFF 100%)" }} />
        <div style={{ position: "absolute", left: -300, top: -200, width: 1700, height: 1500, background: "radial-gradient(closest-side, rgba(80,190,255,0.5), rgba(80,190,255,0))", transform: `translate(${Math.sin(t) * 40}px, ${Math.cos(t * 0.8) * 30}px)` }} />
        <div style={{ position: "absolute", inset: -500, transform: `rotate(${-28 + Math.sin(t * 0.4) * 3}deg)`, background: "repeating-linear-gradient(90deg, rgba(255,255,255,0) 0px, rgba(170,220,255,0.10) 110px, rgba(255,255,255,0) 260px, rgba(255,255,255,0) 480px)" }} />
        {/* scia dietro al robot */}
        <Trail d={SWIRL} head={swirlHead} len={0.14} width={13} opacity={ip(f, [1452, 1470], [0, 1])} clip={{ x: 0, y: 0, w: 1080, h: SPLIT }} sparkle={false} />
        {/* ombra + robot */}
        <div style={{ position: "absolute", left: rcx - 190, top: ROBOT.cy + 440, width: 380, height: 56, borderRadius: "50%", background: "radial-gradient(closest-side, rgba(0,10,60,0.5), rgba(0,10,60,0))", transform: `scale(${pop * (1 - bob / 90)})` }} />
        <Img
          src={staticFile("img/robot.png")}
          style={{ position: "absolute", left: rcx - ROBOT.w / 2, top: rcy - ROBOT.h / 2, height: ROBOT.h, transform: `scale(${pop}) rotate(${(1 - Math.min(1, pop)) * 26 + Math.sin(t * 1.6) * 2.5}deg)`, transformOrigin: "50% 80%", filter: "drop-shadow(0 30px 40px rgba(0,10,70,0.45))" }}
        />
        {/* bagliore degli occhi */}
        <div style={{ position: "absolute", left: rcx - 150, top: rcy - 215, width: 370, height: 190, background: "radial-gradient(closest-side, rgba(80,170,255,0.55), rgba(80,170,255,0))", opacity: pop * (0.5 + 0.5 * Math.sin(t * 6)), mixBlendMode: "screen" }} />
        <Trail d={SWIRL} head={swirlHead} len={0.14} width={13} opacity={ip(f, [1452, 1470], [0, 1])} clip={{ x: 0, y: SPLIT, w: 1080, h: 700 }} />
        {/* fumetto */}
        {f >= 1462 ? (
          <div style={{ position: "absolute", left: bx, top: by, width: bw, height: bh, transform: `scale(${Math.min(1.08, dotB)}) rotate(${-5 * big}deg)`, transformOrigin: "0% 100%" }}>
            <div style={{ position: "absolute", left: 22, bottom: -34, width: 0, height: 0, borderLeft: "0 solid transparent", borderRight: "64px solid transparent", borderTop: "62px solid #fff", transform: "skewX(18deg)" }} />
            <div style={{ position: "absolute", inset: 0, borderRadius: Math.min(bh / 2, 120), background: "linear-gradient(180deg,#FFFFFF,#EAF2FF)", boxShadow: "0 24px 50px rgba(0,20,90,0.35), inset 0 -6px 14px rgba(120,160,230,0.25)" }} />
            {big < 0.5
              ? [0, 1, 2].map((i) => (
                  <div key={i} style={{ position: "absolute", left: 45 + i * 34, top: 43 - Math.max(0, Math.sin((f - 1462 - i * 5) / 4)) * 14, width: 22, height: 22, borderRadius: 11, background: "#1D6BF3", opacity: 1 - big * 2 }} />
                ))
              : null}
            <div style={{ position: "absolute", left: 0, right: 0, top: 44, textAlign: "center", fontFamily: FONT, fontStyle: "italic", opacity: ip(big, [0.5, 0.9], [0, 1]) }}>
              <div style={{ fontSize: 62, fontWeight: 700, color: C.navy, lineHeight: 1, transform: `translateY(${(1 - ip(f, [1491, 1511], [0, 1], EZ.out)) * 30}px)` }}>Parla con</div>
              <div style={{ fontSize: 104, fontWeight: 800, color: C.orange, lineHeight: 1.05, letterSpacing: -2, transform: `scale(${sp(f, 1497, { damping: 8, stiffness: 190 })})` }}>Jarvis</div>
            </div>
          </div>
        ) : null}
        {/* elenco con spunte */}
        {PILLS.map((p, i) => {
          const a = sp(f, PILL_AT[i], { damping: 11, stiffness: 170 });
          const chk = sp(f, PILL_AT[i] + 7, { damping: 7, stiffness: 240 });
          if (f < PILL_AT[i]) return null;
          return (
            <div key={p} style={{ position: "absolute", left: 592, top: 730 + i * 136, width: 466, height: 108, borderRadius: 54, background: "linear-gradient(180deg,#FFFFFF,#E9F1FF)", boxShadow: "0 16px 34px rgba(0,20,90,0.3)", transform: `translateX(${(1 - a) * 560}px) scale(${0.85 + 0.15 * a})`, opacity: Math.min(1, a * 2.5) }}>
              <div style={{ position: "absolute", left: 14, top: 16, width: 76, height: 76, borderRadius: 38, background: "linear-gradient(180deg,#5FE03A,#1FAE2B)", boxShadow: "0 6px 14px rgba(30,160,40,0.5)", transform: `scale(${chk}) rotate(${(1 - Math.min(1, chk)) * -90}deg)`, display: "flex", alignItems: "center", justifyContent: "center" }}>
                <CheckIcon s={50} w={3.6} />
              </div>
              <div style={{ position: "absolute", left: 102, top: 0, height: 108, display: "flex", alignItems: "center", fontFamily: FONT, fontSize: 31, fontWeight: 600, color: C.navy, letterSpacing: -0.9, whiteSpace: "nowrap" }}>{p}</div>
            </div>
          );
        })}
      </AbsoluteFill>
      {/* bordo luminoso del portale */}
      {f < 1466 ? <div style={{ position: "absolute", left: PORTAL.x - R, top: PORTAL.y - R, width: R * 2, height: R * 2, borderRadius: "50%", border: "12px solid #FFB14A", boxShadow: "0 0 70px 24px rgba(255,130,20,0.85), inset 0 0 70px 14px rgba(255,150,40,0.7)", boxSizing: "border-box" }} /> : null}
    </AbsoluteFill>
  );
};
