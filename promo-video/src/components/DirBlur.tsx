import React from "react";
import { AbsoluteFill } from "remotion";

/** Sfocatura direzionale (motion blur economico) applicata a tutto il contenuto. */
export const DirBlur: React.FC<{ id: string; x?: number; y?: number; children: React.ReactNode }> = ({ id, x = 0, y = 0, children }) => {
  const on = x > 0.4 || y > 0.4;
  return (
    <AbsoluteFill style={{ filter: on ? `url(#${id})` : undefined }}>
      {on ? (
        <svg width="0" height="0" style={{ position: "absolute" }}>
          <filter id={id} x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation={`${x.toFixed(2)} ${y.toFixed(2)}`} />
          </filter>
        </svg>
      ) : null}
      {children}
    </AbsoluteFill>
  );
};
