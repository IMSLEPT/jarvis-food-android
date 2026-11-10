import React from "react";
import { AbsoluteFill, random } from "remotion";
import { H, W } from "../theme";

type Props = {
  frame: number;
  /** 0 = spazio scuro, 1 = blu luminoso */
  mood: number;
  /** spostamento "camera" in px: le particelle scorrono in parallasse */
  camX: number;
  camY: number;
  /** velocita' camera (px/frame): allunga le particelle in scie */
  velX: number;
  velY: number;
  energy: number;
};

const PARTICLES = new Array(46).fill(0).map((_, i) => ({
  x: random(`px${i}`) * W,
  y: random(`py${i}`) * H,
  z: 0.25 + random(`pz${i}`) * 0.75,
  r: 1.5 + random(`pr${i}`) * 3.5,
  warm: random(`pw${i}`) > 0.6,
  ph: random(`pp${i}`) * Math.PI * 2,
}));

const wrap = (v: number, m: number) => ((v % m) + m) % m;
const mix = (a: number[], b: number[], t: number) =>
  `rgb(${a.map((v, i) => Math.round(v + (b[i] - v) * t)).join(",")})`;

/** Fondo continuo per tutto lo spot: gradiente, bagliori, fasci di luce e particelle in parallasse. */
export const Background: React.FC<Props> = ({ frame, mood, camX, camY, velX, velY, energy }) => {
  const top = mix([3, 7, 22], [8, 44, 150], mood);
  const mid = mix([8, 22, 72], [18, 98, 226], mood);
  const bot = mix([4, 10, 34], [10, 60, 180], mood);
  const speed = Math.hypot(velX, velY);
  const ang = (Math.atan2(velY, velX) * 180) / Math.PI;
  const t = frame / 60;
  return (
    <AbsoluteFill style={{ background: `linear-gradient(170deg, ${top} 0%, ${mid} 48%, ${bot} 100%)`, overflow: "hidden" }}>
      {/* bagliori morbidi */}
      <div
        style={{
          position: "absolute",
          left: -500 + Math.sin(t * 0.5) * 120 - camX * 0.06,
          top: 200 + Math.cos(t * 0.4) * 140 - camY * 0.06,
          width: 1300,
          height: 1300,
          background: `radial-gradient(closest-side, rgba(40,140,255,${0.28 + mood * 0.25}), rgba(40,140,255,0))`,
        }}
      />
      <div
        style={{
          position: "absolute",
          left: 380 + Math.cos(t * 0.45) * 160 - camX * 0.1,
          top: 900 + Math.sin(t * 0.38) * 180 - camY * 0.1,
          width: 1300,
          height: 1300,
          background: `radial-gradient(closest-side, rgba(255,120,10,${0.2 + energy * 0.16}), rgba(255,120,10,0))`,
        }}
      />
      <div
        style={{
          position: "absolute",
          left: 500 + Math.sin(t * 0.3 + 2) * 100 - camX * 0.08,
          top: -500 + Math.cos(t * 0.5) * 80 - camY * 0.08,
          width: 1100,
          height: 1100,
          background: `radial-gradient(closest-side, rgba(0,220,255,${0.1 + mood * 0.22}), rgba(0,220,255,0))`,
        }}
      />
      {/* fasci di luce diagonali */}
      <div
        style={{
          position: "absolute",
          inset: -600,
          transform: `rotate(${-24 + Math.sin(t * 0.25) * 4}deg) translateX(${(-camX * 0.15) % 600}px)`,
          background:
            "repeating-linear-gradient(90deg, rgba(255,255,255,0) 0px, rgba(120,190,255,0.07) 90px, rgba(255,255,255,0) 210px, rgba(255,160,60,0.045) 330px, rgba(255,255,255,0) 520px)",
          opacity: 0.55 + mood * 0.45,
        }}
      />
      {/* particelle / scie di velocita' */}
      {PARTICLES.map((p, i) => {
        const x = wrap(p.x - camX * p.z * 0.6 + Math.sin(t * 0.6 + p.ph) * 14, W + 80) - 40;
        const y = wrap(p.y - camY * p.z * 0.6 - t * 22 * p.z, H + 80) - 40;
        const stretch = 1 + speed * p.z * 0.9;
        const tw = 0.45 + 0.55 * Math.sin(t * 2.2 + p.ph);
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: x,
              top: y,
              width: p.r * 2,
              height: p.r * 2,
              borderRadius: p.r,
              background: p.warm ? "rgba(255,190,110,0.95)" : "rgba(170,215,255,0.95)",
              boxShadow: `0 0 ${p.r * 4}px ${p.warm ? "rgba(255,140,30,0.9)" : "rgba(90,170,255,0.9)"}`,
              opacity: (0.25 + 0.6 * p.z) * tw * (0.7 + 0.3 * energy),
              transform: `rotate(${ang}deg) scaleX(${stretch})`,
            }}
          />
        );
      })}
      {/* vignettatura */}
      <AbsoluteFill
        style={{ background: "radial-gradient(ellipse at 50% 45%, rgba(0,0,0,0) 45%, rgba(0,3,20,0.55) 100%)" }}
      />
    </AbsoluteFill>
  );
};
