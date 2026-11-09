import React from "react";
import { C } from "../theme";

/**
 * Smartphone stilizzato (cornice, isola fotocamera, riflesso).
 * Il contenuto è lo schermo dell'app; dimensioni pensate per un canvas 1080×1920.
 */
export const Phone: React.FC<{
  children: React.ReactNode;
  width?: number;
  height?: number;
  screenBg?: string;
  style?: React.CSSProperties;
}> = ({ children, width = 700, height = 1420, screenBg = C.appBg, style }) => {
  const bezel = 18;
  return (
    <div
      style={{
        position: "relative",
        width,
        height,
        borderRadius: 96,
        padding: bezel,
        background: "linear-gradient(145deg, #3a3f5c 0%, #0d1024 40%, #22263d 100%)",
        boxShadow:
          "0 60px 120px rgba(0,0,0,0.55), 0 0 0 3px rgba(255,255,255,0.08), 0 0 80px rgba(242,94,0,0.25)",
        ...style,
      }}
    >
      <div
        style={{
          position: "relative",
          width: "100%",
          height: "100%",
          borderRadius: 80,
          overflow: "hidden",
          background: screenBg,
        }}
      >
        {children}
        {/* Isola fotocamera */}
        <div
          style={{
            position: "absolute",
            top: 22,
            left: "50%",
            width: 34,
            height: 34,
            marginLeft: -17,
            borderRadius: "50%",
            background: "#05060d",
            boxShadow: "inset 0 0 0 5px #1a1d2e",
          }}
        />
        {/* Riflesso del vetro */}
        <div
          style={{
            position: "absolute",
            inset: 0,
            background:
              "linear-gradient(120deg, rgba(255,255,255,0.18) 0%, rgba(255,255,255,0) 28%, rgba(255,255,255,0) 70%, rgba(255,255,255,0.06) 100%)",
            pointerEvents: "none",
          }}
        />
      </div>
    </div>
  );
};

/** Barra di stato Android minimale. */
export const StatusBar: React.FC<{ dark?: boolean }> = ({ dark }) => (
  <div
    style={{
      height: 78,
      display: "flex",
      alignItems: "flex-end",
      justifyContent: "space-between",
      padding: "0 46px 8px",
      fontFamily: "DM Sans",
      fontWeight: 600,
      fontSize: 24,
      color: dark ? "#fff" : C.text,
    }}
  >
    <span>9:41</span>
    <svg width="92" height="26" viewBox="0 0 92 26">
      {[0, 1, 2, 3].map((i) => (
        <rect key={i} x={i * 9} y={18 - i * 5} width="6" height={8 + i * 5} rx="1.5" fill="currentColor" />
      ))}
      <rect x="46" y="4" width="40" height="20" rx="5" fill="none" stroke="currentColor" strokeWidth="2.5" />
      <rect x="50" y="8" width="26" height="12" rx="2.5" fill="currentColor" />
      <rect x="87" y="10" width="3" height="8" rx="1" fill="currentColor" />
    </svg>
  </div>
);

/** Dimensioni dello schermo per i filmati reali (720×1432 ritagliati, rapporto 0,503). */
export const SCREEN_W = 620;
export const STATUS_H = 56;
export const VIDEO_H = Math.round((SCREEN_W * 1432) / 720); // 1233
export const PHONE_W = SCREEN_W + 36;
export const PHONE_H = STATUS_H + VIDEO_H + 36;
