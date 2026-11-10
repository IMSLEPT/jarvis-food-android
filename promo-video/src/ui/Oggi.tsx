import React from "react";
import { Img } from "remotion";
import { C } from "../theme";
import { EZ, ip, itNum, pulse } from "../anim";
import { Abs, MacroBar, MealRow, Ring, Shell, T, cardStyle, img, pop, rise } from "./kit";

/** Tempi (frame assoluti) della schermata Oggi */
export const OGGI_T = { ring: 486, ringEnd: 540, bars: 500, actions: [555, 570, 585, 600], tap: 640, rows: 566 };

const ACTIONS = [
  { x: 45, w: 205, icon: "ic_aggiungi.png", label: "Aggiungi", iw: 78 },
  { x: 268, w: 200, icon: "ic_foto.png", label: "Foto piatto", iw: 78 },
  { x: 485, w: 196, icon: "ic_frigo.png", label: "Frigo", iw: 66 },
  { x: 698, w: 197, icon: "ic_digiuno.png", label: "Digiuno", iw: 72 },
];
const MEALS = [
  { y: 958, photo: "ph_colazione.jpg", title: "Colazione", kcal: 520 },
  { y: 1092, photo: "ph_spuntino.jpg", title: "Spuntino", kcal: 190 },
  { y: 1222, photo: "ph_pranzo.jpg", title: "Pranzo", kcal: 900 },
  { y: 1354, photo: "ph_cena.jpg", title: "Cena", kcal: 680 },
];
const MACROS = [
  { y: 316, label: "Proteine", v: 148, p: 0.585, color: "#17B26A", dot: "#F0527A" },
  { y: 399, label: "Carboidrati", v: 254, p: 0.635, color: "#FDB913", dot: "#FDB913" },
  { y: 485, label: "Grassi", v: 72, p: 0.635, color: "#1570EF", dot: "#12B8A6", tail: { from: 0.5, color: "#FF5A1F" } },
];

export const Oggi: React.FC<{ f: number }> = ({ f }) => {
  const ringP = ip(f, [OGGI_T.ring, OGGI_T.ringEnd], [0, 1], EZ.outSoft);
  const done = pulse(f, OGGI_T.ringEnd - 2, 5, 22);
  const beatPulse = Math.max(pulse(f, 600, 3, 14), pulse(f, 660, 3, 14)) * 0.02;
  const tap = pulse(f, OGGI_T.tap, 4, 12);
  const ripple = ip(f, [OGGI_T.tap + 2, OGGI_T.tap + 30], [0, 1], EZ.out);
  const hyd = ip(f, [606, 656], [0, 0.72], EZ.out);
  return (
    <Shell title="Oggi" nav="nav_oggi.png">
      <div style={{ position: "absolute", left: 223, top: 443, transform: `scale(${1 + done * 0.045 + beatPulse})` }}>
        <Ring cx={0} cy={0} r={149} stroke={24} progress={ringP * 0.79} id="ringOggi" headGlow={f < OGGI_T.ringEnd + 8 ? 1 - done * 0.4 : ip(f, [OGGI_T.ringEnd + 8, OGGI_T.ringEnd + 24], [0.6, 0])} />
      </div>
      <T x={223} y={388} w={300} size={66} weight={800} align="center" style={{ letterSpacing: -2 }}>
        {itNum(Math.round(2050 * ringP))}
      </T>
      <T x={223} y={458} w={300} size={33} weight={500} italic align="center" color={C.blueText}>
        / 2.600 kcal
      </T>
      {MACROS.map((m, i) => {
        const p = ip(f, [OGGI_T.bars + i * 10, OGGI_T.bars + i * 10 + 46], [0, 1], EZ.out);
        return (
          <React.Fragment key={m.label}>
            <div style={{ position: "absolute", left: 440, top: m.y - 2, width: 42, height: 42, borderRadius: 21, background: m.dot, transform: `scale(${pop(f, OGGI_T.bars + i * 10 - 6)})`, display: "flex", alignItems: "center", justifyContent: "center" }}>
              {i === 0 ? (
                <svg width="26" height="26" viewBox="0 0 24 24">
                  <path d="M5 12.500 10 17 19 7.500" stroke="#fff" strokeWidth="3.200" fill="none" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              ) : null}
            </div>
            <T x={505} y={m.y + 2} size={31} weight={600}>
              {m.label}
            </T>
            <T x={875} y={m.y + 2} w={200} size={31} weight={600} align="right">
              {Math.round(m.v * p)} g
            </T>
            <MacroBar x={505} y={m.y + 46} w={370} p={m.p * p} color={m.color} tail={m.tail} />
          </React.Fragment>
        );
      })}
      {ACTIONS.map((a, i) => {
        const s = pop(f, OGGI_T.actions[i]);
        const pr = i === 0 ? tap : 0;
        return (
          <div key={a.label} style={{ position: "absolute", left: a.x, top: 605, width: a.w, height: 142, ...cardStyle, transform: `scale(${s * (1 - pr * 0.09)}) translateY(${(1 - s) * 40}px)`, opacity: Math.min(1, s * 2) }}>
            <Img src={img(a.icon)} style={{ position: "absolute", left: (a.w - a.iw) / 2, top: 12, width: a.iw }} />
            <T x={a.w / 2} y={94} w={a.w} size={29} weight={600} align="center">
              {a.label}
            </T>
            {i === 0 && ripple > 0 && ripple < 1 ? (
              <div style={{ position: "absolute", left: a.w / 2 - 110, top: 50 - 110, width: 220, height: 220, borderRadius: 110, border: "5px solid rgba(255,122,0,0.9)", transform: `scale(${0.3 + ripple * 0.9})`, opacity: 1 - ripple }} />
            ) : null}
          </div>
        );
      })}
      {/* idratazione / vitamine / attivita' */}
      {[
        { x: 25, w: 316, d: 0 },
        { x: 358, w: 277, d: 6 },
        { x: 652, w: 266, d: 12 },
      ].map((c, i) => {
        const s = rise(f, OGGI_T.rows - 6 + c.d);
        return (
          <div key={i} style={{ position: "absolute", left: c.x, top: 786, width: c.w, height: 136, ...cardStyle, background: i === 1 ? "#F1FBF2" : "#fff", transform: `translateY(${(1 - s) * 70}px)`, opacity: s }}>
            {i === 0 ? (
              <>
                <Img src={img("ic_idra.png")} style={{ position: "absolute", left: 18, top: 14, width: 54 }} />
                <T x={90} y={22} size={27} weight={500}>
                  Idratazione
                </T>
                <T x={90} y={58} size={34} weight={700}>
                  {itNum(1.8 * (hyd / 0.72), 1)} <span style={{ fontSize: 24, fontWeight: 500 }}>L / 2,5 L</span>
                </T>
                <div style={{ position: "absolute", left: 26, top: 102, width: 264, height: 14, borderRadius: 7, background: "#E1E7F0" }}>
                  <div style={{ width: 264 * hyd, height: 14, borderRadius: 7, background: "linear-gradient(90deg,#18B4F5,#1297E8)" }} />
                </div>
              </>
            ) : i === 1 ? (
              <>
                <Img src={img("ic_vit.png")} style={{ position: "absolute", left: 18, top: 16, width: 62, mixBlendMode: "multiply" }} />
                <T x={92} y={22} size={27} weight={500}>
                  Vitamine
                </T>
                <T x={92} y={52} size={27} weight={500}>
                  e minerali
                </T>
                <div style={{ position: "absolute", left: 196, top: 88, width: 40, height: 40, borderRadius: 20, background: "#22B43A", transform: `scale(${pop(f, 624)})`, display: "flex", alignItems: "center", justifyContent: "center" }}>
                  <svg width="24" height="24" viewBox="0 0 24 24">
                    <path d="M5 12.500 10 17 19 7.500" stroke="#fff" strokeWidth="3.400" fill="none" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </div>
              </>
            ) : (
              <>
                <Img src={img("ic_att.png")} style={{ position: "absolute", left: 18, top: 10, width: 66 }} />
                <T x={98} y={26} size={29} weight={500}>
                  Attività
                </T>
                <T x={96} y={68} size={32} weight={700} color={C.blueText}>
                  {Math.round(420 * s)} <span style={{ fontWeight: 500 }}>kcal</span>
                </T>
              </>
            )}
          </div>
        );
      })}
      {MEALS.map((m, i) => (
        <MealRow key={m.title} x={35} y={m.y} w={872} h={118} photo={m.photo} title={m.title} kcal={m.kcal} show={rise(f, OGGI_T.rows + 10 + i * 9, 30)} />
      ))}
    </Shell>
  );
};
