/**
 * Palette e tempi del promo, allineati al tema "Aurora" dell'app
 * (app/src/main/java/com/jarvis/food/ui/Theme.kt).
 */
export const C = {
  bgDeep: "#060B26",
  navy: "#17286B",
  navyDeep: "#13225F",
  navyBright: "#1B3382",
  orange: "#F25E00",
  orangeLight: "#FF8A1C",
  gold: "#FFB02E",
  text: "#1D1B3A",
  muted: "#5E5B7A",
  appBg: "#F7F9FC",
  line: "#E9ECF1",
  carbs: "#3B82F6",
  protein: "#F43F6E",
  fat: "#F59E0B",
  green: "#10B981",
  water: "#5CD6EA",
  fasting: "#8B5CF6",
  white: "#FFFFFF",
} as const;

export const FONT_TITLE = "Bricolage";
export const FONT_BODY = "DM Sans";

/**
 * Griglia musicale: 120 BPM a 60 fps → 1 beat = 30 frame, 1 battuta = 120 frame.
 * Ogni scena (tranne la prima) inizia 12 frame prima del suo downbeat,
 * perché le transizioni durano 24 frame centrate sul taglio.
 */
export const FPS = 60;
export const BEAT = 30;
export const TRANSITION = 24;
export const LEAD = TRANSITION / 2; // primo downbeat nel tempo locale della scena

/** Durate delle sequenze (transizioni incluse). Somma − 6×24 = 1800 frame = 30 s. */
export const SCENES = {
  intro: 252, // 0 → 4 s
  hook: 264, // 4 → 8 s
  diary: 384, // 8 → 14 s
  aiPhoto: 264, // 14 → 18 s
  fasting: 264, // 18 → 22 s
  features: 264, // 22 → 26 s
  outro: 252, // 26 → 30 s
} as const;

export const TOTAL = 1800;
