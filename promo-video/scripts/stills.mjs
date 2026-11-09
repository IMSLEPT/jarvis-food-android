/**
 * Renderizza fotogrammi di controllo (PNG) della composizione JarvisPromo.
 * Uso: node scripts/stills.mjs <cartella-output> [frame,frame,...]
 * Variabile opzionale REMOTION_BROWSER = eseguibile Chrome/Chromium headless.
 */
import { bundle } from "@remotion/bundler";
import { renderStill, selectComposition } from "@remotion/renderer";
import fs from "node:fs";
import path from "node:path";

const out = process.argv[2] ?? "out/stills";
const frames = (process.argv[3] ?? "60,200,300,330,400,560,700,900,1000,1150,1260,1380,1460,1530,1600,1700")
  .split(",")
  .map(Number);
fs.mkdirSync(out, { recursive: true });

const browserExecutable = process.env.REMOTION_BROWSER || null;
const serveUrl = await bundle({ entryPoint: path.resolve("src/index.ts") });
const composition = await selectComposition({ serveUrl, id: "JarvisPromo", browserExecutable });
for (const frame of frames) {
  const output = path.join(out, `f${String(frame).padStart(4, "0")}.png`);
  await renderStill({ composition, serveUrl, frame, output, browserExecutable, scale: 0.5 });
  console.log("ok", output);
}
