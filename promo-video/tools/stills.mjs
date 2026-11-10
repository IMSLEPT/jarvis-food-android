// Rende fotogrammi di controllo e li impagina in un provino.  Uso: node tools/stills.mjs 0,30,60 [nome] [scala]
import { bundle } from "@remotion/bundler";
import { renderStill, selectComposition } from "@remotion/renderer";
import { execSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";

const frames = process.argv[2].split(",").map(Number);
const name = process.argv[3] ?? "sheet";
const scale = Number(process.argv[4] ?? 0.4);
const comp = process.argv[5] ?? "JarvisFoodSpot";
const out = "/tmp/stills";
fs.mkdirSync(out, { recursive: true });
const browserExecutable = process.env.CHROME_BIN;
const serveUrl = fs.existsSync("/tmp/jf-bundle/index.html") && process.env.REUSE ? "/tmp/jf-bundle" : await bundle({ entryPoint: path.resolve("src/index.ts"), outDir: "/tmp/jf-bundle" });
const composition = await selectComposition({ serveUrl, id: comp, browserExecutable });
const files = [];
for (const fr of frames) {
  const o = `${out}/${name}_${String(fr).padStart(4, "0")}.png`;
  await renderStill({ composition, serveUrl, frame: fr, output: o, scale, browserExecutable });
  files.push(o);
}
const cols = Math.min(files.length, Number(process.env.COLS ?? 6));
execSync(`ffmpeg -v error -y ${files.map((f) => `-i ${f}`).join(" ")} -filter_complex "${files.map((_, i) => `[${i}]drawtext=text='${frames[i]}':fontsize=28:fontcolor=yellow:box=1:boxcolor=black@0.6:x=6:y=6[v${i}]`).join(";")};${files.map((_, i) => `[v${i}]`).join("")}xstack=inputs=${files.length}:layout=${files.map((_, i) => `${(i % cols)}*w0_${Math.floor(i / cols)}*h0`.replace(/(\d+)\*w0/, (m, a) => (a === "0" ? "0" : new Array(Number(a)).fill("w0").join("+"))).replace(/_(\d+)\*h0/, (m, a) => "_" + (a === "0" ? "0" : new Array(Number(a)).fill("h0").join("+")))).join("|")}" /tmp/stills/${name}.png`);
console.log("ok", `/tmp/stills/${name}.png`);
