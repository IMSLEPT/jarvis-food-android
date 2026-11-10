import React from "react";
import { Img } from "remotion";
import { C } from "../theme";
import { EZ, ip, itNum } from "../anim";
import { Abs, CheckIcon, Chevron, Ring, Shell, T, cardStyle, img, pop, rise } from "./kit";

export const PROG_T = { line: 1222, lineEnd: 1290, badge: 1290, stats: 1322, bars: 1340, media: 1400 };

/** punti del grafico peso (coordinate schermata) */
export const PTS: [number, number][] = [
  [160, 507], [250, 538], [330, 558], [411, 579], [492, 597], [590, 613], [689, 630], [843, 652],
];
const SEGL = PTS.slice(1).map((p, i) => Math.hypot(p[0] - PTS[i][0], p[1] - PTS[i][1]));
const TOTAL = SEGL.reduce((a, b) => a + b, 0);
const CUM = SEGL.reduce<number[]>((acc, l) => [...acc, acc[acc.length - 1] + l], [0]).map((v) => v / TOTAL);

/** posizione della testa della linea (0..1) -> punto in coordinate schermata */
export const chartHead = (p: number): [number, number] => {
  const t = Math.max(0, Math.min(1, p));
  for (let i = 1; i < PTS.length; i++) {
    if (t <= CUM[i]) {
      const k = (t - CUM[i - 1]) / (CUM[i] - CUM[i - 1]);
      return [PTS[i - 1][0] + (PTS[i][0] - PTS[i - 1][0]) * k, PTS[i - 1][1] + (PTS[i][1] - PTS[i - 1][1]) * k];
    }
  }
  return PTS[PTS.length - 1];
};
export const chartProgress = (f: number) => ip(f, [PROG_T.line, PROG_T.lineEnd], [0, 1], EZ.inOut);

const YL = ["75,5", "74,5", "73,5", "72,5", "71,5", "70,5"];
const XL: [string, number][] = [["1 gen", 168], ["8 gen", 330], ["15 gen", 493], ["22 gen", 667], ["29 gen", 837]];
const STATS = [
  { x: 35, w: 209, icon: "ic_peso.png", label: "Peso", from: 75, to: 71.2, unit: "kg", dec: 1 },
  { x: 263, w: 201, icon: "ic_grasso.png", label: "Grasso", from: 17.1, to: 14.3, unit: "%", dec: 1 },
  { x: 483, w: 202, icon: "ic_muscolo.png", label: "Muscolo", from: 56.2, to: 57.5, unit: "kg", dec: 1 },
  { x: 705, w: 203, icon: "ic_acqua.png", label: "Acqua media", from: 1.2, to: 1.9, unit: "L", dec: 1 },
];
const BARS = [
  { d: "Lun", x: 202, top: 1110, pct: 95 },
  { d: "Mar", x: 306, top: 1100, pct: 98 },
  { d: "Mer", x: 408, top: 1110, pct: 93 },
  { d: "Gio", x: 511, top: 1101, pct: 97 },
  { d: "Ven", x: 615, top: 1093, pct: 100 },
  { d: "Sab", x: 720, top: 1103, pct: 96 },
  { d: "Dom", x: 825, top: 1106, pct: 94 },
];

export const Progressi: React.FC<{ f: number }> = ({ f }) => {
  const lp = chartProgress(f);
  const head = chartHead(lp);
  const linePts = PTS.filter((_, i) => CUM[i] <= lp).map((p) => p.join(","));
  if (lp > 0 && lp < 1) linePts.push(head.join(","));
  const badge = pop(f, PROG_T.badge);
  const arrow = ip(f, [PROG_T.badge + 6, PROG_T.badge + 28], [0, 1], EZ.out);
  const goal = ip(f, [1350, 1404], [0, 1], EZ.out);
  return (
    <Shell title="Progressi" nav="nav_progressi.png" navY={1528}>
      {/* schede */}
      {[
        { x: 38, w: 272, l: "Peso", on: true },
        { x: 322, w: 296, l: "Composizione" },
        { x: 630, w: 275, l: "Calorie" },
      ].map((t) => (
        <div key={t.l} style={{ position: "absolute", left: t.x, top: 295, width: t.w, height: 72, borderRadius: 36, background: t.on ? "linear-gradient(180deg,#FF8A1F,#FF5A00)" : "#fff", border: t.on ? "none" : "1.5px solid #E1E8F5", boxShadow: t.on ? "0 8px 18px rgba(255,100,0,0.4)" : "0 4px 10px rgba(40,70,140,0.10)", boxSizing: "border-box" }}>
          <T x={t.w / 2} y={20} w={t.w} size={t.on ? 35 : 32} weight={t.on ? 800 : 600} italic={t.on} align="center" color={t.on ? "#fff" : C.ink}>
            {t.l}
          </T>
        </div>
      ))}
      {/* grafico peso */}
      <div style={{ position: "absolute", left: 35, top: 398, width: 872, height: 345, ...cardStyle, borderRadius: 30 }} />
      <T x={65} y={410} size={31} weight={700} italic>
        Andamento del peso
      </T>
      <T x={65} y={445} size={25} weight={400} color={C.mute}>
        Ultime 4 settimane
      </T>
      {YL.map((l, i) => (
        <React.Fragment key={l}>
          <T x={110} y={482 + i * 38.600} w={60} size={22} weight={400} align="right" color={C.mute}>
            {l}
          </T>
          <div style={{ position: "absolute", left: 130, top: 492 + i * 38.600, width: 745, height: 1.500, background: "#E3E9F3" }} />
        </React.Fragment>
      ))}
      <T x={98} y={706} w={40} size={18} weight={400} align="right" color={C.mute}>
        kg
      </T>
      {XL.map(([l, x]) => (
        <React.Fragment key={l}>
          <T x={x} y={697} w={120} size={22} weight={400} align="center" color={C.mute}>
            {l}
          </T>
          <div style={{ position: "absolute", left: x - 8, top: 492, width: 1.500, height: 193, background: "#E9EEF6" }} />
        </React.Fragment>
      ))}
      <svg width={941} height={1672} style={{ position: "absolute", left: 0, top: 0 }}>
        <defs>
          <linearGradient id="areaG" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#3D9BFF" stopOpacity="0.55" />
            <stop offset="1" stopColor="#3D9BFF" stopOpacity="0.04" />
          </linearGradient>
          <clipPath id="areaClip">
            <rect x={150} y={480} width={Math.max(0, head[0] - 150)} height={215} />
          </clipPath>
        </defs>
        <polygon clipPath="url(#areaClip)" fill="url(#areaG)" points={`${PTS.map((p) => p.join(",")).join(" ")} 843,685 160,685`} />
        {linePts.length > 1 ? <polyline points={linePts.join(" ")} fill="none" stroke="#1D6BF3" strokeWidth={5.500} strokeLinecap="round" strokeLinejoin="round" /> : null}
        {lp > 0 && lp < 1 ? (
          <g transform={`translate(${head[0]} ${head[1]})`}>
            <circle r={34} fill="#3D9BFF" opacity={0.45} style={{ filter: "blur(10px)" }} />
            <circle r={9} fill="#fff" />
            <ellipse rx={46} ry={2.5} fill="#fff" opacity={0.9} />
            <ellipse rx={2.5} ry={34} fill="#fff" opacity={0.9} />
          </g>
        ) : null}
        {PTS.map((p, i) => {
          const s = lp >= CUM[i] - 0.001 && lp > 0 ? pop(f, PROG_T.line + CUM[i] * (PROG_T.lineEnd - PROG_T.line) - 2) : 0;
          const last = i === 0 || i === PTS.length - 1;
          return s > 0 ? <circle key={i} cx={p[0]} cy={p[1]} r={(last ? 11 : 8.500) * s} fill="#fff" stroke="#1D6BF3" strokeWidth={4.500} /> : null;
        })}
        {/* freccia verde */}
        {arrow > 0 ? (
          <g opacity={Math.min(1, arrow * 3)}>
            <path d="M622 512 C 650 580, 720 612, 782 606" fill="none" stroke="#35B925" strokeWidth={13} strokeLinecap="round" pathLength={1} strokeDasharray={`${arrow} 2`} />
            {arrow > 0.85 ? <path d="M770 578 L 804 604 L 768 632 Z" fill="#35B925" transform={`scale(${(arrow - 0.85) / 0.15})`} style={{ transformOrigin: "786px 605px" }} /> : null}
          </g>
        ) : null}
      </svg>
      <div style={{ position: "absolute", left: 658, top: 445, width: 218, height: 76, borderRadius: 30, background: "linear-gradient(180deg,#5FD82F,#27AE2A)", boxShadow: "0 10px 22px rgba(40,170,40,0.45)", transform: `scale(${badge * (1 + (1 - Math.min(1, badge)) * 0.6)}) rotate(${(1 - Math.min(1, badge)) * -14}deg)`, opacity: Math.min(1, badge * 3) }}>
        <T x={109} y={15} w={218} size={45} weight={800} italic align="center" color="#fff">
          −3,8 kg
        </T>
      </div>
      {/* statistiche */}
      {STATS.map((s, i) => {
        const r = rise(f, 1218 + i * 5, 28);
        const c = ip(f, [PROG_T.stats + i * 6, PROG_T.stats + 46 + i * 6], [0, 1], EZ.out);
        return (
          <div key={s.label} style={{ position: "absolute", left: s.x, top: 762, width: s.w, height: 170, ...cardStyle, transform: `translateY(${(1 - r) * 80}px) scale(${0.9 + 0.1 * r})`, opacity: r }}>
            <Img src={img(s.icon)} style={{ position: "absolute", left: s.w / 2 - 32, top: 12, height: 58, mixBlendMode: "multiply" }} />
            <T x={s.w / 2} y={76} w={s.w} size={27} weight={500} align="center">
              {s.label}
            </T>
            <T x={s.w / 2} y={114} w={s.w} size={39} weight={800} align="center" style={{ letterSpacing: -1 }}>
              {itNum(s.from + (s.to - s.from) * c, s.dec)} {s.unit}
            </T>
          </div>
        );
      })}
      {/* aderenza calorica */}
      <div style={{ position: "absolute", left: 35, top: 958, width: 872, height: 355, ...cardStyle, borderRadius: 30, opacity: rise(f, 1228, 28), transform: `translateY(${(1 - rise(f, 1228, 28)) * 80}px)` }} />
      <div style={{ opacity: rise(f, 1238, 28) }}>
        <T x={63} y={972} size={33} weight={700} italic>
          Aderenza calorica
        </T>
        <T x={63} y={1008} size={25} weight={400} color={C.mute}>
          Ultimi 7 giorni
        </T>
        {["2.800", "2.600", "2.400", "2.200", "2.000", "1.800"].map((l, i) => (
          <T key={l} x={120} y={1049 + i * 31.200} w={80} size={22} weight={400} align="right" color={C.mute}>
            {l}
          </T>
        ))}
        <T x={104} y={1236} w={60} size={18} weight={400} align="right" color={C.mute}>
          kcal
        </T>
        <div style={{ position: "absolute", left: 140, top: 1214, width: 745, height: 2, background: "#B9C3D6" }} />
        <div style={{ position: "absolute", left: 140, top: 1058, width: 2, height: 157, background: "#B9C3D6" }} />
        {BARS.map((b) => (
          <T key={b.d} x={b.x} y={1226} w={100} size={23} weight={400} align="center" color={C.mute}>
            {b.d}
          </T>
        ))}
        <div style={{ position: "absolute", left: 180, top: 1272, width: 28, height: 20, borderRadius: 5, background: "#7ADB6A" }} />
        <T x={220} y={1271} size={22} weight={400} color={C.mute}>
          Calorie assunte
        </T>
        <div style={{ position: "absolute", left: 408, top: 1281, width: 46, height: 0, borderTop: "3.5px dashed #FF8A1F" }} />
        <T x={468} y={1271} size={22} weight={400} color={C.mute}>
          Obiettivo giornaliero (2.600 kcal)
        </T>
      </div>
      {BARS.map((b, i) => {
        const g = ip(f, [PROG_T.bars + i * 8, PROG_T.bars + i * 8 + 26], [0, 1], EZ.back);
        const h = (1214 - b.top) * g;
        const lab = pop(f, PROG_T.bars + i * 8 + 16);
        return (
          <React.Fragment key={b.d}>
            <div style={{ position: "absolute", left: b.x - 39, top: 1214 - h, width: 78, height: Math.max(0, h), borderRadius: "8px 8px 0 0", background: b.pct === 100 ? "linear-gradient(180deg,#4FD12E,#35B925)" : "linear-gradient(180deg,#8FE37C,#6AD45A)" }} />
            <div style={{ position: "absolute", left: b.x - 38, top: 1056, width: 76, height: 30, borderRadius: 15, background: "#E6F8E2", transform: `scale(${lab})`, opacity: Math.min(1, lab * 2) }}>
              <T x={38} y={5} w={76} size={20} weight={600} align="center" color="#1B3A1A">
                {b.pct} %
              </T>
            </div>
          </React.Fragment>
        );
      })}
      <div style={{ position: "absolute", left: 140, top: 1093, width: 745 * ip(f, [PROG_T.bars, PROG_T.bars + 50], [0, 1], EZ.out), height: 0, borderTop: "3.5px dashed #FF8A1F" }} />
      <div style={{ position: "absolute", left: 660, top: 975, width: 230, height: 56, borderRadius: 28, background: "#F1F5FB", border: "1.5px solid #E1E8F5", boxSizing: "border-box", transform: `scale(${pop(f, PROG_T.media)})` }}>
        <div style={{ position: "absolute", left: 14, top: 9, width: 36, height: 36, borderRadius: 18, background: "#22B43A", display: "flex", alignItems: "center", justifyContent: "center" }}>
          <CheckIcon s={24} />
        </div>
        <T x={66} y={14} size={26} weight={400}>
          Media: <b style={{ fontWeight: 800 }}>96 %</b>
        </T>
      </div>
      {/* obiettivo */}
      <div style={{ position: "absolute", left: 35, top: 1336, width: 872, height: 176, ...cardStyle, borderRadius: 30, opacity: rise(f, 1238, 28) }}>
        <Ring cx={122} cy={88} r={64} stroke={15} progress={0.76 * goal} id="ringGoal" />
        <T x={122} y={66} w={160} size={42} weight={800} italic align="center">
          {Math.round(76 * goal)} %
        </T>
        <div style={{ position: "absolute", left: 232, top: 28, width: 2, height: 122, background: "#DDE4F0" }} />
        <T x={264} y={28} size={30} weight={600}>
          Progresso verso l’obiettivo
        </T>
        <div style={{ position: "absolute", left: 264, top: 72, width: 190, height: 66, borderRadius: 26, background: "#DFF6DA" }}>
          <T x={95} y={14} w={190} size={40} weight={800} italic align="center" color="#2AA52A">
            −3,8 kg
          </T>
        </div>
        <T x={470} y={92} size={27} weight={400}>
          da 75,0 kg a <b style={{ fontWeight: 700 }}>71,2 kg</b>
        </T>
        <Abs x={812} y={68}>
          <Chevron s={40} />
        </Abs>
      </div>
    </Shell>
  );
};
