import { loadFont } from "@remotion/fonts";
import { staticFile } from "remotion";
import { FONT_BODY, FONT_TITLE } from "./theme";

/** Font dell'app (Bricolage Grotesque + DM Sans), caricati da public/fonts. */
export const fontsReady = Promise.all([
  loadFont({ family: FONT_TITLE, url: staticFile("fonts/bricolage_semibold.ttf"), weight: "600" }),
  loadFont({ family: FONT_TITLE, url: staticFile("fonts/bricolage_bold.ttf"), weight: "700" }),
  loadFont({ family: FONT_TITLE, url: staticFile("fonts/bricolage_extrabold.ttf"), weight: "800" }),
  loadFont({ family: FONT_BODY, url: staticFile("fonts/dmsans_regular.ttf"), weight: "400" }),
  loadFont({ family: FONT_BODY, url: staticFile("fonts/dmsans_medium.ttf"), weight: "500" }),
  loadFont({ family: FONT_BODY, url: staticFile("fonts/dmsans_semibold.ttf"), weight: "600" }),
]);
