import React from "react";
import { AbsoluteFill, Easing, Img, interpolate, Sequence, staticFile, useCurrentFrame } from "remotion";
import { AppScreen } from "../../components/AppScreen";
import { C, FONT_BODY, FONT_TITLE } from "../../theme";
import { Pill, Words } from "../Kinetic";
import { beatPulse, useSceneStart, useWords } from "../tl";

const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

// ─────────────────────────────────────────────────────────────────────────────
/** "Piano pasti. Ricette. Spesa. Progressi." — un taglio per ogni parola pronunciata. */
export const Montage: React.FC = () => {
  const frame = useCurrentFrame();
  const s0 = useSceneStart();
  const w = useWords("L10");
  const cuts = [
    { at: 0, clip: "piano", label: "Piano pasti", color: C.orange },
    { at: w[2].start - 2, clip: "cucina", label: "Ricette", color: C.green },
    { at: w[3].start - 2, clip: "spesa", label: "Spesa", color: C.carbs },
    { at: w[4].start - 2, clip: "progressi", label: "Progressi", color: C.fasting },
  ];
  const idx = cuts.reduce((a, c, i) => (frame >= c.at ? i : a), 0);
  const since = frame - cuts[idx].at;
  const punch = Math.exp(-since / 5);
  const pulse = beatPulse(s0 + frame, 6);

  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      {/* parola gigante dietro al telefono */}
      <div
        style={{
          position: "absolute",
          top: 120,
          fontFamily: FONT_TITLE,
          fontWeight: 800,
          fontSize: 150,
          color: cuts[idx].color,
          letterSpacing: -2,
          scale: `${1.25 - Math.min(1, since / 8) * 0.25}`,
          textShadow: `0 0 60px ${cuts[idx].color}88`,
        }}
      >
        {cuts[idx].label}
      </div>
      <div
        style={{
          position: "absolute",
          top: 360,
          scale: `${0.86 + punch * 0.05 + pulse * 0.01}`,
          rotate: `${(idx % 2 ? 1 : -1) * punch * 2}deg`,
          transformOrigin: "50% 0%",
        }}
      >
        {cuts.map((c, i) => (
          <Sequence key={c.clip} from={c.at} durationInFrames={(cuts[i + 1]?.at ?? 400) - c.at} layout="none">
            <AppScreen clip={c.clip} />
          </Sequence>
        ))}
      </div>
      <div style={{ position: "absolute", top: 1620, display: "flex", gap: 16 }}>
        {cuts.map((c, i) => (
          <div key={c.clip} style={{ width: i === idx ? 90 : 24, height: 24, borderRadius: 12, background: i <= idx ? c.color : "rgba(255,255,255,0.25)" }} />
        ))}
      </div>
    </AbsoluteFill>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Tutto, in un'unica app." — tre schermate si aprono a ventaglio. */
export const AllInOne: React.FC = () => {
  const frame = useCurrentFrame();
  const open = interpolate(frame, [0, 22], [0, 1], { ...clamp, easing: Easing.bezier(0.16, 1, 0.3, 1) });
  const phones = [
    { clip: "cucina", r: -14, x: -330 },
    { clip: "progressi", r: 14, x: 330 },
    { clip: "piano", r: 0, x: 0 },
  ];
  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      <div style={{ position: "absolute", top: 150 }}>
        <Words line="L11" size={128} accent={{ 2: C.orangeLight, 3: C.orangeLight }} variant="slam" />
      </div>
      {phones.map((p) => (
        <div
          key={p.clip}
          style={{
            position: "absolute",
            top: 560,
            scale: p.r === 0 ? "0.78" : "0.66",
            translate: `${p.x * open}px ${p.r === 0 ? 0 : 60 * open}px`,
            rotate: `${p.r * open}deg`,
            transformOrigin: "50% 0%",
          }}
        >
          <AppScreen clip={p.clip} />
        </div>
      ))}
    </AbsoluteFill>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Il futuro delle app è già qui. Ed è nel tuo telefono." */
export const Future: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L12");
  const tPhone = w[7].start;
  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      <div style={{ position: "absolute", top: 130 }}>
        <Words line="L12" to={7} size={104} accent={{ 1: C.orangeLight }} />
      </div>
      {/* anelli orbitali */}
      {[0, 1, 2].map((i) => (
        <div
          key={i}
          style={{
            position: "absolute",
            top: 640 - i * 60,
            width: 760 + i * 160,
            height: 760 + i * 160,
            borderRadius: "50%",
            border: `2px solid rgba(255,${138 - i * 30},${28 + i * 60},${0.5 - i * 0.12})`,
            transform: `rotateX(70deg) rotateZ(${frame * (0.6 + i * 0.3)}deg)`,
            opacity: interpolate(frame, [i * 6, i * 6 + 20], [0, 1], clamp),
          }}
        />
      ))}
      <div
        style={{
          position: "absolute",
          top: 520,
          perspective: 1800,
          scale: `${interpolate(frame, [0, 30], [0.6, 0.78], { ...clamp, easing: Easing.spring({ damping: 14 }) })}`,
          transformOrigin: "50% 0%",
        }}
      >
        <div style={{ transform: `rotateY(${Math.sin(frame / 30) * 10}deg) rotateX(${6 + Math.cos(frame / 40) * 3}deg)`, translate: `0px ${Math.sin(frame / 25) * 14}px` }}>
          <AppScreen clip="diary" playbackRate={0.6} />
        </div>
      </div>
      <div style={{ position: "absolute", top: 1560 }}>
        <Words line="L12" from={7} size={84} accent={{ 10: C.orangeLight, 11: C.orangeLight }} />
      </div>
      {/* lampo di luce quando dice "telefono" */}
      <AbsoluteFill
        style={{
          background: `radial-gradient(circle at 50% 55%, ${C.orange}55, transparent 55%)`,
          opacity: interpolate(frame, [tPhone, tPhone + 8, tPhone + 40], [0, 1, 0.3], clamp),
          pointerEvents: "none",
        }}
      />
    </AbsoluteFill>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
/** "Jarvis Food. Mangia meglio, ogni giorno." + call to action. */
export const Outro: React.FC = () => {
  const frame = useCurrentFrame();
  const w = useWords("L13");
  const tEnd = w[w.length - 1].end;
  return (
    <AbsoluteFill style={{ alignItems: "center" }}>
      {[0, 1].map((i) => {
        const p = interpolate(frame, [i * 8, 70 + i * 8], [0, 1], { ...clamp, easing: Easing.bezier(0.16, 1, 0.3, 1) });
        return (
          <div key={i} style={{ position: "absolute", top: 360, width: 600, height: 600, borderRadius: "50%",
            border: `${10 - i * 4}px solid ${i ? C.gold : C.orange}`, scale: `${0.6 + p * 2.4}`, opacity: (1 - p) * 0.9 }} />
        );
      })}
      <div
        style={{
          position: "absolute",
          top: 60,
          width: 1200,
          height: 1200,
          borderRadius: "50%",
          background: `repeating-conic-gradient(from 0deg, ${C.orange}33 0deg 6deg, transparent 6deg 24deg)`,
          maskImage: "radial-gradient(circle, black 0%, transparent 62%)",
          WebkitMaskImage: "radial-gradient(circle, black 0%, transparent 62%)",
          rotate: `${frame * 0.25}deg`,
          opacity: interpolate(frame, [0, 30], [0, 0.9], clamp),
        }}
      />
      <Img
        src={staticFile("jarvis_logo.png")}
        style={{
          position: "absolute",
          top: 380,
          width: 480,
          height: 480,
          borderRadius: "50%",
          boxShadow: "0 30px 90px rgba(0,0,0,0.6), 0 0 160px rgba(242,94,0,0.7)",
          scale: `${interpolate(frame, [0, 34], [2.2, 1], { ...clamp, easing: Easing.spring({ damping: 13 }) })}`,
          opacity: interpolate(frame, [0, 8], [0, 1], clamp),
        }}
      />
      <div style={{ position: "absolute", top: 940 }}>
        <Words line="L13" to={2} size={150} variant="slam" accent={{ 1: C.orangeLight }} width={1000} />
      </div>
      <div style={{ position: "absolute", top: 1140 }}>
        <Words line="L13" from={2} size={70} font="body" weight={500} variant="rise" color="rgba(255,255,255,0.92)" />
      </div>
      <div
        style={{
          position: "absolute",
          top: 1340,
          opacity: interpolate(frame, [tEnd, tEnd + 8], [0, 1], clamp),
          scale: `${interpolate(frame, [tEnd, tEnd + 20], [0.6, 1], { ...clamp, easing: Easing.spring({ damping: 10 }) })}`,
        }}
      >
        <Pill color="#C94D00" size={62}>Disponibile per Android</Pill>
      </div>
      <div
        style={{
          position: "absolute",
          top: 1560,
          fontFamily: FONT_BODY,
          fontWeight: 600,
          fontSize: 44,
          letterSpacing: 2,
          color: "rgba(255,255,255,0.7)",
          opacity: interpolate(frame, [tEnd + 14, tEnd + 30], [0, 1], clamp),
        }}
      >
        Diario · AI · Digiuno · Ricette
      </div>
    </AbsoluteFill>
  );
};
