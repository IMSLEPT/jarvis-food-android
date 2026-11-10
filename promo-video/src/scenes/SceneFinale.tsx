import React from "react";
import { AbsoluteFill, Img, staticFile } from "remotion";
import { EZ, ip, kf, pulse, sp } from "../anim";
import { C, FONT } from "../theme";
import { Logo } from "../components/Logo";
import { Trail, ellipsePath } from "../components/Trail";
import { Caption, words } from "../components/Caption";

export type Cta = "neutral" | "googlePlay";
const START = 1664;
const HIT = 1680;
const ITEMS = [
  { n: "f_tomatoes.png", x: 945, y: 170, w: 330, r: 8, z: 1.2 },
  { n: "f_basil2.png", x: 120, y: 250, w: 280, r: -150, z: 0.9 },
  { n: "f_oats.png", x: 84, y: 1235, w: 205, r: 0, z: 1.1 },
  { n: "f_basil.png", x: 1004, y: 1215, w: 230, r: 12, z: 0.8 },
  { n: "f_dumbbell.png", x: 965, y: 1640, w: 280, r: -6, z: 1.3 },
  { n: "f_salmon.png", x: 105, y: 1650, w: 210, r: 16, z: 1.0 },
];
const ORBIT = ellipsePath(540, 720, 480, 430, -12, 3);

/** SCENA 8 (28-30 s): il cappello sul petto del robot diventa il logo; impatto sul colpo finale, ingredienti che esplodono, invito all'azione. */
export const SceneFinale: React.FC<{ f: number; cta: Cta }> = ({ f, cta }) => {
  if (f < START) return null;
  const t = f / 60;
  const arrive = ip(f, [1668, HIT + 2], [0, 1], EZ.in);
  const punch = kf(f, [[HIT + 2, 1], [HIT + 8, 1.1, EZ.out], [HIT + 34, 1, EZ.inOut]]);
  const width = (3200 - (3200 - 860) * arrive) * punch;
  const shock = ip(f, [HIT, HIT + 44], [0, 1], EZ.out);
  const shock2 = ip(f, [HIT + 8, HIT + 60], [0, 1], EZ.out);
  const idle = ip(f, [HIT + 20, HIT + 50], [0, 1]);
  const side = ip(f, [1670, HIT + 2], [1, 0], EZ.in);
  return (
    <AbsoluteFill>
      <AbsoluteFill style={{ background: "radial-gradient(ellipse at 50% 38%, #0C2468 0%, #050D2E 55%, #02040F 100%)", opacity: ip(f, [START, HIT], [0, 1]) }} />
      <AbsoluteFill style={{ opacity: ip(f, [HIT, HIT + 20], [0, 1]) }}>
        {new Array(26).fill(0).map((_, i) => {
          const a = (i * 137.5 * Math.PI) / 180;
          const d = 180 + ((i * 53) % 700) + (f - HIT) * (0.6 + (i % 5) * 0.25);
          return <div key={i} style={{ position: "absolute", left: 540 + Math.cos(a) * d, top: 760 + Math.sin(a) * d * 1.3, width: 5 + (i % 3) * 2, height: 5 + (i % 3) * 2, borderRadius: 4, background: i % 3 ? "#FFB45E" : "#9CCBFF", boxShadow: `0 0 12px ${i % 3 ? "#FF8A1F" : "#4F9DFF"}`, opacity: 0.35 + 0.5 * Math.sin(t * 3 + i) ** 2 }} />;
        })}
      </AbsoluteFill>
      {/* pavimento luminoso */}
      <div style={{ position: "absolute", left: 60, top: 1065, width: 960, height: 210, borderRadius: "50%", background: "radial-gradient(closest-side, rgba(255,200,110,0.95), rgba(255,120,10,0.5) 42%, rgba(255,120,10,0))", transform: `scaleX(${ip(f, [HIT, HIT + 22], [0.1, 1], EZ.out)}) scaleY(${0.3 + pulse(f, HIT, 4, 30) * 0.4})`, opacity: f < HIT ? 0 : 0.9 }} />
      {/* ingredienti */}
      {ITEMS.map((it, i) => {
        const a = sp(f, HIT + 2 + i * 3, { damping: 12, stiffness: 110, mass: 0.9 });
        if (f < HIT + 2 + i * 3) return null;
        const fl = Math.sin(t * 1.4 + i * 1.9) * 14;
        return <Img key={it.n + i} src={staticFile(`img/${it.n}`)} style={{ position: "absolute", left: 540 + (it.x - 540) * a - it.w / 2, top: 740 + (it.y - 740) * a + fl - it.w / 2, width: it.w, transform: `scale(${0.3 + 0.7 * Math.min(1.08, a)}) rotate(${it.r + (1 - Math.min(1, a)) * 160 + Math.sin(t + i) * 4}deg)`, filter: `drop-shadow(0 24px 30px rgba(0,0,20,0.55)) blur(${it.z > 1.15 ? 2 : 0}px)` }} />;
      })}
      {/* onde d'urto */}
      {[shock, shock2].map((s, i) => (s > 0 && s < 1 ? <div key={i} style={{ position: "absolute", left: 540 - 700, top: 720 - 700, width: 1400, height: 1400, borderRadius: "50%", border: `${(i ? 5 : 12) * (1 - s)}px solid rgba(255,180,80,${1 - s})`, transform: `scale(${0.2 + s * 1.1})`, boxShadow: `0 0 70px rgba(255,130,20,${0.8 * (1 - s)})` }} /> : null))}
      <Trail d={ORBIT} head={ip(f, [HIT + 4, 1800], [0.02, 0.62])} len={0.08} width={12} opacity={ip(f, [HIT + 4, HIT + 18], [0, 1])} clip={{ x: 0, y: 0, w: 1080, h: 700 }} />
      <Logo
        cx={540}
        cy={720 + Math.sin(t * 2) * 7 * idle}
        width={width}
        ry={Math.sin(t * 1.4) * 8 * idle + (1 - arrive) * -60}
        rx={Math.cos(t * 1.1) * 3.5 * idle}
        opacity={ip(f, [1668, 1676], [0, 1])}
        glow={0.5 + 0.5 * arrive + pulse(f, HIT, 3, 26) * 0.9}
        pieces={{
          jarvis: { x: -side * 1300, rotY: side * 70 },
          food: { x: side * 1300, rotY: -side * 70 },
          smile: { wipe: ip(f, [HIT + 2, HIT + 18], [0, 1], EZ.out), opacity: f < HIT + 2 ? 0 : 1 },
          sparks: { scale: sp(f, HIT + 12, { damping: 7, stiffness: 230 }), opacity: f < HIT + 12 ? 0 : 1 },
        }}
      />
      <Trail d={ORBIT} head={ip(f, [HIT + 4, 1800], [0.02, 0.62])} len={0.08} width={12} opacity={ip(f, [HIT + 4, HIT + 18], [0, 1])} clip={{ x: 0, y: 700, w: 1080, h: 1220 }} />
      {/* lampo d'impatto */}
      <AbsoluteFill style={{ background: "radial-gradient(ellipse at 50% 38%, rgba(255,245,220,1) 0%, rgba(255,150,40,0.85) 35%, rgba(255,120,10,0) 75%)", opacity: pulse(f, HIT - 5, 5, 12), mixBlendMode: "screen" }} />
      {cta === "googlePlay" ? (
        <>
          <Caption frame={f} inAt={HIT + 26} outAt={99999} y={1238} size={92} weight={800} italic dark lines={[words("Scarica ora")]} stagger={6} />
          <div style={{ position: "absolute", left: 0, right: 0, top: 1344, textAlign: "center", fontFamily: FONT, fontStyle: "italic", fontWeight: 800, fontSize: 104, letterSpacing: -2, lineHeight: 1, background: "linear-gradient(180deg,#FFB02E,#FF5A00)", WebkitBackgroundClip: "text", color: "transparent", transform: `scale(${sp(f, HIT + 38, { damping: 9, stiffness: 180 })})`, opacity: f < HIT + 38 ? 0 : 1 }}>Jarvis Food</div>
          <Img src={staticFile("img/play_badge.png")} style={{ position: "absolute", left: 540 - 270, top: 1512, width: 540, borderRadius: 30, transform: `scale(${sp(f, HIT + 52, { damping: 9, stiffness: 170 })}) translateY(${(1 - ip(f, [HIT + 52, HIT + 76], [0, 1], EZ.out)) * 60}px)`, opacity: f < HIT + 52 ? 0 : 1, boxShadow: "0 0 50px rgba(255,130,20,0.6)" }} />
        </>
      ) : (
        <>
          <Caption frame={f} inAt={HIT + 26} outAt={99999} y={1300} size={60} weight={600} dark lines={[words("La tua nutrizione"), words("intelligente", ["intelligente"])]} stagger={7} />
          <svg width={1080} height={1920} style={{ position: "absolute" }}>
            <path d="M 380 1500 Q 540 1466 700 1484" fill="none" stroke={C.orange} strokeWidth={10} strokeLinecap="round" pathLength={1} strokeDasharray={`${ip(f, [HIT + 56, HIT + 82], [0, 1], EZ.out)} 2`} opacity={f < HIT + 56 ? 0 : 1} />
          </svg>
        </>
      )}
    </AbsoluteFill>
  );
};
