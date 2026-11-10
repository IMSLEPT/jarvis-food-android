import React from "react";
import { Img, staticFile } from "remotion";
import { C, FONT, SCREEN_H, SCREEN_W } from "../theme";
import { EZ, ip, itNum, sp } from "../anim";

export const img = (n: string) => staticFile(`img/${n}`);

export const Abs: React.FC<{ x: number; y: number; w?: number; h?: number; style?: React.CSSProperties; children?: React.ReactNode }> = ({ x, y, w, h, style, children }) => (
  <div style={{ position: "absolute", left: x, top: y, width: w, height: h, ...style }}>{children}</div>
);

export const cardStyle: React.CSSProperties = {
  background: "#FFFFFF",
  borderRadius: 28,
  border: "1.5px solid #E4EBF7",
  boxShadow: "0 8px 20px rgba(40,70,140,0.10), 0 1px 3px rgba(40,70,140,0.08)",
  boxSizing: "border-box",
};

export const T: React.FC<{ x: number; y: number; size: number; w?: number; weight?: number; italic?: boolean; color?: string; align?: "left" | "right" | "center"; children: React.ReactNode; style?: React.CSSProperties }> = ({ x, y, size, w, weight = 600, italic, color = C.ink, align = "left", children, style }) => (
  <div
    style={{
      position: "absolute",
      left: align === "left" ? x : align === "right" ? x - (w ?? 400) : x - (w ?? 400) / 2,
      top: y,
      width: align === "left" ? w : (w ?? 400),
      textAlign: align,
      fontFamily: FONT,
      fontSize: size,
      fontWeight: weight,
      fontStyle: italic ? "italic" : "normal",
      color,
      lineHeight: 1,
      whiteSpace: "nowrap",
      letterSpacing: -0.3,
      ...style,
    }}
  >
    {children}
  </div>
);

// ---------- icone vettoriali ----------
export const Flame: React.FC<{ s?: number }> = ({ s = 30 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24">
    <path d="M12.5 1.5c.6 3.6-1.4 5.4-3 7.300C7.800 10.700 6.500 12.600 6.500 15a5.500 5.500 0 0 0 11 0c0-2.200-.9-3.800-1.800-5.200-.5 1.300-1.200 2.100-2.200 2.600.6-3.500-.2-7.700-1-10.900z" fill="none" stroke="#FF7A00" strokeWidth="2.3" strokeLinejoin="round" />
    <path d="M12 21.500a2.800 2.800 0 0 0 2.800-2.800c0-1.500-1.300-2.400-2.800-4.200-1.500 1.800-2.800 2.700-2.800 4.200A2.800 2.800 0 0 0 12 21.500z" fill="#FF7A00" />
  </svg>
);
export const Clock: React.FC<{ s?: number }> = ({ s = 30 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24">
    <circle cx="12" cy="12" r="10" fill="#FF7A00" />
    <path d="M12 6.500V12l3.600 2.200" stroke="#fff" strokeWidth="2.400" fill="none" strokeLinecap="round" />
  </svg>
);
export const Chevron: React.FC<{ s?: number; color?: string }> = ({ s = 34, color = "#1F5FD6" }) => (
  <svg width={s} height={s} viewBox="0 0 24 24">
    <path d="M8.500 4.500 16 12l-7.500 7.500" stroke={color} strokeWidth="2.800" fill="none" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);
export const HeartIcon: React.FC<{ s?: number; fill?: string; stroke?: string }> = ({ s = 40, fill = "#fff", stroke = "none" }) => (
  <svg width={s} height={s} viewBox="0 0 24 24">
    <path d="M12 20.500C6 16 3 12.900 3 9.300 3 6.700 5 4.800 7.400 4.800c1.800 0 3.400 1 4.600 2.700 1.200-1.700 2.800-2.700 4.600-2.700C19 4.800 21 6.700 21 9.300c0 3.600-3 6.700-9 11.200z" fill={fill} stroke={stroke} strokeWidth="2" strokeLinejoin="round" />
  </svg>
);
export const CheckIcon: React.FC<{ s?: number; color?: string; w?: number }> = ({ s = 30, color = "#fff", w = 3.4 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24">
    <path d="M5 12.800 10 17.500 19.500 7" stroke={color} strokeWidth={w} fill="none" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);
export const SearchIcon: React.FC<{ s?: number }> = ({ s = 44 }) => (
  <svg width={s} height={s} viewBox="0 0 24 24">
    <circle cx="10.500" cy="10.500" r="6.500" stroke="#16306F" strokeWidth="2.200" fill="none" />
    <path d="M15.500 15.500 21 21" stroke="#16306F" strokeWidth="2.400" strokeLinecap="round" />
  </svg>
);

// ---------- struttura comune delle schermate ----------
export const Shell: React.FC<{ title: string; nav: string; navY?: number; children: React.ReactNode; titleIcon?: React.ReactNode }> = ({ title, nav, navY = 1512, children, titleIcon }) => (
  <div style={{ position: "absolute", left: 0, top: 0, width: SCREEN_W, height: SCREEN_H, background: C.uiBg, fontFamily: FONT, overflow: "hidden" }}>
    <div style={{ position: "absolute", left: 4, right: 4, top: 162, height: 190, borderRadius: "44px 44px 0 0", background: "linear-gradient(180deg, #1668DC 0%, #0C4DB8 100%)" }} />
    <div style={{ position: "absolute", left: 4, right: 4, top: 268, bottom: 0, borderRadius: "44px 44px 0 0", background: C.uiBg }} />
    <T x={titleIcon ? 165 : 72} y={182} size={74} weight={800} italic color="#fff" style={{ letterSpacing: -1.5 }}>
      {title}
    </T>
    {titleIcon ? <Abs x={78} y={180}>{titleIcon}</Abs> : null}
    {children}
    <Img src={img("header.png")} style={{ position: "absolute", left: 0, top: 0, width: SCREEN_W }} />
    <Img src={img(nav)} style={{ position: "absolute", left: 0, top: navY, width: SCREEN_W }} />
  </div>
);

// ---------- anello calorie ----------
export const Ring: React.FC<{ cx: number; cy: number; r: number; stroke: number; progress: number; id: string; headGlow?: number }> = ({ cx, cy, r, stroke, progress, id, headGlow = 0 }) => {
  const c = 2 * Math.PI * r;
  const p = Math.max(0, Math.min(1, progress));
  const a = p * Math.PI * 2 - Math.PI / 2;
  const size = (r + stroke) * 2 + 80;
  const o = size / 2;
  return (
    <svg width={size} height={size} style={{ position: "absolute", left: cx - o, top: cy - o, overflow: "visible" }}>
      <defs>
        <linearGradient id={id} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#7BE01A" />
          <stop offset="1" stopColor="#17B845" />
        </linearGradient>
      </defs>
      <circle cx={o} cy={o} r={r} fill="none" stroke="#D8DEE9" strokeWidth={stroke} />
      {p > 0.002 ? (
        <circle cx={o} cy={o} r={r} fill="none" stroke={`url(#${id})`} strokeWidth={stroke} strokeLinecap="round" strokeDasharray={`${c * p} ${c}`} transform={`rotate(-90 ${o} ${o})`} />
      ) : null}
      {headGlow > 0.01 ? (
        <g transform={`translate(${o + Math.cos(a) * r} ${o + Math.sin(a) * r})`} opacity={headGlow}>
          <circle r={stroke * 1.9} fill="#FF8A1F" opacity={0.55} style={{ filter: "blur(8px)" }} />
          <circle r={stroke * 0.62} fill="#FFF6DE" />
        </g>
      ) : null}
    </svg>
  );
};

export const MacroBar: React.FC<{ x: number; y: number; w: number; p: number; color: string; tail?: { from: number; color: string } }> = ({ x, y, w, p, color, tail }) => (
  <div style={{ position: "absolute", left: x, top: y, width: w, height: 10, borderRadius: 5, background: "#D5DBE6", overflow: "hidden" }}>
    <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: w * p, borderRadius: 5, background: tail && p > tail.from ? `linear-gradient(90deg, ${color} 0, ${color} ${(tail.from / p) * 100}%, ${tail.color} ${(tail.from / p) * 100}%)` : color }} />
  </div>
);

// ---------- riga pasto ----------
export const MealRow: React.FC<{ x: number; y: number; w: number; h: number; photo: string; title: string; kcal: number; show: number; press?: number; glow?: number }> = ({ x, y, w, h, photo, title, kcal, show, press = 0, glow = 0 }) => (
  <div
    style={{
      position: "absolute",
      left: x,
      top: y,
      width: w,
      height: h,
      ...cardStyle,
      borderRadius: 30,
      transformOrigin: "50% 0%",
      transform: `perspective(1400px) rotateX(${(1 - show) * -82}deg) translateY(${(1 - show) * 30}px) scale(${1 - press * 0.035})`,
      opacity: Math.min(1, show * 2.2),
      boxShadow: glow > 0 ? `0 0 0 ${4 * glow}px rgba(255,122,0,${0.9 * glow}), 0 10px 40px rgba(255,122,0,${0.45 * glow})` : cardStyle.boxShadow,
    }}
  >
    <div style={{ position: "absolute", left: 17, top: (h - 96) / 2, width: 196, height: 96, borderRadius: 22, overflow: "hidden" }}>
      <Img src={img(photo)} style={{ width: "100%", height: "100%", objectFit: "cover", transform: "scale(1.12)" }} />
    </div>
    <T x={245} y={h / 2 - 40} size={37} weight={700}>
      {title}
    </T>
    <Abs x={243} y={h / 2 + 6}>
      <Flame s={32} />
    </Abs>
    <T x={284} y={h / 2 + 8} size={30} weight={500} color={C.blueText}>
      {itNum(Math.round(kcal * Math.min(1, show)))} kcal
    </T>
    <Abs x={w - 60} y={h / 2 - 18}>
      <Chevron />
    </Abs>
  </div>
);

export const pop = (f: number, at: number) => sp(f, at, { damping: 9, stiffness: 190, mass: 0.6 });
export const rise = (f: number, at: number, d = 26) => ip(f, [at, at + d], [0, 1], EZ.out);
