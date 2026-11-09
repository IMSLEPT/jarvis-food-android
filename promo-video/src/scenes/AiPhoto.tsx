import React from "react";
import { AbsoluteFill, Easing, Img, Interactive, interpolate, staticFile, useCurrentFrame } from "remotion";
import { C } from "../theme";

const DETECTIONS = [
  { at: 102, label: "Lenticchie", kcal: "180 kcal", x: 70, y: 330, w: 400, h: 300, color: C.orange },
  { at: 132, label: "Avocado", kcal: "120 kcal", x: 330, y: 140, w: 330, h: 260, color: C.green },
  { at: 162, label: "Zucca", kcal: "45 kcal", x: 460, y: 470, w: 290, h: 240, color: C.gold },
];

/**
 * Scena 4 (14–18 s) · Foto del piatto → riconoscimento AI.
 * Scansione tra 24 e 96, rilevamenti sui beat 102/132/162, totale a 192.
 */
export const AiPhoto: React.FC = () => {
  const frame = useCurrentFrame();
  const scan = interpolate(frame, [24, 96], [0, 1], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.bezier(0.45, 0, 0.55, 1),
  });
  const CARD = 860;

  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      <Interactive.Div
        name="Titolo AI"
        style={{
          position: "absolute",
          top: 140,
          width: 940,
          textAlign: "center",
          fontFamily: "Bricolage",
          fontWeight: 800,
          fontSize: 100,
          lineHeight: 1.02,
          color: "#FFFFFF",
          opacity: interpolate(frame, [10, 26], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          translate: interpolate(frame, [10, 40], ["0px -40px", "0px 0px"], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.bezier(0.16, 1, 0.3, 1),
          }),
        }}
      >
        Scatta una foto.
      </Interactive.Div>
      <Interactive.Div
        name="Sottotitolo AI"
        style={{
          position: "absolute",
          top: 262,
          width: 940,
          textAlign: "center",
          fontFamily: "DM Sans",
          fontWeight: 600,
          fontSize: 54,
          color: "#FF8A1C",
          opacity: interpolate(frame, [22, 40], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        Jarvis riconosce il piatto
      </Interactive.Div>

      {/* Scheda foto con mirino */}
      <div
        style={{
          position: "absolute",
          top: 420,
          width: CARD,
          height: CARD,
          scale: `${interpolate(frame, [0, 40], [0.82, 1], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.spring({ damping: 15 }),
          })}`,
          rotate: `${interpolate(frame, [0, 40], [-4, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.spring({ damping: 15 }),
          })}deg`,
        }}
      >
        <div
          style={{
            position: "absolute",
            inset: 0,
            borderRadius: 64,
            overflow: "hidden",
            boxShadow: "0 50px 120px rgba(0,0,0,0.55)",
          }}
        >
          <Img
            src={staticFile("food/quinoa_ceci.webp")}
            style={{
              width: "100%",
              height: "100%",
              objectFit: "cover",
              scale: `${1.12 - scan * 0.08}`,
            }}
          />
          {/* Velo di analisi che si ritira dietro la linea di scansione */}
          <div
            style={{
              position: "absolute",
              left: 0,
              right: 0,
              top: `${scan * 100}%`,
              bottom: 0,
              background: "rgba(10,18,60,0.45)",
              backdropFilter: "saturate(0.3)",
            }}
          />
          {/* Linea di scansione */}
          <div
            style={{
              position: "absolute",
              left: 0,
              right: 0,
              top: `calc(${scan * 100}% - 3px)`,
              height: 6,
              background: `linear-gradient(90deg, transparent, ${C.orangeLight}, #FFFFFF, ${C.orangeLight}, transparent)`,
              boxShadow: `0 0 40px 12px ${C.orange}AA`,
              opacity: scan > 0 && scan < 1 ? 1 : 0,
            }}
          />

          {/* Riquadri di rilevamento */}
          {DETECTIONS.map((d) => {
            const p = interpolate(frame, [d.at - 2, d.at + 16], [0, 1], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
              easing: Easing.spring({ damping: 12 }),
            });
            return (
              <div
                key={d.label}
                style={{
                  position: "absolute",
                  left: d.x,
                  top: d.y,
                  width: d.w,
                  height: d.h,
                  border: `5px solid ${d.color}`,
                  borderRadius: 28,
                  boxShadow: `0 0 30px ${d.color}99, inset 0 0 30px ${d.color}55`,
                  opacity: Math.min(1, p * 1.5),
                  scale: `${1.25 - 0.25 * p}`,
                }}
              >
                <div
                  style={{
                    position: "absolute",
                    left: -5,
                    top: -66,
                    padding: "8px 20px",
                    borderRadius: 18,
                    background: d.color,
                    color: "#FFFFFF",
                    fontFamily: "DM Sans",
                    fontWeight: 600,
                    fontSize: 34,
                    whiteSpace: "nowrap",
                    boxShadow: "0 10px 30px rgba(0,0,0,0.35)",
                  }}
                >
                  {d.label} · {d.kcal}
                </div>
              </div>
            );
          })}
        </div>

        {/* Angoli del mirino */}
        {[
          [0, 0],
          [1, 0],
          [0, 1],
          [1, 1],
        ].map(([cx, cy], i) => (
          <div
            key={i}
            style={{
              position: "absolute",
              left: cx ? undefined : -26,
              right: cx ? -26 : undefined,
              top: cy ? undefined : -26,
              bottom: cy ? -26 : undefined,
              width: 110,
              height: 110,
              borderColor: "#FFFFFF",
              borderStyle: "solid",
              borderWidth: 0,
              borderTopWidth: cy ? 0 : 10,
              borderBottomWidth: cy ? 10 : 0,
              borderLeftWidth: cx ? 0 : 10,
              borderRightWidth: cx ? 10 : 0,
              borderTopLeftRadius: !cx && !cy ? 44 : 0,
              borderTopRightRadius: cx && !cy ? 44 : 0,
              borderBottomLeftRadius: !cx && cy ? 44 : 0,
              borderBottomRightRadius: cx && cy ? 44 : 0,
              opacity: 0.9,
              translate: `${(cx ? 1 : -1) * interpolate(frame, [4, 34], [60, 0], {
                extrapolateLeft: "clamp",
                extrapolateRight: "clamp",
                easing: Easing.bezier(0.16, 1, 0.3, 1),
              })}px ${(cy ? 1 : -1) * interpolate(frame, [4, 34], [60, 0], {
                extrapolateLeft: "clamp",
                extrapolateRight: "clamp",
                easing: Easing.bezier(0.16, 1, 0.3, 1),
              })}px`,
            }}
          />
        ))}
      </div>

      {/* Totale aggiunto al diario */}
      <div
        style={{
          position: "absolute",
          top: 1370,
          display: "flex",
          alignItems: "center",
          gap: 26,
          padding: "30px 46px",
          borderRadius: 44,
          background: "rgba(255,255,255,0.96)",
          boxShadow: "0 30px 80px rgba(0,0,0,0.45)",
          opacity: interpolate(frame, [190, 200], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
          translate: `0px ${interpolate(frame, [190, 214], [80, 0], {
            extrapolateLeft: "clamp",
            extrapolateRight: "clamp",
            easing: Easing.spring({ damping: 12 }),
          })}px`,
        }}
      >
        <svg width="72" height="72" viewBox="0 0 72 72">
          <circle cx="36" cy="36" r="36" fill={C.green} />
          <path
            d="M20 37 L31 48 L53 25"
            stroke="#fff"
            strokeWidth="8"
            fill="none"
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeDasharray="60"
            strokeDashoffset={interpolate(frame, [198, 216], [60, 0], {
              extrapolateLeft: "clamp",
              extrapolateRight: "clamp",
            })}
          />
        </svg>
        <div style={{ display: "flex", flexDirection: "column" }}>
          <div style={{ fontFamily: "Bricolage", fontWeight: 800, fontSize: 62, color: C.text, lineHeight: 1 }}>
            345 kcal
          </div>
          <div style={{ fontFamily: "DM Sans", fontWeight: 500, fontSize: 34, color: C.muted, marginTop: 6 }}>
            aggiunte al pranzo
          </div>
        </div>
      </div>

      <Interactive.Div
        name="Nota AI"
        style={{
          position: "absolute",
          top: 1620,
          fontFamily: "DM Sans",
          fontWeight: 500,
          fontSize: 44,
          color: "rgba(255,255,255,0.85)",
          opacity: interpolate(frame, [205, 225], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
        }}
      >
        AI offline sul telefono o online
      </Interactive.Div>
    </AbsoluteFill>
  );
};
