# maprender

Renders the map's tiles from the Old School RuneScape game cache, for the [map-tiles](https://github.com/leagueplans/map-tiles) repository. GitHub Pages serves that repository at `https://www.leagueplans.com/map-tiles/`, and the app's map loads its tiles from there.

## Updates

The map-tiles repository's [Update the map](https://github.com/leagueplans/map-tiles/blob/main/.github/workflows/update-map.yml) workflow runs the renderer every Thursday, after the game's weekly update, and opens a pull request with whatever changed. The pull request's description is the render's report, with a link to its overview. Merging it publishes the tiles. Responses are cached for up to four hours, so changed tiles can take that long to appear.

## Rendering by hand

To try a change to the renderer, render into the `docs` folder of a map-tiles checkout. It takes around 20 minutes.

```bash
sbt "mapRenderer/run --latest <download directory> <map-tiles checkout>/docs [<results directory>]"
```

`--latest` downloads the newest live cache from the [OpenRS2 archive](https://archive.openrs2.org/caches) into the download directory first. To render a cache already on disk, give its directory instead, the folder holding `main_file_cache.dat2`: `sbt "mapRenderer/run <cache directory> <map-tiles checkout>/docs"`. The results directory gets `report.md`, which says in Markdown what changed, and `overview.png`, the whole map with the regions that changed highlighted, one panel per plane with changes. Without a results directory, neither is written.

Check the tiles in the app by starting its dev server as in [ui/README.md](../ui/README.md), with `MAP_TILES_DIR` set to the same `docs` folder. Without it, the dev server loads tiles from the live site.

The render is written to a temporary folder beside the output directory, then compared with what's there. Only tiles whose pixels changed are written, so the repository only changes when the map does. Tiles that no longer exist are deleted, `index.json` and `places.json` are replaced, and other files, such as `.nojekyll`, are left alone. If a render has more than 5% fewer tiles than the output, the report opens with a warning, since that is what a cache the library can't fully read looks like. The removed regions are filled red in the overview.

## The cache library

The renderer uses RuneLite's [cache library](https://github.com/runelite/runelite/tree/master/cache), pinned in `build.sbt`. Game updates can change the cache's format, so a new cache may need a newer version. If a render fails, or its tiles look wrong, try the latest version of the library.

To bump the library, change its version in `build.sbt`, run `sbt mapRenderer/test`, then render and look over the result as above. The tests cover the zoom pyramid, the index, reading places, choosing a cache, publishing, the report and the overview, but not drawing the map. The renderer calls one of the library's private methods, `MapImageDumper.drawMapIcons`, to draw icons that cross region borders (see `RegionRenderer`). If a new version renames or changes it, the render fails on the first region it draws.

## What it writes

- `<plane>/<zoom>/<x>_<y>.png`: 256 px tiles in Leaflet's tile scheme, with one map unit per game tile. At zoom 2 a tile is one region at 4 px per game tile, and each zoom below halves the scale, down to -3. `TileKey` has the arithmetic. Only tiles with something on them are written.
- `index.json`: every tile that exists, so the map never requests a missing one.
- `places.json`: the place names on the game's world map, for the map's labels and place search.

The app reads all three, and it is deployed separately from the tiles, so these layouts are published formats. Add fields to the JSON files rather than renaming or removing them, and keep each tile's path a function of game coordinates alone. The formats are defined once, in `common` (`TileKey`, `TileIndex` and `Place`), which the renderer and the app share.
