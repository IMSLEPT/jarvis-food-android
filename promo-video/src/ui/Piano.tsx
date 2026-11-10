import React from "react";
import { Img } from "remotion";
import { C } from "../theme";
import { EZ, ip, itNum, pulse } from "../anim";
import { Abs, CheckIcon, Chevron, MacroBar, MealRow, Ring, Shell, T, cardStyle, img, pop, rise } from "./kit";
import { sp } from "../anim";

export const PIANO_T = { days: [752, 765, 778], ring: 760, rows: [780, 795, 810, 825], checks: [876, 891, 906], press: 934 };

const DAYS = ["Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom"];
const DX = [110, 233, 355, 473, 594, 714, 838];
const MEALS = [
  { y: 680, photo: "ph_colazione.jpg", title: "Colazione", kcal: 520 },
  { y: 797, photo: "ph_spuntino.jpg", title: "Spuntino", kcal: 190 },
  { y: 913, photo: "ph_pranzo.jpg", title: "Pranzo", kcal: 900 },
  { y: 1030, photo: "ph_cena.jpg", title: "Cena", kcal: 680 },
];
const MACROS = [
  { y: 462, label: "Proteine", v: 148, p: 0.55, color: "#17B26A", dot: "#F0527A" },
  { y: 520, label: "Carboidrati", v: 254, p: 0.63, color: "#FDB913", dot: "#FDB913" },
  { y: 580, label: "Grassi", v: 72, p: 0.58, color: "#1570EF", dot: "#12B8A6", tail: { from: 0.44, color: "#FF5A1F" } },
];
const ITEMS = [
  { x: 85, y: 1262, pic: "li_banane.png", label: "Banane", check: 0 },
  { x: 85, y: 1334, pic: "li_salmone.png", label: "Salmone", check: 1 },
  { x: 85, y: 1404, pic: "li_pomodorini.png", label: "Pomodorini", check: -1 },
  { x: 527, y: 1262, pic: "li_zucchine.png", label: "Zucchine", check: -1 },
  { x: 527, y: 1334, pic: "li_uova.png", label: "Uova", check: -1 },
  { x: 527, y: 1404, pic: "li_yogurt.png", label: "Yogurt greco", check: 2 },
];

export const Piano: React.FC<{ f: number }> = ({ f }) => {
  // il selettore scorre Lun -> Mar -> Mer a tempo
  const s1 = sp(f, PIANO_T.days[1], { damping: 14, stiffness: 220 });
  const s2 = sp(f, PIANO_T.days[2], { damping: 12, stiffness: 220 });
  const pillX = DX[0] + (DX[1] - DX[0]) * s1 + (DX[2] - DX[1]) * s2;
  const stretch = 1 + Math.sin(Math.min(1, s1) * Math.PI) * 0.25 * (f < PIANO_T.days[2] ? 1 : 0) + Math.sin(Math.min(1, s2) * Math.PI) * 0.25;
  const active = f < PIANO_T.days[1] + 4 ? 0 : f < PIANO_T.days[2] + 4 ? 1 : 2;
  const ringP = ip(f, [PIANO_T.ring, PIANO_T.ring + 54], [0, 1], EZ.outSoft);
  const press = pulse(f, PIANO_T.press, 5, 40);
  const glow = ip(f, [PIANO_T.press, PIANO_T.press + 8], [0, 1]);
  return (
    <Shell title="Piano" nav="nav_piano.png" titleIcon={<CalIcon />}>
      <div style={{ position: "absolute", left: 37, top: 277, width: 870, height: 70, borderRadius: 35, ...cardStyle }} />
      <div style={{ position: "absolute", left: pillX - 56, top: 282, width: 112, height: 60, borderRadius: 24, background: "linear-gradient(180deg,#FF8A1F,#FF5A00)", boxShadow: "0 6px 16px rgba(255,100,0,0.45)", transform: `scaleX(${stretch})` }} />
      {DAYS.map((d, i) => (
        <T key={d} x={DX[i]} y={296} w={120} size={31} weight={600} align="center" color={i === active ? "#fff" : C.ink}>
          {d}
        </T>
      ))}
      <div style={{ position: "absolute", left: 33, top: 366, width: 875, height: 294, ...cardStyle, borderRadius: 34 }} />
      <Ring cx={220} cy={512} r={118} stroke={21} progress={ringP} id="ringPiano" headGlow={ip(f, [PIANO_T.ring + 40, PIANO_T.ring + 62], [1, 0])} />
      <T x={220} y={462} w={300} size={58} weight={800} align="center" style={{ letterSpacing: -2 }}>
        {itNum(Math.round(2600 * ringP))}
      </T>
      <T x={220} y={524} w={300} size={31} weight={500} align="center" color={C.blueText}>
        / 2.600 kcal
      </T>
      <div style={{ position: "absolute", left: 376, top: 392, width: 2, height: 242, background: "#DDE4F0" }} />
      <Img src={img("ic_target.png")} style={{ position: "absolute", left: 412, top: 382, width: 56, mixBlendMode: "multiply" }} />
      <T x={480} y={384} size={26} weight={500}>
        Obiettivo giornaliero
      </T>
      <T x={480} y={416} size={29} weight={600} color={C.blueText}>
        2.600 kcal
      </T>
      {MACROS.map((m, i) => {
        const p = ip(f, [PIANO_T.ring + 6 + i * 8, PIANO_T.ring + 50 + i * 8], [0, 1], EZ.out);
        return (
          <React.Fragment key={m.label}>
            <div style={{ position: "absolute", left: 408, top: m.y + 2, width: 32, height: 32, borderRadius: 16, background: m.dot }} />
            <T x={460} y={m.y + 3} size={27} weight={600}>
              {m.label}
            </T>
            <T x={876} y={m.y + 3} w={200} size={29} weight={600} align="right">
              {Math.round(m.v * p)} g
            </T>
            <MacroBar x={460} y={m.y + 36} w={416} p={m.p * p} color={m.color} tail={m.tail} />
          </React.Fragment>
        );
      })}
      {MEALS.map((m, i) => (
        <MealRow key={m.title} x={33} y={m.y} w={876} h={106} photo={m.photo} title={m.title} kcal={m.kcal} show={ip(f, [PIANO_T.rows[i], PIANO_T.rows[i] + 28], [0, 1], EZ.backSoft)} press={i === 2 ? press : 0} glow={i === 2 ? glow : 0} />
      ))}
      {/* lista spesa */}
      <div style={{ position: "absolute", left: 25, top: 1163, width: 890, height: 322, ...cardStyle, borderRadius: 34, transform: `translateY(${(1 - rise(f, 828, 34)) * 90}px)`, opacity: rise(f, 828, 34) }}>
        <Img src={img("ic_cart.png")} style={{ position: "absolute", left: 35, top: 9, width: 70, mixBlendMode: "multiply" }} />
        <T x={120} y={20} size={42} weight={800} italic>
          Lista spesa
        </T>
        <T x={818} y={26} w={220} size={29} weight={500} align="right" color="#1F5FD6">
          6 articoli
        </T>
        <Abs x={832} y={22}>
          <Chevron />
        </Abs>
        <div style={{ position: "absolute", left: 18, top: 80, width: 852, height: 224, borderRadius: 24, background: "#FAFCFF", border: "1.5px solid #E8EEF8" }} />
        <div style={{ position: "absolute", left: 448, top: 98, width: 2, height: 190, background: "#DDE4F0" }} />
        {ITEMS.map((it) => {
          const at = it.check >= 0 ? PIANO_T.checks[it.check] : 0;
          const c = it.check >= 0 ? pop(f, at) : 0;
          return (
            <React.Fragment key={it.label}>
              <div style={{ position: "absolute", left: it.x - 25 - 18, top: it.y - 1163 - 16, width: 34, height: 34, borderRadius: 8, border: `3px solid ${c > 0.3 ? "#FF6B00" : "#8C97AD"}`, background: c > 0.3 ? "#FF6B00" : "#fff", boxSizing: "border-box", transform: `scale(${1 + Math.sin(Math.min(1, c) * Math.PI) * 0.35})`, display: "flex", alignItems: "center", justifyContent: "center" }}>
                {c > 0.3 ? <CheckIcon s={26} /> : null}
              </div>
              <Img src={img(it.pic)} style={{ position: "absolute", left: it.x - 25 + 36, top: it.y - 1163 - 32, height: 64, mixBlendMode: "multiply" }} />
              <T x={it.x - 25 + 160} y={it.y - 1163 - 14} size={29} weight={500}>
                {it.label}
              </T>
            </React.Fragment>
          );
        })}
      </div>
    </Shell>
  );
};

const CalIcon: React.FC = () => (
  <svg width="70" height="70" viewBox="0 0 24 24">
    <rect x="3" y="5" width="18" height="16" rx="3.500" fill="none" stroke="#fff" strokeWidth="2.200" />
    <path d="M3 10h18M8 3v4M16 3v4M7.500 14h9M7.500 17.500h5" stroke="#fff" strokeWidth="2.200" strokeLinecap="round" />
  </svg>
);
