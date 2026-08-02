import { defineConfig } from "vite";
import { linkOutputDir, uicommonAliases } from "../vite.shared.js";

// Vite outputs a warning about sourcemaps. I don't know why, since the browser can
// find and use the sourcemaps correctly. I did an investigation and wrote up a
// summary here:
// https://github.com/scala-js/vite-plugin-scalajs/issues/4#issuecomment-1771614021
export default defineConfig(({ command }) => ({
  root: "src/main/web",
  publicDir: "dynamic",
  resolve: {
    alias: [
      ...uicommonAliases(),
      { find: "@linkOutputDir", replacement: linkOutputDir("ui", command) }
    ]
  },
  json: { stringify: true },
  worker: { format: "es" },
  server: { port: 5173, strictPort: true },
  preview: { port: 5173, strictPort: true },
  build: { outDir: "../../../target/vite", emptyOutDir: true }
}));
