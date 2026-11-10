import React from "react";
import { AbsoluteFill, staticFile, useCurrentFrame } from "remotion";
import { Audio } from "@remotion/media";
import "./fonts";
import { kf, KF, EZ, ip } from "./anim";
import { Background } from "./components/Background";
import { Caption, words } from "./components/Caption";
import { SceneLogo } from "./scenes/SceneLogo";
import { PhoneJourney } from "./scenes/PhoneJourney";
import { SceneJarvis } from "./scenes/SceneJarvis";
import { Cta, SceneFinale } from "./scenes/SceneFinale";

// "Camera" del fondo: scatti rapidi sulle transizioni (a tempo), deriva lenta durante le presentazioni.
const CAM_Y: KF = [[0, 0], [200, 80], [252, 900, EZ.expoInOut], [440, 980], [492, 1900, EZ.expoInOut], [690, 1980], [940, 2040], [984, 3100, EZ.expoInOut], [1176, 3180], [1230, 6200, EZ.expoInOut], [1405, 6280], [1456, 7200, EZ.expoInOut], [1644, 7280], [1694, 8600, EZ.expoInOut], [1800, 8640]];
const CAM_X: KF = [[0, 0], [690, 60], [752, 1700, EZ.expoInOut], [1800, 1800]];
const MOOD: KF = [[440, 0], [500, 0.8], [1640, 0.8], [1682, 0]];

export const Spot: React.FC<{ cta: Cta }> = ({ cta }) => {
  const f = useCurrentFrame();
  const camX = kf(f, CAM_X);
  const camY = kf(f, CAM_Y);
  const fog = kf(f, [[484, 0], [530, 1], [1650, 1], [1676, 0]]);
  const light = { dark: false } as const;
  return (
    <AbsoluteFill style={{ backgroundColor: "#030716" }}>
      <Background frame={f} mood={kf(f, MOOD)} camX={camX} camY={camY} velX={camX - kf(f - 1, CAM_X)} velY={camY - kf(f - 1, CAM_Y)} energy={f % 240 < 120 && f >= 240 ? 1 : 0.4} />
      <PhoneJourney f={f} />
      <SceneLogo f={f} />
      <SceneJarvis f={f} />
      {/* velo chiaro per i titoli delle funzionalita' */}
      <AbsoluteFill style={{ background: "linear-gradient(180deg, rgba(255,255,255,0) 71%, rgba(255,255,255,0.88) 80%, rgba(255,255,255,0.98) 86%, #FFFFFF 100%)", opacity: fog }} />
      <SceneFinale f={f} cta={cta} />
      {/* titoli */}
      <Caption frame={f} inAt={298} outAt={440} y={1512} size={72} weight={800} dark lines={[words("Un’unica app"), words("per dieta, allenamento"), words("e benessere", ["benessere"])]} />
      <Caption frame={f} inAt={588} outAt={688} y={1586} {...light} lines={[words("Tieni sotto controllo"), words("calorie e macro", ["calorie", "e", "macro"])]} />
      <Caption frame={f} inAt={812} outAt={930} y={1586} {...light} lines={[words("Organizza i tuoi pasti"), words("con un piano personalizzato", ["piano", "personalizzato"])]} />
      <Caption frame={f} inAt={1036} outAt={1172} y={1586} {...light} lines={[words("Scopri ricette sane, veloci", ["ricette"]), words("e adatte ai tuoi obiettivi")]} />
      <Caption frame={f} inAt={1304} outAt={1410} y={1586} {...light} lines={[words("Monitora i tuoi progressi", ["progressi"]), words("e raggiungi i tuoi obiettivi")]} />
      <Caption frame={f} inAt={1506} outAt={1646} y={1586} {...light} lines={[words("Il tuo assistente AI", ["assistente", "AI"]), words("per nutrizione e stile di vita")]} />
      {/* leggerissimo lampo sui colpi di cassa principali: lega immagine e musica */}
      <AbsoluteFill style={{ background: "#fff", opacity: f >= 240 && f < 1680 && f % 240 < 6 ? ip(f % 240, [0, 6], [0.10, 0]) : 0, mixBlendMode: "overlay" }} />
      <Audio src={staticFile("audio/music.wav")} volume={0.84} />
      <Audio src={staticFile("audio/sfx.wav")} volume={0.84} />
    </AbsoluteFill>
  );
};
