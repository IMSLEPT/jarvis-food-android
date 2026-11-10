import { loadFont } from "@remotion/fonts";
import { cancelRender, continueRender, delayRender, staticFile } from "remotion";

const weights = ["400", "500", "600", "700", "800"];
const handle = delayRender("Caricamento font Poppins");
Promise.all(
  weights.flatMap((w) =>
    (["normal", "italic"] as const).map((style) =>
      loadFont({
        family: "Poppins",
        url: staticFile(`fonts/poppins-latin-${w}-${style}.woff2`),
        weight: w,
        style,
      }),
    ),
  ),
)
  .then(() => continueRender(handle))
  .catch((e) => cancelRender(e));
