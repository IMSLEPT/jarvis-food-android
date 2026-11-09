import { Video } from "@remotion/media";
import React from "react";
import { staticFile } from "remotion";
import { PHONE_H, PHONE_W, Phone, STATUS_H, StatusBar, VIDEO_H } from "./Phone";

/**
 * Telefono con una registrazione reale dell'app (public/footage/*.mp4).
 * La barra di stato originale (con il timer di registrazione) è stata ritagliata
 * e sostituita da una barra pulita.
 */
export const AppScreen: React.FC<{
  clip: string;
  playbackRate?: number;
  trimBefore?: number;
  style?: React.CSSProperties;
  children?: React.ReactNode;
}> = ({ clip, playbackRate = 1, trimBefore, style, children }) => (
  <Phone width={PHONE_W} height={PHONE_H} screenBg="#F7F7F7" style={style}>
    <div style={{ height: STATUS_H, background: "#F7F7F7" }}>
      <div style={{ marginTop: -22 }}>
        <StatusBar />
      </div>
    </div>
    <div style={{ position: "relative", height: VIDEO_H, overflow: "hidden" }}>
      <Video
        src={staticFile(`footage/${clip}.mp4`)}
        muted
        playbackRate={playbackRate}
        trimBefore={trimBefore}
        objectFit="cover"
        style={{ width: "100%", height: "100%", display: "block" }}
      />
      {children}
    </div>
  </Phone>
);
