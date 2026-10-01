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
  server: { port: 5173, strictPort: true },
  preview: { port: 5173, strictPort: true },
  build: { outDir: "../../../target/vite", emptyOutDir: true }
}));
