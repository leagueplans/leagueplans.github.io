# maprender

Renders the map's tiles from the Old School RuneScape game cache, for the [map-tiles](https://github.com/leagueplans/map-tiles) repository. GitHub Pages serves that repository at `https://www.leagueplans.com/map-tiles/`, and the app's map loads its tiles from there.

## Rendering a new tile set

1. Download the newest live `oldschool` cache from the [OpenRS2 archive](https://archive.openrs2.org/caches) in the `.dat2`/`.idx` format, and unzip it. The cache directory is the folder holding `main_file_cache.dat2`.
2. Render into the `docs` folder of a map-tiles checkout. It takes around 15 minutes.
   ```bash
   sbt "mapRenderer/run <cache directory> <map-tiles checkout>/docs"
   ```
3. Check the tiles in the app before publishing them. Start its dev server as in [ui/README.md](../ui/README.md), with `MAP_TILES_DIR` set to the same `docs` folder. Without it, the dev server loads tiles from the live site.
4. Commit in the map-tiles checkout. Pushing publishes the tiles. Responses are cached for up to four hours, so changed tiles can take that long to appear.

A render replaces everything it owns in the output directory: the plane folders `0` to `3`, `index.json` and `places.json`. Tiles that no longer exist disappear, and other files, such as `.nojekyll`, are left alone. The render is written to a temporary folder beside the output directory and only moved in once it has succeeded, so a failed render leaves the output as it was.

## The cache library

The renderer uses RuneLite's [cache library](https://github.com/runelite/runelite/tree/master/cache), pinned in `build.sbt`. Game updates can change the cache's format, so a new cache may need a newer version. If a render fails, or its tiles look wrong, try the latest version of the library.

To bump the library, change its version in `build.sbt`, run `sbt mapRenderer/test`, then render and look over the result as above. The tests cover the zoom pyramid, writing the index and reading places, but not drawing. The renderer calls one of the library's private methods, `MapImageDumper.drawMapIcons`, to draw icons that cross region borders (see `RegionRenderer`). If a new version renames or changes it, the render fails on the first region it draws.

## What it writes

- `<plane>/<zoom>/<x>_<y>.png`: 256 px tiles in Leaflet's tile scheme, with one map unit per game tile. At zoom 2 a tile is one region at 4 px per game tile, and each zoom below halves the scale, down to -3. `TileKey` has the arithmetic. Only tiles with something on them are written.
- `index.json`: every tile that exists, so the map never requests a missing one.
- `places.json`: the place names on the game's world map, for the map's labels and place search.

The app reads all three, and it is deployed separately from the tiles, so these layouts are published formats. Add fields to the JSON files rather than renaming or removing them, and keep each tile's path a function of game coordinates alone. The formats are defined once, in `common` (`TileKey`, `TileIndex` and `Place`), which the renderer and the app share.
