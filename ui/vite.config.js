import { defineConfig } from "vite";
import { linkOutputDir, uicommonAliases } from "../vite.shared.js";

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
  server: {
    port: 5173,
    strictPort: true,
    // Watching the ~17k item images costs about 30 seconds of blocked startup on Windows,
    // as chokidar opens a watcher per file. Images added while the server is running,
    // such as by scrapereview, are not served until it restarts.
    watch: { ignored: ["**/dynamic/assets/images/items/**"] }
  },
  preview: { port: 5173, strictPort: true },
  build: { outDir: "../../../target/vite", emptyOutDir: true }
}));
