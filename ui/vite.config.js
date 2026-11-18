import { defineConfig } from "vite";
import { createReadStream, statSync } from "fs";
import { extname, resolve, sep } from "path";
import { linkOutputDir, uicommonAliases } from "../vite.shared.js";

// The map's tiles are published from their own repository, at /map-tiles on the live site.
// In development they come from there too, unless MAP_TILES_DIR names a local render, such as
// the docs folder of a map-tiles checkout, to try out a change to the renderer.
const mapTilesDir = process.env.MAP_TILES_DIR && resolve(process.env.MAP_TILES_DIR);

const mapTileTypes = { ".png": "image/png", ".json": "application/json" };

const localMapTiles = {
  name: "local-map-tiles",
  configureServer(server) {
    server.middlewares.use("/map-tiles", (req, res) => {
      const file = resolve(mapTilesDir, "." + decodeURIComponent(req.url.split("?")[0]));
      const type = mapTileTypes[extname(file)];
      const isFile = (() => { try { return statSync(file).isFile(); } catch { return false; } })();
      if (!file.startsWith(mapTilesDir + sep) || !type || !isFile) {
        res.statusCode = 404;
        res.end();
      } else {
        res.setHeader("Content-Type", type);
        createReadStream(file).pipe(res);
      }
    });
  }
};

export default defineConfig(({ command }) => ({
  root: "src/main/web",
  plugins: mapTilesDir ? [localMapTiles] : [],
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
    watch: { ignored: ["**/dynamic/assets/images/items/**"] },
    proxy: mapTilesDir ? {} : {
      "/map-tiles": { target: "https://www.leagueplans.com", changeOrigin: true }
    }
  },
  preview: { port: 5173, strictPort: true },
  build: { outDir: "../../../target/vite", emptyOutDir: true }
}));
