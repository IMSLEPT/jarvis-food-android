import React from "react";
import { Img, staticFile } from "remotion";
import { H, PHONE_BASE, SCREEN_C, SCREEN_H, SCREEN_W, W } from "../theme";

export type PhonePose = {
  /** punto dello schermo (coordinate app 941x1672) che la camera inquadra */
  focus: { x: number; y: number };
  /** dove quel punto cade nel fotogramma 1080x1920 */
  target: { x: number; y: number };
  scale: number;
  rx: number;
  ry: number;
  rz: number;
  opacity?: number;
};

const BEZEL = 16;
const FRAME = 7;
const PAD = BEZEL + FRAME;
const BW = SCREEN_W + PAD * 2;
const BH = SCREEN_H + PAD * 2;
const R_OUT = 108;
const DEPTH = 8;
const METAL = "linear-gradient(135deg, #FFD7A6 0%, #D9772B 18%, #7A3A12 42%, #F3A458 62%, #5E2C0E 82%, #E08A3C 100%)";

/** Telefono 3D: cornice metallica con spessore, retro, schermo ritagliato e riflesso mobile. */
export const Phone: React.FC<{ pose: PhonePose; children: React.ReactNode; glow?: number }> = ({ pose, children, glow = 0 }) => {
  const s = PHONE_BASE * pose.scale;
  const cx = pose.target.x - (pose.focus.x - SCREEN_C.x) * s;
  const cy = pose.target.y - (pose.focus.y - SCREEN_C.y) * s;
  const glare = ((pose.ry % 360) + 360) % 360;
  const g = glare > 180 ? glare - 360 : glare;
  return (
    <div style={{ position: "absolute", left: 0, top: 0, width: W, height: H, perspective: 2600, perspectiveOrigin: "50% 42%", opacity: pose.opacity ?? 1 }}>
      <div
        style={{
          position: "absolute",
          left: cx - BW / 2,
          top: cy - BH / 2,
          width: BW,
          height: BH,
          transformStyle: "preserve-3d",
          transform: `scale(${s}) rotateZ(${pose.rz}deg) rotateX(${pose.rx}deg) rotateY(${pose.ry}deg)`,
        }}
      >
        {/* spessore */}
        {new Array(DEPTH).fill(0).map((_, i) => (
          <div
            key={i}
            style={{
              position: "absolute",
              inset: 0,
              borderRadius: R_OUT,
              background: i % 2 ? "#B5611F" : "#6A3310",
              transform: `translateZ(${-(i + 1) * 4}px)`,
            }}
          />
        ))}
        {/* retro */}
        <div
          style={{
            position: "absolute",
            inset: 0,
            borderRadius: R_OUT,
            background: "linear-gradient(150deg, #1B2748 0%, #0A1024 55%, #131C3A 100%)",
            border: `${FRAME}px solid #C9722C`,
            boxSizing: "border-box",
            transform: `translateZ(${-(DEPTH + 1) * 4}px) rotateY(180deg)`,
            backfaceVisibility: "hidden",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <Img src={staticFile("img/logo_hat.png")} style={{ width: 300, opacity: 0.9 }} />
        </div>
        {/* fronte */}
        <div
          style={{
            position: "absolute",
            inset: 0,
            borderRadius: R_OUT,
            background: METAL,
            backfaceVisibility: "hidden",
            boxShadow: `0 50px 110px rgba(0,6,40,0.55), 0 0 ${60 + glow * 120}px rgba(255,130,30,${0.18 + glow * 0.5})`,
          }}
        >
          <div style={{ position: "absolute", inset: FRAME, borderRadius: R_OUT - FRAME, background: "#05070D" }} />
          <div
            style={{
              position: "absolute",
              left: PAD,
              top: PAD,
              width: SCREEN_W,
              height: SCREEN_H,
              borderRadius: R_OUT - PAD,
              overflow: "hidden",
              background: "#F1F5FC",
            }}
          >
            {children}
            {/* riflesso che scorre con la rotazione */}
            <div
              style={{
                position: "absolute",
                inset: 0,
                background: `linear-gradient(112deg, rgba(255,255,255,0) ${28 + g * 1.4}%, rgba(255,255,255,0.2) ${40 + g * 1.4}%, rgba(255,255,255,0) ${54 + g * 1.4}%)`,
                pointerEvents: "none",
              }}
            />
          </div>
        </div>
      </div>
    </div>
  );
};
