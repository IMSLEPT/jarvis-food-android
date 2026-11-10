import React, { useMemo } from "react";
import { getLength, getPointAtLength } from "@remotion/paths";
import { H, W } from "../theme";

type Props = {
  d: string;
  /** posizione della testa lungo il percorso 0..1 */
  head: number;
  /** lunghezza della coda (frazione del percorso) */
  len?: number;
  width?: number;
  tone?: "orange" | "blue";
  opacity?: number;
  /** mostra solo la parte del percorso dentro questo rettangolo (per passare dietro/davanti agli oggetti) */
  clip?: { x: number; y: number; w: number; h: number };
  sparkle?: boolean;
};

const TONES = {
  orange: { glow: "#FF6A00", mid: "#FFA630", core: "#FFF3D6" },
  blue: { glow: "#1F7BFF", mid: "#5CC2FF", core: "#EAF7FF" },
};
const SEG = 7;

/** Scia luminosa a cometa: coda che si assottiglia + testa brillante. */
export const Trail: React.FC<Props> = ({ d, head, len = 0.3, width = 16, tone = "orange", opacity = 1, clip, sparkle = true }) => {
  const total = useMemo(() => getLength(d), [d]);
  if (opacity <= 0.001 || head <= 0 || head - len >= 1) return null;
  const col = TONES[tone];
  const h = Math.min(1, head);
  const pt = getPointAtLength(d, h * total);
  const id = `tc-${tone}-${Math.round(clip?.x ?? -1)}-${Math.round(clip?.y ?? -1)}`;
  const segs = new Array(SEG).fill(0).map((_, i) => {
    const k = (i + 1) / SEG; // 1 = coda intera e sottile ... testa corta e spessa
    const l = Math.min(len * (1 - (i / SEG) * 0.88), h);
    return { l, w: width * (0.18 + 0.82 * k * k), a: 0.2 + 0.8 * k };
  });
  const dash = (l: number) => ({ strokeDasharray: `${l} 3`, strokeDashoffset: -(h - l) });
  const headVisible = head <= 1.001 && (!clip || (pt.x >= clip.x && pt.x <= clip.x + clip.w && pt.y >= clip.y && pt.y <= clip.y + clip.h));
  return (
    <svg width={W} height={H} viewBox={`0 0 ${W} ${H}`} style={{ position: "absolute", left: 0, top: 0, opacity, overflow: "visible" }}>
      {clip ? (
        <defs>
          <clipPath id={id}>
            <rect x={clip.x} y={clip.y} width={clip.w} height={clip.h} />
          </clipPath>
        </defs>
      ) : null}
      <g clipPath={clip ? `url(#${id})` : undefined} fill="none" strokeLinecap="round">
        <path d={d} pathLength={1} stroke={col.glow} strokeWidth={width * 3.4} opacity={0.2} style={{ filter: "blur(14px)" }} {...dash(Math.min(len * 0.8, h))} />
        {segs.map((s, i) => (
          <path key={i} d={d} pathLength={1} stroke={i > SEG - 3 ? col.mid : col.glow} strokeWidth={s.w} opacity={s.a * 0.75} {...dash(s.l)} />
        ))}
        <path d={d} pathLength={1} stroke={col.core} strokeWidth={width * 0.3} opacity={0.95} {...dash(Math.min(len * 0.45, h))} />
      </g>
      {sparkle && headVisible ? (
        <g transform={`translate(${pt.x} ${pt.y})`}>
          <circle r={width * 2.6} fill={col.glow} opacity={0.35} style={{ filter: "blur(10px)" }} />
          <circle r={width * 0.85} fill={col.core} />
          <ellipse rx={width * 3.2} ry={width * 0.16} fill={col.core} opacity={0.9} />
          <ellipse rx={width * 0.16} ry={width * 2.4} fill={col.core} opacity={0.9} />
        </g>
      ) : null}
    </svg>
  );
};

/** Ellisse inclinata come path SVG (parte dal punto piu' a destra, senso orario). */
export const ellipsePath = (cx: number, cy: number, rx: number, ry: number, rotDeg = 0, turns = 1) => {
  const r = (rotDeg * Math.PI) / 180;
  const pts: string[] = [];
  const n = 72 * turns;
  for (let i = 0; i <= n; i++) {
    const a = (i / 72) * Math.PI * 2;
    const x = Math.cos(a) * rx;
    const y = Math.sin(a) * ry;
    pts.push(`${i === 0 ? "M" : "L"}${(cx + x * Math.cos(r) - y * Math.sin(r)).toFixed(1)} ${(cy + x * Math.sin(r) + y * Math.cos(r)).toFixed(1)}`);
  }
  return pts.join(" ");
};
