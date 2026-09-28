import { defineConfig } from "vite";
import { linkOutputDir, uicommonAliases } from "../vite.shared.js";

// Dev only. This tool is run by whoever maintains the app's data, never deployed, so
// there is no build or preview configuration.
export default defineConfig(({ command }) => ({
  root: "src/main/web",
  resolve: {
    alias: [
      ...uicommonAliases(),
      { find: "@linkOutputDir", replacement: linkOutputDir("scrapereview", command) }
    ]
  },
  json: { stringify: true },
  // A port of its own, so it can run alongside the app rather than instead of it.
  server: { port: 5174, strictPort: true }
}));
