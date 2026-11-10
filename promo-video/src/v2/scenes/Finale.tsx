import React from "react";
import { AbsoluteFill, Easing, Img, interpolate, Sequence, staticFile, useCurrentFrame } from "remotion";
import { AppScreen } from "../../components/AppScreen";
import { FONT_BODY, FONT_TITLE } from "../../theme";
import { P, Pill, Pop, Stage, Words } from "../Kinetic";
import { beatPulse, useSceneStart, useWords } from "../tl";

const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const OUT = Easing.bezier(0.16, 1, 0.3, 1);

// ─────────────────────────────────────────────────────────────────────────────
/**
 * "Piano pasti. Ricette. Spesa. Progressi." — un taglio per ogni parola pronunciata:
 * il colore di fondo si espande a cerchio, l'etichetta e la schermata cambiano insieme.
 */
export const Montage: React.FC = () => {
  const frame = useCurrentFrame();
  const s0 = useSceneStart();
  const w = useWords("L10");
  const cuts = [
    { at: 0, clip: "piano", label: "Piano pasti", color: P.orange, rate: 1 },
    { at: w[2].start - 2, clip: "cucina", label: "Ricette", color: P.green, rate: 1 },
    { at: w[3].start - 2, clip: "spesa", label: "Spesa", color: P.blue, rate: 1 },
    { at: w[4].start - 2, clip: "progressi", label: "Progressi", color: P.violet, rate: 0.75 },
  ];
  const idx = cuts.reduce((a, c, i) => (frame >= c.at ? i : a), 0);
  const since = frame - cuts[idx].at;
  const punch = Math.exp(-since / 5);
  const pulse = beatPulse(s0 + frame, 6);

  return (
    <AbsoluteFill style={{ background: cuts[0].color, overflow: "hidden" }}>
      {/* fondi a cerchio che si espandono a ogni taglio */}
      {cuts.slice(1).map((c) => (
        <AbsoluteFill
          key={c.clip}
          style={{
            background: c.color,
            clipPath: `circle(${interpolate(frame, [c.at, c.at + 12], [0, 2300], { ...clamp, easing: Easing.bezier(0.5, 0, 0.2, 1) })}px at 540px 1100px)`,
          }}
        />
      ))}
      <div
        style={{
          position: "absolute",
          left: 540 - 650,
          top: 1100 - 650,
          width: 1300,
          height: 1300,
          borderRadius: "50%",
          background: "rgba(255,255,255,0.14)",
          scale: `${1 + pulse * 0.04}`,
        }}
      />
      <AbsoluteFill style={{ alignItems: "center" }}>
        {/* etichetta: sale da dietro una maschera a ogni taglio */}
        <div style={{ position: "absolute", top: 130, height: 190, overflow: "hidden", width: 1080, display: "flex", justifyContent: "center" }}>
          <div
            style={{
              fontFamily: FONT_TITLE,
              fontWeight: 800,
              fontSize: 160,
              lineHeight: 1.1,
              letterSpacing: -3,
              color: "#FFFFFF",
              whiteSpace: "nowrap",
              translate: `0px ${interpolate(since, [0, 11], [190, 0], { ...clamp, easing: OUT })}px`,
            }}
          >
            {cuts[idx].label}
          </div>
        </div>
        <div
          style={{
            position: "absolute",
            top: 380,
            scale: `${0.86 + punch * 0.05}`,
            rotate: `${(idx % 2 ? 1 : -1) * punch * 2.5}deg`,
            transformOrigin: "50% 0%",
          }}
        >
          {cuts.map((c, i) => (
            <Sequence key={c.clip} from={c.at} durationInFrames={(cuts[i + 1]?.at ?? 400) - c.at} layout="none">
              <AppScreen clip={c.clip} playbackRate={c.rate} />
            </Sequence>
          ))}
        </div>
        <div style={{ position: "absolute", top: 1640, display: "flex", gap: 16 }}>
          {cuts.map((c, i) => (
            <div
              key={c.clip}
              style={{ width: i === idx ? 100 : 26, height: 26, borderRadius: 13, background: i <= idx ? "#FFFFFF" : "rgba(255,255,255,0.35)" }}
            />
          ))}
        </div>
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Tutto, in un'unica app." — tre schermate si aprono a ventaglio. */
export const AllInOne: React.FC = () => {
  const frame = useCurrentFrame();
  const open = interpolate(frame, [2, 30], [0, 1], { ...clamp, easing: Easing.spring({ damping: 13 }) });
  const phones = [
    { clip: "cucina", r: -13, x: -330 },
    { clip: "progressi", r: 13, x: 330 },
    { clip: "piano", r: 0, x: 0 },
  ];
  return (
    <Stage
      bg={P.paper}
      shapes={[
        { color: P.softOrange, size: 1200, x: 540, y: 1350 },
        { color: P.orange, size: 230, x: 110, y: 1720, kind: "ring" },
      ]}
    >
      <AbsoluteFill style={{ alignItems: "center" }}>
        <div style={{ position: "absolute", top: 150 }}>
          <Words line="L11" size={140} color={P.ink} accent={{ 2: P.orange, 3: P.orange }} width={1000} />
        </div>
        {phones.map((p) => (
          <div
            key={p.clip}
            style={{
              position: "absolute",
              top: 600,
              scale: p.r === 0 ? "0.78" : "0.66",
              translate: `${p.x * open}px ${(p.r === 0 ? 0 : 70 * open) + (1 - Math.min(1, frame / 14)) * 500}px`,
              rotate: `${p.r * open}deg`,
              transformOrigin: "50% 0%",
            }}
          >
            <AppScreen clip={p.clip} playbackRate={0.5} />
          </div>
        ))}
      </AbsoluteFill>
    </Stage>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Il futuro delle app è già qui. Ed è nel tuo telefono." */
export const Future: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L12");
  const tPhone = w[11].start;
  return (
    <Stage
      bg={P.ink}
      shapes={[
        { color: "#22368A", size: 1500, x: 540, y: 1050 },
        { color: P.orange, size: 760, x: 540, y: 1080 },
      ]}
    >
      <AbsoluteFill style={{ alignItems: "center" }}>
        <div style={{ position: "absolute", top: 130 }}>
          <Words line="L12" to={7} size={112} accent={{ 1: P.orangeLight }} />
        </div>
        {[0, 1, 2].map((i) => (
          <div
            key={i}
            style={{
              position: "absolute",
              top: 700 - i * 70,
              width: 780 + i * 170,
              height: 780 + i * 170,
              borderRadius: "50%",
              border: `3px solid rgba(255,255,255,${0.45 - i * 0.12})`,
              transform: `rotateX(72deg) rotateZ(${frame * (0.7 + i * 0.35)}deg)`,
              opacity: interpolate(frame, [i * 6, i * 6 + 20], [0, 1], clamp),
            }}
          />
        ))}
        <div
          style={{
            position: "absolute",
            top: 500,
            perspective: 1800,
            scale: `${interpolate(frame, [0, 30], [0.5, 0.74], { ...clamp, easing: Easing.spring({ damping: 14 }) })}`,
            transformOrigin: "50% 0%",
          }}
        >
          <div
            style={{
              transform: `rotateY(${Math.sin(frame / 30) * 10}deg) rotateX(${6 + Math.cos(frame / 40) * 3}deg)`,
              translate: `0px ${Math.sin(frame / 25) * 14}px`,
              scale: `${1 + interpolate(frame, [tPhone, tPhone + 6, tPhone + 30], [0, 0.06, 0], clamp)}`,
            }}
          >
            <AppScreen clip="diary" playbackRate={0.6} />
          </div>
        </div>
        <div style={{ position: "absolute", top: 1580 }}>
          <Words line="L12" from={7} size={92} accent={{ 10: P.orangeLight, 11: P.orangeLight }} />
        </div>
      </AbsoluteFill>
    </Stage>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Jarvis Food. Mangia meglio, ogni giorno." + call to action. */
export const Outro: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L13");
  const tEnd = w[w.length - 1].end;
  return (
    <Stage
      bg={P.paper}
      shapes={[
        { color: P.softOrange, size: 1300, x: 540, y: 640 },
        { color: P.softNavy, size: 520, x: 60, y: 1750 },
        { color: P.orange, size: 210, x: 960, y: 1560, kind: "ring" },
      ]}
    >
      <AbsoluteFill style={{ alignItems: "center" }}>
        {[0, 1].map((i) => {
          const p = interpolate(frame, [i * 8, 70 + i * 8], [0, 1], { ...clamp, easing: OUT });
          return (
            <div
              key={i}
              style={{
                position: "absolute",
                top: 340,
                width: 600,
                height: 600,
                borderRadius: "50%",
                border: `${12 - i * 4}px solid ${i ? P.gold : P.orange}`,
                scale: `${0.6 + p * 2.2}`,
                opacity: (1 - p) * 0.9,
              }}
            />
          );
        })}
        <Img
          src={staticFile("jarvis_logo.png")}
          style={{
            position: "absolute",
            top: 380,
            width: 520,
            height: 520,
            borderRadius: "50%",
            boxShadow: "0 40px 90px rgba(23,40,107,0.35)",
            scale: `${interpolate(frame, [0, 34], [2.2, 1], { ...clamp, easing: Easing.spring({ damping: 13 }) })}`,
            opacity: interpolate(frame, [0, 6], [0, 1], clamp),
          }}
        />
        <div style={{ position: "absolute", top: 980 }}>
          <Words line="L13" to={2} size={168} color={P.ink} accent={{ 1: P.orange }} width={1040} />
        </div>
        <div style={{ position: "absolute", top: 1200 }}>
          <Words line="L13" from={2} size={72} font="body" weight={500} color={P.ink} />
        </div>
        <Pop at={tEnd - 4} damping={10} style={{ position: "absolute", top: 1390 }}>
          <Pill bg={P.orange} size={64} style={{ boxShadow: "0 24px 60px rgba(242,94,0,0.45)" }}>
            Disponibile per Android
          </Pill>
        </Pop>
        <div
          style={{
            position: "absolute",
            top: 1600,
            fontFamily: FONT_BODY,
            fontWeight: 600,
            fontSize: 44,
            letterSpacing: 2,
            color: "rgba(23,40,107,0.65)",
            opacity: interpolate(frame, [tEnd + 10, tEnd + 26], [0, 1], clamp),
          }}
        >
          Diario · AI · Digiuno · Ricette
        </div>
      </AbsoluteFill>
    </Stage>
  );
};
