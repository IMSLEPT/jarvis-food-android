/**
 * Configurazione della CLI Remotion (npx remotion studio / render).
 * Docs: https://remotion.dev/docs/config
 */
import { Config } from "@remotion/cli/config";

Config.setVideoImageFormat("jpeg");
Config.setJpegQuality(95);
Config.setCodec("h264");
Config.setCrf(17);
Config.setPixelFormat("yuv420p");
Config.setOverwriteOutput(true);
// Se Remotion non puo' scaricare il suo browser, indicane uno locale: CHROME_BIN=/percorso/chrome npm run render
if (process.env.CHROME_BIN) {
  Config.setBrowserExecutable(process.env.CHROME_BIN);
}
