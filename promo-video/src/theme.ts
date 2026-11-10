// Palette, griglia ritmica e tempi delle scene. 120 BPM a 60 fps: 1 beat = 30 frame, 1 battuta = 120 frame.
export const FPS = 60;
export const W = 1080;
export const H = 1920;
export const BEAT = 30;
export const BAR = 120;
export const DURATION = 1800;

/** Inizio di ogni scena (frame). Ogni scena dura 2 battute, il finale 1. */
export const SC = {
  logo: 0,
  unaApp: 240,
  oggi: 480,
  piano: 720,
  cucina: 960,
  progressi: 1200,
  jarvis: 1440,
  finale: 1680,
} as const;

export const C = {
  orange: "#FF6B00",
  orange2: "#FF9A1F",
  amber: "#FFC24A",
  navy: "#0B1437",
  ink: "#0C1326",
  blue: "#1565D8",
  blueDeep: "#0A2A8C",
  blueText: "#1D3A8A",
  green: "#2DBE3C",
  space0: "#030716",
  space1: "#0A1B4D",
  uiBg: "#F1F5FC",
  card: "#FFFFFF",
  line: "#E3EAF6",
  mute: "#51607F",
};

export const FONT = "Poppins, 'Inter', system-ui, sans-serif";

// Geometria dello schermo dell'app (coordinate delle schermate originali)
export const SCREEN_W = 941;
export const SCREEN_H = 1672;
export const SCREEN_C = { x: SCREEN_W / 2, y: SCREEN_H / 2 };
/** Scala base del telefono nel fotogramma 1080x1920 */
export const PHONE_BASE = 0.8;
