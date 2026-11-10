import React from "react";
import { AbsoluteFill, Img, staticFile } from "remotion";
import { EZ, KF, ip, kf, pulse, sp } from "../anim";
import { Phone, PhonePose } from "../components/Phone";
import { DirBlur } from "../components/DirBlur";
import { Trail, ellipsePath } from "../components/Trail";
import { SCREEN_C } from "../theme";
import { Oggi } from "../ui/Oggi";
import { Piano } from "../ui/Piano";
import { Cucina } from "../ui/Cucina";
import { Progressi } from "../ui/Progressi";

const bell = (f: number, a: number, b: number) => (f <= a || f >= b ? 0 : Math.sin((Math.PI * (f - a)) / (b - a)) ** 2);
const CX = SCREEN_C.x;
const CY = SCREEN_C.y;

/** Regia del telefono: un solo oggetto che attraversa le scene 2-6. Ogni blocco e' una scena. */
export const phonePose = (f: number): PhonePose & { yoff: number } => {
  let scale: KF, fx: KF, fy: KF, tx: KF, ty: KF, ry: KF, rx: KF;
  let rz: KF = [[0, 0]];
  let yoff: KF = [[0, 0]];
  tx = [[0, 540]];
  if (f < 486) {
    // S2 "Un'unica app" + tuffo nello schermo
    scale = [[232, 0.82], [252, 1, EZ.back], [452, 1], [486, 2.3, EZ.expoInOut]];
    fx = [[452, CX], [486, 223, EZ.expoInOut]];
    fy = [[452, CY], [486, 443, EZ.expoInOut]];
    ty = [[452, 770], [486, 780]];
    ry = [[252, 0], [310, -15], [440, 14], [468, 0]];
    rx = [[252, 0], [310, 5], [440, 6], [468, 0]];
  } else if (f < 752) {
    // S3 "Oggi": primo piano sull'anello, carrellata sui macro, arretramento, giro di 360 gradi
    scale = [[486, 2.3], [512, 2.25], [546, 2.0], [596, 1.0], [690, 1.03], [720, 0.9, EZ.in], [752, 1.0, EZ.out]];
    fx = [[512, 223], [546, 640], [596, CX]];
    fy = [[512, 443], [546, 420], [596, CY]];
    ty = [[512, 780], [596, 770]];
    ry = [[546, 0], [596, 13], [688, 8], [752, -372, EZ.expoInOut]];
    rx = [[546, 0], [596, 5], [688, 4], [720, 0], [752, 4]];
  } else if (f < 960) {
    // S4 "Piano": atterraggio, spinta sulla lista spesa, ingresso nella foto del pranzo
    scale = [[752, 1], [858, 1], [912, 1.18], [930, 1.18], [960, 5.5, EZ.in]];
    fx = [[930, CX], [960, 150, EZ.in]];
    fy = [[858, CY], [912, 1290], [930, 1290], [960, 965, EZ.in]];
    ty = [[858, 770], [912, 940], [930, 940], [960, 700, EZ.in]];
    ry = [[752, -12], [858, -7], [930, -3], [954, 0]];
    rx = [[752, 4], [930, 3], [954, 0]];
  } else if (f < 1200) {
    // S5 "Cucina": si esce dalla foto della ricetta, poi frustata verso l'alto
    scale = [[960, 1.233], [1014, 1.0, EZ.out], [1100, 1.0], [1176, 1.08], [1200, 1.08]];
    fx = [[960, 470], [1014, CX, EZ.out]];
    fy = [[960, 670], [1014, CY, EZ.out], [1100, CY], [1176, 930]];
    ty = [[960, 700], [1014, 770, EZ.out]];
    ry = [[960, 0], [1014, 10, EZ.out], [1176, 5]];
    rx = [[960, 0], [1014, 5, EZ.out], [1178, 5], [1200, 16]];
    rz = [[1178, 0], [1200, -9, EZ.in]];
    yoff = [[1178, 0], [1200, -2500, EZ.in]];
  } else {
    // S6 "Progressi": arrivo dal basso sul grafico, arretramento, spinta sull'icona Jarvis
    yoff = [[1200, 2500], [1232, 0, EZ.backSoft]];
    scale = [[1200, 1.3], [1318, 1.3], [1362, 1.0], [1405, 1.0], [1440, 2.6, EZ.in]];
    fx = [[1405, CX], [1440, 832, EZ.in]];
    fy = [[1200, 575], [1318, 575], [1362, CY], [1405, CY], [1440, 1596, EZ.in]];
    tx = [[1405, 540], [1440, 640, EZ.in]];
    ty = [[1200, 680], [1318, 680], [1362, 770], [1405, 770], [1440, 1000, EZ.in]];
    ry = [[1200, -8], [1318, -6], [1362, 8], [1405, 6], [1440, 0]];
    rx = [[1200, 20], [1232, 7, EZ.out], [1362, 4], [1440, 0]];
    rz = [[1200, 9], [1232, 0, EZ.out]];
  }
  const y = kf(f, yoff);
  return {
    scale: kf(f, scale),
    focus: { x: kf(f, fx), y: kf(f, fy) },
    target: { x: kf(f, tx), y: kf(f, ty) + y },
    rx: kf(f, rx),
    ry: kf(f, ry),
    rz: kf(f, rz),
    opacity: ip(f, [232, 246], [0, 1]),
    yoff: y,
  };
};

const Splash: React.FC<{ f: number }> = ({ f }) => (
  <AbsoluteFill style={{ background: "radial-gradient(ellipse at 50% 42%, #123A9A 0%, #081B55 45%, #040B26 100%)" }}>
    <div style={{ position: "absolute", left: 470 - 420, top: 700 - 420, width: 840, height: 840, background: "radial-gradient(closest-side, rgba(255,130,20,0.5), rgba(255,110,0,0))", transform: `scale(${1 + Math.sin(f / 14) * 0.06})` }} />
    <Img src={staticFile("img/logo_full.png")} style={{ position: "absolute", left: 470.5 - 310, top: 700 - 277, width: 620, opacity: f >= 250 ? 1 : 0, transform: `scale(${1 + pulse(f, 250, 3, 16) * 0.08})` }} />
  </AbsoluteFill>
);

const TILES = [
  { n: "t_fork.png", x: 925, y: 400, w: 225, z: 1.3 },
  { n: "t_dumbbell.png", x: 150, y: 600, w: 240, z: 1.0 },
  { n: "t_heart.png", x: 938, y: 700, w: 212, z: 0.8 },
  { n: "t_leaf.png", x: 148, y: 930, w: 225, z: 1.2 },
  { n: "t_chart.png", x: 892, y: 985, w: 232, z: 1.0 },
];
const FOOD = [
  { n: "f_salmon.png", x: 86, y: 372, w: 185, z: 1.3, r: -12 },
  { n: "f_basil2.png", x: 930, y: 170, w: 270, z: 1.0, r: 10 },
  { n: "f_zucchini.png", x: 66, y: 610, w: 150, z: 0.8, r: 18 },
  { n: "f_tomato2.png", x: 955, y: 1290, w: 300, z: 1.2, r: -8 },
];
const ORBIT = ellipsePath(540, 810, 520, 165, -13, 2);
const SWEEP_H = "M -220 980 C 260 700, 760 1080, 1320 720";
const SWEEP_V = "M 660 -260 C 470 420, 690 1180, 500 2240";

/** SCENE 2-6: il telefono, le icone che gli orbitano attorno, gli ingredienti e le scie di raccordo. */
export const PhoneJourney: React.FC<{ f: number }> = ({ f }) => {
  if (f < 228 || f > 1470) return null;
  const pose = phonePose(f);
  const blurXY = 7 * bell(f, 460, 488) + 11 * bell(f, 942, 974) + 6 * bell(f, 1424, 1446);
  const bx = blurXY + 30 * bell(f, 698, 744);
  const by = blurXY + 36 * bell(f, 1182, 1224);
  const reveal = ip(f, [466, 494], [0, 1350], EZ.inOut);
  const t = f / 60;
  const orbitHead = ip(f, [236, 474], [0.02, 1.0]);
  const orbitOp = ip(f, [236, 250], [0, 1]) * ip(f, [458, 476], [1, 0]);
  const navTap = pulse(f, 1420, 4, 22);

  let screen: React.ReactNode;
  if (f < 466) screen = <Splash f={f} />;
  else if (f < 496)
    screen = (
      <>
        <Splash f={f} />
        <AbsoluteFill style={{ clipPath: `circle(${reveal}px at 470px 700px)` }}>
          <Oggi f={f} />
        </AbsoluteFill>
        <div style={{ position: "absolute", left: 470 - reveal, top: 700 - reveal, width: reveal * 2, height: reveal * 2, borderRadius: "50%", border: "14px solid #FFB14A", boxShadow: "0 0 60px 20px rgba(255,130,20,0.85), inset 0 0 60px 10px rgba(255,130,20,0.6)", boxSizing: "border-box" }} />
      </>
    );
  else if (f < 720) screen = <Oggi f={f} />;
  else if (f < 955) screen = <Piano f={f} />;
  else if (f < 966)
    screen = (
      <>
        <Piano f={f} />
        <AbsoluteFill style={{ opacity: ip(f, [955, 965], [0, 1]) }}>
          <Cucina f={f} />
        </AbsoluteFill>
      </>
    );
  else if (f < 1200) screen = <Cucina f={f} />;
  else
    screen = (
      <>
        <Progressi f={f} />
        {navTap > 0 ? <div style={{ position: "absolute", left: 832 - 90, top: 1600 - 90, width: 180, height: 180, borderRadius: 90, border: "6px solid rgba(255,122,0,0.95)", boxShadow: "0 0 40px rgba(255,122,0,0.8)", transform: `scale(${0.5 + (1 - navTap) * 0.9})`, opacity: navTap }} /> : null}
      </>
    );

  return (
    <AbsoluteFill>
      {/* scia in orbita: meta' dietro al telefono */}
      {f < 480 ? <Trail d={ORBIT} head={orbitHead} len={0.2} width={15} opacity={orbitOp} clip={{ x: 0, y: 0, w: 1080, h: 810 }} sparkle={false} /> : null}
      <DirBlur id="phoneBlur" x={bx} y={by}>
        <Phone pose={pose} glow={bell(f, 236, 262) + bell(f, 1410, 1440) * 0.6}>
          {screen}
        </Phone>
      </DirBlur>
      {f < 480 ? <Trail d={ORBIT} head={orbitHead} len={0.2} width={15} opacity={orbitOp} clip={{ x: 0, y: 810, w: 1080, h: 1110 }} /> : null}
      {/* S2: le aree dell'app escono dal telefono e gli orbitano attorno */}
      {f < 482
        ? TILES.map((tl, i) => {
            const a = sp(f, 255 + i * 15, { damping: 9, stiffness: 150 });
            const out = ip(f, [442 + i * 7, 468 + i * 7], [0, 1], EZ.in);
            if (f < 255 + i * 15 || out >= 1) return null;
            const fl = Math.sin(t * 1.7 + i * 1.3) * 14;
            const px = 540 + (tl.x - 540) * a * (1 - out) + pose.ry * 1.6 * tl.z * (1 - out);
            const py = 720 + (tl.y - 720) * a * (1 - out) + fl * (1 - out);
            return (
              <Img
                key={tl.n}
                src={staticFile(`img/${tl.n}`)}
                style={{
                  position: "absolute",
                  left: px - tl.w / 2,
                  top: py - tl.w / 2,
                  width: tl.w,
                  transform: `scale(${a * (1 - out)}) rotate(${(1 - Math.min(1, a)) * -90 + Math.sin(t * 1.3 + i) * 4 + out * 200}deg)`,
                  filter: "drop-shadow(0 24px 30px rgba(0,5,40,0.5))",
                }}
              />
            );
          })
        : null}
      {/* S5: ingredienti in parallasse */}
      {f >= 962 && f < 1206
        ? FOOD.map((fd, i) => {
            const a = sp(f, 966 + i * 6, { damping: 10, stiffness: 130 });
            const fl = Math.sin(t * 1.5 + i * 1.7) * 16;
            return (
              <Img
                key={fd.n}
                src={staticFile(`img/${fd.n}`)}
                style={{
                  position: "absolute",
                  left: 540 + (fd.x - 540) * a + pose.ry * 2 * fd.z - fd.w / 2,
                  top: 760 + (fd.y - 760) * a + fl + pose.yoff * (0.7 + fd.z * 0.5) - fd.w / 2,
                  width: fd.w,
                  transform: `scale(${Math.min(1.1, a)}) rotate(${fd.r + (1 - Math.min(1, a)) * 120 + Math.sin(t + i) * 5}deg)`,
                  filter: `drop-shadow(0 22px 26px rgba(0,5,40,0.45)) blur(${Math.abs(pose.yoff) > 40 ? 6 : 0}px)`,
                }}
              />
            );
          })
        : null}
      {/* scie di raccordo */}
      <Trail d={SWEEP_H} head={ip(f, [698, 740], [0, 1.4], EZ.inOut)} len={0.5} width={20} opacity={f > 696 && f < 744 ? 1 : 0} />
      <Trail d={SWEEP_V} head={ip(f, [1178, 1220], [0, 1.4], EZ.inOut)} len={0.5} width={22} tone="blue" opacity={f > 1176 && f < 1224 ? 1 : 0} />
    </AbsoluteFill>
  );
};
