import React from "react";
import { Img } from "remotion";
import { C } from "../theme";
import { EZ, ip, pulse } from "../anim";
import { Abs, Chevron, Clock, Flame, HeartIcon, SearchIcon, Shell, T, cardStyle, img, pop, rise } from "./kit";

export const CUCINA_T = { chips: [970, 978, 986, 994, 1002], rows: [972, 980, 988], heart: 1050, scroll: 1085, heart2: 1140 };

const CHIPS = [
  { x: 43, w: 167, label: "Tutte" },
  { x: 232, w: 152, label: "Veloci" },
  { x: 407, w: 140, label: "Sane" },
  { x: 570, w: 114, label: "Fit" },
  { x: 706, w: 196, label: "Proteiche" },
];
const ROWS = [
  { y: 985, photo: "ph_bowl.jpg", title: "Bowl proteica", min: 15, kcal: 450 },
  { y: 1153, photo: "ph_toast.jpg", title: "Toast avocado e uova", min: 10, kcal: 380 },
  { y: 1322, photo: "ph_insalata.jpg", title: "Insalata pollo e ceci", min: 15, kcal: 520 },
];

const Burst: React.FC<{ p: number; r: number }> = ({ p, r }) =>
  p > 0 && p < 1 ? (
    <>
      <div style={{ position: "absolute", left: -r, top: -r, width: r * 2, height: r * 2, borderRadius: r, border: `${6 * (1 - p)}px solid #FF4D6D`, transform: `scale(${0.4 + p})`, opacity: 1 - p }} />
      {new Array(8).fill(0).map((_, i) => {
        const a = (i / 8) * Math.PI * 2;
        return <div key={i} style={{ position: "absolute", left: Math.cos(a) * r * (0.6 + p * 1.1) - 6, top: Math.sin(a) * r * (0.6 + p * 1.1) - 6, width: 12, height: 12, borderRadius: 6, background: i % 2 ? "#FF8A1F" : "#FF4D6D", opacity: 1 - p, transform: `scale(${1 - p * 0.6})` }} />;
      })}
    </>
  ) : null;

export const Cucina: React.FC<{ f: number }> = ({ f }) => {
  const scroll = ip(f, [CUCINA_T.scroll, CUCINA_T.scroll + 34], [0, 178], EZ.inOut);
  const h1 = pop(f, CUCINA_T.heart);
  const b1 = ip(f, [CUCINA_T.heart, CUCINA_T.heart + 30], [0, 1], EZ.out);
  const h2 = pop(f, CUCINA_T.heart2);
  const b2 = ip(f, [CUCINA_T.heart2, CUCINA_T.heart2 + 28], [0, 1], EZ.out);
  const titleP = rise(f, 968, 30);
  const meta = ip(f, [972, 1012], [0, 1], EZ.out);
  return (
    <Shell title="Cucina" nav="nav_cucina.png">
      {/* area che scorre */}
      <div style={{ position: "absolute", left: 0, top: 500, width: 941, height: 1012, overflow: "hidden" }}>
        <div style={{ position: "absolute", left: 0, top: -500 - scroll, width: 941 }}>
          <div style={{ position: "absolute", left: 33, top: 519, width: 874, height: 430, ...cardStyle, borderRadius: 34, overflow: "hidden" }}>
            <Img src={img("ph_hero.jpg")} style={{ position: "absolute", left: 0, top: 0, width: 874, height: 303, objectFit: "cover", transform: `scale(${1.02 + ip(f, [960, 1200], [0.08, 0])})` }} />
            <div style={{ position: "absolute", left: 0, top: 303, width: 874, height: 127, background: "#fff", borderRadius: "26px 26px 0 0", marginTop: -22 }} />
            <div style={{ position: "absolute", left: 32, top: 316, overflow: "hidden", height: 60, width: 760 }}>
              <T x={0} y={8} size={45} weight={700} style={{ transform: `translateY(${(1 - titleP) * 60}px)`, letterSpacing: -1 }}>
                Pasta salmone e zucchine
              </T>
            </div>
            <Abs x={32} y={377} style={{ opacity: meta }}>
              <Clock s={34} />
            </Abs>
            <T x={82} y={380} size={32} weight={400} style={{ opacity: meta }}>
              {Math.round(20 * meta)} min
            </T>
            <div style={{ position: "absolute", left: 220, top: 378, width: 2, height: 36, background: "#C9D2E3", opacity: meta }} />
            <Abs x={256} y={376} style={{ opacity: meta }}>
              <Flame s={36} />
            </Abs>
            <T x={306} y={380} size={32} weight={400} style={{ opacity: meta }}>
              {Math.round(620 * meta)} kcal
            </T>
            <Abs x={812} y={346}>
              <Chevron s={40} />
            </Abs>
            {/* cuore */}
            <div style={{ position: "absolute", left: 802, top: 63, width: 0, height: 0 }}>
              <div style={{ position: "absolute", left: -44, top: -44, width: 88, height: 88, borderRadius: 44, background: h1 > 0.2 ? "linear-gradient(180deg,#FF5C7A,#F0244E)" : "rgba(255,255,255,0.92)", border: "4px solid #fff", boxSizing: "border-box", boxShadow: "0 6px 16px rgba(0,0,0,0.25)", transform: `scale(${1 + Math.sin(Math.min(1, h1) * Math.PI) * 0.5 + pulse(f, CUCINA_T.heart - 4, 4, 4) * -0.15})`, display: "flex", alignItems: "center", justifyContent: "center" }}>
                <HeartIcon s={48} fill={h1 > 0.2 ? "#fff" : "none"} stroke={h1 > 0.2 ? "none" : "#F0244E"} />
              </div>
              <Burst p={b1} r={74} />
            </div>
          </div>
          {ROWS.map((r, i) => {
            const s = rise(f, CUCINA_T.rows[i], 30);
            const fav = i === 0 ? h2 : 0;
            return (
              <div key={r.title} style={{ position: "absolute", left: 35, top: r.y, width: 874, height: 154, ...cardStyle, borderRadius: 30, transform: `translateY(${(1 - s) * 120}px)`, opacity: s }}>
                <div style={{ position: "absolute", left: 12, top: 8, width: 262, height: 137, borderRadius: 22, overflow: "hidden" }}>
                  <Img src={img(r.photo)} style={{ width: "100%", height: "100%", objectFit: "cover", transform: "scale(1.1)" }} />
                </div>
                <T x={294} y={34} size={35} weight={700} style={{ letterSpacing: -0.8 }}>
                  {r.title}
                </T>
                <Abs x={294} y={86}>
                  <Clock s={30} />
                </Abs>
                <T x={336} y={88} size={29} weight={400}>
                  {r.min} min
                </T>
                <div style={{ position: "absolute", left: 444, top: 86, width: 2, height: 32, background: "#C9D2E3" }} />
                <Abs x={466} y={84}>
                  <Flame s={32} />
                </Abs>
                <T x={508} y={88} size={29} weight={400}>
                  {r.kcal} kcal
                </T>
                <div style={{ position: "absolute", left: 760, top: 77, width: 0, height: 0 }}>
                  <div style={{ position: "absolute", left: -38, top: -38, width: 76, height: 76, borderRadius: 38, background: fav > 0.2 ? "linear-gradient(180deg,#FF5C7A,#F0244E)" : "#F3F6FB", boxShadow: "0 3px 8px rgba(40,70,140,0.15)", transform: `scale(${1 + Math.sin(Math.min(1, fav) * Math.PI) * 0.45})`, display: "flex", alignItems: "center", justifyContent: "center" }}>
                    <HeartIcon s={42} fill={fav > 0.2 ? "#fff" : "none"} stroke={fav > 0.2 ? "none" : "#16306F"} />
                  </div>
                  {i === 0 ? <Burst p={b2} r={60} /> : null}
                </div>
                <Abs x={822} y={60}>
                  <Chevron />
                </Abs>
              </div>
            );
          })}
        </div>
      </div>
      {/* ricerca + filtri (fissi) */}
      <div style={{ position: "absolute", left: 4, right: 4, top: 268, height: 236, background: C.uiBg, borderRadius: "44px 44px 0 0" }} />
      <div style={{ position: "absolute", left: 45, top: 303, width: 850, height: 76, borderRadius: 38, background: "#fff", border: "2px solid #C5D3EC", boxSizing: "border-box", transform: `translateX(${(1 - rise(f, 964, 30)) * 500}px)`, opacity: rise(f, 964, 30) }}>
        <Abs x={26} y={14}>
          <SearchIcon />
        </Abs>
        <T x={92} y={22} size={29} weight={400} italic color="#55668E">
          Cerca ricette, ingredienti o piatti…
        </T>
      </div>
      {CHIPS.map((c, i) => {
        const s = pop(f, CUCINA_T.chips[i]);
        return (
          <div key={c.label} style={{ position: "absolute", left: c.x, top: 410, width: c.w, height: 72, borderRadius: 36, background: i === 0 ? "linear-gradient(180deg,#FF8A1F,#FF5A00)" : "#fff", border: i === 0 ? "none" : "1.5px solid #E1E8F5", boxShadow: i === 0 ? "0 8px 18px rgba(255,100,0,0.4)" : "0 4px 10px rgba(40,70,140,0.10)", boxSizing: "border-box", transform: `translateX(${(1 - Math.min(1, s)) * 260}px) scale(${0.6 + 0.4 * s})`, opacity: Math.min(1, s * 2) }}>
            <T x={c.w / 2} y={20} w={c.w} size={31} weight={600} italic align="center" color={i === 0 ? "#fff" : C.ink}>
              {c.label}
            </T>
          </div>
        );
      })}
    </Shell>
  );
};
